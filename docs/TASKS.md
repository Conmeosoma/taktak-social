# Taktak — Công việc và cổng xác nhận

## Branded intro video có tiếng — 23/10/2026

- [x] Phân tích MP4/AAC, bitstream M4A khớp; bundle nguồn nguyên byte.
- [x] Native Java/XML/ViewBinding/MediaPlayer, fit-center, lifecycle, skip/error.
- [x] Full Intro/Quick Start, sound/preview ở Profile, preferences riêng.
- [x] JVM135, build, lint0error85warning, resources2304.
- [x] USB22/22; cold riêng1/1, Home return không replay; session bytes giữ nguyên.
- [x] Người dùng xác nhận tiếng gốc, không lệch rõ; ảnh sạch đã review.
- [ ] Ma trận API26–30, DND/vibrate/focus thực, codec lỗi, stress RAM/jank.

Core Android15 đã xác minh, matrix PARTIAL; [hồ sơ](QA_BRANDED_INTRO_20261023.md).
Renderer520ms bên dưới đã được thay theo yêu cầu video mới, không tiếp tục sửa
spacing/assets của renderer cũ. Không tự chuyển bước hoặc commit/push/deploy.

## Splash native — 23/10/2026

- [x] Audit launcher/theme/session/logo; tích hợp Java/XML, không SplashActivity.
- [x] Build/debug test APK, JVM127, lint debug0error83warning, resources2256.
- [x] Sáu test animation lifecycle/reduced motion/RAM routing trên AVD đạt.
- [x] USB6/6 PASS8,281s; sửa harness launcher MIUI, giữ phiên cá nhân.
- [x] Quan sát đủ logo/tim/sparkle/brand trên USB với fixture Main.
- [ ] Sửa/duyệt góc trắng PNG và spacing accents/brand, chưa visual DONE.
- [ ] API26–30, cold Main/session thật, expired-refresh/logout và warm-start có kiểm soát.

Trạng thái **PARTIAL**, [chi tiết](QA_SPLASH_20261023.md). Không tự mở rộng animation
màn khác; contrast Friends/History vẫn là việc riêng chưa hoàn thành.

## QA USB — 23/10/2026

- [x] Nhận Xiaomi Android15, build check và install-r giữ dữ liệu; hash APK khớp.
- [x] Bộ hồi quy28/28 PASS118,724s trên điện thoại thật; phiên/appearance bảo toàn.
- [x] Review ảnh fixture và ghi rõ contrast Friends/History sáng còn tồn tại.
- [ ] Sửa palette contrast Friends/History sáng, thêm regression và chạy lại USB.

[Hồ sơ QA](QA_USB_UI_20261023.md). Không coi fixture PASS là toàn app/production PASS.

## Sửa lint/UI tồn đọng — 22/10/2026

- [x] 179 MissingTranslation: bản dịch sáu locale + guard XML/format đạt.
- [x] Icon quay lại/tạo chat, tương phản Room/Inbox/New Chat và receipt plate.
- [x] Root cause TabView/Received bị cắt ở280dp/font2; tab scrollable, không giảm font.
- [x] Bỏ streak/BFF giả, hook performClick, QR username/namespace, wording privacy/theme.
- [x] Build debug/test/JVM127; lint debug0/83, release0/90.
- [x] Runtime cuối28/28 PASS105,929s; review ảnh APK bàn giao (xem hồ sơ QA).
- [ ] Sửa palette icon QR/hint/username Friends light, QA contrast đầy đủ màn này.
- [ ] Xử lý nhóm warning còn lại với regression riêng; không suppress/xóa tùy tiện.
- [ ] Chốt cách bỏ/ghi rõ demo hoặc thay số liệu Recap mẫu bằng dữ liệu thật.
- [ ] TalkBack/RTL, review đa ngôn ngữ và launcher widget thật.

[Bằng chứng và giới hạn](QA_UI_LINT_20261022.md). Không mở rộng backend/commit/push/deploy.

## Build/QA USB — 22/10/2026

- [x] Rerun build debug/androidTest/JVM127; cài-r điện thoại, SHA APK khớp.
- [x] Chạy18 ca USB (17 đạt), diagnostic attachment riêng (FAIL); giữ phiên QA.
- [x] Chẩn đoán/sửa race harness attachment, bộ18/18 USB đạt90,45s; không sửa production.

[Bằng chứng](QA_LATEST_APK_USB_20261022.md). Không tự chuyển gate/commit/push.

## Production rollout restore-security — 21/10/2026

- [x] Người dùng duyệt, kiểm tra state/hash/permissions, backup script và unit.
- [x] Cài restore.sh + SQL + systemd drop-in, verify + daemon-reload.
- [x] Guard loaded, healthUP4 mẫu/PID giữ nguyên; JAR/env/base unit không đổi.
- [x] Shell contract chạy lại12/12; không thực thi SQL hoặc restore production.
- [ ] Production restore thật/mở lại sau restore: ngoài phạm vi, cần duyệt riêng.

[Bằng chứng](QA_RESTORE_PRODUCTION_ROLLOUT.md). Không đánh dấu gate khác đã đạt.

## RESTORE-SECURITY-01 — 21/10/2026

- [x] Full backend verify191/191, BUILD SUCCESS2 phút28 giây.

- [x] Restore fail-closed và SQL reconciliation, không tự mở lại traffic.
- [x] SQL/HTTP local4/4: baseline JWT hồi sinh, fix JWT/refresh401, credential mới giữ.
- [x] Shell contract12/12, gồm rollback và thiếu guard.
- [x] Native QA DB/ảnh/systemd: version9→10, refresh revoked, manual start bị chặn.
- [x] Production rollout script/SQL/unit theo duyệt mới; không restart/restore DB.
- [ ] Mở lại traffic sau một restore thật: cần phê duyệt riêng, chưa thực hiện.

[Báo cáo](QA_RESTORE_SECURITY.md). Không coi là đã khép các gate còn lại.

## Gate tổng hợp cập nhật — 21/10/2026

- [x] Backend187/JVM127; USB matrix2/2 và Chat fixture6/6 chạy lại đạt.
- [x] Gmail thật: reset/revocation test đạt, người dùng nhận đủ3 thư QA.
- [x] SSH điều tra; bật FCM theo duyệt, servicehealthUP.
- [x] FCM→Worker→AppWidgetHost thật1/1; foreground/background/delete/logout assertion.
- [x] Native encrypted DB/image backup/restore QA; backup hỏng bị từ chối.
- [ ] **FAIL** restore giữ revocation: security_version9→7; sửa trước public traffic.
- [ ] **FAIL** lint179 MissingTranslation mỗi variant; chưa suppress/bỏ gate.
- [ ] Widget launcher/logout visual, mixedAPK/crash/binary rollback, Chat VPS UI,
  load dài hạn/alert routing/accessibility.

[Bằng chứng](QA_ACCEPTANCE_ALL.md). Không coi checkbox lịch sử dưới đây là
nghiệm thu lại. Không đổi JAR, restore DB chính, commit/push trong lượt này.

## USB vận hành HARDENING-01 — 21/10/2026

- [x] USB cache/reply9/9; phiên cá nhân không đổi trước khi được duyệt logout.
- [x] GET HTTPS từ điện thoại3/3: health, ID server, chặn API private/management.
- [x] Chuyển QA theo quyền người dùng; sửa foreground MIUI và thu hồi bind user0.
- [ ] FCM thật: FAIL tại created-event sau upload (90s); chưa PASS widget/background.
- [ ] Đăng nhập SSH có quyền đọc outbox/FCM log để tìm nguyên nhân và chạy lại.

Không bỏ assertion hoặc giả event; không deploy/reset DB. [Chi tiết](QA_OPERATIONAL_USB.md).

## HARDENING-01 — được duyệt 21/10/2026

- [x] Outbox cùng transaction ảnh/chat, retry backoff, lease/reclaim và quyền hiện tại.
- [x] Phân biệt FCM ACCEPTED/RETRY/DISABLED/UNREGISTERED/REJECTED; không mất ảnh vì push lỗi.
- [x] Correlation ID do server sinh, metric bounded, cảnh báo backlog/5xx/pool và runbook không PII.
- [x] Load harness local riêng, baseline/after; giảm N+1 inbox, thêm query-budget regression.
- [x] Cache ảnh TTL/LRU theo phiên; không dùng cache thay kiểm tra quyền server.
- [x] Kế hoạch compatibility/rollback không hồi sinh JWT; migration V5→V6 giữ revocation được test.
- [x] Backend cuối187/187 + migration/load targeted2/2, JVM127/127, AVD17/17; hồ sơ review/handoff.
- [x] Triển khai VPS thật (`remote.io.vn` / 14.250.250.68:11918): Backup DB `taktak_pre_v6_*.dump`, cập nhật JAR mới 118MB, migrate thành công Flyway V6 (`durable push outbox`), dịch vụ `taktak.service` active, `X-Request-ID` telemetry xác nhận hoạt động 200 OK.
- [ ] Gate staging: backup/restore thực, mixed APK, restart/process crash, FCM thật và alert routing.
- [ ] Phê duyệt riêng retention tự động/replay DEAD và cấu hình ngưỡng theo SLO thực tế.

Không tự triển khai các checkbox còn mở. [Bằng chứng](QA_SYSTEM_HARDENING.md).

## Nghiệm thu USB — MOMENT-REPLY-01, 21/10/2026

- [x] Opt-in USB rõ ràng, fixture RAM, không sửa phiên hoặc gửi tin cá nhân.
- [x] Sửa harness foreground cho MIUI; APK kiểm thử build đạt 17s.
- [x] Điện thoại Android 15: 7/7 ca đạt, 36,257s; review 7 screenshot QA.
- [x] Khôi phục appearance, xác minh file phiên mã hóa không đổi từng ca và trước/sau.
- [ ] Nghiệm thu hai tài khoản qua staging/VPS V5 và FCM thật sau phê duyệt riêng.
- [ ] Rà soát riêng icon quay lại/contrast timestamp và input Chat thấy trong ảnh QA;
  ngoài phạm vi nghiệm thu chức năng reply, không đánh dấu toàn UI PASS.

Log và giới hạn: [QA_MOMENT_REPLY.md](QA_MOMENT_REPLY.md). Không commit/push/deploy.

## Hiện hành — MOMENT-REPLY-01, được duyệt 20/10/2026

- [x] Khóa phạm vi trả lời ảnh riêng vào chat1–1, không bình luận công khai.
- [x] Server MOMENT_REPLY + V5 mới, kiểm tra owner/grant/friend/block/validation/idempotency/redaction/FCM.
- [x] Android Viewer composer, MVVM/Repository, pending retry/recreation và guard process/phiên.
- [x] Chat hiển thị ảnh + reply text, không mất text khi image error hoặc thu hồi.
- [x] Full server170/170; JVM122/122; APK debug/test/release unsigned build đạt.
- [x] AVD18/18 hồi quy, bản cuối reply7/7, guard1/1; ảnh/log đã review/lưu.
- [x] Ghi lint179errors có sẵn, không đánh dấu toàn app PASS hoặc suppress.
- [ ] Nghiệm thu hai tài khoản Android→staging/VPS và FCM delivery thật sau phê duyệt riêng.

**PASS local/AVD**, không tự deploy/migrate VPS/commit/push hoặc chuyển tiểu bước.
Chi tiết [QA_MOMENT_REPLY.md](QA_MOMENT_REPLY.md). Các lỗi audit cũ vẫn giữ bên dưới.

## Lịch sử — Rà soát UI/UX và tự sửa resource an toàn, 20/10/2026

### Cập nhật AVD

- [x] Người dùng cho phép tự chạy máy ảo; dùng userdata QA riêng, không wipe/reset.
- [x] Chạy bộ UI 34 ca (31 đạt), targeted 12 ca (11 đạt), quyền riêng 1/1 đạt.
- [x] Sửa minHeight Login, Preview grid/top bar, token input và badge History bằng resource.
- [x] Build bàn giao 34s đạt; chạy mới 118 JVM tests không lỗi; lưu ảnh/log.
- [ ] Ma trận List cắt chữ ở TextView không ID: cần test Java diagnostic được duyệt.
- [ ] Chat attachment không ổn định khi chạy cả bộ: chưa giải quyết bằng retry.
- [ ] 179 lỗi bản dịch cũ; TalkBack/RTL/widget launcher/phần cứng thật chưa nghiệm thu.

Trạng thái hiện tại **IN PROGRESS/QA chưa đạt**, không còn blocker thiếu USB.
Chi tiết [UI_UX_AUDIT.md](UI_UX_AUDIT.md); các dòng dưới là snapshot trước AVD.

- [x] Đọc yêu cầu, AGENTS/tài liệu, kiểm tra dirty worktree và kiểm kê 29 layout.
- [x] Xuất bảng phát hiện trước khi sửa; giữ nguyên toàn bộ android:id và Java.
- [x] Sửa resource: tương thích API 26, vùng chạm, token/contrast, dialog và chuỗi mới bảy locale.
- [x] Build debug/test/release unsigned: BUILD SUCCESSFUL (19s).
- [x] Đọc kết quả 118 JVM test không lỗi; task cuối UP-TO-DATE, không claim chạy mới.
- [x] Chạy lint debug/release và ghi đúng lỗi còn lại; không thêm baseline/suppress.
- [ ] Xử lý 179 lỗi MissingTranslation cũ bằng bản dịch đã rà soát, không sao chép để làm xanh lint.
- [ ] Nghiệm thu runtime sau sửa: USB, bốn theme/day-night, font lớn/IME/TalkBack/RTL, QR và widget.
- [x] Ghi giới hạn ngoài phạm vi XML và checklist tại [UI_UX_AUDIT.md](UI_UX_AUDIT.md).

Trạng thái: **IN PROGRESS**, nghiệm thu thiết bị **BLOCKED** vì ADB không nhận máy.
Không đổi Java/backend hoặc mở bước roadmap mới; không commit/push/deploy.

## Lịch sử — QR được duyệt kiểm tra tất cả luồng, 20/10/2026

- [x] Tái hiện hai lỗi QR trước sửa trên USB: 2/2 FAIL có log.
- [x] Sửa phần đọc ảnh, parsing, CameraX, quyền/lifecycle, xác minh bạn bè
  và quản lý async qua Repository/ViewModel; giữ thay đổi người dùng.
- [x] 118 JVM test đạt; tạo APK debug/test/release unsigned.
- [x] Theo quyền mới chạy AVD riêng: 5 regression + 2 UI instrumentation
  PASS 7/7 (44,265s), 8 ảnh QA; thêm permission deny/grant thật PASS 1/1
  (14,942s). Sửa lỗi My QR dismiss không resume và harness Back quá sớm.
- [x] USB: 7/7 regression/UI cuối PASS (20,863s); optical/torch thật 1/1
  PASS (6,881s), picker chọn QR khác danh tính 1/1 PASS (12,990s).
  Giữ phiên cá nhân, không gửi lời mời thật; dọn đúng hai ảnh QA đã cho phép.
- [ ] Lint toàn dự án: **FAIL 201 lỗi ngoài phần QR**; không tự thay đổi
  theme/ngôn ngữ đang được người dùng chỉnh. Xem [QA_QR.md](QA_QR.md).
- [x] Nghiệm thu **phạm vi QR QA USB + AVD**, không tự đánh dấu production/toàn dự án PASS.

## Lịch sử — CHAT-01–05 được duyệt, 20/10/2026

- [x] CHAT-01: V4/REST/phân quyền/read/idempotency/FCM after-commit; 11 ca chat, full backend 163 PASS.
- [x] CHAT-02: Main 5 tab + Inbox/search/empty/unread; Chat USB đạt.
- [x] CHAT-03: chọn bạn/mở hội thoại; Chat USB đạt.
- [x] CHAT-04: text/moment private/read/retry/recreate/IME; Chat USB đạt.
- [x] CHAT-05: FCM event + sync foreground + generic notification nền; cloud thật trên USB đạt 1/1, ChatThemeUiTest 6/6.
- [x] Hồi quy Media/List/Profile USB chạy lại 11/11 PASS sau sửa helper MIUI; giữ assertion, không sửa production Viewer. Review/tài liệu bàn giao [QA_CHAT.md](QA_CHAT.md) đã cập nhật. CHAT-01–05 PASS local/USB, chưa deploy/production.

Mục CHAT chưa tích ở phần kế hoạch cũ bên dưới là lịch sử trước phê duyệt, không ghi đè kết quả hiện hành này. Không deploy/commit/push hoặc mở tiểu bước mới.

## Phạm vi hiện hành — Multi-theme,19/10/2026

- [x] UI-THEME-01: audit/kế hoạch/tokens tại [UI_THEME_AUDIT.md](UI_THEME_AUDIT.md);92XML0invalid; Original baseline AVD4/4 PASS. Không sửa source ứng dụng/backend.
- [ ] Người dùng duyệt ánh xạ Gradient=ảnh3,Glass=ảnh2,Soft=ảnh1 (prompt mô tả ảnh1/3 ngược attachments).
- [x] UI-THEME-02: người dùng đã duyệt; preferences riêng/IDs/mode/revision/manager/resolver và fallback Original đã triển khai. Build/lint0errors,93JVM,8/8AVD+hai probe1/1qua process restart đạt; xem [QA_THEME_FOUNDATION.md](QA_THEME_FOUNDATION.md). Chưa nối manager vào màn hoặc tạo3style visual.
- [x] UI-THEME-03: **PASS phạm vi chooser**.4card/preview/mode/Apply/Cancel/error/retry vàentrypoints Login/Hồ sơ;101JVM/build/lint0errors,15/15UI cuối110.614s,72layout combinations đạt.3style chưa áp lên màn nghiệp vụ. [QA_THEME_CHOOSER.md](QA_THEME_CHOOSER.md).
- [x] UI-THEME-04: **PASS phạm vi Login4theme**; Original fixed-dark, ba style mới sáng/tối; pending giữ draft/loading. Build debug/test/release,101JVM,lint0errors,20/20UI AVD124.809s;72layout Login đạt. [QA_THEME_LOGIN.md](QA_THEME_LOGIN.md).
- [x] UI-THEME-05: **PASS Register/Recovery4theme trên USB**; build45s,101JVM,lint0errors,8/8UI110.116s,32lượt form/144layout combinations,restore appearance/session byte checks đạt. Đã sửa hint contrast từ ảnh QA. [QA_THEME_AUTH.md](QA_THEME_AUTH.md).
- [x] UI-THEME-06: **PASS phạm vi Media 4 theme trên AVD**; tokens media_themes.xml, MediaBackdropView shader native, ThemeManager 3 roles (MAIN, PREVIEW, VIEWER), ScrollView responsive chống nghẽn landscape/font 2.0x; 5/5 UI tests PASS (138.247s), 105 JVM tests PASS, 152 server tests PASS. Xem [QA_THEME_MEDIA.md](QA_THEME_MEDIA.md).
- [x] UI-THEME-07: **PASS phạm vi History, Friends & List Items 4 theme trên AVD**; tokens media_themes.xml & fallback themes.xml, gỡ hardcoded theme trên fragment_history/friends/item_friend/item_friends_section/item_history_footer, fix layout weight/singleLine username chống tràn biên; 4/4 UI tests PASS (31.607s), 5/5 Media regression tests PASS (116.985s), 105 JVM tests PASS, 152 server tests PASS. Đóng AVD an toàn. Xem [QA_THEME_LISTS.md](QA_THEME_LISTS.md).
- [x] UI-THEME-08: **PASS phạm vi Profile screen, Avatar upload & Logout 4 theme trên AVD**; tokens media_themes.xml (ttMediaOutlineStyle, Widget.TakTakMedia.Outline) & fallback themes.xml, gỡ hardcoded theme trên fragment_profile, tích hợp mock UserApi/DeviceApi trong MediaThemeFixture; 3/3 Profile UI tests PASS (24.209s), 5/5 Media regression tests PASS (114.147s), 4/4 List regression tests PASS (29.027s), 105 JVM tests PASS, 152 server tests PASS. Đóng AVD an toàn. Xem [QA_THEME_PROFILE.md](QA_THEME_PROFILE.md).
- [ ] UI-THEME-09: Chuẩn bị kiểm tra và tối ưu Dark mode contrast, Dynamic color audit & WCAG compliance across all 4 themes.

Quyền chạy emulator thay điện thoại áp dụng yêu cầu mới; recovery/Gmail vẫn chờ App Password,không chạy lại gửi thư trong audit.

## Phạm vi mới nhất — 20/10/2026: Module Tin nhắn & Chat khoảnh khắc (Direct Messaging & Moment Chat)

Người dùng yêu cầu đưa module Tin nhắn (thiết kế 5 màn hình: Hộp thư đến, Khung chat 1-1 text/ảnh, Bàn phím ảo IME insets, Soạn tin mới chọn bạn bè, Trạng thái rỗng "Chưa có tin nhắn") vào kế hoạch triển khai chính thức. Điều hướng Bottom Navigation mở rộng từ 4 tab thành 5 tab: **Camera (0) | Lịch sử (1) | Tin nhắn (2) | Bạn bè (3) | Hồ sơ (4)**.

- [ ] **CHAT-01**: Backend Database & REST API (Spring Boot 3, Flyway V4, PostgreSQL). Migration `V4__direct_messaging.sql` (bảng `conversations`, `messages`), Entities/Repositories/Services/Controllers: danh sách hội thoại, tạo/lấy chat với bạn bè, lấy lịch sử tin nhắn, gửi tin nhắn text & đính kèm moment, đánh dấu đã đọc `read_at`, kiểm tra quyền bạn bè & block hai chiều, FCM data message `chat_message`. Maven verify integration tests.
- [ ] **CHAT-02**: Android Navigation 5 Tabs & Messages Inbox UI (Màn 1 & 5). Cập nhật `res/menu/main_tabs.xml` thêm `nav_messages`, điều chỉnh `MainActivity` & `MainPagerAdapter` (5 tabs). Triển khai `MessagesFragment` & `fragment_messages.xml`: danh sách hội thoại RecyclerView, avatar, last message, thời gian, unread badge, ô tìm kiếm cuộc trò chuyện, nút `+` tạo tin mới, empty state "Chưa có tin nhắn" kèm nút CTA "Tìm bạn ngay". Hỗ trợ 4 theme (Original, Gradient, Glass, Soft).
- [ ] **CHAT-03**: Android New Chat / Select Friend UI (Màn 4). Triển khai `NewChatActivity` & `activity_new_chat.xml`: danh sách bạn bè hiện có, tìm kiếm bạn bè, nhấp chọn bạn bè -> `POST /api/conversations/with-friend/{friendId}` -> mở `ChatActivity`. Hỗ trợ 4 theme.
- [ ] **CHAT-04**: Android Chat Room UI & Send Text/Moment (Màn 2 & 3). Triển khai `ChatActivity` & `activity_chat.xml`: top bar bạn bè, RecyclerView tin nhắn đa dạng (text người gửi/nhận, moment ảnh 1:1 kèm caption), trạng thái "Đã xem", thanh input bar (nút đính kèm ảnh, input text, nút gửi gradient), xử lý WindowInsets bàn phím ảo IME không che nội dung. Hỗ trợ 4 theme.
- [ ] **CHAT-05**: Realtime Sync & FCM Integration & UI Automation Tests. Lắng nghe FCM `chat_message` qua `PushEventBus` và `TaktakMessagingService`, đồng bộ realtime khi đang mở chat, cập nhật unread badge trên tab Tin nhắn và danh sách hội thoại, Local Notification khi chạy ngầm. Bộ kiểm thử tự động UI Instrumentation `ChatThemeUiTest` trên AVD qua 4 theme.

- [x] Feature A (Account Recovery & Email Verification): Backend migration V3, verification flow, temporary reset session, 151/151 tests PASS, UI USB 10/10 PASS, Gmail acceptance test PASS.
- [x] Feature B (Registration Validation): Hoàn tất đầy đủ 7 hạng mục (min 3 ký tự username, quy tắc dấu gạch dưới, min 1 ký tự displayName, phân biệt lỗi 409 EMAIL_CONFLICT vs USERNAME_CONFLICT, inline validation onFocusChange, visual indicator độ mạnh mật khẩu, debounce kiểm tra tồn tại username theo thời gian thực).
- [x] Đóng nghiệm thu Feature B: Backend verify 152/152 tests PASS; Android verify 105/105 JVM tests PASS, lint 0 errors, assembleDebug APK PASS. SIGNED OFF.
- [x] UI-THEME-06: **PASS TOÀN DIỆN Media 4 theme (Camera / Preview / Navigation / Viewer)**. 5/5 AVD UI tests PASS, 105/105 JVM tests PASS, 152/152 server tests PASS. Đóng AVD an toàn.
- [x] UI-THEME-07: **PASS TOÀN DIỆN History, Friends & List Items 4 theme**. 4/4 AVD UI tests PASS, 5/5 Media regression tests PASS, 105/105 JVM tests PASS, 152/152 server tests PASS. Đóng AVD an toàn.
- [x] UI-THEME-08: **PASS TOÀN DIỆN Profile screen, Avatar upload & Logout 4 theme**. 3/3 AVD UI tests PASS, 5/5 Media regression tests PASS, 4/4 List regression tests PASS, 105/105 JVM tests PASS, 152/152 server tests PASS. Đóng AVD an toàn. Dừng chờ duyệt bước tiếp theo.

Cập nhật: 18/10/2026. Nguồn đối chiếu: [đặc tả](TAKTAK_PROJECT.md), [trạng thái](PROJECT_STATUS.md), [bàn giao](SESSION_HANDOFF.md) và lịch sử yêu cầu trong cuộc trò chuyện.

Phạm vi hiện hành: **redesign cả5 màn Register/Camera/History/Friends/Profile theo ảnh**, đã được người dùng xác nhận sau khi làm rõ prompt Login-only. Giữ phần Login đang có, dùng USB, không mở emulator. PASS build/unit/UI7/7; chưa nghiệm thu lại các mutation/API/FCM/widget. Không tự commit/push/deploy hoặc mở chức năng tiếp theo. Xem [QA_PREMIUM_FIVE_SCREENS.md](QA_PREMIUM_FIVE_SCREENS.md).

Phạm vi trước: **Login theo ảnh tham chiếu**, chỉ Java/XML/UI; kết quả và giới hạn tại [QA_LOGIN_REFERENCE.md](QA_LOGIN_REFERENCE.md). Quyền Login-only đã được thay bằng xác nhận5 màn, không mở rộng nghiệp vụ/backend.

Quyền mới nhất: **Hồ sơ cá nhân + Avatar cá nhân + FCM/widget thực tế**, người dùng chọn emulator trước vì đang dùng điện thoại. Hồ sơ/avatar đã triển khai, Firebase cloud/QA AppWidgetHost đã test trên AVD; USB/OEM và VPS còn chờ nghiệm thu. Bằng chứng hiện hành: [QA_PROFILE_AVATAR_FCM.md](QA_PROFILE_AVATAR_FCM.md). Giữ toàn bộ bản vá review chưa commit; không commit/push/deploy hoặc tự chuyển tính năng mới.

Bản vá review được giữ nguyên; baseline hiện hành sau Hồ sơ/avatar: **136 backend/72 JVM PASS**, batch mới 6/6 và hồi quy cuối 10/10 AVD PASS, 11 contract tests mock, build/lint debug/release đạt. FCM cloud thật đã kiểm chứng tới AVD/QA widget host; chưa đóng cổng nghiệm thu điện thoại/OEM hoặc production. Bước đề xuất duy nhất: nghiệm thu điện thoại thật khi người dùng sẵn sàng.

`PASS` chỉ áp dụng cho phạm vi đã có bằng chứng. `CHƯA DUYỆT` nghĩa là không được tự triển khai; `CHƯA XÁC MINH` không đồng nghĩa thất bại.

| Mã | Công việc | Trạng thái | Bằng chứng / điều kiện |
|---|---|---|---|
| CHAT-01 | Backend Database & REST API (Spring Boot 3, Flyway V4, PostgreSQL) | CHỜ PHÊ DUYỆT BẮT ĐẦU | Migration V4 conversations/messages, REST APIs, Friend/Block Auth, FCM push, Integration tests |
| CHAT-02 | Android Navigation 5 Tabs & Messages Inbox UI (Màn 1 & 5) | KẾ HOẠCH | 5 tabs BottomNav, MessagesFragment, danh sách hội thoại, unread badge, empty state, 4 theme |
| CHAT-03 | Android New Chat / Select Friend UI (Màn 4) | KẾ HOẠCH | NewChatActivity, danh sách bạn bè, tìm kiếm bạn, khởi tạo chat, 4 theme |
| CHAT-04 | Android Chat Room UI & Send Text/Moment (Màn 2 & 3) | KẾ HOẠCH | ChatActivity, bong bóng chat text/moment, đã xem, input bar gửi text/ảnh, IME insets, 4 theme |
| CHAT-05 | Realtime Sync & FCM Integration & UI Automation Tests | KẾ HOẠCH | PushEventBus sync, realtime chat, Local Notification, UI Instrumentation test 4 theme trên AVD |
| PREMIUM-05 | Redesign Register/Camera/History/Friends/Profile và navbar theo5ảnh | PASS build/unit/UI USB |72JVM;7UI gồm Login;54 tổ hợp layout/font;0lint errors;ảnh native;QA_PREMIUM_FIVE_SCREENS |
| LOGIN-REFERENCE | Login tối/gradient theo ảnh,logo gốc,form/keyboard/responsive/native | PASS build/unit/UI USB; API USB cuối NOT VERIFIED |72JVM;4UI thật;0lint errors/15warnings;QA_LOGIN_REFERENCE;không thay phiên cá nhân |
| PROFILE | Xem/sửa hồ sơ, bản nháp/recreate/logout, Java/XML M3E | PASS local/AVD | ProfileFragment/Repository/ViewModel; QA_PROFILE_AVATAR_FCM |
| AVATAR | Upload/tải JPEG private, friend/block, transaction/race, UI bạn bè/lịch sử | PASS local/AVD | 6 backend cases,3JVM mới,5 UI cases; snapshot chứaavatars |
| FCM-REAL-AVD | Firebase cloud thật tới AVD, Worker cập nhật QA RemoteViews | PASS AVD | Foreground/background, xóa/logout; real opt-in test1/1 rồi batch6/6;4ảnhwidget |
| FCM-REAL-USB | FCM/widget trên điện thoại và launcher OEM | CHƯA NGHIỆM THU | Người dùng chọn emulator trước vì đang dùng điện thoại; không tự tác động máy |
| M3E-01 | Foundation + Login Material3Expressive1.14.0, Java/XML | PASS local/USB | Theme opt-in;light/dark,font1.5,màn hẹp/IME,offline/retry/auth/logout;28unit,4+4USB,build/lint;QA_M3E_LOGIN |
| M3E-02 | Redesign Register | PASS local/USB | Light/dark,long form,font1.5,IME,validation/loading/recreate,offline/retry/API/conflict;28unit,4+7USB,build/lint;QA_M3E_REGISTER |
| M3E-03 | Camera + trạng thái phiên theo M3E | PASS local/USB | Camera-first, scoped theme giữ Friends cũ; 28unit/20USB/build/lint, QA_M3E_CAMERA |
| M3E-04 | Redesign Bạn bè/Friends | PASS local/USB | M3E cards/header cuộn/light-dark/IME/dialog;28unit,20USB,build/lint;QA_M3E_FRIENDS |
| 0A | Khóa định danh và quy ước repository | PASS — tài liệu/quy ước | Có đủ 4 file đầu ra; kiểm tra lại ignore, link, nội dung và định danh; xem bàn giao |
| RESUME-DOCS | Khôi phục ngữ cảnh và tạo hồ sơ trạng thái còn thiếu | PASS — hồ sơ | Kiểm tra 7 file, 19 link local, 3 file mới không bị ignore; SHA-256 xác nhận 62 file có sẵn giữ nguyên |
| 0B | Firebase Android `com.taktak.app`, chỉ FCM | PASS — đăng ký/cấu hình | Kiểm tra local, ảnh Console khớp định danh, FCM V1 Enabled; người dùng xác nhận chưa thiết lập dịch vụ bị loại trừ |
| 0C | Kịch bản kết nối emulator và máy thật | PASS local | Alias emulator và máy thật qua USB reverse đã kiểm tra; LAN/tunnel chưa chạy |
| 1A | Server Java 17 / Spring Boot 3 / Maven tối thiểu | PASS local | JAR build; 6 test PostgreSQL/Flyway/HTTP/constraints, không skipped; độc lập Firebase |
| 1B | Android Java/XML tối thiểu | PASS bootstrap | ViewBinding/dependency/config mạng; APK debug/release và UI emulator đạt; Firebase khớp ảnh Console |
| 1C | Build, database/migration, health và kết nối thiết bị | PASS local | Backend/migration/build có bằng chứng PASS; emulator alias 4/4 và máy thật USB reverse 4/4 PASS; cài/mở/UI đạt |
| 2A | Đăng ký backend, User/Repository, BCrypt, validation, unique/transaction | PASS | Maven verify 13/13 integration test, gồm race username/email và regression; xem REGISTRATION_API |
| 2B | Login/JWT, refresh rotation/logout, giới hạn thử login | PASS | 29/29 test; expired/forged JWT, rotation/replay/race/rollback, logout và IP/account limits; HTTP smoke ngoài test profile PASS |
| 2C | GET/PUT hồ sơ cá nhân, chỉ sửa displayName | PASS | 36/36 test, 7 case hồ sơ mới; isolation A/B, mass assignment, validation, JWT, DTO; HTTP smoke ngoài test profile và JAR PASS |
| 2D | Integration test/curl cho auth/hồ sơ, chốt lỗi | PASS local | 39/39 test và JAR, curl thật cho expiry/forgery/duplicate/rotation/logout; hợp đồng lỗi chung đã kiểm tra |
| 3A | Login/Register Java XML, ViewBinding/Material, validation/loading | PASS UI local | Build/lint debug/release; 8 unit/12 emulator test; screenshot light/dark/required/IME, sửa keyboard che field; chưa nối API |
| 3B | Auth API/Repository/ViewModel, phiên mã hóa và refresh đồng bộ | PASS local | Build/lint debug/release; 20 unit + 17 emulator test với backend thật; xem QA_ANDROID_AUTH |
| 3C | Điều hướng phiên, Camera destination và logout UI | PASS | Local lịch sử 20 unit/25 emulator; USB mới 6/6 test chọn lọc, cold-process restore/logout, 5 screenshot; xem QA_SESSION_ROUTING |
| 3D | CameraX → upload chỉ mình tôi → GET bằng JWT | PASS | Maven46/46; Android build/lint +20 unit;4 ca media +1 lặp đủ sáng +6 auth regression máy thật PASS |
| UI-CAMERA | Redesign Camera sau3D, không thêm chức năng | PASS local/USB | Python3.12.10/CLI skill; Java/XML theme Camera riêng, build/lint +20 unit;13 ca USB/14 lượt đạt gồm quyền, sáng/tối/busy, ảnh thật/offline/retry và logout; xem QA_CAMERA_REDESIGN |
| 4A | Search username, gửi/list pending incoming server | PASS backend local | Maven65/65 +JAR;19 ca mới gồm quota/block/DTO/isolation/concurrency/rollback,46 regression; schema/Android không đổi |
| 4B | Accept/reject, canonical friendship/list/unfriend/block/unblock | PASS backend local | Maven87/87 +JAR;22 HTTP/PostgreSQL tests mới/65 regression, race/rollback/receiver-only/idempotency; schema/Android giữ nguyên |
| 4C | FriendsFragment nối API qua Repository/ViewModel | PASS local/USB/AVD | Search/incoming/friends và actions;28 unit,6 USB cuối,7 regression,offline/retry,A/USB↔B/AVD+C;build/lint;avatar local, chưa recipient-aware media5C |
| 5A | ViewPager2/bốn tab, CameraX quyền/đổi camera/flash/lifecycle/rotation | PASS local/USB | 33JVM,33ca USB duy nhất,build/lint debug/release;QA_MAIN_CAMERA;History/Profile chưa chức năng |
| 5B | Cache JPEG/crop/mirror; Preview/chụp lại/caption50/chọn bạn | PASS local/USB | 35 JVM,32 USB sau retest;21 bạn phân trang,đổi phiên,IME/offline/JWT;QA_PREVIEW |
| 5C | Gửi ảnh cho bạn bè; nén bounded/private storage/recipient authorization | PASS local/USB | 95backend,39JVM,15USB; chọn2/đủ21bạn,100recipient/race/rollback/EXIF/JWT/offline;QA_MOMENT_SHARING |
| 5D | Tiến trình upload, retry & Idempotency Key | PASS local/USB | 99backend, 42JVM, 3USB; DB V2 index, SHA-256 fingerprint, CountingRequestBody 0–100%, SavedStateHandle retry; QA_MOMENT_IDEMPOTENCY |
| 5E | Lịch sử ảnh đã nhận (Received Moments History) | PASS local/USB | 104backend, 45JVM, 3USB; HistoryFragment M3E, LRU Bitmap cache 20MB, JWT image load, relative time, native index query; QA_MOMENT_HISTORY |
| 6A | Đăng ký thiết bị & FCM Token (Device & FCM Registration) | PASS local/AVD | 111backend, 49JVM, 2AVD; POST/DELETE/GET /api/devices, Firebase Admin SDK fallback, DeviceIdProvider, TaktakMessagingService, auto-sync token; QA_DEVICE_REGISTRATION |
| 6B | Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (Push Notification Dispatching) | PASS local/AVD | 118backend, 56JVM, 4AVD; FCM data message sau commit (created/deleted), DELETE /api/moments/{id}, stale token cleanup, PushEventBus, HistoryViewModel real-time sync; QA_PUSH_DISPATCHING |
| 6C | AppWidgetProvider 2×2 & WorkManager Widget Sync | PASS local/AVD | 122backend, 60JVM, 6AVD; GET /api/moments/latest, AppWidgetProvider 2×2, RemoteViews, decodeSampledBitmap, WorkManager sync & periodic, FCM push trigger, logged out state; QA_WIDGET_SYNC |
| 7A | Lịch sử Đã gửi & Xem toàn màn hình (Sent Moments History & Viewer) | PASS local/AVD | 123backend, 62JVM, 8AVD; GET /api/moments?type=sent, TabLayout 2 tab, MomentViewerActivity, LRU cache 20MB; QA_SENT_HISTORY_VIEWER |
| 7B | Xóa khoảnh khắc phía máy chủ & Thu hồi quyền tức thời (Server-side Delete) | PASS local/AVD | 123backend, 66JVM, 10AVD; DELETE /api/moments/{id}, soft delete, discard image file, push moment_deleted, UI confirm & rollback; QA_MOMENT_DELETION |
| 7C | Tải ảnh vào bộ sưu tập thiết bị (Save to Device Gallery) | PASS local/AVD | 123backend, 66JVM, 12AVD; MediaStore Pictures/Taktak, Scoped Storage API 29+, compatibility API 26-28, user action only; QA_SAVE_TO_GALLERY |
| 8A | Chuẩn bị môi trường VPS Ubuntu & Cấu hình dịch vụ (VPS Setup) | CHƯA NGHIỆM THU VPS | 123backend, package JAR; application-prod.yml, .env.example, taktak.service, Dockerfile, docker-compose.yml, setup-vps.sh; QA_VPS_SETUP |
| 8B | Nginx Reverse Proxy, TLS Let's Encrypt & Tường lửa UFW | CHƯA NGHIỆM THU TLS/RESTORE THẬT | Nginx taktak.conf, Certbot HTTPS, rate limit, upload 10M, UFW 22/80/443, logrotate, backup.sh, restore.sh, backup timer; QA_NGINX_SSL_BACKUP |
| 8C | Kiểm thử cuối, bảo mật client-server & Quy trình Update/Rollback | PASS LOCAL, CHƯA NGHIỆM THU TOÀN DIỆN | 130 backend,69 JVM,22 Android khác nhau qua batch,11 contract mock; không suy ra production/FCM thật; QA_REVIEW_FIXES |

## Nghiệm thu phiên khôi phục trước khi duyệt 0B (lịch sử)

- [x] Đọc toàn bộ đặc tả, quy tắc agent, README và quyết định kỹ thuật.
- [x] Kiểm tra Git status, lịch sử commit local, thay đổi staged/unstaged/untracked.
- [x] Đọc source, Gradle, Manifest, layout và test mẫu liên quan.
- [x] Xác định 0A là tiểu bước gần nhất được duyệt; 0B chưa được cho phép.
- [x] Kiểm tra lại các đầu ra 0A; chỉ ghi kết quả kiểm tra đã thực hiện.
- [x] Tạo PROJECT_STATUS, TASKS và SESSION_HANDOFF sau khi xác nhận chúng chưa tồn tại.
- [x] Xác minh file mới, liên kết, dấu conflict, mẫu credentials và tính toàn vẹn các file có sẵn.
- [x] Hoàn tất hồ sơ bàn giao; giữ điểm dừng tại cổng xác nhận 0B.

## 0B — việc còn thiếu trong phạm vi được duyệt

Người dùng đã xác nhận “continue” cho 0B. Không xin phê duyệt lại; chỉ cần bổ sung kết nối/bằng chứng còn thiếu.

- [x] Đọc đặc tả, code/config và trạng thái Git trước khi sửa.
- [x] Kiểm tra JSON, loại trừ Admin private-key fields, đối chiếu App ID/project number và package/applicationId.
- [x] Kiểm tra file cấu hình bị ignore, không có cấu hình/credential filenames tương ứng trong Git index.
- [x] Kiểm tra Firebase dependency khai báo trực tiếp chỉ gồm BoM và Messaging; giữ nguyên source/SDK.
- [x] Tìm khả năng truy cập Firebase: không có CLI trong PATH, tìm thấy plugin chính thức nhưng chưa cài/kết nối; đã đề xuất tích hợp.
- [x] Ghi 10 kết quả local và hướng dẫn đối chiếu vào [FIREBASE_SETUP.md](FIREBASE_SETUP.md).
- [x] Đối chiếu project ID/project number và Android App ID/package trong ảnh Console với JSON local: khớp.
- [x] JSON khớp app/project trong ảnh; người dùng xác nhận chưa thiết lập Auth/Firestore/Storage/Functions; không tạo Admin credentials.
- [x] Ghi nguồn ảnh/xác nhận thủ công và kết luận 0B PASS trong FIREBASE_SETUP; chưa kiểm thử FCM runtime.

Không tạo lại project/app khi chưa đối chiếu tài nguyên đã có; không gửi nguyên JSON hoặc key vào báo cáo. Khi 0B đạt, tiếp tục các việc còn lại của 0–1 theo quyền AUTO CONTINUE; không xin lại phê duyệt từng tiểu bước.

## Bàn giao AUTO CONTINUE

- [x] Chuẩn bị JDK 17/Maven portable có checksum; helper chỉ chọn Java trong process.
- [x] Tạo server, migration V1, security mặc định đóng, env mẫu, Wrapper và 6 integration test với PostgreSQL thật.
- [x] Build executable JAR; test startup/schema/constraints/HTTP.
- [x] Tận dụng Android skeleton; ViewBinding, Java dependencies, URL/network policy theo variant, FCM auto-init tắt.
- [x] Build/lint debug và release; xác nhận ba cấu hình mạng không hợp lệ bị từ chối.
- [x] Chạy 4 instrumentation test qua reverse và xem screenshot/UI dump theo skill kiểm tra Android.
- [x] Thử lại đường alias; lưu FAIL timeout riêng, không che lỗi hoặc sửa package.
- [x] Có bằng chứng Firebase Console để nghiệm thu 0B và liên kết Firebase của 1B.
- [x] Sửa lỗi alias Android 17: quyền local network riêng debug, cấp quyền trong instrumentation; 4/4 test PASS trực tiếp `10.0.2.2:8080`, không reverse.
- [x] Kiểm tra health/app trên máy thật: Android 15 API 35 / 23129RAA4G, USB reverse, 4/4 test PASS và screenshot/UI dump.
- [x] Đạt điều kiện bootstrap local Bước 0/giai đoạn 1; dừng trước giai đoạn 2. LAN/tunnel chưa chạy, chưa FCM runtime/deploy.

## 2A — hoàn tất sau khi người dùng duyệt “continue”

- [x] Đọc đặc tả/source/Git; bảo toàn thay đổi các phiên trước.
- [x] Tạo User/JpaRepository và service transaction, không sửa migration V1.
- [x] Mở riêng POST register, trả 201 với DTO an toàn; các route nghiệp vụ khác vẫn 401.
- [x] Chuẩn hóa username/email, validation, password UTF-8/Unicode và BCrypt cost 12.
- [x] Xử lý 400/409/415 theo JSON lỗi; không phản chiếu password/SQL.
- [x] Chạy 13 test thật trên PostgreSQL test riêng, gồm hai case HTTP đồng thời, 0 failures/errors/skipped; build JAR PASS.
- [x] Ghi hợp đồng/cách chạy/kết quả và bàn giao; không commit/push/deploy.

## 2B — đã được duyệt và hoàn tất

- [x] Login bằng email/password; BCrypt/dummy hash, không tạo phiên khi credentials sai.
- [x] JWT HS256, secret môi trường fail-fast; kiểm tra exp/iat/iss/aud/sub/chữ ký, 401 JSON an toàn.
- [x] Refresh token ngẫu nhiên/hash-only trong bảng V1 có sẵn, lock/transaction rotation; replay 401, race chỉ một thành công, failure rollback.
- [x] Refresh không phụ thuộc access JWT, kể cả expired bearer gửi kèm; logout idempotent thu hồi refresh được gửi.
- [x] Login quota 20/IP và 5/account trong 60 giây; Retry-After, bounded memory, đồng thời không bypass; không tin X-Forwarded-For.
- [x] Maven verify 29/29 PASS, JAR; HTTP register→login→refresh→replay→logout→revoked refresh đúng 201/200/200/401/204/401.
- [x] AUTH_API/README/env mẫu/quyết định/trạng thái/bàn giao cập nhật; không đổi V1 hoặc source Android.

## 2C — đã được duyệt và hoàn tất

- [x] Đọc đặc tả/quyết định/trạng thái/Git/source; bảo toàn thay đổi cũ của 2A/2B.
- [x] GET/PUT `/api/users/me` xác thực qua filter chain hiện có, user ID chỉ lấy từ JWT subject.
- [x] DTO chỉ trả id/email/username/displayName/createdAt; no-store, không entity/hash/private path.
- [x] Chỉ bind và cập nhật displayName; strip/nonblank/max 255 như đăng ký, transaction JPA.
- [x] User không tồn tại trả 401 an toàn; input lỗi giữ format 400/415 có sẵn.
- [x] Thêm 7 test HTTP/PostgreSQL: GET, PUT/persistence/idempotency, A/B và mass assignment, invalid body, biên độ dài, JWT và orphan subject.
- [x] Maven verify 36/36 PASS, không failures/errors/skipped; JAR có controller và không chứa test artifacts.
- [x] Smoke ngoài test profile: register→login→GET→PUT→GET→refresh→GET→logout đúng 201/200/200/200/200/200/200/204.
- [x] PROFILE_API/quyết định/trạng thái/bàn giao cập nhật; không sửa migration, dependency, Android, Firebase hoặc README.

## 2D — đã được duyệt và hoàn tất

- [x] Đối chiếu Git/source/bằng chứng 36 test cũ, không coi output cũ là mới chạy.
- [x] Curl thật trên loopback: register/login/me/PUT, username trùng, JWT giả/hết hạn, refresh/replay/logout/revoked refresh.
- [x] Không đưa credentials vào argv/file/log; curl config qua stdin, bỏ curlrc/proxy và có timeout.
- [x] Test ban đầu bắt được 404 bị đổi thành 403; sửa MVC handler 404/405, giữ filter chain và `/error` đóng.
- [x] Chốt JSON lỗi/headers/path/time; xử lý 406/500 an toàn; fault injection chỉ test scope.
- [x] Script Windows nghiệm thu đầy đủ và chế độ CurlOnly đã chạy; bản cuối 39/39 test PASS, JAR build.
- [x] API_ERRORS/QA_AUTH và hồ sơ bàn giao cập nhật, không migration/dependency/Android/Firebase/README.

## 3A — đã được duyệt và hoàn tất trong phạm vi UI

- [x] Đọc skill được chỉ định, đặc tả/quyết định/trạng thái/Git và source Android trước sửa; bảo toàn thay đổi cũ.
- [x] Login/Register Java/XML/ViewBinding/Material, lỗi từng field, password toggle/confirmation, scroll/IME/insets và light/dark.
- [x] Validation theo giới hạn backend; password không trim/cắt/lưu view state hoặc log.
- [x] Renderer loading khóa nút/inputs và guard duplicate; submit hợp lệ chỉ báo chưa nối server, không giả auth.
- [x] Launcher có lối mở form; auth activity không exported; chưa routing Camera/phiên.
- [x] Sửa root cause môi trường/style/min26; không tắt test/lint hoặc thay stack.
- [x] Skill UI dump/screenshot phát hiện keyboard che confirmation; sửa viewport, thêm test IME, xem ảnh sau sửa.
- [x] Bản cuối 8 unit + 12 instrumentation PASS, 0 failures/errors/skipped; build/lint debug và release PASS.
- [x] Manifest release/ignore/credentials và hồ sơ QA/trạng thái/bàn giao được kiểm tra; không commit/push/deploy.

## 3B — đã được duyệt và nghiệm thu local

- [x] Khôi phục Git/code: HEAD 8766f26 đã chứa triển khai; giữ nguyên commit/source và kiểm tra lại thay vì tạo lại.
- [x] AuthApi/UserApi, DTO Gson, ApiClient tách auth/protected và Repository gọi server thật.
- [x] ViewModel/LiveData + worker, loading/duplicate guard, lỗi local an toàn, cooldown Retry-After.
- [x] EncryptedSharedPreferences/Keystore, ghi cặp token, origin binding, backup exclusion, fail-closed.
- [x] Bearer đúng đích; refresh đồng bộ, tối đa một replay; clear phiên khi revoke/401, guard kết quả trễ sau đổi phiên.
- [x] Chạy lại 20 unit test, 0 failures/errors/skipped; 17 instrumentation PASS trên AVD read-only/backend/PostgreSQL tạm thật.
- [x] Build/lint debug/release PASS; 0 lint errors, 3 warnings được ghi rõ, không suppress.
- [x] Xem screenshot UI thành công/lỗi, rà policy/credentials và cập nhật hồ sơ; không commit/push/deploy.

Giới hạn chưa chạy và lệnh kiểm thử tại [QA_ANDROID_AUTH.md](QA_ANDROID_AUTH.md). Nghiệm thu auth máy thật/đóng mở app/routing còn ở 3C; không đánh dấu toàn giai đoạn 3 hoàn tất.

## 3C — PASS local và nghiệm thu máy thật Android 15

- [x] Đọc tài liệu/Git/source/skill; giữ 7 hồ sơ dirty từ 3B.
- [x] Launcher kiểm tra phiên trên worker, verify me/refresh trước Camera; phiên vắng/revoke về Login.
- [x] Login/logout dọn task/back stack, Camera destination placeholder chưa chụp ảnh.
- [x] Lỗi kết nối/read timeout/permission/storage có UI; retry hồi phục và logout local khi mạng lỗi.
- [x] Không báo xóa token thành công khi disk clear lỗi, giữ gate qua recreate và thử logout lại.
- [x] Sửa thông báo bị initial LiveData che, đồng bộ test IME theo visibility thực thay vì delay cố định.
- [x] Build/lint debug/release PASS (0 errors, 3 warnings); 20 unit + 25 emulator test PASS, fixture cold start 1/1 và ADB force-stop/relaunch/logout PASS.
- [x] Xem screenshot, review bảo mật và cập nhật hồ sơ; không đổi backend/migration/Firebase/dependency.
- [x] Điện thoại USB kết nối lại, không có phiên auth trước test; build/cập nhật debug giữ dữ liệu và cold launcher → Login trên máy thật PASS.
- [x] Máy thật: login đúng/sai, encrypted persistence, cold-process restore, mất kết nối/retry, logout/root task và revoke refresh PASS. Hai test launcher + bốn AuthBackendTest = 6/6 PASS; xác minh lại cold launcher về Login sau logout. Không tuyên bố suite cũ 25 test máy thật đạt; suite mặc định mới 27 test chưa chạy toàn bộ.

Mốc3C đã được duyệt tiếp3D sau đó; xem nghiệm thu3D bên dưới.

## 3D — PASS backend local và Android15 qua USB

- [x] Đọc đặc tả/quyết định/trạng thái/Git/source, bảo toàn thay đổi3B/3C/UI review.
- [x] CameraX sau, xin/từ chối quyền an toàn, lifecycle, rotation/crop1:1/JPEG có giới hạn; không gallery.
- [x] POST multipart owner-only, sender từ JWT, không recipient/FCM; JPEG thực tế/caption/dung lượng/pixel được kiểm tra.
- [x] Private storage tên server sinh, rollback dọn file, V1 không đổi; GET kiểm tra owner/deleted từng lần, B404/thiếu hoặc saiJWT401.
- [x] Repository/ViewModel dùng transport/refresh hiện có, chốt generation chống media qua phiên, không fake ảnh local; retry known id chỉ GET.
- [x] Maven46/46 +JAR;20 unit, build/lint debug/release0 errors/3 warnings;4 ca media mới máy thật và6 auth regression PASS, lặp chụp đủ sáng1/1 PASS.
- [x] Xem screenshot, kiểm tra cold logout không còn ảnh/token; smoke4row/4file/0recipient, dừng runner/gỡ reverse; giữ dữ liệu cá nhân.
- [x] Ghi giới hạn unknown POST response có thể tạo thêm moment; idempotency hoàn chỉnh ở5D, chưa coi31 test mặc định đã chạy.

Mốc3D đã được duyệt tiếp4A sau UI-CAMERA. API/hướng dẫn/giới hạn3D tại [MOMENT_API.md](MOMENT_API.md), [QA_SELF_MOMENT.md](QA_SELF_MOMENT.md).

## 4A — PASS backend local

- [x] Đọc toàn bộ đặc tả/quyết định, trạng thái/bàn giao/Git/source; bảo toàn dirty/untracked trước phiên.
- [x] GET users/search prefix chuẩn hóa, underscore literal, max20/public DTO, loại self/block hai chiều; quota user/IP bounded và Retry-After.
- [x] POST friends/requests receiverId, sender JWT; self/absent/block/friendship/duplicate checks; khóa cặp PostgreSQL và unique V1 chống race.
- [x] GET incoming chỉ người nhận/pending, lọc block hai chiều, page20/sort ổn định/hasNext; không email/hash/đường dẫn private.
- [x]14 integration +5 limiter tests mới,46 regression; Maven verify65/65,0 failures/errors/skipped +JAR,44.283giây.
- [x] Ghi lỗi fixture orphan và sửa test đúng nguyên nhân; không bỏ FK/JWT assertion, không dùng DB cá nhân.
- [x] Cập nhật hợp đồng/API_ERRORS/quyết định/trạng thái/handoff; không Android/migration/dependency hoặc commit/push/deploy.

Mốc4A đã được duyệt tiếp4B riêng.

## 4B — PASS backend local

- [x] Đọc toàn bộ đặc tả/Git, quyết định và source4A; bảo toàn dirty/untracked, không sửa Android hoặc migration.
- [x] PUT accept/reject chỉ receiver, hidden404; status/responded_at/friendship transaction, canonical UUID PostgreSQL, retry không resurrect.
- [x] GET friends page20/hasNext/public DTO, block filter hai chiều; DELETE friend chỉ cặp actor–target/idempotent.
- [x] POST block atomically ghi block/xóa friendship/reject pending hai chiều; DELETE unblock chỉ hướng của actor, không restore.
- [x] Cùng lockPair users cho send/respond/unfriend/block/unblock; projection tránh status cache cũ trước khóa; kiểm thử race và chờ lock thật.
- [x]22 test mới gồm receiver-only/JWT/DTO/input/pagination/rollback/concurrency,65 regression; Maven verify87/87,0 failures/errors/skipped +JAR,46.821giây.
- [x] Ghi3 lỗi harness ban đầu, sửa assertion tập kết quả và snapshot thống kê đúng nguyên nhân; không bỏ kiểm thử hoặc FK/transaction.
- [x] Cập nhật hợp đồng/API_ERRORS/quyết định/trạng thái/handoff; không commit/push/deploy hoặc tự chuyển4C.

## 6A — Đăng ký thiết bị & FCM Token (PASS local/AVD)
- [x] Backend Spring Boot: POST /api/devices, DELETE /api/devices/{id}, GET /api/devices.
- [x] Android Client: DeviceIdProvider, DeviceApi, DeviceRepository, TaktakMessagingService, SessionViewModel token sync.
- [x] 111/111 backend tests PASS, 49/49 unit tests PASS, 2 connected acceptance tests PASS. Xem [QA_DEVICE_REGISTRATION.md](QA_DEVICE_REGISTRATION.md).

## 6B — Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (PASS local/AVD)
- [x] Backend Spring Boot: FCM push notification afterCommit, recipientIds=[] không push, DELETE /api/moments/{id} xóa file và phát moment_deleted.
- [x] Android Client: PushEventBus, TaktakMessagingService, HistoryViewModel auto-refresh/auto-remove, MomentRepository delete.
- [x] 118/118 backend tests PASS, 56/56 unit tests PASS, 4 connected acceptance tests PASS. Xem [QA_PUSH_DISPATCHING.md](QA_PUSH_DISPATCHING.md).

## 6C — AppWidgetProvider 2×2 & WorkManager Widget Sync (PASS local/AVD)
- [x] Backend Spring Boot: GET /api/moments/latest, trả về ảnh mới nhất hoặc 204 No Content.
- [x] Android Client: TaktakAppWidgetProvider 2x2, WidgetUpdateWorker downsample an toàn, PeriodicWorkRequest 15 phút, push trigger.
- [x] 122/122 backend tests PASS, 60/60 unit tests PASS, 6 connected acceptance tests PASS. Xem [QA_WIDGET_SYNC.md](QA_WIDGET_SYNC.md).

## 7A — Màn lịch sử Đã gửi & Xem toàn màn hình (PASS local/AVD)
- [x] Backend Spring Boot: GET /api/moments?type=sent&page=0 và sender authorization GET /image.
- [x] Android Client: TabLayout 2 tab ("Đã nhận" / "Đã gửi"), HistoryViewModel dual-tab state, HistoryImageCache LruCache 20MB, MomentViewerActivity toàn màn hình.
- [x] 123/123 backend tests PASS, 62/62 unit tests PASS, 8 connected acceptance tests PASS. Xem [QA_SENT_HISTORY_VIEWER.md](QA_SENT_HISTORY_VIEWER.md).

## 7B — Xóa khoảnh khắc phía máy chủ & Thu hồi quyền tức thời (PASS local/AVD)
- [x] Backend Spring Boot: DELETE /api/moments/{id} sender-only (403 với non-sender), soft delete + storage.discard, GET /image trả về 404 tức thời, phát FCM moment_deleted.
- [x] Android Client: icon xóa ic_delete.xml trên MomentViewerActivity khi isSender == true, MaterialAlertDialog xác nhận, xóa cache RAM, emit PushEventBus, enqueue widget sync, xử lý offline bảo toàn local data.
- [x] 123/123 backend tests PASS, 66/66 unit tests PASS, assemble/lint debug/release 0 errors, 10 connected acceptance tests PASS trên Android Emulator Medium_Phone. Xem [QA_MOMENT_DELETION.md](QA_MOMENT_DELETION.md).

## 7C — Tải ảnh vào bộ sưu tập thiết bị (PASS local/AVD)
- [x] Android Client: GallerySaver lưu ảnh vào MediaStore (Pictures/Taktak) chuẩn Scoped Storage trên Android 10+ và hỗ trợ Android 8–9 với quyền WRITE_EXTERNAL_STORAGE maxSdkVersion 28.
- [x] MomentViewerActivity: nút lưu ảnh ic_download.xml (viewerSaveButton) trên viewerTopBar cho cả sender và recipient; ảnh riêng tư không tự động xuất ra Gallery khi chưa có hành động người dùng (User Action).
- [x] 66/66 unit tests PASS, assemble/lint debug/release 0 errors, 12 connected acceptance tests PASS trên Android Emulator Medium_Phone. Xem [QA_SAVE_TO_GALLERY.md](QA_SAVE_TO_GALLERY.md).

**Đề xuất duy nhất: Giai đoạn 8A — Chuẩn bị môi trường VPS Ubuntu & Cấu hình dịch vụ (VPS Ubuntu Setup & System Services). Chờ duyệt, không tự ý triển khai.**
