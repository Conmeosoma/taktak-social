# Taktak — Bàn giao phiên khôi phục

Ngày: 16/10/2026 (Asia/Saigon). Nhánh khi kiểm tra: `main`. HEAD: `99b6db4` — `first commit`.

**Trạng thái mới nhất:** AUTO CONTINUE cho **toàn Bước 0 và giai đoạn 1**, kể cả phần độc lập khi blocker khác còn tồn tại. 1A PASS local; Android 1B đã triển khai và kiểm thử local. 0B vẫn BLOCKED Console; 0C/1C chưa DONE vì alias timeout và chưa máy thật. Xem mục AUTO CONTINUE cuối file và QA_BOOTSTRAP. Các mục trước đó là lịch sử, không thu hẹp quyền thực hiện mới nhất; không xin lại phép từng tiểu bước trong 0–1.

## Quyền thực hiện và kết luận khôi phục

Yêu cầu gần nhất là SAFE AUTO RESUME: đọc repo và tài liệu, tạo hồ sơ thiếu, chỉ tiếp tục tiểu bước đã duyệt. Trong lịch sử cuộc trò chuyện, người dùng chỉ duyệt **0A** và agent đã báo PASS cho tài liệu/quy ước. Câu hỏi phân biệt API key Firebase client với Admin private key không phải xác nhận triển khai 0B. Không có tiểu bước code dang dở được duyệt để tiếp tục.

Trạng thái phiên khôi phục: **PASS trong phạm vi khôi phục và hồ sơ**, đã kiểm tra 3 file mới. 0A được kiểm tra lại và vẫn PASS trong phạm vi tài liệu/quy ước; chưa có bằng chứng ứng dụng build hoặc tính năng chạy. Điểm dừng bắt buộc là chờ xác nhận 0B.

## Trước khi thực hiện

- Ba file PROJECT_STATUS, TASKS và SESSION_HANDOFF chưa tồn tại. AGENTS, README, đặc tả và PROJECT_DECISIONS có sẵn và đã được đọc; đặc tả được đọc toàn bộ trước khi tạo file.
- Lịch sử Git local chỉ có một commit; `git ls-files` chỉ liệt kê README. README đã sửa nhưng chưa staged; tài liệu 0A và source Android còn untracked. Các thay đổi này có sẵn, không được tự nhận là phát sinh trong phiên resume.
- `server/` và `deploy/` rỗng. Có Android Java/XML skeleton, Gradle Groovy, Firebase Messaging dependency và JSON cấu hình local.
- Có thư mục build/cache nhưng không dùng sự tồn tại của output để tuyên bố build/test PASS. Chưa kiểm tra remote, Firebase Console, database hoặc hạ tầng bên ngoài.

## Phạm vi và thay đổi của phiên này

Kế hoạch: khôi phục bằng chứng → kiểm tra lại 0A → tạo đúng ba hồ sơ trạng thái → kiểm tra tính toàn vẹn và bàn giao. Không triển khai 0B, 0C, giai đoạn 1 hoặc thay đổi source/build/dependency.

File mới:

- [PROJECT_STATUS.md](PROJECT_STATUS.md): tiến độ, bằng chứng, giới hạn và cổng xác nhận.
- [TASKS.md](TASKS.md): công việc và checklist nghiệm thu phiên khôi phục.
- [SESSION_HANDOFF.md](SESSION_HANDOFF.md): bối cảnh, kiểm tra và hành động tiếp theo.

Không chủ động sửa AGENTS, README, `.gitignore`, đặc tả hoặc quyết định kỹ thuật. Không add/commit/push/merge/deploy, tạo database hay thao tác Firebase Console.

## Kiểm tra đã chạy trong phiên này

| Kiểm tra | Cách thực hiện | Kết quả thực tế |
|---|---|---|
| Git và lịch sử | `git status --short --untracked-files=all`, `git log -6`, log theo path, `git ls-files`, diff/stat và staged diff | Đã đối chiếu; một commit local, chưa staged thay đổi |
| Quy tắc ignore 0A | `git check-ignore --no-index -q` trên 44 trường hợp kỳ vọng | PASS: credentials/env/build/IDE được ignore; source, env mẫu và Wrapper được giữ |
| Đầu ra tài liệu 0A | Đọc 4 file, kiểm tra mẫu credentials, dấu conflict và khoảng trắng cuối dòng | PASS trong phạm vi nội dung được quét; không phải audit mọi file trong repo |
| Liên kết tài liệu 0A | Resolve 4 Markdown link local và `Test-Path` | PASS: 4/4 tồn tại |
| Định danh Android | Kiểm tra source và `app/build.gradle` | PASS tĩnh: package/namespace/applicationId `com.taktak.app`, minSdk 26 |
| Kotlin source/build script | `rg --files android-client server -g '*.kt' -g '*.kts'` | Không tìm thấy file; không có nghĩa dependency transitive không dùng Kotlin |
| Cấu hình Firebase local | Parse JSON local, chỉ kiểm tra package; không in key/project identifier | Có cấu hình khớp `com.taktak.app`; không xác nhận Console/FCM hoặc nghiệm thu 0B |
| Khoảng trắng Git | `git diff --check` | PASS; Git cảnh báo LF→CRLF cho README có sẵn, không phải lỗi kiểm tra |
| Tài liệu sau khi tạo hồ sơ | Quét 7 file tài liệu/ignore, resolve link bằng PowerShell và `Test-Path` | PASS: không phát hiện mẫu credentials, dấu conflict hoặc khoảng trắng cuối dòng; 19/19 link local tồn tại |
| Chính sách Git cho hồ sơ | `git check-ignore --no-index -q` cho 3 file trạng thái | PASS: cả 3 file không bị ignore |
| Bảo toàn workspace | So sánh SHA-256 trước/sau của 62 file có sẵn, ngoài `.git/`, `.gradle/`, `build/` | PASS: không có file có sẵn thay đổi hoặc mất; chỉ thêm đúng 3 hồ sơ trạng thái |

Giới hạn kiểm tra toàn vẹn: không hash Git metadata, Gradle cache hoặc build output. HEAD vẫn là `99b6db4`, staged diff vẫn rỗng khi kiểm tra cuối; không thực hiện thao tác Git ghi trạng thái.

Chưa chạy `assembleDebug`, Gradle unit/instrumented tests, `mvn test`, health/database/migration hoặc kiểm thử thiết bị/mạng/FCM. Lý do: 0A chỉ tài liệu/ignore; chưa được duyệt bước phát triển tiếp, backend chưa có Maven project. Không có log lỗi build/test mới để sửa.

Các phép quét chỉ ghi loại kiểm tra và kết quả, không sao chép API key/token/private key vào hồ sơ. API key trong `google-services.json` là cấu hình client, khác Admin private key; chính sách hiện tại vẫn giữ JSON Android local bằng `.gitignore`.

## Vấn đề còn tồn tại và phụ thuộc

- Cần người dùng xác nhận bắt đầu **0B**. Khi chưa có xác nhận, dừng phát triển sau khi bàn giao hồ sơ.
- Android chưa bật ViewBinding, chưa có MVVM/Repository, URL theo build type hay chính sách cleartext khai báo rõ. Có `activity-ktx` từ skeleton. Ghi nhận để xử lý trong phạm vi 1B được duyệt sau này; chưa thay dependency hoặc quyết định kiến trúc.
- Phiên bản AGP/Gradle/SDK/JDK Android và thư viện chưa được nghiệm thu tương thích. Java 17 cố định cho backend không tự áp đặt phiên bản JDK build Android.
- Không thể kết luận trạng thái GitHub Private/Public, dịch vụ Firebase đã bật hoặc tài nguyên bên ngoài từ repo local. Không tự động tạo lại vì thiếu bằng chứng.
- 0B cần bằng chứng đăng ký app đúng package và phạm vi FCM; 0C cần thử mạng; 1A/1B/1C cần triển khai và build/runtime checks. Chưa coi các bước này là hoàn thành.

## Hướng dẫn phiên tiếp theo

Đọc [AGENTS.md](../AGENTS.md), [đặc tả](TAKTAK_PROJECT.md), [quyết định](PROJECT_DECISIONS.md), [trạng thái](PROJECT_STATUS.md) và [công việc](TASKS.md); kiểm tra Git và yêu cầu mới nhất. Không suy ra quyền thực hiện từ mục đề xuất trong hồ sơ. Nếu vẫn chưa có xác nhận 0B thì không bắt đầu 0B. Mỗi lần chỉ một tiểu bước và dừng trước bước kế tiếp.

## Lần đối chiếu AUTO RESUME tiếp theo — 16/10/2026

- Đã đọc lại AGENTS, ba hồ sơ trạng thái và PROJECT_DECISIONS; đối chiếu Gradle Android, MainActivity, Git status, lịch sử commit và staged diff. Không có xác nhận mới cho 0B trong yêu cầu AUTO RESUME.
- So sánh SHA-256 với snapshot của lần khôi phục trước: 62/62 file có sẵn trong snapshot giữ nguyên, bao gồm đặc tả đã đọc toàn bộ. Phạm vi snapshot không gồm Git metadata, Gradle cache hoặc build output.
- HEAD vẫn là `99b6db4`; staged diff rỗng. Android vẫn là skeleton Java/XML, `server/` và `deploy/` vẫn không có file. Không có công việc dang dở thuộc tiểu bước được duyệt cần tiếp tục.
- `git diff --check` đã chạy lại và PASS. Các kết quả 44 trường hợp ignore, 19 link và quét credentials ở phần trên là của phiên khôi phục trước, không được tính là vừa chạy lại trong lần đối chiếu này.
- Phiên này chỉ bổ sung mục bàn giao này; giữ nguyên source, hồ sơ tiến độ, quyết định kỹ thuật và các thay đổi người dùng có sẵn. Không chạy build/test ứng dụng, không add/commit/push/deploy.
- Kết luận: giữ trạng thái **0A PASS trong phạm vi tài liệu/quy ước**. Đề xuất duy nhất: **0B — kiểm tra và hoàn tất đăng ký Firebase Android `com.taktak.app`, chỉ dùng FCM**. Dừng và chờ người dùng xác nhận trước khi triển khai.

## Phiên 0B — đã được duyệt bằng “continue”, 16/10/2026

UNDERSTAND: đọc toàn bộ đặc tả, quy tắc, hồ sơ và code/config liên quan; Git vẫn `main` / `99b6db4`, nhiều file untracked có sẵn. JSON Android đã tồn tại, không tạo lại project/app hoặc ghi đè JSON. Đối chiếu tài liệu Firebase chính thức cho Android config và FCM.

PLAN: xác minh cấu hình và chính sách Git local → tìm đường truy cập tài khoản Firebase → ghi checklist Console và trạng thái chính xác. Không đổi stack, SDK, package hoặc chuyển sang 0C/1B/6.

IMPLEMENT: thêm [FIREBASE_SETUP.md](FIREBASE_SETUP.md); cập nhật README, PROJECT_STATUS, TASKS và mục bàn giao này. Giữ nguyên AGENTS, PROJECT_DECISIONS, đặc tả, source, Gradle và JSON. Đã tìm plugin Firebase chính thức và đề xuất tích hợp; plugin chưa cài/kết nối, Firebase/GCloud CLI không có trong PATH. Không có lệnh gọi Firebase API được xác thực hoặc thao tác Console nào đã chạy.

VERIFY: chạy 10 kiểm tra PowerShell/Git, tất cả PASS local:

1. JSON parse thành công, không in nội dung cấu hình.
2. Không có `private_key`, `private_key_id` hoặc `type=service_account` trong JSON Android.
3. Configuration version 1, project ID có giá trị và project number là số.
4. Đúng một Android client có package `com.taktak.app` (so sánh phân biệt hoa/thường).
5. Firebase App ID định dạng Android, project number trong App ID khớp metadata.
6. Có client API key không rỗng; không xác minh hiệu lực/restrictions qua mạng.
7. JSON package khớp namespace/applicationId trong Gradle.
8. `git check-ignore --no-index -q` xác nhận JSON bị ignore.
9. `git ls-files` không chứa google-services.json, Admin credential filenames hoặc `.env` tương ứng; đây là kiểm tra tên file trong index, không phải audit bí mật toàn bộ lịch sử.
10. Dependency Firebase khai báo trực tiếp trong Gradle/TOML/POM chỉ có BoM/Messaging; không kết luận về sản phẩm Console hoặc dependency transitive.

REVIEW: chưa chạy build/test app, lấy token hoặc gửi push vì chúng không thuộc 0B. Không có bằng chứng Console, không đánh dấu 0B PASS. Kiểm tra 8 file tài liệu/ignore PASS cho mẫu credentials, dấu conflict và khoảng trắng; 26 link Markdown local hợp lệ; tài liệu FIREBASE_SETUP không bị ignore; `git diff --check` PASS (cảnh báo LF→CRLF cho README). So sánh SHA-256 65 file có sẵn: chỉ README, PROJECT_STATUS, TASKS và SESSION_HANDOFF thay đổi; thêm đúng FIREBASE_SETUP. Source, Gradle và JSON giữ nguyên; không hash Git metadata/cache/build output. HEAD vẫn `99b6db4`, staged diff rỗng. Không coi các kiểm tra local là bằng chứng đăng ký tài nguyên còn tồn tại.

HANDOFF: **BLOCKED — thiếu truy cập/bằng chứng Console**, không phải thiếu phê duyệt 0B. Người dùng có thể cài và kết nối plugin Firebase chính thức để agent đối chiếu, hoặc tự kiểm tra theo FIREBASE_SETUP và xác nhận từng mục; không cần cung cấp secret/API key. Tiếp tục hoàn tất 0B khi có bằng chứng. 0C vẫn chưa được duyệt. Không commit/push/deploy.

## AUTO CONTINUE 0–1 — 16/10/2026

UNDERSTAND/PLAN: đọc toàn bộ đặc tả, hồ sơ và source; giữ các thay đổi chưa commit, ghi nhận quyền mới trong AGENTS. 0B thiếu Console nhưng server 1A độc lập Firebase nên triển khai và kiểm thử trước, sau đó phần Android/network độc lập. Không đánh dấu vượt qua 0B, không làm giai đoạn 2.

IMPLEMENT: server Java 17/Spring Boot 3.5.16/Maven Wrapper 3.9.16; env YAML/placeholder mẫu, health, deny-all security, 8 bảng Flyway và test PostgreSQL thật. Thêm helper chọn Java và smoke server test-scope. Android tận dụng source cũ, bật ViewBinding, Java 17, bổ sung dependency đã khóa, bỏ KTX khai báo trực tiếp, thêm debug network allowlist/release HTTPS gate, tắt backup và FCM auto-init; chưa có auth/camera/widget. Giữ nguyên JSON Firebase và migration V1 sau khi đã chạy.

VERIFY: xem [QA_BOOTSTRAP.md](QA_BOOTSTRAP.md). Server clean verify: 6/6 PASS, không skipped; JAR đóng gói. Android assembleDebug/assembleRelease/unit mẫu/lint debug+release PASS; ba negative-config case bị từ chối đúng lý do. Emulator headless/read-only: ADB đã kết nối sau khi xử lý popup qua console screenshot; install/cold launch, UI dump và screenshot có chữ Taktak. Instrumentation qua reverse: 4/4 PASS, không skipped, gồm health 200 và protected API 401. Đường 10.0.2.2 timeout trên cả wlan0 và eth0; lưu kết quả FAIL riêng, chưa xác định root cause, không chỉnh firewall host. Chưa có máy thật hoặc bằng chứng Firebase Console.

REVIEW: runtime kiểm tra theo skill android_ui_verification, screenshot và UI dump nằm trong `.local/artifacts/` được ignore. Hai warning màu chưa dùng giữ nguyên; không suppress lint. Native/protobuf/Gradle deprecation được ghi nhận; không nâng stack ngoài phạm vi. `.tools/` giữ JDK/Maven/cache local; không đổi Java hệ thống. Chưa commit/push/merge/deploy hoặc thay database của người dùng. Chi tiết file/thử nghiệm và cleanup được xác minh cuối phiên.

HANDOFF: giữ quyền tiếp tục 0–1, dừng trước 2. Hành động cần người dùng: đối chiếu Firebase Console theo FIREBASE_SETUP (hoặc kết nối tài khoản), và kết nối điện thoại thật đã cấp quyền để nghiệm thu phần còn thiếu. Cần kiểm tra mạng alias/tunnel theo [NETWORK_TEST_PLAN.md](NETWORK_TEST_PLAN.md); ADB reverse đã có bằng chứng emulator hoạt động. Không tạo lại các phần đã build/test được, không xin lại quyền bắt đầu 0B/0C/1A–1C.

Kiểm tra cuối AUTO CONTINUE: 48 file source/tài liệu được quét theo mẫu credentials/conflict, 35 link local hợp lệ, 10 case ignore PASS; không có Kotlin source. Manifest debug/release và APK/JAR được kiểm tra đúng policy, migration source khớp bản đã test. So sánh SHA-256 66 file có sẵn: 15 file cập nhật trong phạm vi, 17 file mới, không file có sẵn bị xóa; JSON Firebase không đổi. HEAD `99b6db4`, staged diff rỗng. Smoke server đã kết thúc exit 0, không còn listener 52312; cluster test riêng đã thu hồi, ADB reverse đã gỡ và emulator test đã dừng. Tools/cache/artifact local được giữ lại và ignore. Phần còn lại cần Firebase Console/máy thật/mạng, không phải thiếu quyền chuyển tiểu bước trong 0–1.
