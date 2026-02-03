import SwiftUI
#if canImport(UIKit)
import UIKit
#endif

struct ContentView: View {
    @State private var isLoggedIn = APIClient.shared.isLoggedIn()
    @State private var showingSignup = false
    @State private var email = ""
    @State private var password = ""
    @State private var nickname = ""
    @State private var loginError: String?
    @State private var isLoading = false

    var body: some View {
        Group {
            if isLoggedIn {
                mainTab
            } else if showingSignup {
                signupView
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
                #if canImport(UIKit)
                .keyboardType(UIKeyboardType.emailAddress)
                .autocapitalization(UITextAutocapitalizationType.none)
                #endif
                .padding()
                .background(textFieldBackground)
                .cornerRadius(8)
            SecureField("비밀번호", text: $password)
                .textContentType(.password)
                .padding()
                .background(textFieldBackground)
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
            Button(action: { showingSignup = true }) {
                Text("회원가입")
                    .font(.subheadline)
                    .foregroundColor(.blue)
            }
            .padding(.top, 8)
        }
        .padding(40)
        .alert("로그인 실패", isPresented: .constant(loginError != nil)) {
            Button("확인") { loginError = nil }
        } message: {
            if let msg = loginError { Text(msg) }
        }
    }

    private var signupView: some View {
        VStack(spacing: 20) {
            Text("회원가입")
                .font(.largeTitle)
            TextField("이메일", text: $email)
                .textContentType(.emailAddress)
                #if canImport(UIKit)
                .keyboardType(UIKeyboardType.emailAddress)
                .autocapitalization(UITextAutocapitalizationType.none)
                #endif
                .padding()
                .background(textFieldBackground)
                .cornerRadius(8)
            SecureField("비밀번호 (8자 이상)", text: $password)
                .textContentType(.newPassword)
                .padding()
                .background(textFieldBackground)
                .cornerRadius(8)
            TextField("닉네임 (2~20자)", text: $nickname)
                .textContentType(.username)
                .padding()
                .background(textFieldBackground)
                .cornerRadius(8)
            Button(action: signup) {
                if isLoading {
                    ProgressView().progressViewStyle(CircularProgressViewStyle(tint: .white))
                        .frame(maxWidth: .infinity).padding()
                } else {
                    Text("가입")
                        .frame(maxWidth: .infinity)
                        .padding()
                }
            }
            .background(Color.green)
            .foregroundColor(.white)
            .cornerRadius(8)
            .disabled(isLoading || email.isEmpty || password.isEmpty || nickname.isEmpty)
            if let err = loginError {
                Text(err).font(.caption).foregroundColor(.red)
            }
            Button(action: {
                showingSignup = false
                loginError = nil
            }) {
                Text("로그인으로 돌아가기")
                    .font(.subheadline)
                    .foregroundColor(.blue)
            }
            .padding(.top, 8)
        }
        .padding(40)
        .alert("회원가입 실패", isPresented: .constant(loginError != nil && showingSignup)) {
            Button("확인") { loginError = nil }
        } message: {
            if let msg = loginError { Text(msg) }
        }
    }

    private var textFieldBackground: Color {
        #if canImport(UIKit)
        return Color(UIColor.systemGray6)
        #else
        return Color(white: 0.96)
        #endif
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

    private func signup() {
        loginError = nil
        isLoading = true
        Task {
            do {
                try await APIClient.shared.signup(email: email, password: password, nickname: nickname)
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
