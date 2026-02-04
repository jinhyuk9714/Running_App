import Foundation
import Combine
import CoreLocation
import MapKit

/// 실시간 GPS 수집, 경로(route) 배열 생성
final class LocationManager: NSObject, ObservableObject {
    private let manager = CLLocationManager()
    @Published var authorizationStatus: CLAuthorizationStatus = .notDetermined
    @Published var currentLocation: CLLocation?
    @Published var routePoints: [CLLocation] = []
    @Published var isTracking = false
    @Published var totalDistance: Double = 0  // meter

    override init() {
        super.init()
        manager.delegate = self
        manager.desiredAccuracy = kCLLocationAccuracyBest
        manager.distanceFilter = 2  // 2m 간격으로 더 촘촘히 수집
        manager.activityType = .fitness  // 야외 러닝에 맞는 GPS 최적화
        manager.allowsBackgroundLocationUpdates = true
        manager.pausesLocationUpdatesAutomatically = false
        authorizationStatus = manager.authorizationStatus
    }

    func requestPermission() {
        manager.requestWhenInUseAuthorization()
    }

    func startTracking() {
        routePoints.removeAll()
        totalDistance = 0
        isTracking = true
        manager.startUpdatingLocation()
    }

    func stopTracking() {
        isTracking = false
        manager.stopUpdatingLocation()
    }

    /// 서버에 보낼 route 형식: [{ "lat": 37.5, "lng": 127.0, "timestamp": "2025-02-01T07:00:00.000Z" }, ...]
    func routeForServer() -> [[String: Any]] {
        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime, .withFractionalSeconds]
        return routePoints.map { loc in
            [
                "lat": loc.coordinate.latitude,
                "lng": loc.coordinate.longitude,
                "timestamp": formatter.string(from: loc.timestamp)
            ] as [String: Any]
        }
    }

    var region: MKCoordinateRegion {
        guard !routePoints.isEmpty else {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 37.5, longitude: 127.0),
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            )
        }
        let lats = routePoints.map(\.coordinate.latitude)
        let lngs = routePoints.map(\.coordinate.longitude)
        let minLat = lats.min() ?? 0, maxLat = lats.max() ?? 0
        let minLng = lngs.min() ?? 0, maxLng = lngs.max() ?? 0
        let center = CLLocationCoordinate2D(
            latitude: (minLat + maxLat) / 2,
            longitude: (minLng + maxLng) / 2
        )
        let span = MKCoordinateSpan(
            latitudeDelta: max(0.005, (maxLat - minLat) * 1.2),
            longitudeDelta: max(0.005, (maxLng - minLng) * 1.2)
        )
        return MKCoordinateRegion(center: center, span: span)
    }
}

extension LocationManager: CLLocationManagerDelegate {
    func locationManagerDidChangeAuthorization(_ manager: CLLocationManager) {
        authorizationStatus = manager.authorizationStatus
    }

    func locationManager(_ manager: CLLocationManager, didUpdateLocations locations: [CLLocation]) {
        guard let loc = locations.last else { return }
        // 정확도가 나쁜 점(65m 초과)은 경로에 넣지 않아 궤적이 튀지 않게 함
        if loc.horizontalAccuracy < 0 || loc.horizontalAccuracy > 65 { return }
        if isTracking {
            var newPoints = routePoints
            var newDistance = totalDistance
            if let last = routePoints.last {
                newDistance += loc.distance(from: last)
            }
            newPoints.append(loc)
            DispatchQueue.main.async { [weak self] in
                self?.currentLocation = loc
                self?.routePoints = newPoints
                self?.totalDistance = newDistance
            }
        } else {
            DispatchQueue.main.async { [weak self] in
                self?.currentLocation = loc
            }
        }
    }
}
