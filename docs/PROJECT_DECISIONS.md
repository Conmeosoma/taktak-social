# Taktak — Quyết định kỹ thuật đã khóa

Mốc mới nhất: **4B PASS backend local**, người dùng đã duyệt riêng chấp nhận/từ chối, friendship, hủy bạn và block; xem [FRIEND_RELATIONS_API.md](FRIEND_RELATIONS_API.md). UI-CAMERA/4A đã nghiệm thu trước đó. Các mốc “chưa triển khai/chưa duyệt” bên dưới là lịch sử; không thay thế yêu cầu mới nhất. Giữ stack/định danh/JWT/schema; chưa duyệt4C hoặc commit/push/deploy.

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

Debug HTTP chỉ cho `10.0.2.2`, `127.0.0.1`, `localhost` bằng network-security-config riêng debug; đích khác dùng HTTPS. Release chặn cleartext, không có URL mặc định và bắt buộc truyền URL HTTPS khi build. ADB reverse là phương án debug bổ sung. Phiên khôi phục tiếp theo đã kiểm chứng đường trực tiếp `10.0.2.2:8080` PASS sau sửa quyền runtime Android 17; LAN/máy thật chưa kiểm tra. Không thêm CAMERA permission hoặc triển khai camera/widget/auth trong giai đoạn 1.

Với target SDK 37 đang khóa, debug khai báo `ACCESS_LOCAL_NETWORK` để dùng backend local trên Android 17. Instrumentation cấp quyền cho app bằng UiAutomation trước smoke request; UI bootstrap chưa gọi mạng. Khi triển khai request local từ UI phải xử lý xin quyền/từ chối trước request. Quyền này không nằm trong manifest release. Đây là sửa tương thích cho cấu hình hiện có theo [Android local network permission](https://developer.android.com/privacy-and-security/local-network-permission?hl=en), không thay stack hoặc chính sách cleartext.

Server chỉ mở GET `/actuator/health`; phần còn lại trả 401, chưa có user mặc định hoặc auth giả. Flyway clean bị tắt, migration V1 tạo 8 bảng; chưa có JPA entities nên `ddl-auto=validate` chưa thay thế kiểm thử schema SQL. JWT secret/FCM credential chưa sử dụng ở 1A; phải được kiểm tra bắt buộc khi triển khai tính năng tương ứng, không dùng secret rỗng cho auth.

## Bổ sung triển khai 2A — 16/10/2026

Người dùng đã duyệt riêng 2A bằng “continue” sau khi 0–1 hoàn tất. Các đoạn 1A ở trên phản ánh mốc lịch sử. Hiện server mở thêm đúng POST `/api/auth/register`; login/JWT chưa triển khai. User entity map schema users V1 và `ddl-auto=validate` đã chạy với entity; không sửa migration, stack hoặc phiên bản dependency.

Quy ước validation 2A: email strip/lowercase Locale.ROOT, tối đa 254; username strip/lowercase Locale.ROOT, chỉ `[a-z0-9_]+`, tối đa 255; displayName strip/nonblank, tối đa 255 theo schema. Password tối thiểu 8 Unicode code point, tối đa 72 byte UTF-8, không NUL, không trim/cắt chuỗi. BCrypt cost 12/salt ngẫu nhiên; không trả password/hash/token. Username/email unique vẫn do DB quyết định, transaction saveAndFlush; SQLState 23505 trả 409 chung. Chi tiết tại [REGISTRATION_API.md](REGISTRATION_API.md). Các lựa chọn này triển khai yêu cầu 2A, không thay các quyết định công nghệ đã khóa.

## Bổ sung triển khai 2B — 16/10/2026

Người dùng yêu cầu riêng “triển khai 2B”. Dùng Spring Security OAuth2 Resource Server/JWT (Security 6.5.11, Nimbus 9.37.4 do Boot 3.5.16 quản lý và xác minh từ JAR), không dùng auth provider bên ngoài hoặc tự viết crypto. HS256 với Base64 secret ít nhất 32 byte ngẫu nhiên lấy từ môi trường, không có fallback production; access mặc định 15 phút (1–60), refresh 30 ngày (1–90).

Issuer logic `https://taktak.invalid`, audience `taktak-app`; issuer này chỉ là metadata xác thực, không phải URL API/discovery, không gọi mạng. Subject UUID, có iat/exp/jti; strict expiry không grace. Refresh opaque SecureRandom 32 byte, chỉ SHA-256 hex được lưu trong bảng refresh_tokens của V1; lock hàng + transaction revoke/insert để một refresh chỉ xoay được một lần. Không sửa migration V1 hoặc thêm bảng ngoài phạm vi.

Auth POST dùng filter chain không có bearer authentication để refresh/logout không bị access JWT hết hạn chặn. Ngoài auth, API yêu cầu JWT; actuator khác vẫn đóng. Logout chỉ revoke refresh được gửi, JWT đã cấp còn dùng tới exp; chưa token-family/denylist. Rate limit single JVM bounded 10.000 key, 20 request/IP và 5/account/60 giây, không tin X-Forwarded-For; trusted proxy/phân tán cần cấu hình khi triển khai. Chi tiết, giới hạn và bằng chứng tại [AUTH_API.md](AUTH_API.md). Chưa làm 2C.

## Bổ sung triển khai 2C — 16/10/2026

Người dùng duyệt riêng “2C — GET/PUT”. GET/PUT `/api/users/me` nhận danh tính chỉ từ JWT subject đã xác minh, không nhận ID tài khoản đích từ request. Nếu UUID không có user, trả 401 chung; không tạo tài khoản từ claims. DTO trả đúng id/email/username/displayName/createdAt, không entity/hash/đường dẫn private; response no-store.

Chỉ bind displayName, các thuộc tính ngoài DTO bị bỏ qua và không có quyền cập nhật. Validation giữ cùng 2A: strip, nonblank, max 255 đơn vị UTF-16; ghi qua JPA transaction. Không sửa migration/schema, phiên bản công nghệ hoặc filter chain 2B; chưa avatar/search/Android UI. Chi tiết và 36 test PASS tại [PROFILE_API.md](PROFILE_API.md). 2D chưa được duyệt.

## Bổ sung nghiệm thu 2D — 16/10/2026

Người dùng duyệt riêng 2D. Chốt JSON lỗi đang dùng gồm timestamp/status/code/message/path, giữ nguyên success DTO và JWT/refresh policy. MVC và security cùng Clock UTC; path không kèm query, lỗi JSON/no-store/không cookie. Danh mục code, ý nghĩa HTTP và hướng xử lý client tại [API_ERRORS.md](API_ERRORS.md). Đây là hoàn thiện hợp đồng lỗi trong phạm vi 2D, không thay quyết định công nghệ.

Kiểm thử chứng minh 404 bị error dispatch đổi thành 403; xử lý trực tiếp 404/405 trong advice, có Allow cho 405, thêm 406 JSON và fallback 500 an toàn, không mở `/error` hoặc nới filter chain. Curl được chạy thật trong integration test, config UTF-8 qua stdin, không credential trong argv/file/output; Clock điều khiển và fault injection chỉ trong test source/profile. 39 test PASS; script và giới hạn tại [QA_AUTH.md](QA_AUTH.md). Không sửa V1/dependency/Android/Firebase hoặc tự chuyển 3A.

## Bổ sung 3A — 16/10/2026

Người dùng duyệt riêng UI Java/XML và skill android_ui_verification. Login/Register dùng ViewBinding/Material/theme DayNight hiện có, không thêm dependency hoặc ViewModel/Repository rỗng trước 3B. MainActivity vẫn launcher; chỉ thêm lối mở form, hai auth activity không exported. Submit hợp lệ báo chưa nối server, loading chỉ renderer/hook UI, không giả đăng nhập/đăng ký hoặc lưu token. Password/confirmation không lưu view state, không trim/cắt hoặc log.

Validation tương ứng giới hạn backend; email Android Patterns chỉ là precheck, server vẫn authoritative khi 3B nối API. Insets áp dụng ở FrameLayout ngoài để ScrollView có viewport đúng khi mở IME; accessibility qua ViewCompat để giữ min26. Cách chọn JDK đầy đủ bằng Gradle -D trong QA_AUTH_UI chỉ là lệnh môi trường local, không thay phiên bản stack/SDK hoặc daemon criteria trong repo. Test/screenshot và giới hạn tại [QA_AUTH_UI.md](QA_AUTH_UI.md); 3B chưa được duyệt.

## Bổ sung 3B — 16/10/2026

Người dùng duyệt riêng nối API/Repository/ViewModel và phiên. Giữ toàn bộ stack/version/package đã khóa, không thay backend hoặc migration. Auth request không mang bearer, protected request chỉ gắn token cho origin/API path của backend; không tự follow redirect. Refresh chạy sync trên worker với khóa SessionManager, tối đa một replay; dùng chung AuthGraph singleton trong process, không tạo manager độc lập cho mỗi request.

Token pair/expiry được lưu bằng EncryptedSharedPreferences/Keystore, gắn BASE_URL, loại khỏi backup/device transfer; lỗi lưu trữ không fallback plaintext. Generation guard chặn kết quả cũ sau đổi phiên; refresh 400/401 hoặc replay 401 xóa phiên tương ứng, lỗi mạng/5xx giữ phiên với cửa sổ gộp lỗi refresh 5 giây. Không tự retry POST auth khi lỗi transport không rõ kết quả.

LiveData chỉ giữ trạng thái UI không chứa password/token; lỗi hiển thị từ resource local, không phản chiếu raw body/message. Đăng ký không tự đăng nhập. Logout hook đã có/test nhưng UI/routing/process-death flow thuộc 3C; unregister FCM thuộc bước 6. EncryptedSharedPreferences deprecated nhưng vẫn giữ đúng yêu cầu công nghệ, chưa được quyền thay thế. Bằng chứng: 20 unit + 17 emulator test và build/lint debug/release PASS local tại QA_ANDROID_AUTH; chưa tự chuyển 3C.

## Bổ sung 3C — 16/10/2026

MainActivity giữ vai trò launcher và điểm đến Camera placeholder, không thêm splash Activity/dependency hoặc tính năng CameraX trước 3D. SessionViewModel xác minh me/refresh ngoài main thread trước khi hiện Camera, kiểm tra lại lúc vào foreground. Không mở Camera chỉ vì token tồn tại hoặc expiry local; lỗi mạng giữ phiên, có retry/logout. Login/logout dùng task mới/clear task, không gửi token/profile/password qua Intent.

Logout sử dụng chung AuthGraph/SessionManager: thử thu hồi refresh, xóa local; lỗi mạng nhưng clear được hiển thị cảnh báo chỉ logout local, lỗi clear storage không giả thành công. Device unregister chưa thực hiện vì registration thuộc bước 6. Thông báo điều hướng chỉ whitelist resource ID và renderer giữ notice khi LiveData idle. Không đổi chính sách access JWT còn dùng tới exp hoặc stack/phiên bản/migration. Nghiệm thu tại QA_SESSION_ROUTING; không coi local PASS là auth máy thật PASS.

## Bổ sung triển khai3D —16/10/2026

Dùng CameraX/Material/Glide đã có, không thêm/nâng dependency. Camera sau tối thiểu, xin CAMERA lúc dùng, rotation rồi crop1:1, JPEG trong bộ nhớ riêng; ảnh displayed chỉ từ GET có JWT. Thêm MomentRepository/MomentViewModel dùng cùng ApiClient/SessionManager; media gắn generation ngay khi bắt đầu chụp để không chuyển ảnh giữa tài khoản.

POST `/api/moments` và GET `/{id}/image` triển khai trên schema moments V1 hiện có, không sửa migration. Chỉ danh sách rỗng (bỏ field hoặc literal `[]`), từ chối mọi người nhận khác tới giai đoạn5; sender từ JWT và kiểm tra user tồn tại. JPEG tối đa5 MiB/4096 mỗi chiều/16MP, re-encode bỏ metadata, tên UUID server sinh, storage private cấu hình qua IMAGE_STORAGE_ROOT, rollback dọn file. GET owner-only và kiểm tra deleted_at từng request,404 chung che dữ liệu người khác; no-store. Không thêm route public, FCM, DELETE hoặc lịch sử.

3D dùng buffer trong RAM thay vì ghi raw capture ra gallery; không thêm dịch vụ backend giả. Known id retry chỉ GET; POST mất phản hồi trước id có cảnh báo có thể tạo thêm một ảnh hoàn chỉnh. Idempotency hoàn chỉnh dành5D, không thay đổi phạm vi roadmap. Smoke runner có offline/online connector qua stdin chỉ trong test source để mô phỏng mất kết nối thật, không endpoint production. Chi tiết [MOMENT_API.md](MOMENT_API.md); không thay quyết định kỹ thuật đã khóa.

## Bổ sung triển khai4A —17/10/2026

Người dùng duyệt riêng4A. GET users/search dùng username prefix strip/lowercase, underscore literal, loại bản thân/block hai chiều, tối đa20 DTO id/username/displayName. Search30/user và60/IP/60giây, bounded10.000 key single JVM; remoteAddr, không tin XFF. POST friends/requests bind receiverId, sender từ JWT; GET chỉ pending đến người gọi, page20/hasNext, sort createdAt/id DESC. Không email/hash/avatar path hoặc FCM.

Dùng Entity/JPA và bảng V1 hiện có; không sửa migration. Gửi mời khóa users theo thứ tự UUID PostgreSQL trước kiểm tra block/friendship/pending, unique pending-pair DB bảo vệ thêm. Self400, target absent/block404 chung, trùng/đã là bạn409; không tự accept mời chéo. 4B phải phối hợp cùng khóa cặp khi thêm accept/block/unfriend. Hợp đồng, giới hạn quota/pagination và65 test/JAR tại [FRIEND_API.md](FRIEND_API.md). Không đổi stack/JWT/Android hoặc tự chuyển4B.

## Bổ sung triển khai4B —17/10/2026

Respond chỉ receiver từ JWT, hidden404 nếu khác người nhận/không tồn tại. Mọi mutation quan hệ cùng lockPair users theo UUID PostgreSQL; respond lấy projection participants trước, đọc/khóa entity request sau để không dùng status cũ. Accept/status/responded_at/friendship cùng transaction; friendship canonical LEAST/GREATEST/unique V1. Retry cùng action200 giữ timestamp và không tạo lại friendship đã unfriend; action trái trạng thái409.

GET friends trả page20/hasNext/public DTO, lọc block hai chiều. DELETE friends/{id} dùng **userId người bạn**, chỉ cặp với actor, idempotent204. Block có hướng, đồng thời xóa friendship và reject pending hai chiều; unblock chỉ xóa block của actor, không restore. Native DML trong JPA repository/transaction dùng schema V1, không thêm migration/dependency hoặc đổi JWT. Hợp đồng/giới hạn và87 test/JAR tại [FRIEND_RELATIONS_API.md](FRIEND_RELATIONS_API.md); chưa Friends UI/4C, gửi ảnh bạn bè5C/FCM hoặc deploy.

## Bổ sung triển khai4C —17/10/2026

FriendsFragment Java/XML/ViewBinding dùng FriendRepository/FriendsViewModel/LiveData và cùng ApiClient/SessionManager. Kiểm tra generation trước/sau request và trước publish; đổi phiên xóa snapshot cũ rồi xác minh lại, không xóa phiên mới. Không thay stack, định danh, JWT hoặc API server. Search explicit submit, page20/hasNext và reload sau mutation; không tự lặp mutation khi đọc lại lỗi.

Lối mở từ Camera và Back/logout tối thiểu phục vụ4C; bốn tab/ViewPager2/BottomNavigation vẫn thuộc5A. Avatar qua Glide là drawable mặc định local vì DTO chưa có API avatar; không thêm URL giả/public hoặc endpoint ngoài phạm vi. Chặn có confirm, chưa UI unblock/list block; API4B giữ nguyên. Bằng chứng/nghiệm thu và giới hạn ở [QA_FRIENDS.md](QA_FRIENDS.md). Đây là ghi nhận triển khai, không thay các quyết định kỹ thuật đã khóa.
