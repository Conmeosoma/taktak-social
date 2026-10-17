# Taktak — Chấp nhận, friendship và block (4B)

Ngày: 17/10/2026. **PASS backend local**, sau4A. Chỉ backend; chưa FriendsFragment/4C, gửi ảnh bạn bè/5C, FCM hoặc deploy. Giữ Java17/Spring Boot/JPA/PostgreSQL, schema V1, JWT/refresh, response4A và Android. Hợp đồng search/gửi/list lời mời tại [FRIEND_API.md](FRIEND_API.md).

## Xác thực và endpoint

Mọi endpoint yêu cầu JWT hợp lệ và user subject còn tồn tại; actor chỉ lấy từ JWT. Thiếu/sai/hết hạn/orphan JWT trả401. Response thành công có no-store; không serialize entity/email/password/token/avatar_path. Không có quyền cập nhật actor/sender/receiver/status bằng field ngoài DTO.

| Method và endpoint | Input | Thành công |
|---|---|---|
| PUT /api/friends/requests/{id} | JSON action accept hoặc reject | 200 RequestResponse như4A, status cập nhật |
| GET /api/friends?page=0 | page0..10000 | 200 FriendsPage |
| DELETE /api/friends/{id} | id là **userId của người bạn**, không phải friendshipId | 204 |
| POST /api/blocks/{userId} | Không cần body | 204 |
| DELETE /api/blocks/{userId} | Không cần body | 204 |

## Phản hồi lời mời

```json
{"action":"accept"}
```

Action chỉ nhận chính xác `accept`/`reject`, phân biệt hoa thường, không trim. Thiếu/null/sai action, JSON hoặc UUID hỏng trả400 VALIDATION_ERROR; sai Content-Type415. Request không tồn tại hoặc actor không phải **receiver** cùng trả404 FRIEND_REQUEST_NOT_FOUND, không tiết lộ request cho sender/người thứ ba.

- pending→accepted: cập nhật status/responded_at và tạo một friendship chuẩn hóa trong cùng transaction.
- pending→rejected: cập nhật status/responded_at, không tạo friendship.
- Đã có block hai chiều:404 FRIEND_TARGET_UNAVAILABLE, không được accept/reject vượt block.
- Retry **cùng quyết định** đã xử lý:200 với RequestResponse hiện tại, không đổi responded_at, không tạo lại friendship. Quyết định ngược với trạng thái đã chốt:409 FRIEND_REQUEST_ALREADY_RESPONDED.
- `accepted` là lịch sử lời mời, **không chứng minh vẫn đang là bạn**. Sau unfriend, retry accept lời mời cũ vẫn200 nhưng không khôi phục quan hệ; phải gửi và chấp nhận lời mời mới. Client lấy trạng thái quan hệ từ GET friends, không suy từ receipt cũ.
- Không cho tự accept khi gửi lời mời chéo; POST4A vẫn409 khi đã có pending bất kỳ chiều nào hoặc friendship.
- RequestResponse4A giữ nguyên id/sender public/receiverId/status/createdAt; responded_at lưu ở DB, không thêm field mới vào DTO4A.

## Danh sách bạn và hủy bạn

GET friends chỉ các friendship chứa actor, loại user bị block hai chiều ngay cả khi có dữ liệu legacy không nhất quán. Mỗi item chỉ id/username/displayName. Sort username ASC, page size20, fetch21 để tính hasNext; không total count. Page không hợp lệ400; hết dữ liệu200 items rỗng. Offset pagination có thể dịch trang khi dữ liệu thay đổi, chưa cursor/snapshot.

```json
{"items":[],"page":0,"hasNext":false}
```

DELETE friends/{id} chỉ xóa cặp actor–target; không nhận friendshipId hoặc ID tài khoản nguồn từ body. Cả hai phía có quyền hủy. Target tồn tại nhưng không còn friendship vẫn204, không xóa lịch sử/lời mời khác. Target không tồn tại404 FRIEND_TARGET_UNAVAILABLE; tự hủy với chính mình400 FRIEND_SELF. Người thứ ba gọi với target của A/B chỉ tác động cặp của chính họ, không xóa được A–B.

## Chặn và bỏ chặn

POST blocks/{userId} thực hiện **cùng transaction**:

1. Ghi block có hướng actor→target; nếu đã có thì không tạo bản ghi hoặc đổi created_at.
2. Xóa friendship của cặp ở cả cách gọi A/B.
3. Chuyển mọi lời mời pending hai chiều sang rejected, ghi responded_at; giữ lịch sử accepted/rejected trước đó.

Mỗi bước lỗi DB đều rollback cả ba bước. Block thành công204, lặp lại204. Self400 BLOCK_SELF, target không tồn tại404 FRIEND_TARGET_UNAVAILABLE. Có reverse block vẫn cho actor tạo block riêng của mình; không thông báo có reverse block hay không.

DELETE blocks chỉ xóa **actor→target**, không xóa target→actor. Không còn block riêng vẫn204 nếu target tồn tại. Không khôi phục friendship hoặc pending; còn reverse block thì search/gửi mời vẫn bị ẩn/chặn. Sau khi cả hai phía bỏ chặn, có thể gửi lời mời **mới**. Không thêm GET blocked-list hoặc UI ngoài API đặc tả.

Block4B đã phối hợp search/gửi/list incoming4A và list friends. Ảnh vẫn giữ lát cắt3D owner-only, từ chối mọi recipientIds khác rỗng, kể cả đã là bạn; chưa nhận đã hoàn thiện kiểm tra recipient/block của gửi ảnh bạn bè5C hoặc toàn bộ FRIEND-02 MVP.

## Transaction và đồng thời

Mọi mutation send/accept/reject/unfriend/block/unblock dùng cùng `UserRepository.lockPair`: khóa users bằng ORDER BY UUID PostgreSQL FOR UPDATE, không dùng UUID.compareTo Java. UUID dấu bit cao được kiểm thử, friendship insert dùng LEAST/GREATEST và unique pair V1.

Respond chỉ lấy projection senderId/receiverId để kiểm tra receiver trước khóa cặp; sau khi có khóa users mới đọc/khóa entity request. Không cache entity/status từ trước khi chờ khóa để tránh quyết định trên trạng thái cũ. Friendship insert và các mutation blocks dùng native SQL có bind trong JPA repository/transaction hiện có. Unique pending-pair/friendship và PK blocks ở DB giữ nguyên; không migration hoặc ddl-auto=update.

Accept đồng thời cùng action trả200, chỉ một friendship. Accept/reject cạnh tranh: một quyết định thắng; cùng action thắng có thể retry200, action còn lại409. Accept hoặc send cạnh tranh block: kết quả HTTP tùy thứ tự khóa, nhưng sau block commit không còn friendship/pending của cặp nếu không có thao tác unblock/new request tiếp theo. Unfriend/accept retry không được hồi sinh friendship từ lời mời đã xử lý. Đây là kiểm thử concurrency chức năng, không benchmark/load test hoặc chứng minh mọi lịch chạy có thể có.

## Kiểm thử Windows và bằng chứng

```powershell
. ./scripts/Use-TaktakJava.ps1 -Target Server
cd server
./mvnw.cmd "-Dmaven.repo.local=$((Get-Location).Path)/../.tools/maven-repository" -B -ntp verify
```

Chỉ test4B:

```powershell
./mvnw.cmd "-Dmaven.repo.local=$((Get-Location).Path)/../.tools/maven-repository" -B -ntp '-Dtest=FriendRelationIntegrationTest' test
```

Kết quả cuối **87/87 PASS**,0 failures/errors/skipped, Maven verify46.821s và executable JAR build/repackage. **22 test4B mới PASS4.648s**;65 regression4A/auth/profile/media được chạy lại cùng suite. Test chạy HTTP thật qua filter chain/MVC/JPA với PostgreSQL14.22 cluster riêng dưới server/target, JWT key ngẫu nhiên trong bộ nhớ; không DB dev/VPS/Android hoặc credentials trong argv.

22 ca4B bao gồm: canonical UUID cả hai hướng, DTO/list isolation, reject/idempotency/timestamp, receiver-only/404, validation/mass assignment,12 accept retries,12 accept/reject cạnh tranh, hai phía unfriend/không resurrect, third-party isolation, block accepted/search/send hai chiều, block pending hai chiều/unblock không restore, chỉ bỏ block riêng, giữ lịch sử/quan hệ khác, self/unknown/malformed, pagination20/lọc legacy block, JWT4 dạng, rollback friendship insert, rollback block cleanup, accept/block (4 cặp×12 calls), send/block, chờ lock rồi đọc trạng thái rejected vừa commit, unfriend/accept retries.

Rollback dùng trigger **chỉ cluster test**, đã tháo trong finally. Ca chờ lock xác nhận HTTP request thực sự chờ users lock trước khi transaction khác reject/commit, không chỉ dùng sleep để đoán race. Lượt đầu18/21 đạt,3 FAIL do test: AssertJ containsOnly đòi xuất hiện mọi mã trong danh sách cho phép dù block có thể thắng tất cả; và pg_stat_activity cache snapshot theo transaction khiến không thấy waiter mới. Đã sửa assertion kiểm tra tập mã hợp lệ **kèm đúng6 block204/invariants cuối**, gọi pg_stat_clear_snapshot trong test để quan sát lock mới, không đổi timeout hoặc bỏ test. Retest21/21 đạt13.786s; thêm ca unfriend/accept rồi full verify87/87 đạt. Log negative rollback/duplicate là mong đợi, không phải test FAIL mới.

Artifact `.local/artifacts/4b/relation-tests-initial.txt`, relation-tests-retest.txt, maven-verify-final.txt và server/target/surefire-reports, ignored. Context/PostgreSQL đóng trong @AfterAll; file/cluster test được giữ local, không xóa/reset dữ liệu cá nhân. Không Android build/USB, không gửi FCM hoặc deploy; không nhận các bằng chứng Android lịch sử là vừa chạy lại.

Hành vi snapshot và cách lấy lại dữ liệu thống kê trong test đã đối chiếu [PostgreSQL14 — Viewing Statistics](https://www.postgresql.org/docs/14/monitoring-stats.html#MONITORING-STATS-VIEWS). pg_stat_clear_snapshot chỉ bỏ snapshot thống kê hiện tại, không reset dữ liệu hoặc counter.

## File thay đổi và điểm dừng

- Sửa5 file trong server/src/main/java/com/taktak/server/friend/: FriendRequest.java, FriendRequestRepository.java, FriendDtos.java, FriendService.java, FriendController.java. Không ghi đè nội dung4A đã có; helper khóa cặp dùng chung cho send4A và4B.
- Tạo server/src/test/java/com/taktak/server/FriendRelationIntegrationTest.java.
- Tạo tài liệu này; cập nhật FRIEND_API, API_ERRORS, PROJECT_DECISIONS, PROJECT_STATUS, TASKS, SESSION_HANDOFF. Không thay stack/định danh/JWT/Android/README/dependency/migration/Firebase config.
- Không có blocker hoặc thao tác thủ công bắt buộc để hoàn tất4B. Chưa nghiệm thu UI/hai thiết bị của toàn giai đoạn4, rate limiter phân tán/trusted proxy hoặc dữ liệu production lớn.

Rà soát cuối: snapshot chỉ5 source friend cũ đổi và1 test mới;70 file Android, V1 và các source khác giữ nguyên, không xóa file.13 file source/hồ sơ của bước này không có mẫu private key/API key/JWT thực, conflict hoặc trailing whitespace;80 link local hợp lệ,git diff --check đạt. JAR có FriendController, không test classes/EmbeddedPostgres; artifacts/cluster ignored, index rỗng/HEAD8766f26, spec/Firebase config hash giữ nguyên. Kiểm tra chọn lọc, không nhận là audit toàn lịch sử Git.

**Dừng sau4B. Đề xuất duy nhất4C — FriendsFragment nối API qua Repository/ViewModel; cần người dùng xác nhận. Không commit/push/deploy.**
