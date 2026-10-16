# Taktak — Nghiệm thu backend auth và hồ sơ (2D)

Ngày: 16/10/2026. Người dùng duyệt riêng 2D. Tiếp nối 36 test đã có ở 2C, bổ sung curl thật và chốt [hợp đồng lỗi](API_ERRORS.md); không triển khai Android auth, avatar, media hoặc deploy.

## Chạy lại trên Windows

Tại Git root, cần JDK 17/Maven portable đã chuẩn bị và `curl.exe` trong PATH:

```powershell
# Toàn bộ integration/regression + curl + đóng gói JAR.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Test-AuthApi.ps1

# Chỉ luồng curl; không thay thế nghiệm thu toàn bộ regression/JAR.
powershell.exe -NoProfile -ExecutionPolicy Bypass -File .\scripts\Test-AuthApi.ps1 -CurlOnly
```

Script chọn JDK bằng helper chỉ trong process, cache Maven `.tools/maven-repository`. Nếu thiếu JDK/cache cần chuẩn bị môi trường theo hồ sơ bootstrap; không dùng Java 8 trong PATH. Thiếu curl là lỗi kiểm thử, không tự skip. Test trực tiếp Spring Boot/JPA/Flyway/PostgreSQL tạm, port HTTP/DB ngẫu nhiên trên loopback, tự đóng sau test. Không cần server dev chạy sẵn, không dùng/reset database cá nhân.

Test `curlExercisesAuthLifecycleIncludingExpiryForgeryAndDuplicateUsername` gọi tiến trình `curl.exe` trên Windows hoặc `curl` trên hệ khác, không giả lập HTTP client. Invocation thực tế là `curl.exe --disable --config -`: URL/method/headers/JSON được truyền qua config UTF-8 trên stdin, stdout chứa response và status trailer chỉ giữ trong bộ nhớ. Không đặt credentials trong argv, file tạm, curlrc hoặc log; không bật verbose/trace, không follow redirect, bỏ proxy cho loopback. Timeout mỗi request 15 giây; lỗi transport làm test FAIL.

Để xem một request công khai bằng curl thủ công (không thay cho toàn bộ test):

```powershell
# Sau khi khởi động Start-LocalSmokeServer.ps1 ở terminal khác.
curl.exe --disable --silent --show-error --noproxy '*' --max-time 15 `
    --write-out '\nHTTP %{http_code}\n' http://127.0.0.1:8080/api/users/me
```

Kỳ vọng 401 JSON. Với request có credential, dùng test/hướng dẫn stdin, không dán token hoặc password vào terminal command, chat hoặc Git. HTTP chỉ dành cho loopback/debug tin cậy; HTTPS/public chưa được nghiệm thu.

## Ma trận đã kiểm tra

| ID | Kịch bản | Bằng chứng bắt buộc |
|---|---|---|
| AUTH-REGISTER | Register hợp lệ, chuẩn hóa, BCrypt/salt, DTO | 201, DB có đúng dữ liệu; không trả password/hash |
| AUTH-UNIQUE | Username/email trùng, cả race hai HTTP request | 201+409, DB chỉ một user; unique constraint thực tế |
| AUTH-LOGIN | Đúng/sai password và email không tồn tại | 200 hoặc cùng 401 an toàn; không tạo session sai |
| AUTH-JWT | Thiếu/giả chữ ký/malformed/claims sai/alg none | 401, không truy cập protected API |
| AUTH-EXPIRED | Access đúng chữ ký hết hạn ở đúng 15 phút | GET me 401; refresh với expired bearer 200; GET access mới 200 |
| AUTH-ROTATE | Refresh replay/race/rollback | Replay 401; chỉ một rotation thành công; failure rollback |
| AUTH-LOGOUT | Logout hiện tại/idempotent/nhiều phiên | 204, refresh tương ứng không dùng lại; không thu hồi nhầm session khác |
| AUTH-RATE | IP/account/quota đồng thời, X-Forwarded-For giả | 429/Retry-After, không bypass quota |
| PROFILE | GET/PUT, Unicode/độ dài, persistence, A/B, mass assignment | Chỉ sửa displayName của JWT subject, DTO an toàn |
| ERROR-CONTRACT | Validation/security/routing/media/quota/internal | JSON đúng 5 trường, status/code/path/time/headers; không query hoặc chi tiết nội bộ |
| CURL-LIFECYCLE | Register→login→me→PUT→expiry→refresh→me→logout | Thực thi curl thật, cả username trùng/JWT giả/replay/revoked refresh |

Expiry được tạo bằng Clock chỉ thuộc test: cấp access hợp lệ trước, tiến Clock 15 phút rồi gửi lại chính token đó bằng curl. Không sửa exp của JWT rồi gọi đó là “hết hạn”, không tắt kiểm tra chữ ký, không thay thời gian hệ thống hoặc thêm HTTP test endpoint. Production dùng Clock.systemUTC và access mặc định 15 phút giữ nguyên.

## Kết quả và giới hạn

Kết quả thực tế 16/10/2026:

| Lệnh/kiểm tra | Kết quả |
|---|---|
| Test routing đầu tiên trước sửa | FAIL: 1 test, expected 404 nhưng nhận 403; không tính là PASS |
| `Test-AuthApi.ps1` sau sửa | PASS: BUILD SUCCESS, 39 test, 0 failures/errors/skipped, JAR |
| `Test-AuthApi.ps1 -CurlOnly` | PASS: 1 testcase curl, 0 failures/errors/skipped |
| `Test-AuthApi.ps1` bản cuối sau bổ sung 406 | PASS: BUILD SUCCESS, 39 test, 0 failures/errors/skipped, JAR |
| Parse XML Surefire | PASS: 22 auth/profile/curl, 13 bootstrap/register, 3 token config, 1 quota concurrency; có testcase curl |
| Production JAR | PASS: có handler, không probe/Clock/encoder/harness/embedded PostgreSQL test |
| Review repo | PASS trong phạm vi quét: 47 file không mẫu key/conflict, 69 link local tồn tại, git diff --check và cú pháp PowerShell hợp lệ |

36 test trước được giữ lại, thêm 3 testcase (curl lifecycle, routing/error contract, unexpected failure); helper lỗi cũ được siết thêm headers/path/Clock. Không thay điều kiện kiểm tra để che failure. Handler 406 được kiểm tra bằng GET me với Accept text/html trong testcase routing; fallback 500 dùng exception fixture, không phải lỗi production bị bỏ qua.

Xem kết quả chạy mới nhất tại [SESSION_HANDOFF.md](SESSION_HANDOFF.md) và [PROJECT_STATUS.md](PROJECT_STATUS.md). Báo cáo máy local tại `server/target/surefire-reports/` (ignore), gồm tên testcase curl và từng suite. Output/build cũ không được tính là mới chạy. Lệnh `-CurlOnly` chỉ chạy một testcase, không chứng minh toàn bộ regression hoặc JAR.

Kiểm thử ban đầu 2D đã phát hiện route không tồn tại trả 403 thay vì 404 do error dispatch; phải sửa handler MVC và chạy lại, không nới kỳ vọng. Error handler hiện xử lý 404/405/406 và lỗi nội bộ an toàn; filter chain giữ nguyên. Test failure endpoint/Clock/encoder chỉ ở test source/profile, không nằm trong production JAR.

Lỗi duplicate DB, fault-injection encoder hoặc generic failure cố ý trong log test là kết quả mong đợi; test vẫn phải xác nhận status/body/rollback. Không có test bị bỏ qua để đạt nghiệm thu. Chưa chạy Android/thiết bị/VPS/TLS trong 2D vì không thuộc thay đổi; chưa có login UI hoặc lưu phiên Android. Giai đoạn 3 chỉ bắt đầu khi được duyệt riêng.
