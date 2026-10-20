---
title: "TAKTAK — TÀI LIỆU DỰ ÁN & ROADMAP VIBE CODE"
subtitle: "Ứng dụng Android chia sẻ khoảnh khắc riêng tư qua widget"
author: "Tài liệu triển khai — bản đặc tả 1.2 (roadmap có kiểm thử sớm)"
date: "15/10/2026"
lang: vi
---

> **Trạng thái:** Đây là tài liệu thiết kế và hướng dẫn triển khai, **chưa phải mã nguồn hoàn thành**. Khi thực thi, mỗi lần chỉ làm **một giai đoạn hoặc tiểu bước**, kiểm thử đạt, commit Git, rồi chờ người dùng xác nhận. Không tự động chuyển giai đoạn.

## 1. Mục tiêu và phạm vi

> Mở rộng được người dùng duyệt ngày 20/10/2026: MOMENT-REPLY-01 cho phép người
> nhận trả lời ảnh vào chat riêng 1–1 với chủ ảnh, không có bình luận công khai.
> Hợp đồng/quyền xem ảnh được ghi tại [CHAT_API.md](CHAT_API.md). Không đổi stack,
> không mở quyền ảnh qua chat và không tự triển khai production.

> Bổ sung được người dùng duyệt ngày 20/10/2026: triển khai CHAT-01–05 theo [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md) và [CHAT_API.md](CHAT_API.md). Module chat 1–1 giữa bạn bè mở rộng điều hướng thành Camera / Lịch sử / Tin nhắn / Bạn bè / Hồ sơ. Đây là ngoại lệ có phê duyệt đối với quy tắc bốn tab ở bản đặc tả gốc bên dưới; không cho phép chatbot AI, nhóm chat, public feed hoặc triển khai VPS tự động.

**Tên sản phẩm:** Taktak. **Nền tảng:** Android. **Ý tưởng:** người dùng chụp một khoảnh khắc, chọn nhóm bạn thân và gửi trực tiếp; ảnh mới nhất nhận được có thể xuất hiện trên widget 2×2 của bạn bè. Taktak ưu tiên riêng tư, sự đơn giản và cảm giác kết nối gần gũi.

### 1.1. Chức năng của MVP

1. Đăng ký, đăng nhập, xem/sửa hồ sơ; `username` duy nhất, có tên hiển thị và avatar.
2. Tìm bạn theo username; gửi/chấp nhận/từ chối lời mời; danh sách bạn, hủy kết bạn và chặn.
3. Mở app vào camera sau khi đăng nhập; CameraX; ảnh vuông 1:1; đổi camera, flash; caption tối đa **50 ký tự**.
4. Chọn tất cả hoặc một số bạn, nén ảnh mục tiêu **500–800 KB**, gửi có tiến trình và thử lại.
5. Widget 2×2 hiện ảnh mới nhất và tên người gửi; cập nhật nhờ FCM + WorkManager, có xử lý khi chưa đăng nhập; chạm mở app.
6. Lịch sử Đã nhận/Đã gửi; người gửi được xóa ảnh và thu hồi quyền xem ở máy nhận; người có quyền xem được lưu ảnh về máy.
7. Chế độ sáng/tối và các trạng thái màn hình rỗng, đang tải, mất mạng.

**Không thuộc MVP:** bảng tin công khai, like/bình luận công khai, live stream, video, filter/sticker nâng cao, tự xóa theo thời hạn, báo cáo nội dung. Những mục này đưa vào backlog để tránh nở phạm vi.

### 1.2. Quy ước trải nghiệm

- Sau khi xác thực thành công, `MainActivity` khởi động ở tab **Camera**.
- Điều hướng chính giữ đúng bốn tab theo tài liệu tham chiếu: **Camera / Lịch sử / Bạn bè / Hồ sơ**; không tự ý thêm tab thứ năm.
- Ảnh không có URL public; muốn xem phải gọi endpoint ảnh với JWT và được server cấp quyền.
- FCM chỉ là **tín hiệu có thay đổi**; không gửi ảnh, caption hay access token qua FCM.

## 2. Cách sử dụng tài liệu khi vibe code

Tài liệu tham chiếu 14 trang nhấn mạnh: làm bước nhỏ, chạy thử ngay, commit khi chạy được, cung cấp Logcat đầy đủ khi lỗi và không yêu cầu AI viết toàn app một lần. Taktak **kế thừa quy trình làm việc ấy**, nhưng thay toàn bộ backend Firebase bằng hệ thống do bạn tự vận hành.

**Quy trình cho từng giai đoạn:**

1. Dán *Prompt nền Taktak* (Mục 14) vào cuộc trò chuyện AI mới.
2. Dán duy nhất prompt của giai đoạn/tiểu bước cần làm; bắt đầu từ **Bước 0** trước khi code.
3. Yêu cầu đường dẫn file, code đầy đủ, lệnh chạy và kết quả mong đợi.
4. Tạo/sửa file, chạy thử trên máy và chụp log nếu lỗi.
5. Chỉ chuyển bước sau khi checklist đạt, sau đó `git add`, `git commit`.
6. Không đưa mật khẩu, JWT secret, database credentials hoặc private key vào cuộc trò chuyện AI hỗ trợ code, log hoặc Git repository.

**Quy ước mã:** Java và tên biến tiếng Anh; comment tiếng Việt ngắn gọn; Android XML + ViewBinding; MVVM + Repository; API trả JSON thống nhất; Java 17 ở server; đặt tính đúng và khả năng kiểm thử lên trước tối ưu sớm.

## 3. Thay đổi quan trọng so với PDF tham chiếu

| PDF Locket tham chiếu | Taktak thực hiện | Lý do |
|---|---|---|
| Firebase Authentication | Spring Security + BCrypt + JWT access/refresh | Tự kiểm soát tài khoản và phiên đăng nhập |
| Cloud Firestore | PostgreSQL + Spring Data JPA + Flyway | Dữ liệu quan hệ, chỉ mục và giao dịch rõ ràng |
| Firebase Storage/downloadUrl | File riêng trên VPS + endpoint xác thực | Không phát tán link ảnh có token/public |
| Cloud Functions Node.js | Spring Boot services + Firebase Admin SDK | Toàn bộ nghiệp vụ nằm ở backend Java |
| Firestore Security Rules | Authorization tại controller/service + kiểm thử | Server quyết định ai được xem/gửi/xóa ảnh |
| Firebase FCM lưu bên Firestore | `device_tokens` trong PostgreSQL | Hỗ trợ nhiều thiết bị và thu hồi token |
| Giai đoạn 8A/8B nâng cao | Backlog sau MVP | Ưu tiên luồng chụp → gửi → widget và quyền riêng tư |

**Nguồn tham chiếu:** `Roadmap_Vibe_Code_Locket_da_sua.pdf`, trang 1–14, đặc biệt quy tắc vibe code (trang 1), điều hướng và kiến trúc Android (trang 1–2), CameraX (trang 5), widget với WorkManager (trang 8), quyền riêng tư (trang 10–12), checklist (trang 13). Các lựa chọn liên quan server riêng ở tài liệu này là **thiết kế chuyển đổi theo yêu cầu Taktak**, không phải nội dung Firebase gốc.

## 4. Kiến trúc tổng thể

```text
┌────────────────────── Android Taktak (Java) ──────────────────────┐
│ Activity / Fragment (XML, ViewBinding, Material)                  │
│              ↓                       ↑ LiveData                  │
│          ViewModel                  Repository                   │
│                  ↓ Retrofit / OkHttp + JWT interceptor            │
│ CameraX · Glide · WorkManager · FCM · AppWidgetProvider           │
└─────────────────────────────┬─────────────────────────────────────┘
                              │ HTTPS / JSON / multipart
                              ▼
┌──────────────────────────── VPS Ubuntu ──────────────────────────┐
│ Nginx + TLS → Spring Boot 3 / Java 17                             │
│ Controllers → Services → Repositories → PostgreSQL                │
│                         ├─ Ảnh private trên ổ đĩa                 │
│                         └─ Firebase Admin SDK → FCM              │
└───────────────────────────────────────────────────────────────────┘
```

**Luồng phân quyền:** JWT dùng để nhận biết người gọi; mỗi tác vụ server còn phải xét tình trạng bạn bè, chặn, quyền sở hữu ảnh và quyền người nhận. Client không được tự tuyên bố `senderId` hay quyết định người nào được phép tải ảnh.

### 4.1. Luồng chụp và gửi ảnh

1. CameraX chụp JPEG vào cache → xử lý góc xoay, mirror camera trước → crop 1:1.
2. Client nén ảnh nhằm đạt 500–800 KB; giữ độ nét hợp lý; giới hạn đầu vào trước khi gửi.
3. Người dùng nhập caption ≤50 ký tự. Khi gửi cho bạn bè, chọn ít nhất một người nhận hợp lệ; `recipientIds=[]` được phép như chế độ **chỉ mình tôi** để kiểm thử và xem lại ảnh của chính chủ, không gửi FCM.
4. Retrofit gửi `multipart/form-data` tới `POST /api/moments` cùng `Authorization: Bearer ...`.
5. Server kiểm tra JWT, file thực tế, dung lượng, người nhận đang là bạn và hai chiều không bị chặn.
6. Server tạo ID ảnh, ghi vào đường dẫn private, commit bản ghi `moments` và `moment_recipients`.
7. Sau khi ghi thành công, server gửi FCM data-only chứa loại sự kiện và `momentId`.
8. Máy nhận trước tiên có thể xác minh ảnh qua màn **Đã nhận** (làm ở bước 5E); sau đó FCM đánh thức Worker gọi server lấy moment mới nhất, tải bitmap có xác thực và cập nhật widget.

**Lưu ý nhất quán:** khi file đã ghi nhưng giao dịch DB thất bại, server phải dọn file rác; khi FCM thất bại, **không** đảo ngược ảnh đã gửi. Moment chỉ mình tôi (`recipientIds=[]`) thuộc quyền xem của sender, không xuất hiện trong lịch sử Đã nhận của người khác. Ghi log kỹ thuật an toàn và cho phép Worker đồng bộ lại.

### 4.2. Luồng xóa ảnh

Người gửi gọi `DELETE /api/moments/{id}` → server xác thực chủ sở hữu → đánh dấu không còn truy cập được và xóa file an toàn → gửi FCM `moment_deleted` cho người nhận → mỗi Worker truy vấn `latest` để đưa ảnh hợp lệ kế tiếp lên widget. Nếu người nhận đang mất mạng, lần đồng bộ sau phải loại ảnh đã xóa. Cache ảnh private cần bị loại bỏ/không được hiển thị khi xác nhận xóa.

## 5. Công nghệ và nguyên tắc cấu hình

| Thành phần | Lựa chọn cố định |
|---|---|
| Android | Java; minSdk 26; XML + ViewBinding; Material Components |
| Client architecture | MVVM + Repository; ViewModel/LiveData |
| HTTP/ảnh | Retrofit, OkHttp, Gson, Glide; timeouts phù hợp |
| Máy ảnh/nền | CameraX; WorkManager; AppWidgetProvider/RemoteViews |
| Lưu phiên | EncryptedSharedPreferences; tự động refresh access token có khóa đồng bộ |
| Push | Firebase Cloud Messaging SDK (client), Firebase Admin SDK (server) |
| Server | Java 17; Spring Boot 3 Web, Security, Data JPA, Validation |
| Database | PostgreSQL; Flyway migration; index; UUID khóa chính |
| Auth | BCrypt; access JWT ngắn hạn; refresh token xoay vòng, chỉ lưu hash phía server |
| Ảnh | Ổ đĩa VPS private, không nằm trong `public/`, chỉ stream qua endpoint xác thực |
| Triển khai | Nginx, Let's Encrypt, systemd **hoặc** Docker Compose; backup định kỳ |

**Phiên bản thư viện:** khóa phiên bản sau khi kiểm tra tương thích Android Gradle Plugin, Gradle, JDK, Android SDK và Spring Boot; không trộn phiên bản theo từng câu trả lời AI. Giai đoạn 1 sẽ chốt một tập phiên bản nhất quán và chạy smoke test.

## 6. Cấu trúc repository và thư mục chuẩn

Đề xuất **một Git repository chứa hai project độc lập**, có thể mở từng phần bằng Android Studio và IntelliJ/VS Code:

```text
taktak/
├── android-client/
│   ├── app/
│   │   ├── src/main/
│   │   │   ├── java/com/taktak/app/
│   │   │   │   ├── ui/auth/        # LoginActivity, RegisterActivity
│   │   │   │   ├── ui/camera/      # CameraFragment, PreviewActivity
│   │   │   │   ├── ui/history/     # HistoryFragment, adapters
│   │   │   │   ├── ui/friends/     # FriendsFragment
│   │   │   │   ├── ui/profile/     # ProfileFragment
│   │   │   │   ├── data/model/     # DTO, request/response models
│   │   │   │   ├── data/remote/    # Retrofit interfaces, interceptors
│   │   │   │   ├── data/repository/
│   │   │   │   ├── viewmodel/
│   │   │   │   ├── widget/         # WidgetProvider, WidgetUpdateWorker
│   │   │   │   ├── service/        # FirebaseMessagingService
│   │   │   │   └── util/
│   │   │   ├── res/layout/        # XML màn hình và widget
│   │   │   ├── res/xml/           # widget provider info
│   │   │   └── AndroidManifest.xml
│   │   ├── build.gradle
│   │   └── google-services.json   # local, không đưa vào repo công khai
│   ├── settings.gradle
│   ├── build.gradle
│   ├── gradle.properties
│   └── gradle/wrapper/
├── server/
│   ├── pom.xml
│   ├── src/main/java/com/taktak/server/
│   │   ├── config/                # security, JWT, CORS nếu cần
│   │   ├── auth/                  # auth controller/service/DTO
│   │   ├── user/
│   │   ├── friend/
│   │   ├── moment/
│   │   ├── device/
│   │   ├── storage/               # private file storage
│   │   ├── notification/          # Firebase Admin
│   │   ├── common/                # lỗi, phân trang, validation
│   │   └── TaktakApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/migration/          # V1__init_schema.sql, ...
│   └── src/test/java/
├── deploy/
│   ├── nginx/taktak.conf.example
│   ├── docker-compose.yml        # nếu chọn Docker Compose
│   └── .env.example
├── docs/
│   └── TAKTAK_PROJECT.md
├── .gitignore
└── README.md
```

**Quyết định đã khóa từ Bước 0:** package Android và `applicationId` cùng là **`com.taktak.app`** khi đăng ký app trên Firebase; server dùng `com.taktak.server`. Android dùng **Gradle Groovy**, server dùng **Maven**. Không đổi package / `applicationId` tùy tiện sau khi tạo `google-services.json`; nếu đổi phải cập nhật đăng ký Firebase và cấu hình liên quan.

## 7. Mô hình dữ liệu PostgreSQL

Sáu bảng cốt lõi của Taktak gồm `users`, `friend_requests`, `friendships`, `moments`, `moment_recipients` và `device_tokens`. Để **chặn** và **refresh token xoay vòng** hoạt động đúng, bổ sung **hai bảng** `blocks` và `refresh_tokens`. Tổng cộng **8 bảng**, không có dữ liệu hội thoại AI.

| Bảng | Trường quan trọng | Ràng buộc / chỉ mục |
|---|---|---|
| `users` | `id`, `email`, `username`, `password_hash`, `display_name`, `avatar_path`, `created_at` | `UNIQUE(lower(email))`, `UNIQUE(username)`; username chuẩn hóa lowercase |
| `friend_requests` | `id`, `sender_id`, `receiver_id`, `status`, `created_at`, `responded_at` | Status pending/accepted/rejected; chống trùng pending cho cặp |
| `friendships` | `id`, `user_low_id`, `user_high_id`, `created_at` | `UNIQUE(user_low_id,user_high_id)`; sắp xếp UUID nhất quán |
| `blocks` **(bổ sung)** | `blocker_id`, `blocked_id`, `created_at` | PK kép; không tự chặn chính mình |
| `moments` | `id`, `sender_id`, `caption`, `file_path`, `mime_type`, `byte_size`, `created_at`, `deleted_at` | Index `(sender_id,created_at DESC)`; đường dẫn do server tạo |
| `moment_recipients` | `moment_id`, `user_id`, `created_at` | PK `(moment_id,user_id)`; **INDEX `(user_id,created_at DESC)`** |
| `device_tokens` | `id`, `user_id`, `fcm_token`, `device_id`, `updated_at` | Chống token trùng, hỗ trợ nhiều thiết bị |
| `refresh_tokens` **(bổ sung)** | `id`, `user_id`, `token_hash`, `expires_at`, `revoked_at`, `created_at` | Token raw không lưu plaintext; index user/expires |

**Chú ý về `moment_recipients.created_at`:** cột này phản chiếu thời gian moment được tạo để tối ưu truy vấn ảnh đến; phải ghi cùng giao dịch với `moments` và không cho client tự chỉnh. Khởi tạo toàn bộ schema bằng Flyway migration, không dựa vào `hibernate.ddl-auto=update` trên production.

**Các quy tắc nghiệp vụ bắt buộc:**

- Không kết bạn với chính mình; không có hai friendship ngược chiều cho cùng một cặp.
- Khi chặn, hủy friendship hiện có và vô hiệu yêu cầu kết bạn chưa xử lý giữa hai phía.
- Khi gửi ảnh cho bạn bè, server phải **kiểm tra toàn bộ recipientIds**: là bạn đang accepted, không bị chặn theo cả hai chiều, không trùng, không chứa chính mình; chỉ chấp nhận nếu tất cả hợp lệ (không âm thầm đổi danh sách người nhận). `recipientIds=[]` biểu thị moment **chỉ mình tôi**: chỉ sender xem được và không phát FCM.
- Chỉ sender hoặc recipient được phép lấy metadata/ảnh; chỉ sender được xóa.
- Kết quả `latest` chỉ lấy ảnh **chưa bị xóa** và người gọi còn được phép xem.

## 8. Hợp đồng API v1

**Base URL:** `https://api.ten-mien-cua-ban.vn` (placeholder, thay khi triển khai). **Base path:** `/api`. JSON dùng `camelCase`, timestamp ISO-8601 UTC; ID kiểu UUID chuỗi. Multipart chỉ dùng khi truyền file.

| Method | Endpoint | Mục đích / điểm lưu ý |
|---|---|---|
| POST | `/api/auth/register` | email, username, displayName, password; username duy nhất |
| POST | `/api/auth/login` | email + password → accessToken, refreshToken, expiresIn |
| POST | `/api/auth/refresh` | refreshToken → token mới, thu hồi token cũ |
| POST | `/api/auth/logout` **(đề xuất thêm)** | thu hồi refresh token/phiên hiện tại |
| GET / PUT | `/api/users/me` | xem/sửa displayName; thông tin riêng chỉ thuộc chính mình |
| POST | `/api/users/me/avatar` **(đề xuất thêm)** | avatar multipart, lưu private |
| GET | `/api/users/search?q=` | tìm username; ẩn người có quan hệ block |
| POST | `/api/friends/requests` | gửi lời mời với receiverId |
| PUT | `/api/friends/requests/{id}` | body action: `accept` hoặc `reject` |
| GET | `/api/friends` | danh sách bạn đã xác nhận |
| GET | `/api/friends/requests` **(đề xuất thêm)** | lời mời đến/chờ phản hồi |
| DELETE | `/api/friends/{id}` | hủy quan hệ bạn bè |
| POST | `/api/blocks/{userId}` | chặn và hủy kết bạn |
| DELETE | `/api/blocks/{userId}` **(đề xuất thêm)** | bỏ chặn |
| POST | `/api/moments` | multipart: `image`, `caption`, `recipientIds` lặp lại; `[]` = chỉ mình tôi; trả về `id` |
| GET | `/api/moments/latest` | moment nhận được mới nhất hoặc 204 nếu chưa có |
| GET | `/api/moments?type=received\|sent&page=0` | phân trang, mới nhất trước |
| GET | `/api/moments/{id}/image` | stream image **cần JWT**, kiểm tra quyền từng lần |
| DELETE | `/api/moments/{id}` | chỉ sender; phát sự kiện widget làm mới |
| POST | `/api/devices` | đăng ký/cập nhật FCM token cho thiết bị |
| DELETE | `/api/devices/current` **(đề xuất thêm)** | ngừng nhận FCM khi logout |

**Ngoại lệ xác thực hợp lý:** `/api/auth/register` và `/api/auth/login` mở công khai; `/api/auth/refresh` **không yêu cầu access JWT còn hạn**, nhưng buộc gửi refresh token hợp lệ và xoay vòng; `/api/auth/logout` có thể nhận refresh token để thu hồi phiên. Các endpoint còn lại yêu cầu JWT hợp lệ.

**Mẫu lỗi thống nhất:**

```json
{
  "timestamp": "2026-10-15T12:00:00Z",
  "status": 403,
  "code": "MOMENT_FORBIDDEN",
  "message": "Bạn không có quyền xem ảnh này.",
  "path": "/api/moments/xxx/image"
}
```

**Mã trạng thái chính:** 400 validation, 401 thiếu/hết hạn token, 403 không có quyền, 404 không tìm thấy hoặc che giấu tài nguyên private, 409 username trùng/lời mời trùng, 413 file quá lớn, 415 sai định dạng, 429 rate limit, 500 lỗi server và 503/504 dịch vụ tạm thời không sẵn sàng hoặc timeout. Client hiển thị thông điệp an toàn và có nút Thử lại khi phù hợp.

## 9. Roadmap — Bước 0 + 8 giai đoạn, làm lần lượt

| Giai đoạn | Mục tiêu | Thời gian tham khảo | Hoàn thành khi |
|---|---|---:|---|
| **0 (chuẩn bị)** | Git, Firebase FCM, khóa `com.taktak.app`, chuẩn bị mạng debug | 0,5–1 ngày | Firebase có Android app đúng package; kế hoạch kết nối emulator/máy thật rõ ràng |
| **1** | 2 project + cấu trúc thư mục + cấu hình | 1–2 ngày | Android build; server health OK; DB/Flyway chạy |
| **2** | Server đăng ký/đăng nhập JWT + hồ sơ | 3–4 ngày | Curl register/login/refresh/me thành công; test quyền |
| **3** | Android auth; **3D: chụp → tự upload → tải lại có JWT** | 4–6 ngày | Luồng ảnh riêng tư chạy từ Android đến VPS/local server và trở lại |
| **4** | Bạn bè server + client | 4–6 ngày | Hai tài khoản tìm, mời, chấp nhận, hủy, chặn đúng |
| **5** | Camera + nén + gửi ảnh; **5E: màn Đã nhận** | 4–7 ngày | A gửi B, B xem được ở Đã nhận, C bị chặn truy cập |
| **6** | FCM + widget 2×2 | 4–6 ngày | Máy B có thể cập nhật ảnh mới trên widget khi app không mở* |
| **7** | Lịch sử Đã gửi, xóa, lưu ảnh | 2–4 ngày | Xóa ảnh A, B không tải lại được, widget đồng bộ |
| **8** | VPS HTTPS, backup, audit, demo | 3–6 ngày | Triển khai có TLS, khôi phục backup thử, checklist nghiệm thu đạt |

**Tổng ước tính:** khoảng **26–42 ngày làm việc** (tham khảo, không phải cam kết); phần camera/upload được kéo sớm sang 3D rồi tái sử dụng, không viết lại ở giai đoạn 5. *Android/FCM có giới hạn hệ thống: khi người dùng force-stop app, chính sách pin hoặc OEM chặn background, không bảo đảm cập nhật ngay tức thì.*

### Bước 0 — Chuẩn bị trước khi code (làm đầu tiên)

**Tiểu bước 0A — Khóa định danh:** tạo repository `taktak` (khuyến nghị Private), chốt Android package và `applicationId` **`com.taktak.app`**; backend package `com.taktak.server`. Quy ước đặt tên package, nhánh Git và Gradle Groovy. Chưa viết nghiệp vụ.

**Tiểu bước 0B — Firebase Cloud Messaging:** tạo Firebase project Taktak, **Add app → Android** với package chính xác `com.taktak.app`; nhận `google-services.json`. Khi `android-client/app/` được tạo ở 1B thì đưa JSON vào đúng vị trí. Firebase chỉ dùng **FCM**, không bật Firebase Auth/Firestore/Storage/Cloud Functions. Credentials Firebase Admin dành cho server tạo ở bước 6 và phải giữ riêng, không đưa lên Git.

**Tiểu bước 0C — Chốt kịch bản thử máy thật:** Android Emulator gọi máy host bằng `http://10.0.2.2:<port>/`; điện thoại Android thật gọi bằng `http://<IP-LAN-của-máy-tính>:<port>/` **chỉ để debug trên Wi-Fi tin cậy**, hoặc dùng tunnel HTTPS. Kiểm tra thiết bị và máy tính cùng mạng, firewall cho phép đúng port, server lắng nghe địa chỉ phù hợp. Không dùng `localhost` trên điện thoại để trỏ tới server máy tính.

**Quy tắc HTTP rõ ràng:** bản **debug** có thể dùng network-security/manifest override cho phép cleartext HTTP tới môi trường local; **release** tắt cleartext và bắt buộc URL `https://`. Cấu hình khác nhau bằng build type và `BuildConfig`/Gradle, không hard-code `10.0.2.2` vào release. **HTTP debug vẫn truyền JWT không mã hóa**, nên chỉ thử trên mạng tin cậy; ưu tiên HTTPS tunnel ngay khi có thể.

**Checklist DONE Bước 0:** Firebase Console hiển thị Android app `com.taktak.app`; lưu JSON ngoài repo hoặc theo chính sách Git đã chốt; đã chọn cách kết nối emulator và máy thật; ghi ngắn vào `README.md`/checklist dự án. **Dừng để xác nhận rồi mới sang 1A.**

### Giai đoạn 1 — Khởi tạo cấu trúc và cấu hình (bước sẽ triển khai đầu tiên)

**Đầu ra bắt buộc:** repo `taktak`, project `android-client` Java XML, project `server` Spring Boot 3 Java 17, chuẩn hóa package, `.gitignore`, `.env.example`, cấu hình môi trường local và hướng dẫn chạy.

**Tiểu bước 1A — Server tối thiểu**

- Tạo Maven Spring Boot 3 với Web, Security, JPA, Validation, PostgreSQL Driver, Flyway (+ driver PostgreSQL cho Flyway), Actuator, Test; chuẩn Java 17.
- `/actuator/health` hoạt động ở local. Cấu hình `application.yml` lấy thông số DB, JWT secret, storage và FCM credentials từ biến môi trường; không hard-code bí mật.
- Tạo migration V1 các bảng cần thiết; xác định chiến lược `ddl-auto=validate` sau migration. Không triển khai chức năng auth ở bước này.

**Tiểu bước 1B — Android tối thiểu**

- Tạo Empty Views Activity **Java**, `minSdk=26`, `compileSdk/targetSdk` phù hợp; bật `viewBinding`. `namespace`/`applicationId` phải khớp **`com.taktak.app`** đã đăng ký Firebase ở Bước 0; đặt `google-services.json` đúng `android-client/app/`.
- Khai báo các dependency đã khóa version: Material, Lifecycle, Retrofit, OkHttp, Gson, Glide, CameraX, WorkManager, Firebase Messaging, security-crypto, ViewPager2/RecyclerView.
- Màn hình placeholder được mở; chỉ thêm quyền `INTERNET`/`CAMERA` vào thời điểm tính năng thực sự dùng; thiết lập `usesCleartextTraffic=false` cho bản release.
- Định nghĩa `BASE_URL` **riêng cho debug/release**; emulator dùng `10.0.2.2`, máy thật dùng IP LAN/tunnel HTTPS. Tạo cấu hình cleartext **chỉ cho debug** (`src/debug/AndroidManifest.xml` hoặc `network_security_config` riêng debug); release `usesCleartextTraffic=false` và chỉ HTTPS. Không vô tình đưa HTTP LAN vào APK release.

**Tiểu bước 1C — Kết nối và kiểm tra**

- Chạy PostgreSQL local, `mvn test` và `mvn spring-boot:run`; kiểm tra migration đã tạo bảng và health endpoint trả OK.
- Kiểm tra từ emulator qua `10.0.2.2` và từ máy thật qua LAN/tunnel tới `/actuator/health` (có thể làm bằng browser trước, Retrofit ở bước sau). Nếu máy thật không tới được, kiểm tra bind address và firewall, không đổi package để chữa lỗi mạng.
- Android `gradlew assembleDebug`, cài app, mở và không crash; chuẩn bị một smoke request tới backend ở bước sau.
- Tạo `README.md` với bước chạy cho Windows; commit `chore: bootstrap taktak android and server`.

**Cấu hình mẫu (chỉ là tên biến, không phải secret):**

```dotenv
DB_URL=jdbc:postgresql://localhost:5432/taktak
DB_USERNAME=taktak_dev
DB_PASSWORD=<set-locally>
JWT_ACCESS_SECRET=<base64-random-at-least-32-bytes>
JWT_ACCESS_MINUTES=15
JWT_REFRESH_DAYS=30
IMAGE_STORAGE_ROOT=/var/lib/taktak/private-images
GOOGLE_APPLICATION_CREDENTIALS=/opt/taktak/secrets/firebase-service-account.json
```

**Cảnh báo:** `.env`, Firebase Admin service-account JSON, `google-services.json` trong repo public, token và khóa API không được commit. File `google-services.json` phía Android là config FCM, **không** phải Firebase Admin secret; xử lý theo chính sách repo và không nhầm hai loại file.

**Checklist dừng ở giai đoạn 1:** ① Android Sync/Build thành công và nhận đúng `google-services.json`; ② server khởi động không lỗi; ③ Postgres/Flyway thành công; ④ kiểm tra `.gitignore`; ⑤ có lệnh khởi động trong README; ⑥ đã thử truy cập health từ emulator và máy thật. **Chưa làm login, bạn bè hay camera ở giai đoạn này.**

### Giai đoạn 2 — Server: đăng ký, đăng nhập, refresh JWT và hồ sơ

**Tiểu bước 2A:** Entity/repository `users`, BCrypt; `POST /auth/register` chuẩn hóa username (`a-z`, `0-9`, `_`), unique ở **DB** để chống race condition; validate email/password; transaction đúng.

**Tiểu bước 2B:** `POST /auth/login` và cấu hình Spring Security, JWT access 15 phút; `POST /auth/refresh` có refresh token ngẫu nhiên, hash trong DB và rotation, `/auth/logout` để thu hồi phiên. Giới hạn thử đăng nhập theo IP/tài khoản.

**Tiểu bước 2C:** `GET/PUT /users/me`, chỉ cho tự sửa `displayName`; DTO không trả `passwordHash`, token hash hoặc bí mật khác. Upload avatar làm khi phần media đã ổn hoặc triển khai endpoint riêng được xác thực.

**Tiểu bước 2D:** Integration test + curl cho register, đăng nhập, refresh, JWT hết hạn, JWT giả, username trùng; chọn format lỗi thống nhất trước khi viết client.

**Nghiệm thu:** đăng ký → login → gọi `/me` → refresh → logout; tài khoản A không xem dữ liệu private của B; gọi API không token nhận 401; việc trùng username được DB chặn thực sự.

### Giai đoạn 3 — Client: auth nối server

**Tiểu bước 3A:** XML LoginActivity/RegisterActivity bằng Material TextInputLayout, validation, ViewBinding. Nút đăng nhập/đăng ký có trạng thái loading, không bấm liên tục.

**Tiểu bước 3B:** `AuthApi`, `AuthRepository`, `AuthViewModel`, `ApiClient`; Gson models khớp DTO, lưu token bằng EncryptedSharedPreferences; OkHttp interceptor gắn access JWT; authenticator refresh **một lần có đồng bộ**, tránh nhiều request refresh đồng thời/vòng lặp 401.

**Tiểu bước 3C:** Splash/launcher route vào Camera sau khi phiên hợp lệ, xử lý logout xóa token + đăng ký thiết bị ở bước 6. Lỗi mạng/timeout/validation hiện ở UI, tuyệt đối không để crash.

**Nghiệm thu auth:** đăng nhập từ Android thật, đóng/mở app còn phiên, sai mật khẩu có lỗi rõ, ngắt mạng UI báo lỗi, token invalid không tạo vòng lặp request.

**Tiểu bước 3D — Lát cắt dọc ảnh đầu cuối (làm sau 3C, trước giai đoạn 4)**

- Thêm CameraX tối thiểu để **chụp một ảnh**; chưa cần UI chọn bạn, tab hoàn chỉnh, flash hay đa camera.
- Dùng chính endpoint `POST /api/moments` (không tạo `/self-test` public): upload multipart với `recipientIds=[]`, server hiểu là **moment chỉ mình tôi**. Dùng JWT của tài khoản đăng nhập, lưu ảnh private trên disk và metadata vào `moments`; bảng `moment_recipients` không cần dòng nào cho ảnh này; không gửi FCM.
- API trả `momentId`/`id`; client lập tức gọi `GET /api/moments/{id}/image` với JWT và **hiển thị file tải ngược về**. Không dùng ảnh local để giả lập kết quả.
- Kiểm chứng sai token → 401; tài khoản B thử tải ID ảnh của A → 403/404; không có JWT → 401; mất mạng/retry không để file mồ côi hoặc tạo bản ghi nửa chừng. Có thể dùng curl trước để phân biệt lỗi server và Android.
- Code CameraX/Retrofit/Storage service viết theo cấu trúc chính thức để **tái sử dụng** ở 5A–5D; không thêm một backend giả hay thư mục public chỉ để demo.

**Checklist DONE 3D:** ảnh chụp trên máy Android A → upload qua API được xác thực → server lưu vào private storage → client tải **lại từ server** và hiển thị đúng ảnh; B không xem được. Đây là mốc xác minh kiến trúc, chưa phải tính năng gửi bạn bè hoàn chỉnh.

### Giai đoạn 4 — Bạn bè: server + client

**Tiểu bước 4A:** Server search username và friend requests; check người chặn hai chiều, không gửi chính mình/đơn trùng; giới hạn tần suất tìm kiếm để giảm dò tài khoản.

**Tiểu bước 4B:** Chấp nhận/từ chối, chuyển thành friendship cặp chuẩn hóa có transaction; xóa bạn; chặn và tự xóa quan hệ. API phải kiểm tra request thuộc người nhận trước khi cập nhật.

**Tiểu bước 4C:** `FriendsFragment` có ô tìm, danh sách lời mời đến và danh sách bạn; avatar bằng Glide; empty/loading/error; gọi API thật qua Repository/ViewModel.

**Nghiệm thu:** hai tài khoản A/B trên hai thiết bị, gửi mời và đồng ý; C không bị xem như bạn; A chặn B thì B không thể gửi mời hoặc gửi ảnh cho A kể cả gọi API thủ công.

### Giai đoạn 5 — Camera, xem trước, nén và gửi ảnh

**Tiểu bước 5A:** Tái sử dụng CameraX từ 3D; hoàn thiện `MainActivity` với ViewPager2 + BottomNavigationView bốn tab; `CameraFragment` PreviewView, xin quyền ở runtime, đổi camera/flash, xử lý lifecycle và xoay ảnh.

**Tiểu bước 5B:** Chụp cache, crop 1:1, mirror camera trước khi cần; màn Preview: chụp lại, caption tối đa 50 ký tự, chọn Tất cả hoặc nhiều bạn (mặc định tất cả).

**Tiểu bước 5C:** Hoàn thiện dịch vụ lưu ảnh private/API đã chạy ở 3D để hỗ trợ người nhận thật; nén ảnh mục tiêu 500–800 KB bằng điều chỉnh cạnh/quality có giới hạn; server giới hạn multipart (ví dụ 5 MB), kiểm tra magic bytes/MIME, loại bỏ EXIF nhạy cảm, sinh tên file. `POST /moments` validate người nhận và lưu file private.

**Tiểu bước 5D:** Progress và retry upload trong UI, idempotency key để chống gửi trùng khi retry timeout (đề xuất); lỗi mất mạng cho retry, không tạo bản ghi nửa chừng.

**Tiểu bước 5E — Màn lịch sử Đã nhận (chuyển sớm từ 7A):** Tạo `HistoryFragment` phần **Đã nhận** bằng RecyclerView + ViewModel/Repository. Gọi `GET /api/moments?type=received&page=0`, ảnh thumbnail/full-size đều tải bằng JWT; hiển thị sender, caption, thời gian, loading/error/empty. Không cần FCM hay widget để kiểm tra dữ liệu. Ảnh chỉ mình tôi (`recipientIds=[]`) không xuất hiện trong tab Đã nhận.

**Nghiệm thu:** A gửi cho B, **B thấy ảnh tại Đã nhận** ngay khi tải lại màn hình; C không được tải ảnh của B; đổi camera không đảo/nghiêng ảnh; input rỗng hoặc sai định dạng bị từ chối; mất mạng có thể thử lại. **Chỉ sau khi đạt mới làm FCM/widget.**

### Giai đoạn 6 — FCM và widget cập nhật

**Tiểu bước 6A:** Dùng lại Firebase project/app đã đăng ký ở **Bước 0**; client lấy FCM token và đăng ký `POST /api/devices` sau login; server Firebase Admin SDK đọc credentials server-side từ file/env riêng; hỗ trợ nhiều thiết bị một tài khoản.

**Tiểu bước 6B:** Server phát **data message nhỏ** `{type: "moment_created", momentId: "..."}` hoặc `{type: "moment_deleted", momentId: "..."}`; **không** gửi image URL, caption, bearer token trong push. Xóa token FCM hết hạn khi Admin SDK báo không đăng ký.

**Tiểu bước 6C:** Widget 2×2 dùng RemoteViews + AppWidgetProvider. `onUpdate` **chỉ enqueue WorkManager**, không làm network trực tiếp; Worker gọi `/moments/latest`, tải ảnh qua HTTP có JWT, downsample bitmap phù hợp, cập nhật tên. FCM nhận sự kiện thì enqueue Worker; periodic worker dùng làm dự phòng theo giới hạn hệ thống. Nhấn widget mở app.

**Nghiệm thu:** khi không mở app (không force-stop), A gửi thì B thấy ảnh mới trên widget; nhiều widget hoặc nhiều thiết bị vẫn cập nhật; logout hiện trạng thái yêu cầu đăng nhập; khi không có ảnh, widget hiện placeholder.

**Giới hạn Android phải trình bày:** không thể đảm bảo widget cập nhật lập tức trong mọi tình huống do chính sách nền, tiết kiệm pin, lực dừng ứng dụng và OEM. WorkManager periodic không chạy chính xác theo phút.

### Giai đoạn 7 — Hoàn thiện lịch sử Đã gửi, xóa và lưu ảnh

**Tiểu bước 7A:** Mở rộng `HistoryFragment` đã có tab Đã nhận ở 5E: thêm **Đã gửi**, phân trang và sort mới nhất; ảnh tải qua endpoint có xác thực; full-screen xem caption và sender.

**Tiểu bước 7B:** Chỉ sender gọi DELETE; xóa phải thu hồi quyền xem **ngay trên server** và phát FCM để widget truy vấn lại ảnh thay thế; client cũng vô hiệu cache hiển thị theo trạng thái xóa. Kiểm thử ở trạng thái offline.

**Tiểu bước 7C:** Lưu ảnh về máy bằng Android MediaStore, yêu cầu quyền theo phiên bản Android và user action; ảnh private không được tự động xuất ra gallery khi chưa có thao tác.

**Nghiệm thu:** A xóa ảnh, B không gọi lại được endpoint ảnh (404/403), ảnh không còn trong lịch sử sau đồng bộ, widget hiển thị ảnh kế tiếp hoặc placeholder; tải ảnh lưu được vào máy khi người dùng yêu cầu.

### Giai đoạn 8 — Triển khai VPS và kiểm thử cuối

**Tiểu bước 8A:** Ubuntu VPS; tạo user dịch vụ không phải root; PostgreSQL chỉ lắng nghe local/private network; private image dir ngoài Nginx public root; `application-prod.yml` lấy secrets từ môi trường; chạy qua systemd **hoặc** Docker Compose; healthcheck.

**Tiểu bước 8B:** Nginx reverse proxy tới Spring Boot, TLS Let's Encrypt, HTTP→HTTPS, giới hạn upload/request rate, timeout hợp lý; firewall/UFW, kiểm tra quyền thư mục, log rotation và backup PostgreSQL + ảnh + cấu hình khôi phục.

**Tiểu bước 8C:** Kiểm thử trên máy thật, hai tài khoản/hai thiết bị, một tài khoản/hai thiết bị; thử mất mạng, token hết hạn, giới hạn đăng nhập, block, xóa ảnh, widget; kiểm tra quyền truy cập ảnh bằng curl. Viết README triển khai và quy trình update/rollback.

**Nghiệm thu:** endpoint HTTPS dùng được; không có tài nguyên ảnh public; backup có thử restore; APK release không chứa JWT secret, mật khẩu DB hay thông tin bí mật của server; có video demo và checklist các ca đã chạy.

## 10. An toàn bảo mật — không được hoãn đến giai đoạn 8

1. **JWT:** access token ngắn hạn; refresh token có thời hạn dài hơn, raw token chỉ gửi cho client, server lưu hash và hỗ trợ rotation + revoke. Kiểm tra issuer/expiry/chữ ký, chặn JWT sai.
2. **Mật khẩu:** BCrypt; không log body password; không trả hash qua bất kỳ DTO nào.
3. **Phân quyền ảnh:** xét quan hệ sender/recipient từ PostgreSQL **mỗi khi GET ảnh**; file chỉ nằm trong private storage, không `static/`, không mapping Nginx public.
4. **Input:** validate username/caption; giới hạn kích thước ảnh và số recipient; kiểm tra MIME thực tế, không chỉ extension; tên/path server tự sinh để ngăn path traversal.
5. **Friends/blocks:** kiểm tra server-side cả hai chiều; `search` cũng phải tôn trọng block. Client-side lọc chỉ là trải nghiệm phụ.
6. **FCM:** token thiết bị không phải thông tin xác thực; dữ liệu push không được cấp quyền tải ảnh; Worker vẫn gửi JWT. Firebase project được đăng ký ở Bước 0, Admin credentials không nằm trong Android.
7. **Android:** EncryptedSharedPreferences cho token; tránh log Authorization; **HTTP chỉ dành cho local debug**, release buộc HTTPS và không cho phép cleartext; khi logout xóa phiên local + device registration.
8. **VPS:** quyền thư mục tối thiểu, secrets qua env/file root-only, DB backup encrypted, HTTPS bắt buộc khi public.
9. **Xóa:** người gửi xóa phải khiến các yêu cầu ảnh mới bị từ chối ngay; đồng bộ cache/widget có thể trễ vì mạng hoặc Android, phải có cơ chế làm mới sau đó.

## 11. Bộ kiểm thử MVP — checklist thực hành

| ID | Tình huống | Kết quả mong đợi |
|---|---|---|
| AUTH-01 | Đăng ký username trùng bằng hai request đồng thời | Chỉ một request thành công, không có hai user trùng |
| AUTH-02 | Access JWT hết hạn nhưng refresh hợp lệ | Refresh một lần và gọi lại thành công |
| AUTH-03 | Dùng refresh token đã xoay vòng/thu hồi | 401, không cấp token mới |
| AUTH-04 | Dùng JWT sai chữ ký | 401 |
| SETUP-01 | Package app so với Firebase | `applicationId=com.taktak.app` khớp `google-services.json` |
| NETWORK-01 | Emulator/máy thật truy cập health | Emulator dùng `10.0.2.2`; máy thật dùng LAN/tunnel; release không HTTP |
| SELF-01 | 3D: chụp và upload `recipientIds=[]`, rồi GET ảnh | A thấy đúng ảnh tải từ server với JWT; B không được xem |
| FRIEND-01 | A mời B, B chấp nhận | Xuất hiện một friendship duy nhất |
| FRIEND-02 | A chặn B; B tìm/mời/gửi ảnh | Server từ chối/ẩn theo chính sách |
| CAMERA-01 | Camera trước, ảnh xoay thiết bị | Ảnh thẳng chiều và mirror đúng yêu cầu |
| CAMERA-02 | Từ chối quyền camera | UI thông báo, không crash |
| MOMENT-01 | A gửi B, C không thuộc recipients thử GET ảnh | C bị 403/404, B xem được |
| MOMENT-02 | Upload tập tin giả JPEG hoặc quá lớn | 415/413; không lưu file rác |
| MOMENT-03 | Timeout trong lúc upload rồi retry | Không gửi trùng nếu dùng idempotency key |
| HISTORY-00 | A gửi B khi chưa có FCM/widget, B vào Đã nhận | B thấy ảnh qua GET API; C không được xem |
| WIDGET-01 | App B ở nền, A gửi ảnh | Widget B cập nhật khi điều kiện hệ thống cho phép |
| WIDGET-02 | Logout trên B | Widget hiển thị yêu cầu đăng nhập hoặc placeholder |
| DELETE-01 | A xóa ảnh đã gửi | GET ảnh cũ bị từ chối; widget B đồng bộ về ảnh hợp lệ |
| HISTORY-01 | A/B có nhiều ảnh, phân trang | Mới nhất trước, không lặp/trượt trang bất thường |
| DEPLOY-01 | Truy cập trực tiếp URL file trong VPS | Không thể tải ảnh nếu chưa qua endpoint xác thực |
| BACKUP-01 | Restore thử từ backup DB + image | Phục hồi được dữ liệu nhất quán |

**Cách ghi kết quả:** tạo bảng QA với các cột `ID`, `Ngày thử`, `Thiết bị`, `Bước thực hiện`, `Kết quả mong đợi`, `Kết quả thực tế`, `Pass/Fail`, `Log/screenshot` khi bước triển khai đã bắt đầu.

## 12. Các điểm cần khóa trước khi code và tiêu chí hoàn thành

| Quyết định | Mặc định đề xuất trong tài liệu |
|---|---|
| Kiểu login | Email + mật khẩu; username dùng để tìm bạn |
| Package Android / Firebase | **`com.taktak.app`**, chốt trước khi tạo `google-services.json` |
| Build Android | Gradle **Groovy DSL**; không dùng Kotlin code |
| Build server | Maven + Java 17 |
| Navigation | Bốn tab như PDF: Camera, Lịch sử, Bạn bè, Hồ sơ |
| Caption | ≤50 ký tự Unicode |
| Phương thức upload recipientIds | Form-data `recipientIds` lặp lại theo ID |
| Không có ảnh widget | Placeholder có nút/điểm chạm mở app |
| Ảnh bị xóa | Không còn truy cập được trên server ngay; client đồng bộ lại |
| Lưu ảnh local | Chỉ khi người dùng chọn “Lưu ảnh” |
| Timestamp | UTC ở DB/API; đổi múi giờ khi hiển thị |
| Hệ thống build | Android client và Spring server độc lập; cùng một repo |
| Kiểm thử sớm | Sau 3C làm **3D: CameraX → POST owner-only → GET ảnh bằng JWT** |
| Màn Đã nhận | Làm tại **5E**, trước FCM và widget |
| Mạng Android | Emulator `10.0.2.2`; máy thật LAN/tunnel; debug có thể HTTP, release chỉ HTTPS |

**Định nghĩa “DONE” của mỗi bước:** mã nguồn build được; tính năng có test tay hoặc test tự động phù hợp; không còn crash nghiêm trọng; log lỗi xử lý được; checklist đạt; commit Git; có hướng dẫn chạy mới; **AI phải dừng và chờ xác nhận**.

## 13. Backlog sau MVP (không triển khai trong 8 giai đoạn)

Theo hai mục nâng cao 8A–8B trong PDF gốc, có thể bổ sung sau khi Taktak MVP ổn định: bộ lọc ảnh, chữ/sticker, vẽ tay, khung Polaroid; tùy biến widget; báo cáo nội dung/người dùng; ảnh tự hết hạn, kiểm soát screenshot (`FLAG_SECURE`, chỉ hạn chế chứ không chống chụp bằng thiết bị khác). Những mục này phải có giai đoạn và test riêng, không được tự thêm vào MVP.

## 14. PROMPT NỀN TAKTAK — dán vào đầu cuộc chat code

> **Vai trò:** Bạn là senior Android Java + Spring Boot backend engineer. Hướng dẫn tôi làm ứng dụng **Taktak**, ảnh chụp gửi nhóm bạn và hiện trên widget 2×2. Trả lời bằng tiếng Việt, code chạy được, giải thích ngắn và nêu cách kiểm thử.
>
> 
> **Android:** Java hoàn toàn, **không Kotlin, không Jetpack Compose**; XML + ViewBinding, Material Components, minSdk 26, MVVM + Repository/ViewModel/LiveData; Retrofit/OkHttp/Gson/Glide/CameraX/WorkManager/AppWidgetProvider/RemoteViews/EncryptedSharedPreferences; FCM chỉ làm tín hiệu đánh thức đồng bộ. Bốn tab: Camera, Lịch sử, Bạn bè, Hồ sơ.
>
> 
> **Server:** Java 17, Spring Boot 3 Web/Security/Data JPA/Validation, PostgreSQL + Flyway, BCrypt, JWT access/refresh, ảnh private trên VPS qua API xác thực, Firebase Admin SDK gửi FCM. Không dùng Firebase Auth, Firestore, Firebase Storage hay Cloud Functions.
>
> 
> **Bảo mật:** mọi media bắt buộc kiểm tra sender/recipient, chỉ gửi bạn thật không bị block, không lộ image URL public, token, API key; giới hạn upload/đăng nhập/tìm kiếm. `/auth/refresh` xác thực bằng refresh token và phải hoạt động khi access token hết hạn.
>
> 
> **Lộ trình khóa:** Bước **0** đăng ký Firebase Android `com.taktak.app` và chốt mạng thử; sau auth Android (3C) làm lát cắt dọc 3D (ảnh chỉ mình tôi, upload rồi tải lại bằng JWT); sau gửi ảnh (5D) làm 5E **Đã nhận**, sau đó mới FCM/widget. HTTP chỉ cho debug local, release HTTPS.
>
> **Cách làm:** Chỉ làm **một giai đoạn hoặc tiểu bước tôi yêu cầu**. Đưa đường dẫn và **nội dung đầy đủ** của từng file cần tạo/sửa; nói rõ Gradle/Maven/Manifest/SQL/env thay đổi; không dùng `...` thay cho code; đưa lệnh chạy cho Windows và curl khi có API; giải thích ngắn; có checklist test, lỗi thường gặp. Khi hoàn tất, **DỪNG** và chờ tôi xác nhận rồi mới tiếp tục. Nếu thiếu một quyết định quan trọng, hỏi trước, không tự thay stack.

### Prompt triển khai tiếp theo — chỉ Bước 0

> Bắt đầu **Bước 0** của dự án Taktak. Hướng dẫn tôi từng thao tác tạo Firebase project chỉ dùng FCM, đăng ký Android app với package **`com.taktak.app`**, lấy `google-services.json` và giữ ở nơi an toàn chờ tạo `android-client/app/`. Chốt cấu trúc repo Git `taktak`, cách kết nối máy ảo `10.0.2.2` và máy thật IP LAN hoặc HTTPS tunnel, giải thích rõ debug HTTP/release HTTPS. Không triển khai Java, auth, camera hay widget ở bước này. Cho checklist DONE, **dừng chờ tôi xác nhận** trước khi sang giai đoạn 1A.

## 15. Mẫu prompt khi lỗi và bàn giao

**Lỗi build/crash:** “Đang ở Taktak giai đoạn [x], tiểu bước [y]. Đây là log nguyên văn [log] và file [đường dẫn + code]. Hãy chỉ ra nguyên nhân, sửa **chỉ phần liên quan**, cho lệnh kiểm tra lại. Không chuyển bước.”

**Sai hành vi:** “Hiện tại [hành vi thực tế], mong muốn [hành vi đúng], API response/Logcat [nội dung]. Hãy tìm nguyên nhân client hay server và hướng dẫn sửa tối thiểu. Không viết lại kiến trúc.”

**Trước khi chuyển bước:** “Tôi đã chạy [các ca test], đây là output [log]. Hãy đối chiếu checklist DONE của giai đoạn này; nếu đạt, cho câu lệnh Git commit. Chỉ hỏi tôi có chuyển bước tiếp không.”

---

**Kết luận:** Taktak là bản thiết kế **Android Java + backend Spring Boot riêng** dựa trên phương pháp thực thi tuần tự của roadmap Locket gốc. Văn bản này khóa phạm vi, luồng dữ liệu, bảo mật, API và các bài test trước khi bắt đầu coding. **Bước thực hiện đầu tiên duy nhất:** **Bước 0 — khóa package `com.taktak.app`, tạo Firebase FCM và kế hoạch thử mạng**, sau khi bạn xác nhận. Sau đó mới đến giai đoạn 1A.
