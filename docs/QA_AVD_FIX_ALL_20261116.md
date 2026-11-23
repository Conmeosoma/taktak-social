# Sửa lỗi và nghiệm thu AVD — 16/11/2026

Trạng thái: PASS trong phạm vi sửa lỗi và nghiệm thu local/AVD đã chạy. Người dùng yêu cầu “fix all
với máy ảo”, sau đó duyệt tiếp tục dùng tunnel tạm vì chưa có domain.
Skill: android_ui_verification. Không commit/push/deploy hoặc sửa dữ liệu VPS.

## Lỗi đã sửa

- Camera/Live: timer UI cũ tính cả thời gian chờ frame đầu, có thể kết thúc clip
  bằng ERROR_NO_VALID_DATA trên AVD. Giới hạn 3 giây chuyển vào Recorder;
  ERROR_DURATION_LIMIT_REACHED được xử lý như kết thúc hợp lệ. Watchdog riêng
  15 giây bảo vệ trường hợp không có dữ liệu. Trạng thái đang quay chỉ hiển thị
  khi recorder đã có dữ liệu, không ngay lúc tạo recorder.
- QA Camera đồng bộ theo trạng thái quay thực thay vì sleep cố định tính cả
  thời gian bind camera; giữ assertion duration/audio/cancel/playback.
- Poll: tái hiện vote không chờ transaction close commit. Vote và close dùng
  cùng khóa moment với create, chặn ghi dựa trên trạng thái OPEN đã cũ.
  Không thay schema, API hoặc migration đã áp dụng.
- Preview: màu chữ Xóa hết theo palette theme, thanh công cụ không căn baseline
  gây cắt nhãn nhiều dòng. Có ma trận layout/font/contrast hồi quy.
- System bars: chọn icon theo nền Taktak thực, thay vì colorSurface mặc định
  Material; so sánh tương phản đen/trắng. Kiểm tra runtime qua 8 appearance.
- AI/History/Poll: dùng format resources thay ghép chuỗi UI; dịch nút đóng Poll
  trong 7 locale. Glyph AI chuyển sang resource không dịch.
- QR overlay tái sử dụng Path thay vì cấp phát mỗi frame.
- Outbox singleton luôn giữ application context.
- Mô tả widget 4×2 trong 7 locale không còn hứa thao tác reaction chưa hỗ trợ.
- AuthGraph có factory gắn cleanup phiên dùng chung cho graph thật và transport
  QA; kiểm thử widget inject transport QA không còn bỏ sót callback logout.
- Fixture graph đồng bộ với khóa khởi tạo AuthGraph; cleanup Media dùng store
  draft đã sở hữu, không tạo lại store từ phiên đã logout.
- Harness QR nhận được Activity PAUSED/STARTED khi dialog quyền đang ở trước;
  vẫn kiểm tra từ chối/cấp lại bằng dialog hệ thống thật, không tự giả kết quả.

## Bằng chứng kiểm tra

Log nằm trong .local/artifacts/, không chứa trong Git:

| Phạm vi | Kết quả / log |
|---|---|
| Backend đầy đủ | 231/231, không fail/error/skip; qa-all-backend-recompiled.log |
| Poll race trước sửa | FAIL xác nhận vote không chờ close; qa-all-poll-race-before.log |
| Poll sau sửa | 8/8; qa-all-poll-after.log; được chạy lại trong full suite |
| Android build/unit/lint | BUILD SUCCESS; 144/144 unit test; lint 0 lỗi, 114 cảnh báo; qa-all-delivery-build.log |
| Camera/Live thực trên AVD | PASS clip có/không mic, cap, thả dừng, cancel, playback; qa-all-camera-recorder-limit.log |
| Hồi quy tổng hợp trước sửa cuối | 67 phát hiện, 65 đạt, 2 assumption skip; qa-all-runtime-final.log |
| Hồi quy tổng hợp bản cuối | 67/67 PASS, 0 skip, 351,862 giây; qa-all-runtime-verified.log |
| Chat FCM thật | 1/1; service callback, recipient GET, idempotency, thông báo nền chung; qa-all-fcm-cloud.log |
| FCM → Worker → RemoteViews | 1/1; foreground/background/delete/logout, đọc được title logout; qa-all-widget-cleanup-final.log |
| Outbox offline → online | 1/1, 36,163 giây; WorkManager/HTTP thật, một moment và payload được dọn; qa-all-outbox-network-02.log |
| Outbox hai process | Write/read đều đạt, force-stop giữa hai lượt; qa-all-outbox-process-write.log và qa-all-outbox-process-read.log |
| Android Outbox Live + Photo Poll | Đạt ca MediaApi; qa-all-media-api-and-preview-matrix.log có ca ma trận cùng lượt FAIL, không coi toàn suite PASS |
| Preview/system bars bản sửa cuối | 4/4, 29,519 giây; qa-all-preview-systembars-verified.log |
| QR permission thật | 1/1, 6,744 giây; qa-all-qr-permission-verified.log |
| QR picker thật | 1/1, 13,038 giây; qa-all-qr-picker-automated.log; pha gallery, không claim torch/optical |
| Load local có giới hạn | 1/1 PASS, 0 skip; qa-all-load-final.log; 240 request đo, không lỗi |
| Widget ghim launcher | PASS ghim widget 2×2 trên Pixel Launcher, hiển thị trạng thái chưa đăng nhập; đã gỡ đúng widget QA |
| APK cuối | Cài và cold launch tới Login; apksigner verify exit 0; BASE_URL HTTPS đúng; SHA256 bản Downloads khớp bản build |
| HTTPS VPS | GET /actuator/health trả status UP tại thời điểm bàn giao; không chứng minh toàn bộ nghiệp vụ production |

Lượt full verify đầu gặp class test sinh ra bị JVM báo dư byte. Giữ file lỗi
trong artifacts, biên dịch lại và chạy đủ 231 ca, không bỏ kiểm thử.
Các log FAIL ban đầu Camera, fixture, picker và matrix được giữ để đối chiếu.
Picker cần xác nhận Done sau chọn ảnh; controller chỉ tap khi thumbnail khớp
ảnh QR QA và đang ở PhotoPicker, không inject callback/URI.

## Giới hạn và dữ liệu QA

- AVD Medium_Phone, Android 37.2, 1080×2400. Camera trước/sau là camera emulated.
  Không có xác nhận phần cứng torch, OEM pin policy, âm thanh người nghe hoặc
  ma trận API26–30 từ lượt này.
- FCM dùng Firebase thật nhưng tài khoản, API và PostgreSQL là QA loopback riêng.
  Host RemoteViews độc lập với auth routing chỉ có trong debug, không export.
  Đã nghiệm thu ghim widget trên Pixel Launcher của AVD; chưa nghiệm thu chính sách launcher/OEM máy thật.
- Video fixture Android test chỉ là clip màu tổng hợp, cùng SHA256 với fixture
  server: C2B164012615B494854E1BFA02BB0DD4FFE078E62D92A06B7A82B2698EDC71A3.
- Hai process Outbox chứng minh DB/checkpoint/key bền vững qua force-stop;
  không tự coi là thử process chết đúng giữa giao dịch upload server.
- Không gửi email, gọi Gemini thật hoặc dùng tài khoản cá nhân. Không ghi raw
  QA token/mật khẩu vào report/Git; credential test chỉ ở RAM hoặc store sẵn có.
- VPS chưa nhận bản sửa Poll của lượt này. Tunnel tạm vẫn có thể đổi URL;
  người dùng chưa có domain và đã chọn tiếp tục dùng tunnel tạm.
- Không coi kiểm thử local/AVD là nghiệm thu tải dài hạn, alert routing,
  mixed APK/rollback production hoặc release signing.

## Đo tải, review và artifact bàn giao

AVD đã tắt trước phép đo tải. Dataset gồm 25 user, 24 conversation, 1.440 message,
24 ảnh; concurrency 4, sáu nhóm mỗi nhóm 40 request. p95 (ms): profile 55,09;
inbox 66,34; chat messages 73,77; received history 62,05; private image 29,08;
chat send/retry 226,80. Inbox dùng 4 prepared statement/request, trong ngân sách
test. Báo cáo máy đọc: server/target/system-load-qa/report.json. Đây là phép đo
loopback giới hạn trên laptop, không phải cam kết năng lực VPS hoặc tải dài hạn.

Lint còn 114 cảnh báo, chủ yếu unused resource, overdraw, plurals candidate và
notifyDataSetChanged; không tuyên bố đã xử lý mọi cảnh báo. Không tự nâng dependency.
git diff --check đạt; quét 76 file text thay đổi không phát hiện mẫu credential
đã kiểm tra. Quét theo mẫu không thay thế kiểm toán bí mật toàn diện.

APK debug: C:/Users/NamTien/Downloads/Taktak-20261116-qa-fixed.apk.
SHA256: 9F2D61B3EF156F41A7E927893D8DF0C940F635AFFEFC610E10896112F4003A3B.
JAR backend local: 53F51933EC6AECE9B839CE137A3FFFCD43CD4ABA25032B2A3C1D13C2DC5404FE.
JAR này chứa sửa Poll và chưa deploy; VPS vẫn dùng bản triển khai trước.

Ảnh final-login.png, preview-poll-selected.png, widget-launcher.png nằm trong
.local/artifacts/qa-all-20261116-images/. Đã xem trực tiếp ảnh; Preview là trạng
thái đã cuộn để kiểm tra toolbar/Poll, không phải ảnh toàn trang.
Các process backend QA đã dừng, media QA đã được dọn, reverse ADB/quyền bind
widget tạm đã gỡ và AVD đã tắt. Không thay phiên/tài khoản cá nhân.

Hành động tiếp theo nếu người dùng yêu cầu: triển khai JAR đã kiểm thử, rồi
nghiệm thu Poll đồng thời trên VPS. Các cổng máy thật/API cũ, vận hành dài hạn,
mixed version/rollback và release signing vẫn cần bằng chứng riêng.

## File thay đổi của lượt này

Production source: PollService; AuthGraph; CameraFragment; ThemeManager;
AiFragment; HistoryFragment; MomentViewerActivity; OutboxRepository;
QrScannerOverlayView; activity_preview.xml; activity_moment_viewer.xml;
fragment_ai.xml; presentation_formats.xml và ui_review_fixes.xml của 7 locale;
strings.xml/feature_strings.xml cho mô tả widget trong 7 locale.

QA: fixture Media/Chat/QR, MediaThemeUiTest, AdvancedFeaturesDeviceQaTest,
AiChatUiTest, UiReviewLayoutTest, QrUsbUiTest, RealFcmWidgetAcceptanceTest,
OutboxProcessReloadTest, OutboxNetworkAcceptanceTest, OutboxMediaApiAcceptanceTest,
AdvancedFeaturesQaTest, video asset, debug WidgetQaHostActivity và manifest debug.
Các thay đổi UI/báo cáo có từ trước được bảo toàn.
