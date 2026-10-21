# Vận hành và quan sát Taktak

## Phạm vi triển khai

Observability dùng Micrometer/Actuator đã có trong Spring Boot, không thêm SaaS,
exporter, tracing backend hoặc dịch vụ phân tích người dùng. Chưa triển khai
monitoring/alert delivery trên VPS. Các mục dưới đây là hành vi code và runbook,
không phải cam kết SLO hoặc thông báo đã tới người trực.

## Correlation ID và log an toàn

Mỗi HTTP request đồng bộ được server tạo UUID ngẫu nhiên, trả trong header
`X-Request-ID`; mọi giá trị header cùng tên do client gửi đều bị bỏ qua, kể cả
UUID hợp lệ. ID nằm trong MDC `requestId` trong thời gian xử lý và được xóa
trong `finally`, kể cả khi Security trả 401/403 hoặc chain ném exception.
Log pattern mặc định thêm `[requestId=...]`; tác vụ ngoài request có `none`.

Default web/framework logging được giới hạn WARN, Security INFO và logger
Hibernate SqlExceptionHelper tắt riêng: lỗi constraint PostgreSQL có thể chứa
email/giá trị bị từ chối. API handler hiện hữu chỉ log thông báo cố định an toàn.
Đánh đổi: không có SQL exception dump chi tiết trong journal; điều tra theo mã
lỗi an toàn/correlation và tái hiện bằng dữ liệu QA thay vì bật log private.
Không bật HTTP/JDBC body/SQL-parameter TRACE khi điều tra production. Cấu hình
Nginx/collector là lớp riêng cần audit; correlation không tự làm sạch log proxy.

Không dùng ID làm metric label. Filter không đọc/log body, query, Authorization,
cookie, email, username, ảnh, caption hoặc nội dung chat; không log raw URI,
method lạ, header client hay exception message. ID chỉ là dấu vết xử lý, không
phải credential hay cách vượt qua phân quyền. Đây không phải distributed tracing;
không tự truyền MDC sang thread/scheduled/async hoặc gán ID request cho outbox.
Nếu thêm async HTTP sau này phải có kiểm thử context propagation riêng.

Khi người dùng báo lỗi, chỉ yêu cầu thời gian UTC, thao tác và X-Request-ID.
Không yêu cầu JWT, password, mã email, ảnh private hoặc dump request.

## Metrics hữu hạn

| Meter | Nội dung |
|---|---|
| `taktak.requests` | Counter số request hoàn tất, cả 401/403 và exception |
| `taktak.request.duration` | Timer thời gian request đồng bộ (count, total, max) |
| `hikaricp.connections.active/max` | Gauge pool do Actuator hiện hữu bind |

Request tags chỉ có `method` (7 phương thức chuẩn hoặc OTHER), `category`
(health/auth/moments/chat/api/other), `status_class` (1xx..5xx/other): tối đa
288 tổ hợp cho mỗi meter, không tăng theo số tài khoản, UUID hoặc request.
Timer không bật histogram/percentile; không được suy luận p95/p99 từ average.
`http.server.requests` built-in bị tắt để tránh ghi song song tag do framework
hoặc handler tương lai quyết định ngoài allowlist này.

Registry nằm trong process. `/actuator/health` vẫn công khai, chỉ `{status}`;
metrics/env/configprops/prometheus không được web-expose. Security từ chối
các endpoint quản trị khác kể cả JWT người dùng bình thường. Không sửa rule
thành `/actuator/** permitAll`, không dùng tài khoản app để scrape. Chưa có
Prometheus registry nên không có endpoint Prometheus hoạt động.

Muốn exporter sau này: duyệt riêng, chạy management listener loopback và một
security chain vận hành tách biệt, scrape qua mạng private/SSH tunnel, không
proxy management ra Nginx public. Rà soát labels trước khi bật exporter.

## Cảnh báo nội bộ có giới hạn

`OperationalSummary` chạy fixed-delay 60 giây mặc định; có thể override
`taktak.observability.summary-interval-ms` bằng cấu hình vận hành tin cậy.
Chỉ giữ hai số đếm tổng hợp của cửa sổ hiện tại, không giữ lịch sử request:

- `OPS_HTTP_SUMMARY`: INFO số request và số 5xx/exception trong cửa sổ.
- `OPS_HTTP_ERROR_RATE`: WARN khi cửa sổ có ít nhất 20 request và >=5% 5xx.
- `OPS_DB_POOL_SATURATED`: WARN khi tổng active >= tổng maximum Hikari qua
  hai lần lấy mẫu liên tiếp; trở lại dưới ngưỡng thì reset trạng thái.

Khoảng cửa sổ là fixed-delay thực tế, không phải SLA đúng 60 giây. Alert HTTP
là một cửa sổ, pool là hai mẫu; đây không phải đo liên tục mọi thời điểm.
Restart làm mất bộ đếm trong RAM; khi không có request không log summary.
Không tự restart service, xóa dữ liệu, revoke phiên hoặc thay đổi pool size.
Log mức WARN không đồng nghĩa email/on-call đã nhận cảnh báo. Cần người vận hành
kết nối collector/journald alert được duyệt và kiểm thử delivery riêng.

## Runbook xử lý sự cố

1. Ghi UTC, mã sự cố và X-Request-ID nếu có; giới hạn truy cập log.
2. Xác định blast radius: health, request errors, pool, disk và DB connectivity;
   không thử sửa bằng cách mở public media hoặc bỏ xác thực.
3. Với HTTP 5xx: tìm ID trong journal và so sánh thời điểm phát hành/migration;
   kiểm tra dependency/storage/mail/FCM bằng tín hiệu an toàn, không dump payload.
4. Với pool bão hòa: kiểm tra thời gian query/lock/transaction và mức đồng thời;
   tải thử chỉ trên DB QA cô lập; không tăng pool mù hoặc kill session production.
5. Với push trễ: kiểm tra trạng thái/tuổi outbox theo runbook reliability;
   ảnh đã commit không rollback vì FCM lỗi, không replay thủ công hàng loạt.
6. Với disk thấp: xác minh vùng private-images, backup/retention và file đang được
   tham chiếu. Không xóa thư mục ảnh/DB để giải phóng dung lượng tự động.
7. Khôi phục theo kế hoạch tương thích/rollback, giữ state thu hồi JWT; ghi lại
   nguyên nhân, bằng chứng, hành động và kiểm thử ngăn tái diễn.

Log cần rotation/retention và quyền tối thiểu theo cấu hình triển khai. Tài liệu
này không tự áp retention hoặc cấu hình collector trên máy chủ.

## Xác minh

Template Nginx trong `deploy/nginx/taktak.conf` đổi access log sang JSON chỉ
time/method/status/duration/upstream status/request ID từ backend; không IP,
URI/query, Referer, User-Agent hoặc Authorization. Cả HTTP redirect và HTTPS
dùng format này. Bootstrap HTTP tắt access log. Vì Nginx error log có thể tự
đính raw request URI/query ngay ở lỗi proxy/rate-limit, template ghi error log
vào `/dev/null crit`: đánh đổi mất chi tiết chẩn đoán TLS/proxy; dùng status,
latency, health và metric hệ điều hành để cảnh báo. Chỉ bật diagnostic logging
trong môi trường không có PII và có phê duyệt; không bật trên production tùy tiện.
Chưa chạy `nginx -t` với chứng chỉ/server thật trong lượt local này; kiểm tra
tĩnh không thay cho staging. Không chỉnh access log của site khác trên VPS.

`RequestTelemetryTest`: UUID server ngay cả input sai/dài/CRLF; cleanup MDC khi
exception và 401/403; 200 path/method/body khác nhau chỉ tạo 2 meter series;
summary/reset không có dữ liệu private; pool cần hai mẫu và phục hồi.
`ObservabilitySecurityTest`: HTTP server thật + PostgreSQL test riêng, xác minh
health/auth failure có ID, ordinary JWT không mở được endpoint quản trị.
Kết quả thực thi cụ thể ghi trong báo cáo bàn giao, không suy từ checklist này.

Ngày 21/10/2026: targeted `RequestTelemetryTest,ObservabilitySecurityTest`
đã chạy 7/7 PASS, BUILD SUCCESS 14,831 giây; log local được Git ignore:
`.local/artifacts/observability-tests.log`. Assertion logging OFF được bổ sung
sau lượt targeted này và cần full regression của đợt bàn giao để xác nhận.
