# Taktak — Đo tải có kiểm soát

`SystemLoadAcceptanceIT` là harness opt-in, không chạy trong suite mặc định.
Harness **không nhận URL hoặc database đích**: tự khởi động Spring Boot trên
127.0.0.1/port ngẫu nhiên, Embedded PostgreSQL và storage riêng trong target/.
Không gửi SMTP/FCM thật, không dùng tài khoản cá nhân hoặc reset database có sẵn.

## Tái lập

Từ repository root trên Windows, chọn JDK17 cho server. Lần đầu cần dependency
đã cache; bỏ `-o` nếu có quyền tải dependency. Maven portable có sẵn ở `.tools/`
là phương án local nếu wrapper không khởi động được.

```powershell
. .\scripts\Use-TaktakJava.ps1 -Target Server
Set-Location server
.\mvnw.cmd -o -Dtest=SystemLoadAcceptanceIT -Dtaktak.load.qa=true `
  -Dtaktak.load.report=baseline.json test
```

Không chạy load lên VPS, tunnel, tài khoản thật hoặc database production. Không
tăng concurrency/dataset mặc định trong một lượt QA laptop mà chưa kiểm tra
tài nguyên host. Harness giữ concurrency4, mỗi HTTP request timeout15s và mỗi
batch deadline90s. Lỗi hoặc deadline là FAIL, không bỏ mẫu chậm để làm đẹp số.

## Workload và cách đọc

- Dữ liệu synthetic:25user,24hội thoại,1440tin văn bản,24ảnh JPEG480×480.
- 5 warmup +40 request cho mỗi nhóm: hồ sơ, inbox20hội thoại, trang50tin,
  lịch sử nhận20ảnh và GET một ảnh private. JWT thật và authorization vẫn chạy.
- 20 lần ghi tin, mỗi lần retry cùng clientMessageId, tổng40POST; assert chỉ
  tạo20message. Không gọi đăng nhập BCrypt trong vòng tải.
- Đo p50/p95/p99 elapsed phía HTTP client, throughput trong từng batch và số
  prepareStatement/prepareCall qua DataSource proxy chỉ trong test.
- Số statement là chỉ báo khối lượng SQL, không phải execution time/query plan;
  không kết luận CPU/disk/thread pool là bottleneck nếu chưa có profile.
- Không có PII/token/response body hoặc ID tài nguyên trong JSON report. Request
  timeout, auth failure, SQL error đều không được coi là kết quả thành công.

Report JSON trong `server/target/system-load-qa/`, log local ignored. Giữ baseline
và after riêng, không ghi đè để mất bằng chứng so sánh. Thử nghiệm trên laptop,
dataset nhỏ, cache ấm và ảnh nhỏ **không** chứng minh capacity production, upload
500–800KB, hàng triệu user, tail latency dài hạn hoặc multi-node throughput.

## Quyết định tối ưu

Ưu tiên loại N+1 query trong inbox nếu số liệu baseline xác nhận; giữ nguyên
pagination/order, totalUnread, quyền friend/block và redaction của MOMENT_REPLY.
Không thêm Redis, tăng connection pool, thay thread pool hoặc index speculative.
Sau tối ưu, chạy lại cùng workload và toàn bộ ChatIntegrationTest.

Nếu profile production/staging cho thấy disk hoặc pool bão hòa: ghi dataset,
RPS/concurrency, heap/GC, Hikari active/pending/max, Tomcat busy/max và SQL plan
trước khi chọn thay đổi. Quan sát registry/runbook tại OBSERVABILITY.md. Không
log bind parameters, caption, text, email hoặc Authorization để phân tích tải.

## Kết quả QA local — 21/10/2026

Hai lượt cùng workload trên laptop, không chạy AVD trong lúc đo. Baseline
`hardening-load-baseline.log` BUILD SUCCESS26,167s; after
`hardening-load-after.log` BUILD SUCCESS15,781s. Mỗi lượt240request đo +25warmup,
0response lỗi;20POST đầu tiên tạo20tin,20retry không tạo bản sao.

| Inbox20 hội thoại,40request | Trước | Sau |
|---|---:|---:|
| Prepared statements |2560 (64/request)|160 (4/request)|
| p50 HTTP ms |34,7135|12,4161|
| p95 HTTP ms |44,0256|18,6497|
| p99 HTTP ms |48,1900|28,5037|
| Request/s trong batch |108,63|268,41|

Giảm93,75% số statement cho inbox văn bản. Đây là tối ưu N+1 ở
ChatRepository/ChatService: join peer+lastMessage, tính unread cùng truy vấn,
giữ `safe()` kiểm tra grant cho tin có ảnh. Inbox ảnh vẫn có truy vấn kiểm tra
quyền riêng; không vô hiệu authorization để đạt query budget. Không thêm index,
đổi Hikari/Tomcat pool hoặc tuyên bố disk bottleneck khi chưa đo được.

Số latency/throughput chỉ là một cặp lượt thử, không phải kết quả thống kê hoặc
capacity/SLO production. Các nhóm khác ở baseline/after.json được giữ đầy đủ;
không suy ra mọi route nhanh hơn. Budget regression inbox text ≤8statement/request
được thêm vào harness sau đo; pagination/peer/unread/block có integration test.
