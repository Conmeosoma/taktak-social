# Taktak — Search và lời mời kết bạn (4A)

Cập nhật sau4B: chấp nhận/từ chối, friendship và block đã triển khai/nghiệm thu tại [FRIEND_RELATIONS_API.md](FRIEND_RELATIONS_API.md). Các câu “chưa có4B” bên dưới mô tả phạm vi/lịch sử4A, không phải trạng thái hiện tại. Hợp đồng search/gửi/list pending4A giữ nguyên; mọi mutation quan hệ cùng dùng khóa cặp PostgreSQL.

Ngày: 17/10/2026. Phạm vi: backend search username, gửi và liệt kê lời mời đến đang chờ. Chấp nhận/từ chối, tạo/hủy friendship, API block/unblock thuộc **4B**, UI Friends thuộc **4C**; chưa triển khai trong 4A. Android, JWT/refresh policy, schema V1 và dependency giữ nguyên.

## Xác thực và dữ liệu công khai

Cả ba endpoint yêu cầu access JWT hợp lệ qua filter chain hiện có; subject phải là user còn tồn tại. Thiếu/sai/hết hạn JWT hoặc subject không tồn tại trả401. Không nhận senderId/currentUserId từ client. Response thành công có `Cache-Control: no-store`; lỗi dùng JSON chung trong [API_ERRORS.md](API_ERRORS.md).

PublicUser chỉ có `id`, `username`, `displayName`. Không email, avatar_path, password/hash, token hoặc thông tin phiên. Avatar private chưa có endpoint nên không tạo URL giả hoặc trả đường dẫn nội bộ.

## GET /api/users/search?q=

- q bắt buộc; raw input tối đa255 ký tự; strip/lowercase Locale.ROOT, sau chuẩn hóa phải khớp `[a-z0-9_]{1,255}`. Cho phép username hợp lệ ngắn một ký tự, không tự áp minimum3 khác đăng ký.
- **Prefix search**, không contains/fuzzy; underscore được escape thành ký tự literal, không wildcard SQL. Query có `%`, dấu nháy hoặc ký tự khác trả400, không phản chiếu query vào lỗi.
- Loại chính người gọi và mọi user có block hai chiều. Không tiết lộ user bị block bằng tổng số kết quả.
- Trả JSON array, tối đa20, sort username tăng dần; không total count hoặc phân trang search. Không có kết quả trả200 `[]`.
- Ví dụ dữ liệu dưới đây là placeholder, không phải tài khoản có sẵn:

```json
[{"id":"00000000-0000-0000-0000-000000000002","username":"friend_name","displayName":"Bạn của tôi"}]
```

Hạn mức **30 request/user/60giây** và **60 request/IP/60giây**, đồng bộ trong một JVM, tối đa10.000 khóa đang hoạt động, hết hạn được dọn. Đổi q, token hoặc X-Forwarded-For không tạo hạn mức user mới; IP lấy từ remoteAddr, không tin header client. Cạn quota/capacity trả429 `SEARCH_RATE_LIMITED`, `Retry-After: 60` (giá trị chờ bảo thủ). Input không hợp lệ vẫn tính quota khi đã xác minh user; thiếu q bị MVC từ chối trước service.

Đây là lớp giảm dò tài khoản cho MVP, không chống tuyệt đối bot nhiều IP/tài khoản; chưa distributed limiter/trusted proxy. Khi deploy cần chốt cấu hình proxy/rate limit ở bước triển khai. Query prefix đã bound và giới hạn kết quả; chưa benchmark/EXPLAIN trên dữ liệu production, không nhận đã tối ưu index cho mọi collation.

## POST /api/friends/requests

Content-Type: application/json. Body duy nhất được bind:

```json
{"receiverId":"00000000-0000-0000-0000-000000000002"}
```

Trả201:

```json
{
  "id":"00000000-0000-0000-0000-000000000003",
  "sender":{"id":"00000000-0000-0000-0000-000000000001","username":"sender_name","displayName":"Người gửi"},
  "receiverId":"00000000-0000-0000-0000-000000000002",
  "status":"pending",
  "createdAt":"2026-10-17T00:00:00Z"
}
```

- sender từ JWT; id/status/createdAt do server sinh; responded_at null. Field ngoài DTO bị bỏ qua như quy ước profile, không có quyền giả sender/status.
- Self400 `FRIEND_REQUEST_SELF`; receiver null/UUID sai/JSON sai400 `VALIDATION_ERROR`; sai Content-Type415.
- Receiver không tồn tại hoặc block ở **bất kỳ chiều nào** cùng trả404 `FRIEND_TARGET_UNAVAILABLE`, không tiết lộ lý do block.
- Đã có friendship hoặc pending giữa cùng cặp ở bất kỳ chiều nào:409 `FRIEND_REQUEST_CONFLICT`. Không tự accept khi hai người mời chéo; không tạo friendship trong4A.
- Lịch sử rejected không cản một pending mới. Chưa thêm thời gian chờ gửi lại hoặc expiry/cancel ngoài đặc tả.
- Transaction khóa hai hàng users theo `ORDER BY id FOR UPDATE` của PostgreSQL (không dùng UUID.compareTo Java để quyết định thứ tự), sau đó kiểm tra block/friendship/pending và saveAndFlush. Unique pending-pair V1 vẫn là lớp bảo vệ DB cho thao tác bỏ qua service.
- Lỗi DB rollback toàn bộ; SQL unique pending được đổi409 có code của friends, không nhầm ACCOUNT_CONFLICT. Không gửi FCM.
- 4B phải tái sử dụng quy ước khóa cặp này khi accept/block/unfriend để phối hợp kiểm tra quan hệ. Chưa nhận đã giải quyết race với API4B chưa tồn tại.

## GET /api/friends/requests?page=0

Chỉ lời mời **đến người gọi, status pending**, ẩn sender có block hai chiều. Không cho chọn receiverId khác hoặc xem outgoing của người khác. Query page là integer0..10000; page size cố định20, sort createdAt DESC rồi id DESC. Fetch21 để xác định hasNext, không total count; hết dữ liệu trả200 items rỗng. Batch-load sender bằng một query riêng, không serialize entity.

```json
{"items":[],"page":0,"hasNext":false}
```

Mỗi item có cùng RequestResponse như POST. Phân trang offset ổn định khi dữ liệu không đổi; dữ liệu mới/chuyển trạng thái giữa hai lần tải có thể làm dịch trang, chưa snapshot/cursor pagination. PUT accept/reject, GET friends, DELETE friend và block/unblock chưa có ở4A.

## Kiểm thử và cách chạy Windows

Không cần điện thoại cho 4A. Dùng JDK17/Maven Wrapper đã có; test tạo PostgreSQL cluster/storage tạm dưới server/target, port loopback ngẫu nhiên, JWT key ngẫu nhiên trong bộ nhớ. Không dùng DB_URL hoặc reset database cá nhân.

```powershell
. ./scripts/Use-TaktakJava.ps1 -Target Server
cd server
./mvnw.cmd "-Dmaven.repo.local=$((Get-Location).Path)/../.tools/maven-repository" -B -ntp verify
```

Chỉ chạy test4A khi phát triển:

```powershell
./mvnw.cmd "-Dmaven.repo.local=$((Get-Location).Path)/../.tools/maven-repository" -B -ntp '-Dtest=FriendIntegrationTest,SearchRateLimiterTest' test
```

Test HTTP đi qua Spring Security/MVC/service/JPA và PostgreSQL thật, không mock repository. Fixture user/JWT tạo ở test scope; không tắt JWT hoặc dùng auth endpoint giả trong production.

- FriendIntegrationTest14 ca: normalize/public DTO, literal underscore/20-result order, invalid query, block hai chiều và unknown target, sender mass assignment/list isolation, malformed/self, duplicate/reverse +DB unique, already-friends,12 request mời chéo đồng thời chỉ1 thành công, incoming pagination/status/block hai chiều, JWT thiếu/giả/hết hạn/orphan, quota/XFF, rollback/retry, rejected history.
- SearchRateLimiterTest5 ca: user30 qua nhiều IP, IP60 qua nhiều user, expiry60giây bằng Clock,64 call đồng thời chỉ30 được phép, capacity10.000 fail-closed/dọn khi hết hạn.
- 46 test auth/profile/media cũ được chạy lại cùng suite; không nhận Android UI hoặc toàn giai đoạn4 đã nghiệm thu.
- Lượt đầu14 ca có13 PASS/1 ERROR do fixture orphan gọi TokenService.issue và vô tình tạo refresh cho user không tồn tại, bị FK chặn đúng. Sửa test dùng JwtEncoder ký subject không tồn tại, không insert refresh; giữ FK/JWT policy và assertion401. Log lỗi ban đầu giữ tại `.local/artifacts/4a/initial-fixture-failure.txt`.

**Kết quả cuối: PASS4A backend local.** Maven verify **65/65**, 0 failures/errors/skipped; tổng44.283giây. FriendIntegrationTest14/14(4.428s), SearchRateLimiterTest5/5(0.595s),46 regression auth/profile/media đạt. Executable JAR build/repackage thành công. Báo cáo Surefire ở server/target/surefire-reports; Maven log `.local/artifacts/4a/maven-verify-final.txt`, ignored. Cluster test/context đóng trong @AfterAll; không chạy dịch vụ hoặc migration lên DB dev/VPS. Không chạy lại Android build/UI vì Android giữ nguyên.

## File của bước4A

- Mới trong server/src/main/java/com/taktak/server/friend/: FriendRequest, FriendRequestRepository, FriendDtos, FriendService, FriendController, FriendFailure, SearchRateLimiter, SearchThrottled.
- Sửa có đọc trước: user/UserRepository.java thêm query search/lock pair; common/ApiExceptionHandler.java thêm hai handler, giữ mọi thay đổi3D đã có.
- Mới trong server/src/test/java/com/taktak/server/: FriendIntegrationTest.java, SearchRateLimiterTest.java.
- Hồ sơ: FRIEND_API.md, API_ERRORS.md, PROJECT_DECISIONS.md, PROJECT_STATUS.md, TASKS.md, SESSION_HANDOFF.md. Không sửa Android/README/dependency/migration hoặc credentials; không commit/push/deploy.

Review cuối: snapshot xác nhận chỉ2 file source cũ sửa và10 file source/test mới;70 file Android và V1 giữ nguyên, không xóa file nào. Kiểm tra18 file của4A:0 mẫu credential/private key/JWT thực,0 conflict/trailing whitespace,74 link local hợp lệ; git diff --check đạt. JAR có FriendController, không có test classes/EmbeddedPostgres. Đặc tả/Firebase config hash giữ nguyên, artifacts/cluster được ignore, index rỗng/HEAD8766f26. Đây là rà soát chọn lọc, không phải audit toàn bộ lịch sử Git. Không có bước thủ công bắt buộc để hoàn tất4A; muốn tiếp tục cần duyệt đúng4B.
