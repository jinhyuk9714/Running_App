import Foundation
import Combine
import HealthKit

/// 심박수·걸음수(케이던스 추정) 수집
final class HealthKitManager: ObservableObject {
    private let store = HKHealthStore()
    @Published var averageHeartRate: Int?  // bpm
    @Published var cadence: Int?  // steps/min (SPM)
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
    }

    func endRun(at endDate: Date) {
        runEnd = endDate
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
                self?.stepCountDuringRun = steps
                let durationMin = max(1, end.timeIntervalSince(start) / 60)
                self?.cadence = steps / Int(durationMin)  // SPM
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
