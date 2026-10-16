# Taktak — 0C/1C: kế hoạch và bằng chứng kết nối

Ngày: 16/10/2026. Đã được duyệt trong AUTO CONTINUE 0–1. **Chưa DONE**: chưa có máy Android thật và đường trực tiếp `10.0.2.2` trên AVD hiện tại đang timeout.

| Kịch bản | Địa chỉ và điều kiện | Kết quả thực tế |
|---|---|---|
| Host local | Backend bind `127.0.0.1`, port test động | PASS: HTTP health trả UP |
| Emulator thông thường | `http://10.0.2.2:<port>/`, default debug base URL port 8080 | FAIL môi trường hiện tại: SocketTimeoutException trên AVD, cả route wlan0 và eth0; chưa xác định nguyên nhân gốc |
| Emulator qua USB transport/ADB reverse | Map đúng port, dùng `http://127.0.0.1:<port>/` trong emulator | PASS: 4 instrumentation test, gồm HTTP 200 health và 401 endpoint được bảo vệ |
| Điện thoại thật | Ưu tiên HTTPS tunnel tới host; URL cụ thể cần người dùng cung cấp/thiết lập. ADB reverse là lựa chọn USB bổ sung | CHƯA CHẠY: không có thiết bị thật kết nối; không giả định emulator thay cho máy thật |
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

Nếu kiểm tra lại đường mặc định emulator, thay argument health bằng `http://10.0.2.2:<port>/actuator/health`, không dùng reverse để tuyên bố đường alias đã hoạt động. Phiên này không thay firewall host hoặc mở backend ra LAN để chữa timeout.

## Giới hạn và việc còn lại

- Debug cleartext chỉ cho ba host local; HTTP LAN không nằm trong allowlist hiện tại. Máy thật dùng HTTPS tunnel hoặc reverse USB. HTTPS endpoint phải có chứng chỉ được Android tin cậy; không tắt xác minh TLS.
- Bản release không có network security config debug và bắt buộc HTTPS. URL `.invalid` trong build QA là giá trị kiểm thử không hoạt động, không được dùng như cấu hình phát hành thật.
- AVD đã chạy headless/read-only, kích thước 1080×2400. Đã xử lý popup ADB trên chính emulator test, không xóa key hoặc thay dữ liệu AVD gốc.
- Cần máy Android thật đã cấp quyền USB debugging để kiểm tra health và app. Nếu chọn tunnel, cần URL HTTPS cụ thể; không yêu cầu secret/credential gửi vào chat.
- 0B vẫn chờ đối chiếu Firebase Console; không coi network/backend PASS là bằng chứng Firebase đã đăng ký đúng hoặc FCM chạy.
