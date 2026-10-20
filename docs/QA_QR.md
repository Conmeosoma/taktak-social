# Taktak — Kiểm tra và sửa QR (20/10/2026)

## Phạm vi và trạng thái

Yêu cầu: kiểm tra tất cả luồng QR trên điện thoại USB: mã của tôi, camera,
chọn ảnh, xác minh bạn bè, chia sẻ và vòng đời màn hình. Ban đầu dùng USB;
người dùng đã cho phép chuyển sang máy ảo khi điện thoại mất kết nối.
không dùng tài khoản cá nhân để gửi lời mời, không backend/database/deploy.

**PASS nghiệm thu QR trong phạm vi QA trên điện thoại USB và AVD.**
USB đã đạt 7 regression/UI và 2 phase phần cứng/picker độc lập; AVD trước đó
đạt 8 ca gồm deny/grant quyền. Lint toàn dự án vẫn FAIL 201 lỗi ngoài QR;
không đồng nghĩa mọi cổng chất lượng hay môi trường production đã đạt.

## Nghiệm thu USB bổ sung — 20/10/2026

- Xiaomi 23129RAA4G, serial `8e90fdff`, Android 15, 1080 × 2400.
  Cài app/test bằng `adb install -r`, giữ dữ liệu. Không thu hồi quyền Camera
  đã cấp trên điện thoại cá nhân; deny/grant đã được kiểm tra riêng trên AVD.
- **7/7 regression/UI PASS**, lần đầu 22,233s tại
  `qr-usb-acceptance-regression.log`; chạy lại trên harness cuối
  **7/7 PASS, 20,863s**, `qr-usb-final-regression.log`.
- Bổ sung `usbOpticalTorchAndActualGallerySelection` với cờ opt-in
  `taktak.allowQrOpticalQa=true`, tách `taktak.qrHardwarePhase`:
  - **gallery: 1/1 PASS, 12,990s**, `qr-usb-gallery-final.log`.
    Người dùng chọn ảnh QA thật qua picker của điện thoại, không inject URI
    hay callback. Phải nhận đúng `qa_qr_gallery`, UUID đuôi `0044`, và cả
    `pickingImage`/`readingImage` đều false. Ảnh bằng chứng
    `qr-usb-gallery-final.png` đã review, hiển thị **QA QR Gallery**.
  - **optical: 1/1 PASS, 6,881s**, `qr-usb-optical-final.log`.
    Camera sau đọc QR gốc mở trên laptop và nhận đúng `qa_qr_alice`, UUID
    đuôi `0022`. Test không inject payload. Nút flash được bật/tắt, assertion
    CameraX `TorchState.ON/OFF` đạt. Ảnh bằng chứng
    `qr-usb-optical-final.png` đã lưu.
- **Không tính PASS giả:** log `qr-usb-optical-picker-retry.log` từng ghi
  OK nhưng review ảnh phát hiện assertion gallery có thể dùng lại READY
  của optical. Lượt này không được dùng để nghiệm thu gallery. Đã sửa test
  dùng hai mã khác nhau, kiểm tra target ID/username và trạng thái picker,
  rồi tách phase để chạy độc lập. `qr-usb-optical-picker.log` và
  `qr-usb-optical-picker-distinct.log` có timeout do chưa hướng vào đúng QR
  gốc; giữ log, không loại assertion để vượt qua. Hai log final ở trên mới
  là bằng chứng nghiệm thu phần cứng/picker.
- Phiên mã hóa cá nhân giữ nguyên byte-for-byte qua assertion cleanup của
  mọi lượt UI đạt. Friend requests chỉ tới proxy RAM; optical/gallery có
  assertion `fixture.sends == 0`. Không gửi lời mời thật, không logout, không
  thay backend/database. Share dừng ở chooser bị intercept, không xác minh
  delivery sang ứng dụng bên ngoài.
- Hai ảnh tạm do QA tạo trong Downloads, được người dùng đồng ý trước,
  đã xóa đúng tên sau đối chiếu SHA-256; đã kiểm tra không còn tồn tại:
  `taktak-qr-qa-20261020-b18c.png` và
  `taktak-qr-gallery-qa-20261020-b18c.png`. Không xóa ảnh cá nhân. Bản sao
  QR thử vẫn ở `.local/artifacts/qr-usb-{optical,gallery}-target.png` trên
  laptop nếu cần khôi phục. Không lưu ảnh camera vào gallery.
- Build app/test đạt 25s (`qr-usb-acceptance-build.log`); build harness
  tách phase cuối đạt 13s (`qr-usb-split-build.log`). Không đổi production
  Java/XML/dependency trong lượt nghiệm thu bổ sung, chỉ mở rộng test/fixture
  và cập nhật hồ sơ. Kết quả 118 JVM, APK release và lint ở lượt trước giữ
  nguyên, không giả nhận đã chạy lại full build/lint trong lượt này.
- Thay đổi: `QrUsbUiTest.java`, `QrFixture.java`, `QA_QR.md`,
  `PROJECT_STATUS.md`, `TASKS.md`, `SESSION_HANDOFF.md`. Artifacts giữ local,
  không commit/push/deploy. Bong bóng Zalo vẫn có trong một số ảnh;
  không che mã/nút chính, đây là nghiệm thu chức năng, không phải pixel audit.

## Cập nhật nghiệm thu máy ảo

- Theo yêu cầu mới “tự chạy máy ảo”, dùng `Medium_Phone`, Android API 37,
  1080 × 2400, headless, 1536 MB RAM, 2 core, SwiftShader.
- AVD gốc có PIN, user ở `RUNNING_LOCKED`; hai lượt đầu có 4/7 đạt,
  3 ca không chạy được vì cache/Activity trong kho dữ liệu chưa mở khóa.
  Giữ log `qr-avd-initial.log`, `qr-avd-cold-run.log`. Không thử PIN,
  không reset khóa, không wipe dữ liệu gốc. Khởi động lại với `-datadir`
  và `-data` trỏ tới `.local/artifacts/qr-avd-data`, tạo userdata QA riêng;
  xác nhận `RUNNING_UNLOCKED` trước khi tiếp tục.
- Lượt chạy trên userdata QA phát hiện lỗi production thật: sau khi đóng
  My QR, Fragment vẫn còn trong FragmentManager lúc callback dismiss chạy,
  khiến scanner giữ trạng thái pause. Sửa `QrScanActivity.isMyCodeShown()`
  dựa trên Dialog còn thực sự hiển thị thay vì chỉ kiểm tra tag Fragment.
  Log lỗi trước sửa: `qr-avd-isolated-run.log` (6/7 đạt).
- Lượt tiếp theo phát hiện lỗi harness: Back được gửi sau 800 ms trong khi
  log cho thấy Photo Picker mất hơn 5 giây để hiển thị. Giữ assertion camera
  phải tiếp tục; thay sleep cố định bằng chờ Accessibility root của
  Photo Picker/DocumentsUI. Không thay production để che lỗi test.
  Log chưa đạt: `qr-avd-final.log` (6/7 đạt), `qr-avd-picker-logcat.log`.
- **PASS 7/7, 44,265 giây**, `qr-avd-final-retry.log`:
  5 `QrRegressionTest` và 2 `QrUsbUiTest`. Tên class giữ để chạy lại cả USB
  lẫn AVD; kết quả lượt này là máy ảo, không phải điện thoại.
  Đạt: hardware/software image decode, giới hạn ảnh/hỏng/không QR,
  parser/legacy/đảo màu, Y stride/bốn hướng xoay; self/offline/retry,
  not found/UUID mismatch/bạn ở trang hai, một lời mời tới fixture RAM;
  My QR/share chooser bị intercept, gallery fixture/cache, picker thật mở
  và hủy, camera bind lại, Activity recreate và chặn khi phiên thay đổi.
- **PASS 1/1, 14,942 giây**, `qr-avd-permission.log`:
  `cameraPermissionDenialAndGrantThroughSystemDialog`. Chỉ thu hồi quyền
  CAMERA trên userdata QA riêng trước lượt này; từ chối rồi cấp lại bằng
  nút trong hộp thoại Android thật thông qua Accessibility, không inject
  callback. Khi từ chối không có camera bound, nút gallery/My QR vẫn dùng
  được; mở My QR không cần Camera; sau cấp lại CameraX bind thành công.
  Chạy riêng với `-e taktak.allowQrPermissionQa true` và
  `-e taktak.allowQrQa true`. Ca này chủ động skip nếu không có cờ permission
  QA; lượt riêng có đủ cờ đã thực sự chạy và PASS, không tính skip là PASS.
- Assertion file phiên cá nhân không đổi byte-for-byte đạt trong cả 3 ca UI.
  API fixture RAM, không gửi lời mời/chia sẻ thật, không đăng nhập tài khoản.
- Build debug/test/release và 118 JVM test sau sửa My QR đạt:
  `qr-avd-fixed-build.log`, BUILD SUCCESSFUL 45 giây. Build test bổ sung cuối
  đạt 16 giây: `qr-picker-test-build.log`. Không đổi dependency/backend/DB.
- 8 ảnh tại `.local/artifacts/qr-avd-final-images`: camera, my-code,
  lookup-offline, lookup-ready, gallery-result, gallery-picker,
  permission-denied, permission-granted. Đã review hình mã cá nhân và
  trạng thái từ chối quyền. `qr-avd-final-runtime.log` không có
  `FATAL EXCEPTION`/crash Taktak trong lượt kiểm tra.
- Đã tắt AVD sau kiểm thử để trả RAM. Userdata QA và ảnh/log còn trong
  `.local/artifacts`, đã xác minh Git ignore; không commit dữ liệu máy ảo.

Giới hạn ở lượt AVD (đã bổ sung nghiệm thu USB phía trên): camera là virtual scene, chưa xác minh quét quang học và torch
trên điện thoại. Ảnh QR hợp lệ được đưa vào callback gallery từ file cache
QA; đã kiểm thử đọc ảnh và flow sau chọn ảnh, chưa thao tác chọn một ảnh
QR thật trong giao diện picker. Picker thật đã kiểm tra mở/hủy. Chia sẻ
kiểm tra intent text/chooser và chặn trước gửi, không xác nhận delivery
sang ứng dụng bên ngoài. Lint còn lỗi nền đã ghi phía dưới, không bỏ qua.

## Hồ sơ lượt USB ban đầu

## Bằng chứng trước sửa

- USB `8e90fdff`, Xiaomi 23129RAA4G, kích thước 1080 × 2400; Camera đã cấp quyền.
- Build baseline app + test APK đạt: `.local/artifacts/qr-baseline-build.log`.
- Đã cài baseline qua `adb install -r`, không xóa dữ liệu ứng dụng.
- `QrRegressionTest` chạy trên USB: **2/2 FAIL**, 0,337 giây,
  `.local/artifacts/qr-baseline-usb.log`.
  - Ảnh `ImageDecoder.ALLOCATOR_HARDWARE` không đọc được bằng `getPixels`,
    bị bắt lỗi rồi trả về không tìm thấy QR.
  - Parser nhận nhầm host/path gần giống URL Taktak hợp lệ.
- Review source còn phát hiện đọc plane Y bỏ qua row/pixel stride,
  thiếu xoay 180°, thiếu xin quyền Camera, executor không đóng, callback
  có thể chạy sau khi Activity/dialog đã đóng, chỉ kiểm tra trang bạn bè đầu.
- Khi API lỗi/không tìm thấy username, code cũ vẫn có thể gửi lời mời tới
  UUID lấy trực tiếp từ QR. Không chạy thao tác gửi thật để tái hiện lỗi này.

## Thay đổi

- `QrCodeUtils`: kiểm tra host/path/query/username/UUID, loại mã giả mạo và
  query trùng, giữ hỗ trợ username/link legacy; quiet zone 4 module;
  xử lý hardware bitmap, QR đảo màu, giới hạn kích thước.
- `QrImageDecoder`: software bitmap, thu nhỏ ảnh tối đa 2048 px cạnh dài,
  đóng stream, xử lý ảnh không hợp lệ. Không yêu cầu quyền toàn bộ thư viện.
- `QrLuminance`: đọc plane Y đúng row/pixel stride và vị trí buffer,
  hỗ trợ 0/90/180/270°, không làm thay đổi buffer gốc.
- `QrScanViewModel`: Repository/LiveData, xác minh username và UUID qua API,
  kiểm tra self và toàn bộ các trang bạn bè, chặn gửi khi không xác minh được,
  ràng buộc thế hệ phiên, chống gửi lặp, hủy kết quả đã hết hiệu lực,
  xử lý conflict và dọn executor/listener khi ViewModel kết thúc.
- `QrScanActivity`: xin quyền/runtime settings/retry, CameraX chỉ unbind
  use case của màn QR, tắt torch/dừng camera khi ra nền, khôi phục khi quay
  lại; tạm dừng scan khi mở picker/mã của tôi/dialog; hủy picker không kẹt;
  callback có kiểm tra vòng đời; bitmap gallery được recycle.
- `MyQrCodeBottomSheet`: xử lý danh tính không hợp lệ, disable share nếu
  không tạo được QR, giải phóng bitmap, báo đóng sheet để tiếp tục quét.
  Chia sẻ vẫn là text/link qua chooser, không thêm export ảnh/gallery.
- `FriendsFragment`: bỏ executor rò rỉ ở nút mã của tôi; dùng ViewModel,
  observer theo view lifecycle và hiển thị lỗi thay vì nuốt lỗi.
- Layout scanner thêm trạng thái quyền/retry và system insets. Chuỗi mới
  có tiếng Việt và sáu bản dịch tương ứng locale đang có trong repo.

## Kiểm tra sau sửa

- `testDebugUnitTest`: **118 tests, 0 failure, 0 error**; gồm 3 test mới
  `QrLuminanceTest`: padding/pixel stride/buffer position, bốn hướng xoay
  của ảnh không vuông và dữ liệu plane không hợp lệ.
- Đã tạo APK debug, test và release unsigned bằng Gradle offline.
  Lượt build cuối **BUILD SUCCESSFUL, 26 giây**:
  `.local/artifacts/qr-apk-tests-final.log`.
  Release dùng URL placeholder HTTPS qua tham số build, không đổi URL
  trong cấu hình dự án. APK release chưa ký và không deploy.
- `lintDebug`, `lintRelease`: **FAIL, 201 errors** mỗi biến thể;
  gồm 22 `NewApi` và 179 `MissingTranslation`, ở các file theme/chuỗi ngoài
  phần QR sửa trong lượt này. Ví dụ `values/taktak_styles.xml:20` dùng
  `android:textFontWeight` API 28 khi minSdk 26; các bộ chuỗi auth/recovery
  thiếu bản dịch trong các locale mới. Không suppress hoặc tạo baseline
  để bỏ qua. Các file QR chỉ còn cảnh báo, không có lint error trong báo cáo.
  Log: `.local/artifacts/qr-verification.log`; chạy lại lint trên source cuối
  vẫn FAIL cùng 201 lỗi mỗi biến thể, 24 giây:
  `.local/artifacts/qr-lint-final.log`. Báo cáo tại
  `android-client/app/build/reports/lint-results-{debug,release}.html`.
- Không chạy lại backend test: không sửa backend hoặc migration.
- `git diff --check` trên các file tracked sửa trong phạm vi đạt (chỉ có
  cảnh báo chuẩn hóa LF/CRLF). Quét pattern private key/API key/JWT trong
  source/test QR và báo cáo mới không có kết quả; đây là kiểm tra giới hạn,
  không phải cam kết đã audit toàn bộ credentials của repository.

## Kế hoạch USB trước khi được phép chuyển máy ảo (lịch sử)

- `QrRegressionTest` (5 ca): hardware bitmap, URI giả mạo,
  round-trip/legacy/đảo màu, bốn hướng khung Y có padding, ảnh lớn/hỏng.
- `QrUsbUiTest` (2 ca dài): self/offline/retry/not found/UUID mismatch,
  bạn bè ở trang thứ hai, gửi đúng một lời mời tới fixture RAM;
  bind camera thật, My QR, chặn chooser share (không gửi bên ngoài),
  gallery fixture trong cache, hủy picker thật, recreate và đổi phiên.
- Các test UI cần `-e taktak.allowQrQa true`. `QrFixture` thay AuthGraph
  bằng API proxy RAM trước khi mở màn, khôi phục sau test, kiểm tra file
  phiên mã hóa cá nhân không đổi byte-for-byte. Đây là thiết kế test;
  chưa có bằng chứng chạy sau sửa nên không tuyên bố các assertion đã đạt.
- Chưa nghiệm thu quét QR quang học thực tế, torch, cấp/từ chối quyền
  thực trên thiết bị, chọn một ảnh QR bằng picker và ảnh chụp giao diện sau sửa.
  Không thu hồi quyền Camera cá nhân để giả lập lỗi trong lượt này.

## Chạy tiếp khi USB có lại (chỉ nghiệm thu bổ sung phần thiết bị thật)

Từ root, dùng ADB trong Android SDK:

```powershell
$adb = "$env:LOCALAPPDATA/Android/Sdk/platform-tools/adb.exe"
& $adb devices -l
& $adb -s 8e90fdff install -r android-client/app/build/outputs/apk/debug/app-debug.apk
& $adb -s 8e90fdff install -r android-client/app/build/outputs/apk/androidTest/debug/app-debug-androidTest.apk
& $adb -s 8e90fdff shell am instrument -w -r -e taktak.allowQrQa true -e class com.taktak.app.ui.qr.QrRegressionTest,com.taktak.app.ui.qr.QrUsbUiTest com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner
```

Đối chiếu serial thực tế trước khi chạy. Chỉ đọc QR thử, không chứa dữ liệu
riêng tư; chia sẻ dừng tại chooser; không gửi lời mời bằng tài khoản cá nhân.
Sau test kéo ảnh từ external files `artifacts/qr` về `.local/artifacts/qr`.
Không báo PASS nếu không chạy được hoặc còn assertion thất bại.

Lúc bàn giao USB ban đầu, `adb devices -l` không có thiết bị. **APK sửa QR chưa được cài
lên điện thoại**; bản cài gần nhất của lượt này là baseline trước sửa.
Không cần USB cho các ca AVD đã đạt ở trên. Khi cần nghiệm thu camera thật:
cắm lại USB, mở khóa, cho phép debugging và giữ Taktak
ở foreground. Phần lint ngoài QR cần một lượt sửa riêng được phê duyệt;
không ghi đè các thay đổi theme/ngôn ngữ chưa commit của người dùng.
