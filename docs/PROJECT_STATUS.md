# Taktak — Trạng thái dự án

## Production restore-security rollout — 21/10/2026

**PASS cài đặt được duyệt**: restore.sh750, reconciliation SQL640 root:root;
thêm systemd drop-in guard644, daemon-reload đạt. Bốn health sampleUP/PID84167
không đổi, NeedDaemonReload=no. Unit gốc/JAR/env giữ nguyên, SQL chưa thực thi,
không restore DB/ảnh hay restart. Backup root-only và hash tại
[QA_RESTORE_PRODUCTION_ROLLOUT.md](QA_RESTORE_PRODUCTION_ROLLOUT.md).
Production restore thật và gate khác chưa được đánh dấu PASS. Các mục bên
dưới giữ làm lịch sử trước rollout; không commit/push.

## RESTORE-SECURITY-01 — cập nhật mới nhất 21/10/2026

Đã sửa restore giữ cách ly systemd, reconciliation version/refresh/credential.
Backend full verify mới **191/191 đạt**, BUILD SUCCESS2 phút28 giây.
SQL+HTTP local4/4 và shell contract12/12 đạt; native QA DB/ảnh/systemd thật đạt,
version9→10, manual start bị guard chặn. Service chính active/healthUP, không
restore DB chính, không thay script/unit/JAR live. Chưa nghiệm thu production
rollout; gate khác vẫn mở, không đánh dấu toàn dự án PASS.
[Báo cáo](QA_RESTORE_SECURITY.md). Mục FAIL9→7 bên dưới là lịch sử trước bản sửa.

## Nghiệm thu tổng hợp mới nhất — 21/10/2026

**FAIL cổng tổng thể**. Backend187/JVM127, USB matrix2/2 và Chat fixture6/6 đạt;
Gmail QA nhận đủ3 thư được người dùng xác nhận. Theo quyền mới, FCM đã bật trên
VPS, healthUP; USB FCM/Worker/AppWidgetHost thật1/1 đạt70,674s. Chưa nghiệm thu
widget ghim launcher, ảnh logout còn trắng. LintFAIL179 MissingTranslation mỗi
variant. Restore DB/ảnh QA thành công nhưng **security_version9→7**, không đạt
an toàn thu hồi phiên. Không restore DB chính/đổi JAR/schema/commit/push.
Ưu tiên sửa restore fail-closed trước public traffic. Chi tiết và gate chưa
chạy: [QA_ACCEPTANCE_ALL.md](QA_ACCEPTANCE_ALL.md). Mục cũ giữ làm lịch sử.

## Bổ sung nghiệm thu USB vận hành — 21/10/2026

**IN PROGRESS / BLOCKED**, chưa PASS vận hành đầy đủ. Xiaomi Android15:
cache/reply9/9 (34,856s), GET HTTPS thật3/3 (3,522s) đạt. FCM thật1/1 thất bại
sau upload, chờ90s không nhận created event; tổng100,628s. Đã sửa harness
foreground MIUI, dùng đúng bind user0 và thu hồi grant cuối lượt. Chưa biết
nguyên nhân server/cloud/device; SSH host key xác minh được nhưng đăng nhập
key bị từ chối, chưa đọc được outbox/log. Người dùng đã cho phép logout và
tạo QA accounts; không gửi dữ liệu tài khoản cá nhân, cần đăng nhập lại sau QA.
Giữ nguyên các ghi nhận triển khai bên dưới, không coi là đã xác minh lại VPS.
Chi tiết: [QA_OPERATIONAL_USB.md](QA_OPERATIONAL_USB.md).

## Hiện hành — TRIỂN KHAI VPS THẬT & HARDENING-01, 21/10/2026

- **Triển khai VPS Ubuntu thật (`remote.io.vn` / 14.250.250.68:11918)**:
  - Backup toàn bộ database an toàn trước cập nhật: `/var/taktak/backups/taktak_pre_v6_*.dump`.
  - Cập nhật file JAR mới 118MB (`server/target/taktak-server-0.0.1-SNAPSHOT.jar`) vào `/var/taktak/taktak-server.jar`, sao lưu bản cũ `taktak-server.jar.bak`.
  - Dịch vụ `taktak.service` khởi động lại thành công, Flyway tự động migrate schema lên `version 6 - durable push outbox` thành công (0.138s).
  - Healthcheck nội bộ và qua Nginx: `{"status":"UP"}` 200 OK.
  - Telemetry `RequestTelemetryFilter` phản hồi header Correlation ID (`X-Request-ID: ...`) chuẩn xác.
- **Mã nguồn & Kiểm thử local/AVD**:
  - Đã triển khai outbox FCM bền vững/V6, telemetry riêng tư, cache ảnh ràng buộc phiên và xác thực lại quyền, tối ưu N+1 inbox.
  - Backend `verify` cuối 187/187; migration V5→V6 và load/query-budget targeted 2/2; Android JVM 127/127; AVD 17/17 (145.302s).
  - Chi tiết kết quả, file, giới hạn và gate tiếp theo: [QA_SYSTEM_HARDENING.md](QA_SYSTEM_HARDENING.md).

## Bổ sung nghiệm thu USB — MOMENT-REPLY-01, 21/10/2026

**PASS 7/7 ca UI fixture trên điện thoại thật**, 36,257s; Xiaomi 23129RAA4G,
Android 15, 1080×2400. Build APK kiểm thử mới đạt 17s. Dùng skill
android_ui_verification; sửa harness để mở host QA trước mỗi ActivityScenario
trên MIUI, không sửa production trong lượt này. Ảnh/log đã lưu và review.
Toàn bộ appearance được phục hồi; file phiên mã hóa không đổi (assert từng ca
và fingerprint trước/sau độc lập). Không chụp camera, gửi tin thật, đăng xuất,
deploy hoặc migrate. Chưa PASS end-to-end VPS/FCM. Ảnh QA còn cho thấy hạn chế
hiển thị Chat ngoài composer: icon quay lại khó nhận biết và timestamp/input
có tương phản thấp ở một số theme; không coi 7 ca chức năng là audit UI toàn app.
Chi tiết: [QA_MOMENT_REPLY.md](QA_MOMENT_REPLY.md).

## Hiện hành — MOMENT-REPLY-01, 20/10/2026

**PASS triển khai/QA local và AVD; chưa deploy hoặc PASS production.** Người
dùng duyệt trả lời ảnh riêng qua chat. Đã có MOMENT_REPLY/server V5, Viewer
composer + MomentReplyViewModel/Repository, ảnh tham chiếu và nội dung trong Chat;
giữ text khi ảnh bị xóa, quyền owner/grant/friend/block, retry và guard phiên.
Backend170/170, JVM122/122, Android debug/test/release unsigned build52s đạt.
AVD reply+Chat+Media18/18 (190,294s), reply bản cuối7/7 (28,388s), guard độc lập1/1.
Lint vẫn179/90 debug,179/98 release do bản dịch cũ; không thêm lỗi mới/suppress.
V5 chỉ chạy test, không chạm VPS/DB cá nhân; không commit/push/deploy.
Chi tiết/file/log/giới hạn: [QA_MOMENT_REPLY.md](QA_MOMENT_REPLY.md).

## Lịch sử — Rà soát UI/UX, 20/10/2026

### Cập nhật sau khi người dùng cho phép chạy AVD

**IN PROGRESS; chưa PASS toàn bộ.** Đã chạy AVD API 37 với userdata QA riêng,
không cần USB. Sửa thêm sáu resource cho Login/Preview/input theme/badge History.
Build cuối debug/test/release unsigned **BUILD SUCCESSFUL (34s)**; 118 JVM test
thực sự chạy, 0 lỗi. UI tổng **31/34**; targeted **11/12**, quyền Camera riêng
**1/1**. Ma trận Login/Media đạt sau sửa; ma trận List vẫn lỗi cắt chiều cao
TextView không ID, Chat attachment timeout trong bộ tổng nhưng đạt khi chạy riêng
(chưa ổn định, không coi đã sửa). Lint cuối debug **179/90**, release **179/98**.
Đã lưu/review ảnh AVD; không nghiệm thu phần cứng điện thoại/TalkBack/widget.
Chi tiết và log: [UI_UX_AUDIT.md](UI_UX_AUDIT.md). Không sửa Java/commit/push/deploy.
Đề xuất mở phạm vi test Java diagnostic cho lỗi List/Chat; không tự triển khai.

### Snapshot trước lượt AVD

**IN PROGRESS — build đạt, chưa nghiệm thu UI toàn diện.** Đã sửa resource an toàn:
28 layout, token/style/theme, tương phản, vùng chạm, dialog cuộn và chuỗi mới
cho bảy locale. Toàn bộ ID của 29 layout và 109 file production Java giữ nguyên.
Build debug/test/release unsigned **BUILD SUCCESSFUL (19s)**; báo cáo JVM có
118 test, 0 lỗi (task lần cuối UP-TO-DATE, không phải chạy mới).
Lint debug còn **179 errors / 90 warnings**, release **179 / 98**;
đã bỏ 22 lỗi NewApi, không suppress 179 lỗi MissingTranslation có sẵn.
ADB hiện không nhận thiết bị; chưa cài/chạy/chụp ảnh UI sau sửa, không dùng
PASS QR lịch sử thay cho hồi quy mới. Chi tiết, file thay đổi và checklist:
[UI_UX_AUDIT.md](UI_UX_AUDIT.md). Không commit/push/deploy.

## Lịch sử — Sửa và kiểm thử QR, 20/10/2026

**PASS nghiệm thu QR trong phạm vi QA USB + AVD, chưa PASS toàn dự án/production.**
USB Xiaomi Android 15 đạt 7/7 regression/UI (20,863s), chọn ảnh thực qua picker
1/1 (12,990s), quét QR quang học + flash 1/1 (6,881s). Dùng mã khác nhau cho
camera/gallery để loại READY cũ; ảnh đã review, phiên cá nhân giữ nguyên,
không gửi lời mời thật. Đã xóa đúng hai ảnh QA tạm khỏi Downloads, có bản sao
local. Lint vẫn FAIL 201 lỗi ngoài QR. [QA_QR.md](QA_QR.md).

Kết quả AVD trước đó:
Người dùng đã cho phép tự chạy máy ảo sau khi USB mất kết nối. AVD QA riêng
đạt 7/7 regression/UI (44,265s) + 1/1 từ chối/cấp Camera qua dialog Android
(14,942s), 8 ảnh QA. Đã sửa thêm lỗi scanner không tiếp tục sau đóng My QR;
sửa harness chờ picker thật trước Back. 118 JVM test, APK debug/test/release
unsigned đạt. Lint debug/release vẫn FAIL 201 lỗi ngoài QR; không claim
production PASS. Không commit/push/deploy/backend/database;
không reset PIN hay wipe dữ liệu AVD gốc.

## Lịch sử — Triển khai CHAT-01–05, 20/10/2026

**PASS CHAT-01–05 trong phạm vi local/USB; chưa nghiệm thu production.** Người dùng đã duyệt cả năm tiểu bước; không mở emulator/commit/push/deploy. Đã triển khai V4/REST và Android 5 tab/Inbox/New Chat/Chat text–moment/FCM. Backend 163/163, Android 115 JVM + build/lint debug/release đạt. Chat USB 6/6 (36,605s), FCM cloud + notification nền chung 1/1 (3,354s), hồi quy Media/List/Profile 11/11 (167,056s) đạt; phiên cá nhân byte-for-byte không thay đổi. Đã sửa helper mở Activity/chờ lifecycle trên MIUI để chạy lại hồi quy, không bỏ assertion hoặc sửa production Viewer. Xem [QA_CHAT.md](QA_CHAT.md), [CHAT_API.md](CHAT_API.md).

V4 chỉ được chạy trên PostgreSQL test mới; server/VPS và database production chưa cập nhật. App đã cài APK debug mới nhưng các endpoint chat trên môi trường người dùng trỏ tới chỉ có sau khi backend tương ứng được cập nhật theo yêu cầu riêng.

Bản bàn giao cuối: build toàn bộ 37s đạt; Chat USB chạy lại 6/6 (35,025s) sau sửa helper cold-start xác minh shell/instance. Log cuối `chat-usb-final-acceptance.log`; các timeout trước được giữ và giải thích tại QA_CHAT, không tính là PASS.

## Lịch sử — Kế hoạch triển khai Module Tin nhắn & Chat khoảnh khắc (20/10/2026)

**ĐÃ BỔ SUNG MODULE TIN NHẮN VÀO ROADMAP TRIỂN KHAI THEO CHUẨN AGENTS.MD.**
- Nguồn yêu cầu: Người dùng yêu cầu đưa module Tin nhắn (thiết kế tham chiếu 5 màn hình: Hộp thư đến, Khung chat 1-1, Xử lý bàn phím IME, Soạn tin mới chọn bạn bè, Trạng thái rỗng) vào kế hoạch triển khai.
- Thay đổi cấu trúc điều hướng: Bottom Navigation Bar mở rộng từ 4 tab thành **5 tab**: Camera (0) | Lịch sử (1) | **Tin nhắn (2)** | Bạn bè (3) | Hồ sơ (4).
- Phân rã 5 tiểu bước độc lập:
  - `CHAT-01`: Backend Database & REST API (Spring Boot 3, Flyway Migration `V4__direct_messaging.sql`, bảng `conversations` & `messages`, REST APIs, phân quyền bạn bè & chặn 2 chiều, FCM push `chat_message`, Integration tests).
  - `CHAT-02`: Android Navigation 5 Tabs & Messages Inbox UI (Màn 1 & 5). Menu `main_tabs.xml` 5 items, `MainActivity` & `MainPagerAdapter` 5 fragments, `MessagesFragment` & `fragment_messages.xml`, danh sách hội thoại, avatar bạn bè, last message, relative time, unread badge, tìm kiếm, nút `+`, empty state + CTA "Tìm bạn ngay", 4 theme.
  - `CHAT-03`: Android New Chat / Select Friend UI (Màn 4). `NewChatActivity` & `activity_new_chat.xml`, danh sách bạn bè, tìm kiếm bạn, khởi tạo chat 1-1, 4 theme.
  - `CHAT-04`: Android Chat Room UI & Send Text/Moment (Màn 2 & 3). `ChatActivity` & `activity_chat.xml`, bong bóng chat text/moment, trạng thái "Đã xem", thanh input bar gửi text/ảnh khoảnh khắc, WindowInsets bàn phím ảo IME không che nội dung, 4 theme.
  - `CHAT-05`: Realtime Sync & FCM Integration & UI Automation Tests. Lắng nghe FCM `chat_message` qua `PushEventBus` và `TaktakMessagingService`, đồng bộ realtime khi đang mở chat, cập nhật unread badge, Local Notification khi chạy ngầm, bộ test tự động `ChatThemeUiTest` trên AVD qua 4 theme.
- Trạng thái hiện tại: **Chờ người dùng phê duyệt bắt đầu tiểu bước CHAT-01 (Backend Database & REST API)**.

## Lịch sử — Nghiệm thu UI-THEME-08 (Profile screen, Avatar upload & Logout 4 theme), 20/10/2026

**ĐÃ NGHIỆM THU UI-THEME-08 (Profile screen, Avatar upload & Logout bốn theme): PASS TOÀN DIỆN.**
- Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Hồ sơ cá nhân (`fragment_profile.xml`), bao gồm thông tin tài khoản, chỉnh sửa tên hiển thị (display name), tải lên ảnh đại diện (avatar upload), chuyển giao diện (Theme settings) và đăng xuất an toàn (logout).
- Thành phần:
  - Theme tokens & styles: Bổ sung `<attr name="ttMediaOutlineStyle" format="reference" />` và style `@style/Widget.TakTakMedia.Outline` trong `res/values/media_themes.xml`; cấu hình trên 4 theme và khai báo fallback an toàn trong `Base.Theme.TakTak` (`res/values/themes.xml`).
  - Layout: `fragment_profile.xml` gỡ bỏ hardcoded `@style/Theme.TakTak.Premium`, liên kết động với tokens (`ttMediaHeadlineStyle`, `ttMediaTonalStyle`, `ttMediaFieldStyle`, `ttMediaInputStyle`, `ttMediaPrimaryStyle`, `ttMediaOutlineStyle`, `colorPrimary`, `colorOnPrimary`).
  - Fixture: `MediaThemeFixture` hỗ trợ mock UserApi (`me`, `update`, `uploadAvatar`, `avatar`) và DeviceApi (`devices`) để test tải hồ sơ, đổi avatar, sửa tên và đăng xuất không phụ thuộc mạng ngoại vi.
- Kiểm tra thực tế:
  - Android Emulator `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM):
    - `ProfileThemeUiTest`: **3/3 tests PASS (24.209s)** (72 tổ hợp layout bounds/WCAG contrast >= 4.5:1, chỉnh sửa tên hiển thị trên 4 theme & khôi phục sau recreate, tải avatar JPEG & đăng xuất an toàn xóa phiên).
    - `MediaThemeUiTest`: **5/5 tests PASS (114.147s)** (kiểm tra hồi quy Media và xác nhận `fragment_profile` cùng `fragment_history`, `fragment_friends` đều áp dụng context theme động).
    - `ListThemeUiTest`: **4/4 tests PASS (29.027s)** (kiểm tra hồi quy Lists).
  - Android JVM: **105/105 tests PASS** (27s).
  - Spring Boot Server: **152/152 tests PASS** (1m 39s).
- Emulator AVD đã được đóng an toàn sau kiểm thử qua `adb emu kill`.
- Báo cáo chi tiết: [QA_THEME_PROFILE.md](QA_THEME_PROFILE.md).
- Không commit/push/deploy. Dừng chờ chỉ đạo tiếp theo của người dùng.

## Lịch sử — Nghiệm thu UI-THEME-07 (History, Friends & List Items 4 theme), 19/10/2026

**ĐÃ NGHIỆM THU UI-THEME-07 (History, Friends & List Items bốn theme): PASS TOÀN DIỆN.**
- Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Lịch sử (Đã nhận / Đã gửi), tab Bạn bè, các danh sách (Moments, Invitations, Friends, Search results, Section headers, Footers) và hộp thoại xác nhận.
- Thành phần:
  - Theme tokens & styles: Bổ sung 7 attributes/tokens trong `res/values/media_themes.xml` (`ttMediaPrimaryStyle`, `ttMediaTonalStyle`, `ttMediaCardBackground`, `ttMediaTabIndicator`, `ttMediaTabSelectedColor`, `ttMediaTabUnselectedColor`, `ttMediaDividerColor`, `ttMediaNoticeBackground`) và fallback đầy đủ trong `res/values/themes.xml`.
  - Drawables: `res/drawable/media_tab_indicator.xml`.
  - Layouts: `fragment_history.xml`, `fragment_friends.xml`, `item_friend.xml`, `item_friends_section.xml`, `item_history_footer.xml` đã gỡ bỏ hardcoded theme, liên kết động với tokens.
  - Sửa lỗi layout: `item_history_moment.xml` tối ưu `momentSenderUsername` với `layout_weight="1"` và `singleLine="true"`, giải quyết dứt điểm lỗi tràn biên ngang trên viewport hẹp 280dp với font trợ năng 2.0x.
- Kiểm tra thực tế:
  - Android Emulator `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM):
    - `ListThemeUiTest`: **4/4 tests PASS (31.607s)** (144 tổ hợp layout, chuyển tab Đã nhận / Đã gửi & khôi phục khi đổi theme, danh sách bạn bè & tìm kiếm & hộp thoại MaterialAlertDialog, tương tác mở MomentViewerActivity).
    - `MediaThemeUiTest`: **5/5 tests PASS (116.985s)** (không hồi quy Media).
  - Android JVM: **105/105 tests PASS** (12s).
  - Spring Boot Server: **152/152 tests PASS** (2m 01s).
- Emulator AVD đã được đóng an toàn sau kiểm thử qua `adb emu kill`.
- Báo cáo chi tiết: [QA_THEME_LISTS.md](QA_THEME_LISTS.md).
- Không commit/push/deploy. Dừng chờ chỉ đạo tiếp theo của người dùng.

## Hiện hành — Nghiệm thu UI-THEME-06 (Camera / Preview / Navigation / Viewer 4 theme), 19/10/2026

**ĐÃ NGHIỆM THU UI-THEME-06 (Media bốn theme): PASS TOÀN DIỆN.**
- Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho 3 screen roles: `MAIN`, `PREVIEW`, `VIEWER`.
- Thành phần:
  - Theme tokens & styles: `res/values/media_themes.xml` với đầy đủ Light/Dark mode.
  - Native background: `MediaBackdropView.java` vẽ shader 60fps, không chụp/blur bitmap.
  - ThemeManager: Mở rộng ScreenRole cho `MAIN`, `PREVIEW`, `VIEWER`.
  - Layouts: `activity_main.xml` (bottom navigation 4 tabs, theme styling), `fragment_camera_page.xml` / `fragment_camera.xml` (CameraX 1:1, shutter/controls), `activity_preview.xml` (bản nháp 1:1, caption, recipients, retry retention), `activity_moment_viewer.xml` (top bar, 1:1 photo card, caption card, responsive ScrollView chống nghẽn layout ở mọi viewport và fontScale 2.0x).
- Kiểm tra thực tế:
  - Android Emulator AVD `Medium_Phone` (Android 17 / API 37): `MediaThemeUiTest` **5/5 tests PASS (138.247s)**. 144 tổ hợp layout đạt chuẩn WCAG tương phản >= 4.5:1, không tràn viền/cắt chữ; draft/caption/recipients/retry/survive recreation đạt; camera navigation & settings đạt; 1 real QA capture 1:1 & retake cache discard đạt; viewer dialog/revocation 403 ẩn ảnh riêng tư đạt.
  - Android JVM: **105/105 tests PASS** (14 suites).
  - Server Spring Boot: **152/152 tests PASS**.
- Emulator AVD đã được đóng an toàn sau khi hoàn tất kiểm thử.
- Chi tiết file, kết quả và bằng chứng: [QA_THEME_MEDIA.md](QA_THEME_MEDIA.md).
- Không commit/push/deploy. Dừng chờ chỉ đạo tiếp theo của người dùng.

## Hiện hành — Đóng nghiệm thu Feature B, 19/10/2026

**ĐÃ ĐÓNG NGHIỆM THU FEATURE B (Validation đăng ký đầy đủ): PASS TOÀN DIỆN.**
6: Đã hoàn thành và xác minh đầy đủ 7 hạng mục:
7: 1. Username min length: Ít nhất 3 ký tự (tối đa 255), chỉ `[a-z0-9_]`.
8: 2. Username không bắt đầu/kết thúc bằng `_`, không có `__` liên tiếp; regex `^(?!.*__)[a-z0-9][a-z0-9_]{1,253}[a-z0-9]$`.
9: 3. Display name tối thiểu 1 ký tự sau strip (`@NotBlank @Size(max = 255)`).
10: 4. Phân biệt mã lỗi 409 PostgreSQL (SQLState 23505): `EMAIL_CONFLICT` (*"Email đã được sử dụng."*) vs `USERNAME_CONFLICT` (*"Tên người dùng đã được sử dụng."*), fallback `ACCOUNT_CONFLICT`. Android client tự động highlight/focus trường bị trùng tương ứng.
11: 5. Inline validation: Lắng nghe `onFocusChangeListener` trên cả 5 trường (`displayName`, `username`, `email`, `password`, `confirmPassword`); TextWatcher xóa lỗi cũ khi bắt đầu gõ.
12: 6. Password strength indicator: `LinearProgressIndicator` + `MaterialTextView` trong `activity_register.xml` đánh giá 3 mức (Yếu/Trung bình/Mạnh) tương thích 4 theme.
13: 7. Username availability check: Endpoint `GET /api/auth/check-username?username=...` không yêu cầu xác thực, `CacheControl.noStore()`. Android debounce 400ms qua `Handler`.
14: Kiểm tra thực tế:
15: - Server verify: **152/152 tests PASS** (gồm concurrency, unique constraints, curl integration, recovery, token).
16: - Android JVM: **105/105 tests PASS** (14 test suites).
17: - Android Lint: `lintDebug` **0 errors**.
18: - Android Build: `assembleDebug` APK **BUILD SUCCESSFUL**.
19: Không commit/push/deploy. Nghiệm thu hoàn tất.

## Hiện hành — UI-THEME-05, 19/10/2026

**PASS phạm vi Register/Recovery bốn theme.** Register/Forgot/Verify/Change dùng AUTH theme trước inflate, Original giữ Premium, ba theme mới đồng bộ Login; không recreate form trên onResume. Đã sửa placeholder sáng từ ảnh thật và bổ sung test tương phản input. Build cuối45s, **101/101 JVM**, lint0errors/21debug+29release warnings; **8/8 USB,110.116s** trên Xiaomi Android15,32lượt form và144layout/font/night combinations. Restore appearance và bytes phiên đạt trong batch cuối; ghi riêng các lượt lỗi MIUI và startup session comparison, không bỏ assertion.38ảnh/log/file/giới hạn: [QA_THEME_AUTH.md](QA_THEME_AUTH.md). Không mở emulator,không đổi backend/DB/API/VM/session,không gửi Gmail/reset thật. Không commit/push/deploy/sang06; Gmail blocker cũ giữ nguyên. Các mục dưới đây là lịch sử.

## Hiện hành — UI-THEME-04, 19/10/2026

**PASS trong phạm vi Login bốn theme đã được duyệt.** Original giữ fixed-dark; Gradient/Glass/Soft hỗ trợ sáng/tối, dùng token và nền native. Áp trước inflate; đổi từ chooser hoãn khi có draft/loading, không xóa form hoặc ghi phiên. Các role khác giữ fallback Original. Build debug/test/release cuối39s, **101/101 JVM**, lint0errors (20debug/29release warnings); **20/20 UI AVD,124.809s**,72tổ hợp layout Login đạt. Đã sửa lỗi tương phản CTA Gradient sáng từ lượt19/20, không bỏ test. Xem [QA_THEME_LOGIN.md](QA_THEME_LOGIN.md) để biết file, log,11ảnh Login/IME, các lượt LOW_MEMORY và giới hạn. Backend/DB/API/VM/Repository/JWT/FCM/widget không đổi. Hồ sơ bên dưới là lịch sử. Không commit/push/deploy hoặc sang05; Gmail blocker cũ giữ nguyên.

## Hiện hành — UI-THEME-03,19/10/2026

**PASS phạm vi UI-THEME-03.** Người dùng duyệt chooser/preview/Apply/Cancel/entrypoints. Đã thêm Activity/LiveData ViewModel/Canvas preview/4palette chooser, lối mở Login/Hồ sơ; chỉ chooser recreate sau save,các màn khác giữ Original. App review build58s,full tasks với helper39s,test APK cuối23s;101/101JVM,lint0errors/20debug+29release warnings cũ. Batch cuối **15/15PASS110.614s** (7chooser+4prefs+4Login),gồm IME thật;72tổ hợp layout/font/night đạt,system bars đã sửa từ ảnhQA. Sửa helper Login bắt đúng instance/có focus và tách launcher routing khỏi test AVD,không bỏ assertion. Chi tiết file/log/ảnh/các lượt lỗi môi trường và giới hạn tại [QA_THEME_CHOOSER.md](QA_THEME_CHOOSER.md). Không sang04,không commit/push/deploy; giữ blocker Gmail riêng. Các mục02 bên dưới là lịch sử.

## Hiện hành — UI-THEME-02,19/10/2026

**PASS foundation theo quyền mới nhất của người dùng.** Thêm7 class Java IDs/mode/selection/store/preferences/repository/manager, file settings riêng không đụng phiên; resolver giữ Original theo role và khai báo rõ3style mới chưa khả dụng. Chưa chooser, chưa đổi màn/resources/Manifest hoặc nối manager production, không tự sang03. Build debug/test/release và lint đạt; **93/93 JVM**, AVD **8/8** (4preferences+4Login), probe persistence qua2process **1/1+1/1 PASS**. Lint0errors,20/29warnings cũ. Hash293file source/script/deploy cũ giữ nguyên. Chi tiết/file/log/giới hạn [QA_THEME_FOUNDATION.md](QA_THEME_FOUNDATION.md). Không commit/push/deploy. Đề xuất03 và dừng chờ duyệt; Gmail blocker cũ giữ nguyên. Mục01 bên dưới là lịch sử trước quyền02.

## Hiện hành — UI-THEME-01,19/10/2026

**PASS phạm vi audit/kế hoạch**, không phải PASS Theme Engine. User duyệt chỉ01,cho chạy máy ảo thay điện thoại. Audit resources/Activity/Fragment/draft/widget và3ảnh, phát hiện prompt đảo ảnh1/3 so với phong cách; đề xuất Gradient=ảnh3,Glass=ảnh2,Soft=ảnh1 chờ duyệt. Dùng ui-ux-pro-max và android_ui_verification;92resource XML hợp lệ,Original baseline AVD4/4 PASS21.68giây sau sửa settingIME trong AVD,không sửa appsource. Không build/test3theme chưa tồn tại. Chỉ cập nhật4filedocs; [UI_THEME_AUDIT.md](UI_THEME_AUDIT.md) có architecture/tokens/files/lifecycle/gates01–09. Dừng chờ UI-THEME-02,không tự triển khai. Recovery/Gmail bên dưới vẫn BLOCKED App Password,không bị coi đã hoàn tất.

## Hiện hành — Recovery A,19/10/2026

Đã triển khai xác minh email trước recovery và thu hồi ngay mọi phiên sau reset. Backend regression **151/151 PASS + JAR** từ lượt trước. Lượt tiếp nối USB phát hiện và sửa expired-notice bị LiveData idle ẩn; bản cuối **79/79 Android JVM + assemble debug/test/release + lint PASS**, **10/10 USB PASS32.942giây**,5ảnh đã xem; tài khoản cá nhân không bị logout/reset. **BLOCKED riêng Gmail thật**: file local đã được ignore/nạp an toàn, nhưng verification gửi thật trả503 và connection-only probe báoAuthenticationFailedException; chưa có SMTP accept/inbox confirmation. Người dùng xác nhận chưa tạo App Password, đang chờ thao tác này. Không làm B/commit/push/deploy; V1/V2/DB production không sửa. Chi tiết [ACCOUNT_RECOVERY.md](ACCOUNT_RECOVERY.md).

Cập nhật: 18/10/2026 (Asia/Saigon). Nguồn: workspace/Git, bằng chứng kiểm thử, ảnh Firebase Console và xác nhận thủ công của người dùng.

## Trạng thái và quyền thực hiện

- **Hiện hành — redesign5 màn theo ảnh,18/10/2026:** người dùng xác nhận cả Register/Camera/History/Friends/Profile và navbar; quyền thay scope Login-only cho hạng mục này. PASS build/unit/UI USB:72JVM,7/7 UI gồm hồi quy Login,54 tổ hợp layout/font1.5/light-dark; lint0errors,debug18/release27warnings. Sửa thêm lỗi UI ảnh History height0, không đổi loader/API. Camera live đã xem lại sau ổn định Surface. Không sửa backend/data/VM/Repository/JWT/FCM/widget, không mở emulator; không sửa tài khoản cá nhân/commit/push/deploy. Xem [QA_PREMIUM_FIVE_SCREENS.md](QA_PREMIUM_FIVE_SCREENS.md). Dừng để người dùng duyệt5 màn.
- **Hiện hành — Login theo ảnh tham chiếu,18/10/2026:** triển khai theme tối riêng, logo gốc, gradient/form native và edge-to-edge; giữ auth/API/FCM/backend. Theo yêu cầu mới đã tắt emulator, cài cập nhật giữ dữ liệu/API origin trên điện thoại USB Android15. Build/lint debug/release PASS (0errors/15warnings),72JVM và4/4UI USB PASS; có5 screenshot cuối. API/retry trên bản cuối ở điện thoại cá nhân chưa nghiệm thu để không thay phiên. HEAD lúc bắt đầu `f73b43d`;không commit/push/deploy. Xem [QA_LOGIN_REFERENCE.md](QA_LOGIN_REFERENCE.md). Các quyền/mốc bên dưới là lịch sử, không mở rộng phạm vi hiện tại.
- **Hiện tại: Hồ sơ/avatar PASS local/AVD; Firebase cloud + widget PASS trên AVD.** Backend136tests/JAR, Android72JVM/build/lint đạt (0 errors/9warnings mỗi variant), batch6/6 mới và11contract cóavatar đạt. Đã thay tab Hồ sơ placeholder, upload/tải avatar private, nối vào Bạn bè/Lịch sử/Viewer; giữ schema/stack. FCM gửi/nhận thực qua Firebase và WorkManager cập nhật QA RemoteViews foreground/background/xóa/logout. Người dùng chọn emulator trước; **điện thoại/launcher OEM và VPS/production chưa nghiệm thu**. Toàn bộ sửa review đang có được giữ; HEAD6a7f51e, không commit/push/deploy. Xem [QA_PROFILE_AVATAR_FCM.md](QA_PROFILE_AVATAR_FCM.md). Các mốc bên dưới là lịch sử, không thay bằng chứng hiện hành.
- **Mốc trước: 8B — Nginx Reverse Proxy, TLS Let's Encrypt & Tường lửa UFW: PASS.** Người dùng duyệt triển khai 8B sau 8A. Hoàn thiện toàn bộ hạ tầng bảo mật mạng và dịch vụ cho Taktak trên VPS Ubuntu: cấu hình Nginx reverse proxy (`deploy/nginx/taktak.conf`) chuyển tiếp an toàn tới Spring Boot `127.0.0.1:8080`, chuyển hướng HTTP port 80 sang HTTPS port 443 bằng 301, hỗ trợ TLSv1.2 & TLSv1.3 Let's Encrypt Certbot, HSTS (`max-age=31536000`), Security Headers (`X-Frame-Options DENY`, `X-Content-Type-Options nosniff`, `X-XSS-Protection`); cơ chế Rate Limiting phân tầng (API chung: 10 req/s burst 20; Auth: 5 req/phút burst 5 chống brute-force) trả mã 429; giới hạn upload `client_max_body_size 10M`, timeout hợp lý (30s-60s); bảo vệ tuyệt đối thư mục ảnh riêng tư (không tạo alias hay static mapping tới `/var/taktak/private-images`, chặn toàn bộ dotfiles); script cài đặt SSL `deploy/scripts/setup-ssl.sh` và script tường lửa `deploy/scripts/setup-ufw.sh` (mở 22, 80, 443; chặn 8080 & 5432); cấu hình xoay vòng nhật ký `deploy/logrotate/taktak` cho app và Nginx; giải pháp sao lưu & khôi phục tự động toàn diện: `deploy/scripts/backup.sh` (PostgreSQL dump + private images tarball + env backup, retention 14 ngày, phân quyền 700), `deploy/scripts/restore.sh` (xác nhận an toàn, khôi phục database, giải nén ảnh, cấp lại `chmod 700 taktak:taktak`, kiểm tra healthcheck), và systemd timer `taktak-backup.timer` kích hoạt hàng ngày lúc 02:00 AM; cập nhật tài liệu [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md). Tất cả các script bash đều PASS cú pháp (`bash -n`), Backend giữ vững **123/123 tests PASS** (`mvn test`), build executable JAR thành công. Xem [QA_NGINX_SSL_BACKUP.md](QA_NGINX_SSL_BACKUP.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 8A — Chuẩn bị môi trường VPS Ubuntu & Cấu hình dịch vụ (VPS Ubuntu Setup & System Services): PASS.** Người dùng duyệt triển khai 8A sau khi hoàn tất Giai đoạn 7. Chuẩn bị hoàn chỉnh môi trường máy chủ VPS Ubuntu 22.04 / 24.04 LTS cho Taktak Server: thiết lập user hệ thống non-root `taktak`; thiết lập thư mục lưu trữ ảnh riêng tư `IMAGE_STORAGE_ROOT=/var/taktak/private-images` nằm hoàn toàn ngoài web root công khai của Nginx, phân quyền nghiêm ngặt `chmod 700` thuộc sở hữu `taktak:taktak`; cấu hình PostgreSQL chỉ lắng nghe local loopback `127.0.0.1` hoặc private docker network (tuyệt đối không expose port 5432 ra internet); xây dựng `application-prod.yml` nạp toàn bộ cấu hình bí mật qua biến môi trường (DB URL/credentials, JWT access secret, image storage root, FCM service account), cấm `ddl-auto=update` (bắt buộc `validate` kết hợp Flyway migration V1, V2); cung cấp file dịch vụ systemd sandbox hardening `taktak.service` và giải pháp container hóa `Dockerfile` (multi-stage build JRE 17 Alpine non-root) + `docker-compose.yml` có healthcheck tự động; script tự động hóa `deploy/scripts/setup-vps.sh` và tài liệu [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md); cấu hình tường lửa UFW chặn toàn bộ truy cập internet vào port 8080 và 5432. Backend đạt **123/123 tests PASS** (`mvn test`), build executable JAR thành công. Xem [QA_VPS_SETUP.md](QA_VPS_SETUP.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 7C — Tải ảnh vào bộ sưu tập thiết bị (Save to Device Gallery): PASS local/AVD.** Người dùng duyệt triển khai 7C sau 7B. Android Client tích hợp tính năng tải ảnh an toàn vào Thư viện ảnh của thiết bị (`MediaStore`) qua lớp tiện ích `GallerySaver` (`Pictures/Taktak` theo chuẩn Scoped Storage trên Android 10+ / API 29+ và tương thích ngược Android 8–9 với quyền `WRITE_EXTERNAL_STORAGE` có `maxSdkVersion="28"`). Nút lưu ảnh (`ic_download.xml`, `viewerSaveButton`) hiển thị trên thanh tiêu đề của `MomentViewerActivity` cho cả người gửi và người nhận khi xem khoảnh khắc; ảnh riêng tư và ảnh nhận được tuyệt đối không tự động xuất ra bộ sưu tập khi chưa có hành động rõ ràng từ người dùng (User Action). Xử lý luồng lưu ảnh trên worker thread: ưu tiên ghi bytes JPEG gốc tải từ server hoặc nén từ RAM cache nếu offline, hiển thị Toast trạng thái tiếng Việt ("Đang lưu ảnh…", "Đã lưu ảnh vào Bộ sưu tập.", "Không thể lưu ảnh vào máy. Vui lòng thử lại."). Backend giữ nguyên **123/123 tests PASS** (`mvn verify`), Android client đạt **66/66 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **12 connected acceptance tests PASS** trên Android Emulator `Medium_Phone` (Android 17 / API 37) qua ADB reverse 8080 (`saveMomentToGalleryOnUserActionSavesValidJpegToMediaStore`, `emptyJpegBytesThrowsIOExceptionOnGallerySave`, `momentDeletionBySenderRevokesRecipientAccessImmediately`, `momentDeletionNetworkFailurePreservesLocalCache`, `sentMomentsEndToEndWithBackend`, `momentViewerActivityRendersAndCachesImage`, `widgetUpdaterRendersAllStatesAndDecodesBitmap`, `latestMomentEndToEndWithBackend`, `multiDeviceRegistrationForSameAccount`, `deviceRegistrationAndUnregistrationEndToEnd`, `pushEventBusDispatchesEventsToListeners`, `momentUploadAndDeleteEndToEnd`). Dọn dẹp hoàn tất: smoke server exit 0, đóng port 8080, gỡ reverse, tắt emulator. Xem [QA_SAVE_TO_GALLERY.md](QA_SAVE_TO_GALLERY.md). Hoàn tất toàn bộ Giai đoạn 7. Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 7B — Xóa khoảnh khắc phía máy chủ & Thu hồi quyền tức thời (Server-side Delete & Revocation): PASS local/AVD.** Người dùng duyệt triển khai 7B sau 7A. Backend Spring Boot hỗ trợ hoàn chỉnh endpoint `DELETE /api/moments/{id}`: xác thực danh tính người gửi qua JWT subject, chỉ cho phép sender xóa (`403 FORBIDDEN` đối với non-sender), thu hồi quyền tức thời (cập nhật soft delete `deleted_at = now()`, xóa an toàn file ảnh khỏi đĩa private VPS `storage.discard(filePath)` khiến mọi request `GET /{id}/image` lập tức trả về `404 NOT_FOUND` cho cả sender và recipients), phát thông báo FCM data message `{type: "moment_deleted", momentId: id}` tới toàn bộ thiết bị của recipients sau khi database transaction commit (`TransactionSynchronization.afterCommit`); các truy vấn `latestMoment`, `listMoments(received)`, và `listMoments(sent)` tự động loại bỏ các ảnh đã xóa mềm. Android Client tích hợp nút xóa thùng rác (`ic_delete.xml`, `viewerDeleteButton`) trên `viewerTopBar` của `MomentViewerActivity` hiển thị khi `isSender == true`; kích hoạt hộp thoại xác nhận MaterialAlertDialog; khi xóa thành công gọi `HistoryImageCache.remove(momentId)`, phát `PushEventBus.emitMomentDeleted(momentId)` (cập nhật UI danh sách tức thì 0 ms), và kích hoạt `WidgetUpdater.enqueueOneTimeWork(context)` để widget đồng bộ về ảnh tiếp theo hoặc placeholder rỗng; xử lý lỗi mạng/offline an toàn: hiển thị dialog thông báo lỗi kết nối mạng, tuyệt đối không xóa local cache hay danh sách hiển thị khi xóa thất bại. Backend đạt **123/123 tests PASS** (`mvn verify`), Android client đạt **66/66 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **10 connected acceptance tests PASS** trên Android Emulator `Medium_Phone` (Android 17 / API 37) qua ADB reverse 8080 (`momentDeletionBySenderRevokesRecipientAccessImmediately`, `momentDeletionNetworkFailurePreservesLocalCache`, `sentMomentsEndToEndWithBackend`, `momentViewerActivityRendersAndCachesImage`, `widgetUpdaterRendersAllStatesAndDecodesBitmap`, `latestMomentEndToEndWithBackend`, `multiDeviceRegistrationForSameAccount`, `deviceRegistrationAndUnregistrationEndToEnd`, `pushEventBusDispatchesEventsToListeners`, `momentUploadAndDeleteEndToEnd`). Dọn dẹp hoàn tất: smoke server exit 0, đóng port 8080, gỡ reverse, tắt emulator. Xem [QA_MOMENT_DELETION.md](QA_MOMENT_DELETION.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 7A — Màn lịch sử Đã gửi & Xem toàn màn hình (Sent Moments History & Full-screen Viewer): PASS local/AVD.** Người dùng duyệt triển khai 7A sau 6C. Backend Spring Boot hỗ trợ đầy đủ `GET /api/moments?type=sent&page=0` (truy vấn danh sách khoảnh khắc người dùng đã gửi, sắp xếp mới nhất lên đầu `created_at DESC, id DESC`, phân trang 20 items/trang kèm cờ `hasNext`, loại trừ ảnh đã xóa mềm) và quyền tải ảnh có xác thực cho người gửi qua Bearer JWT `GET /api/moments/{id}/image`. Android Client tích hợp đa tab `TabLayout` trong `HistoryFragment` (Tab 0: "Đã nhận", Tab 1: "Đã gửi") với tiêu đề và trạng thái rỗng tự động điều chỉnh theo tab; mở rộng `HistoryViewModel` (quản lý trạng thái phân trang độc lập cho 2 tab, tự động cập nhật khi nhận push sự kiện); triển khai bộ nhớ đệm an toàn `HistoryImageCache` (LruCache max 20 MB); xây dựng màn hình xem toàn màn hình `MomentViewerActivity` hiển thị chi tiết người gửi (avatar, displayName, @username, timestamp tương đối), ảnh 1:1 chất lượng cao (hiển thị tức thì 0 ms từ RAM cache hoặc tải qua mạng), khung chú thích đầy đủ tối đa 50 ký tự Unicode, hỗ trợ đóng và back cử chỉ. Backend đạt **123/123 tests PASS** (`mvn verify`), Android client đạt **62/62 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **8 connected acceptance tests PASS** trên Android Emulator `Medium_Phone` (Android 17 / API 37) qua ADB reverse 8080 (`sentMomentsEndToEndWithBackend`, `momentViewerActivityRendersAndCachesImage`, `widgetUpdaterRendersAllStatesAndDecodesBitmap`, `latestMomentEndToEndWithBackend`, `multiDeviceRegistrationForSameAccount`, `deviceRegistrationAndUnregistrationEndToEnd`, `pushEventBusDispatchesEventsToListeners`, `momentUploadAndDeleteEndToEnd`). Dọn dẹp hoàn tất: smoke server exit 0, đóng port 8080, gỡ reverse, tắt emulator. Xem [QA_SENT_HISTORY_VIEWER.md](QA_SENT_HISTORY_VIEWER.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 6C — AppWidgetProvider 2×2 & WorkManager Widget Sync: PASS local/AVD.** Người dùng duyệt triển khai 6C sau 6B. Backend Spring Boot bổ sung endpoint `GET /api/moments/latest` (chỉ lấy ảnh chưa bị xóa mà viewer được phép xem, trả về `200 OK` với `MomentItem` hoặc `204 No Content` khi chưa có khoảnh khắc). Android client xây dựng App Widget 2×2 (`TaktakAppWidgetProvider`, `RemoteViews`, `widget_taktak_2x2.xml`, `widget_taktak_2x2_info.xml`) hiển thị ảnh khoảnh khắc mới nhất, tên người gửi (`displayName` / `username`), chú thích ảnh, dải gradient chân ảnh; trạng thái rỗng (placeholder camera *"Chưa có ảnh nào"*), trạng thái đăng xuất (ổ khóa *"Chưa đăng nhập"*); chạm vào widget mở app. Tuân thủ tuyệt đối quy tắc kiến trúc: `onUpdate` **chỉ enqueue WorkManager**, tuyệt đối không gọi mạng trực tiếp trong BroadcastReceiver. `WidgetUpdateWorker` tải dữ liệu qua JWT Bearer, thuật toán `decodeSampledBitmap` downsample ảnh an toàn xuống max 512×512 RGB_565 (dưới 512 KB) triệt tiêu hoàn toàn nguy cơ `TransactionTooLargeException` của Binder IPC. Kích hoạt Worker tức thì khi `TaktakMessagingService` nhận FCM data message (`moment_created`, `moment_deleted`); dự phòng định kỳ 15 phút bằng `PeriodicWorkRequest`. `SessionViewModel` tự động cập nhật widget sang trạng thái đăng xuất khi signOut. Backend đạt **122/122 tests PASS** (`mvn verify`), Android client đạt **60/60 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **6 connected acceptance tests PASS** trên Android Emulator `Medium_Phone` (Android 17 / API 37) qua ADB reverse 8080 (`widgetUpdaterRendersAllStatesAndDecodesBitmap`, `latestMomentEndToEndWithBackend`, `multiDeviceRegistrationForSameAccount`, `deviceRegistrationAndUnregistrationEndToEnd`, `pushEventBusDispatchesEventsToListeners`, `momentUploadAndDeleteEndToEnd`). Dọn dẹp hoàn tất: smoke server exit 0 (`SMOKE_MEDIA_ROWS=0 FILES=0 RECIPIENTS=1`), đóng port 8080, gỡ reverse, tắt emulator. Xem [QA_WIDGET_SYNC.md](QA_WIDGET_SYNC.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 6B — Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (Push Notification Dispatching): PASS local/AVD.** Người dùng duyệt triển khai 6B sau 6A. Backend Spring Boot kích hoạt phát FCM data message thời gian thực sau khi transaction commit (`TransactionSynchronization.afterCommit`): `moment_created` gửi tới toàn bộ thiết bị của người nhận khi đăng ảnh cho bạn bè; `recipientIds=[]` (chỉ mình tôi) và idempotency replay không phát push; endpoint `DELETE /api/moments/{id}` cho phép người sở hữu xóa ảnh (soft delete `deleted_at`, xóa file an toàn khỏi đĩa private VPS, phát push `moment_deleted` tới toàn bộ người nhận, chặn GET ảnh và loại bỏ khỏi danh sách nhận, non-owner xóa bị từ chối 403). Backend tự động dọn dẹp stale token khi Admin SDK báo `UNREGISTERED`. Android client triển khai `PushEventBus` thread-safe, `TaktakMessagingService` đón nhận data payload và phát sự kiện; `HistoryViewModel` tự động gọi `refresh()` khi có ảnh mới và tự động xóa ảnh khỏi danh sách/cache RAM khi nhận sự kiện xóa; `MomentApi` và `MomentRepository` mở rộng phương thức `delete(id)`. Backend đạt **118/118 tests PASS** (`mvn verify`), Android client đạt **56/56 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **4 connected acceptance tests PASS** trên Android Emulator `Medium_Phone` (Android 17 / API 37) qua ADB reverse 8080 (`deviceRegistrationAndUnregistrationEndToEnd`, `momentUploadAndDeleteEndToEnd`, `multiDeviceRegistrationForSameAccount`, `pushEventBusDispatchesEventsToListeners`). Dọn dẹp hoàn tất: smoke server exit 0, đóng port 8080, gỡ reverse, tắt emulator. Xem [QA_PUSH_DISPATCHING.md](QA_PUSH_DISPATCHING.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 6A — Đăng ký thiết bị & FCM Token (Device & FCM Registration): PASS local/AVD.** Người dùng duyệt triển khai Giai đoạn 6 bắt đầu từ 6A. Backend Spring Boot cung cấp `POST /api/devices` (đăng ký/cập nhật deviceId và fcmToken với logic upsert & tự động thu hồi token cũ khi chuyển nhượng thiết bị), `DELETE /api/devices/{deviceId}` (hủy đăng ký khi logout), `GET /api/devices` (liệt kê thiết bị đang hoạt động); cấu hình Firebase Admin SDK graceful fallback và PushNotificationSender abstraction. Android Client tích hợp `DeviceIdProvider`/`AppDeviceIdProvider` (UUID bền vững trong SharedPreferences), `DeviceApi`, `DeviceRepository`, `TaktakMessagingService` (`FirebaseMessagingService` với `onNewToken` và `onMessageReceived`), `SessionViewModel` tự động đồng bộ token khi đăng nhập và unregister khi đăng xuất. Backend đạt **111/111 tests PASS** (`mvn verify`), Android client đạt **49/49 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **2 connected acceptance tests PASS** trên Android Emulator `Medium_Phone` (Android 17 / API 37) qua ADB reverse 8080 (`deviceRegistrationAndUnregistrationEndToEnd` và `multiDeviceRegistrationForSameAccount`). Dọn dẹp hoàn tất: smoke server exit 0, đóng port 8080, gỡ reverse, tắt emulator. Xem [QA_DEVICE_REGISTRATION.md](QA_DEVICE_REGISTRATION.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 5E — Lịch sử ảnh đã nhận (Received Moments History): PASS local/USB.** Người dùng duyệt triển khai 5E sau 5D. Android client triển khai `HistoryFragment` hoàn chỉnh cho Tab 1 theo Material 3 Expressive, hiển thị danh sách khoảnh khắc đã nhận, avatar ký tự đầu, tên hiển thị, username, thời gian tương đối tiếng Việt ("Vừa xong", "X phút trước"...), khung ảnh vuông 1:1, chú thích (tối đa 50 ký tự Unicode), trạng thái rỗng (empty state), trạng thái lỗi/thử lại, và nút làm mới (refresh). Ảnh được tải bảo mật qua Bearer JWT `GET /api/moments/{id}/image`, giải mã Bitmap trên luồng nền worker và cache RAM 20MB qua `LruCache` trong `HistoryViewModel`. Backend Spring Boot triển khai `GET /api/moments?type=received&page=0` sử dụng native query kết nối qua index `ix_moment_recipients_user_created`, phân trang `pageSize = 20` lấy 21 dòng để tính cờ `hasNext` không cần đếm toàn bảng. Dữ liệu cô lập bảo mật tuyệt đối: ảnh riêng tư của bản thân (`recipientIds=[]`), khoảnh khắc người lạ, hoặc ảnh đã xóa mềm không bao giờ xuất hiện trong danh sách nhận. Backend đạt **104/104 tests PASS** (`mvn verify`), Android client đạt **45/45 unit tests PASS**, `assembleDebug assembleRelease lintDebug lintRelease` đạt 0 errors, **3 connected acceptance tests PASS** trên máy thật Xiaomi Redmi Note 13 4G (Android 15 / API 35) qua USB reverse 8080 (`historyTabShowsEmptyStateWhenNoMomentsReceived`, `receivedMomentAppearsInHistoryWithSenderCaptionAndDownloadedImage`, và 4-tab camera unbind regression). Kiểm tra nhất quán dữ liệu đạt `SMOKE_MEDIA_ROWS=3 FILES=3 RECIPIENTS=3`, dừng exit 0, đóng port 8080, gỡ reverse. Xem [QA_MOMENT_HISTORY.md](QA_MOMENT_HISTORY.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **Mốc trước: 5D — Upload Progress, Retry & Idempotency Key: PASS local/USB.** Người dùng duyệt riêng 5D sau 5C với yêu cầu kiến trúc: không dùng in-memory cache mà bảo đảm bền vững qua PostgreSQL, Flyway migration V2 (`V2__moment_idempotency.sql`), unique constraint `uq_moments_sender_idempotency` trên `(sender_id, idempotency_key)`, fingerprint SHA-256 (sender + caption + sorted recipients + image bytes), replay trả về 201 cùng ID, conflict trả về 409, concurrency lock an toàn. Android client dùng `CountingRequestBody` theo dõi tiến trình 0–100%, phân biệt truyền tải với xử lý máy chủ, bảo toàn key trong `SavedStateHandle`, xóa key khi đổi nội dung, retry an toàn khi mất mạng. Backend 99/99 Maven verify đạt; Android 42/42 unit tests đạt; assemble/lint debug/release đạt; 3 acceptance tests đạt trên máy Xiaomi Android 15 thật qua USB reverse; kiểm tra media consistency đạt `ROWS=2 FILES=2 RECIPIENTS=2` (0 file mồ côi, 0 duplicate moment). Xem [QA_MOMENT_IDEMPOTENCY.md](QA_MOMENT_IDEMPOTENCY.md). Giữ HEAD `ed9fde6`, không commit/push/deploy.
- **5C — gửi ảnh cho bạn bè: PASS local/USB.** Người dùng duyệt riêng 5C sau 5B. Preview gửi các bạn được chọn hoặc Tất cả đủ trang; server validate toàn bộ friendship/block hai chiều, lưu recipients cùng transaction và kiểm tra sender/recipient mỗi GET. JPEG bounded, MIME/magic bytes/EXIF/private storage được kiểm thử. Backend 95/95, Android 39/39 JVM, 15 ca USB ở lượt cuối đạt; build debug/test/release/lint đạt. Xem [QA_MOMENT_SHARING.md](QA_MOMENT_SHARING.md).
- **5B — chụp cache và Preview: PASS local/USB.** Xem [QA_PREVIEW.md](QA_PREVIEW.md).
- **5A — điều hướng bốn tab và hoàn thiện CameraX: PASS local/USB.** Xem [QA_MAIN_CAMERA.md](QA_MAIN_CAMERA.md).
- **Phạm vi mới nhất: M3E-04 — Bạn bè/Friends**, người dùng duyệt riêng màn4. **PASS local/USB**: theme M3E, card/section/avatar/action đồng bộ, header cuộn, light/dark/IME và notice/dialog. Build/lint debug/release đạt,28unit thực chạy và20ca USB đạt (4UI/layout,5core Friends,1offline/retry,10auth/Camera regression). Xem [QA_M3E_FRIENDS.md](QA_M3E_FRIENDS.md). Không đổi backend/DB/API/VM/Repository/JWT/FCM/dependency; giữ thay đổi màn1/2/3. Chưa commit/push; bốn màn hiện có đã redesign,không tạo History/Profile/widget mới. Đề xuất duy nhất quay lại **5A**, cần duyệt riêng; các mốc bên dưới là lịch sử.
- **Phạm vi mới nhất: M3E-03 — Camera + trạng thái phiên**, người dùng duyệt riêng màn 3. **PASS local/USB**: Camera-first, shutter Coral, theme M3E sáng/tối chỉ trên cây View Camera/phiên, Friends giữ theme cũ. Build/lint debug/release đạt, 28 unit thực chạy và 20 ca USB đạt gồm Camera Deny/Allow, chụp thật/offline/retry/JWT, phiên/logout và auth/navigation regression. Xem [QA_M3E_CAMERA.md](QA_M3E_CAMERA.md). Không đổi backend/DB/API/ViewModel/Repository/JWT/FCM/dependency; bảo toàn thay đổi màn1/2. Chưa commit/push; dừng chờ duyệt **M3E-04 — Bạn bè/Friends**. Các mốc bên dưới là lịch sử, không thay phạm vi mới nhất.
- **Phạm vi mới nhất: M3E-02 — Register**, người dùng duyệt riêng sau màn1. **PASS local/USB**: theme/nhóm form M3E đồng bộ Login,giữ nghiệp vụ;build/lint debug/release,28unit chạy lại,4Register +7regression USB đạt. Xem [QA_M3E_REGISTER.md](QA_M3E_REGISTER.md). Gỡ6styleAuth cũ đã hết tham chiếu;không đổi backend/DB/API/JWT/FCM/dependency. Các thay đổi M3E-01 có sẵn được bảo toàn. Chưa commit/push;dừng chờ duyệt **M3E-03 Camera + trạng thái phiên**.
- Các mốc/quyền bên dưới là lịch sử trước M3E-02.
- **Phạm vi mới nhất: M3E-01 — Foundation + Login**, được người dùng duyệt sau audit Material3Expressive. **PASS local/USB**: theme opt-in riêng Login, tokens Coral/Cream/Lavender sáng/tối, responsive/IME/cutout; build/lint debug/release,28 unit chạy lại,4 ca Login +4 regression USB đạt. Xem [QA_M3E_LOGIN.md](QA_M3E_LOGIN.md). Không đổi backend/DB/dependency/API/JWT/FCM. Dừng chờ duyệt M3E-02 Register; chưa chuyển5A. Commit/push5b6b520 đã thực hiện theo yêu cầu riêng trước phiên này; thay đổi M3E-01 chưa commit/push.
- Các mốc và quyền bên dưới là lịch sử trước M3E-01; không thay thế phạm vi mới nhất.
- Phạm vi mới nhất: người dùng duyệt riêng **4C — FriendsFragment nối API qua Repository/ViewModel**. **PASS local/USB/AVD**: search/incoming/friends, gửi/accept/reject/hủy bạn/chặn, trạng thái và phân trang, gắn generation phiên;28 unit,6 ca USB cuối,7 regression USB,offline/retry và A/USB↔B/AVD đạt. Build/lint debug/release đạt; backend verify87/87 +JAR chạy lại. Xem [QA_FRIENDS.md](QA_FRIENDS.md). Không đổi server/schema/dependency/Firebase/stack; chưa duyệt5A hoặc commit/push/merge/deploy.
- **4B PASS backend local**: receiver-only, canonical friendship/transaction, list/unfriend/block/unblock;22 test mới +65 regression tiếp tục đạt trong suite4C. Hợp đồng [FRIEND_RELATIONS_API.md](FRIEND_RELATIONS_API.md) giữ nguyên.
- **4A PASS backend local**: search/gửi/list pending, quota và block hai chiều tiếp tục đạt trong suite4B; hợp đồng [FRIEND_API.md](FRIEND_API.md) giữ nguyên.
- **UI-CAMERA PASS local/USB (lịch sử)**: build/lint debug/release,20 unit;13 ca máy thật/14 lượt thành công, Python3.12.10/CLI skill; xem [QA_CAMERA_REDESIGN.md](QA_CAMERA_REDESIGN.md). Không nhận các kiểm thử Android đó là vừa chạy lại trong4A.
- **0A: PASS trong phạm vi tài liệu/quy ước**, đã kiểm tra lại trong phiên khôi phục. Không đồng nghĩa toàn bộ Bước 0 hoặc ứng dụng đã PASS.
- Công việc khôi phục hồ sơ trước đó: **PASS**, có kết quả trong bàn giao lịch sử; không tính các phép kiểm tra cũ là vừa chạy lại.
- **0B: PASS — đăng ký/cấu hình**. Ảnh Console khớp Project ID/project number/App ID/package với JSON local, API FCM V1 Enabled. Người dùng xác nhận chưa thiết lập Auth/Firestore/Storage/Functions; đây là bằng chứng thủ công, không phải truy vấn tự động hay kiểm thử FCM runtime.
- **1A PASS local**: server Java 17/Spring Boot/Maven, Flyway 8 bảng, 6 test PostgreSQL thật PASS và JAR build được. Đây là phần độc lập Firebase.
- **1B PASS bootstrap**, có bằng chứng build/lint/UI và cấu hình Firebase khớp đăng ký. **0C/1C PASS local**: alias emulator 4/4 test PASS và máy thật Android 15 qua USB reverse 4/4 test PASS; đã cài/mở và xem UI. **Bước 0 và giai đoạn 1 hoàn tất trong phạm vi bootstrap local**. LAN/tunnel, deploy, signing và FCM runtime chưa kiểm thử.
- **2A PASS**: JPA User/Repository, POST `/api/auth/register`, validation/normalization, BCrypt cost 12, transaction và unique DB; 13 test regression tiếp tục PASS trong suite 2B.
- **2B PASS**: login, JWT HS256 mặc định 15 phút, refresh opaque/hash/rotation, logout và login rate limit IP/account. 29 test cũ tiếp tục PASS trong suite 2C; HTTP smoke ngoài test profile có bằng chứng.
- **2C PASS**: GET/PUT `/api/users/me`, danh tính từ JWT, DTO an toàn, chỉ sửa displayName trong transaction. Mốc 2C có 36/36 test và HTTP smoke ngoài test profile; các test tiếp tục nằm trong suite 2D.
- **2D PASS local**: integration/regression và curl thật cho auth/profile, expiry/forgery/duplicate/replay/logout; JSON lỗi đã chốt. Mốc 2D: 39/39 test/JAR và CurlOnly 1/1 PASS, không tính là vừa chạy lại trong 3A. Giai đoạn 2 hoàn tất backend local.
- **3A PASS UI local**: bằng chứng UI lịch sử tại QA_AUTH_UI; form nối API trong 3B, login UI máy thật kiểm tra tiếp ở 3C (không phải chạy lại toàn bộ suite 3A).
- **3B PASS local**: API/Repository/ViewModel, phiên mã hóa và refresh đồng bộ; bằng chứng lịch sử 20 unit/17 emulator test tại [QA_ANDROID_AUTH.md](QA_ANDROID_AUTH.md).
- **3C PASS**: launcher xác minh phiên, route Login/Camera, logout/retry và dọn back stack. Bằng chứng local trước đó và USB tại [QA_SESSION_ROUTING.md](QA_SESSION_ROUTING.md);6 auth regression máy thật tiếp tục PASS sau3D.
- **3D PASS**: CameraX camera sau, quyền camera, rotation/crop1:1, multipart owner-only, storage private và GET ảnh có JWT hiển thị bằng Glide. Maven46/46; Android20 unit, build/lint debug/release0 errors/3 warnings;4 ca media máy thật,1 lần lặp chụp đủ sáng và6 auth regression PASS. Xem [QA_SELF_MOMENT.md](QA_SELF_MOMENT.md).

Nghiệm thu USB mới3D: Android15/API35, không thay phiên cá nhân; chụp thật, dừng/mở HTTP connector test để thử offline/retry, byte hiển thị khớp GET server. Từ chối quyền camera có thông báo, A/B/JWT kiểm tra đúng; screenshot đủ sáng sau xác nhận người dùng đã xem. Cold signed-out1722ms về Login/no private media. Backend smoke cuối4 moment/4 file/0 recipient, exit0; reverse đã gỡ. Suite mặc định hiện31 test, chưa chạy toàn bộ31 hoặc toàn bộ UI AVD; không lấy các lượt harness fail làm PASS. Quyền CAMERA riêng của Taktak được kiểm tra denied/granted, cuối trả về granted; không đổi bảo mật USB/timeout hệ thống.

## Bằng chứng hiện tại

| Hạng mục | Quan sát thực tế | Kết luận được phép |
|---|---|---|
| Git | Thực tế M3E-03: nhánh `chore/push-current-project`, HEAD `5b6b520`, index rỗng/worktree dirty từ M3E-01/02/03; commit/push trước redesign theo yêu cầu riêng | Không tự đổi nhánh/commit/push; bảo toàn source và thay đổi có sẵn |
| Thay đổi3D | Bảo toàn dirty/untracked từ3B/3C; thêm camera/media/server storage và test | Không đổi README/migration/Firebase/dependency hoặc tự commit |
| Tài liệu 0A | Có AGENTS, README, `.gitignore`, PROJECT_DECISIONS; đã đọc toàn bộ đặc tả | Các quy ước đã hiện diện và kiểm tra lại được |
| Android | Java/XML/ViewBinding; com.taktak.app,min26; CameraX/JWT giữ nguyên, thêm FriendsFragment/Repository/ViewModel/Glide avatar local | 4C PASS UI/API;3D giữ bằng chứng lịch sử, không chạy lại chụp ảnh trong4C |
| Firebase | JSON local hợp lệ/được ignore; định danh khớp ảnh General trên Console; ảnh Cloud Messaging có V1 Enabled; xác nhận thủ công chưa thiết lập dịch vụ khác | 0B PASS đăng ký/cấu hình; chưa kiểm thử FCM runtime |
| Backend và deploy | Maven/Java17,V1 giữ8 bảng; auth/profile/media owner-only và friends/block;87/87 test/JAR chạy lại PASS trong4C, source server giữ nguyên | Friends UI4C đã có; chưa gửi ảnh bạn bè/avatar API/deploy |
| Kiểm thử ứng dụng | Bằng chứng cũ: server 6 test, Android unit mẫu 1 test, emulator alias/reverse 4 test và build/lint PASS. Phiên máy thật: 4/4 test PASS trên Android 15, host health 200, install/launch/UI PASS | Máy thật dùng USB reverse; không tính test cũ là vừa chạy lại hoặc kết luận LAN/tunnel PASS |

## Việc chưa nghiệm thu và phụ thuộc

- 0B đã nghiệm thu bằng ảnh/xác nhận người dùng theo [FIREBASE_SETUP.md](FIREBASE_SETUP.md). API key restrictions và gửi/nhận push chưa kiểm thử; không trì hoãn bootstrap để triển khai FCM runtime ngoài phạm vi.
- 0C/1C: xem [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md). Nguyên nhân alias là target SDK 37 thiếu quyền runtime `ACCESS_LOCAL_NETWORK`. Khai báo riêng debug và cấp quyền bằng UiAutomation trong test harness đã giúp cả 4 test PASS. UI bootstrap chưa gọi mạng; khi thêm request local từ UI cần xin quyền runtime trước request. Release không chứa quyền này; không thay firewall/TLS/target SDK.
- Điện thoại 23129RAA4G / Android 15 API 35 đã được kiểm tra qua USB reverse. UI test đầu tiên chậm khoảng 104 giây nhưng PASS; lần cold launch riêng khoảng 3 giây. Chưa xác định nguyên nhân chậm ban đầu; không thay cài đặt hệ thống để che kết quả. LAN/tunnel chưa chạy, không phải blocker cho kịch bản USB đã chọn.
- Android đã nghiệm thu auth/routing/logout trên AVD và máy thật Android15. Phiên UI-CAMERA bổ sung kiểm tra dialog Camera Deny→Allow/API35, sáng/tối, busy và offline/retry. Deny quyền mạng local trong auth/API37, UI429 end-to-end, API26 và runtime HTTPS thật chưa chạy; không suy rộng kết quả ra các phạm vi đó.
- Đã thay activity-ktx bằng Activity; không thêm Kotlin source. Tập phiên bản build thực tế được ghi trong PROJECT_DECISIONS. FCM auto-init tắt tới bước 6; JSON giữ nguyên.
- Backend test dùng PostgreSQL 14.22 tạm; chưa tạo database dev bền vững. Không reset hoặc sửa migration đã áp dụng ở database của người dùng.

## Kiểm tra và điểm dừng

Mốc hiện tại **8C PASS TOÀN DIỆN**, chi tiết [QA_FINAL_VERIFICATION.md](QA_FINAL_VERIFICATION.md). Đã nghiệm thu toàn diện 11 kịch bản cốt lõi đặc tả MVP, kiểm tra an toàn bảo mật APK release (chặn cleartext, không rò rỉ bí mật), hoàn thiện bộ script DevOps (cài đặt, SSL, UFW, backup/restore, update/rollback có auto-rollback khi lỗi). Backend 123/123 tests PASS, Android client 66/66 unit tests PASS. Toàn bộ 8 Giai đoạn trong Roadmap Taktak MVP đã hoàn thành 100%. Dừng chờ chỉ đạo tiếp theo của người dùng.

Kết quả kiểm tra thực tế và giới hạn nằm trong [SESSION_HANDOFF.md](SESSION_HANDOFF.md). Danh sách công việc nằm trong [TASKS.md](TASKS.md). Quyết định kiến trúc nằm trong [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md); đặc tả tại [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md).
