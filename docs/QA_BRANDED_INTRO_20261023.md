# Branded intro MP4 có tiếng — 23/10/2026

## Kết quả

**CODE COMPLETE; xác minh trên Xiaomi Android15. PARTIAL đối với toàn bộ ma trận
thiết bị/chế độ âm thanh.** Người dùng đã xác nhận: nghe được soundtrack gốc,
không thấy lệch tiếng/hình rõ rệt. Không dùng xác nhận này để tuyên bố độ lệch
AV đo được bằng0ms hoặc frame pacing đã benchmark.

Yêu cầu video+sound mới thay thế giới hạn không dùng MP4/âm thanh và renderer
520ms ở [QA splash trước](QA_SPLASH_20261023.md). Không thay backend/JWT/API/FCM.

## Tài nguyên và phân tích nguồn

- Nguồn: `gemini_generated_video_5143011b (online-video-cutter.com).mp4`.
- Media gốc được copy nguyên byte đến
  `android-client/app/src/main/res/raw/taktak_intro.mp4` (253.483byte).
- H.264 High/yuv420p,720×1280,SAR1:1,30fps; AAC-LC48000Hz stereo.
  Duration khoảng3,34s, timestamp bắt đầu0. FFmpeg đã decode cả hai stream.
- Video SHA256: `1B133D6F0E56CCFABB6DA123920D987A881C8F15B76E5BC56C7B76ABB6A9227F`.
- AAC bitstream trong MP4 và `original_audio.m4a` có cùng SHA256:
  `5a15454f6efb8631f2f8735f0ff96c46075591f27b31952e8f34c49b616e8542`.
  **Không bundle/phát thêm M4A**, không hai luồng tiếng, không thay soundtrack.
- Đã xem contact sheet FFmpeg3fps: logo reveal/scale, heart/sparkles, Taktak,
  highlight và giữ frame thương hiệu cuối. Không vẽ lại logo hoặc animation.
- Background tĩnh #19162C lấy mẫu vùng góc hình tham chiếu; video có gradient
  nhẹ nên không cam kết mọi pixel vùng fit ngoài video trùng mọi frame.

## Kiến trúc và hành vi

System SplashScreen vẫn dùng AndroidX1.2.0, nền đồng màu/icon trong suốt,
remove khi ready; không giữ hệ thống chờ hết clip hoặc thêm SplashActivity.
Sau đó MainActivity chứa branded intro XML/ViewBinding+TextureView+MediaPlayer.
Nguồn local, prepareAsync; audio và video dùng **cùng clock của một player**.
TextureView fit-center giữ tỷ lệ, không centerCrop/cắt logo/chữ. Không giả status bar.

- Full Intro và sound mặc định bật. IntroPolicy tiêu thụ cơ hội một lần/process;
  chỉ launcher mới không saved state/entry đặc biệt và animator enabled mới phát.
- Quick Start/animation disabled bỏ branded intro. Notification/action đặc biệt
  không chờ intro; đổi tab, quay lại foreground hoặc recreate không replay.
- Bỏ qua/Back dừng ngay. onPause/destroy/surface destroyed/low-memory cũng
  release; không tiếp tục âm thanh nền. Init/error bỏ intro; watchdog5s chỉ
  bảo vệ surface/prepare/window-focus, không giữ splash để trang trí.
- Chờ window focus trước khi bắt đầu cả hai track, không chạy hình rồi phát tiếng
  muộn. Player không loop, callbacks cũ kiểm tra generation instance/done.
- SessionViewModel vẫn xác minh profile/refresh như cũ. Trong intro chỉ hoãn
  render private UI/điều hướng; kết thúc dùng trạng thái xác thực mới nhất.
  Không tin token local, không sửa SessionManager/Repository/ViewModel auth.
- API26 dùng các native API đã có; chưa có device API26–30 để chạy thật.

## Âm thanh và cài đặt

Hồ sơ → **Hiệu ứng khởi động** mở dialog tối thiểu trong màn hiện có:
Full Intro/Quick Start, Âm thanh khởi động, Xem thử animation. Không Activity
settings mới. SharedPreferences `startup_intro` riêng, không JWT/session.
Preview phát lại có Bỏ qua/Back; nút tiếng trên intro cập nhật lựa chọn sound.

Mặc định audio chỉ được bật khi foreground/window focus, ringer NORMAL,
DND ALL, media volume>0, stream không muted và không có nhạc khác lúc vào intro.
Nếu không đáp ứng thì vẫn phát hình và mute. Không set âm lượng hệ thống hoặc
tắt DND/ringer của người dùng. Cục bộ player gain0,55; không yêu cầu quyền mới.
AudioFocus transient-may-duck chỉ xin sau guard nhạc khác; focus denied/lost,
ringer/DND đổi hoặc window focus mất thì mute/abandon. Không tự resume tiếng
sau interruption. Không tuyên bố loại bỏ được mọi race với app khác vừa phát nhạc.

## File thay đổi của lượt video

- MainActivity.java: phối hợp intro/lifecycle/presentation gate, không đổi xác thực.
- ProfileFragment.java và fragment_profile.xml: nút settings intro.
- ui/splash/: IntroPolicy, IntroPreferences, BrandedIntro, IntroSettings (Java).
- raw/taktak_intro.mp4; drawable/splash_empty.xml; values/splash.xml.
- layout/view_branded_intro.xml và dialog_intro_settings.xml.
- values và values-{en,es,hi,ja,ko,zh}/intro_strings.xml:8 chuỗi/locale.
- IntroPolicyTest, BrandedIntroUiTest, IntroColdLaunchUiTest.
- README, PROJECT_DECISIONS/STATUS/TASKS/SESSION_HANDOFF và hồ sơ này.

Gỡ riêng renderer/test/resources splash520ms do agent tạo ở lượt trước:
SplashExitAnimation.java, SplashExitAnimationTest.java, splash_accents.xml và
ba drawable splash_logo/heart/sparkle. Logo Login gốc không đổi. Các file cũ này
chưa tracked ở HEAD; không coi `git checkout` là cách khôi phục chúng. Hồ sơ
QA cũ vẫn giữ lịch sử. Không xóa file, dữ liệu hoặc thay đổi khác của người dùng.

## Kiểm thử thực tế

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug | PASS; build đầu39s, bản sau60s, handoff49s |
| JVM |135/135,0failure/error;8 test policy mới; lần handoff unit up-to-date |
| Resources |PASS2.304 localized strings; XML/duplicate/coverage/format |
| Lint debug |0error/85warning, không baseline/suppress; có Overdraw của overlay intro |
| USB bộ cuối |22/22 PASS125,346s: intro6+routing3+List4+Chat6+Profile3 |
| Launcher đầu process QA riêng |1/1 PASS7,289s; lần quay sạch1/1 PASS7,192s |
| Sound OFF |Native player vẫn hoàn tất; policy mute, không player thứ hai |
| Silent thực |ringer0/media10/DND ALL → MUTED_BY_DEVICE_POLICY |
| Normal thực |ringer2/media10/DND ALL → AUDIBLE_POLICY_ALLOWED; người dùng xác nhận nghe/không lệch rõ |
| Skip/background/recreate/low-memory callback |PASS fixture: player+Surface null sau kết thúc, Home return không replay |
| Session/settings |Prefs QA khôi phục; bytes phiên mã hóa trước/sau toàn lượt không đổi |
| Git diff --check |PASS |

APK cuối đã cài-r app/test Success, SHA256:
`B4067B00BE46D382F15051B478AAA8124E0319941F086926023DA081E401E1D9`.

Giữ bằng chứng thất bại ban đầu:4/5 khi task QA tái sử dụng sai giữa fixture;
sửa first-entry CLEAR_TASK riêng trong harness, không đổi assertion sản phẩm.
Lượt sau6/6 PASS17,576s. Cold test ban đầu FAIL vì đợi cố định3,5s từ isPlaying;
isPlaying không bảo đảm decoder đã hiển thị frame đầu. Đã chờ callback completion
có deadline, không thêm delay production; hai lần cold cuối đạt như bảng.

Tên class `IntroColdLaunchUiTest` mô tả gate first-entry; instrumentation đã mở
process trước khi mở Main. Đây **không phải phép đo cold-process OS standalone**
hoặc TTID, cũng không chứng minh mọi warm-start OS. Không force-stop rồi mở
phiên cá nhân chỉ để đo vì gate QA ở lượt này giữ nguyên tài khoản/phiên thực.

## Bằng chứng và giới hạn

Artifacts Git ignore `.local/artifacts/acceptance-all/`:
intro-reference.png, intro-build.log, intro-handoff-build.log,
intro-usb-regression-final.log, intro-usb-cold-final.log,
intro-usb-cold-clean.log, intro-acceptance-clean.mp4, intro-acceptance-frames.png.

Video cuối540×1200 giữ tỷ lệ màn1080×2400. Đã review hình sau khi người dùng
đóng bong bóng Zalo: đúng nguồn MP4, đủ heart/sparkles/text, không góc trắng PNG
cũ. Có native controls/insets và fit margins theo tỷ lệ thiết bị, khác video
tham chiếu toàn khung là có chủ đích. Không chỉnh nội dung logo trong video.

ADB screenrecord **không thu internal audio**; không bật microphone. Audio được
xác minh riêng bằng bitstream AAC, policy/player và người dùng nghe trực tiếp;
chưa đo AV offset bằng thiết bị đo hoặc capture hai track phát thực tế.
Video thô có thể chứa launcher/bong bóng bên ngoài app, chỉ giữ local, không Git
hoặc public README. Clip cắt chỉ để QA, không thay tài nguyên MP4 của ứng dụng.

Chưa chạy: API26–30, DND/vibrate thực, nhạc app khác/focus race thực, codec lỗi
fault-injection, áp lực RAM hệ thống thật, benchmark jank/battery, mọi loại deep
link/notification thực, JWT expired/logout qua server. Low-memory ở test là gọi
callback, không tuyên bố đã stress RAM. Unit policy không thay nghiệm thu OEM.
Cold/warm/hot-process standalone chưa được tách thành phép đo kiểm soát.

Full Intro thêm3,34s nội dung và thời gian surface/decoder; Quick Start bỏ chi phí
này. Video chỉ253KB, không thêm multimedia library nhưng có chi phí GPU/decoder;
không gọi startup là nhanh hơn khi chưa có baseline benchmark.

## Bàn giao

Không commit/push/deploy hoặc backend/database change. Các thay đổi dirty khác
được giữ nguyên. Core video/sound/settings đã triển khai và đạt phạm vi USB;
gate đa thiết bị/chế độ âm thanh còn PARTIAL như giới hạn trên. Bước đề xuất
tiếp theo chỉ là nghiệm thu API26–30/audio interruption, không tự triển khai.

Tham khảo native lifecycle/audio focus:
[MediaPlayer](https://developer.android.com/media/platform/mediaplayer/state-resources),
[Audio focus](https://developer.android.com/media/optimize/audio-focus).
