# Taktak — 0C/1C: kế hoạch và bằng chứng kết nối

Ngày: 16/10/2026. **0C/1C PASS cho kịch bản local đã chọn**: emulator qua alias và điện thoại thật qua USB reverse. LAN/tunnel HTTPS chưa kiểm thử, không được ghi PASS cho các đường đó.

| Kịch bản | Địa chỉ và điều kiện | Kết quả thực tế |
|---|---|---|
| Host local | Backend bind `127.0.0.1`, port test động | PASS: HTTP health trả UP |
| Emulator thông thường | `http://10.0.2.2:<port>/`, default debug base URL port 8080 | PASS phiên mới: 4/4 instrumentation test trên Medium_Phone / Android 17, không reverse. Lần trước thiếu quyền local network nên TCP timeout |
| Emulator qua USB transport/ADB reverse | Map đúng port, dùng `http://127.0.0.1:<port>/` trong emulator | PASS: 4 instrumentation test, gồm HTTP 200 health và 401 endpoint được bảo vệ |
| Điện thoại thật | USB reverse `tcp:8080`, health `http://127.0.0.1:8080/actuator/health` | PASS: 4 test, 0 failures/errors/skipped trên 23129RAA4G / Android 15 (API 35); health 200/UP, protected API 401; app cài/mở thành công |
| Release | URL HTTPS riêng, cleartext=false | Build và lint PASS; chưa có tên miền backend thật, signing hoặc deploy |

## Chạy lại qua ADB reverse

Chuẩn bị `adb` trong PATH, emulator/thiết bị đã cấp quyền debugging, JDK Android; không dùng `localhost` trên Android để gọi máy tính nếu chưa có reverse. Tại Git root, terminal thứ nhất:

```powershell
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Start-LocalSmokeServer.ps1 -Port 8080
```

Terminal thứ hai (chọn đúng serial từ `adb devices -l`, không chép serial ví dụ sang thiết bị khác):

```powershell
adb devices -l
adb -s emulator-5556 reverse tcp:8080 tcp:8080
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
. .\scripts\Use-TaktakJava.ps1 -Target Android
Push-Location android-client
.\gradlew.bat connectedDebugAndroidTest '-Pandroid.testInstrumentationRunnerArguments.taktakHealthUrl=http://127.0.0.1:8080/actuator/health'
Pop-Location
adb -s emulator-5556 reverse --remove tcp:8080
```

Phiên QA thực tế dùng port 52312 được server chọn tự động; 8080 ở trên là cách chọn port cố định khi chạy lại. Trong app, có thể build debug với `-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/` khi sử dụng reverse. API chưa được gọi từ UI; request smoke hiện chỉ có trong instrumentation test theo phạm vi 1C.

## Chạy trực tiếp qua alias emulator

Khởi chạy smoke server port 8080 bằng lệnh ở trên, dùng emulator đã cấp quyền ADB, sau đó chạy tại `android-client/` với JDK Android:

```powershell
.\gradlew.bat --no-daemon --console=plain connectedDebugAndroidTest
```

Lệnh dùng `BuildConfig.BASE_URL=http://10.0.2.2:8080/` mặc định, không cần reverse hoặc argument ghi đè URL. Trên Android 17, test harness cấp quyền runtime `ACCESS_LOCAL_NETWORK` trước HTTP request; manifest chỉ khai báo quyền cho debug. UI bootstrap chưa có request mạng nên chưa hiển thị hộp thoại xin quyền. Nếu thêm request local vào UI ở bước sau, cần xin quyền và xử lý từ chối trước khi gọi API.

Đã tái hiện trước sửa: 4 test, 1 fail tại connect `10.0.2.2:8080` (10 giây timeout). Sau khai báo/cấp quyền: 4 test, 0 fail/error/skipped, health 200/UP và API được bảo vệ 401. `adb reverse --list` rỗng. Không thay firewall, bind address, target SDK hay policy cleartext.

Theo [tài liệu Android](https://developer.android.com/privacy-and-security/local-network-permission?hl=en), target SDK 37 chịu kiểm soát local network bắt buộc; thiếu quyền có thể gây TCP timeout. Kết quả trước/sau xác nhận đây là nguyên nhân của lỗi smoke hiện tại.

## Giới hạn và việc còn lại

- Debug cleartext chỉ cho ba host local; HTTP LAN không nằm trong allowlist hiện tại. Máy thật dùng HTTPS tunnel hoặc reverse USB. HTTPS endpoint phải có chứng chỉ được Android tin cậy; không tắt xác minh TLS.
- Bản release không có network security config debug và bắt buộc HTTPS. URL `.invalid` trong build QA là giá trị kiểm thử không hoạt động, không được dùng như cấu hình phát hành thật.
- AVD đã chạy headless/read-only, kích thước 1080×2400. Đã xử lý popup ADB trên chính emulator test, không xóa key hoặc thay dữ liệu AVD gốc.
- Đã kiểm tra máy Android thật qua USB sau khi người dùng kết nối và cấp quyền debugging. Kịch bản này giữ backend trên loopback và không cần mở firewall/LAN. Tunnel/LAN là các phương án chưa chạy; nếu sử dụng sau này cần kiểm tra riêng.
- 0B đã PASS đăng ký/cấu hình bằng ảnh Console đối chiếu JSON và xác nhận thủ công người dùng; xem FIREBASE_SETUP. Chưa kiểm thử gửi/nhận FCM.

## Kết quả máy thật — 16/10/2026

`connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.taktakHealthUrl=http://127.0.0.1:8080/actuator/health` đã BUILD SUCCESSFUL, 4/4 test PASS qua reverse USB tới app Spring Boot thật và cluster PostgreSQL test mới. Bằng chứng: `.local/artifacts/android-physical-usb-results.xml`.

Test ActivityScenario lần đầu mất khoảng 104 giây; chưa xác định nguyên nhân chậm, không timeout hoặc failure. Đã thu SIGQUIT cho tiến trình Taktak để chẩn đoán trong lúc chờ. Sau đó cài/mở APK bằng ADB: Success / Status ok / COLD khoảng 3 giây; screenshot và UI dump có chữ Taktak. Không thay cài đặt bảo mật hoặc animation của điện thoại. App debug được giữ trên máy để người dùng xem placeholder; backend test và mapping reverse được gỡ khi bàn giao.
