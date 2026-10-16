# Taktak — Bàn giao phiên khôi phục

Ngày: 16/10/2026 (Asia/Saigon). Nhánh khi kiểm tra mới nhất: `main`. HEAD: `96d2311` — `docs: simplify product README`. Các HEAD `99b6db4` trong phần dưới là lịch sử.

**Trạng thái mới nhất:** người dùng duyệt riêng **3A** và skill android_ui_verification, hiện **3A PASS UI local**: Login/Register Java/XML, validation/loading hook; 8 unit/12 emulator test, build/lint debug/release và screenshot PASS sau sửa IME che field. 0–1/2A–2D PASS theo bằng chứng lịch sử. Chưa nối API/phiên Android/avatar; dừng trước **3B**, chờ duyệt riêng. Các phần cũ bên dưới là lịch sử; máy thật cho UI mới, LAN/tunnel, VPS và FCM runtime chưa kiểm thử.

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

## AUTO CONTINUE tiếp theo — sửa kết nối alias, 16/10/2026

UNDERSTAND: đọc đặc tả/quy tắc/quyết định/hồ sơ và code/test; đối chiếu XML test cũ thay vì suy đoán từ file build. Git ban đầu sạch ở `96d2311`, có `a4a2bf5` bootstrap và README đã push theo yêu cầu riêng trước đó. Yêu cầu hiện tại cấm commit/push. Không có điện thoại thật trong `adb devices -l`, Firebase/GCloud CLI không có và không có callable tool Firebase được xác thực.

PLAN: tiếp tục 0C/1C độc lập Firebase, tái hiện alias với backend test mới trên port 8080; xác định nguyên nhân, sửa trong phạm vi cấu hình debug/test, kiểm tra release và cập nhật hồ sơ. Giữ README giới thiệu sản phẩm.

IMPLEMENT: tái hiện timeout trên emulator 37.1.11 / AVD Medium_Phone Android 17. Đối chiếu tài liệu Android chính thức: target SDK 37 yêu cầu runtime local-network permission. Thêm `ACCESS_LOCAL_NETWORK` chỉ vào debug manifest; instrumentation cấp quyền cho app bằng UiAutomation trên API 37+ rồi kiểm tra trạng thái quyền trước request. UI bootstrap chưa gọi mạng, chưa thêm dialog/UI mạng. Khi tính năng sau gọi backend local từ UI phải xin quyền trước và xử lý từ chối; release không khai báo quyền này.

VERIFY: trước sửa 4 test / 1 failure timeout; sau sửa 4/4 PASS, 0 failures/errors/skipped qua URL mặc định `10.0.2.2:8080`, không ADB reverse. Release build, lint debug/release PASS và 0 lint errors. Kiểm tra merged manifests PASS cho debug permission/release isolation. Install Success, cold launch Status ok. Kiểm tra Firebase local/package/ignore/index PASS, không in config hoặc key. Server integration/unit test không chạy lại; bằng chứng cũ được ghi rõ ở QA_BOOTSTRAP.

REVIEW/HANDOFF: thay đổi 2 file Android và 6 hồ sơ (PROJECT_STATUS, TASKS, PROJECT_DECISIONS, NETWORK_TEST_PLAN, QA_BOOTSTRAP, SESSION_HANDOFF). Không sửa server/schema/migration, JSON Firebase, stack hoặc README. Skill android_ui_verification được dùng để kiểm tra screenshot/cấp quyền ADB trên AVD read-only do phiên này tạo. Kết quả alias mới PASS; toàn 0–1 vẫn BLOCKED nghiệm thu Console và máy thật. Cần người dùng xác nhận ba mục Console trong FIREBASE_SETUP và kết nối điện thoại đã cấp USB debugging. Không có phần độc lập còn thiếu để chuyển sang giai đoạn 2; giữ quyền tiếp tục 0–1 khi môi trường sẵn sàng.

Kiểm tra cuối: screenshot Taktak mới đã xem, UI dump xác nhận text; quét 8 file và 25 link local PASS, `git diff --check` PASS, JSON Firebase giữ nguyên hash. Không staged/commit/push; HEAD vẫn `96d2311`. Server smoke exit 0, không còn listener 8080/51141/5554/5555 sau khi đóng đúng emulator do phiên này tạo; test cluster được runner tự thu hồi. Bằng chứng trước/sau và UI nằm trong `.local/artifacts/`, đã ignore. Phần còn lại yêu cầu đối chiếu Console và máy Android vật lý, không thể nghiệm thu bằng source hoặc emulator.

## Nghiệm thu Firebase bằng bằng chứng người dùng — 16/10/2026

Người dùng cung cấp ảnh Project settings/General, vị trí JSON local và Cloud Messaging. Đã chạy đối chiếu PowerShell: Project ID, project number, App ID và đúng một client package `com.taktak.app` đều khớp ảnh. Cloud Messaging API V1 hiển thị Enabled; Legacy Disabled. Sau hướng dẫn kiểm tra các dịch vụ bị loại trừ, người dùng xác nhận “tôi chưa thiết lập” — ghi nhận là xác nhận thủ công chưa thiết lập Auth/Firestore/Storage/Functions. Không trình bày ảnh/xác nhận như truy vấn API quản trị tự động; chưa chứng minh gửi/nhận FCM hoặc restrictions API key.

Kết luận: 0B PASS đăng ký/cấu hình; gỡ phụ thuộc Firebase còn treo của 1B, 1B PASS bootstrap dựa trên build/lint/UI đã chạy trước. Cập nhật FIREBASE_SETUP, PROJECT_STATUS, TASKS, NETWORK_TEST_PLAN, QA_BOOTSTRAP và bàn giao; giữ nguyên thay đổi Android và tài liệu từ phiên trước, không sửa JSON hoặc tạo credentials. Phiên cập nhật này không chạy lại build/test ứng dụng. `adb devices -l` vừa chạy vẫn không có thiết bị. Việc duy nhất cần người dùng để tiếp tục 0C/1C là kết nối điện thoại thật, bật USB debugging và xác nhận Allow; chưa sang giai đoạn 2, không commit/push.

## Hoàn tất máy thật 0C/1C — 16/10/2026

UNDERSTAND/PLAN: người dùng đã kết nối điện thoại và yêu cầu tiếp tục. ADB nhận đúng một máy thật đã cấp quyền, model 23129RAA4G / Android 15 API 35 / 1080×2400; không có package Taktak hoặc reverse mapping trước kiểm tra. Git vẫn có các thay đổi từ phiên trước, bảo toàn toàn bộ. Chọn USB reverse đã ghi trong kế hoạch để kiểm tra app và backend local.

IMPLEMENT/VERIFY: chạy Start-LocalSmokeServer port 8080 với PostgreSQL cluster test mới port 49887; host health 200. Thiết lập đúng reverse tcp:8080 cho máy thật, chạy connectedDebugAndroidTest với health URL 127.0.0.1:8080: **BUILD SUCCESSFUL, 4 test / 0 failures/errors/skipped**. Health 200/UP, API /api/users/me 401, cleartext allowlist và UI/package đều đạt. Test UI đầu mất 103,718 giây; đã thu SIGQUIT của riêng tiến trình Taktak trong lúc chẩn đoán, không sửa code hay cài đặt điện thoại; không có failure, chưa kết luận nguyên nhân chậm. Cài APK debug rồi cold launch riêng: Success/Status ok, 3041 ms. UI dump/screenshot theo android_ui_verification xác nhận foreground đúng MainActivity và chữ Taktak; đã xem screenshot. Không chạy lại server integration/lint/release vì code không đổi trong phiên máy thật, dùng bằng chứng cũ được ghi ngày/phạm vi.

REVIEW/HANDOFF: cập nhật PROJECT_STATUS, TASKS, NETWORK_TEST_PLAN, QA_BOOTSTRAP và SESSION_HANDOFF; không sửa source, migration, JSON Firebase, stack hoặc README trong phiên này. Artifact ignored: android-physical-usb-results.xml, taktak-physical-view.xml, taktak-physical.png tại .local/artifacts. 0C/1C PASS theo kịch bản USB reverse, không tuyên bố LAN/tunnel PASS. Toàn phạm vi bootstrap 0–1 hoàn tất với bằng chứng Firebase/build/DB/emulator/máy thật; chưa có auth, camera, nghiệp vụ hoặc FCM runtime. Dừng trước 2; tiểu bước tiếp theo duy nhất là **2A — đăng ký tài khoản backend**, cần người dùng xác nhận. Không commit/push/merge/deploy.

Kiểm tra cuối phiên máy thật: XML 4/4 hợp lệ; quét 5 hồ sơ cập nhật không phát hiện mẫu key/conflict và 24 link local tồn tại; git diff --check PASS. Ba artifact máy thật được ignore; JSON Firebase giữ nguyên hash. Reverse tcp:8080 đã gỡ, server smoke exit 0, không còn listener HTTP 8080 hoặc PostgreSQL test 49887. Giữ app debug trên điện thoại theo mục đích kiểm tra, không xóa dữ liệu/cài đặt cá nhân. HEAD vẫn 96d2311, index rỗng; bảo toàn mọi thay đổi từ các phiên trước.

## 2A — đăng ký tài khoản backend, 16/10/2026

UNDERSTAND: “continue” sau đề xuất 2A là phê duyệt riêng tiểu bước này, không phải AUTO CONTINUE cả giai đoạn 2. Đọc toàn bộ đặc tả, quy tắc, quyết định và source/schema/test. Working tree đã có thay đổi Android/Firebase/hồ sơ từ các phiên trước; bảo toàn. V1 có đủ bảng users/unique constraints, không cần migration mới hoặc sửa migration đã chạy.

PLAN/IMPLEMENT: User/JpaRepository, RegisterRequest/Response, AuthService transaction, AuthController; SecurityConfig thêm BCrypt cost 12 và mở riêng POST register. Thêm ApiError/ApiExceptionHandler cho JSON 400/409/415, không phản chiếu body hoặc SQL. Giữ password nguyên bản, giới hạn Unicode/72 byte UTF-8, chống ghi dữ liệu entity từ client; DB giải quyết duplicate/race. Không token/login/refresh/auth Android. application.yml chỉ chỉnh comment mô tả phạm vi JWT còn chưa dùng.

VERIFY: Maven Wrapper/JDK 17 verify đã chạy hai lần, cả hai BUILD SUCCESS; lần cuối 13 test, 0 failures/errors/skipped, executable JAR. Sáu bootstrap test vẫn PASS khi thêm entity; bảy test đăng ký mới gồm success/normalization/BCrypt/DTO, duplicate tuần tự, race username, race email, validation không tạo row, password 72 byte/salt, malformed JSON/content-type/route đóng. PostgreSQL test cluster mới/port ngẫu nhiên, không dùng DB cá nhân. BCrypt hash có cost 12, xác minh matches; DTO không hash/password/token; hai race mỗi lần 201+409 và đúng một row. Guard độ dài password trong bytecode đã được đối chiếu sau build. Lỗi SQLState 23505 trong log là duplicate mong đợi từ test, không phải test bị bỏ qua; không log password/body. Không chạy lại Android vì source Android không đổi trong phiên 2A.

HANDOFF: thêm 8 Java production file dưới auth/user/common và REGISTRATION_API; sửa SecurityConfig/ServerBootstrapTest/comment application.yml, cập nhật PROJECT_STATUS/TASKS/PROJECT_DECISIONS/SESSION_HANDOFF. Không đổi POM/dependency/schema, JSON Firebase, README hoặc source Android. Hướng dẫn Windows và thử HTTP nằm trong [REGISTRATION_API.md](REGISTRATION_API.md); lệnh thủ công trong đó chưa được tính là đã chạy. 2A PASS, toàn giai đoạn 2 chưa DONE; không có blocker cho 2A. Đề xuất duy nhất **2B — login/JWT access/refresh rotation/logout và giới hạn thử đăng nhập**, chờ xác nhận. Không commit/push/deploy.

Review cuối 2A: quét 19 file server/tài liệu đang thay đổi hoặc mới (có cả tài liệu từ phiên trước), không phát hiện mẫu private key/API key/conflict, 33 link local hợp lệ; git diff --check PASS. XML test bản cuối xác nhận 13/13, production JAR có AuthController và không có test harness/PostgreSQL nhúng. V1/POM/README không đổi so với Git, hash JSON Firebase giữ nguyên. Không còn listener ở hai port PostgreSQL test 61479/57990; các cluster test đã được teardown. HEAD vẫn 96d2311, index rỗng.

## Đối chiếu yêu cầu AUTO CONTINUE 0–1 mới — 16/10/2026

Yêu cầu mới lặp lại quyền AUTO CONTINUE chỉ trong Bước 0/giai đoạn 1; không phê duyệt 2B. Đã đối chiếu tài liệu, source SecurityConfig/AuthController/AuthService, debug manifest/instrumentation, Git status/log/index và ba XML báo cáo hiện có. HEAD main/96d2311, index rỗng; các thay đổi 0–1/2A chưa commit được giữ nguyên. AGENTS/đặc tả/V1 không có diff so với Git.

Xác nhận từ báo cáo đã chạy ở các phiên trước: backend 13/13, emulator alias 4/4, máy thật USB reverse 4/4, tất cả 0 failures/errors/skipped. Phiên đối chiếu này chỉ parse/kiểm tra báo cáo và chạy git diff --check (PASS), không chạy lại build/test hoặc coi các test cũ là vừa chạy. Không phát hiện tiểu bước bootstrap 0–1 còn dang dở cần triển khai; LAN/tunnel vẫn chưa kiểm thử, không suy ra PASS cho các đường này.

Kết luận: phạm vi AUTO CONTINUE 0–1 đã hoàn tất; 2A đã hoàn tất theo phê duyệt riêng trước đó. Phiên này chỉ bổ sung mục bàn giao, không sửa source/quyết định kỹ thuật, không commit/push/deploy. Đề xuất duy nhất vẫn là 2B — login/JWT/refresh rotation/logout và giới hạn thử đăng nhập; cần người dùng cho phép bước này, không phải bổ sung tài khoản hay thiết bị để tiếp tục bootstrap.

## 2B — login/JWT/refresh/logout, 16/10/2026

UNDERSTAND/PLAN: người dùng duyệt rõ 2B. Đối chiếu đặc tả 2B/API/security, trạng thái và source 2A/schema V1; bảo toàn thay đổi Android/Firebase/docs có sẵn. Phạm vi login, JWT access, refresh token rotation/hash-only, logout và rate limit; không làm 2C hoặc chuyển phase Android. Sử dụng bảng refresh_tokens có sẵn và thư viện JWT Spring thay vì tự viết.

IMPLEMENT: thêm starter OAuth2 Resource Server do Boot quản lý; TokenConfiguration/TokenService/SessionService/refresh entity-repository và DTO, AuthFailure/LoginRateLimiter, SecurityErrorHandler. Auth chain riêng bỏ bearer filter ở public auth endpoints; protected chain xác minh JWT. Secret Base64 >=32 byte bắt buộc, default access15m/refresh30d, issuer/audience/exp/iat/sub kiểm tra nghiêm; token opaque random/hash SHA-256, pessimistic lock và transaction rotation. Logout idempotent, quota fixed window IP/account bounded trong JVM, no-store token responses. Thêm xử lý JSON 401/403/429/500 an toàn; validation message dùng chung cho auth. LocalSmokeServer/bootstrap test dùng key tạm sinh ngẫu nhiên chỉ test scope; không production fallback.

VERIFY: lần đầu 27 test có 2 failure/1 error do issuer string không chuyển được URL trong accessor và probe test chưa đăng ký; sửa issuer logic HTTPS và đăng ký probe chỉ test, không nới expectation. Lần sau 27/27 PASS. Review bổ sung quota concurrency, cấu hình lifetime, alg none dùng claims hợp lệ và tách cấu hình test khỏi env; bản cuối **Maven verify BUILD SUCCESS, 29 test, 0 failures/errors/skipped** (12 auth integration, 13 bootstrap/registration, 3 config, 1 quota race). HTTP thật/PostgreSQL test mới: expired access ở đúng 15 phút vẫn refresh được khi gửi expired bearer; replay/race chỉ một rotation; fault injection encoder kiểm tra rollback; logout chọn phiên; JWT giả/key/claims/alg sai bị từ chối; account/IP quota + forged forwarding header và quota đồng thời đạt. Fault-injection ERROR log là tình huống mong đợi, không skip test.

Smoke runner ngoài profile test đã chạy register201/login200/refresh200/replay401/logout204/revoked-refresh401 bằng PowerShell HTTP; không in token/password/key. Runner exit 0 và cluster test riêng được thu hồi. JAR có OAuth2 Security6.5.11/Nimbus9.37.4, không chứa test probe/controller/clock/encoder/harness/PostgreSQL. Chưa chạy lại Android vì source không đổi trong 2B. Chưa chạy curl riêng hoặc triển khai server thật; manual command docs không tính là test đã chạy.

HANDOFF: thay đổi backend auth/config/common, UserRepository, POM/env/comment YAML, bootstrap/smoke tests và thêm 3 test class; thêm AUTH_API, cập nhật REGISTRATION_API/quyết định/trạng thái/TASKS/README/bàn giao. README chỉ bổ sung JWT env cần thiết, giữ dạng giới thiệu sản phẩm. Không sửa migration, JSON Firebase, Android source, không commit/push/deploy. Access JWT không bị logout vô hiệu trước exp; refresh logout phải dùng token hiện tại, chưa family/denylist; quota single JVM reset khi restart và cần trusted proxy khi lên VPS. Các giới hạn được ghi rõ trong AUTH_API, không đánh dấu toàn phase2 DONE. **2B PASS**, không blocker trong phạm vi này; đề xuất duy nhất **2C — GET/PUT hồ sơ cá nhân**, chờ xác nhận.

Review cuối 2B: đồng bộ query email login với lower(email) của unique index; chạy lại toàn suite sau thay đổi, bản cuối 29/29 PASS và JAR build. Quét 39 file backend/tài liệu đang thay đổi hoặc mới (gồm phần có sẵn từ các phiên trước), không phát hiện mẫu key/conflict; 44 link local hợp lệ. Script kiểm tra link ban đầu không xử lý parent rỗng của README gốc; đã sửa harness sang đường dẫn '.'/ErrorAction Stop và chạy lại thành công, không tính lần lỗi là PASS. git diff --check PASS, V1 giữ nguyên và hash JSON Firebase không đổi; báo cáo generated được ignore. Smoke server đã exit 0, không còn listener 8080/63421. HEAD vẫn 96d2311/index rỗng, mọi thay đổi chưa commit được giữ nguyên.

## 2C — GET/PUT hồ sơ cá nhân, 16/10/2026

UNDERSTAND: người dùng duyệt riêng “2C — GET/PUT”; không mở quyền 2D hoặc Android auth. Đọc toàn bộ đặc tả, AGENTS, quyết định, trạng thái/công việc/bàn giao và source entity/repository/auth/security/test. Git main/96d2311, đã có nhiều thay đổi chưa commit từ bootstrap/2A/2B; giữ nguyên. Schema V1 đã có display_name nên không cần migration.

PLAN: thêm DTO/controller/service cho chính chủ; tái sử dụng JWT filter chain và lỗi chung; kiểm thử HTTP/PostgreSQL cho persistence, validation, dữ liệu an toàn và cô lập A/B. Dùng skill api-endpoint-builder cho checklist validation/phân quyền/error/docs, giữ nguyên stack và format API hiện có, không dùng ví dụ Express hoặc thay response envelope.

IMPLEMENT: GET/PUT `/api/users/me` chỉ lấy UUID từ JWT subject đã xác minh. UserService transaction chỉ đổi displayName qua method hẹp trên entity; read-only cho GET. DTO trả đúng id/email/username/displayName/createdAt, no-store; request strip/nonblank/max 255 UTF-16 như đăng ký. Thuộc tính ngoài DTO bị bỏ qua, không mass assignment; query ID không chọn user khác. JWT có sub không tồn tại trả 401 AuthFailure chung, không tạo user. Chưa avatar/search/UI; không sửa SecurityConfig, POM, env hoặc migration trong 2C.

File tạo mới:

- `server/src/main/java/com/taktak/server/user/UpdateProfileRequest.java`
- `server/src/main/java/com/taktak/server/user/UserProfileResponse.java`
- `server/src/main/java/com/taktak/server/user/UserService.java`
- `server/src/main/java/com/taktak/server/user/UserController.java`
- [PROFILE_API.md](PROFILE_API.md)

File bổ sung/chỉnh sửa (không ghi đè các thay đổi có sẵn): `server/src/main/java/com/taktak/server/user/User.java`, `server/src/test/java/com/taktak/server/AuthenticationIntegrationTest.java`, [AUTH_API.md](AUTH_API.md), [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md), [PROJECT_STATUS.md](PROJECT_STATUS.md), [TASKS.md](TASKS.md) và file bàn giao này. Tổng 12 file trong phiên 2C; User.java/test auth đã là untracked từ phiên trước, không nhận là file mới của phiên này.

VERIFY: Maven Wrapper với JDK 17/helper và cache `.tools/maven-repository`, `verify` chạy hai lần đều BUILD SUCCESS; bản cuối **36 test / 0 failures / 0 errors / 0 skipped**, executable JAR. 19 auth/profile integration (12 cũ + 7 mới), 13 bootstrap/register, 3 config, 1 quota concurrency. Test mới GET safe DTO; PUT Unicode/persistence/idempotency; giả ID/query/mass assignment A/B; invalid bodies không đổi DB; biên tên 255/256; thiếu/sai/hết hạn JWT, refresh dùng nhầm và JWT giả chữ ký; JWT signed subject không tồn tại. Review siết test route UUID phải trả 4xx, không chấp nhận 500, rồi chạy lại toàn bộ suite đạt. Lỗi encoder giả lập và SQL duplicate trong regression là tình huống test mong đợi.

Smoke HTTP ngoài profile test qua loopback đã chạy với tài khoản/key ngẫu nhiên và PostgreSQL cluster riêng: register201 → login200 → GET200 → PUT200 → GET lại200 → refresh200 → GET bằng access mới200 → logout204. Kiểm tra tên thực sự được lưu, email gửi thêm không đổi, DTO đúng năm trường. Không in token/password/key; runner exit 0, cluster được runner thu hồi, không còn listener 8080. Không chạy curl riêng, không dùng database cá nhân; hướng dẫn thủ công Windows tại PROFILE_API không tính thay cho các phép thử đã chạy.

REVIEW/HANDOFF: XML xác nhận 36/36, JAR có UserController và không test probe/harness/embedded PostgreSQL. Quét 44 file backend/tài liệu đang thay đổi hoặc mới (gồm thay đổi cũ) không phát hiện mẫu API key/private key/conflict; 49 link local hợp lệ, git diff --check PASS. Đây là quét mẫu có phạm vi, không tuyên bố audit toàn bộ secret. V1 không có diff, JSON Firebase giữ nguyên hash bằng chứng cũ; không sửa README hoặc source Android trong 2C. Android test/VPS/HTTPS/UI hồ sơ chưa chạy; không nghiệm thu ngoài backend. Không staged/commit/push/merge/deploy.

**2C PASS**, không blocker trong phạm vi. Server smoke đã đóng, không để dev server chạy nền. Không cần thao tác thủ công để hoàn tất 2C; muốn chạy server dev bền vững thì tự đặt DB/JWT env theo AUTH_API, không gửi secret vào chat. Đề xuất duy nhất: **2D — integration test và curl cho auth/hồ sơ, chốt hợp đồng lỗi trước client**; cần người dùng duyệt riêng. Không tự đánh dấu toàn giai đoạn 2 DONE hoặc chuyển bước.

## 2D — nghiệm thu auth/hồ sơ và hợp đồng lỗi, 16/10/2026

UNDERSTAND: người dùng duyệt riêng “2D”. Đối chiếu AGENTS/đặc tả/quyết định/trạng thái/công việc/bàn giao và source auth/profile/security/common/test, Git main/96d2311 với thay đổi chưa commit 0–1/2A–2C được giữ nguyên. Mốc trước có 36 test, chưa curl thật; không suy ra 2D đã hoàn tất từ output cũ. Windows có curl 8.16.0; JDK 17/Maven portable/cache có sẵn. Không mở quyền 3A/Android hoặc commit/push/deploy.

PLAN: tái sử dụng integration harness PostgreSQL/JPA/HTTP hiện có, thêm curl lifecycle và test error contract; chốt format lỗi trước client, sửa nguyên nhân nếu phát hiện lỗi. Dùng skill api-endpoint-builder để rà validation/authorization/error/docs; giữ envelope và stack dự án, không tạo framework mới hoặc auth giả.

IMPLEMENT: thêm ba testcase trong AuthenticationIntegrationTest (curl lifecycle, routing/error contract, unexpected MVC failure), siết helper kiểm tra Content-Type/no-store/no-cookie/path/timestamp ngoài 5 trường JSON/status/code/message có sẵn. Curl thực thi bằng ProcessBuilder, argv chỉ executable/--disable/--config/-, headers/JSON qua UTF-8 stdin, stdout chỉ giữ trong bộ nhớ; không file credential, proxy/curlrc/redirect/verbose, có timeout. Test cấp JWT hợp lệ rồi tiến Clock riêng 15 phút để curl chứng minh expiry; JWT giả chỉ đổi chữ ký, không làm giả expiry. Kiểm tra username duplicate bằng curl và DB count, còn race email/username được regression PostgreSQL giữ lại.

Test routing đầu chạy riêng **FAIL** (1 test/1 failure, expected404 nhưng403), chứng minh `/error` bị security chặn đã che lỗi MVC. Sửa ApiExceptionHandler xử lý trực tiếp 404/405 (Allow), 406 JSON và generic 500 an toàn; không mở error dispatcher hoặc sửa filter chain. MVC dùng cùng Clock với security, path không có query, không phản chiếu exception/body/SQL. Generic failure test controller chỉ thuộc test source/profile; không thêm endpoint thử nghiệm vào production. Giữ nguyên nghiệp vụ 2A–2C, refresh/logout/rate policy và schema.

File mới: `scripts/Test-AuthApi.ps1`, [API_ERRORS.md](API_ERRORS.md), [QA_AUTH.md](QA_AUTH.md).

File bổ sung/chỉnh sửa: `server/src/main/java/com/taktak/server/common/ApiExceptionHandler.java`, `server/src/test/java/com/taktak/server/AuthenticationIntegrationTest.java`, [AUTH_API.md](AUTH_API.md), [PROFILE_API.md](PROFILE_API.md), [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md), [PROJECT_STATUS.md](PROJECT_STATUS.md), [TASKS.md](TASKS.md), file bàn giao này. Tổng 11 file trong 2D; không nhận file untracked có sẵn là file mới của phiên.

VERIFY: chạy `powershell.exe -NoProfile -ExecutionPolicy Bypass -File scripts/Test-AuthApi.ps1` sau sửa: **39/39 PASS, BUILD SUCCESS/JAR**. Chạy chế độ `-CurlOnly`: **1/1 PASS**. Review thêm kiểm tra Accept text/html phải 406 JSON, rồi chạy lại script đầy đủ: **39 test, 0 failures/errors/skipped, BUILD SUCCESS** (bản cuối 13:59:23 +07). Gồm 22 auth/profile/curl integration, 13 bootstrap/register, 3 token config, 1 quota concurrency. Curl thực tế register201, duplicate409, missingJWT401, wrongLogin401, login200, me200, PUT200, forged401, expired401, refresh200 khi kèm expired bearer, replay401, me mới200, logout204, revoked refresh401; DB active refresh count0. Lỗi SQL duplicate và fault injection encoder/generic failure trong log là tình huống mong đợi được assert, không skip.

REVIEW: parse XML xác nhận tổng39 và testcase curl PASS, production JAR có ApiExceptionHandler và không chứa test probe/controller/Clock/encoder/harness/PostgreSQL nhúng. Báo cáo/JAR được ignore. Cú pháp PowerShell PASS, git diff --check PASS; quét 47 file server/docs/script đang thay đổi hoặc mới không phát hiện mẫu API key/private key/conflict, 69 link local tồn tại (gồm file cũ, không phải audit toàn repo). V1 không có diff; JSON Firebase hash giữ nguyên; index rỗng/HEAD96d2311. Các port PostgreSQL test đã quan sát 56919/57135/57656/49598 không còn listener; harness đóng context/cluster, không để server dev chạy. Không dùng/reset DB cá nhân hoặc sửa migration, POM, env, README, source Android/Firebase trong 2D.

HANDOFF: **2D PASS local**, giai đoạn2 hoàn tất backend auth/hồ sơ trong phạm vi đã được duyệt; chưa Android auth/avatar/media/VPS/TLS/FCM runtime. Chưa chạy lại Android vì không sửa source Android. Không có blocker/thao tác thủ công bắt buộc; muốn chạy lại dùng Test-AuthApi.ps1, không cần gửi tài khoản hoặc secret. Hợp đồng lỗi và ma trận nghiệm thu tại API_ERRORS/QA_AUTH. Đề xuất duy nhất **3A — XML LoginActivity/RegisterActivity, validation và loading bằng ViewBinding/Material**; dừng chờ phê duyệt, không triển khai hoặc commit/push/merge/deploy.

## 3A — Login/Register Java/XML, 16/10/2026

UNDERSTAND: người dùng duyệt riêng 3A và chỉ định skill android_ui_verification; đã đọc đầy đủ skill, đặc tả và quy tắc, đối chiếu hồ sơ/Git/source Android/build/test. Git vẫn main/96d2311; mọi thay đổi bootstrap/2A–2D có sẵn được bảo toàn. Backend mốc 2D 39 test PASS nhưng UI chỉ placeholder; chưa có thiết bị ADB khi bắt đầu. Không mở quyền 3B hoặc tự triển khai API/token/Camera.

PLAN: hai form Java/XML/ViewBinding/Material, validation theo giới hạn backend và loading hook; thêm lối mở từ launcher nhưng không đổi launcher/session route. Build/unit/lint, giữ bốn instrumentation bootstrap/mạng cũ, thêm test UI và kiểm tra screenshot/IME/light-dark theo skill. Dùng AVD Medium_Phone read-only sẵn có, không tạo lại AVD hoặc chạm điện thoại/dữ liệu người dùng.

IMPLEMENT: LoginActivity/RegisterActivity, AuthFormActivity chia sẻ insets/validation display/loading, AuthInputValidator; layout/style/string Material DayNight. Email precheck bằng Android Patterns, username/displayName/password theo giới hạn backend, password không trim/cắt, confirmation khớp nguyên văn. Password/confirmation saveEnabled=false, không đưa vào Intent/Bundle/log/token storage. Submit hợp lệ chỉ chạy một UI post để kết thúc loading và báo rõ chưa nối server; không fake auth thành công hoặc delay mạng giả. Loading renderer khóa nút/inputs và guard submit lặp; test kiểm tra trực tiếp renderer, không nhận là request backend. Auth activities không exported, MainActivity chỉ thêm hai nút điều hướng. Repository/ViewModel/network thực tế thuộc 3B, không tạo class rỗng trong 3A.

File mới (9 source/test + 1 tài liệu):

- `android-client/app/src/main/java/com/taktak/app/ui/auth/AuthInputValidator.java`
- `android-client/app/src/main/java/com/taktak/app/ui/auth/AuthFormActivity.java`
- `android-client/app/src/main/java/com/taktak/app/ui/auth/LoginActivity.java`
- `android-client/app/src/main/java/com/taktak/app/ui/auth/RegisterActivity.java`
- `android-client/app/src/main/res/layout/activity_login.xml`
- `android-client/app/src/main/res/layout/activity_register.xml`
- `android-client/app/src/main/res/values/auth_styles.xml`
- `android-client/app/src/test/java/com/taktak/app/ui/auth/AuthInputValidatorTest.java`
- `android-client/app/src/androidTest/java/com/taktak/app/AuthUiTest.java`
- [QA_AUTH_UI.md](QA_AUTH_UI.md)

File chỉnh sửa: `android-client/app/src/main/AndroidManifest.xml`, `android-client/app/src/main/java/com/taktak/app/MainActivity.java`, `android-client/app/src/main/res/layout/activity_main.xml`, `android-client/app/src/main/res/values/strings.xml`, [PROJECT_DECISIONS.md](PROJECT_DECISIONS.md), [PROJECT_STATUS.md](PROJECT_STATUS.md), [TASKS.md](TASKS.md), file bàn giao này. Tổng 18 file trong 3A. Không sửa hai file Android debug/instrumentation đang dirty từ phiên trước, README, server, migration, Firebase JSON, POM/Gradle/version/dependency hoặc cấu hình Java toàn máy.

VERIFY — lỗi và nguyên nhân đã xử lý:

1. Build đầu/đổi -P vẫn FAIL: daemon criteria chọn VS Code JRE25.0.4.1 thiếu jlink. Dùng system properties -Dorg.gradle.java.installations.paths trỏ JBR Android Studio đầy đủ và -Dorg.gradle.java.installations.auto-detect=false, --no-daemon; không sửa stack/SDK hoặc daemon criteria file. Căn cứ tài liệu Gradle được link trong QA_AUTH_UI.
2. AAPT FAIL vì parent Widget.Material3.TextView không tồn tại; sửa sang platform TextView với Material typography. Lint FAIL vì accessibilityHeading XML cần API28; dùng ViewCompat giữ min26, không suppress/baseline/hạ điều kiện.
3. Bản kế tiếp debug build/lint/unit PASS và 11 instrumentation PASS. Skill screenshot phát hiện confirmation field bị keyboard che dù test cũ đạt. Sửa FrameLayout nhận insets ngoài, NestedScrollView viewport nhỏ đúng IME, request focused rectangle; thêm test IME. Chạy lại đầy đủ: **BUILD SUCCESSFUL, 8/8 unit + 12/12 instrumentation, 0 failures/errors/skipped**. Tám UI test mới gồm điều hướng/back, required/email/username/confirmation, submit không giả auth, loading/duplicate guards, recreate không giữ password, dark scroll, IME; bốn regression bootstrap/mạng giữ nguyên.
4. Release assemble/lint riêng **BUILD SUCCESSFUL** với HTTPS placeholder https://api.example.invalid/, không signing/phát hành. Lint debug/release cuối **0 errors, 2 warning UnusedResources** black/white có sẵn; toolchain/protobuf deprecation vẫn được báo. Một harness đếm SARIF level ban đầu báo warning=0 vì level dùng mặc định; đã đối chiếu hai rule UnusedResources và text report, không dùng số 0 đó làm kết quả cuối.

Skill verification: AVD Android17/API37 đo wm size 1080×2400 trước tap, UI dump tìm đúng resource-id/bounds, chờ animation và screencap/pull/view_image. Đã xem login, required errors, register light/dark, IME trước/sau sửa và login bản cuối; focused confirmation sau sửa bounds [63,1281][1017,1428], nằm trên keyboard. Không ghi password thật vào screenshot. Khi đổi theme, kiểm tra trang login không khớp đã dừng fail-fast, đọc lại bounds và xác minh lại đúng trang trước chụp. Native logcat TestRunner/AndroidRuntime không có crash test; XML cuối xác nhận 12 test (AuthUiTest8 + bootstrap4), unit XML8 (validator7 + cũ1). Artifact ignored tại .local/artifacts/3a theo QA_AUTH_UI, gồm XML kết quả copy và screenshot trước/sau.

REVIEW/CLEANUP: manifest release chặn cleartext, không ACCESS_LOCAL_NETWORK debug và auth activity không exported. Install debug Success, cold launch MainActivity Status ok (2393ms lần cài lại cuối); instrumentation uninstall APK nên lần am-start ngay sau test báo activity missing, đã install lại APK trước kiểm tra UI, không coi lần lỗi là PASS. Backend smoke test mới health200/protected401 từ emulator qua alias 10.0.2.2, không reverse; không chạy lại toàn 39 backend test vì source không đổi. Smoke server/cluster test riêng port59155 đã đóng exit0, không reset DB người dùng. Emulator console kill không hỗ trợ trong lần này; đã xác minh PID18640 có command line Medium_Phone -read-only rồi đóng đúng tiến trình do phiên tạo, không xóa AVD/files hoặc dừng thiết bị khác; theme night test đã trả no trước cleanup.

Review repo: quét 28 file Android/docs mới hoặc dirty (gồm phần có sẵn), không mẫu API key/private key/conflict; XML hợp lệ, 72 link local tồn tại; git diff --check PASS, index rỗng/HEAD96d2311. Không có Kotlin source. JSON Firebase giữ nguyên SHA256 bằng chứng cũ; artifact được ignore. Đây là quét mẫu phạm vi, không phải audit tất cả secret. Không commit/push/merge/deploy.

HANDOFF: **3A PASS UI local**, chưa Android auth đầu cuối/3B hoặc routing Camera/3C. Máy thật UI mới, API26, landscape/font scale chưa chạy, không lấy bằng chứng bootstrap cũ để tuyên bố các mục này đạt. Không có blocker/thao tác thủ công bắt buộc để hoàn tất 3A. Đề xuất duy nhất **3B — AuthApi/Repository/AuthViewModel/ApiClient, phiên mã hóa và refresh một lần có đồng bộ**; dừng chờ duyệt riêng.
