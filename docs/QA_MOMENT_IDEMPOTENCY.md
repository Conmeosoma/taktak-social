# Taktak — Nghiệm thu Giai đoạn 5D: Upload Progress, Retry & Idempotency Key

Cập nhật: 18/10/2026. Mốc kiểm thử: **Giai đoạn 5D — Upload Progress, Retry & Idempotency Key: PASS local/USB**.

Đối chiếu quy tắc: tuân thủ `AGENTS.md`, `docs/TAKTAK_PROJECT.md`, `docs/PROJECT_DECISIONS.md`. Không tự động commit, push, merge hoặc deploy.

---

## 1. Thiết kế kiến trúc và giải pháp kỹ thuật

Theo chỉ đạo của người dùng, cơ chế Idempotency Key không sử dụng single-JVM in-memory cache mà được lưu trữ bền vững qua PostgreSQL, đảm bảo hoạt động an toàn kể cả khi server restart.

### 1.1. Backend Database Migration (V2) & Entity
- Giữ nguyên toàn bộ migration `V1__init_schema.sql` đã áp dụng.
- Tạo migration mới `V2__moment_idempotency.sql`:
  - Thêm cột `idempotency_key UUID` vào bảng `moments`.
  - Thêm cột `payload_hash VARCHAR(64)` vào bảng `moments`.
  - Tạo unique index có điều kiện:
    ```sql
    CREATE UNIQUE INDEX uq_moments_sender_idempotency
        ON moments (sender_id, idempotency_key)
        WHERE idempotency_key IS NOT NULL;
    ```
- Cập nhật entity `Moment.java` và repository `MomentRepository.java`:
  - Thêm các trường `idempotencyKey` và `payloadHash`.
  - Thêm truy vấn `findBySenderIdAndIdempotencyKey(UUID senderId, UUID idempotencyKey)`.

### 1.2. Fingerprint Request & Xử lý Idempotency
- **Fingerprint Payload**: Tính bằng mã băm SHA-256 qua `senderId`, `caption` (UTF-8), danh sách `recipientIds` đã chuẩn hóa và sắp xếp tăng dần theo UUID, và toàn bộ raw bytes của file ảnh JPEG.
- **Xử lý Replay**:
  - Khi nhận request có header `Idempotency-Key`:
    1. Kiểm tra định dạng UUID; nếu sai định dạng trả `400 INVALID_IDEMPOTENCY_KEY`.
    2. Khóa bi quan sender (và các recipients nếu có) bằng `UserRepository.lockAll` theo thứ tự UUID để tuần tự hóa các request đồng thời từ cùng một tài khoản.
    3. Tìm kiếm moment đã lưu theo `(senderId, idempotencyKey)`.
    4. Nếu đã tồn tại và `payloadHash` trùng khớp: trả về ngay `201 Created` với `{"id":"<momentId cũ>"}`, không ghi đè dữ liệu, không tạo thêm bản ghi `moment_recipients`, không lưu file ảnh trùng lặp lên VPS, và không phát lại FCM.
    5. Nếu đã tồn tại nhưng `payloadHash` khác nhau (thay đổi caption, danh sách người nhận hoặc ảnh): trả về ngay `409 IDEMPOTENCY_CONFLICT`.
    6. Nếu chưa tồn tại: tiến hành xác thực ảnh, lưu file ảnh vào storage private, ghi nhận DB trong cùng transaction. Nếu gặp xung đột unique constraint do race condition ở tầng DB, `ApiExceptionHandler` bắt lỗi và chuyển thành `409 IDEMPOTENCY_CONFLICT`.

### 1.3. Android Client: Progress Tracking & Retry
- **CountingRequestBody**: Kế thừa `RequestBody` của OkHttp, sử dụng `ForwardingSink` để đếm số byte thực tế đã ghi vào socket (`bytesWritten`) so với `contentLength()`.
- **Phân biệt tiến trình truyền tải vs xác nhận máy chủ**:
  - Khi đang gửi dữ liệu (0–99%): ProgressBar hiển thị determinate dạng phần trăm kèm thông báo "Đang tải lên: X%".
  - Khi đã ghi xong 100% byte và đang chờ server xử lý/lưu ảnh/xác nhận: ProgressBar chuyển sang indeterminate kèm thông báo "Máy chủ đang xử lý và xác thực ảnh...".
  - Chỉ khi server phản hồi thành công và tải xong ảnh xác minh qua JWT mới hiển thị trạng thái hoàn thành.
- **Quản lý Idempotency Key & Retry**:
  - Khởi tạo key UUID cho mỗi bản nháp và lưu trữ trong `SavedStateHandle` của `PreviewViewModel` để bảo toàn key qua vòng đời Activity (xoay màn hình, recreate) hoặc process bị gián đoạn.
  - Khi người dùng chỉnh sửa nội dung bản nháp (thay đổi caption hoặc thay đổi danh sách người nhận), idempotency key tự động được xóa để lần gửi tiếp theo tạo key mới độc lập.
  - Khi gặp lỗi mạng (IOException / timeout): trạng thái chuyển sang cho phép retry, giữ nguyên idempotency key và nội dung payload đã đóng băng (frozen payload) để gửi lại đúng bản nháp ban đầu.
  - Khi gặp lỗi không thể retry (400 validation, 401 unauthorized, 409 idempotency conflict): giải phóng trạng thái đóng băng và xóa key để người dùng có thể chỉnh sửa lại.

---

## 2. Bằng chứng kiểm thử chi tiết

### 2.1. Backend (Spring Boot & PostgreSQL)
- **Công cụ**: Apache Maven 3.8.1, Java 17 (Eclipse Adoptium 17.0.20.1+1), PostgreSQL 14.22 Embedded.
- **Lệnh chạy**: `mvn verify` (tại thư mục `server/`).
- **Kết quả**:
  - `MomentIntegrationTest`: 12/12 tests PASS.
    - `idempotentUploadReplaysExistingMomentWithoutDuplicateFileOrRow`: PASS (Replay cùng key + payload trả cùng ID, DB chỉ có 1 row, storage chỉ có 1 file).
    - `idempotencyConflictReturns409WhenPayloadChanges`: PASS (Cùng key khác caption hoặc khác ảnh trả về 409 IDEMPOTENCY_CONFLICT).
    - `invalidIdempotencyKeyReturnsBadRequest`: PASS (Key sai định dạng UUID trả về 400 INVALID_IDEMPOTENCY_KEY).
    - `concurrentIdempotentUploadsReturnSameMomentWithoutDuplicates`: PASS (Hai thread đồng thời gửi cùng key, chỉ 1 row được tạo, cả hai nhận cùng ID).
  - `ServerBootstrapTest`: PASS (Kiểm tra 2 Flyway migrations V1 & V2 thành công, index `uq_moments_sender_idempotency` tồn tại trên DB).
  - Toàn bộ suite backend: **99/99 tests PASS, 0 failures, 0 errors, 0 skipped**.
  - Đóng gói file JAR thực thi thành công: `target/taktak-server-0.0.1-SNAPSHOT.jar`.

### 2.2. Android Client (Unit Tests & Build Lint)
- **Công cụ**: Gradle 9.6, Android Studio JBR 21, compileSdk 37, minSdk 26.
- **Unit Tests**:
  - Lệnh chạy: `./gradlew.bat testDebugUnitTest --no-daemon`
  - Kết quả: **42/42 tests PASS, 0 failures, 0 errors**:
    - `MomentCaptionTest`: 9/9 tests PASS (bổ sung test kiểm tra header `Idempotency-Key` được gửi đúng, kiểm tra `CountingRequestBody` cập nhật tiến trình 0–100%, kiểm tra validation chặn key không hợp lệ trước khi gọi mạng, kiểm tra phản hồi 409).
    - `AuthSessionTest`: 12/12 tests PASS.
    - `FriendRepositoryTest`: 8/8 tests PASS.
    - `AuthInputValidatorTest`: 7/7 tests PASS.
    - `CameraRotationTest`: 3/3 tests PASS.
    - `CameraSettingsViewModelTest`: 2/2 tests PASS.
    - `ExampleUnitTest`: 1/1 test PASS.
- **Biên dịch và Phân tích tĩnh (Lint)**:
  - Lệnh chạy: `./gradlew.bat assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/ -PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/ --no-daemon`
  - Kết quả: **BUILD SUCCESSFUL**, 0 errors (debug và release APK đều đóng gói thành công).

### 2.3. Kiểm thử trên thiết bị thật Xiaomi (USB Reverse)
- **Thiết bị**: Xiaomi Redmi Note 13 4G (mã `23129RAA4G`, serial `8e90fdff`), chạy Android 15 (API 35).
- **Môi trường kết nối**: `Start-LocalSmokeServer.ps1 -Port 8080` (Embedded PostgreSQL) + `adb reverse tcp:8080 tcp:8080`.
- **Kết quả kết nối từ thiết bị**:
  - `curl -s http://127.0.0.1:8080/actuator/health` từ shell Xiaomi trả về `{"status":"UP"}`.
- **Connected Acceptance Tests thực tế trên thiết bị**:
  1. `PreviewAcceptanceTest#detailedPhotosHaveBoundedBytesDimensionsAndDoNotUpscaleSmallPhotos`: **PASS** (1.770 s).
  2. `PreviewAcceptanceTest#privateSaveUsesExplicitConfirmationAndDownloadsJwtImage`: **PASS** (7.025 s).
     - Kiểm tra xác nhận dialog, gửi ảnh, hiển thị tiến trình, server lưu DB và tải lại đúng byte qua JWT.
  3. `PreviewAcceptanceTest#realCameraSendsSelectedFriendsAndDeniesUnselectedFriend`: **PASS** (10.596 s).
     - Kiểm tra gửi ảnh cho bạn bè với header Idempotency-Key, server ghi nhận chính xác 2 người nhận, bạn bè ngoài danh sách bị từ chối 404.
- **Kiểm tra tính nhất quán dữ liệu (Data & File Consistency)**:
  - Khi tắt `LocalSmokeServer`, hệ thống tự động chạy kiểm tra consistency giữa PostgreSQL và thư mục ảnh:
    ```text
    SMOKE_MEDIA_ROWS=2 FILES=2 RECIPIENTS=2
    ```
  - **Xác nhận**: Không có bất kỳ moment trùng lặp nào, không có file ảnh mồ côi nào trên đĩa VPS.

---

## 3. Tổng hợp trạng thái kiểm thử

| Nhóm kiểm thử | Phạm vi | Số ca | Kết quả | Ghi chú |
|---|---|---|---|---|
| Backend Unit & Integration | Spring Boot / PostgreSQL | 99 | **PASS** | 0 failures, 0 errors, 0 skipped. Gồm 4 ca idempotency & concurrency. |
| Android Unit Tests | JVM / Mockito / TestServer | 42 | **PASS** | 0 failures, 0 errors. Bổ sung kiểm tra header key & progress listener. |
| Android Build & Lint | assemble/lint debug/release | 4 tasks | **PASS** | BUILD SUCCESSFUL, 0 errors. |
| Xiaomi Device (Android 15) | Acceptance tests qua USB reverse | 3 | **PASS** | Chụp ảnh thật, gửi riêng tư, gửi bạn bè, tải qua JWT. |
| Media & Database Consistency | Embedded Postgres & VPS disk | 1 | **PASS** | `ROWS=2 FILES=2 RECIPIENTS=2`. Tuyệt đối không trùng lặp hay mồ côi. |

---

## 4. Kết luận & Đề xuất

- **Đánh giá Giai đoạn 5D**: **PASS**.
- Toàn bộ yêu cầu kỹ thuật của người dùng về tính bền vững qua PostgreSQL, Flyway V2, SHA-256 fingerprint, replay 201, conflict 409, concurrency lock, progress 0–100% và bảo toàn key trong SavedStateHandle đã được hoàn tất và nghiệm thu với bằng chứng thực tế.
- **Đề xuất tiểu bước tiếp theo**: **Giai đoạn 5E — Lịch sử ảnh đã nhận (Received Moments History)**.
- **Hành động hiện tại**: **DỪNG LẠI và chờ người dùng phê duyệt trước khi triển khai 5E**.
