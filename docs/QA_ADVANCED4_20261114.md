# QA bốn tính năng nâng cao —14/11/2026

## Kết quả mới nhất — Live Moment, microphone và lint

**PASS cho phạm vi sửa này. Toàn bộ Advanced4 vẫn IN PROGRESS.** Sáu lỗi ban
đầu đã có sửa và assertion hồi quy đạt; các ca E2E chưa chạy vẫn giữ mở.

Thay đổi:

- LiveVideoValidator mới: MP4/H.264, optional AAC, max10MiB, max3.300ms,
  max1920 mỗi chiều/2.073.600pixel; ffprobe metadata + FFmpeg full decode,
  chặn protocol mạng, timeout15s/process, tối đa2 request/JVM, dọn file tạm.
- PrivateImageStorage/MomentService dùng metadata thực, không clamp duration
  client thành3giây; đăng ký rollback thumbnail trước ghi video.
- Manifest RECORD_AUDIO và microphone optional; LiveVideoMetadata dùng
  MediaMetadataRetriever trên minSdk26. PreviewViewModel/OutboxUploadWorker
  không còn hard-code3000/false. Không đổi CameraX/API/JWT/phiên cá nhân.
- values-es/hi/ja/ko/zh feature_strings: 30 bản dịch mỗi locale, không suppress lint.
- Tests: LiveVideoStorageTest, AdvancedFeaturesQaTest, AdvancedFeaturesDeviceQaTest;
  ba fixture MP4 màu tổng hợp, runner scripts/qa/advanced4-usb.ps1 mới.
- Tài liệu: LIVE_MOMENT_VALIDATION, PROJECT_STATUS, TASKS, SESSION_HANDOFF và
  PROJECT_DECISIONS. Giữ nguyên toàn bộ thay đổi người dùng có sẵn.

Kết quả đã chạy:

| Kiểm tra | Kết quả | Bằng chứng trong .local/artifacts |
|---|---|---|
| Backend full verify | 228/228 PASS,0fail/error/skip, BUILD SUCCESS | advanced4-live-backend-verify.log |
| Backend bổ sung mới nhất | 13/13 PASS, BUILD SUCCESS | advanced4-live-additional-verify.log |
| Android debug/test APK, JVM, lint | BUILD SUCCESS;143/143;0errors/105warnings | advanced4-live-final-android.log |
| Test APK sau sửa Preview harness | BUILD SUCCESS | advanced4-live-preview-qa-build.log |
| USB Xiaomi Android15 | 8/8 PASS,19,599s | advanced4-live-usb-final.log |
| Diff whitespace | git diff --check không có lỗi | chạy tại workspace |

13 ca targeted bao gồm các ca trùng fullsuite và hai ca thêm sau fullsuite:
metadata client giả không được lưu, frame hỏng dù container còn metadata.
Không cộng số test của hai lượt thành số ca duy nhất/fullsuite mới.

USB ghi hai clip có/không mic vào cache và ảnh kép theo fallback tuần tự.
Đọc metadata thật, xác nhận audio track và giới hạn thời lượng, kiểm tra JPEG
vuông; session/API fixture RAM, không gọi upload thật. Đã xem ảnh
advanced4-live-preview.png và advanced4-dual-preview.png cuối: Preview hiển thị
đầy đủ, không còn transition/session error che ảnh. Không đưa ảnh QA vào Git.
Các draft do test tạo được dọn, microphone được khôi phục false sau suite.

Lượt USB trước chỉ hoàn tất2ca rồi Android kill process do revoke quyền mic
trong finally. Runner mới revoke sau suite; lượt cuối đủ8ca. Windows chặn chạy
.ps1 mặc định nên dùng ExecutionPolicy Bypass chỉ cho process, không đổi policy
máy. Lượt lint/build đầu và sau sửa đều đạt, không tạo baseline để che lỗi.

Giới hạn: 105warning lint vẫn còn. Chưa chạy tương tác từ chối quyền qua dialog,
offline→online/process-death đầy đủ, vote/close race, playback/private cache,
hoặc upload/download Live qua VPS. Video giữ nguyên bytes, không cam kết scrub
metadata. Runtime mới FFmpeg/ffprobe cần chuẩn bị trước deploy; chưa load-test
chi phí decode. Không commit/push/deploy hoặc migrate production.

Chi tiết [hợp đồng và chuẩn bị runtime](LIVE_MOMENT_VALIDATION.md).
Các phần tiếp theo lưu lịch sử QA, không phải trạng thái mới nhất.

## Lượt sửa tiếp theo: Photo Poll và Outbox

Người dùng yêu cầu tiếp tục sau đề xuất sửa hai nhóm này. Đã sửa code trực tiếp,
không deploy hoặc push. Các kết quả FAIL ban đầu bên dưới được giữ làm lịch sử;
chưa được xem là toàn bộ bốn tính năng đã nghiệm thu.

- PollService nhận actor từ JWT, kiểm tra user/chủ ảnh/chưa xóa trước khi ghi,
  khóa row ảnh và trả response trong cùng transaction. Cả POST Poll riêng và
  nhánh Poll trong upload multipart đều truyền actor. Retry cùng nội dung trả
  Poll cũ; nội dung khác trả409; khóa row tuần tự hóa tạo đồng thời.
- SQLite Outbox V1→V2 thêm `created_moment_id`, không drop dữ liệu. Sau upload
  lưu checkpoint trước tạo Poll. Poll503 giữ FAILED_RETRYABLE và payload;
  retry dùng checkpoint, không upload ảnh lần nữa. Không nuốt lỗi tạo Poll.
- Các mutation task kiểm generation trong SQL và không ghi đè CANCELLED/COMPLETED.
  Worker kiểm generation của task/phiên và owner profile trước gửi, không bắt đầu
  upload khi transition bị từ chối hoặc WorkManager đã stop. PollRepository chặn
  phiên cũ và không nhận body thành công thiếu/sai Poll như một kết quả hợp lệ.

Kiểm tra đã chạy:

- Backend Poll targeted: **5/5 PASS**,0fail/error/skip; `advanced4-poll-fixed-tests.log`.
- Android build debug/test +143unit tests: PASS; `advanced4-integrity-reviewed-build.log`.
- USB bản cuối: **6/6 PASS,0,562s**, `advanced4-integrity-reviewed-usb.log`.
  Bao gồm generation sai, Poll503→retry thành công với đúng1upload, checkpoint
  giữ qua mở lạiDB, late progress không hồi sinh task đã hủy, migration giữ row,
  persistence và compositor. Không chụp/quay mới trong lượt sửa này.
- Full backend lượt đầu sau sửa có223ca,32fail/6error, không được coiPASS.
  Phát hiện nhánh multipart còn gọi chữ ký createPoll cũ; đã đồng bộ lại và
  đang chạy lại đầy đủ. Ca MP4 giả vẫn nằm trong suite, không skip/nới assertion.

Giới hạn: chưa tự sửa MP4/audio/lint ở bước này; chưa kiểm Poll vote/close race,
offline→online đầy đủ hoặc process-death E2E. Các task cũ đã báo COMPLETED sai
không được tự phục hồi/xóa/sửa; cần xử lý riêng có kiểm tra. SQLite mới additive
nhưng APK cũ version1 chưa có đường downgradeDB an toàn, không tự rollback APK.

**Kết quả hồi quy cuối:** `advanced4-integrity-regression-final.log` chạy223ca,
222PASS/1FAIL/0error/0skip,02:10. Ca duy nhất còn FAIL là
`fakeEightByteMp4MustBeRejected`. Hai nhóm MomentIntegrationTest và
MomentPushIntegrationTest đã chạy lại đạt sau khi đồng bộ caller multipart.
Không coi lệnh Maven verify này là BUILD SUCCESS; giữ nguyên assertion MP4.
Phạm vi sửa lỗi tạo Poll + Outbox đã đạt các ca liên quan; toàn Advanced4 vẫn FAIL.

File production đã sửa trong lượt này: server `MomentRepository.java`,
`PollService.java`, `PollController.java`, `MomentController.java`; Android
`OutboxDatabaseHelper.java`, `OutboxRepository.java`, `OutboxUploadWorker.java`,
`PollRepository.java`. Hai test acceptance được bổ sung retry/concurrency,
migration/persistence/cancellation; docs trạng thái/bàn giao được cập nhật.
V8/V9 không thay đổi; không chạm database VPS hoặc session store cá nhân.

**Kết luận: FAIL, chưa đủ điều kiện nghiệm thu hoặc deploy.** Phạm vi chỉ QA;
không sửa source production, không commit/push/deploy, không áp V8/V9 lên VPS.
Giữ nguyên các thay đổi chưa commit của người dùng.

Theo chỉ đạo mới nhất dùng USB Xiaomi23129RAA4G/Android15,1080x2400. AVD ban đầu
khởi động được nhưng mất kết nối trước cài APK; không báo emulatorPASS. Người dùng
cho phép camera trước/sau và quay QA vào cache; không Internet/gallery. AuthGraph
fixtureRAM được khôi phục trong finally; không đăng xuất hoặc sửa tài khoản cá nhân.

## Kết quả chạy

| Nhóm | Kết quả thực tế | Bằng chứng |
|---|---|---|
| Backend hồi quy hiện có |217/217 PASS,0fail/error/skip,03:46; chưa bao phủ các lỗi mới | `.local/artifacts/advanced4-backend-verify.log` |
| Backend acceptance mới |4ca:1PASS/3FAIL,0error; không nới assertion | `advanced4-security-qa.log`, surefire `AdvancedFeaturesQaTest` |
| Android JVM |143PASS,0fail | test-results/testDebugUnitTest |
| Android assemble debug/test APK |Đóng gói được; cài-r lên USB thành công | `advanced4-android-build.log`, `advanced4-device-harness-build.log` |
| Android lintDebug |FAIL31errors/105warnings: thiếu dịch resource và MissingPermission RECORD_AUDIO | app/build/reports/lint-results-debug.txt |
| USB acceptance đầu |5ca:3PASS/2FAIL,11,81s | `advanced4-device-qa.log` |
| USB WorkManager bổ sung |1ca FAIL,0,222s: Poll503 nhưng taskCOMPLETED | `advanced4-outbox-poll-qa.log` |
| USB WorkManager chạy lại cuối |VẫnFAIL,0,226s sau chỉnh điều kiện chờ terminalstate; testAPK build12s | `advanced4-outbox-poll-final.log` |

Ba ca USB đạt: SQLiteOutbox giữ record/idempotencyKey/owner qua close/reopen;
compositor trái/phải tạo JPEGvuông1080 với màuPiP đúng và≤800KiB; camera thật
chụp kép tuần tự tạo JPEGvuông rồi quay Live3s ra MP4cache, duration đọc bằng
MediaMetadataRetriever trong khoảng2–4giây. Không coi đây là kiểm tra dual concurrent,
lip-sync/audio hay upload/download toàn trình.

## Lỗi chặn nghiệm thu

1. **P1 PhotoPoll: ghi trái quyền dù trả404.** PollController gọi createPoll trước
   getPoll phân quyền; hai transaction tách. Người ngoài dùng JWT riêng gọiPOST
   ảnh chủ: response404 nhưng DB có1poll mới. Test yêu cầu0,thực tế1.
2. **P1 PhotoPoll: recipient tạoPoll ảnh người khác.** Vì createPoll không có actor
   và không kiểm sender, recipientPOST nhận201; test yêu cầu403/404.
3. **P1 LiveMedia: chấp nhậnMP4 giả8byte.** sanitizeVideo chỉ kiểm ftyp offset4;
   file không có track/duration/codec vẫn được nhận. Test đòi exception nhưng không
   có. Chưa có bằng chứng server kiểm thời lượng3s thật hoặc loại metadata nhạy cảm.
4. **P1 Outbox: bỏ qua generation khi hoàn tất.** Test tạo task987654321 rồi gọi
   markCompleted bằng987654322: trạng thái chuyểnCOMPLETED, payload bị xóa.
   AssertionPENDING thất bại. Các update/cancel/retry khác cũng cần review owner/
   generation; chưa suy rộng thành ca runtime đã chạy cho mọi method.
5. **P2 LiveAudio: Manifest không khai RECORD_AUDIO.** Runtime check không thay
   được khai báoManifest. NativePackageManager assertion thất bại và lint cũng báo.
6. **P1 Outbox/Poll: mấtPoll nhưng báo xong.** WorkManager thật dùng MomentApi
   fixture thành công và PollApi503. Poll request đã chạy nhưng trạng thái cuối
   COMPLETED thay vìFAILED_RETRYABLE. Task/payloadQA được dọn theo đúngID trong
   finally; không ảnh/token/endpoint production.

## Các phát hiện code review, chưa coi là runtime PASS/FAIL

- Đã tái hiện nuốt exceptionPoll bằng WorkManager (lỗi6); cần giữ IDảnh để retry
  riêng phầnPoll. Chưa thử timeout/idempotency của API tạoPoll dưới tải đồng thời.
- Worker chỉ so generation, chưa kiểm ownerUserId; cần kiểm đổi tài khoản/process.
- PreviewViewModel/OutboxUploadWorker truyền duration3000/hasAudiofalse cố định,
  chưa khớp metadata thực tế khi người dùng bật mic.
- Viewer cachevideo theo momentId, chỉ tải khi chưa có file; cần kiểm thu hồi
  quyền/xóa/đổi phiên và lifecyclecache. Chưa thử truy cập lại video private sau revoke.
- Dual thực hiện fallback tuần tự, không hai CameraX stream đồng thời; UI PiP/
  tên nút cần phân biệt khả năng thực tế.

## Ảnh và giới hạn

Ảnh `advanced4-dual-preview.png`/`advanced4-live-preview.png` trong `.local/artifacts`
đã kéo từUSB và xem. Chụp lúc Activity chuyển màn, preview chưa qua sessiongate
fixture nên **không phải ảnh nghiệm thu UI Preview sạch**. Đã thấy text nútCamera
wrap mạnh; chưa định nguyên nhân hoặc sửa layout.

Còn thiếu: offline→online upload thực, kill/relaunch/đổi user/retry/cancel đang
gửi, Pollvote/concurrency/close/delete/block, Liveaudio/quyền từ chối/playback/
seek/range/download/delete/access control. Không bật mạng bằng cách gọiVPS để
giả thành QA local. Backend4ca dùng PostgreSQLtest mới, không dữ liệu production.

## File thêm và hướng xử lý

- `server/src/test/java/com/taktak/server/AdvancedFeaturesQaTest.java`.
- `android-client/app/src/androidTest/java/com/taktak/app/ui/theme/AdvancedFeaturesDeviceQaTest.java`.
- Tài liệu QA này và hồ sơ trạng thái/bàn giao.

Đề xuất một bước tiếp theo: sửa các lỗi phân quyền/integrity đã tái hiện rồi chạy
lại các ca đỏ trước khi nghiệm thu hoặc triển khai bốn tính năng.
