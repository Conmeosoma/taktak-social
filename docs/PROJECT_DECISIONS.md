# Taktak — Quyết định kỹ thuật đã khóa

Ngày ghi nhận: 16/10/2026. Phạm vi: Bước 0A.

Nguồn: [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md), đặc biệt các mục 5, 6, 9, 10 và 12; cùng yêu cầu Bước 0A của người dùng. Đây là quyết định thiết kế, không phải tuyên bố các thành phần đã được triển khai hoặc kiểm thử.

## Định danh và repository

| Quyết định | Giá trị đã khóa |
|---|---|
| Sản phẩm và repository | Taktak; một Git repository `taktak` |
| Android package/namespace/applicationId | `com.taktak.app` |
| Server package | `com.taktak.server` |
| Project Android | `android-client/`, build độc lập bằng Gradle Groovy |
| Project backend | `server/`, build độc lập bằng Maven |
| Tài liệu và triển khai | `docs/` và `deploy/` |
| Quy ước nhánh | Nhánh chính hiện tại `main`; công việc dùng `feat/`, `fix/`, `docs/`, `chore/` + tên tiếng Anh kebab-case |
| Đặt tên code | Class, method, biến bằng tiếng Anh; package con chữ thường theo chức năng |

Android package/applicationId phải khớp đăng ký Firebase. Không tùy tiện đổi sau khi tạo cấu hình Firebase. Mở `android-client/` trong Android Studio; Git root vẫn là `taktak/`.

## Stack và kiến trúc

- Android chỉ Java/XML, ViewBinding, Material Components, minSdk 26; không Kotlin, Compose hay Gradle Kotlin DSL.
- MVVM + Repository, ViewModel/LiveData; Retrofit/OkHttp/Gson cho API, Glide cho ảnh; CameraX, WorkManager, AppWidgetProvider/RemoteViews; EncryptedSharedPreferences cho phiên local.
- Server Java 17, Spring Boot 3, Maven, Spring Security, JPA, Validation. Controller → Service → Repository; PostgreSQL và Flyway cho dữ liệu/schema.
- Auth dùng BCrypt, JWT access ngắn hạn và refresh token xoay vòng; server chỉ lưu hash refresh token. Refresh không phụ thuộc access JWT còn hạn.
- Firebase chỉ dùng FCM; Messaging SDK phía Android và Firebase Admin SDK phía server phục vụ push. Không dùng Firebase Auth, Firestore, Firebase Storage, Cloud Functions; không chatbot AI/Gemini.
- Ảnh lưu private trên VPS, ngoài public/static/web root; API xác thực kiểm tra quyền sender/recipient mỗi lần tải. FCM chỉ báo thay đổi; không mang ảnh, caption hay thông tin xác thực.

## Chính sách mạng và cấu hình

- Debug được phép dùng HTTP local có kiểm soát, cấu hình riêng build debug, chỉ trên mạng tin cậy. Emulator dùng `10.0.2.2`, máy thật dùng IP LAN của máy tính hoặc tunnel HTTPS; kế hoạch thử cụ thể thuộc 0C.
- Release bắt buộc HTTPS, không cho phép cleartext; tách base URL theo build type, không đóng gói HTTP LAN/emulator trong release. Chính sách này được triển khai và kiểm chứng ở bước cấu hình Android, chưa sửa Manifest/Gradle trong 0A.
- Không hard-code secret/password/API key, không log token hoặc Authorization. Không commit `.env`, keystore, `google-services.json` hoặc Firebase Admin service-account JSON; cấu hình local qua môi trường/file được ignore.
- `google-services.json` là cấu hình Android, không phải Admin private key. Repo giữ local cả hai loại; Admin credentials không bao giờ đưa vào Android và chỉ chuẩn bị ở bước 6.
- File env mẫu chỉ dùng placeholder. Giữ Gradle/Maven Wrapper trong Git; không ignore toàn bộ JSON, properties hoặc YAML vì chúng có thể là mã nguồn/cấu hình dùng chung.

## Quy ước thực thi và những mục chưa khóa

Mỗi lần chỉ một tiểu bước được yêu cầu; bảo toàn file và thay đổi sẵn có; kiểm tra phù hợp rồi báo cáo bằng tiếng Việt, dừng chờ xác nhận. Không tự commit/push/deploy. Các mốc commit trong đặc tả không thay thế yêu cầu cho phép riêng của người dùng.

Giữ thứ tự kiểm thử sớm: 3D sau 3C (ảnh chỉ mình tôi, upload rồi tải lại có JWT); 5E Đã nhận trước FCM/widget. Bốn tab Camera / Lịch sử / Bạn bè / Hồ sơ, caption ≤50 ký tự Unicode, ảnh vuông và nén mục tiêu 500–800 KB theo đặc tả.

Phiên bản AGP, Gradle, JDK cho Android, compileSdk/targetSdk, thư viện và phiên bản cụ thể Spring Boot 3/PostgreSQL sẽ được kiểm tra tương thích, khóa và smoke test ở giai đoạn 1. Chưa chọn Firebase project ID, máy chủ, tên miền, cổng local hay systemd/Docker Compose ở 0A; không suy đoán các giá trị này từ file có sẵn.

Bước 0A chỉ khóa quy ước và tài liệu. Android skeleton/cấu hình FCM hiện có được bảo toàn, chưa được nghiệm thu trong bước này. Các bước 0B, 0C và giai đoạn 1 chỉ bắt đầu khi người dùng yêu cầu.

## Bổ sung giai đoạn 1 — 16/10/2026

Người dùng đã duyệt AUTO CONTINUE cho Bước 0 và giai đoạn 1, bao gồm phần độc lập khi bước khác bị chặn. Không đổi stack/package, không cho phép giai đoạn 2 hoặc commit/push/deploy.

| Thành phần | Phiên bản/config đã build kiểm tra |
|---|---|
| Server | Spring Boot 3.5.16, Temurin 17.0.20.1, Maven Wrapper 3.9.16 |
| Server test database | Zonky embedded-postgres 2.2.2 / PostgreSQL 14.22, test scope; không nằm trong production JAR |
| Android build | AGP 9.4.1, Gradle 9.6.0, JBR 25.0.3; source/target Java 17 |
| Android SDK | minSdk 26, compile/target 37, Build Tools 36.0.0 |
| UI | Activity 1.13.0 (không KTX), AppCompat 1.8.0, Material 1.14.0, ConstraintLayout 2.2.2 |
| Lifecycle/navigation | Lifecycle 2.11.0, ViewPager2 1.1.0, RecyclerView 1.4.0 |
| Networking/images | Retrofit/converter-gson 3.0.0, OkHttp 5.5.0, Gson 2.14.0, Glide 5.0.9 |
| Camera/background/session | CameraX 1.6.2, WorkManager 2.12.0, security-crypto 1.1.0 |
| Firebase client | Giữ BoM 35.0.1 + Messaging từ source có sẵn; tắt FCM auto-init cho tới bước 6 |

Phiên bản server phù hợp yêu cầu [Spring Boot 3.5](https://docs.spring.io/spring-boot/3.5/system-requirements.html); Gradle/SDK Android được đối chiếu [AGP 9.4](https://developer.android.com/build/releases/agp-9-4-0-release-notes) và build thực tế. Test database không khóa phiên bản PostgreSQL production tương lai; cần kiểm tra trên phiên bản VPS được chọn ở bước triển khai.

Debug HTTP chỉ cho `10.0.2.2`, `127.0.0.1`, `localhost` bằng network-security-config riêng debug; đích khác dùng HTTPS. Release chặn cleartext, không có URL mặc định và bắt buộc truyền URL HTTPS khi build. ADB reverse là phương án debug bổ sung đã chạy được trên emulator; không tuyên bố LAN/đường 10.0.2.2 đã PASS. Không thêm CAMERA permission hoặc triển khai camera/widget/auth trong giai đoạn 1.

Server chỉ mở GET `/actuator/health`; phần còn lại trả 401, chưa có user mặc định hoặc auth giả. Flyway clean bị tắt, migration V1 tạo 8 bảng; chưa có JPA entities nên `ddl-auto=validate` chưa thay thế kiểm thử schema SQL. JWT secret/FCM credential chưa sử dụng ở 1A; phải được kiểm tra bắt buộc khi triển khai tính năng tương ứng, không dùng secret rỗng cho auth.
