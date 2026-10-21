# Taktak — Quyết định kỹ thuật đã khóa

## HARDENING-01 — Reliability và vận hành, duyệt 21/10/2026

- Bổ sung V6, không sửa migration cũ: enqueue push trong transaction nghiệp vụ;
  dispatcher lease/retry riêng, at-least-once, không hứa exactly-once hoặc delivery.
- Không lưu token/nội dung ảnh/chat trong outbox; recheck registration và quyền
  hiện tại trước gửi. DEAD cần can thiệp, chưa tự replay/xóa terminal events.
- Metric chỉ tag hữu hạn; request ID do server sinh. Không mở public Actuator
  metrics/env, không log URI/query/body/PII để điều tra sự cố.
- Cache ảnh RAM có TTL/LRU và generation phiên; luôn xác thực quyền trước đọc
  cache, không coi cache/offline là grant. Không cam kết xóa từ xa gallery/backup.
- Rollback không được về binary bỏ security_version hoặc restore DB cũ rồi mở
  traffic chưa hòa giải revocation; ưu tiên forward fix/migration additive.
- Tối ưu dựa đo local: batch inbox peer/lastMessage/unread, giữ kiểm tra grant;
  chưa tăng pool/thêm hạ tầng khi chưa có số liệu bottleneck.
- Cấu hình Nginx mẫu dùng access log whitelist và tắt error log có raw URI:
  đánh đổi mất chẩn đoán TLS/proxy, cần review staging, không tự áp dụng VPS.

Chi tiết: [QA_SYSTEM_HARDENING.md](QA_SYSTEM_HARDENING.md). Stack/định danh không đổi.

## MOMENT-REPLY-01 — Trả lời ảnh vào chat riêng, duyệt 20/10/2026

Người dùng xác nhận triển khai tính năng bình luận ảnh dưới dạng trả lời riêng
qua hội thoại 1–1 với chủ ảnh. Đây là phạm vi mới có phê duyệt, không phải bình
luận công khai. Dùng MOMENT_REPLY, content tối đa 2000 Unicode và momentId;
server xác minh ảnh thuộc peer, actor đã có grant và pair friend/block hợp lệ.
Giữ reply text khi ảnh bị thu hồi, che tham chiếu ảnh. V5 chỉ mở rộng constraints
của messages, không thêm bảng comments hoặc sửa migration cũ. Android dùng
Java/XML/ViewBinding/MVVM, giữ JWT/FCM và toàn bộ stack hiện tại.
Không tự commit/push/deploy/migrate production. Chi tiết [CHAT_API.md](CHAT_API.md).

## Module Tin nhắn & Chat khoảnh khắc (Direct Messaging & Moment Chat) — 20/10/2026

Phạm vi CHAT-01–05 đã được người dùng phê duyệt triển khai cùng lượt ngày 20/10/2026, nghiệm thu trên USB, không emulator/commit/push/deploy. Đối chiếu schema thực tế: friendship hợp lệ là bản ghi cặp chuẩn hóa trong `friendships` (không có cột status); chặn nằm ở `blocks`, không tạo bảng `user_blocks`. V4 dùng `CHECK(user1_id < user2_id)` + `UNIQUE(user1_id,user2_id)` thay biểu thức `LEAST/GREATEST` trong UNIQUE constraint không hợp lệ của bản kế hoạch. Không sửa migration V1–V3.

Chat chỉ liên kết moment mà **cả hai** đã có quyền xem, không âm thầm cấp thêm người nhận cho ảnh. Tin văn bản tối đa 2.000 ký tự Unicode. Retry dùng `clientMessageId` duy nhất theo sender; đổi nội dung với cùng mã bị 409. Đồng bộ foreground bằng FCM báo thay đổi + polling 5 giây trong lifecycle đang mở; không WebSocket hoặc cam kết delivery tức thì khi OEM/force-stop chặn nền. Bản nháp/pending giữ qua activity recreation nhưng chủ động bỏ sau process death hoặc đổi thế hệ phiên để không phục hồi nội dung tài khoản cũ. Chi tiết hợp đồng: [CHAT_API.md](CHAT_API.md).

Người dùng yêu cầu đưa module Tin nhắn vào kế hoạch triển khai của ứng dụng Taktak dựa trên thiết kế 5 màn hình tham chiếu.
- **Mở rộng điều hướng (Navigation)**: Bottom Navigation Bar chuyển đổi từ 4 tab lên **5 tab**: Camera (0) | Lịch sử (1) | **Tin nhắn (2)** | Bạn bè (3) | Hồ sơ (4).
- **Thiết kế 5 màn hình**:
  1. *Hộp thư đến (`MessagesFragment` / `fragment_messages.xml`)*: Danh sách cuộc trò chuyện 1-1, avatar bạn bè, tên hiển thị, trích đoạn tin nhắn gần nhất, thời gian tương đối, huy hiệu (badge) đếm tin nhắn chưa đọc, ô tìm kiếm hội thoại, nút `+` góc trên phải để mở màn hình chọn bạn bè tạo cuộc trò chuyện mới.
  2. *Khung chat 1-1 (`ChatActivity` / `activity_chat.xml`)*: Bong bóng chat văn bản (bên phải: người gửi - gradient/accent; bên trái: người nhận - tonal/card), bong bóng ảnh khoảnh khắc 1:1 kèm caption, trạng thái "Đã xem" (read receipt) hiển thị dưới tin nhắn cuối cùng đã được đối phương đọc.
  3. *Xử lý bàn phím ảo IME*: Sử dụng WindowInsets API để đẩy mượt mà khung chat và input bar lên trên khi bàn phím ảo hiển thị, đảm bảo không che khuất tin nhắn hoặc ô nhập liệu ở mọi kích thước màn hình.
  4. *Soạn tin mới (`NewChatActivity` / `activity_new_chat.xml`)*: Danh sách bạn bè hiện có từ API bạn bè, ô tìm kiếm bạn bè, nhấp chọn bạn bè để mở hoặc tạo mới cuộc trò chuyện 1-1.
  5. *Trạng thái rỗng (Empty state)*: Minh họa icon viền cam, headline "Chưa có tin nhắn", phụ đề "Bắt đầu cuộc trò chuyện với bạn bè của bạn", nút CTA "Tìm bạn ngay" (chuyển hướng sang tab Bạn bè).
- **Tương thích giao diện 4 theme**: Đầy đủ 4 phong cách (Original, Gradient, Glass, Soft) hỗ trợ DayNight thông qua semantic tokens của `ThemeManager`.
- **Kiến trúc Cơ sở dữ liệu (Flyway Migration V4)**:
  - Bảng `conversations`: `id UUID PRIMARY KEY`, `user1_id UUID NOT NULL REFERENCES users(id)`, `user2_id UUID NOT NULL REFERENCES users(id)`, `last_message_id UUID`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`, `updated_at TIMESTAMP WITH TIME ZONE NOT NULL`. `CHECK(user1_id < user2_id)` và `CONSTRAINT uq_conversations_pair UNIQUE(user1_id,user2_id)` đảm bảo cặp chuẩn hóa duy nhất.
  - Bảng `messages`: `id UUID PRIMARY KEY`, `conversation_id UUID NOT NULL REFERENCES conversations(id) ON DELETE CASCADE`, `sender_id UUID NOT NULL REFERENCES users(id)`, `recipient_id UUID NOT NULL REFERENCES users(id)`, `message_type VARCHAR(20) NOT NULL` (giá trị: `TEXT`, `MOMENT`), `content TEXT`, `moment_id UUID REFERENCES moments(id) ON DELETE SET NULL`, `read_at TIMESTAMP WITH TIME ZONE`, `created_at TIMESTAMP WITH TIME ZONE NOT NULL`. Chỉ mục `idx_messages_conversation_created` phục vụ phân trang tin nhắn hiệu năng cao.
- **Phân quyền & Bảo mật**:
  - Chỉ người dùng có bản ghi cặp bạn bè chuẩn hóa trong `friendships` và không bị chặn hai chiều trong `blocks` mới được phép nhắn tin, đọc hoặc tạo hội thoại.
  - Khi gửi `MOMENT`, server kiểm tra quyền xem hiện có của cả sender và recipient, xác nhận moment chưa bị xóa (`deleted_at IS NULL`); không cấp thêm người nhận qua chat.
  - Ảnh trong tin nhắn chat phải tải qua endpoint bảo mật có xác thực JWT, không dùng public URL.
  - FCM thông báo tin nhắn mới là data-only message (loại sự kiện `chat_message`), tuyệt đối không truyền nội dung tin nhắn, access token hay private path trong payload FCM.
- **Lộ trình phân rã 5 tiểu bước**:
  - `CHAT-01`: Backend Database & REST API (Spring Boot 3, Flyway V4, PostgreSQL, Controller/Service/Repo, Integration Tests).
  - `CHAT-02`: Android Navigation 5 Tabs & Messages Inbox UI (Màn 1 & 5).
  - `CHAT-03`: Android New Chat / Select Friend UI (Màn 4).
  - `CHAT-04`: Android Chat Room UI & Send Text/Moment (Màn 2 & 3).
  - `CHAT-05`: Realtime Sync & FCM Integration & UI Automation Tests.

## UI-THEME-08 — Profile screen, Avatar upload & Logout bốn theme, 20/10/2026

Nghiệm thu UI-THEME-08: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Hồ sơ cá nhân (`fragment_profile.xml`), bao gồm thông tin tài khoản, chỉnh sửa tên hiển thị, tải lên ảnh đại diện, chuyển giao diện và đăng xuất an toàn.
- Bổ sung `<attr name="ttMediaOutlineStyle" format="reference" />` và style `@style/Widget.TakTakMedia.Outline` trong `res/values/media_themes.xml`; cấu hình trên 4 theme và khai báo fallback an toàn trong `Base.Theme.TakTak` (`res/values/themes.xml`).
- Layout `fragment_profile.xml` gỡ bỏ hardcoded `@style/Theme.TakTak.Premium`, liên kết động với tokens (`ttMediaHeadlineStyle`, `ttMediaTonalStyle`, `ttMediaFieldStyle`, `ttMediaInputStyle`, `ttMediaPrimaryStyle`, `ttMediaOutlineStyle`, `colorPrimary`, `colorOnPrimary`).
- Fixture `MediaThemeFixture` hỗ trợ mock UserApi (`me`, `update`, `uploadAvatar`, `avatar`) và DeviceApi (`devices`) để test tải hồ sơ, đổi avatar, sửa tên và đăng xuất không phụ thuộc mạng ngoại vi.
- Kiểm tra thực tế: `ProfileThemeUiTest` 3/3 PASS (24.209s), `MediaThemeUiTest` 5/5 PASS (114.147s), `ListThemeUiTest` 4/4 PASS (29.027s) trên emulator `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM). 105/105 Android JVM unit tests PASS, 152/152 Spring Boot server tests PASS. Đóng emulator an toàn qua `adb emu kill`. Chi tiết: [QA_THEME_PROFILE.md](QA_THEME_PROFILE.md).

Nghiệm thu UI-THEME-07: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Lịch sử (Đã nhận / Đã gửi), tab Bạn bè, các phần tử danh sách (Moments, Invitations, Friends, Search results, Section headers, Footers) và hộp thoại xác nhận.
- Gỡ bỏ hardcoded `@style/Theme.TakTak.Premium` trong `fragment_history.xml` và `fragment_friends.xml`, thừa hưởng Context Theme từ MainActivity.
- Bổ sung 7 theme attributes và styles (`ttMediaPrimaryStyle`, `ttMediaTonalStyle`, `ttMediaCardBackground`, `ttMediaTabIndicator`, `ttMediaTabSelectedColor`, `ttMediaTabUnselectedColor`, `ttMediaDividerColor`, `ttMediaNoticeBackground`) trong `res/values/media_themes.xml`.
- Bổ sung fallback tokens đầy đủ trong `Base.Theme.TakTak` (`res/values/themes.xml`).
- Sửa lỗi tràn biên ngang trên viewport hẹp (280dp + fontScale 2.0x): cấp `layout_weight="1"` và `singleLine="true"` cho `momentSenderUsername` trong `item_history_moment.xml`.
- Cập nhật mock model `MomentModels.PublicUser`, đầy đủ các trường bắt buộc của `FriendModels.Invitation` và các mock mutations quan hệ bạn bè trong `MediaThemeFixture.java`.
- Kiểm tra thực tế: `ListThemeUiTest` 4/4 PASS (31.607s), `MediaThemeUiTest` 5/5 PASS (116.985s) trên emulator `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM). 105/105 Android JVM unit tests PASS, 152/152 Spring Boot server tests PASS. Đóng emulator an toàn qua `adb emu kill`. Chi tiết: [QA_THEME_LISTS.md](QA_THEME_LISTS.md).

## UI-THEME-06 — Media bốn theme (Camera / Preview / Navigation / Viewer), 19/10/2026

Người dùng duyệt nghiệm thu UI-THEME-06. Mở rộng `ThemeManager.ScreenRole` cho 3 vai trò `MAIN`, `PREVIEW`, `VIEWER`. Cung cấp đầy đủ style tokens và color palettes trong `res/values/media_themes.xml` cho cả 4 phong cách (Original, Gradient, Glass, Soft) hỗ trợ DayNight.
- Nền native `MediaBackdropView` vẽ Canvas shader 60fps, không chụp/blur bitmap.
- Khung hình 1:1 vuông được bảo toàn tuyệt đối cho CameraX preview, ảnh chụp nháp và ảnh trong Viewer.
- Bảo toàn idempotency key, retry retention, caption UTF-8 50 ký tự, lựa chọn người nhận và không upload khi Retake.
- Moment Viewer ẩn ảnh/tên người gửi và khóa các nút hành động khi bị thu hồi quyền (403).
- Bổ sung `ScrollView` responsive cho `activity_moment_viewer.xml` đảm bảo không bị nén chiều cao chữ ở mọi viewport ({280, 560}, {360, 720}, landscape {840, 420}) và fontScale 2.0x.
- Kiểm tra thực tế: `MediaThemeUiTest` trên AVD 5/5 tests PASS (138.247s), 105/105 JVM tests PASS, 152/152 server tests PASS. Đóng AVD an toàn sau kiểm thử.

## Feature B — Validation đăng ký tài khoản đầy đủ, 19/10/2026

Người dùng duyệt nghiệm thu Feature B. Đã hoàn thành 7 yêu cầu:
1. Username min length: Ít nhất 3 ký tự (tối đa 255), chỉ `[a-z0-9_]`.
2. Username không được bắt đầu hoặc kết thúc bằng `_` và không có 2 dấu `__` liên tiếp; regex `^(?!.*__)[a-z0-9][a-z0-9_]{1,253}[a-z0-9]$`.
3. Display name tối thiểu 1 ký tự sau strip (đã bảo đảm bằng `@NotBlank @Size(max = 255)` và `AuthInputValidator.displayNameError`).
4. Phân biệt mã lỗi 409 PostgreSQL (SQLState 23505): index `uq_users_email_lower` trả về `EMAIL_CONFLICT` (*"Email đã được sử dụng."*), constraint `users_username_key` trả về `USERNAME_CONFLICT` (*"Tên người dùng đã được sử dụng."*), fallback `ACCOUNT_CONFLICT`. Android ánh xạ lỗi và tự động highlight/focus trường bị trùng.
5. Inline validation: Lắng nghe `onFocusChangeListener` trên cả 5 trường (`displayName`, `username`, `email`, `password`, `confirmPassword`); TextWatcher xóa lỗi cũ khi người dùng bắt đầu sửa ký tự.
6. Password strength indicator: `LinearProgressIndicator` + `MaterialTextView` trong `activity_register.xml` đánh giá 3 mức (Yếu/Trung bình/Mạnh) theo độ dài Unicode code point và 4 nhóm ký tự (hoa, thường, số, đặc biệt), hiển thị màu sắc tương thích toàn bộ 4 theme.
7. Username availability check: Endpoint `GET /api/auth/check-username?username=...` không yêu cầu xác thực, `CacheControl.noStore()`, trả về `UsernameAvailabilityResponse(username, available)`. Android debounce 400ms bằng `Handler`, hiển thị trạng thái đang kiểm tra và tính khả dụng của tên người dùng.
Kiểm tra thực tế: 152/152 server tests PASS, 105/105 Android JVM tests PASS, lint 0 errors, assembleDebug APK PASS.

## UI-THEME-05 — Register/Recovery và QA USB, 19/10/2026

Người dùng duyệt05, yêu cầu dùng điện thoại thay emulator. AUTH resolver hỗ trợ4theme qua Theme.TakTak.Auth; Original kế thừa Premium và dùng renderer cũ,3style mới tái dùng palette/Canvas Login với style input/field/CTA/headline riêng. Áp style/local night mode trước super.onCreate; không global mode, không ép recreate form/password/reset-session. Placeholder mới dùng semantic tokens để đạt contrast; không thay màu cũ toàn app. MAIN/PREVIEW/VIEWER vẫn fallback, cần duyệt06trở đi. Giữ Material1.14.0,Java/XML/ViewBinding và toàn bộ auth/recovery/SMTP/session/backend/DB/FCM/widget. QA dùng dữ liệu sai/fixture RAM, appearance tạm có restore và bytes-session assertion, không gửi email hoặc reset thật. [Kết quả và hạn chế](QA_THEME_AUTH.md).

## UI-THEME-04 — tích hợp riêng Login, 19/10/2026

Người dùng duyệt Login4theme. LOGIN resolver nay hỗ trợ Original/Gradient/Glass/Soft; role AUTH/MAIN/PREVIEW/VIEWER giữ fallback cũ, không tuyên bố toàn app đã hỗ trợ. Original Login giữ fixed-dark theo audit; ba style mới dùng DayNight, token Login riêng và Canvas tĩnh, giữ logo và Material1.14.0. Chọn style/mode được áp trước super.onCreate/inflate, local night mode, không đổi default toàn app. Khi quay từ chooser, chỉ recreate Login đang RESUMED/idle/form trống và chưa success/reset; nếu có draft/loading thì báo pending riêng, không lưu password hoặc sửa AuthViewModel để giữ theme. Không thay API/JWT/backend/DB/FCM/widget. Khác biệt màu tím CTA sáng được điều chỉnh vì kiểm thử tương phản, không đổi palette chooser toàn cục. [Bằng chứng/giới hạn](QA_THEME_LOGIN.md).05 phải được duyệt riêng.

## UI-THEME-03 — chooser riêng,19/10/2026

Người dùng duyệt riêng màn chọn theme.4style chooser có preview/tokens DayNight riêng, không đổi resource hoặc availability5role nghiệp vụ02. Chọn chỉ giữ draft; Apply mới save qua repository/executor, commit đạt mới báo thành công và recreate riêng chooser. Hủy/Back bỏ draft, không hủy giả commit đang chạy. Login/Hồ sơ mở bằng Activity thông thường, không restart task/ép recreate màn cha hoặc đổi AppCompat default night mode toàn app. System bars của chooser đồng bộ surface và icon contrast. Không nâng Material1.14.0/stack,không đổi API/backend/DB/JWT/FCM/widget. Tích hợp từng màn thuộc04–08 phải được duyệt tiếp; chi tiết/bằng chứng [QA_THEME_CHOOSER.md](QA_THEME_CHOOSER.md).

## UI-THEME-02 — nền tảng lựa chọn giao diện,19/10/2026

Người dùng duyệt riêng02 theo audit. IDs original/gradient/glass/soft và mode system/light/dark độc lập, lưu trong SharedPreferences private taktak_appearance_v1 tách auth/device, schema1/revision. Unknown/corrupt đọc fallback không tự sửa; future schema chặn ghi; save worker/commit đạt mới trả thành công, không auto recreate. Resolver theo5screenRole giữ Original resources cũ;3style chưa render phải trả availability=false/effective Original, không giả đã có theme mới. Chưa chuyển các Activity/layout, chưa thêm chooser hoặc đổi stack/Material1.14.0/backend/database; tích hợp từng bước sau khi được duyệt. Chi tiết [QA_THEME_FOUNDATION.md](QA_THEME_FOUNDATION.md).

## Xác minh email và thu hồi phiên sau reset —19/10/2026

Người dùng duyệt bổ sung xác minh email trước recovery và vô hiệu ngay mọi JWT/refresh cũ khi reset thành công. Đây là ngoại lệ được duyệt thay chính sách access-JWT-còn-hạn **riêng cho reset**, không thay logout thông thường. Dùng security_version đối chiếu DB mỗi request, khóa user thống nhất login/refresh/reset, migrationV3 mới; không sửa V1/V2 hoặc DB production. Tài khoản cũ không được tự đánh dấu verified; cần mật khẩu hiện tại + mã mailbox trước recovery. Mật khẩu tạm15phút chỉ cấp reset session opaque một lần, không full access. SMTP STARTTLS, không log secrets; không coi HTTP200 generic/mock mail là đã giao Gmail thật. Chi tiết: [ACCOUNT_RECOVERY.md](ACCOUNT_RECOVERY.md). Phần B validation đăng ký chưa được duyệt.

## Ghi nhận phạm vi UI5 màn —18/10/2026

Người dùng xác nhận redesign Register,Camera,History,Friends,Profile và navbar theo5 ảnh, thay giới hạn Login-only ở prompt trước. Đây là thay đổi presentation, không đổi stack/version/API/nghiệp vụ. Theme Premium opt-in và palette DayNight riêng; giữ theme Login/foundation, widget RemoteViews và Preview/Viewer ngoài phạm vi. Nền canvas tĩnh thay blur đắt, controls native; không hard-code nội dung/ảnh cá nhân từ ảnh mẫu. Camera giữ1:1 theo đặc tả, có thể cuộn thay vì đổi aspect để khớp ảnh tham chiếu. Scope và kiểm thử: [QA_PREMIUM_FIVE_SCREENS.md](QA_PREMIUM_FIVE_SCREENS.md).

## Hồ sơ/avatar và FCM thật — 18/10/2026

Người dùng duyệt Hồ sơ cá nhân, Avatar cá nhân và nghiệm thu FCM/widget; sau đó chọn emulator trước khi có thể dành điện thoại cho QA. ProfileFragment thay tab placeholder, dùng ProfileRepository/ViewModel và session gate hiện có. PUT me vẫn chỉ sửa displayName; avatar dùng POST multipart riêng, GET có JWT cho chính chủ/bạn accepted không block. Stranger/block nhận404; chưa có avatar trả204. Không đưa private path vào DTO, không public URL/cache đĩa Glide.

Avatar dùng cột users.avatar_path V1, lưu trong IMAGE_STORAGE_ROOT/avatars bằng UUID server tạo; JPEG512×512 bỏ metadata, sanitizer/giới hạn input có sẵn. Row lock user dùng chung cho sửa tên/avatar; dọn file mới khi rollback và file cũ sau commit. Backup/restore chứa cả avatars và xác minh references của users; không thêm/sửa migration. Picker callback ở Activity để tồn tại qua session-gate Fragment recreation, kiểm tra generation trước dùng ảnh.

Firebase Cloud Messaging thật đã nghiệm thu trên AVD với Admin JSON local và QA AppWidgetHost/RemoteViews; không suy ra launcher OEM hoặc điện thoại thật đã PASS. Không đổi stack, dependency/package hoặc cấp quyền deploy. Bằng chứng và giới hạn: [QA_PROFILE_AVATAR_FCM.md](QA_PROFILE_AVATAR_FCM.md).

## Ghi nhận sửa lỗi review — 18/10/2026

Không thay quyết định stack/định danh/nghiệp vụ. Xóa vật lý ảnh chỉ sau DB commit; request sau soft-delete vẫn bị từ chối. Viewer và thao tác xuất gallery phải tải bằng JWT, không dùng cache làm fallback khi mất mạng/mất quyền; logout/đổi phiên chặn publication cũ và dọn dữ liệu hiển thị/cache/widget. FCM disabled là lựa chọn explicit; bật nhưng sai credentials thì fail-fast, production không giữ recorder hoặc log raw device token.

Native backup/restore có khóa bảo trì, archive mã hóa root-only và backup key tách riêng; restore có xác nhận, DB transaction và ảnh cũ có thể phục hồi. Rollback JAR không tự restore/reset DB hay đảo migration. Nginx ghi đè XFF, backend chỉ tin proxy đã xác định. Đây là cách thực thi các yêu cầu an toàn đã có, không phê duyệt deploy hoặc hạ điều kiện nghiệm thu. Bằng chứng/giới hạn: [QA_REVIEW_FIXES.md](QA_REVIEW_FIXES.md), [runbook](../deploy/MAINTENANCE.md).

## Mốc hiện tại — 5C, ngày 17/10/2026

5C được duyệt riêng và PASS local/USB, xem [QA_MOMENT_SHARING.md](QA_MOMENT_SHARING.md). Các đoạn5B/3D “chưa hỗ trợ gửi bạn bè” bên dưới là lịch sử. Giữ stack/package/dependency/JWT/schemaV1/FCM; không sửa migration hoặc backend ngoài media trong5C. Giữ HEAD `ed9fde6`, không commit/push.

Repeated recipientIds từ1–100 UUID đầy đủ, không trùng/bản thân/JSON/CSV/trộn[]; bỏ field hoặc[] giữ owner-only. Server khóa mọi user liên quan theo UUID PostgreSQL cùng thứ tự FriendService, xét friendship/block hai chiều sau chờ khóa rồi lưu moment/recipients cùng transaction/cùngcreated_at. Một ID sai từ chối cảset, không tự lọc. GET xét sender hoặc recipient được cấp cho moment và deleted_at mỗi lần; friendship không tự cấp quyền. Block/unfriend ngăn gửi mới, không tự đổi grant cũ ngoài đặc tả. FileMIMEimage/jpeg và signature/decode thực, re-encode bỏEXIF; giới hạn5MiB/file,6MiB/request,104parts(containerbounded),4096/cạnh/16MP. Storageprivate/UUID/rollback/no-store giữ nguyên.

Android Preview gửi selected snapshot hoặc đọc đủ trang cho Tất cả, rỗng/quá100/lỗi không gửi một phần. Khóa payload khi POST chưa rõ;400 xác định từ chối cho sửa, knownID chỉGET, không chuyển retry gửi bạn sang owner-only. GET ảnh có tối đa1transportretry, không retryHTTP/ApiFailure/POST; không thay auth/refresh policy. Nén cạnh1280/1024/768,quality92→52 bước8,dừng≤800KiB;500–800KiB là mục tiêu,không upscale/padảnh nhỏ. Cache/session/caption/mirror/CameraX/FCMgiữ nguyên. Chưa idempotency5D/History5E,không tự chuyển bước.

## Mốc hiện tại — 5B, ngày17/10/2026

5B đã được duyệt và PASS local/USB; chưa duyệt5C. Git hiện tại `ed9fde6` được commit/push theo yêu cầu riêng trước5B; thay đổi5B chưa commit/push. Các ghi nhận bên dưới là lịch sử.

Camera nhận buffer CameraX, tái sử dụng PhotoProcessor để rotation/mirror camera trước/crop1:1 và ghi JPEG vào cache internal gắn generation. Intent chỉ chứa UUID/generation, Preview không exported. Repository chặn path không hợp lệ, kiểm tra phiên trước/sau I/O, giới hạn byte và từ chối draft quá24giờ. Cache có thể bịOS thu hồi; chụp lại/Back/logout/cleared dọn đúng file thuộc màn, không gallery. Recreate giữ caption/lựa chọn bằng SavedStateHandle, không bitmap/token trong Bundle; đổi phiên không được tái sử dụng ảnh/caption cũ.

CaptureViewModel/PreviewViewModel tách chụp với xem trước và lưu riêng tư; gỡ MomentViewModel cũ hết tham chiếu. Caption≤50 Unicode code point, InputFilter không cắt surrogate, giữ span và chặnNUL. Client thêm multipart caption đúng contract server đã có; recipientIds vẫn[]. Friends lấy API thật page20/hasNext, mặc định all, giữ lựa chọn qua tải trang/recreate. Gửi bạn bè hiển thị chưa khả dụng tới5C. Lưu riêng tư có confirm, POST rồi GET JWT; retry biếtmomentId chỉ GET. Giữ backend/schema/JWT/FCM/stack/version và giới hạn nén3D; không coi đã làm5C/idempotency5D. Xem [QA_PREVIEW.md](QA_PREVIEW.md).

## Ghi nhận lịch sử

Mốc mới nhất: **M3E-04 Bạn bè/Friends PASS local/USB**, sau Foundation/Login/Register/Camera. Người dùng duyệt riêng màn4; chưa duyệt5A hoặc màn mới. Các mốc “chưa triển khai/chưa duyệt” bên dưới là lịch sử; không thay thế yêu cầu mới nhất. Commit/push5b6b520 trước redesign theo yêu cầu riêng; M3E chưa commit/push. Giữ stack/định danh/JWT/schema; xem [QA_M3E_FRIENDS.md](QA_M3E_FRIENDS.md).

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

## Bổ sung M3E-01 —17/10/2026

Người dùng duyệt Modern M3 Expressive + Soft Social với Coral Pink/Cream/Lavender và riêng màn1 Foundation + Login. Giữ Material1.14.0 stable đã khóa; không dependency mới hoặc nâng SDK. Dùng `Theme.Material3Expressive.DayNight.NoActionBar`, Expressive button/progress, XML/ViewBinding và font hệ thống. Theme riêng Login qua Manifest; application/Register/Main không chuyển theme trong màn1. Semantic resource prefix m3_/TakTakM3 tách khỏi camera/auth styles cũ, values-night đầy đủ; form max480dp, typography sp, touch≥48dp, CTA≥56dp. Insets Auth thêm displayCutout cùng systemBars/IME; không thay ViewModel/Repository/auth lifecycle hoặc backend. Widget tương lai chỉ chia sẻ màu/drawable phù hợp RemoteViews, không Material Views/Compose. Các màn chưa có không được dựng chức năng giả hoặc tự chuyển roadmap. Nghiệm thu và giới hạn tại QA_M3E_LOGIN; dừng trước Register.

## Bổ sung M3E-02 —17/10/2026

Register opt-in cùng foundation bằng Theme.TakTak.M3.Register,giữ5trường trên1form và max-width480dp,hai section có heading native. Chỉ đổi presentation/resource và2dòng ViewCompat cho heading;không đổi validation/normalization/Repository/ViewModel/API/password/session. Success vẫn ở Register,xóa hai password và không tự login. Sau khi cả hai form đã chuyển M3E,gỡ auth_styles.xml gồm6style không còn tham chiếu;khôi phục được từ Git,không thay theme Main/Camera/Friends. Giữ Material1.14.0/Java/XML/ViewBinding,không nâng stack. Bằng chứng/giới hạn [QA_M3E_REGISTER.md](QA_M3E_REGISTER.md);dừng chờ duyệt M3E-03.

## Bổ sung M3E-03 —17/10/2026

Camera/phiên opt-in Theme.TakTak.M3.Camera trên cây View, không đổi theme toàn MainActivity để Friends chưa redesign vẫn giữ palette cũ. Camera max480dp, preview vuông bo28dp, shutter Expressive88dp Coral ngay dưới ảnh; status semantic Lavender/error/success có text và retry hiện có. Insets bao gồm systemBars/IME/displayCutout. Giữ binding IDs, CameraX, API/JWT/VM/Repository và mọi nghiệp vụ; không backend/DB/FCM/dependency mới. Gỡ riêng resource Camera hết tham chiếu, giữ theme/màu cũ Friends đang dùng. Đây là chi tiết triển khai design system đã được duyệt, không đổi các quyết định kỹ thuật cố định. Nghiệm thu/giới hạn [QA_M3E_CAMERA.md](QA_M3E_CAMERA.md); dừng chờ duyệt M3E-04 Friends.

## Bổ sung M3E-04 —17/10/2026

Friends opt-in Theme.TakTak.M3.Friends trên cây View/rows; dialog xác nhận dùng root themed context,không đổi theme toàn Activity hoặc hành vi mutation. CoordinatorLayout/AppBarLayout cho header thu gọn cùng RecyclerView tái sử dụng hàng,max480dp; card24dp,avatar48dp,primary/search/refresh≥56dp và action phụ≥48dp. Giữ ba nhóm kết quả/lời mời/bạn,explicit search/IME,page20,retry và generation/session hiện có;không thêm tab hoặc API/avatar/unblock. Semantic notice và progress slot cố định;gỡ hai typography cũ hết tham chiếu sau tìm kiếm,không xóa file. Main root dùng surface M3E ở cả Camera/Friends;Camera và Auth giữ layout/nghiệp vụ. Stack1.14.0/JavaXML/ViewBinding,backend/DB/JWT/FCM không thay đổi. Chi tiết/giới hạn [QA_M3E_FRIENDS.md](QA_M3E_FRIENDS.md);dừng trước5A hoặc màn chưa tồn tại.

## Bổ sung 5A —17/10/2026

Người dùng duyệt riêng 5A sau bốn màn M3E. Main dùng ViewPager2/FragmentStateAdapter và BottomNavigationView Expressive có sẵn trong Material1.14.0; thứ tự Camera/Lịch sử/Bạn bè/Hồ sơ, cold authenticated entry vào Camera, Back từ tab khác về Camera, giữ tab qua recreate. Friends giữ chức năng4C. History/Profile chỉ là điểm đến có thông báo chưa triển khai; không giả dữ liệu hoặc thực hiện5E/7C. Navigation ẩn khi IME mở; private pager bị gỡ trước xác minh lại phiên, không bind Camera từ restored Fragment khi chưa được session gate cho phép.

CameraX tái sử dụng3D, chỉ bind khi tab Camera RESUMED và phiên hợp lệ; ticket async chống bind muộn, chỉ unbind use case của chính Fragment khi pause/destroy. Đổi trước/sau và flash ImageCapture Off/Auto/On theo khả năng phần cứng; không torch hoặc flash giả trên camera không hỗ trợ. Activity ViewModel giữ lens/flash qua recreate trong RAM, không lưu ảnh/token hoặc thay session. OrientationEventListener cập nhật targetRotation; xử lý JPEG vẫn qua PhotoProcessor hiện có. Không triển khai cache/Preview/caption/recipient/mirror của5B. Giữ API owner-only3D, JWT/Repository/backend/DB/FCM/dependency và các quyết định cố định. Chi tiết bằng chứng và giới hạn [QA_MAIN_CAMERA.md](QA_MAIN_CAMERA.md); dừng trước5B.

## Bổ sung UI-THEME-08 — 20/10/2026

Profile screen (`fragment_profile.xml`) gỡ bỏ hoàn toàn `@style/Theme.TakTak.Premium`, kế thừa Context Theme động của Activity theo người dùng chọn (Original, Gradient, Glass, Soft). Bổ sung token `ttMediaOutlineStyle` và style `Widget.TakTakMedia.Outline` dùng `?attr/ttLoginSecondary` và `?attr/colorOnSurface`. Hoàn thiện kiểm thử ProfileThemeUiTest trên RAM qua MediaThemeFixture (hỗ trợ UserApi và DeviceApi unregister khi logout). Hoàn tất chuyển đổi toàn bộ 4 tab chính của ứng dụng sang kiến trúc 4 theme. Chi tiết nghiệm thu tại [QA_THEME_PROFILE.md](QA_THEME_PROFILE.md).

