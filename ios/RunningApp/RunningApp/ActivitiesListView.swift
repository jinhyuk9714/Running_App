import SwiftUI

/// 저장된 활동 목록 (서버에서 조회)
struct ActivitiesListView: View {
    var onLogout: (() -> Void)?
    @State private var items: [APIClient.ActivityItem] = []
    @State private var isLoading = false
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Group {
                if isLoading, items.isEmpty {
                    ProgressView("불러오는 중...")
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if let err = errorMessage {
                    VStack(spacing: 12) {
                        Text(err).foregroundColor(.red).multilineTextAlignment(.center)
                        Button("다시 시도", action: load)
                    }
                    .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else if items.isEmpty {
                    Text("저장된 러닝 기록이 없습니다.")
                        .foregroundColor(.secondary)
                        .frame(maxWidth: .infinity, maxHeight: .infinity)
                } else {
                    List(items) { item in
                        NavigationLink(value: item.id) {
                            ActivityRowView(item: item)
                        }
                    }
                }
            }
            .navigationTitle("기록")
            .navigationDestination(for: Int.self) { id in
                ActivityDetailView(activityId: id)
            }
            .toolbar {
                if let onLogout = onLogout {
                    ToolbarItem(placement: .navigationBarTrailing) {
                        Button("로그아웃") {
                            APIClient.shared.logout()
                            onLogout()
                        }
                    }
                }
            }
            .refreshable { await loadAsync() }
            .onAppear { Task { await loadAsync() } }
        }
    }

    private func load() {
        Task { await loadAsync() }
    }

    private func loadAsync() async {
        await MainActor.run { isLoading = true; errorMessage = nil }
        do {
            let page = try await APIClient.shared.getActivities(page: 0, size: 50)
            await MainActor.run { items = page.content; isLoading = false }
        } catch {
            await MainActor.run {
                errorMessage = error.localizedDescription
                isLoading = false
            }
        }
    }
}

struct ActivityRowView: View {
    let item: APIClient.ActivityItem

    private var startedAtShort: String {
        guard let date = ISO8601DateFormatter().date(from: item.startedAt) else { return item.startedAt }
        let f = DateFormatter()
        f.dateFormat = "M/d HH:mm"
        f.locale = Locale(identifier: "ko_KR")
        return f.string(from: date)
    }

    var body: some View {
        VStack(alignment: .leading, spacing: 4) {
            Text(startedAtShort)
                .font(.subheadline)
                .foregroundColor(.secondary)
            HStack(spacing: 12) {
                Text(String(format: "%.2f km", item.distance))
                    .font(.headline)
                Text("\(item.duration / 60):\(String(format: "%02d", item.duration % 60))")
                    .font(.subheadline)
                if let pace = item.averagePace {
                    Text("\(pace / 60)'\(String(format: "%02d", pace % 60))\"/km")
                        .font(.caption)
                        .foregroundColor(.secondary)
                }
            }
        }
        .padding(.vertical, 4)
    }
}

extension APIClient.ActivityItem: Identifiable {}
