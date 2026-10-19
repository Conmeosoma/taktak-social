# QA CHAT-01–05 — 20/10/2026

Trạng thái: PASS trong phạm vi local/USB, chưa nghiệm thu production. Người dùng duyệt toàn bộ CHAT-01–05, yêu cầu điện thoại USB thay emulator. Không commit/push/deploy, không migration vào DB cá nhân/production.

## Kết quả đã xác minh

- Backend: `mvn verify` 163/163, không fail/error/skip, JAR build; sau redaction DTO, ChatIntegrationTest 11/11 + JAR chạy lại đạt. Logs `.local/artifacts/chat01-verify-final.log`, `chat01-redaction-verify.log`.
- Android: build debug/test/release và lint debug/release đạt, 115 JVM tests. Logs `.local/artifacts/chat-final-build.log`, `chat-handoff-build.log`. Lint 0 errors, còn 36 cảnh báo debug/44 release; không tuyên bố hết cảnh báo. Helper regression mới có build riêng `chat-usb-harness-build.log`.
- USB: Xiaomi 23129RAA4G, Android 15, 1080×2400. APK app và instrumentation cập nhật bằng install -r, giữ dữ liệu. ChatThemeUiTest 6/6 PASS (36,605s); ChatCloudAcceptanceTest 1/1 PASS (3,354s), gồm assertion notification chung thực sự hiện trên Android. Logs `chat-usb-ui.log`, `chat-usb-cloud.log` trong `.local/artifacts`.
- Quyền `POST_NOTIFICATIONS`: Android ban đầu báo denied dù người dùng đã bật; sau xác nhận cho phép thông báo, quyền được đồng bộ qua ADB và xác minh granted. Không cấp quyền contacts/storage hoặc xóa app data. Phiên cá nhân và appearance được so sánh/khôi phục bằng assertion.
- 14 ảnh QA đã kéo về `.local/artifacts/chat-usb-images`; đã xem Inbox Original/Soft, retry với IME và ảnh moment fixture vuông. Không ảnh chụp thật trong lượt chat.
- Lượt hồi quy cũ đầu: 11 ca, 7 fail (6 timeout mở Main từ nền trên MIUI, 1 chờ viewer403 sau chuyển màn quá sớm). Đã sửa helper shell foreground và chờ lifecycle/đúng instance; chạy lại cùng 11 ca PASS (167,056s), không bỏ assertion, không sửa production Viewer. Log đầu `chat-usb-regression.log`, log đạt `chat-usb-regression-final.log`. Không chạy ca Camera chụp thật vì không cần cho phạm vi chat; không tính ca đó là PASS.
- APK bàn giao cuối: build toàn bộ đạt (37s), install -r app/test đạt. Lượt Chat UI cuối cùng **6/6 PASS (35,025s)**, log `chat-usb-final-acceptance.log`. Trước đó lượt `chat-usb-handoff.log` có 1 timeout Main cold-start/5 đạt; đã làm sạch màn QA tồn đọng sau khi cài fixture, dùng NEW_TASK|CLEAR_TOP và assert kết quả shell launch, không tăng timeout/bỏ assertion. Build test harness cuối 15s đạt (`chat-cold-start-harness-build.log`). Không quy lỗi cho người dùng khi chưa có bằng chứng thao tác máy.
- Review: `git diff --check` phần code chat không lỗi; `PROJECT_DECISIONS.md` có blank-line EOF đã tồn tại trước lượt chat, giữ nguyên thay đổi người dùng. Link tài liệu mới: 0 link hỏng. Tìm pattern key/private-key trong source không có kết quả; `.local/secrets`, JSON Firebase client và artifacts đều được Git ignore, không có credentials thuộc pattern được theo dõi. Đây là kiểm tra có giới hạn, không tuyên bố một audit bảo mật toàn repository.

## File thuộc lượt CHAT

Không tính các thay đổi auth/recovery/theme đã có trước và được giữ nguyên.

- Server mới: `chat/ChatDtos.java`, `ChatFailure.java`, `ChatRepository.java`, `ChatService.java`, `ChatController.java`; `db/migration/V4__direct_messaging.sql`; `ChatIntegrationTest.java`.
- Server cập nhật: `common/ApiExceptionHandler.java`, `ServerBootstrapTest.java` (schema V4).
- Android mới: `data/model/ChatModels.java`, `data/remote/ChatApi.java`, `data/repository/ChatRepository.java`, `viewmodel/ChatViewModel.java`, `data/push/ChatNotifications.java`; `ui/chat/ChatUi.java`, `ConversationAdapter.java`, `MessagesFragment.java`, `NewChatActivity.java`, `ChatActivity.java`.
- Android XML mới: `fragment_messages.xml`, `activity_new_chat.xml`, `activity_chat.xml`, `item_conversation.xml`, `item_chat_message.xml`, `values/chat_strings.xml`, `drawable/ic_chat.xml`.
- Android cập nhật: `MainActivity.java`, `ui/main/MainPagerAdapter.java`, `data/AuthGraph.java`, `data/remote/ApiClient.java`, `data/repository/AuthRepository.java`, `data/push/PushEventBus.java`, `TaktakMessagingService.java`, `AndroidManifest.xml`, `menu/main_tabs.xml`.
- Test mới: JVM `ChatRepositoryTest.java`, `ChatPushEventBusTest.java`; instrumentation `ChatThemeFixture.java`, `ChatThemeUiTest.java`, `ChatCloudAcceptanceTest.java`.
- Test hồi quy cập nhật theo năm tab/USB: `MainCameraAcceptanceTest.java`, `MediaThemeUiTest.java`, `ListThemeUiTest.java`, `ProfileThemeUiTest.java`.
- Hồ sơ: `AGENTS.md`, `docs/TAKTAK_PROJECT.md`, `PROJECT_DECISIONS.md`, `PROJECT_STATUS.md`, `TASKS.md`, `SESSION_HANDOFF.md`, `CHAT_API.md`, `QA_CHAT.md`.

## Ca kiểm thử

Backend PostgreSQL/HTTP: hội thoại cặp duy nhất, đồng thời mở/gửi, IDOR, friend/block/race, Unicode 2000, validation, retry/idempotency, rollback/no push trước commit, read cutoff, quyền cả hai trên moment/xóa/redaction, phân trang và unread vượt trang đầu.

JVM mới: ChatRepositoryTest (8), ChatPushEventBusTest (2): JWT transport, cùng key khi retry, validation trước network, payload foreign/malformed, thay phiên khi response đang chạy, UUID event/listener isolation.

ChatThemeUiTest: layout bốn theme/day-night/font/viewport, Inbox/search/5 tabs/CTA, New Chat, retry/recreate/draft/IME, push/session clear, đọc/moment private, SavedState khác process/phiên. Tài khoản và HTTP proxies RAM, không gửi ra Internet.

ChatCloudAcceptanceTest: hai tài khoản QA trong PostgreSQL riêng, đăng ký FCM token chỉ vào server QA, tạo bạn/hội thoại, gửi text qua REST → Firebase thật → service Android → tải message qua JWT; notification chung nếu permission đã cấp. Không giả emit sự kiện để thay delivery cloud; không đọc/in private key/token. Cleanup chỉ device/phiên QA và tag notification phòng QA; phục hồi AuthGraph và so sánh bytes encrypted session cá nhân.

Lệnh USB (sau build/cài APK, không chạy đồng thời instrumentation):

```powershell
adb -s <usb-serial> shell am instrument -w -r -e taktak.allowChatQa true -e class com.taktak.app.ui.theme.ChatThemeUiTest com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
adb -s <usb-serial> reverse tcp:58143 tcp:58143
adb -s <usb-serial> shell am instrument -w -r -e taktak.allowChatCloud true -e taktak.chatBaseUrl http://127.0.0.1:58143/ -e class com.taktak.app.ui.theme.ChatCloudAcceptanceTest com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Server cloud QA: `scripts/Start-LocalSmokeServer.ps1 -Port 58143 -EnableFcm -FirebaseCredentials <file-local-ignored>`. Cluster test mới, chỉ bind loopback. Không dùng DB/VPS thật. Khi kết thúc dừng bằng Enter và bỏ đúng reverse port QA, giữ các kết nối khác.

## Giới hạn

Chưa nghiệm thu VPS/HTTPS production, hai điện thoại, process bị OEM giết/Doze/force-stop, tải lớn lâu dài hoặc API26 trên thiết bị thật. Lint kiểm tra minSdk26; không giả kết quả emulator. Không tự triển khai lên VPS.

Cloud QA server đã dừng, đúng adb reverse 58143 đã bỏ, dữ liệu ảnh/avatar test đều 0 theo kiểm tra lúc dừng. Backend production và BASE_URL có sẵn không đổi; muốn dùng chat với server hiện hành cần một lượt cập nhật backend/migration được người dùng duyệt riêng. Chạm notification qua toàn bộ luồng login production chưa được nghiệm thu trong lượt này.
