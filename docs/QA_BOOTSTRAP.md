# Taktak — QA bootstrap Bước 0 và giai đoạn 1

Ngày chạy: 16/10/2026 (Asia/Saigon). Chỉ ghi kết quả đã thực hiện; không có auth/camera/widget trong phạm vi này.

| ID | Kiểm tra thực tế | Kết quả |
|---|---|---|
| SERVER-BUILD | JDK 17; `server/mvnw.cmd -B -ntp clean verify` với Maven repository local | PASS: compile release 17, 6 test, 0 lỗi, 0 skipped, đóng gói executable JAR |
| SERVER-SCHEMA | PostgreSQL 14.22 thật trong cluster test mới; Flyway V1, 8 bảng/index, migrate lần hai không áp dụng lại | PASS |
| SERVER-CONSTRAINTS | Email không trùng bất kể hoa/thường, username unique, cặp bạn/lời mời, không tự chặn, FK, byte size >0, caption 50 Unicode code point | PASS |
| SERVER-SECURITY | GET health 200 chỉ có status UP; `/api/users/me`, actuator env/health detail và private path trả 401, không redirect/login cookie/default Basic | PASS |
| SERVER-SMOKE | `Start-LocalSmokeServer.ps1`, app thật + PostgreSQL test riêng; HTTP từ host | PASS: UP trên port 52312 trong phiên test |
| ANDROID-DEBUG | `assembleDebug testDebugUnitTest lintDebug` | PASS: APK debug; unit test mẫu 1/1 (chỉ arithmetic, không phải test nghiệp vụ); lint không error |
| ANDROID-RELEASE | `assembleRelease lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/` | PASS build/lint; APK unsigned, URL QA không phải API thật |
| CONFIG-NEGATIVE | Release thiếu URL, release HTTP, debug HTTP public host | PASS: từng lệnh thất bại đúng thông báo yêu cầu; không phải lỗi bị bỏ qua |
| DEVICE-REVERSE | `connectedDebugAndroidTest` với health URL `http://127.0.0.1:52312/actuator/health` và ADB reverse | PASS: 4 test, 0 fail/error/skipped trên Medium_Phone / Android 17 emulator |
| DEVICE-UI | Cài APK, `am start -W`, UI dump và screenshot theo android_ui_verification | PASS: cold launch Status ok, UI có chữ Taktak, Activity không crash trong phép thử |
| DEVICE-ALIAS | Cùng health request qua `10.0.2.2:52312`, thử lại trên route eth0 sau wlan0 | FAIL: SocketTimeoutException; chưa xác định root cause môi trường, không đổi kỳ vọng hoặc xóa bằng chứng |
| DEVICE-PHYSICAL | Điện thoại Android thật | CHƯA CHẠY: chưa có thiết bị thật |
| FIREBASE-CONSOLE | Đăng ký project/app, nguồn JSON và phạm vi dịch vụ | BLOCKED: chưa có kết nối/xác nhận Console; không gửi push hoặc tạo Admin key |
| STATIC-REVIEW | Quét 48 source/tài liệu theo mẫu credentials/conflict; 35 link local; 10 trường hợp ignore; không có Kotlin source | PASS trong phạm vi kiểm tra tĩnh, không phải chứng minh tuyệt đối không có secret |
| ARTIFACT-POLICY | Manifest merged debug/release, FCM auto-init false; release APK không có debug network XML; production JAR không chứa test harness/PostgreSQL nhúng | PASS |
| WORKSPACE | Đối chiếu SHA-256 66 file có sẵn; Git status/index/HEAD | 15 file sửa trong phạm vi, 17 source/tài liệu mới, không xóa file có sẵn; JSON Firebase giữ nguyên; HEAD 99b6db4, không staged/commit |

## Báo cáo và artifact local

- Backend: `server/target/surefire-reports/com.taktak.server.ServerBootstrapTest.txt` và XML tương ứng.
- Debug/release lint: `android-client/app/build/reports/lint-results-debug.html`, `lint-results-release.html`. Có hai warning màu black/white không dùng từ skeleton; không suppress hoặc xóa resource của người dùng. Công cụ còn cảnh báo deprecation Gradle 10, protobuf Unsafe và native debug symbols; build vẫn thành công, chưa nâng Gradle 10.
- Android runtime PASS qua reverse: `.local/artifacts/android-adb-reverse-results.xml` (4/4). Kết quả FAIL alias riêng: `.local/artifacts/android-direct-alias-results.xml` (1/1 fail); báo cáo mặc định Gradle instrumentation hiện phản ánh lần thử alias cuối, không phải lần reverse.
- UI: `.local/artifacts/taktak-bootstrap-view.xml`, `.local/artifacts/taktak-bootstrap.png`; screenshot đã xem, không chỉ dựa vào log build.
- Các artifact, cache và tool local đều bị ignore; số liệu chính được lưu trong tài liệu này để không phụ thuộc file generated khi clone.

## Lỗi môi trường đã xử lý và giới hạn

Java trong PATH ban đầu trỏ Java 8, javac trỏ JDK 25. Dùng Temurin 17 portable đã xác minh SHA256 cho server, JBR 25.0.3 của Android Studio cho Android; không đổi cài đặt Java toàn máy. Maven 3.9.16 đã kiểm tra SHA512 và Wrapper có SHA256 pin. Lần compile trong sandbox lỗi truy cập JAR; build ngoài sandbox sau phê duyệt đã clean verify thành công, không bỏ test.

PowerShell máy chặn file script; helper chạy được với ExecutionPolicy Bypass chỉ cho process. Harness kiểm tra âm ban đầu bị PowerShell coi stderr native là exception; đã sửa cách thu exit code và chạy lại cả ba case, không sửa app để bỏ kiểm tra.

ADB unauthorized đã được xử lý trên emulator test bằng console screenshot và xác nhận đúng popup, không lưu Always allow, không xóa key. Mở app và runtime network policy đã được xác minh. Lỗi đường 10.0.2.2 vẫn được giữ như vấn đề chưa hoàn tất 0C/1C; reverse là phương án độc lập hoạt động, không được gọi là đã sửa xong alias.

## Đánh giá nghiệm thu

1A **PASS local**. 1B đã triển khai, build/lint và UI emulator **PASS local**, nhưng nghiệm thu liên kết Firebase còn phụ thuộc 0B. 0C/1C **IN PROGRESS / chờ môi trường**: có đường reverse chạy được nhưng chưa máy thật và alias còn lỗi. Bước 0 và giai đoạn 1 **chưa DONE**. Không có căn cứ chuyển sang giai đoạn 2.

Cleanup cuối phiên: smoke server kết thúc bình thường (exit 0), cluster PostgreSQL test riêng được thu hồi; không còn listener test port 52312. Đã gỡ đúng ADB reverse và dừng emulator do phiên này tạo. Giữ tools/cache/artifact để tái sử dụng và đối chiếu, không xóa dữ liệu người dùng.

## Lần AUTO CONTINUE tiếp theo — sửa alias Android 17

Các bảng ở trên là bằng chứng lịch sử. Kết quả mới dưới đây thay kết luận alias còn lỗi, không thay các điều kiện nghiệm thu Firebase/máy thật.

| Kiểm tra vừa chạy | Kết quả thực tế |
|---|---|
| Git trước sửa | `main` / `96d2311`, working tree sạch; bootstrap và README đã commit theo yêu cầu riêng trước đó |
| Backend smoke | App thật với PostgreSQL test riêng, bind `127.0.0.1:8080`; host health UP |
| Alias trước sửa | 4 test, 1 failure `SocketTimeoutException` qua `10.0.2.2:8080`, 0 skipped; ba test khác PASS |
| Alias sau sửa | **PASS 4/4**, 0 failures/errors/skipped; health 200/UP và protected API 401, `adb reverse --list` rỗng |
| Build/lint | `connectedDebugAndroidTest assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/`: BUILD SUCCESSFUL; lint debug/release 0 errors; release unsigned với URL QA |
| Manifest isolation | PASS: debug có `ACCESS_LOCAL_NETWORK`; release không có quyền local network/debug network XML reference và cleartext=false |
| Launch | Cài APK Success, cold launch `MainActivity` Status ok |
| Firebase local | PASS kiểm tra JSON/package, trường private credentials, ignore và tên file credential không tracked; không chứng minh Console |

Nguyên nhân: target SDK 37 trên Android 17 cần quyền runtime `ACCESS_LOCAL_NETWORK`. Thêm khai báo chỉ trong debug manifest; test harness cấp quyền bằng UiAutomation và kiểm tra trạng thái trước request. Không đổi firewall, target SDK, TLS hoặc địa chỉ backend. UI bootstrap chưa gọi API; flow xin quyền khi UI truy cập local phải được xử lý cùng tính năng mạng ở bước sau. Xem [Android local network permission](https://developer.android.com/privacy-and-security/local-network-permission?hl=en).

Artifact mới được ignore: `.local/artifacts/android-alias-before-local-permission.xml` (4 test / 1 fail) và `.local/artifacts/android-alias-local-permission-results.xml` (4/4 PASS). Bằng chứng alias thất bại cũ được giữ nguyên. Không chạy lại server integration/unit test vì source server không đổi; sáu server test và unit arithmetic phía trên là kết quả phiên trước.

UI dump mới `.local/artifacts/taktak-resume-view.xml` có text Taktak; screenshot `.local/artifacts/taktak-resume.png` đã được xem theo skill android_ui_verification. Kiểm tra cuối: `git diff --check` PASS; quét 8 file thay đổi không phát hiện mẫu private key/API key hoặc conflict, 25 link local hợp lệ; hash JSON Firebase giữ nguyên. HEAD vẫn `96d2311`, index rỗng. Smoke server kết thúc exit 0, không còn listener HTTP 8080/PostgreSQL test 51141/AVD 5554–5555; không có reverse được tạo trong phiên. Không commit/push hoặc sửa migration.

Cập nhật nghiệm thu 0B cùng ngày: ảnh General trên Console đã được đối chiếu JSON local bằng PowerShell, cả Project ID/project number/App ID/package đều khớp; ảnh Cloud Messaging cho thấy API V1 Enabled. Người dùng xác nhận chưa thiết lập Auth/Firestore/Storage/Functions. **0B PASS đăng ký/cấu hình**, 1B PASS bootstrap; bằng chứng dịch vụ là ảnh/xác nhận thủ công, không phải API quản trị hoặc FCM runtime. Lệnh `adb devices -l` khi cập nhật không có thiết bị; 0C/1C vẫn chờ điện thoại thật. Không chạy lại build/test trong lần cập nhật hồ sơ này.

## Máy thật 0C/1C — 16/10/2026

| Kiểm tra vừa chạy | Kết quả |
|---|---|
| Thiết bị | ADB device đã cấp quyền; 23129RAA4G / Android 15 API 35, màn hình 1080×2400; ban đầu không có package Taktak/reverse mapping |
| Backend smoke | App Spring Boot thật, PostgreSQL test cluster riêng, bind 127.0.0.1:8080; host health 200 |
| Instrumentation | `connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.taktakHealthUrl=http://127.0.0.1:8080/actuator/health` với reverse USB: **BUILD SUCCESSFUL**, 4 test, 0 failures/errors/skipped |
| HTTP từ máy thật | PASS trong instrumentation: health 200/UP; protected /api/users/me 401 |
| UI/package/policy | Ba test còn lại PASS: package com.taktak.app, MainActivity có text Taktak, allowlist local/chặn cleartext example.com |
| Cài/mở riêng | Install Success, cold launch Status ok, TotalTime 3041 ms; foreground đúng MainActivity |
| Kiểm tra trực quan | UI dump có Taktak; đã xem screenshot theo skill android_ui_verification, placeholder sáng trên emulator và tối trên máy thật đều hiển thị |

UI test lần đầu chậm 103,718 giây nhưng không fail; đã thu SIGQUIT riêng tiến trình Taktak trong lúc chờ, không thay animation/security trên điện thoại. Chưa chứng minh nguyên nhân chậm; lần mở app riêng khoảng 3 giây đã được ghi nhận độc lập. Không bỏ test, không thay kỳ vọng.

Artifact local bị ignore: `.local/artifacts/android-physical-usb-results.xml`, `taktak-physical-view.xml`, `taktak-physical.png`. Báo cáo Gradle instrumentation mặc định hiện là lần máy thật; bằng chứng emulator alias/reverse vẫn được giữ ở artifact riêng.

Kết luận mới nhất: **Bước 0/giai đoạn 1 PASS bootstrap local**, bao gồm 0C/1C qua kịch bản emulator alias và máy thật USB reverse. LAN/tunnel, FCM runtime, auth, signing/deploy chưa chạy và không nằm trong nghiệm thu này. Server 6 integration test, unit mẫu và lint/release là kết quả các phiên trước, không tính là vừa chạy lại. Chưa chuyển giai đoạn 2.

Cleanup/review máy thật: reverse 8080 đã gỡ; smoke server exit 0, không còn listener 8080/49887. App debug được giữ trên điện thoại; dữ liệu/cài đặt cá nhân không bị xóa/thay đổi. XML 4/4 đã parse, 5 hồ sơ và 24 link local được kiểm tra, git diff --check PASS; artifact được ignore, hash JSON Firebase giữ nguyên, HEAD 96d2311/index rỗng.
