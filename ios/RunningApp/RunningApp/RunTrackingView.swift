import SwiftUI
import MapKit

/// 나이키 앱처럼: 시작 → 실시간 GPS·지도 → 완료 시 서버 저장
struct RunTrackingView: View {
    @StateObject private var locationManager = LocationManager()
    @StateObject private var healthKit = HealthKitManager()
    @State private var isRunning = false
    @State private var startTime: Date?
    @State private var endTime: Date?
    @State private var showingSaveError: String?
    @State private var isSaving = false
    @State private var saved = false
    /// 1초마다 갱신해서 거리·시간 실시간 표시
    @State private var tick: Int = 0

    private var durationSeconds: Int {
        guard let start = startTime else { return 0 }
        let end = isRunning ? Date() : (endTime ?? start)
        return Int(end.timeIntervalSince(start))
    }

    private var distanceKm: Double { locationManager.totalDistance / 1000 }
    /// 10m 미만이거나 페이스가 비정상(20분/km 초과)이면 표시하지 않음
    private var averagePaceSecPerKm: Int? {
        guard distanceKm >= 0.01, durationSeconds > 0 else { return nil }
        let secPerKm = Int(Double(durationSeconds) / distanceKm)
        guard secPerKm <= 1200 else { return nil } // 20분/km 초과 시 숨김
        return secPerKm
    }
    private var estimatedCalories: Int? {
        guard distanceKm > 0 else { return nil }
        return Int(distanceKm * 65)
    }

    var body: some View {
        ZStack(alignment: .bottom) {
            MapViewWithRoute(routePoints: locationManager.routePoints, region: locationManager.region)
                .ignoresSafeArea()

            VStack(spacing: 12) {
                if isRunning || saved {
                    HStack(spacing: 16) {
                        stat("거리", "\(String(format: "%.2f", distanceKm)) km")
                        stat("시간", "\(durationSeconds / 60):\(String(format: "%02d", durationSeconds % 60))")
                        if let pace = averagePaceSecPerKm {
                            stat("페이스", "\(pace / 60)'\(String(format: "%02d", pace % 60))\"/km")
                        } else if isRunning || saved {
                            stat("페이스", "--")
                        }
                        if let hr = healthKit.averageHeartRate { stat("심박", "\(hr) bpm") }
                        if let cad = healthKit.cadence { stat("케이던스", "\(cad) SPM") }
                    }
                    .padding(8)
                    .background(.ultraThinMaterial)
                    .cornerRadius(8)
                    .id(tick)
                }

                Button(action: toggleRun) {
                    Text(isRunning ? "완료" : "시작")
                        .font(.title2)
                        .frame(maxWidth: .infinity)
                        .padding()
                        .background(isRunning ? Color.red : Color.green)
                        .foregroundColor(.white)
                        .cornerRadius(12)
                }
                .disabled(isSaving)
            }
            .padding()
        }
        .alert("저장 실패", isPresented: .constant(showingSaveError != nil)) {
            Button("확인") { showingSaveError = nil }
        } message: {
            if let msg = showingSaveError { Text(msg) }
        }
        .onAppear {
            locationManager.requestPermission()
            Task { await healthKit.requestAuthorization() }
        }
        .onReceive(Timer.publish(every: 1, on: .main, in: .common).autoconnect()) { _ in
            if isRunning { tick += 1 }
        }
    }

    private func stat(_ label: String, _ value: String) -> some View {
        VStack(spacing: 2) {
            Text(label).font(.caption).foregroundColor(.secondary)
            Text(value).font(.headline)
        }
    }

    private func toggleRun() {
        if isRunning {
            endRun()
        } else {
            startTime = Date()
            locationManager.startTracking()
            healthKit.startRun(at: Date())
            saved = false
            isRunning = true
        }
    }

    private func endRun() {
        let ended = Date()
        endTime = ended
        isRunning = false
        locationManager.stopTracking()
        healthKit.endRun(at: ended)
        let dist = distanceKm
        let dur = durationSeconds
        let pace = averagePaceSecPerKm
        let cal = estimatedCalories
        let route = locationManager.routePoints.isEmpty ? nil : locationManager.routeForServer()
        let started = startTime ?? Date()
        isSaving = true
        Task {
            do {
                if dist <= 0 || dur <= 0 {
                    throw NSError(domain: "RunTracking", code: -1, userInfo: [NSLocalizedDescriptionKey: "거리나 시간이 0입니다. 잠시 더 달린 뒤 완료해 주세요."])
                }
                try await Task.sleep(nanoseconds: 2_500_000_000)
                try await APIClient.shared.postActivity(
                    distance: dist,
                    duration: dur,
                    averagePace: pace,
                    calories: cal,
                    averageHeartRate: healthKit.averageHeartRate,
                    cadence: healthKit.cadence,
                    route: route,
                    startedAt: started,
                    memo: nil
                )
                await MainActor.run { saved = true; isSaving = false }
            } catch {
                await MainActor.run {
                    showingSaveError = error.localizedDescription
                    isSaving = false
                }
            }
        }
    }
}

/// MKMapView + 경로선( MKPolyline ) 표시
struct MapViewWithRoute: UIViewRepresentable {
    var routePoints: [CLLocation]
    var region: MKCoordinateRegion

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.showsUserLocation = true
        map.delegate = context.coordinator
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        map.setRegion(region, animated: true)
        map.removeOverlays(map.overlays)
        if routePoints.count >= 2 {
            let coords = routePoints.map(\.coordinate)
            let polyline = MKPolyline(coordinates: coords, count: coords.count)
            map.addOverlay(polyline)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    class Coordinator: NSObject, MKMapViewDelegate {
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let poly = overlay as? MKPolyline {
                let renderer = MKPolylineRenderer(polyline: poly)
                renderer.strokeColor = .systemBlue
                renderer.lineWidth = 4
                return renderer
            }
            return MKOverlayRenderer(overlay: overlay)
        }
    }
}
