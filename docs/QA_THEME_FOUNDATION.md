# UI-THEME-02 — Theme preferences và resolver

Ngày: 19/10/2026. Trạng thái: **PASS phạm vi foundation**, không phải hoàn tất Multi-theme UI.

## Phạm vi và hiện trạng

Người dùng duyệt UI-THEME-02 sau audit [UI_THEME_AUDIT.md](UI_THEME_AUDIT.md). Trước bước này chỉ có kế hoạch, chưa có preferences/manager. Worktree đã có nhiều thay đổi UI/recovery chưa commit tại HEAD `f73b43d`, nhánh `chore/push-current-project`; các thay đổi đó được bảo toàn. Không tự chuyển bước03 hoặc sửa backend/DB.

Đã thêm package Java `com.taktak.app.ui.theme`:

- `AppTheme`: ID ổn định `original`, `gradient`, `glass`, `soft`; không persist ordinal/tên dịch.
- `ColorMode`: `system`, `light`, `dark`, độc lập với style.
- `ThemeSelection`: immutable, có revision không âm; mặc định Original/System/0.
- `ThemeStore`, `ThemePreferences`, `ThemeRepository`: settings-only; không tham chiếu AuthRepository/SessionManager/API hoặc token.
- `ThemeManager`: resolver theo ScreenRole, ánh xạ AppCompat night mode và entrypoint apply-before-create để tích hợp từng màn ở bước sau.

File riêng `taktak_appearance_v1`, private SharedPreferences; chỉ bốn key `style_id`, `color_mode`, `schema_version`, `revision`. Đây không phải dữ liệu nhạy cảm nên không dùng file encrypted auth. Đọc thiếu/sai kiểu/ID lạ/revision âm/schema không hỗ trợ trả Original/System mà không sửa disk. Schema mới hơn bị từ chối ghi để tránh downgrade phá dữ liệu. No-op không ghi/tăng revision. Save trên worker, commit đồng bộ cả bộ key; chỉ trả thành công sau commit đạt. Lỗi commit khôi phục cache các key sở hữu và báo IOException; rollback thất bại cũng báo lỗi, không hứa disk đã phục hồi khi cả hai lần ghi đều thất bại. Static lock tuần tự hóa read-modify-write giữa các instance trong cùng process; **không hỗ trợ multi-process writer**.

## Fallback và ranh giới tích hợp

| ScreenRole | Original resource giữ nguyên |
|---|---|
| LOGIN | Theme.TakTak.LoginReference |
| AUTH | Theme.TakTak.Premium |
| MAIN | Theme.TakTak.Premium |
| PREVIEW | Theme.TakTak.M3.Camera |
| VIEWER | Theme.TakTak.M3 |

Ba style mới hiện **chưa có renderer/resources**. Resolver giữ requested selection/revision, trả effective Original và `requestedStyleAvailable=false`, không tạo alias giả thành theme mới. Original trả availability=true. Login Original vẫn fixed-dark như audit, không tuyên bố đã có Login light mới.

Chưa thay Manifest, Activity/Fragment, XML/layout, palette hoặc dependency; chưa nối manager vào các màn hiện có. `applyBeforeCreate` chỉ dành cho caller gọi trước super.onCreate/inflate; chưa có caller production. Chưa ViewModel/chooser/entrypoint/theme tokens mới: làm khi có consumer ở03–04. Không auto recreate/onResume observer, không restart task, không thay draft/password/session/CameraX. Không có giao diện mới để người dùng chọn ở02. Widget RemoteViews không đổi.

## Kiểm thử đã chạy

Build cuối: `.local/artifacts/theme02-final-build.log`, **BUILD SUCCESSFUL in59s**. Dùng JDK helper hiện có, Gradle offline/no-daemon/max-workers2; không nâng dependency. Các task: assembleDebug, assembleDebugAndroidTest, testDebugUnitTest, lintDebug, assembleRelease, lintRelease. Release BASE_URL dùng placeholder HTTPS `https://api.example.invalid/` chỉ để xác minh build, không deploy.

| Kiểm tra | Kết quả thực tế |
|---|---|
| Toàn bộ Android JVM | **93/93**, 0 failures/errors/skipped; gồm14 test foundation mới |
| Resolver | 4style × 3mode × 5role; requested/effective/availability/resource đúng |
| Preferences JVM | Empty read, tất cả lựa chọn, no-op/revision, dữ liệu hỏng, schema mới, overflow/null, commit/rollback failure, retry, giữ unrelated key, concurrent instances |
| Lint debug/release | **0 errors**; 20/29 warnings cũ, 0 findings tại package theme mới |
| AVD batch | **8/8 PASS,23.635s**:4 ThemePreferencesDeviceTest +4 LoginReferenceTest |
| Process persistence | Probe ghi **1/1 PASS**, force-stop app/test trên AVD, instrumentation process mới đọc **1/1 PASS**; file fixture QA được dọn sau đọc |
| Rà soát scope | Hash293 file trước bước:0 sửa/mất; chỉ thêm7 file source foundation trong cây main, không sửa source cũ/server/deploy/scripts |

Device tests dùng file QA riêng, không mở file auth/thay theme preference thật của người dùng. Session isolation là sentinel trong **file session QA**, kiểm tra ghi theme không đụng sentinel và clear QA session không xóa theme; **không phải** nghiệm thu logout/login API thật. Probe persistence dùng riêng `qa_appearance_process_theme02`, không file app thật. Save failure được fault-inject bằng JVM fake mô phỏng cache update dù commit=false; không cố làm đầy disk AVD.

Thiết bị: AVD Medium_Phone read-only/headless, API37,1080×2400,1.5GB RAM/2core. Không dùng điện thoại. Skill `android_ui_verification` được áp dụng cho calibration, regression và kiểm tra ảnh; không dùng hướng dẫn React Native/JS logs. Không chạy lại design CLI ui-ux-pro-max vì02 không thiết kế UI mới; kết quả design audit01 giữ nguyên.

Log AVD:

- `.local/artifacts/theme02-device.log`
- `.local/artifacts/theme02-process-write.log`
- `.local/artifacts/theme02-process-read.log`
- Năm ảnh Login regression trong `.local/artifacts/theme02-login/login-reference-usb/`; đã xem `login.png`, không có ảnh cho3theme chưa triển khai.

IME AVD đổi tạm show_ime_with_hard_keyboard từ0→1 cho test bàn phím, đã hoàn trả0. AVD do phiên khởi chạy được đóng sau QA để giải phóng RAM; không xóa AVD/dữ liệu. Artifacts được Git ignore, không commit/push/deploy.

Review cuối: `git diff --check` exit0; cả10file Java mới không trailing whitespace. Quét mẫu private-key/API-key/JWT/hard-coded credential trong10file mới có0findings (không phải chứng nhận audit secrets toàn repo). Index không có file staged. ADB devices trống sau đóng AVD. Tổng thay đổi riêng bước02:10Java mới +1QA doc mới +4docs quản lý được cập nhật, không thay đổi các file source có sẵn.

## Giới hạn và bàn giao

Chưa kiểm tra reboot thiết bị, system-mode transition khi Activity thật áp dụng manager, runtime đổi theme, bảo toàn draft khi recreate, accessibility/contrast/performance của3theme mới hoặc OEM. Chưa có các tính năng đó ở02 nên không đánh dấu đạt. Backend/Gmail/FCM không chạy lại; blocker Gmail App Password trước đây vẫn giữ nguyên.

Tiếp theo đề xuất **UI-THEME-03 — chooser, preview, Apply/Cancel và entrypoints**, chỉ làm sau xác nhận. Cần hiển thị rõ availability/fallback trong giai đoạn chuyển tiếp; không nói ba theme đã phủ toàn bộ app. Ánh xạ ảnh Gradient=ảnh3,Glass=ảnh2,Soft=ảnh1 từ audit vẫn chờ chốt trước khi triển khai visual. Không có thao tác thủ công bắt buộc cho02.
