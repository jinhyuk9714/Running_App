import SwiftUI
import MapKit

/// 활동 상세: 통계 + 지도에 경로 표시
struct ActivityDetailView: View {
    let activityId: Int
    @State private var item: APIClient.ActivityItem?
    @State private var isLoading = true
    @State private var errorMessage: String?

    private var coordinates: [CLLocationCoordinate2D] {
        guard let route = item?.route, !route.isEmpty else { return [] }
        return route.map { CLLocationCoordinate2D(latitude: $0.lat, longitude: $0.lng) }
    }

    private var region: MKCoordinateRegion {
        guard !coordinates.isEmpty else {
            return MKCoordinateRegion(
                center: CLLocationCoordinate2D(latitude: 37.5, longitude: 127.0),
                span: MKCoordinateSpan(latitudeDelta: 0.01, longitudeDelta: 0.01)
            )
        }
        let lats = coordinates.map(\.latitude)
        let lngs = coordinates.map(\.longitude)
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

    var body: some View {
        Group {
            if isLoading {
                ProgressView("불러오는 중...")
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let err = errorMessage {
                VStack(spacing: 12) {
                    Text(err).foregroundColor(.red).multilineTextAlignment(.center)
                    Button("다시 시도") { Task { await load() } }
                }
                .frame(maxWidth: .infinity, maxHeight: .infinity)
            } else if let item = item {
                ScrollView {
                    VStack(alignment: .leading, spacing: 16) {
                        statsGrid(item: item)
                        if !coordinates.isEmpty {
                            Text("이동 경로")
                                .font(.headline)
                            MapViewWithRouteOnly(coordinates: coordinates, region: region)
                                .frame(height: 220)
                                .cornerRadius(12)
                        }
                    }
                    .padding()
                }
                .navigationTitle(formatDate(item.startedAt))
                .navigationBarTitleDisplayMode(.inline)
            }
        }
        .task { await load() }
    }

    private func statsGrid(item: APIClient.ActivityItem) -> some View {
        let paceStr = item.averagePace.map { "\($0 / 60)'\(String(format: "%02d", $0 % 60))\"/km" } ?? "-"
        let durationStr = "\(item.duration / 60):\(String(format: "%02d", item.duration % 60))"
        return LazyVGrid(columns: [
            GridItem(.flexible()),
            GridItem(.flexible())
        ], spacing: 12) {
            statCell("거리", String(format: "%.2f km", item.distance))
            statCell("시간", durationStr)
            statCell("평균 페이스", paceStr)
            if let c = item.calories { statCell("칼로리", "\(c) kcal") }
            if let hr = item.averageHeartRate { statCell("평균 심박수", "\(hr) bpm") }
            if let cad = item.cadence { statCell("케이던스", "\(cad) SPM") }
        }
    }

    private func statCell(_ label: String, _ value: String) -> some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(label).font(.caption).foregroundColor(.secondary)
            Text(value).font(.headline)
        }
        .frame(maxWidth: .infinity, alignment: .leading)
        .padding()
        .background(Color(.systemGray6))
        .cornerRadius(8)
    }

    private func formatDate(_ iso: String) -> String {
        guard let date = ISO8601DateFormatter().date(from: iso) else { return iso }
        let f = DateFormatter()
        f.dateFormat = "M월 d일 HH:mm"
        f.locale = Locale(identifier: "ko_KR")
        return f.string(from: date)
    }

    private func load() async {
        await MainActor.run { isLoading = true; errorMessage = nil }
        do {
            let activity = try await APIClient.shared.getActivity(id: activityId)
            await MainActor.run { item = activity; isLoading = false }
        } catch {
            await MainActor.run {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }
}

/// 경로만 표시하는 지도 (상세용)
struct MapViewWithRouteOnly: UIViewRepresentable {
    var coordinates: [CLLocationCoordinate2D]
    var region: MKCoordinateRegion

    func makeUIView(context: Context) -> MKMapView {
        let map = MKMapView()
        map.isUserInteractionEnabled = true
        map.delegate = context.coordinator
        return map
    }

    func updateUIView(_ map: MKMapView, context: Context) {
        map.setRegion(region, animated: false)
        map.removeOverlays(map.overlays)
        if coordinates.count >= 2 {
            var coords = coordinates
            let polyline = MKPolyline(coordinates: &coords, count: coords.count)
            map.addOverlay(polyline)
        }
    }

    func makeCoordinator() -> Coordinator { Coordinator() }

    class Coordinator: NSObject, MKMapViewDelegate {
        func mapView(_ mapView: MKMapView, rendererFor overlay: MKOverlay) -> MKOverlayRenderer {
            if let poly = overlay as? MKPolyline {
                let r = MKPolylineRenderer(polyline: poly)
                r.strokeColor = .systemBlue
                r.lineWidth = 4
                return r
            }
            return MKOverlayRenderer(overlay: overlay)
        }
    }
}
