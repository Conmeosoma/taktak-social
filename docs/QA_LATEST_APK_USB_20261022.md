# Build APK mới và QA USB — 22/10/2026

## Bản sửa đồng bộ và nghiệm thu mới nhất: PASS18/18

Người dùng duyệt sửa test và chạy lại bộ18. Chỉ đổi ChatThemeUiTest.java:
đợi canSend rồi kiểm tra/dispatch loadAttachableMoments trong cùng main-loop;
không phát lại request đã được nhận, không nới/bỏ assertion. Production không đổi.
Build debug/test/JVM task thành công23s (4 executed/72 up-to-date); test APK
cài-r Success,127 JVM test không failure/error (task có thể up-to-date, không
gọi là chạy mới127 ca ở lượt sửa). USB bộ18 **PASS18/18,90,45s**, không cần retry
batch. Chat6/6, reply7/7, cache2/2 và HTTPS3/3. Phiên trước/sau bộ này byte-for-byte
không đổi; appearance được suite khôi phục. Đã xem ảnh room-private-moment:
fixture hiển thị, hạn chế icon back/contrast cũ vẫn còn, không phải visual toàn
app PASS. Không gửi ảnh/tin thật, không reset hoặc logout tài khoản cá nhân.
Log `attachment-sync-build.log`, `attachment-sync-full18.log`, ảnh
`attachment-sync-room.png` trong `.local/artifacts/acceptance-all/`.
Các FAIL/chẩn đoán bên dưới giữ nguyên làm baseline đỏ. Không commit/push.

## Chẩn đoán bổ sung theo yêu cầu test lại

Test APK diagnostic build23s, cài-r; riêng ca attachment FAIL22,805s với state:
`busyAtAction=true, busy=false, expired=false, error=0, choices=0`.
ChatThemeUiTest kiểm tra idle rồi dispatch loadAttachableMoments ở hai lượt
main thread khác nhau; refresh/markRead/push có thể đặt busy trước action.
ChatViewModel.run tại dòng255 bỏ action khi busy, nên không chạy phần tải/
dedup và danh sách vẫn rỗng. ChatActivity.render dòng79 vô hiệu nút Attach
khi canSend=false; test gọi thẳng ViewModel nên không tuân theo guard UI.
Đã xác định nguyên nhân ca FAIL hiện tại là race đồng bộ trong harness; không
phải bằng chứng API dedup hay ảnh thật bị hỏng. Chưa sửa behavior production,
chưa sửa đồng bộ test hoặc đánh dấu ca này PASS. Chỉ thêm state diagnostic
trong ChatThemeUiTest.java, không log ID/token/nội dung. Phiên vẫn không đổi.
Log `.local/artifacts/acceptance-all/attachment-state-diagnostic.log`.

## Kết quả

**PASS build/cài đặt; FAIL cổng QA USB được chọn (17/18)**. Không phải toàn bộ
ứng dụng đã nghiệm thu. Source HEAD889e4ca, không sửa Java/XML/backend trong lượt này.

- Xiaomi23129RAA4G Android15,1080×2400 qua USB, không emulator.
- Gradle offline/no-daemon, toolchain JBR Android Studio, `--rerun-tasks`
  assembleDebug/assembleDebugAndroidTest/testDebugUnitTest: **BUILD SUCCESSFUL48s,
  76 task thực thi**. JVM127, failures0/errors0.
- App debug build22/10/2026 20:49:19,17.844.134 byte. SHA-256
  `6227596aaf3bc55c8c71eb43c3f0de53011a681d99837ec2d91a220e7071ea2a`.
- App và androidTest APK cài bằng adb install-r: Success. SHA-256 base.apk trên
  điện thoại khớp app-debug.apk mới. Giữ origin HTTPS tunnel hiện tại.
- Không build release hoặc chạy lint lại; release cũ unsigned/qa.invalid không
  dùng phân phối. 179 MissingTranslation ghi nhận trước vẫn chưa xử lý.

## QA trên điện thoại

Chạy fixture RAM có opt-in, không đăng xuất/sửa tài khoản thật, chụp/gửi ảnh,
gửi tin hoặc SMTP. OperationalConnectivity chỉ GET không JWT.

| Suite | Kết quả |
| --- | --- |
| MomentReplyUiTest | 7/7 |
| HistoryCachePrivacyTest | 2/2 |
| ChatThemeUiTest | 5/6 |
| OperationalConnectivityTest | 3/3 |

Tổng18 ca,1 failure,102,449s. Ca lỗi:
`incomingReadAndPrivateMomentAttachmentUseAuthenticatedFixture`, assertion
`Private attachment choices deduplicated` tại dòng153. Chạy diagnostic riêng
vẫn FAIL1/1,22,715s, không dùng retry để đổi thành PASS.
Code run() bỏ qua action nếu busy; test chờ idle và dispatch action ở hai thời
điểm khác nhau, Chat có markRead/poll/push refresh. Đây là giả thuyết race cần
instrumentation thêm để chứng minh, chưa kết luận chắc chắn lỗi test hay app.

Encrypted session fingerprint trước/sau batch và sau diagnostic trùng. Mốc
fingerprint đầu tiên thành công là **sau cài, trước QA**; lệnh trước cài lỗi
quoting, không tuyên bố đã chứng minh byte-for-byte trước/sau install. Các suite
UI còn assert session không đổi và khôi phục appearance. Không ghi token/hash
phiên ra báo cáo. Không wipe/uninstall dữ liệu.

Đã pull7 ảnh fixture reply, xem trực tiếp reply-revoked-photo và chat-reply-soft:
ảnh bị thu hồi không hiển thị, text reply giữ; icon back Chat vẫn khó nhận biết,
timestamp/input contrast thấp ở vài theme. Không đánh dấu visual toàn app PASS.
Không thử lại CameraX thật, FCM cloud/widget launcher, QR, gallery hoặc API ghi
hai tài khoản VPS trong lượt này. Skill android_ui_verification dùng để hiệu
chuẩn thiết bị, QA có fixture và review ảnh, không thay bằng chứng E2E thật.

## Artifact và bàn giao

Local Git-ignore `.local/artifacts/acceptance-all/`:
`usb-latest-20261022-build.log`, `usb-latest-20261022-instrumentation.log`,
`usb-latest-20261022-attachment-diagnostic.log`,
`usb-latest-20261022-reply-images/`.
APK: `android-client/app/build/outputs/apk/debug/app-debug.apk`.
Chỉ sửa hồ sơ QA/trạng thái; không commit/push hoặc deploy.
Ưu tiên tiếp theo: chẩn đoán ca Chat attachment với log state/counters hữu hạn,
không log nội dung riêng tư; sửa đúng nguyên nhân và chạy lại cả bộ.
