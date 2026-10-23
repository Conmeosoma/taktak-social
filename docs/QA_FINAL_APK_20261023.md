# APK bàn giao và commit main — 23/10/2026

Người dùng yêu cầu build APK mới nhất và commit main. Không có quyền push,
deploy hoặc tự đánh dấu mọi gate QA hoàn thành.

## Artifact

- Build type: debug, applicationId com.taktak.app, versionName1.0/versionCode1.
- Nguồn: android-client/app/build/outputs/apk/debug/app-debug.apk.
- Bản giao local: .local/artifacts/apk/Taktak-1.0-debug-B4067B00.apk.
- SHA256: B4067B00BE46D382F15051B478AAA8124E0319941F086926023DA081E401E1D9.
- APK không đưa vào Git (theo ignore *.apk); credentials Firebase/signing cũng không.
- Không đổi versionCode, không tạo release signing hoặc claim Play Store-ready.
- apksigner verify --verbose: **Verifies**, APK Signature Scheme v2=true,
 1 signer. APK20.068.298byte; đây là debug certificate, không release signing.

## Xác minh tại lượt đóng gói

assembleDebug/testDebugUnitTest/lintDebug --offline: **BUILD SUCCESSFUL20s**,
53 task (3 executed/50 up-to-date). JVM task up-to-date:135 kết quả0failure/error
từ lượt triển khai; không viết thành135 ca vừa chạy lại. Lint0error/85warning.
Guard resources2.304 localized strings và git diff --check chạy lại đạt.
API debug hiện cấu hình health HTTP200 khi kiểm tra; tunnel có thể thay đổi/hết
hạn, không phải URL production ổn định được nghiệm thu vĩnh viễn.

USB22/22 và launcher1/1 là kết quả lượt trước, không chạy lại trong lượt build/
commit này. [Hồ sơ chi tiết](QA_BRANDED_INTRO_20261023.md).

## Phạm vi commit

Bao gồm source/test Android, dependency SplashScreen, video intro gốc253KB,
bản dịch/resources, README, hồ sơ QA/trạng thái và Test-AndroidStringResources.ps1.
Giữ ngoài commit: báo cáo Word/BTL, diagrams và scripts tạo báo cáo/cache Python
không liên quan; không xóa hoặc di chuyển chúng. Không dùng git add . hoặc add -A
toàn repository. Không đưa .local/raw video QA/screenshot cá nhân/credential vào Git.

Commit chốt snapshot hiện tại, không đặt tên hoặc ghi trạng thái là mọi chức năng
đã hoàn tất. Còn Friends/History light contrast, Recap mẫu, các warning và các
gate đa thiết bị/accessibility/audio interruption/production trong hồ sơ trạng thái.
Không sửa backend, migration, dữ liệu hoặc deploy trong lượt này.
