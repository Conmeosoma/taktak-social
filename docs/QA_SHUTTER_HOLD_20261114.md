# QA nút chụp: chạm chụp, giữ quay

Yêu cầu người dùng ngày 14/11/2026 thay thao tác chọn mode Live riêng.
**PASS phạm vi này**, không thay kết luận Advanced4 còn các ca E2E mở.

## Hành vi

- Chạm shutter chụp ảnh; nếu đang bật ảnh kép thì giữ xử lý ghép ảnh hiện có.
- Giữ qua ngưỡng long-press của Android chuyển sang quay camera đang chọn.
  Thả tay dừng sớm; tự dừng sau tối đa3giây ghi hình. Không chụp thêm ảnh sau giữ.
- Nếu thả trước khi video camera sẵn sàng thì hủy yêu cầu quay chờ.
- Kéo ngón tay ra ngoài/hệ thống gửi CANCEL hoặc nhiều ngón tay thì hủy;
  rời Camera dừng và bỏ video. Timer cũ không dừng một recording mới.
- Không có nút Ảnh/Live riêng. Mic vẫn là tùy chọn và không tự bật.
- ShutterButton kế thừa AppCompatImageButton, gọi performClick/performLongClick
  và công bố long-click qua listener native. Hướng dẫn/TalkBack có bảy ngôn ngữ.

Để tương thích thiết bị không hỗ trợ đồng thời ba use case, ảnh vẫn bind
Preview+ImageCapture; chỉ khi giữ mới rebind Preview+VideoCapture. Vì thế có
thời gian ngắn khởi tạo recorder sau ngưỡng long-press. Không ép bind ba luồng.

## File thay đổi

- CameraFragment.java: bắt đầu/kết thúc hold, cancel/lifecycle/timer và reset lỗi.
- ShutterButton.java mới; fragment_camera.xml dùng nút native này, bỏ hai mode.
- camera_strings.xml mặc định và feature_strings.xml sáu locale: hướng dẫn mới;
  gỡ hai chuỗi mode không còn được dùng khỏi preview_strings/feature_strings.
- AdvancedFeaturesDeviceQaTest.java: dùng DOWN/UP/CANCEL qua View dispatch thật,
  chạm60ms chụp, giữ4500ms để kiểm auto cap, thả1800ms để kiểm dừng sớm,
  hủy1800ms không mở Preview. CameraX dùng phần cứng thật, không mock recorder.
- PROJECT_STATUS/TASKS/SESSION_HANDOFF/PROJECT_DECISIONS/LIVE_MOMENT_VALIDATION
  và tài liệu QA này. Không sửa server trong lượt này.

## Kết quả thực tế

Thiết bị Xiaomi23129RAA4G, Android15, USB; ảnh/video QA chỉ cache, fixture
session/API trong RAM; không gửi media ra Internet, không đổi tài khoản cá nhân.

| Kiểm tra | Kết quả | Log trong .local/artifacts |
|---|---|---|
| APK debug và AndroidTest | BUILD SUCCESS | shutter-hold-accessibility-build.log |
| JVM | 143/143,0fail/error/skip | XML testDebugUnitTest và log build trên |
| Lint debug | 0errors,105warnings | lint-results-debug.txt |
| USB toàn nhóm | 8/8,20,302s,OK | shutter-hold-usb.log |
| Whitespace | git diff --check không có lỗi | chạy workspace |

Ca camera chứa assertion ảnh vuông, hủy không mở Preview, giữ lâu chỉ tạo video
với duration2.000–3.300ms, silent không audio track; mic bật/thả sớm tạo video
có audio và duration>0/<2.500ms. Các ca Outbox/checkpoint/cancel/generation/
SQLite/compositor cũng chạy lại đạt. Quyền mic trước/sau đều false, runner chỉ
thu hồi sau suite; draft QA do test tạo được dọn.

Lượt build đầu lỗi trùng tên biến lambda với tham số Java, đã sửa. Lượt sau
đạt nhưng có ba warning mới (hai resource mode thừa, touch accessibility);
đã thay bằng ShutterButton có performClick và gỡ resource thừa, chạy lại
build/lint/USB đủ bộ. Không suppress lint hoặc bỏ test.

Ảnh local: shutter-hold-camera.png và shutter-hold-preview.png. Ảnh Camera
được chụp ngay sau bind nên preview còn đen; dùng để đối chiếu bố cục nút,
không coi là bằng chứng live preview đã streaming. Phần control ba nút có
wrap chữ từ giao diện trước, chưa redesign trong yêu cầu thao tác này.

Chưa nghiệm thu thủ công TalkBack, tất cả theme/locale/thiết bị hoặc rời app
giữa recording qua tương tác USB. Các nhánh này có xử lý code nhưng không
đánh dấu PASS hành vi chưa chạy. Backend không đổi nên không chạy lại Maven;
không upload/download video thật hoặc deploy/commit/push.
