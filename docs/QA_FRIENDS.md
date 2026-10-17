# Taktak — Friends client (4C)

Ngày: 17/10/2026 (Asia/Saigon). Phạm vi người dùng duyệt: **FriendsFragment nối API qua Repository/ViewModel**. Không triển khai5A, không commit/push/deploy.

## Trước và sau triển khai

Trước phiên:4A/4B backend đã có search, incoming, respond, friends, unfriend, block/unblock; Android chưa có FriendsFragment. HEAD `8766f26`; dirty/untracked của các phiên trước được giữ nguyên. Kiểm tra Git thực tế cuối phiên cho thấy nhánh `chore/push-current-project`, không phải main như hồ sơ cũ; giữ nguyên nhánh/HEAD, không tự checkout. Đã đọc toàn bộ đặc tả và quy tắc trước khi sửa.

Luồng hiện tại: đăng nhập → Camera → nút **Bạn bè** → FriendsFragment. Quay lại bằng nút Camera hoặc Back; logout dùng SessionViewModel hiện có. MainActivity giữ cổng xác minh phiên và gỡ CameraFragment khi mở Friends để camera không tiếp tục hoạt động phía sau. Chưa tạo bốn tab/ViewPager2 của5A.

FriendsFragment dùng ViewBinding/Material, một RecyclerView cho kết quả tìm kiếm, lời mời đến và danh sách bạn. Có empty/loading/error, tải lại, tải thêm page20, gửi mời, accept/reject, xác nhận hủy bạn/chặn. Truy vấn prefix chỉ gửi khi chọn Tìm/IME Search, không gửi theo từng phím. Chuẩn hóa giống server, không wildcard;429 tôn trọng Retry-After trước khi mở lại nút tìm.

ApiClient dùng lại interceptor/refresh JWT đã có; AuthRepository cung cấp FriendRepository. Repository gắn với generation của phiên, kiểm tra trước/sau HTTP và validate DTO/page. ViewModel chạy I/O ngoài main, xuất snapshot LiveData, chặn thao tác kép và kiểm tra phiên trước khi publish. Khi đổi tài khoản: bỏ dữ liệu cũ, xác minh phiên rồi tạo repository mới; không xóa phiên tài khoản mới hoặc lặp điều hướng. Không log raw server error, query, token hoặc thông tin tài khoản.

Mutation thành công nhưng đọc danh sách thất bại có thông báo riêng; tải lại chỉ đọc, không tự lặp POST/PUT/DELETE. Offset pagination có thể dịch khi dữ liệu bên ngoài thay đổi; tải lại bắt đầu từ trang0, ghép trang theo ID để tránh hiển thị trùng. Không tuyên bố exactly-once khi mất phản hồi.

Avatar được nạp bằng Glide từ drawable local vì PublicUser hiện chỉ có id/username/displayName; không dựng URL ảnh giả hoặc public. API avatar và danh sách unblock không được thêm ở4C; server4B vẫn có DELETE block nhưng chưa có UI quản lý unblock. HTTP local chỉ debug loopback qua USB/AVD reverse; release vẫn HTTPS.

## Bằng chứng kiểm thử đã chạy

| Kiểm tra | Kết quả thực tế |
|---|---|
| Android JVM | **28/28 PASS**,0 failures/errors/skipped:8 FriendRepository mới +20 regression auth/validation/mẫu |
| Backend | **Maven verify87/87 PASS**,0 failures/errors/skipped, executable JAR;63 giây. Source backend/schema không đổi |
| USB Android15/API35 | **5/5 PASS**,49.743s: empty/invalid/no result/recreation/back/logout; gửi/chấp nhận/từ chối/hủy bạn/chặn; duplicate409; revoked refresh; đổi tài khoản không lộ snapshot cũ |
| AVD Android17/API37 | Bốn ca UI/API chính đạt trong lượt5 ca; ca thứ5 gặp quota login dùng chung IP. Chạy lại riêng ca đổi tài khoản cùng offline/retry **2/2 PASS**,73.804s; không tính lượt5 ca có lỗi là toàn bộ PASS |
| Hai thiết bị đồng thời | **2 vai PASS**: A/USB14.307s, B/AVD38.971s. A gửi mời bằng UI, B thấy và chấp nhận bằng UI, hai bên thấy bạn; A chặn, B mất quan hệ, gửi mời bằng API trả404 và search không thấy A; tài khoản C riêng không có friend/incoming |
| Offline/retry | HTTP connector của smoke backend được stop/start qua stdin, giữ DB và reverse; thông báo lỗi, giữ phiên mã hóa, tải lại thành công sau phục hồi. Không thay mạng/firewall/TLS điện thoại |
| Regression USB | **7/7 PASS**,16.209s:2 launcher/login/logout,4 AuthBackend (mã hóa/refresh/replay/ciphertext/origin),1 CameraLayout chữ lớn hai theme. Không chụp lại ảnh cá nhân |
| Build/lint cuối | **PASS** assembleDebug/assembleDebugAndroidTest/assembleRelease và lintDebug/lintRelease,33s.0 errors/3 cảnh báo cũ (PluralsCandidate,2 unused color), không thêm warning UI |
| Header/layout và lặp core cuối | **6/6 USB PASS**,52.116s: chạy lại5 ca core trên APK cuối +FriendsLayoutTest;280dp vùng nội dung/560dp cao, font1.5, hai theme, không tràn ngang/cắt chiều cao chữ, list còn ít nhất100dp. Không đổi font/theme hệ thống |

Unit mới kiểm tra normalize/Unicode whitespace, search và hai loại page qua Bearer, JSON mutation/204, input sai không gọi mạng, body sai,409/404/429 và Retry-After, repository không tái sử dụng sau đổi phiên, phản hồi muộn bị loại. Lớp HTTP fixture chỉ ở test JVM, không thay API production bằng mock.

Ảnh/log ignored: `.local/artifacts/4c/`. Log đạt: `maven-regression.txt`, `gradle-layout-final.txt`, `usb-final-six.txt`, `usb-five-clean-pip.txt`, `avd-network-account.txt`, `two-device-sender.txt`, `two-device-receiver.txt`, `usb-regression.txt`. Ảnh USB/AVD đã pull vào `usb-handoff-screens/`, `avd-final-screens/`; ảnh light/dark, empty/search/incoming/friends/confirmation/conflict/logout/expired/offline/retry. Một số ảnh USB đầu có cửa sổ nổi/PiP, không dùng làm bằng chứng không bị che; ảnh cuối đã xem trực tiếp. Các lượt trước APK cuối vẫn là bằng chứng lịch sử trong cùng4C, không cộng lại thành số ca duy nhất.

## Lỗi phát hiện và cách xử lý

- Sandbox không cho compiler đọc SDK và PostgreSQL tạo restricted token. Chạy lại với quyền phù hợp; sau khi hai build cũ kết thúc, clean outputs build rồi build lại. Không sửa/xóa source hoặc hạ tiêu chí.
- ActivityScenario bị chặn mở Activity ngầm trên MIUI: chuyển harness sang shell launcher đã dùng ở QA trước, theo dõi lifecycle/resource ID, có timeout; không đổi production auth để qua test.
- Lỗi thật Glide khi recreate/destroy: adapter trước đây gọi Glide.with(view) lúc Activity đã destroyed. Sửa giữ RequestManager theo Fragment và clear request qua manager hiện có; recreate/expired/logout đã chạy lại đạt.
- Popup chưa hiện đủ: cuộn chính xác nút vào viewport, dùng Espresso click giữ ràng buộc visible90%, chờ animation. Không tắt animation hệ thống hoặc chạm tọa độ đoán.
- MIUI từng từ chối cài test APK: không nhận lượt chạy APK cũ là bản mới; cài lại sau xác nhận người dùng, kiểm tra Success trước khi chạy.
- Video PiP che nút dialog: ảnh xác minh nguyên nhân, người dùng đóng hẳn; chạy lại5/5 đạt. Không nới điều kiện focus hoặc xóa ca lỗi.
- AVD unauthorized do ADB chạy khác tài khoản và snapshot/kết nối cũ: khởi động lại đúng ADB/AVD của QA, giữ dữ liệu. System UI ANR sau cold boot: đọc dialog/bounds, chọn Wait; không phải crash Taktak. Quota login20/IP/phút do hai suite đồng thời: chờ cửa sổ quota rồi chạy lại, không đổi limiter.

## File của4C

Sửa4 file có sẵn: `MainActivity.java`, `ApiClient.java`, `AuthRepository.java`, `activity_main.xml`.

Thêm14 file Android:

- `data/model/FriendModels.java`, `data/remote/FriendApi.java`, `data/repository/FriendRepository.java`.
- `viewmodel/FriendsViewModel.java`, `ui/friends/FriendsFragment.java`, `ui/friends/FriendsAdapter.java`.
- `res/layout/fragment_friends.xml`, `item_friend.xml`, `item_friends_section.xml`; `res/drawable/friend_avatar.xml`, `res/values/friends_strings.xml`.
- Test: `FriendRepositoryTest.java`, `FriendsAcceptanceTest.java`, `FriendsLayoutTest.java`.

Hồ sơ: QA_FRIENDS mới; PROJECT_DECISIONS, PROJECT_STATUS, TASKS, SESSION_HANDOFF được cập nhật. Không sửa README, đặc tả, server, migration, dependency, cấu hình Firebase hoặc phần Camera/Auth có sẵn ngoài điểm tích hợp đã nêu.

## Giới hạn và chạy lại

- Chưa runtime API26, TalkBack đầy đủ, HTTPS release thật, tải lớn hoặc E2E nút Tải thêm với hơn20 quan hệ trên điện thoại. Contract page được kiểm tra bằng unit client và suite backend; không nhận đã chạy UI pagination quy mô lớn.
- Chưa nghiệm thu UI429 hết thời gian chờ bằng đồng hồ thật; parse Retry-After có unit test và cooldown có implementation, không nhận đã đo E2E60 giây.
- **Chưa nhận toàn bộ giai đoạn4/recipient-aware media PASS**: server3D vẫn từ chối mọi recipient không rỗng. B bị chặn cũng không thể gửi ảnh ở trạng thái hiện tại, nhưng kiểm tra block riêng trong luồng gửi ảnh cho bạn bè phải nghiệm thu ở5C; không mở hỗ trợ recipient trong4C.
- Tài khoản fixture và DB/storage tạm không dùng làm tài khoản dev lâu dài. Test không ghi mật khẩu/token ra args/file/log; đối số hai thiết bị chỉ chứa username QA, không credential. Fixture từ chối phiên không sở hữu; owner ID local dùng phục hồi đúng phiên QA nếu lượt test gián đoạn.

Build Windows: chạy `scripts/Use-TaktakJava.ps1 -Target Android`, rồi Gradle với JDK Android Studio, debug URL `http://127.0.0.1:8080/`, release URL HTTPS placeholder và các task `assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease`. Placeholder release chỉ dùng để kiểm tra build, không phải API đã deploy/signing.

QA runtime: `./scripts/Start-LocalSmokeServer.ps1 -Port 8080`, database mới dưới server/target; cài debug/test APK thành công, `adb reverse tcp:8080 tcp:8080`, chạy class/method bằng AndroidJUnitRunner. Chỉ chọn networkFailure khi có host điều khiển connector và `taktakFriendsOffline=true`; ca hai thiết bị cần username ngẫu nhiên riêng cùng `taktakQaUsername`, `taktakQaPeer`, `taktakQaRole`. Không chạy cả suite đặc biệt thiếu điều kiện rồi coi skipped là PASS.

**4C PASS local/USB/AVD trong phạm vi Friends UI/API.** Smoke đã dừng exit0 (3 moment/3 file/0 recipient, metadata/file khớp); không mở/xem hoặc xóa ảnh. Reverse8080 đã gỡ trên cả hai thiết bị, AVD của phiên QA đã tắt. Điện thoại cold launch1667ms về Login, UI dump có emailInput/không friendsRecycler; fixture đã logout. Không thay timeout, animation, mạng hoặc security hệ thống điện thoại.

Review cuối: snapshot8 file cũ sửa/15 mới/0 xóa;51 file server và66 file Android cũ còn lại giữ nguyên.23 file trong phạm vi không có mẫu API key/private key/JWT thực,conflict hoặc trailing whitespace được quét;73 link local hợp lệ;git diff --check đạt. Spec/Firebase JSON giữ hash,HEAD8766f26/index rỗng;JAR có FriendController/không test harness. Quét chọn lọc không thay thế audit toàn lịch sử.

Trạng thái tổng thể và cleanup được ghi tại [SESSION_HANDOFF.md](SESSION_HANDOFF.md). Đề xuất duy nhất sau4C: **5A — hoàn thiện điều hướng bốn tab và CameraX**, chờ phê duyệt; chưa triển khai.
