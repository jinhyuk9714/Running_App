# Running App (iOS)

나이키 러닝 앱처럼 **시작** → 실시간 GPS·심박수 기록 → **완료** 시 서버에 결과 저장, 지도에 이동 경로 표시.

---

## 요구 사항

- **Xcode 15+**, **iOS 17+**
- **백엔드**: https://jinhyuk-portfolio1.shop (또는 로컬 `http://localhost:8080`)
- **Capabilities**: Location (When In Use / Always), HealthKit (Heart Rate, Step Count)

---

## Xcode에서 열기

1. **Xcode**에서 `ios/RunningApp/RunningApp.xcodeproj` 열기
2. Swift 소스는 이미 앱 타깃에 포함됨 (로그인·러닝·기록·지도·API·GPS·HealthKit)
3. **Target** → **Signing & Capabilities**:
   - **+ Capability** → **Background Modes** → **Location updates** 체크
   - **+ Capability** → **HealthKit** 추가 (심박수·걸음 수용). **Clinical Health Records / Verifiable Health Records는 체크하지 마세요** — 무료 Apple ID에서는 지원되지 않아 프로비저닝 오류가 납니다.
4. **권한 설명 문구 (Info.plist / Target Info)**  
   iOS는 위치·건강 데이터 접근 전에 사용자에게 이유를 보여줍니다. 아래 키를 반드시 넣어야 권한 요청 시 앱이 거부되지 않습니다.

   - 왼쪽에서 **TARGETS** → **RunningApp** 선택
   - 상단 탭에서 **Info** (또는 **Build Settings** 옆 **Info**) 클릭
   - **Custom iOS Target Properties** 영역에서 **+** 버튼으로 아래 행을 하나씩 추가
   - **"Custom macOS Target Properties"만 보일 때**: 툴바에서 **Run 대상(Scheme)** 을 **iPhone 15** 등 iOS 시뮬레이터로 바꾼 뒤 Info 탭을 다시 보면 **Custom iOS Target Properties**가 보일 수 있습니다. 또는 Info 탭 안에 **iOS** / **macOS** 구간이 있으면 **iOS** 쪽에 아래 키들을 추가하면 됩니다.

   | Key (키)                                                      | Type   | Value (설명 문구)                             |
   | ------------------------------------------------------------- | ------ | --------------------------------------------- |
   | `Privacy - Location When In Use Usage Description`            | String | 러닝 경로 기록을 위해 위치 권한이 필요합니다. |
   | `Privacy - Location Always and When In Use Usage Description` | String | 백그라운드에서도 러닝 경로를 기록합니다.      |
   | `Privacy - Health Share Usage Description`                    | String | 심박수·케이던스를 기록에 반영합니다.          |
   | `Privacy - Health Update Usage Description`                   | String | 러닝 활동을 건강 앱에 저장합니다.             |

   **참고**: Xcode Info 탭에서는 위 표의 "Key"가 **Privacy - ...** 로 보입니다. Raw key는 각각 `NSLocationWhenInUseUsageDescription`, `NSLocationAlwaysAndWhenInUseUsageDescription`, `NSHealthShareUsageDescription`, `NSHealthUpdateUsageDescription` 입니다.  
   **직접 Info.plist 파일을 쓰는 경우**: 프로젝트에 `Info.plist` 파일이 있다면 그 파일을 열고 같은 키-값을 `<key>` / `<string>` 로 추가하면 됩니다.

5. **HealthKit 프레임워크 링크**  
   앱이 HealthKit(심박수, 걸음 수)을 쓰려면 빌드 시 HealthKit.framework를 링크해야 합니다.
   - 왼쪽에서 **TARGETS** → **RunningApp** 선택
   - 상단 탭에서 **Build Phases** 클릭
   - **Link Binary With Libraries** 섹션을 펼침
   - **+** 버튼 클릭 → 검색창에 `HealthKit` 입력 → **HealthKit.framework** 선택 → **Add**
   - 목록에 **HealthKit**이 보이면 완료 (필요 시 **Embed** 설정은 건드리지 않아도 됨)

---

## 실제 iPhone에서 실행하기

실기기에 앱을 설치·실행하려면 **Apple ID**(무료)만 있으면 됩니다. 유료 개발자 프로그램은 앱스토어 배포나 장기 설치 시 필요합니다.

### 1. Apple ID로 서명 설정

1. **Xcode** → **TARGETS** → **RunningApp** → **Signing & Capabilities**
2. **Team** 드롭다운에서:
   - **Add an Account...** 선택 → Apple ID 로그인 (없으면 **Create Apple ID**로 무료 가입)
   - 로그인 후 **Team**에 본인 Apple ID 팀이 보이면 선택
3. **Bundle Identifier**가 다른 앱과 겹치면 수정 (예: `com.본인이름.RunningApp`)
4. **Signing**이 **Automatically manage signing** 체크 상태인지 확인

### 2. iPhone 연결 및 실행

1. **iPhone**을 USB로 Mac에 연결
2. iPhone에서 **“이 컴퓨터를 신뢰하시겠습니까?”** → **신뢰** 선택 (필요 시 iPhone 잠금 해제)
3. Xcode 툴바 **Run 대상(디바이스)** 에서 **본인 iPhone** 선택 (시뮬레이터 대신)
4. **▶ Run** (또는 `Cmd + R`) 클릭 → 빌드 후 iPhone에 앱이 설치되고 실행됨

### 3. iPhone에서 “신뢰하지 않은 개발자” 해제

처음 설치 후 실행 시 **“신뢰할 수 없는 개발자”** 라고 나올 수 있습니다.

1. iPhone **설정** → **일반** → **VPN 및 기기 관리** (또는 **기기 관리**)
2. **개발자 앱** 아래에 본인 **Apple ID 이메일** 항목 선택
3. **“[이메일] 신뢰”** 탭 → **신뢰** 확인
4. 앱 아이콘 다시 눌러 실행

### 참고

- **무료 Apple ID**: 같은 Mac에서 빌드한 앱은 **약 7일** 후에 만료됩니다. 7일 지나면 Xcode에서 다시 Run 하면 됩니다.
- **유료 Apple Developer Program**(연 $99): 프로비저닝 1년, TestFlight·앱스토어 배포 가능.
- **위치·HealthKit**: 실기기에서만 제대로 동작합니다. 시뮬레이터에서는 위치는 시뮬레이션, HealthKit은 제한적입니다.

---

## 앱 동작

| 단계         | 동작                                                                                 |
| ------------ | ------------------------------------------------------------------------------------ |
| **로그인**   | 이메일/비밀번호 → `POST /api/auth/login` → JWT 저장                                  |
| **러닝 탭**  | **시작** → GPS·HealthKit 수집, 지도 실시간 경로 → **완료** 시 `POST /api/activities` |
| **기록 탭**  | `GET /api/activities` 목록 → 항목 탭 시 상세 + **지도에 저장된 경로 표시**           |
| **로그아웃** | 기록 탭 우측 상단 버튼                                                               |

---

## API 베이스 URL

- **실서비스**: `https://jinhyuk-portfolio1.shop`
- **로컬**: `http://localhost:8080` (시뮬레이터에서 Mac localhost 사용 시)

`APIClient.swift` 의 `baseURL` 을 환경에 맞게 수정하세요.
