import Foundation
import Combine
import HealthKit
import CoreMotion

/// 심박수(HealthKit) + 실시간 케이던스(CMPedometer)
final class HealthKitManager: ObservableObject {
    private let store = HKHealthStore()
    private let pedometer = CMPedometer()
    @Published var averageHeartRate: Int?  // bpm
    @Published var cadence: Int?  // steps/min (SPM) - 러닝 중 실시간
    @Published var isAuthorized = false
    private var heartRateSamples: [Int] = []
    private var stepCountDuringRun: Int = 0
    private var runStart: Date?
    private var runEnd: Date?

    func requestAuthorization() async {
        guard HKHealthStore.isHealthDataAvailable() else { return }
        let readTypes: Set<HKObjectType> = [
            HKObjectType.quantityType(forIdentifier: .heartRate)!,
            HKObjectType.quantityType(forIdentifier: .stepCount)!,
            HKObjectType.quantityType(forIdentifier: .distanceWalkingRunning)!
        ]
        do {
            try await store.requestAuthorization(toShare: [], read: readTypes)
            await MainActor.run { isAuthorized = true }
        } catch {
            await MainActor.run { isAuthorized = false }
        }
    }

    func startRun(at startDate: Date) {
        runStart = startDate
        runEnd = nil
        heartRateSamples.removeAll()
        stepCountDuringRun = 0
        averageHeartRate = nil
        cadence = nil
        guard CMPedometer.isStepCountingAvailable() else { return }
        pedometer.startUpdates(from: startDate) { [weak self] data, _ in
            guard let self = self, let data = data, let start = self.runStart else { return }
            let steps = data.numberOfSteps.intValue
            let elapsedMin = max(1.0, Date().timeIntervalSince(start) / 60.0)
            let spm = Int(Double(steps) / elapsedMin)
            DispatchQueue.main.async {
                self.stepCountDuringRun = steps
                self.cadence = spm
            }
        }
    }

    func endRun(at endDate: Date) {
        runEnd = endDate
        pedometer.stopUpdates()
        queryHeartRate(from: runStart!, to: endDate)
        queryStepCount(from: runStart!, to: endDate)
    }

    private func queryHeartRate(from start: Date, to end: Date) {
        guard let type = HKQuantityType.quantityType(forIdentifier: .heartRate) else { return }
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        let query = HKSampleQuery(sampleType: type, predicate: predicate, limit: HKObjectQueryNoLimit, sortDescriptors: nil) { [weak self] _, samples, _ in
            let values = (samples as? [HKQuantitySample])?.map { Int($0.quantity.doubleValue(for: HKUnit(from: "count/min"))) } ?? []
            DispatchQueue.main.async {
                self?.heartRateSamples = values
                if !values.isEmpty {
                    self?.averageHeartRate = values.reduce(0, +) / values.count
                }
            }
        }
        store.execute(query)
    }

    private func queryStepCount(from start: Date, to end: Date) {
        guard let type = HKQuantityType.quantityType(forIdentifier: .stepCount) else { return }
        let predicate = HKQuery.predicateForSamples(withStart: start, end: end, options: .strictStartDate)
        let query = HKStatisticsQuery(quantityType: type, quantitySamplePredicate: predicate, options: .cumulativeSum) { [weak self] _, result, _ in
            let steps = Int(result?.sumQuantity()?.doubleValue(for: .count()) ?? 0)
            DispatchQueue.main.async {
                if steps > 0 {
                    self?.stepCountDuringRun = steps
                    let durationMin = max(1, end.timeIntervalSince(start) / 60)
                    self?.cadence = steps / Int(durationMin)
                }
            }
        }
        store.execute(query)
    }

    func reset() {
        averageHeartRate = nil
        cadence = nil
        heartRateSamples.removeAll()
        stepCountDuringRun = 0
    }
}
