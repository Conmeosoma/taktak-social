# Taktak — Đánh giá trước redesign Camera

Cập nhật17/10/2026: người dùng đã duyệt redesign **Camera hiện có sau3D**, đã triển khai và nghiệm thu local/USB tại [QA_CAMERA_REDESIGN.md](QA_CAMERA_REDESIGN.md). Python đã cài và CLI skill chạy thật; không còn blocker Python/Camera của đánh giá cũ. **Toàn bộ nội dung bên dưới là lịch sử trước CameraX**, không phải trạng thái hiện tại hoặc quyền thêm flash/đổi camera/tab. Giữ lại để truy vết; không mở4A.

Ngày: 16/10/2026. Nguồn: yêu cầu PREMIUM MODERN UI/UX REDESIGN của người dùng, AGENTS, đặc tả/quyết định/trạng thái/công việc/bàn giao và source Android hiện tại. **BLOCKED triển khai redesign Camera**: chưa có camera hoạt động. Đây là đề xuất thiết kế, chưa phải design tokens đã áp dụng hoặc UI đã nghiệm thu.

## Phạm vi và điểm dừng

Yêu cầu chỉ cho phép cải tiến Camera và các tokens thực sự cần thiết; nếu Camera chưa tồn tại/chưa chạy thì báo và đề xuất màn có sẵn, không tự xây logic camera. Đối chiếu source xác nhận:

- MainActivity và activity_main.xml có cameraPanel/cameraTitle, nhưng nội dung chỉ là thông báo chức năng chụp ảnh chưa sẵn sàng và nút logout.
- Không có CameraFragment, PreviewView, ProcessCameraProvider hoặc ImageCapture trong main source; Manifest chưa khai báo CAMERA. Dependency CameraX có sẵn không có nghĩa tính năng camera đã triển khai.
- UI đang hoạt động là Login/Register và session gate/logout/retry của 3C. History/Friends/Profile, bốn tab đầy đủ và widget provider/layout chưa có trong main source; không nhận thiếu tính năng tương lai là lỗi regression UI.
- 3C local đã có bằng chứng kiểm thử từ phiên trước, nghiệm thu máy thật còn chờ. Lần kiểm tra ADB phiên này điện thoại đã xuất hiện trở lại; chưa cài APK/thực hiện auth trên điện thoại, không tự chuyển từ redesign sang nghiệm thu 3C.

Không thay source/theme/tokens hoặc dựng nút chụp giả. Đề xuất duy nhất khi người dùng muốn redesign ngay: **màn Login hiện có**, giữ Register và session/API nguyên trạng trừ shared token tối thiểu đã được duyệt, kiểm tra ảnh hưởng theme lên các màn dùng chung. Cần người dùng xác nhận đổi màn trước khi sửa.

## UI/UX hiện tại

- Theme đã kế thừa Material 3 DayNight, nên vấn đề không phải app đang dùng Android UI cũ. Hai theme light/night vẫn gần template mặc định, chưa định nghĩa palette Taktak; colors.xml chỉ có black/white.
- Auth đã có field radius 16dp, button cao ít nhất 56dp/text button 48dp, ViewBinding, validation tại field, loading lock và xử lý insets/IME. Những phần này cần tái sử dụng, không viết lại nghiệp vụ.
- Brand Auth 40sp, title và khoảng cách khai báo rải ở styles/layout; thiếu nhóm semantic color/spacing/typography tokens dùng chung. Việc font lớn gây cuộn trên máy nhỏ/font scale là rủi ro cần kiểm thử, chưa kết luận đã lỗi runtime ở cấu hình chưa chạy.
- Main hiện là bố cục thông báo giữa màn hình, không có preview hoặc hierarchy dành cho thao tác chụp; không thể đánh giá nút shutter/flash/đổi camera vì chưa tồn tại.
- Drawable hiện chủ yếu là launcher template, chưa có bộ action icon riêng. Không thay app icon hoặc tự tạo icon camera hoạt động trong phiên này.

## Áp dụng skill và nguồn

Đã đọc đầy đủ `ui-ux-pro-max/SKILL.md`. Thử CLI `search.py ... --design-system -p Taktak` nhưng Python trong PATH chỉ là Windows Store alias, báo không có Python. **Không có output design-system generator**, không giả đã chạy thành công; không cài Python toàn hệ thống chỉ để vượt blocker Camera.

Fallback: đọc trực tiếp CSV local của skill bằng PowerShell Import-Csv: styles, products, typography và ux-guidelines. Social Media App đề xuất vibrant/motion, nhưng không bê nguyên landing/dashboard hoặc motion-heavy vào yêu cầu soft minimal của Taktak. Neumorphism bị loại vì chính dữ liệu skill cảnh báo low contrast; chỉ giữ spacing mềm, hierarchy gọn và micro-interaction có mục đích. Typography giữ system sans-serif/Roboto, không tải font web hoặc thêm library. Skill không có stack Android Java/XML; không áp default Tailwind hoặc chuyển sang React/Flutter/Compose.

Native accessibility đối chiếu [Android Developers — Make apps more accessible (Views)](https://developer.android.com/guide/topics/ui/accessibility/views/apps-views): touch target ít nhất 48×48dp, text contrast theo cỡ chữ; đặt mục tiêu ít nhất 4.5:1 cho body text của thiết kế. Áp native contentDescription/TalkBack, sp, insets và ripple thay vì ARIA/CSS.

## Design system đề xuất, chưa áp dụng

Modern soft minimal, playful social vừa phải; ảnh là trọng tâm khi Camera được triển khai. Không gradient/glow/glass hoặc shadow lớn. Giữ Java/XML/ViewBinding/Material Components hiện có, không thêm dependency hoặc đổi API/JWT/FCM/MVVM.

| Semantic role | Light | Dark đề xuất |
|---|---|---|
| Primary brand | #FF6483 | #FF91A8 |
| Primary container | #FFF1F3 | #3A232D |
| Background | #FAF9F7 | #17171D |
| Surface | #FFFFFF | #22232D |
| Text chính | #222530 | #FFF8FA |
| Text phụ | #5C5F6A | #C8C2CB |
| Accent trang trí | #A58BF2 | #BEAAFF |

Không dùng trắng trên coral làm chữ CTA nhỏ: tính theo relative luminance sRGB bằng PowerShell được **#FFFFFF/#FF6483 = 2.84:1**. Giữ coral, dùng chữ #222530 trên nút: **5.38:1**. Chữ chính light trên background **14.51:1**; chữ chính dark trên background **17.05:1**. Coral dark #FF91A8 trên #17171D **8.39:1**. Đây là kiểm tra cặp màu đề xuất, không phải audit accessibility toàn màn hoặc mọi state disabled/focus/error.

- Spacing scale 4/8/12/16/24/32dp; screen padding 24dp, card radius 16–24dp; CTA cao 56dp, mọi action tối thiểu 48dp.
- System sans-serif: body 16sp, button 16sp medium, title 28–32sp; giảm dominance của logo, không ép chiều cao text khi font scale lớn.
- Icon VectorDrawable 24dp trong touch target 48dp, cùng nét; không emoji làm icon. State lỗi có chữ và semantic color riêng, không dùng duy nhất màu thương hiệu để báo lỗi.
- Ưu tiên ripple/focus của Material; animation nếu cần 150–200ms, tôn trọng cài đặt giảm/tắt animation. Không giữ spinner vô hạn thay lỗi/retry.
- Khi được duyệt triển khai: semantic colors trong values/values-night, dimens.xml và style chung tối thiểu; không thay global theme hàng loạt mà chưa review ảnh hưởng lên UI 3C.

Định hướng Camera tương lai: preview lớn trong vùng usable, top actions flash/đổi camera rõ nhãn truy cập, shutter chính không bị navigation che; bốn tab giữ đúng Camera/Lịch sử/Bạn bè/Hồ sơ khi tới bước navigation. Đây chỉ là định hướng, không scope triển khai mới.

## Kiểm tra phiên này

- Đã kiểm tra Git status/log và Android Activity/layout/drawable/theme/colors/style/navigation/widget inventory. Giữ mọi thay đổi dirty/untracked có sẵn từ 3B/3C.
- Parse **14 resource XML hiện có: PASS**; không thêm resource/layout mới.
- `assembleDebug`: **BUILD SUCCESSFUL, 11 giây, 39 task UP-TO-DATE**. Đây là build baseline đã có, không phải clean rebuild hoặc nghiệm thu redesign; ViewBinding/resource hiện tại được build system chấp nhận, chưa sửa binding.
- Không chạy lại unit/instrumentation/lint hoặc runtime light/dark/small-screen/font scale trong phiên đánh giá này. Không lấy kết quả 3C cũ thành test redesign mới; không có ảnh trước/sau mới vì chưa thay UI.
- Không cài APK hoặc sửa app/data trên điện thoại. Không sửa backend/DB/CameraX/auth/navigation hoặc commit/push/deploy.
- Kiểm tra cuối: SHA-256 toàn bộ file trong android-client/app/src giữ nguyên trước/sau cập nhật hồ sơ; quét 2 tài liệu theo mẫu key/conflict, resolve 30 link local và git diff --check PASS. Index không có thay đổi staged; các file dirty/untracked 3B/3C được giữ nguyên. Đây không phải kiểm thử UI hoặc audit toàn lịch sử Git.

## Bàn giao

Chỉ tạo tài liệu đánh giá này và bổ sung mục bàn giao vào SESSION_HANDOFF; Android không đổi trong phiên redesign. Kết quả **BLOCKED triển khai Camera**, không phải build FAIL. Chờ người dùng xác nhận **redesign Login trước**; không tự chuyển màn hoặc mở bước 3D.
