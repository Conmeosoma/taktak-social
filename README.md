# Taktak

Ứng dụng Android chụp ảnh, gửi khoảnh khắc riêng tư cho bạn bè và hiển thị ảnh nhận mới nhất trên widget màn hình chính 2×2.

Nguồn đặc tả: [TAKTAK_PROJECT.md](docs/TAKTAK_PROJECT.md). Quyết định cố định: [PROJECT_DECISIONS.md](docs/PROJECT_DECISIONS.md). Quy tắc dành cho coding agent: [AGENTS.md](AGENTS.md).

## Trạng thái và phạm vi

Bước hiện tại: **AUTO CONTINUE trong Bước 0 và giai đoạn 1**, được người dùng duyệt ngày 16/10/2026. 0A PASS; 0B vẫn chờ đối chiếu Console. Phần độc lập 1A đã có server và 6 integration test PostgreSQL PASS. Android có ViewBinding, cấu hình mạng theo build type và đã chạy trên emulator; chưa coi toàn bộ giai đoạn 1 là DONE. Xem [trạng thái](docs/PROJECT_STATUS.md), [QA bootstrap](docs/QA_BOOTSTRAP.md) và [kế hoạch mạng](docs/NETWORK_TEST_PLAN.md).

Repository tận dụng Android skeleton có sẵn; `server/` hiện là Spring Boot/Maven project, `deploy/` chưa có cấu hình triển khai. JSON Firebase local giữ nguyên. Chưa tạo auth, camera, widget, tính năng nghiệp vụ hoặc tài nguyên Firebase mới.

MVP gồm tài khoản/hồ sơ, bạn bè/chặn, chụp ảnh vuông, caption tối đa 50 ký tự, gửi ảnh riêng tư, lịch sử Đã nhận/Đã gửi, widget 2×2, xóa/thu hồi và lưu ảnh theo yêu cầu. App có bốn tab Camera / Lịch sử / Bạn bè / Hồ sơ. Không thêm bảng tin công khai, video, chatbot AI/Gemini hoặc tính năng backlog vào MVP.

## Kiến trúc và công nghệ

Android dùng Activity/Fragment XML + ViewBinding, ViewModel/LiveData và Repository. Repository gọi API qua Retrofit/OkHttp; UI không chứa trực tiếp nghiệp vụ mạng. Backend phân lớp Controller → Service → Repository, sử dụng PostgreSQL và private file storage. Android kết nối backend qua HTTPS với JSON hoặc multipart.

| Thành phần | Công nghệ cố định |
|---|---|
| Android | Java, XML, ViewBinding, Material Components, minSdk 26; MVVM + Repository |
| Định danh Android | package/namespace/applicationId `com.taktak.app` |
| Build Android | Gradle Groovy, Gradle Wrapper |
| HTTP và ảnh | Retrofit, OkHttp, Gson, Glide |
| Camera và tác vụ nền | CameraX, WorkManager, AppWidgetProvider/RemoteViews |
| Lưu phiên | EncryptedSharedPreferences |
| Backend | Java 17, Spring Boot 3, Maven; package `com.taktak.server` |
| Dữ liệu và auth | Spring Security, JPA, Validation, PostgreSQL, Flyway, BCrypt, JWT access/refresh |
| Push | Firebase chỉ dùng FCM; Android Messaging SDK và server Admin SDK |
| Ảnh | File private trên VPS, mỗi lần tải qua API có xác thực và kiểm tra quyền |

Không sử dụng Kotlin source, Compose, Gradle Kotlin DSL, Firebase Auth, Firestore, Firebase Storage hoặc Cloud Functions. Các phiên bản đã build/test được ghi tại [PROJECT_DECISIONS.md](docs/PROJECT_DECISIONS.md); Java 17 là yêu cầu cố định của backend. Thư viện có thể có dependency Kotlin transitive; app không có file Kotlin hoặc plugin Kotlin do dự án khai báo.

FCM chỉ là tín hiệu để WorkManager đồng bộ từ API, không mang ảnh hay thông tin xác thực. Không bảo đảm widget cập nhật tức thì khi Android/OEM hạn chế tác vụ nền hoặc người dùng force-stop app.

## Cấu trúc repository

```text
taktak/
├── android-client/            # Project Gradle Android độc lập, có sẵn
├── server/                    # Spring Boot 3 / Maven / Java 17
├── deploy/                    # Cấu hình triển khai ở bước tương ứng, hiện rỗng
├── docs/
│   ├── TAKTAK_PROJECT.md       # Đặc tả và roadmap đầy đủ
│   └── PROJECT_DECISIONS.md   # Các quyết định đã khóa
├── AGENTS.md
├── .gitignore
└── README.md
```

Mở `android-client/` trong Android Studio để làm Android; mở `server/` trong IntelliJ/VS Code khi backend được tạo. Thư mục `taktak/` là Git root. Không chuyển `app/` ra Git root. Git không theo dõi thư mục rỗng.

## Roadmap

Quyền AUTO CONTINUE hiện cho phép xử lý từng tiểu bước trong 0–1 và các phần độc lập, không vượt qua điều kiện nghiệm thu chưa đạt. Các giai đoạn 2–8 vẫn là kế hoạch, chưa được phép thực hiện.

| Bước | Nội dung |
|---|---|
| 0A — PASS tài liệu/quy ước | Khóa định danh, quy ước repository, tài liệu và `.gitignore` |
| 0B — hiện tại, chờ Console | Đăng ký Firebase Android `com.taktak.app` chỉ dùng FCM; giữ cấu hình local |
| 0C | Chốt và kiểm tra kế hoạch kết nối emulator/máy thật |
| 1A / 1B / 1C | Server tối thiểu / Android tối thiểu / kiểm tra build và kết nối |
| 2A–2D | Auth và hồ sơ backend; kiểm thử API và quyền |
| 3A–3C, sau đó 3D | Android auth; chụp → upload ảnh chỉ mình tôi → tải lại bằng JWT |
| 4A–4C | Bạn bè và chặn ở server/client |
| 5A–5E | Camera, xem trước, nén, gửi; màn Đã nhận trước FCM/widget |
| 6A–6C | FCM, WorkManager và widget 2×2 |
| 7A–7C | Lịch sử Đã gửi, xóa/thu hồi, lưu ảnh |
| 8A–8C | VPS, HTTPS, backup/restore và nghiệm thu cuối |

## Chuẩn bị môi trường Windows

Các thao tác dưới đây là hướng dẫn chuẩn bị, không phải xác nhận công cụ đã được cài hoặc ứng dụng đã build.

1. Chuẩn bị Git for Windows và PowerShell. Mở terminal tại thư mục `taktak`.
2. Chuẩn bị JDK 17 cho backend; đặt `JAVA_HOME` trỏ tới JDK và thêm thư mục `bin` vào `PATH`. Chuẩn bị Maven cho bước 1A. Android dùng Gradle JDK tương thích AGP/Gradle được kiểm tra ở 1B, không mặc định ép Android dùng JDK 17.
3. Chuẩn bị Android Studio với SDK Manager, Platform-Tools và emulator hoặc thiết bị thật. Chọn SDK Platform/Build-Tools theo bộ phiên bản sẽ được khóa ở 1B; cấu hình Gradle JDK trong Android Studio tương ứng.
4. Với thiết bị thật, bật Developer options/USB debugging và cài driver USB phù hợp nếu cần. Khi đến bước kiểm tra thiết bị, thêm thư mục SDK `platform-tools` vào `PATH` để dùng `adb`.
5. Chuẩn bị PostgreSQL cho 1A/1C theo lựa chọn phiên bản tại giai đoạn 1. **Chưa tạo database, user hoặc chạy migration ở 0A.** Docker Compose là lựa chọn triển khai sau này, chưa bắt buộc.

Kiểm tra thủ công trong PowerShell sau khi chuẩn bị công cụ:

```powershell
git --version
java --version
mvn --version
adb version
adb devices
```

`java` và Java mà Maven báo phải là JDK 17 cho backend. `adb devices` chỉ liệt kê được emulator đang chạy hoặc thiết bị đã kết nối và cấp quyền. Máy hiện tại có Java trong PATH không nhất quán, nên dùng helper bên dưới thay vì đổi Java hệ thống.

## Build và chạy local trên Windows

Bộ công cụ của phiên này nằm trong `.tools/` (Git ignore): Temurin JDK 17 và Maven portable đã kiểm tra checksum; không được cài cùng source khi clone repo. Trên máy khác cần chuẩn bị JDK 17 cho server và JDK tương thích Gradle/AGP cho Android. Maven Wrapper tự tải Maven đã khóa.

Trong PowerShell tại Git root, cho phép script **chỉ trong process hiện tại**, rồi chọn JDK server:

```powershell
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
. .\scripts\Use-TaktakJava.ps1 -Target Server
Push-Location server
.\mvnw.cmd -B -ntp verify
Pop-Location
```

`verify` dùng PostgreSQL thật trong cluster test mới, không cần Docker hay DB credentials cá nhân; cluster test được bỏ khi kết thúc. Chạy server smoke với cluster test riêng trong terminal khác, nhấn Enter để dừng:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LocalSmokeServer.ps1 -Port 8080
```

Đây là app Spring Boot thật với database tạm, không phải backend giả. Muốn dùng database dev bền vững, chuẩn bị PostgreSQL riêng và thiết lập `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` theo `server/.env.example` rồi chạy `server/mvnw.cmd spring-boot:run` với JDK 17. Spring Boot không tự nạp `.env`; không ghi credential thật vào file tracked. Chưa tự tạo/reset database dev có sẵn.

Build Android trong terminal PowerShell đã cho phép script ở process hiện tại:

```powershell
. .\scripts\Use-TaktakJava.ps1 -Target Android
Push-Location android-client
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug
Pop-Location
```

Release yêu cầu `-PTAKTAK_RELEASE_BASE_URL` là URL HTTPS có dấu `/` cuối; thiếu URL hoặc dùng HTTP sẽ bị chặn. Build QA dùng `https://api.example.invalid/` để kiểm tra cấu hình, không phải backend thật, không phải bản sẵn sàng phát hành. Release chưa được ký. Xem [kế hoạch mạng và lệnh instrumentation](docs/NETWORK_TEST_PLAN.md) trước khi chạy `connectedDebugAndroidTest` vì test kết nối cần backend đang chạy.

### Kiểm tra đã xác minh

Các kiểm tra dưới đây đã được chạy trong workspace hiện tại; chúng không có nghĩa là toàn bộ MVP đã hoàn tất:

- Backend: `mvnw verify` PASS với PostgreSQL tạm thời, Flyway migration và 6 integration test.
- Android: `assembleDebug`, `assembleRelease`, `testDebugUnitTest`, `lintDebug` và `lintRelease` PASS.
- Emulator: 4 instrumentation test PASS khi dùng `adb reverse`; đường dẫn trực tiếp `10.0.2.2` vẫn cần điều tra thêm.
- UI bootstrap: cài APK debug, mở `MainActivity` và kiểm tra UI dump/screenshot PASS.
- Chưa nghiệm thu Firebase Console và thiết bị Android vật lý; chưa có auth, camera, upload, FCM hoặc widget hoàn chỉnh.

### Clone và bắt đầu làm việc

```powershell
git clone https://github.com/Conmeosoma/taktak-social.git
Set-Location taktak-social
```

Không tạo hoặc commit credential thật sau khi clone. Nếu cần Firebase cho Android, đặt `google-services.json` local tại `android-client/app/`; nếu cần Admin SDK, dùng đường dẫn credential qua biến môi trường ở server. Các file này đã bị `.gitignore` loại khỏi commit.

## Quy ước mạng và bí mật

- Debug local có thể dùng HTTP với cấu hình riêng debug và giới hạn đích local: emulator dùng `10.0.2.2`, máy thật dùng IP LAN của máy tính trên Wi-Fi tin cậy; có thể chọn tunnel HTTPS. `localhost` trên điện thoại là chính điện thoại. Cổng, bind address và firewall sẽ được xác minh tại 0C/1C.
- Release bắt buộc HTTPS, chặn cleartext; không đưa HTTP LAN/emulator vào cấu hình release. HTTP debug truyền cả JWT không mã hóa, chỉ thử trên mạng tin cậy; ưu tiên HTTPS tunnel.
- Không commit `.env`, mật khẩu, API key, JWT secret, token, keystore, Firebase Admin credentials hoặc `google-services.json`. `.env.example`/`.env.*.example` chỉ được chứa placeholder và sẽ được tạo ở bước cần thiết.
- `google-services.json` là cấu hình Android, khác với service-account JSON chứa private credentials của server. Cả hai được Git ignore theo chính sách repo; Admin credentials chỉ chuẩn bị ở bước 6.
- Giữ Gradle/Maven Wrapper và mã nguồn trong Git. `.gitignore` chỉ ngăn thêm file chưa được theo dõi; không tự loại bí mật đã được commit trước đó.

Nhánh chính hiện tại là `main`. Tên nhánh công việc dùng `feat/`, `fix/`, `docs/`, `chore/` với tên tiếng Anh dạng kebab-case. Không tự động commit, push hoặc deploy; chỉ thực hiện khi người dùng yêu cầu. Chỉ ghi PASS cho kiểm tra đã chạy và xác minh, không suy ra trạng thái toàn ứng dụng từ kiểm tra tài liệu.
