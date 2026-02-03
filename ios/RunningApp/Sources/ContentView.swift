import SwiftUI

struct ContentView: View {
    @State private var isLoggedIn = APIClient.shared.isLoggedIn()
    @State private var email = ""
    @State private var password = ""
    @State private var loginError: String?
    @State private var isLoading = false

    var body: some View {
        Group {
            if isLoggedIn {
                mainTab
            } else {
                loginView
            }
        }
        .onAppear { isLoggedIn = APIClient.shared.isLoggedIn() }
    }

    private var mainTab: some View {
        TabView {
            RunTrackingView()
                .tabItem { Label("러닝", systemImage: "figure.run") }
            ActivitiesListView(onLogout: { isLoggedIn = false })
                .tabItem { Label("기록", systemImage: "list.bullet.clipboard") }
        }
    }

    private var loginView: some View {
        VStack(spacing: 20) {
            Text("Running App")
                .font(.largeTitle)
            TextField("이메일", text: $email)
                .textContentType(.emailAddress)
                .keyboardType(.emailAddress)
                .autocapitalization(.none)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(8)
            SecureField("비밀번호", text: $password)
                .textContentType(.password)
                .padding()
                .background(Color(.systemGray6))
                .cornerRadius(8)
            Button(action: login) {
                if isLoading {
                    ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .frame(maxWidth: .infinity).padding()
                } else {
                    Text("로그인")
                        .frame(maxWidth: .infinity)
                        .padding()
                }
            }
            .background(Color.blue)
            .foregroundColor(.white)
            .cornerRadius(8)
            .disabled(isLoading || email.isEmpty || password.isEmpty)
            if let err = loginError {
                Text(err).font(.caption).foregroundColor(.red)
            }
        }
        .padding(40)
        .alert("로그인 실패", isPresented: .constant(loginError != nil)) {
            Button("확인") { loginError = nil }
        } message: {
            if let msg = loginError { Text(msg) }
        }
    }

    private func login() {
        loginError = nil
        isLoading = true
        Task {
            do {
                try await APIClient.shared.login(email: email, password: password)
                await MainActor.run { isLoggedIn = true; isLoading = false }
            } catch {
                await MainActor.run {
                    loginError = error.localizedDescription
                    isLoading = false
                }
            }
        }
    }
}
