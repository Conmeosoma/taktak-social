# Login theo ảnh tham chiếu — 18/10/2026

## Phạm vi và thiết kế

Người dùng duyệt triển khai prompt pixel-precise Login, sau đó yêu cầu tắt emulator vì laptop chậm và chuyển kiểm tra sang điện thoại USB. Bắt đầu ở HEAD `f73b43d`, worktree sạch. Chỉ sửa Login và resources/test liên quan; không thay backend, database, API contract, auth/session/FCM hay dependency; không redesign Register.

Đã sử dụng `ui-ux-pro-max` (Python3.12.10, CLI design-system và UX focus/touch targets) và `android_ui_verification` (calibration, kiểm tra ảnh native, IME, hành vi UI). Khuyến nghị web/chatbot từ CLI không phù hợp nên không áp dụng; ảnh người dùng, Java/XML và nghiệp vụ Taktak quyết định thiết kế.

- Theme `Theme.TakTak.LoginReference` riêng Activity, nền tối cố định theo ảnh, không đổi chế độ toàn app. Background canvas chỉ vẽ trang trí coral/lavender; các controls vẫn là View native.
- Logo là PNG do người dùng cung cấp, giữ nguyên bytes; SHA-256 nguồn và asset cùng `CD714A1846C766DD93BFEE0AD2B8E81B369B10C33FB87D1D43062A3D3D247113`. LoginLogoView chỉ crop viền trắng/mask góc khi render, không dùng ảnh toàn màn hình làm UI.
- Back thật, nhãn Tiếng Việt không clickable, logo giữa, trường Email/mật khẩu có icon/outline/focus/error, password toggle thật, CTA gradient/ripple/loading/disabled và tạo tài khoản giữ điều hướng cũ.
- Quên mật khẩu chưa có backend/UI nghiệp vụ: chỉ nhãn không clickable, contentDescription giải thích chưa hỗ trợ. Không tạo API hoặc luồng giả.
- Insets riêng cho form, background vẽ xuyên dưới system bars; status bar thật. Scroll fillViewport, form tối đa480dp, control tối thiểu48/56dp, kích thước dp/sp, footer và CTA co giãn theo chiều cao.
- `gradle.properties` chỉ escape dấu `:` của các đường dẫn Windows để sửa lỗi lint có sẵn; giữ nguyên đường dẫn JDK và API origin. Không đổi phiên bản build tools.

## Xác minh và các lần lỗi

| Kiểm tra | Bằng chứng thực tế |
|---|---|
| Assemble debug/test/release, lint debug/release | PASS ở `login-reference-usb-final-build.log`;0 errors/15 warnings mỗi variant |
| Android JVM |72 tests,0 failures/errors; XML tại app/build/test-results/testDebugUnitTest |
| Test APK cuối |PASS `login-reference-usb-fixture-final.log` |
| USB UI-only bản cuối |**4/4 PASS**,16.897s ở `login-reference-usb-final.log`: responsive/font1.5 ở6 cấu hình; validation/IME/điều hướng Register; mask/reveal sau validation/loading; nhãn/ripple/disabled |
| API login qua UI trên emulator trước chuyển USB |Ca `AuthUiTest#loginUsesViewModelAndRoutesToCameraWithSavedSession` đạt với backend fixture; không suy ra toàn batch đạt |
| Toàn batch emulator đầu |5 ca,2 lỗi (responsive Back và password assertion); không tính batch này PASS |

Lỗi đã gặp: style Input thiếu parent rỗng khiến AAPT suy ra parent không tồn tại; thuộc tính navigation bar XML cầnAPI27 được bỏ để dùng WindowInsetsControllerCompat đã có, giữ min26; test gọi API Material không public được sửa sang findViewById. Back icon chuyển ImageButton native48dp thay TextView-button để tránh font lớn làm text layout rỗng bị cắt.

ActivityScenario trên điện thoại không đưa Login lên foreground trong lượt đầu; các lượt bị dừng không tính PASS. Test USB mới mở launcher thật rồi Login cùng UID, không cấp quyền MIUI/overlay hay xóa phiên. Ca điều hướng sau đóng IME ở lượt đầu chưa đạt; fixture cuối chờ insets xác nhận IME đóng và layout ổn định trước tap, giữ assertion điều hướng.

## An toàn khi dùng điện thoại cá nhân

Đã tắt emulator theo yêu cầu; backend/embedded PostgreSQL QA local đã dừng sạch với0 moment/avatar rows/files. Điện thoại Android15,1080×2400, fontScale1.0. Trước install đã đọc duy nhất API origin trong APK đang cài để giữ đúng origin: EncryptedSessionStore sẽ xóa phiên nếu đổi origin, nên không cài bản loopback QA lên điện thoại.

Cài bằng `adb install -r`, không uninstall/clear data; không chạy `AuthUiTest` hoặc test nào tự logout/đăng ký/login trên tài khoản cá nhân. Test USB dùng form rỗng/chuỗi ngẫu nhiên và không gửi credentials hợp lệ; chỉ quan sát UI, validation, password, keyboard và điều hướng. Không thay fontScale, độ phân giải hoặc quyền MIUI của điện thoại.

Artifacts ignored tại `.local/artifacts/`: các log `login-reference-*`, ảnh emulator đầu `login-reference-first-screenshots/`, ảnh USB cuối `login-reference-usb-final-screenshots/` gồm login, validation, keyboard-submit, keyboard-closed và register-navigation. Đã xem ảnh Login và IME cuối, xác nhận CTA còn truy cập được, nền trang trí nằm dưới status bar thật. APK copy trước cập nhật chỉ phục vụ đối chiếu origin, không đọc preferences/token.

## File thay đổi

- `android-client/app/src/main/java/com/taktak/app/ui/auth/`: sửa LoginActivity, thêm LoginBackdropView và LoginLogoView.
- `android-client/app/src/main/AndroidManifest.xml`: chỉ đổi theme Login.
- `android-client/app/src/main/res/layout/activity_login.xml`; thêm `values/login_design.xml`,2 color selectors `login_*`,6 drawable XML `login_*`,asset `drawable-nodpi/login_logo_original.png`.
- `android-client/app/src/androidTest/java/com/taktak/app/`: thêm LoginReferenceTest, điều chỉnh theme/tokens/assertion của LoginM3Test; test cũ không bị xóa hoặc skip để che lỗi.
- `android-client/gradle.properties`: sửa escape đường dẫn Windows, giữ API/JDK.
- Báo cáo này, PROJECT_STATUS, TASKS và SESSION_HANDOFF.

## Khác biệt và giới hạn

- Dùng “Email” thay “Số điện thoại hoặc email” vì API hiện chỉ hỗ trợ email.
- Typography phụ thuộc font hệ thống/OEM; footer dùng font hệ thống, không phải asset wordmark riêng. Không tuyên bố khớp từng pixel hoặc phần trăm giống nhau chưa đo.
- Khoảng trống giãn theo tỉ lệ màn hình; ảnh mẫu và máy thật có thể khác aspect ratio. Logo, outline và gradient giữ hướng thiết kế mẫu; không thêm hiệu ứng động không cần thiết.
- Chữ trắng trên đầu coral sáng của gradient bám yêu cầu ảnh nhưng không mặc định đạt WCAG AA4.5:1; chưa nghiệm thu accessibility toàn diện cho CTA này.
- Chưa chạy đăng nhập/retry mạng đầu cuối bằng tài khoản riêng trên bản cuối ở điện thoại, tránh thay phiên cá nhân. Không suy ra test UI-only là nghiệm thu API production/VPS hoặc FCM.

**Kết luận: build/unit và UI USB PASS trong phạm vi đã chạy; nghiệm thu API/retry trên bản cuối ở điện thoại cá nhân NOT VERIFIED.** Chưa commit/push/deploy. Dừng sau Login, không chuyển Register.
