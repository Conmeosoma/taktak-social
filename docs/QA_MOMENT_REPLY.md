# MOMENT-REPLY-01 — Trả lời ảnh trong chat riêng

Người dùng duyệt 20/10/2026. **PASS phạm vi triển khai và kiểm thử local/AVD**;
chưa deploy hoặc nghiệm thu hai thiết bị qua VPS. Không tuyên bố toàn app sạch lint.

## Trải nghiệm và quyền riêng tư

Lịch sử → Đã nhận → mở ảnh → nhập trả lời dưới ảnh → Gửi trả lời → mở Chat
với chủ ảnh. Tin nhắn gồm ảnh tham chiếu và lời trả lời. Chỉ hai thành viên của
hội thoại xem được; không có khu bình luận công khai trên ảnh.

Server không tin sender/owner do Intent hoặc request tự khai. Peer hội thoại
phải là chủ ảnh, người trả lời đã có quyền nhận ảnh, cả hai đang là bạn và không
block hai chiều. Không mở rộng quyền ảnh hoặc upload bản sao. Cùng key/payload
retry không tạo tin/push trùng; đổi nội dung với key cũ trả 409. Tối đa 2000
Unicode code point; blank/NUL/thiếu UUID bị từ chối.

Khi ảnh xóa/thu hồi quyền: ảnh tham chiếu bị ẩn/không tải được bằng JWT; lời trả
lời riêng đã gửi vẫn được giữ. Lỗi tải ảnh không được ghi đè lời trả lời. FCM
vẫn chỉ type/conversationId/messageId, không nội dung hoặc URL.

Draft/pending giữ qua Activity recreation trong cùng process/thế hệ phiên,
pending bất biến khi lỗi; người dùng retry thủ công. Đổi process/phiên/thu hồi
thì xóa draft/pending, chặn callback muộn. Composer chỉ mở sau tải ảnh xác thực,
ẩn ở ảnh mình; IME inset bảo vệ ô nhập và nút gửi. Không lưu nội dung input vào
view state hoặc gallery.

## File trong phạm vi

Backend:

- server/src/main/java/com/taktak/server/chat/ChatService.java
- server/src/main/resources/db/migration/V5__private_moment_replies.sql
- server/src/test/java/com/taktak/server/ChatIntegrationTest.java
- server/src/test/java/com/taktak/server/ServerBootstrapTest.java

Android:

- app/src/main/AndroidManifest.xml: adjustResize cho Viewer.
- app/src/main/java/com/taktak/app/data/repository/ChatRepository.java
- app/src/main/java/com/taktak/app/viewmodel/MomentReplyViewModel.java (mới)
- app/src/main/java/com/taktak/app/viewmodel/ChatViewModel.java
- app/src/main/java/com/taktak/app/ui/history/MomentViewerActivity.java
- app/src/main/java/com/taktak/app/ui/chat/ChatActivity.java
- app/src/main/res/layout/activity_moment_viewer.xml
- app/src/main/res/layout/item_chat_message.xml
- app/src/main/res/values{,-en,-es,-hi,-ja,-ko,-zh}/moment_reply_strings.xml (7 file mới)
- app/src/test/java/com/taktak/app/data/remote/ChatRepositoryTest.java
- app/src/androidTest/java/com/taktak/app/ui/theme/MomentReplyUiTest.java (mới)

Các đường dẫn app ở trên thuộc android-client. Các ID cũ và bản sửa UI/ngôn ngữ/
QR/security/widget dirty/untracked trước lượt này được giữ nguyên. Không đổi
package, stack, dependencies hoặc migration V1–V4.

Tài liệu: CHAT_API, PROJECT_DECISIONS, TAKTAK_PROJECT, PROJECT_STATUS, TASKS,
SESSION_HANDOFF và báo cáo này. Không tự commit/push/deploy.

## Kết quả kiểm thử thực tế

| Kiểm tra | Kết quả | Log tại .local/artifacts |
|---|---|---|
| Server mvn -o verify đầy đủ | PASS 170/170, 0 failure/error/skip; BUILD SUCCESS, 3m03s | moment-reply-server-verify-final.log |
| ChatIntegrationTest trong verify | PASS 18/18, gồm 7 ca mới | cùng log, server/target/surefire-reports |
| Android debug/test/release unsigned + JVM | BUILD SUCCESSFUL 52s; JVM 122/122, task thực thi | moment-reply-handoff-build.log |
| Reply mới + hồi quy Chat/Media trên AVD | PASS 18/18, 190,294s | moment-reply-avd-ui.log |
| Reply trên APK bàn giao sau sửa nhãn image retry | PASS 7/7, 28,388s | moment-reply-avd-final.log |
| Bổ sung tách guard process/generation độc lập | PASS 1/1, 0,041s; test APK build16s | moment-reply-final-guard.log, moment-reply-final-guard-test-build.log |
| Lint debug/release cuối | FAIL 179 errors/90 warnings và 179/98, giống nền trước tính năng; 28s | moment-reply-handoff-lint.log |
| Review bảo mật + diff check | Không phát hiện must-fix mới; diff check không lỗi whitespace, chỉ warning CRLF | review source và git diff --check |

Server test dùng HTTP thật + Embedded PostgreSQL/port ngẫu nhiên, user và dữ
liệu QA riêng. V5 được Flyway kiểm tra ở cluster test mới; không chạm DB cá nhân/
VPS. Push sender ghi nhận trong RAM, không gửi cloud push hoặc email thật.
Kiểm tra owner/self/ảnh không được nhận/ảnh người thứ ba dù chung grant,
block/unfriend, Unicode2000emoji/NUL, key-conflict/concurrent retry/pushonce,
unread/read/inbox/list, xóa/thu hồi/hard-delete reference và transaction rollback.
Các kiểm thử auth cũ tiếp tục chạy trong full verify.

Android dùng Medium_Phone API37, 1080×2400, SwiftShader headless 1536MB/2core,
userdata QA riêng .local/artifacts/qr-avd-data; không wipe/reset AVD gốc.
Fixture graph/session RAM, không gọi backend thật hoặc dùng tài khoản cá nhân.
7 UI cases kiểm tra: gửi Viewer→Chat bốn theme, blank/retry/recreate frozen key,
own-photo + session invalidation, lỗi ảnh/retry + xóa giữ text, IME thật và nút
gửi, ma trận ngang 48 tổ hợp theme/night/font/viewport, process/generation guard.
Ma trận không phải chứng nhận contrast/TalkBack toàn sản phẩm.

Ảnh đã review: .local/artifacts/moment-reply-images-final gồm chat-reply bốn theme,
reply-ime, reply-retry-recreated, reply-revoked-photo. Ảnh vuông một màu là JPEG
fixture có chủ đích, không phải ảnh cá nhân. AVD tắt sau QA để trả RAM.

Các lượt lỗi được giữ: test đầu có checked exception trong lambda, đã sửa test
và build lại; PowerShell ErrorAction Stop từng dừng ở cảnh báo compiler/JVM,
đã ghi stderr và dùng Continue để lấy đúng kết quả build; test hard-delete backend
đầu thiếu xóa recipients theo FK NO ACTION, đã sửa fixture và chạy lại full verify.
Không bỏ assertion, không baseline/suppress lỗi lint hoặc đánh dấu lượt lỗi PASS.

## Chạy lại

Ở repo root, chọn JDK bằng scripts/Use-TaktakJava.ps1 -Target Server hoặc Android.
Sau khi dot-source, đặt $ErrorActionPreference='Continue' nếu pipe native stderr
vào Tee-Object; kiểm tra exit và BUILD SUCCESS, không dựa riêng exit của Tee.

- server: mvn -o verify (hoặc Maven local đã chuẩn bị trong .tools); Embedded
  PostgreSQL cần quyền chạy process và cache binary trên Windows.
- android-client: cmd.exe /c gradlew.bat --offline --no-daemon --max-workers=2
  assembleDebug assembleDebugAndroidTest testDebugUnitTest; release thêm
  -PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/ chỉ làm placeholder build.
- AVD QA: cài -r APK debug/test; am instrument -w -r -e taktak.allowReplyQa true
  -e class com.taktak.app.ui.theme.MomentReplyUiTest
  com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner.
- Hồi quy thêm ChatThemeUiTest, MediaThemeUiTest dùng opt-in allowChatQa,
  allowMediaQa và captureQa tương ứng, chỉ trên AVD/fixture đã kiểm tra.

## Nghiệm thu USB bổ sung — 21/10/2026

Người dùng xác nhận chuẩn bị điện thoại và đồng ý chỉ dùng ảnh/bình luận QA
trong RAM, không camera, logout hoặc gửi tin thật. Thiết bị Xiaomi 23129RAA4G
(sapphire), Android 15, kích thước vật lý 1080×2400. Không chạy emulator lượt này.

- **PASS 7/7**, 36,257s; `moment-reply-usb-final-2.log` có `OK (7 tests)`,
  instrumentation code -1 (JUnit thành công). Logcat PID26533 xác nhận
  `run finished: 7 tests, 0 failed, 0 ignored`, không có FATAL EXCEPTION PID này.
- Build test APK cuối `BUILD SUCCESSFUL in 17s`, log
  `moment-reply-usb-harness-build-2.log`. App/test cài `-r`, không clear/uninstall.
- Chạy đúng bảy ca reply như AVD: Viewer→Chat bốn theme; blank/503 retry với
  frozen key/recreation; own-photo + session revoke; ảnh lỗi/retry/xóa giữ text;
  IME thật không che nút gửi; ma trận ngang48 tổ hợp; guard process/generation.
- Từng ca assert file `auth_session_v1.xml` byte-for-byte không đổi và khôi phục
  toàn bộ appearance map. So sánh fingerprint độc lập từ trước cài APK tới sau
  QA cũng trùng nhau. Không ghi token, nội dung session hoặc hash vào tài liệu.
- Đã xem bảy screenshot trong `.local/artifacts/moment-reply-usb-images`:
  chat-reply-{original,gradient,glass,soft}, reply-ime, reply-retry-recreated,
  reply-revoked-photo. Ảnh tím là JPEG fixture, không phải ảnh cá nhân. Composer,
  retry và giữ text đúng; Chat vẫn có icon quay lại khó nhận biết và timestamp/
  input tương phản thấp ở vài theme. Đây không phải chứng nhận UI/contrast toàn app.

Thay đổi lượt USB chỉ ở `MomentReplyUiTest.java` và báo cáo/trạng thái:
thêm opt-in `taktak.allowReplyUsbQa=true`, bảo vệ phiên/appearance, đường dẫn ảnh
USB riêng; shell mở Main host sau khi inject fixture trước mỗi ActivityScenario.
Fixture chặn restore Main bằng profile503, không đăng ký FCM qua phiên thật.
Không sửa production, backend, migration hoặc quyền hệ thống của điện thoại.

Hai lượt đầu không PASS: MIUI chặn cold-start nền (ActivityTaskManager result102);
đưa host lên foreground một lần chưa đủ vì ActivityScenario.close có thể gỡ task.
Runner kẹt được dừng đúng process QA, không clear data. Log đầu
`moment-reply-usb-acceptance.log` có Process crashed sau lệnh dừng, không phải
bằng chứng crash nghiệp vụ; `moment-reply-usb-final.log` dừng ở ca đầu và ADB
client mất phiên. Lượt cuối sửa mở lại host trước mỗi launch và chạy đủ7/7.
Giữ các log cũ, không xóa assertion hoặc dùng retry để đánh dấu lượt lỗi PASS.

Chạy lại chỉ khi người dùng chuẩn bị USB và đồng ý dùng fixture:

```powershell
adb -s <usb-serial> shell am instrument -w -r `
  -e taktak.allowReplyQa true -e taktak.allowReplyUsbQa true `
  -e class com.taktak.app.ui.theme.MomentReplyUiTest `
  com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Không chạy lại Maven170/JVM122 trong lượt này: kết quả đó là bằng chứng của
lượt triển khai trước, không phải mới chạy trên điện thoại. Chưa nghiệm thu
transport Android→VPS hai tài khoản hoặc cloud FCM; chưa áp V5 lên production.

## Giới hạn và bước thủ công

1. Chưa cập nhật VPS/DB production. Khi có phê duyệt triển khai riêng, backup
   trước, dùng release backend mới để Flyway áp dụng V5 và Android tương thích.
   Client cũ chưa hiểu MOMENT_REPLY; cần phối hợp cập nhật, không tự áp migration
   hoặc rollback database có dữ liệu. Không sửa V1–V4 hay dùng ddl-auto=update.
2. Chưa chạy Android→VPS thật với hai tài khoản/hai thiết bị; chưa nghiệm thu FCM
   cloud cho loại reply. Backend push-payload assertions có chạy, không thay thế
   delivery trên thiết bị/OEM. Không đảm bảo cập nhật tức thì khi force-stop.
3. 179 lỗi bản dịch cũ và lỗi cắt chữ ListThemeUiTest từ audit trước chưa được
   xử lý trong tính năng này. Chat attachment hồi quy lượt này đạt nhưng tính
   không ổn định đã ghi trước đó không được coi tự hết khi một lần PASS.
4. Không chạy lại optical QR/widget/TalkBack/RTL toàn app; không có thay đổi chức
   năng ở các phần đó. Không thêm analytics, feature flags hoặc dependencies.

Đề xuất tiếp theo duy nhất: duyệt nghiệm thu end-to-end trên môi trường staging
hai tài khoản với backend V5 và Android tương thích; chưa tự triển khai bước đó.
