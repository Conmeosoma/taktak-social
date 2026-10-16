# Taktak

Taktak là ứng dụng Android giúp bạn chụp và gửi những khoảnh khắc riêng tư cho bạn bè. Ảnh được lưu trên máy chủ riêng và có thể hiển thị ảnh mới nhất trên widget 2×2 ở màn hình chính.

## Tính năng

- Chụp ảnh vuông và gửi cho một hoặc nhiều người bạn.
- Gửi ảnh riêng tư chỉ cho bản thân hoặc cho danh sách người nhận được chọn.
- Quản lý bạn bè, lời mời kết bạn và chặn người dùng.
- Xem lịch sử ảnh đã gửi và ảnh đã nhận.
- Caption tối đa 50 ký tự Unicode.
- Tải ảnh mới nhất về widget màn hình chính 2×2.
- Lưu ảnh vào thư viện khi người dùng yêu cầu.
- Xóa hoặc thu hồi ảnh theo quyền của người gửi.
- Nhận thông báo thay đổi qua Firebase Cloud Messaging.

## Công nghệ

### Android

- Java và XML, không dùng Kotlin hoặc Jetpack Compose.
- MVVM, Repository, ViewModel và LiveData.
- ViewBinding và Material Components.
- Retrofit, OkHttp, Gson và Glide.
- CameraX, WorkManager và AppWidgetProvider.
- EncryptedSharedPreferences để lưu phiên đăng nhập.
- `minSdk 26`.

Package và `applicationId`: `com.taktak.app`.

### Backend

- Java 17 và Spring Boot 3.
- Spring Security, JPA, Validation và Flyway.
- PostgreSQL.
- JWT access/refresh token và BCrypt.
- Ảnh được lưu private trên VPS, chỉ truy cập qua API có xác thực.

Package: `com.taktak.server`.

Firebase chỉ dùng cho Cloud Messaging. Dự án không sử dụng Firebase Auth, Firestore, Firebase Storage, Cloud Functions hoặc chatbot AI.

## Cấu trúc dự án

```text
taktak/
├── android-client/   Ứng dụng Android
├── server/           Backend Spring Boot
├── docs/              Tài liệu dự án và quyết định kỹ thuật
├── deploy/            Cấu hình triển khai
└── scripts/           Script hỗ trợ phát triển trên Windows
```

## Chạy local

### Android

Mở thư mục `android-client` bằng Android Studio, cài Android SDK phù hợp và chạy module `app` trên emulator hoặc thiết bị Android.

```powershell
Set-Location android-client
.\gradlew.bat assembleDebug
```

### Backend

Backend yêu cầu JDK 17 và PostgreSQL. Thiết lập các biến môi trường `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` và `JWT_ACCESS_SECRET` theo `server/.env.example`, sau đó chạy. JWT secret phải là Base64 của ít nhất 32 byte ngẫu nhiên; Spring Boot không tự nạp `.env`.

```powershell
Set-Location server
.\mvnw.cmd spring-boot:run
```

Endpoint kiểm tra trạng thái:

```text
GET http://localhost:8080/actuator/health
```

HTTP chỉ dành cho debug local. Môi trường release bắt buộc dùng HTTPS.

## Cấu hình Firebase

Android cần file `google-services.json` đặt tại `android-client/app/`. File này là cấu hình client và không được commit. Firebase Admin credentials của server cũng phải được cung cấp qua file local hoặc biến môi trường, không ghi trực tiếp vào source code.

## Tài liệu

- [Đặc tả dự án](docs/TAKTAK_PROJECT.md)
- [Quyết định kỹ thuật](docs/PROJECT_DECISIONS.md)
- [Trạng thái dự án](docs/PROJECT_STATUS.md)
- [Hướng dẫn kiểm thử](docs/QA_BOOTSTRAP.md)

## Bản quyền

Dự án đang được phát triển nội bộ.
