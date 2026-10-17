# Taktak — 5B: Chụp cache và màn Preview

Ngày: 17/10/2026 (Asia/Saigon). **5B PASS local/USB.** Người dùng duyệt riêng5B sau5A và xác nhận điện thoại/cả hai camera hướng an toàn. Nhánh `chore/push-current-project`, HEAD ban đầu `ed9fde6`; worktree sạch. Commit này đã được push theo yêu cầu riêng trước phiên5B; yêu cầu đó không phải quyền tự commit/push các thay đổi5B.

## UNDERSTAND → PLAN

Đã đọc AGENTS, toàn bộ TAKTAK_PROJECT/roadmap, PROJECT_DECISIONS, trạng thái/tasks/handoff và mã nguồn/test. Mốc5A đã đạt; Camera hiện chụp là upload owner-only ngay. Server hiện đã nhận caption nhưng vẫn từ chối mọi recipient không rỗng, vì gửi bạn bè thuộc5C. Không mở rộng server hoặc giả thành công gửi bạn bè trong5B.

Phạm vi: chụp → JPEG cache private → Preview; chụp lại, caption≤50Unicode code point, chọn Tất cả hoặc nhiều bạn từ API thật và phân trang. Giữ Java/XML/ViewBinding/MVVM, Material1.14.0/M3E palette, CameraX và các API/JWT/FCM đang có. Dùng android_ui_verification để đo máy, thao tác, ảnh và kiểm tra hành vi trên USB. Backend/DB/migration/dependency giữ nguyên.

## IMPLEMENT

- `CaptureViewModel` nhận ảnh CameraX, tái sử dụng PhotoProcessor xoay/crop1:1/nén bounded hiện có rồi ghi JPEG cache qua CaptureDraftRepository; mirror trước crop cho camera trước, không mirror camera sau. Raw bitmap được recycle, ImageProxy close ở mọi nhánh. Không tự upload hoặc xuất gallery khi chụp.
- Cache ở `cacheDir/camera-drafts/<generation>/<UUID>.jpg`, app internal; ID/path được kiểm tra, Intent chỉ truyền UUID/generation. Repository kiểm tra phiên trước/sau I/O, giới hạn byte và TTL24giờ; cache mất/hết hạn có thông báo và chụp lại. Hệ thống có thể thu hồi cache; không giả là lưu bền vững. Chụp lại/Back/logout và ViewModel cleared dọn đúng draft của màn, không duyệt/xóa gallery hoặc DB. Lifecycle/configuration change không xóa draft đang dùng.
- `PreviewActivity` không exported, dùng ViewBinding/Material M3E và edge-to-edge/systemBars/IME/cutout, max480dp/ScrollView/ảnh vuông. SessionViewModel xác minh phiên mỗi foreground trước hiện nội dung; CHECKING/error/permission/Login ẩn ảnh, retry giữ draft, logout clear task. Giữ RequestManager Glide tạo khi Activity còn sống, tắt disk/memory cache cho ảnh private.
- `PreviewViewModel` quản lý file, caption, danh sách bạn/pagination/lỗi/retry, lựa chọn và upload riêng tư. Caption và lựa chọn lưu trong SavedStateHandle, không đưa bitmap/token vào Bundle/Intent; đổi generation không được tái sử dụng draft/caption. Worker I/O có kiểm tra cleared/session trước publish; shutdown chờ task đang chạy rồi dọn draft.
- CaptionFilter đếm code point, hỗ trợ emoji/surrogate, paste/replace/delete và IME span; counter50 thống nhất; chặnNUL. Bạn bè lấy GET/api/friends thật, page20/hasNext, ListAdapter/DiffUtil, giữ các ID đã chọn khi tải thêm; mặc định Tất cả. Không mất lựa chọn/caption khi recreate.
- “Gửi cho bạn bè” vô hiệu hóa kèm thông báo chưa khả dụng vì5C chưa triển khai. “Lưu chỉ mình tôi” có xác nhận riêng, ảnh/caption gửi đúng owner-only API hiện có, rồi GET bằng JWT để hiển thị ảnh server. Đây không phải gửi cho danh sách bạn đang chọn. Thêm caption multipart vào hợp đồng client đã được server hỗ trợ; không đổi endpoint/backend. Retry giữ momentId để chỉ GET nếu đã biếtID; unknown POST vẫn có cảnh báo có thể thêm một ảnh, idempotency đầy đủ thuộc5D.
- CameraFragment chuyển sang CaptureViewModel và chặn mở Preview trùng; giữ camera/flash/lifecycle5A. Bỏ serverImage/retry-upload khỏi Camera vì chuyển sang Preview, thay copy chụp không còn nói tự gửi. MomentViewModel cũ không còn tham chiếu sau tách Capture/Preview nên được gỡ; khôi phục từ Git ed9fde6 được, không xóa dữ liệu người dùng. Cập nhật test3D/5A theo luồng mới, giữ assertion JWT/bytes/rotation/lifecycle.

Đối chiếu API với [CameraX ImageCapture](https://developer.android.com/media/camera/camerax/take-photo) và [InputFilter](https://developer.android.com/reference/android/text/InputFilter). CameraX nhận buffer để xử lý rồi ghi JPEG đã thẳng/vuông vào cache, không cần lưu thêm raw capture/EXIF ra disk. Giới hạn nén800KB đang tái sử dụng3D; không nhận đã triển khai đầy đủ thuật toán5C.

## VERIFY

Máy thật Xiaomi23129RAA4G/Android15/API35,1080×2400; USB reverse8080 tới backend smoke/PostgreSQL/JWT/private storage riêng. Không tài khoản cá nhân hoặc DB dev. Fixture có ownership guard, tài khoản/mật khẩu/token chỉRAM, logout/revoke sau test. Artifacts ignored tại `.local/artifacts/5b/`.

- Build đầu `build-initial.txt`: SUCCESS52s; `build-tests.txt`: SUCCESS50s. Sau sửa lifecycle `build-glide-fix.txt`: SUCCESS24s; full builds sau38s/32s, bản cuối `build-verified.txt` SUCCESS29s: debug/test/release, unit và lint. Lint0errors/3warnings cũ sau xử lý warning mới, không suppress/baseline.
- `preview-initial.txt` crash khi logout do gọi Glide.with(Activity) trong onDestroy lúc Activity đã destroyed. Sửa production giữ RequestManager từ onCreate; `preview-retry.txt` **7/7 PASS27.452s** gồm logout/cleanup,cache thật,Unicode/select/recreate,layout,missing/cross-generation,mirror8transform và private upload/JWT.
- `preview-final.txt`8ca đạt,1ca phân trang lỗi ApiFailure do tạo21 tài khoản chạm quota đăng nhập của backend. Không coi suite này PASS. Fixture đọc RATE_LIMIT/Retry-After và chỉ retry sau thời gian server yêu cầu; không đổi limiter/timeout/production auth.

### Kết quả cuối sau sửa và chạy lại

| Nhóm | Kết quả thực tế | Log |
|---|---|---|
| JVM |35/35,0fail/error/skipped;2mới kiểm tra UTF-8 multipart/Unicode validation và chặn upload sau đổi phiên |unit-caption.txt SUCCESS17s và XML testDebugUnitTest |
| Build |Bản production cuối SUCCESS29s,debug/test/release/lint;0errors/3warnings cũ;test APK cuối13s |build-verified.txt,build-swipe-test.txt |
| Preview |11ca duy nhất đạt:9method đạt ở lượt10ca;private-confirm chạy lại đạt,cộng1offline mới |preview-verified.txt115.486s có1fail dialog;preview-dialog-offline.txt2/2PASS61.117s |
| Camera/auth/layout |12method đạt ở lượt13ca;navigation có1fail được chạy lại |regression-camera-auth.txt125.336s |
| Navigation/Friends |6method đạt gồm navigation+5Friends core;2method phụ thuộc bịAssume skipped |regression-navigation-friends.txt68.998s;runnerOK8 nhưng chỉ tính6PASS |
| CAMERA system dialog |Deny1/1PASS4.876s,Allow1/1PASS7.584s |camera-deny.txt,camera-allow-retry.txt |
| Ảnh thật offline |1/1PASS71.895s,chụp→Preview→POSTkhi offline→Retry→GETJWT/so bytes |media-preview-offline.txt |

Tổng **32 ca USB duy nhất đạt sau sửa/retest** =11Preview+18regression+2permission+1mediaoffline. Không cộng lượt lặp;2skipped không tínhPASS. Đối chiếu terminal status từng method sau các lượt cuối:32PASS/0failure còn tồn tại/2skipped. Không gọi suite cófailure làPASS hoặc nhận đã chạy toàn bộ suite Android.

Ca phân trang tạo21friendship thật qua register/login/send/accept;đọc20+1 và giữ lựa chọn ở hai trang. Mirror kiểm tra JPEG bốn góc màu với4rotation×2mirror. Camera trước/sau/flashON chụp thật và so bytes GETserver;ảnh synthetic dùng riêng cho form/layout/cache và được ghi rõ trong test.

Lỗi/retest thêm: dialogNoMatchingView khi friends fetch thay layout;fixture chờ friends idle rồi vẫn bấm button/cancel/confirm thật,2/2đạt. Shellswipe cólượt không đổi tab;đổi sang Espresso.swipeLeft đồng bộ UI,giữassertion nav/pager/nhảcamera và chạy lại đạt. Lượt CameraAllow đầu bị runner kế tiếp gián đoạn (chỉProcess crashed,chưaPASS);chạy riêng lại systemDeny→Allow/previewready/hint đạt7.584s. Không nới timeout/limiter/assertion hoặc sửa backend.

## Lệnh kiểm tra

PowerShell tại android-client, sau chọn JDK qua scripts/Use-TaktakJava.ps1:

```powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

USB dùng SDK platform-tools/adb.exe, cài app-debug.apk và app-debug-androidTest.apk, kiểm tra Success trước chạy runner `com.taktak.app.test/androidx.test.runner.AndroidJUnitRunner`. Nhóm Preview bình thường chọn từng method ngoại trừ sessionOffline; ca đó chạy riêng với `-e taktakPreviewOffline true -e taktakQaRun <unique-run>`. Host chỉ toggle connector của smoke qua stdin, không đổi mạng điện thoại. Nhóm media offline dùng `-e taktakAwaitMediaNetworkRestore true`.

## Giới hạn và điểm dừng

5B chuẩn bị ảnh/caption/người nhận trên client; chưa gửi ảnh bạn bè, idempotency5D,History5E,FCM/widget hoặc triển khai. Caption≤50 được hiểu là Unicode code point (emoji ghép nhiều code point có thể tính nhiều ký tự), không tự đổi sang50grapheme. Cache có thể bịOS thu hồi hoặc mất sau process death; thiếu ảnh dẫn về chụp mới, không nhận đã nghiệm thu full process-death suite. Chưa API26/API37 local-network dialog,TalkBack full,font2×,tablet thật/physical rotation,HTTPS runtime hoặc release signing. Các viewport/font1.5 trong test là đo layout theo context, không máy tablet thật. Backend87test không chạy lại nếu source không đổi.

## REVIEW → HANDOFF

Đã xem ảnh Preview chụp thật,serverdownload,sáng/tối,bàn phím,sessionoffline vàLogin;không lớp phủ. Ảnh đơn sắc ở testtheme/IME làJPEG synthetic để đo form,không giả là Camera. Ảnh thật tại `screens-final/preview-camera.png` và `camera-regression-screens/rear-photo.png`;ảnh camera trước giữ local ignored.

Cleanup: smoke riêng dừngexit0,**7media rows/7private files/0recipients**,port8080 khônglistener. Fixtures logout/revoke,cacheinternal camera-drafts **0file còn lại**;không xóa ảnhserver/gallery hoặcDBdev. Reverse8080gỡ,CAMERAgranted nhưbanđầu,coldstart1489ms;handoff.xml cóemailInput/khôngpreviewImage vàhandoff.pngđãxem. Không cần bước thủ công để nghiệm thu5B. Dùngappdevtiếp cần backend/devDB/kết nối vìsmoke đãdừng.

ReviewGit: backend/schema/SDK/dependency/đặc tả/README/FirebaseJSON giữ nguyên;indexrỗng,HEADed9fde6 khôngđổi. Quét file thay đổi cho credential-pattern/conflict/whitespace,XML/link vàdiffcheck;không phát hiện secret literal. Đây là kiểm tra chọn lọc,không audit toàn lịch sửGit. Artifacts/ảnh/FirebaseJSON vẫnignored. SourceMomentVM cũ được gỡ sau kiểmtra thamchiếu,khôi phục từGitđược.

File Android: thêm CaptureDraftRepository,CaptureViewModel,PreviewViewModel,PreviewActivity,CaptionFilter;activity_preview.xml,item_recipient.xml,preview_strings.xml;PreviewAcceptanceTest,PreviewTestSupport,MomentCaptionTest. Sửa AuthRepository/MomentRepository/MomentApi,CameraFragment/PhotoProcessor,Manifest,fragment_camera.xml/strings.xml vàbaAndroidTestCamera;gỡMomentViewModel cũ. Tài liệu tạoQA_PREVIEW.md,cập nhậtPROJECT_STATUS/TASKS/SESSION_HANDOFF/PROJECT_DECISIONS.

Tổng28file trong phạm vi:15file sửa,12file mới,1source cũ gỡ (23Android+5docs). Kiểm tra27file còn tồn tại:0credential/conflict/whitespace-pattern findings,6XML parse hợp lệ,90link local hợp lệ;git diff --check đạt. Backend thay đổi0file,index0,HEAD vẫned9fde6. Các cảnh báo LF→CRLF từGit là cấu hình line-ending có sẵn, không lỗi build/test.

**5B PASS; đề xuất duy nhất5C và chờ duyệt.** Không commit/push/deploy hoặc tự chuyển5C.
