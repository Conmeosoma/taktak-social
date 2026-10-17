# Taktak — Nghiệm thu Giai đoạn 5E: Lịch sử ảnh đã nhận (Received Moments History)

Cập nhật: 18/10/2026. Mốc kiểm thử: **Giai đoạn 5E — Lịch sử ảnh đã nhận (Received Moments History): PASS local/USB**.

Đối chiếu quy tắc: tuân thủ `AGENTS.md`, `docs/TAKTAK_PROJECT.md`, `docs/PROJECT_DECISIONS.md`. Không tự động commit, push, merge hoặc deploy. Dừng trước Giai đoạn 6 (FCM / Widgets).

---

## 1. Thiết kế kiến trúc và giải pháp kỹ thuật

Giai đoạn 5E hoàn thiện màn hình Tab 1 (Lịch sử — History) trên Android Client và cung cấp API phân trang tải danh sách khoảnh khắc đã nhận từ bạn bè trên Spring Boot Backend.

### 1.1. Backend API & Tối ưu Database
- **Endpoint**: `GET /api/moments?type=received&page=0`, yêu cầu xác thực bằng `Authorization: Bearer <access-token>`.
- **Phân trang & Đếm tối ưu**:
  - Phân trang cố định `pageSize = 20`.
  - Thay vì chạy `COUNT(*)` tốn kém trên toàn bảng, truy vấn lấy `LIMIT 21 OFFSET (page * 20)` để xác định cờ `hasNext` một cách nhẹ nhàng.
- **Tận dụng Index có sẵn**:
  - Bảng `moment_recipients` đã có index tối ưu `ix_moment_recipients_user_created` trên `(recipient_id, created_at DESC)`.
  - Native query join giữa `moment_recipients`, `moments`, và `users` (sender):
    ```sql
    SELECT m.id AS id,
           m.caption AS caption,
           m.created_at AS createdAt,
           u.id AS senderId,
           u.username AS senderUsername,
           u.display_name AS senderDisplayName
    FROM moment_recipients mr
    JOIN moments m ON m.id = mr.moment_id
    JOIN users u ON u.id = m.sender_id
    WHERE mr.recipient_id = :viewerId
      AND m.deleted_at IS NULL
    ORDER BY m.created_at DESC, m.id DESC
    LIMIT 21 OFFSET :offset
    ```
- **Cô lập bảo mật và dữ liệu**:
  - Chỉ người nhận hợp lệ được lưu trong `moment_recipients` mới thấy khoảnh khắc.
  - Người lạ (strangers) và bạn bè không được chọn tuyệt đối không nhìn thấy khoảnh khắc của nhau.
  - Ảnh chỉ mình tôi (`recipientIds=[]`) được cô lập hoàn toàn, không bao giờ xuất hiện trong danh sách đã nhận (`type=received`).
  - DTO response chỉ bao gồm thông tin công khai của người gửi: `id`, `username`, `displayName`. Tuyệt đối không để lộ email, mật khẩu hoặc số điện thoại.

### 1.2. Android Client: Material 3 Expressive History
- **Màn hình Lịch sử (Tab 1 — History)**:
  - Thay thế `PendingTabFragment` giữ chỗ ở Tab 1 của ViewPager2 bằng `HistoryFragment` hoàn chỉnh.
  - Thiết kế tuân thủ Material 3 Expressive: AppBar với tiêu đề "Lịch sử", nút làm mới (Refresh icon), thanh tiến trình `LinearProgressIndicator` (indeterminate) hiển thị khi đang tải dữ liệu.
  - **Trạng thái rỗng (Empty State)**: Khi chưa có ảnh nào được gửi đến, hiển thị minh họa rỗng tinh tế với thông báo "Chưa có khoảnh khắc nào" và "Ảnh từ bạn bè sẽ xuất hiện tại đây".
  - **Trạng thái lỗi / Thử lại (Error / Retry)**: Hiển thị thông báo thân thiện và nút "Thử lại" khi gặp sự cố mạng hoặc lỗi máy chủ.
- **Hiển thị thẻ khoảnh khắc (`item_history_moment.xml`)**:
  - Card bo góc 24dp, viền outline nhẹ nhàng, nền Surface container theo bảng màu M3E.
  - Header: Avatar ký tự đầu của người gửi, tên hiển thị (`momentSenderName`), username (`momentSenderUsername`), và thời gian tương đối (`momentTimeAgo`).
  - Khung ảnh tỷ lệ 1:1 vuông chuẩn, bo góc 18dp.
  - Chú thích (`momentCaption`): Tối đa 50 ký tự Unicode, ẩn đi nếu khoảnh khắc không có chú thích.
- **Tải ảnh an toàn qua JWT & Bộ nhớ đệm (LRU Cache)**:
  - Tất cả ảnh khoảnh khắc được tải bảo mật qua `GET /api/moments/{id}/image` với Bearer JWT xác thực ở tầng Repository/Network.
  - Giải mã byte JPEG thành `Bitmap` ngay trên luồng nền (`worker` thread) để không gây giật lag giao diện (jank).
  - Sử dụng `LruCache<String, Bitmap>` với dung lượng 20MB trong `HistoryViewModel` để cache ảnh trong RAM, hiển thị tức thì khi người dùng cuộn danh sách (scroll) hoặc chuyển tab qua lại.
- **Định dạng thời gian thân thiện (`HistoryTimeFormatter.java`)**:
  - Dưới 1 phút: "Vừa xong"
  - Dưới 1 giờ: "X phút trước"
  - Dưới 24 giờ: "X giờ trước"
  - Dưới 7 ngày: "X ngày trước"
  - Từ 7 ngày trở lên: "dd/MM/yyyy"

---

## 2. Bằng chứng kiểm thử chi tiết

### 2.1. Backend (Spring Boot & PostgreSQL)
- **Công cụ**: Apache Maven 3.8.1, Java 17, PostgreSQL 14.22 Embedded.
- **Lệnh chạy**: `mvn verify` (tại thư mục `server/`).
- **Kết quả**: **104/104 tests PASS**, 0 failures, 0 errors, 0 skipped.
  - `MomentIntegrationTest` gồm 5 integration tests mới cho 5E:
    - `receivedMomentsReturnsNewestFirstWithSenderAndCaption`: PASS — Kiểm tra Alice gửi cho Bob, Bob gọi `GET /api/moments?type=received` nhận đúng thông tin sender, caption và thứ tự thời gian mới nhất trước.
    - `receivedMomentsPaginationReturnsCorrectPageAndHasNextFlag`: PASS — Gửi 25 moments, kiểm tra trang 0 (20 items, hasNext=true) và trang 1 (5 items, hasNext=false).
    - `receivedMomentsDoesNotIncludeOwnerOnlyMoments`: PASS — Kiểm tra ảnh chụp "chỉ mình tôi" không xuất hiện trong danh sách nhận.
    - `receivedMomentsStrangerCannotSeeMoments`: PASS — Kiểm tra người lạ không được chọn không nhìn thấy khoảnh khắc.
    - `receivedMomentsValidatesTypeAndPageParameters`: PASS — Kiểm tra `type` không hợp lệ trả về 400 `INVALID_TYPE`, `page < 0` trả về 400 `INVALID_PAGE`.
  - Executable JAR được build và repackage thành công.

### 2.2. Android Unit Tests
- **Lệnh chạy**: `./gradlew.bat testDebugUnitTest` (tại thư mục `android-client/`).
- **Kết quả**: **45/45 tests PASS**, 0 failures, 0 errors.
  - Bổ sung 3 unit tests trong `MomentCaptionTest.java` kiểm thử API contract:
    - `receivedMomentsCallsEndpointWithBearerTokenAndReturnsPage`: PASS — Kiểm thử mock gọi `GET /api/moments?type=received&page=0` với Bearer header và parse DTO `MomentsPage`.
    - `receivedMomentsThrowsWhenSessionExpired`: PASS — Kiểm thử chặn gọi mạng khi phiên đã hết hạn.
    - `receivedMomentsMapsFieldsCorrectly`: PASS — Kiểm thử map đầy đủ sender, caption, createdAt và pagination flag.

### 2.3. Android Build & Lint
- **Lệnh chạy**: `./gradlew.bat assembleDebug assembleRelease lintDebug lintRelease` (tại thư mục `android-client/`).
- **Kết quả**: **BUILD SUCCESSFUL** trong 32 giây, **0 errors**.

### 2.4. Connected Acceptance Tests trên Thiết bị Thật
- **Thiết bị**: Xiaomi Redmi Note 13 4G (Model `23129RAA4G`, Serial `8e90fdff`, Android 15 / API 35), độ phân giải 1080×2400.
- **Môi trường kết nối**: Cáp USB với quy tắc `adb reverse tcp:8080 tcp:8080` kết nối tới Local Smoke Server Spring Boot.
- **Lệnh chạy**:
  ```powershell
  & $adb -s 8e90fdff reverse tcp:8080 tcp:8080; cd android-client; ./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" -Dorg.gradle.java.installations.auto-detect=false -PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/ connectedDebugAndroidTest -Pandroid.testInstrumentationRunnerArguments.class=com.taktak.app.HistoryAcceptanceTest --console=plain
  ```
- **Kết quả**:
  - `HistoryAcceptanceTest#historyTabShowsEmptyStateWhenNoMomentsReceived`: **PASS (3.801s)**
    - Chuyển sang Tab 1 (Lịch sử).
    - Tiêu đề "Lịch sử" hiển thị chính xác.
    - View rỗng hiển thị thông điệp rỗng khi người dùng chưa nhận ảnh nào.
    - RecyclerView ẩn đúng kỳ vọng.
    - Thao tác bấm nút "Làm mới" (Refresh) hoạt động trơn tru.
  - `HistoryAcceptanceTest#receivedMomentAppearsInHistoryWithSenderCaptionAndDownloadedImage`: **PASS (7.069s)**
    - Thiết lập quan hệ bạn bè giữa Alice (người gửi) và Bob (người nhận).
    - Alice tạo và upload khoảnh khắc ảnh JPEG có chú thích "Khoảnh khắc 5E test" gửi tới Bob.
    - Bob chuyển sang Tab Lịch sử và làm mới.
    - Danh sách nhận diện đúng 1 khoảnh khắc với ID khớp server.
    - Thẻ khoảnh khắc hiển thị đầy đủ tên hiển thị người gửi, chú thích, và ảnh JPEG tải thành công qua JWT với Drawable hợp lệ.
    - Tái tạo Activity (`recreate`) bảo toàn trọn vẹn danh sách và trạng thái hiển thị.
  - `MainCameraAcceptanceTest#tabsRecreationBackAndLogoutNeverKeepCameraBoundOffscreen`: **PASS (35s)**
    - Kiểm tra hồi quy toàn diện 4 tab: Khi chuyển từ Camera (Tab 0) sang History (Tab 1), CameraX giải phóng tài nguyên và unbind camera offscreen đúng thiết kế, không gây rò rỉ tài nguyên (resource leak).

### 2.5. Kiểm tra Nhất quán Dữ liệu (Data Consistency)
- Local Smoke Server kết thúc an toàn với mã thoát 0:
  ```
  SMOKE_MEDIA_ROWS=3 FILES=3 RECIPIENTS=3
  ```
- Số lượng bản ghi `moments` trong DB (3) khớp tuyệt đối với số file ảnh JPEG trên ổ đĩa VPS (3) và số lượng bản ghi phân phối `moment_recipients` (3).
- Không có file ảnh mồ côi (orphaned files) và không có bản ghi khoảnh khắc trùng lặp.
- Port 8080 và quy tắc ADB reverse được đóng và thu hồi sạch sẽ.

---

## 3. Danh sách File thay đổi trong Giai đoạn 5E

### 3.1. Android Client
- `app/src/main/java/com/taktak/app/data/model/MomentModels.java` (mới): DTOs `PublicUser`, `MomentItem`, `MomentsPage`.
- `app/src/main/java/com/taktak/app/data/remote/MomentApi.java`: Thêm method `@GET("api/moments") listMoments`.
- `app/src/main/java/com/taktak/app/data/repository/MomentRepository.java`: Thêm method `received(int page)`.
- `app/src/main/java/com/taktak/app/ui/history/HistoryFragment.java` (mới): Fragment hiển thị lịch sử ảnh đã nhận.
- `app/src/main/java/com/taktak/app/ui/history/HistoryAdapter.java` (mới): Adapter hiển thị thẻ khoảnh khắc và footer phân trang.
- `app/src/main/java/com/taktak/app/ui/history/HistoryTimeFormatter.java` (mới): Định dạng thời gian tương đối tiếng Việt.
- `app/src/main/java/com/taktak/app/viewmodel/HistoryViewModel.java` (mới): Quản lý danh sách, phân trang, tải và cache ảnh Bitmap.
- `app/src/main/java/com/taktak/app/ui/main/MainPagerAdapter.java`: Kết nối Tab 1 tới `HistoryFragment`.
- `app/src/main/res/layout/fragment_history.xml` (mới): Layout Material 3 Expressive cho màn hình Lịch sử.
- `app/src/main/res/layout/item_history_moment.xml` (mới): Layout thẻ hiển thị khoảnh khắc (card, sender, square image, caption).
- `app/src/main/res/layout/item_history_footer.xml` (mới): Layout footer tải trang tiếp theo.
- `app/src/main/res/values/history_strings.xml` (mới): Chuỗi ngôn ngữ tiếng Việt cho màn hình Lịch sử.
- `app/src/test/java/com/taktak/app/data/remote/MomentCaptionTest.java`: Bổ sung 3 unit tests cho API received moments.
- `app/src/androidTest/java/com/taktak/app/HistoryAcceptanceTest.java` (mới): Connected Acceptance Tests cho màn hình Lịch sử.
- `app/src/androidTest/java/com/taktak/app/MainNavigationTestSupport.java`: Thêm helper điều hướng Tab Lịch sử.
- `app/src/androidTest/java/com/taktak/app/MainCameraAcceptanceTest.java`: Cập nhật assertion tiêu đề Tab 1.

### 3.2. Server Backend
- `src/main/java/com/taktak/server/moment/dto/MomentDtos.java`: Bổ sung DTOs `PublicUser`, `MomentItem`, `MomentsPage`.
- `src/main/java/com/taktak/server/moment/Moment.java`: Thêm getter cho `caption` và `createdAt`.
- `src/main/java/com/taktak/server/moment/MomentRepository.java`: Thêm native query `receivedMoments(UUID viewerId, int offset)`.
- `src/main/java/com/taktak/server/moment/MomentService.java`: Thêm method `listMoments(UUID viewer, String type, int page)`.
- `src/main/java/com/taktak/server/moment/MomentController.java`: Thêm mapping `GET /api/moments`.
- `src/test/java/com/taktak/server/MomentIntegrationTest.java`: Thêm 5 integration tests kiểm thử phân trang, bảo mật và cô lập.

### 3.3. Tài liệu
- `docs/MOMENT_API.md`: Bổ sung tài liệu hợp đồng endpoint `GET /api/moments?type=received&page=0`.
- `docs/QA_MOMENT_HISTORY.md` (mới): Báo cáo nghiệm thu chi tiết giai đoạn 5E.
- `docs/PROJECT_STATUS.md`: Cập nhật tiến độ hoàn thành 5E.
- `docs/TASKS.md`: Đánh dấu hoàn thành 5E.
- `docs/SESSION_HANDOFF.md`: Bàn giao trạng thái 5E.

---

## 4. Hạn chế đã biết và Điểm dừng

- **Chưa có Lịch sử ảnh đã gửi / Endpoint DELETE**: Giai đoạn 5E chỉ phục vụ ảnh đã nhận từ bạn bè (`type=received`). Ảnh đã gửi của bản thân hoặc chức năng xóa khoảnh khắc thuộc các tiểu bước tiếp theo.
- **Chưa có FCM runtime thông báo ảnh mới**: Khi bạn bè gửi ảnh, ứng dụng cần người dùng chủ động bấm "Làm mới" (Refresh) hoặc cuộn lại để tải. Thông báo đẩy Firebase Cloud Messaging (FCM) theo thời gian thực sẽ được tích hợp trong Giai đoạn 6.
- **Chưa có AppWidget**: Hiển thị khoảnh khắc mới nhất trên màn hình chính qua Widget sẽ được phát triển trong Giai đoạn 7.
- **Tuân thủ quy tắc bảo mật & Git**: Không tự động commit, push, merge hoặc deploy. Bảo toàn trọn vẹn worktree hiện tại.

**Đề xuất bước tiếp theo**: Báo cáo người dùng phê duyệt hoàn thành Giai đoạn 5E trước khi bước vào **Giai đoạn 6 — Firebase Cloud Messaging (FCM) & Đồng bộ thời gian thực**.
