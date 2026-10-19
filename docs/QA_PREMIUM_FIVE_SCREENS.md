# Redesign 5 màn theo ảnh tham chiếu

Ngày18/10/2026, Asia/Saigon. Người dùng xác nhận rõ **cả5 màn** sau khi được báo prompt đính kèm vẫn ghi Login-only: Register, Camera, Lịch sử, Bạn bè, Hồ sơ. Quyền này thay phạm vi Login-only cho hạng mục UI hiện tại, không mở thêm roadmap/nghiệp vụ/deploy. Dùng điện thoại USB; không mở emulator vì người dùng báo laptop lag. Người dùng đồng ý hướng camera vào khu vực an toàn cho kiểm tra.

## Trạng thái đầu phiên và phạm vi

HEAD `f73b43d`; có thay đổi Login chưa commit và tài liệu của phiên trước. Đã đọc quy tắc/đặc tả/quyết định/trạng thái, source và layout trước sửa; bảo toàn Login và các thay đổi đó. Không sửa backend/database, ViewModel/Repository/data/JWT/FCM/widget, build dependencies hoặc applicationId. Không commit/push/deploy, không hard-code avatar/ảnh camera từ ảnh mẫu.

Skill `ui-ux-pro-max` dùng để khảo sát design system và các ưu tiên readability/performance; `android_ui_verification` dùng để kiểm tra native layout, calibration và ảnh USB. Không áp dụng web/CSS hoặc hiệu ứng blur/animation nặng từ gợi ý CLI. Stack giữ Java/XML/ViewBinding/Material Components.

## Triển khai

- Theme `Theme.TakTak.Premium` opt-in cho Main/Register và các cây View liên quan, palette sáng/tối riêng; không sửa foundation M3 hay theme Login đã nghiệm thu. Nền charcoal/coral/lavender ở dark và cream ở light; canvas trang trí tĩnh nằm dưới system bars thật, không chặn chạm.
- Main: safe content nhận system/IME insets, background vẽ full window, giữ session gate/bốn tab, thanh điều hướng bo mềm có active indicator và icon hai người đúng tab Bạn bè. Camera dùng ánh sáng nền nhẹ hơn các tab còn lại.
- Register: logo gốc đã cung cấp, wordmark, Back thật, nhóm thông tin/bảo mật, outlined fields có icons/helper/password toggle, CTA gradient/loading/disabled; giữ nguyên validation/register API và không lưu password qua state.
- Camera: preview CameraX vuông thật, khung/viền mềm, icon đổi camera/flash, shutter và status card riêng tư. Không chụp hoặc gửi ảnh từ tài khoản cá nhân trong test UI này. Trang vẫn cuộn khi viewport không đủ chỗ.
- Friends: header/subtitle, ô tìm và CTA, card section viền gradient/icon; vẫn dùng dữ liệu thật, phân trang và các action cũ. Không gửi/chấp nhận/hủy/block bạn bè để nghiệm thu trên tài khoản cá nhân.
- History: header, refresh, tab indicator gradient, empty illustration vector neon và vùng rỗng cuộn được; dữ liệu Received/Sent vẫn từ API. Sửa lỗi layout có sẵn: container ảnh height0 trong MaterialCardView không thể dùng constraint ratio ở chính nó; chuyển wrap-content và đặt ratio1:1 lên ImageView con trong ConstraintLayout. Không sửa loader, cache hoặc quyền tải ảnh.
- Profile: header/subtitle, avatar circle/border và nút camera nối cùng hành động chọn ảnh hiện có, username/email thật, field/CTA/save/reload/logout. Busy state khóa đồng thời cả hai lối mở picker; không thêm endpoint hoặc nghiệp vụ avatar.

## File của hạng mục này

- Java: `MainActivity`, `RegisterActivity`, `FriendsAdapter`, `ProfileFragment`; thêm `ui/main/PremiumBackdropView`. Chỉ wiring/presentation; không đổi CameraFragment hoặc lớp nghiệp vụ.
- XML: `activity_main`, `activity_register`, `fragment_camera`, `fragment_camera_page`, `fragment_friends`, `fragment_history`, `fragment_profile`, `item_friend`, `item_friends_section`, `item_history_moment`, `menu/main_tabs` và theme Register trong manifest.
- Resources mới: `values/premium_design.xml`, `values-night/premium_colors.xml`,2 color selectors `premium_*`, drawable backgrounds/gradient/tab indicator, vector inbox và các icon `ic_premium_*`.
- Test: thêm `PremiumScreensTest`. Giữ `LoginReferenceTest` và các thay đổi Login cũ; không xóa/skip test để che lỗi.
- Tài liệu: báo cáo này, PROJECT_STATUS, TASKS, SESSION_HANDOFF, PROJECT_DECISIONS.

## Kiểm thử thực tế

| Kiểm tra | Kết quả |
|---|---|
| assembleDebug/assembleDebugAndroidTest/testDebugUnitTest/lintDebug/assembleRelease/lintRelease | **PASS**, `premium-five-final-build.log`, BUILD SUCCESSFUL1m03s |
| JVM | **72/72 PASS**,0 failures/errors; XML ở app/build/test-results/testDebugUnitTest |
| Lint debug/release |0 errors; lần lượt18/27 warnings, không suppress/baseline để bỏ qua lỗi |
| Test APK fixture cuối |PASS, `premium-five-fixture-retest.log` |
| USB batch cuối |**7/7 PASS**,37.632s, `premium-five-usb-retest.log` |
| Layout/theme |9 layout ×3 viewport(280×560,360×720,840×420dp) ×2 mode =54 tổ hợp, fontScale1.5; kiểm tra bounds/text clipping và text chính/phụ/navigation trên nền >=4.5:1 |
| History ảnh |Layout test assert chiều cao>0 và tỷ lệ1:1; ảnh thật từ JWT đã nhìn thấy trong screenshot sau sửa |

Ba test Premium: responsive/empty layout; Main bốn tab với dữ liệu thật + Received/Sent + validation search + Profile picker busy parity; Register validation/loading/eye/IME không gửi request. Bốn test hồi quy Login: native controls/ripple, mask/reveal, keyboard/Register routing, responsive lớn chữ. Dark UI đã chạy trên điện thoại Android15,1080×2400,font1.0; light/landscape/font1.5 kiểm tra bằng Configuration context trên thiết bị, **không đổi setting toàn máy**.

Lệnh build dùng scripts/Use-TaktakJava.ps1(Target Android), toolchain Android Studio đã có, offline/no-daemon/max-workers2 và release URL HTTPS placeholder để kiểm tra build. APK release unsigned không phải bản triển khai production. Debug APK giữ nguyên API origin đang có trên điện thoại; cài bằng install-r, không uninstall/clear data.

Chạy instrumentation qua ADB nhắm đúng thiết bị USB với hai class `com.taktak.app.PremiumScreensTest,com.taktak.app.LoginReferenceTest`. Không dùng test auth/relationship có tác dụng logout/thay tài khoản hoặc thay dữ liệu người dùng. Health endpoint hiện hành được GET và trả200; không deploy/cấu hình lại server.

### Lỗi đã gặp, không tính PASS cho các lượt này

1. Lượt3-test đầu: Main navigation đạt,2 ca lỗi do đo fragment Camera wrap-content như height cố định và chưa đợi Activity RESUMED. Fixture được sửa theo parent ScrollView/RecyclerView thật và poll lifecycle; giữ các assert bounds/font.
2. Lượt7-test kế tiếp:6 đạt,1 lỗi vì test giả định Friends báo lỗi bằng TextInputLayout.error. Code hiện có báo bằng friendsMessage; sửa assert đúng nội dung/visibility notice, không sửa hành vi ViewModel để làm test qua.
3. Chạy lại đầy đủ7 ca đạt, không bỏ ca lỗi hoặc hạ điều kiện. Lỗi ảnh Lịch sử height0 là lỗi UI thật đã sửa trong production XML và thêm assertion phòng hồi quy.

## Ảnh và đối chiếu

Artifacts local, Git ignore: `.local/artifacts/premium-five-final-screenshots/`. Có Camera, History Received/Sent, Friends, Profile, Register, Register validation/IME; đã xem các ảnh sau sửa. Nội dung tài khoản/ảnh private vẫn là dữ liệu thật, không chép chúng vào source hoặc tài liệu tracked. Không suy ra ảnh mẫu empty đồng nghĩa phải xóa dữ liệu để tạo empty state.

Ảnh Camera tự động có thể bắt trước frame đầu dù control đã bind. Đã mở app bình thường, chờ3giây và chụp thêm `camera-live.png`; xem xác nhận preview CameraX thật đang hoạt động, không phải ảnh tĩnh hoặc màn đen. Không nhấn shutter hay tạo/upload moment để lấy bằng chứng UI.

Khác biệt còn lại:

- Illustration rỗng là vector neon, không phải asset3D tách nền trong ảnh mẫu; font/wordmark theo font hệ thống/OEM, không cam kết giống từng pixel.
- Nền/viền/nav dùng màu và gradient tĩnh, không blur hoặc glass3D thực; giữ chi phí render nhẹ.
- Preview Camera vẫn1:1 theo quy ước dự án dù ảnh tham chiếu có khung khác tỷ lệ. Khi viewport thấp/font lớn, phải cuộn để xem hết status; không ép nhỏ text hoặc thay aspect CameraX để giống ảnh.
- Chữ trắng trên gradient coral sáng bám mẫu nhưng chưa nghiệm thu WCAG AA cho toàn CTA; kiểm tra4.5:1 bên trên chỉ áp dụng các cặp text/nền đã nêu.
- Không chạy lại toàn bộ hành vi mutation/API/upload/download/logout/FCM/widget hoặc backend suite trong hạng mục UI; không tuyên bố những mục đó đã được nghiệm thu lại chỉ nhờ UI tests.

**Kết luận: PASS build/unit và phạm vi UI USB đã nêu.** Đã cài APK trên điện thoại, bảo toàn dữ liệu/phiên; không mở emulator. Dừng để người dùng xem5 màn, không tự chuyển roadmap, commit/push/deploy.

Review cuối: git diff--check đạt;133 liên kết Markdown local tồn tại; quét mẫu credentials trong file thay đổi không có phát hiện (không thay thế secret scanner toàn lịch sử). Diff backend/data/ViewModel/widget bằng0; ảnh QA được git check-ignore xác nhận bỏ qua. ADB cuối chỉ còn điện thoại USB, không có emulator.
