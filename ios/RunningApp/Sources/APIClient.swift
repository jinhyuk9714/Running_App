import Foundation

/// Running App 백엔드 API (로그인, 활동 저장)
final class APIClient {
    static let shared = APIClient()
    private let baseURL = "https://jinhyuk-portfolio1.shop"
    private let session: URLSession
    private var token: String?

    private init() {
        let config = URLSessionConfiguration.default
        config.timeoutIntervalForRequest = 30
        session = URLSession(configuration: config)
        token = UserDefaults.standard.string(forKey: "RunningAppJWT")
    }

    func setToken(_ t: String?) {
        token = t
        if let t = t { UserDefaults.standard.set(t, forKey: "RunningAppJWT") }
        else { UserDefaults.standard.removeObject(forKey: "RunningAppJWT") }
    }

    func isLoggedIn() -> Bool { token != nil && !(token?.isEmpty ?? true) }

    // MARK: - Login

    func login(email: String, password: String) async throws {
        let url = URL(string: "\(baseURL)/api/auth/login")!
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.httpBody = try JSONEncoder().encode(["email": email, "password": password])

        let (data, res) = try await session.data(for: req)
        guard let http = res as? HTTPURLResponse, http.statusCode == 200 else {
            throw NSError(domain: "APIClient", code: -1, userInfo: [NSLocalizedDescriptionKey: "로그인 실패"])
        }
        let json = try JSONSerialization.jsonObject(with: data) as? [String: Any]
        guard let accessToken = json?["accessToken"] as? String else {
            throw NSError(domain: "APIClient", code: -1, userInfo: [NSLocalizedDescriptionKey: "토큰 없음"])
        }
        setToken(accessToken)
    }

    func logout() { setToken(nil) }

    // MARK: - GET Activities (목록·상세)

    struct ActivitiesPage: Decodable {
        let content: [ActivityItem]
        let totalElements: Int
    }

    struct ActivityItem: Decodable {
        let id: Int
        let distance: Double
        let duration: Int
        let averagePace: Int?
        let calories: Int?
        let averageHeartRate: Int?
        let cadence: Int?
        let route: [RoutePoint]?
        let startedAt: String
        let memo: String?
        let createdAt: String?
    }

    struct RoutePoint: Decodable {
        let lat: Double
        let lng: Double
        let timestamp: String?
        enum CodingKeys: String, CodingKey { case lat, lng, timestamp }
        init(from decoder: Decoder) throws {
            let c = try decoder.container(keyedBy: CodingKeys.self)
            lat = try c.decode(Double.self, forKey: .lat)
            lng = try c.decode(Double.self, forKey: .lng)
            timestamp = try c.decodeIfPresent(String.self, forKey: .timestamp)
        }
    }

    func getActivities(page: Int = 0, size: Int = 20) async throws -> ActivitiesPage {
        guard let t = token else { throw NSError(domain: "APIClient", code: 401, userInfo: [NSLocalizedDescriptionKey: "로그인이 필요합니다"]) }
        var comp = URLComponents(string: "\(baseURL)/api/activities")!
        comp.queryItems = [URLQueryItem(name: "page", value: "\(page)"), URLQueryItem(name: "size", value: "\(size)")]
        var req = URLRequest(url: comp.url!)
        req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization")
        let (data, res) = try await session.data(for: req)
        guard let http = res as? HTTPURLResponse, http.statusCode == 200 else {
            throw NSError(domain: "APIClient", code: -1, userInfo: [NSLocalizedDescriptionKey: "목록 조회 실패"])
        }
        let decoder = JSONDecoder()
        return try decoder.decode(ActivitiesPage.self, from: data)
    }

    func getActivity(id: Int) async throws -> ActivityItem {
        guard let t = token else { throw NSError(domain: "APIClient", code: 401, userInfo: [NSLocalizedDescriptionKey: "로그인이 필요합니다"]) }
        var req = URLRequest(url: URL(string: "\(baseURL)/api/activities/\(id)")!)
        req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization")
        let (data, res) = try await session.data(for: req)
        guard let http = res as? HTTPURLResponse, http.statusCode == 200 else {
            throw NSError(domain: "APIClient", code: -1, userInfo: [NSLocalizedDescriptionKey: "상세 조회 실패"])
        }
        return try JSONDecoder().decode(ActivityItem.self, from: data)
    }

    // MARK: - POST Activity (완료 시 서버 저장)

    /// 러닝 완료 후 결과를 서버에 저장
    func postActivity(
        distance: Double,
        duration: Int,
        averagePace: Int?,
        calories: Int?,
        averageHeartRate: Int?,
        cadence: Int?,
        route: [[String: Any]]?,
        startedAt: Date,
        memo: String?
    ) async throws {
        guard let t = token else {
            throw NSError(domain: "APIClient", code: 401, userInfo: [NSLocalizedDescriptionKey: "로그인이 필요합니다"])
        }
        let url = URL(string: "\(baseURL)/api/activities")!
        var req = URLRequest(url: url)
        req.httpMethod = "POST"
        req.setValue("application/json", forHTTPHeaderField: "Content-Type")
        req.setValue("Bearer \(t)", forHTTPHeaderField: "Authorization")

        let formatter = ISO8601DateFormatter()
        formatter.formatOptions = [.withInternetDateTime]
        let startedAtStr = formatter.string(from: startedAt)

        var body: [String: Any] = [
            "distance": distance,
            "duration": duration,
            "startedAt": startedAtStr
        ]
        if let v = averagePace { body["averagePace"] = v }
        if let v = calories { body["calories"] = v }
        if let v = averageHeartRate { body["averageHeartRate"] = v }
        if let v = cadence { body["cadence"] = v }
        if let r = route { body["route"] = r }
        if let m = memo { body["memo"] = m }

        req.httpBody = try JSONSerialization.data(withJSONObject: body)

        let (_, res) = try await session.data(for: req)
        guard let http = res as? HTTPURLResponse, (200...299).contains(http.statusCode) else {
            throw NSError(domain: "APIClient", code: -1, userInfo: [NSLocalizedDescriptionKey: "활동 저장 실패"])
        }
    }
}
