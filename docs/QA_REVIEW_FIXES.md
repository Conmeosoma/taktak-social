# Sửa lỗi quan trọng sau review 5C–8

> Cập nhật tiếp theo cùng ngày: Hồ sơ/avatar đã triển khai và Firebase cloud/widget đã test trên AVD tại [QA_PROFILE_AVATAR_FCM.md](QA_PROFILE_AVATAR_FCM.md). Những giới hạn Profile/FCM trong báo cáo review này là trạng thái lịch sử; điện thoại/launcher OEM và production vẫn chưa nghiệm thu.

Ngày: 18/10/2026, Asia/Saigon. Phạm vi được duyệt: sửa lỗi đã tìm thấy, không thêm tính năng hoặc tự chuyển roadmap. Bắt đầu từ `6a7f51e`, nhánh `chore/push-current-project`, tracked worktree sạch. Không commit/push/deploy, không tác động DB/VPS thật.

## Những gì đã sửa

| Mã review | Nguyên nhân | Sửa chữa và bằng chứng |
|---|---|---|
| R1 | Xóa JPEG trước commit, rollback để lại row active nhưng mất ảnh | `MomentService` discard sau commit. Integration test với PostgreSQL thật chứng minh rollback giữ ảnh/GET 200, commit mới dọn file/GET 404. |
| R2 | Viewer/gallery dùng cache sau mọi lỗi mạng/API, kể cả mất quyền | Viewer luôn GET có JWT khi vào foreground và khi Save; không fallback cache khi download thất bại. Ẩn ảnh, caption, identity và khóa nút khi bị thu hồi; nghe local deletion/session events. Cache/history publication có generation guard. Instrumentation mới kiểm tra 403/404, xóa ảnh đang mở, logout và Save sau khi server xóa ảnh. |
| R3 | Restore SQL không atomic, sai PGPASSWORD ở pipeline, xóa ảnh live sớm | Custom dump, `pg_restore --clean --if-exists --exit-on-error --single-transaction`, credentials truyền đúng process. Xác minh archive/manifest/type/path trước; mandatory pre-restore backup, giữ ảnh cũ và rollback DB/ảnh khi lỗi. Fixture contract tests; chưa chạy restore trên PostgreSQL/Linux thật. |
| R4 | Bật config TLS khi certificate chưa tồn tại | HTTP-only ACME webroot trước, certonly sau, rồi mới TLS + nginx test/reload; deploy hook cho renew. Kiểm tra thứ tự bằng mock, chưa ACME thật. |
| R5 | Compose interpolation sai, nhầm container env_file với CLI env | Dùng required `${DB_PASSWORD:?...}` / `${JWT_ACCESS_SECRET:?...}`; hướng dẫn `--env-file ../.env` và `config --quiet`. Chưa có Docker CLI/engine để xác minh Compose thực. |
| R6 | Production sender giữ vô hạn message/token và log raw token | Gỡ recorder khỏi main source, chuyển sang test-only `RecordingPushSender`, log chỉ generic error code. Test reflection không có Collection recorder, 1000 disabled sends không có side effect; không gọi Firebase thật. |
| R7 | Worker phiên cũ có thể render ảnh sau logout/new login | Publication đồng bộ cùng monitor với clear/login; guard cả empty/image/logged-out. Logout/refresh invalidation clear cache, cancel sync và render logged-out. Latch race test chứng minh logout renderer luôn chạy sau in-flight render cũ. |
| R8 | Firebase đọc property sai, bỏ qua enabled, sai credentials vẫn startup | Đồng bộ `taktak.fcm.*`, conditional enabled, fail-fast nếu thiếu/sai credentials khi bật; stream đóng và FirebaseApp delete khi shutdown. 3 context config tests. |
| R9 | Update bỏ qua backup failure, ghi đè live JAR, restart failure không rollback | Mandatory backup, same-filesystem candidate/rename, exit trap rollback cả lỗi start và health. Fixture test backup failure/start failure/health failure/success + manual JAR rollback. Không tự đảo migration hoặc reset DB. |
| R10 | Callback getToken tới sau ViewModel clear gọi executor đã shutdown | Kiểm tra cleared và generation trước submit/registration, bắt RejectedExecutionException. Build và session regressions; chưa ép callback FCM thật tới muộn trên thiết bị. |
| R11 | Quota IP thấy loopback sau Nginx, không tách user | Nginx ghi đè XFF bằng remote_addr; Tomcat native chỉ tin loopback mặc định. 2 HTTP/Tomcat tests với prod YAML: trusted proxy trả IP client, untrusted peer không spoof được. Chưa nghiệm thu toàn chuỗi Nginx từ 2 IP bên ngoài. |
| R12 | Hồ sơ ghi 100%/production-ready không đúng bằng chứng | Rút lại kết luận toàn diện; cập nhật status/tasks/handoff và mã MOMENT-02/03 đúng đặc tả. Profile vẫn placeholder, không tự triển khai. |

Stack/package, API contracts, JWT rotation, database schema và migrations giữ nguyên. Android vẫn Java/XML/ViewBinding, widget vẫn RemoteViews. Không nâng dependency. Policy HTTP debug/HTTPS release không đổi.

## Kết quả thực tế

- Backend `mvnw ... verify`: **130 tests, 0 failures/errors/skips**, runnable JAR build thành công. Log `.local/artifacts/fixes-20261018/server-final-retest.log`. Lượt đầu fixture Tomcat thiếu auto-customizer nên 1 ca fail; đã sửa fixture như ứng dụng thật, chạy lại riêng 2 ca và toàn suite đạt, không xóa test.
- Android `assembleDebug assembleDebugAndroidTest testDebugUnitTest --rerun lintDebug assembleRelease lintRelease`: **BUILD SUCCESS**, **69 JVM tests, 0 failures/errors/skips**. Log `android-final.log`; test APK mới bổ sung ở `android-test-apk-final.log`. Release URL kiểm thử là HTTPS placeholder, chưa ký hoặc cài release production.
- Connected: **22 ca khác nhau đã có kết quả đạt qua các batch**, gồm 6 privacy mới, 12 DeviceAcceptance, 2 HistoryAcceptance, 2 SessionRouting. Thiết bị test `Medium_Phone`, API 37, 1080×2400, ADB reverse tới smoke server/cluster PostgreSQL/ảnh fixture mới; lúc bắt đầu chưa có điện thoại USB; thiết bị thật xuất hiện khi kiểm tra cuối, chưa được cài/chạy test hay tác động phiên. Lượt gộp 22 ca có 7 lỗi setup login; chạy lại đủ bảy ca trong batch nhỏ đạt (`android-connected-retest.log`), riêng 6 privacy đạt (`privacy-final.log`). Không gọi lượt gộp thất bại là PASS. Probe cùng smoke server xác minh 20 request login sai/distinct account trả 401, 2 request tiếp theo trả 429 trong cửa sổ 60 giây (`login-quota-probe.log`); giữ nguyên quota, chia batch thay vì nới bảo mật. Hai HistoryAcceptance chạy lại trên APK cuối sau guard bổ sung tại điểm bắt đầu tải cũng **PASS 2/2** (`history-final.log`).
- `bash -n` chạy riêng **10 file shell** (8 production/helper + 2 test): PASS (`bash-syntax.log`). **11 contract tests PASS** (`maintenance-final.log`), gồm encrypt/restore/corrupt archive/SQL failure/health failure/backup failure/start failure/JAR rollback/TLS order và storage entry lạ. OpenSSL/tar/checksum thực, các lệnh privileged/DB/locking được mock; không gọi host services thật.
- Lint debug/release: **0 errors, 9 warnings mỗi variant**, không bỏ qua lint errors; không sửa những cảnh báo không thuộc bản vá này.
- Đã áp dụng skill `android_ui_verification`: kiểm tra kích thước màn, chạy ADB trên emulator, chụp/đọc ảnh QA. Hai ảnh `screenshots/revoked-viewer.png`, `screenshots/save-denied-no-cache-fallback.png` xác minh không còn ảnh/caption/identity khi bị từ chối. Chỉ dùng ảnh màu fixture, không chụp nội dung cá nhân. Không dump UIAutomator khi instrumentation đang chạy.

Lệnh Android dùng Java đã khóa qua `scripts/Use-TaktakJava.ps1`, Gradle offline, debug URL `http://127.0.0.1:8080/`; release URL `https://api.example.invalid/`. Backend dùng Maven local cache trong `.tools/`. Không in credentials hoặc response token ra báo cáo.

## Tệp thay đổi theo nhóm

- Backend: `MomentService`, `FirebaseConfig`, `FirebasePushNotificationSender`, `DeviceTokenRepository`, `application-prod.yml`; test `MomentIntegrationTest`, `MomentPushIntegrationTest`, `RecordingPushSender`, `FirebaseConfigurationTest`, `TrustedProxyTest`.
- Android: `AuthGraph`, `AuthRepository`, `MomentRepository`, `SessionManager`, `PushEventBus`, `TaktakMessagingService`, `MomentViewerActivity`, `HistoryViewModel`, `SessionViewModel`, `WidgetUpdateWorker`, `WidgetUpdater`; test `SessionPublishGuardTest`, `PrivacyRegressionAcceptanceTest`.
- Deploy: Compose, Nginx HTTP/TLS templates, helper `common.sh`, backup/restore/update/rollback/setup-vps/setup-ssl, 2 contract test scripts, [runbook hiện hành](../deploy/MAINTENANCE.md), README deploy.
- Hồ sơ: báo cáo này, PROJECT_STATUS, TASKS, SESSION_HANDOFF, PROJECT_DECISIONS, QA_FINAL_VERIFICATION và 2 hướng dẫn VPS. Không sửa README giới thiệu tính năng của ứng dụng.

## Giới hạn và bước thủ công

- Chưa có nghiệm thu production. Contract tests mock PostgreSQL/systemd/nginx/certbot/chown/flock: không chứng minh atomic restore SQL thực, quyền Linux, khóa đa tiến trình, cấp/gia hạn certificate hoặc rollback host thật. Không tự deploy để lấp thiếu bằng chứng này.
- Cần operator cung cấp staging Ubuntu với dữ liệu dùng thử và phê duyệt explicit: chạy backup → restore drill, inject lỗi → kiểm tra DB/ảnh/service; test Docker config/start nếu chọn Docker; TLS/DNS/ACME và quota qua Nginx thật. Runbook native không dùng cho Docker volumes.
- Cần Admin credentials hợp lệ và thiết bị nhận để kiểm thử FCM end-to-end thật. Các push integration tests dùng sender test, không chứng minh Firebase đã giao tin.
- File discard sau commit vẫn best-effort; lỗi filesystem có log nhưng chưa có durable retry/janitor. Server vẫn chặn ảnh soft-deleted; không tuyên bố crash-proof physical cleanup.
- Ảnh người dùng đã chủ động lưu gallery trước khi thu hồi không thể xóa từ xa. Với thao tác Save mới, app bắt buộc tải có JWT thành công, không xuất cache lúc mất quyền/offline.
- Profile vẫn placeholder. Không dùng kết quả bản vá này để ghi toàn dự án 100% DONE.

Rà soát cuối: `git diff --check` đạt; 133 link local của 10 tài liệu được kiểm tra, không link thiếu; quét 45 file thay đổi không thấy mẫu private key/API key/JWT/GitHub token (kiểm tra theo mẫu, không khẳng định chứng minh tuyệt đối). Dependencies, migrations, AGENTS, tài liệu nguồn và README giới thiệu ứng dụng không đổi. HEAD vẫn `6a7f51e`. Logs/screenshots nằm trong `.local/artifacts/fixes-20261018/` đã xác minh bị Git ignore.

Dọn môi trường: smoke server báo **14 active moment rows / 14 JPEG files / 8 recipient rows**, không lệch metadata/file; đóng server/cluster test, gỡ reverse và tắt đúng emulator do phiên này mở. Không xóa dữ liệu người dùng; ảnh cũ trong quy trình restore được giữ để có thể phục hồi.

Trạng thái: **PASS cho bản vá và các kiểm tra local nêu trên; CHƯA NGHIỆM THU production/FCM thật**. Đề xuất đúng một bước tiếp theo: nghiệm thu staging có phê duyệt theo runbook, trước khi coi hệ thống production-ready. Sau bàn giao dừng, không tự làm tính năng hoặc deploy.
