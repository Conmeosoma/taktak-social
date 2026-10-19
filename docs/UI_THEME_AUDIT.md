# UI-THEME-01 — Audit và đề xuất Multi-theme Taktak

Ngày19/10/2026. **Chỉ audit/kế hoạch, chưa triển khai Theme Engine hoặc sửa source ứng dụng.** Yêu cầu mới cho phép dùng emulator thay điện thoại. Mọi thay đổi UI/recovery/backend chưa commit trước phiên được giữ nguyên. Gmail recovery còn chờ App Password, không nằm trong nhiệm vụ này.

## A. Hệ thống hiện tại và bằng chứng

Đã đối chiếu AGENTS, README, đặc tả, quyết định/trạng thái/tasks/handoff, resources, Activity/Fragment, PreviewViewModel/CameraFragment và widget. Android giữ Java/XML/ViewBinding, min26/target37; version catalog khóa Material Components **1.14.0**. Không cần nâng dependency cho đề xuất này.

| Khu vực | Cấu hình thực tế | Điểm cần xử lý khi triển khai |
|---|---|---|
| Base | Theme.TakTak → Material3.DayNight; Theme.TakTak.M3 → Material3Expressive.DayNight.NoActionBar | Chọn một resolver, không xóa các theme cũ |
| Login | Manifest chọn LoginReference; LoginBackdropView vẽ màu tối cố định; login_design.xml, drawable gradient riêng | Không có biến thể sáng; hard-code Canvas, field style, màu icon và status-bar flags |
| Register và3form recovery | Premium qua Manifest, field/CTA Premium | Dùng chung RecoveryFormActivity; phải giữ password saveEnabled=false và reset-only routing |
| Main | setTheme(Premium) trước super; activity_main.xml cũng ép Premium | Chỉ đổi Manifest/setTheme chưa đủ; root theme sẽ phủ cấu hình mới |
| Camera/History/Friends/Profile | Nhiều root/item XML ép Premium; PremiumBackdropView có shader sáng/tối riêng | Gỡ ép theme có chọn lọc trong02–08, không thay đổi callbacks nghiệp vụ |
| Preview/người nhận | Manifest M3.Camera; Activity riêng, ViewModel + SavedStateHandle | Bảo vệ caption/selection/idempotency/progress và draft cache |
| Viewer | Manifest M3 | Mở rộng theming presentation khi06–08 được duyệt, không đổi tải/xóa/lưu ảnh |
| Shared | Material fields/cards/toolbar/dialogs, MaterialButton và CTA gradient AppCompatButton | Phải cập nhật cả drawable selector lẫn trạng thái render màu bằng Java |
| Widget | RemoteViews, native layouts; Provider chỉ enqueue WorkManager | Không áp ThemeOverlay/Canvas/custom View của Activity vào RemoteViews |

Có **9 layout chứa android:theme**,10layout có tham chiếu scoped màu/theme theo truy vấn audit;92resource XML parse hợp lệ. Màu được ghi trực tiếp trong LoginBackdropView/PremiumBackdropView, gradient drawables và một số status renderer Camera/Friends. Main có `setSaveEnabled(false)` cho pager và chủ động clear/recreate fragment sau session gate; không được bỏ gate để chữa theme.

Hiện chưa có ThemeManager/ThemePreferences/AppTheme, chưa thấy persisted style setting hay night-mode setting độc lập. Preferences phiên `auth_session_v1` và device identity đang phục vụ nghiệp vụ; không dùng chúng để lưu theme.

## B. Phân tích3ảnh và điểm cần duyệt

**Số ảnh thực tế đang ngược phần mô tả prompt ở ảnh1/3.** Đề xuất dưới đây ánh xạ theo phong cách, chưa tự khóa trước người dùng duyệt:

| Theme ID ổn định | Tên | Ảnh theo thứ tự đính kèm | Phân tích |
|---|---|---|---|
| original | Taktak mặc định | UI hiện tại, không phải3ảnh mới | Fallback, giữ theme/resources cũ và logo hiện có |
| gradient | Gradient Modern | **Ảnh3**, tên file kết thúc13_31_29 | Coral–pink–purple–blue, vòng tròn lớn, input translucent, CTA capsule có chiều sâu; giảm trang trí quanh ảnh camera |
| glass | Glassmorphism | **Ảnh2**,13_31_34 | Cyan/blue/violet/neon-pink, panel kính lớn có viền, nhiều lớp/orb; không mô phỏng toàn màn bằng screenshot/bitmap blur |
| soft | Soft UI | **Ảnh1**,13_31_38 | Pastel cream/pink/lavender, chữ tím, input nổi nhẹ, viền sáng; tránh shadow làm mờ ranh giới input |

Không copy statusbar/giờ/pin, chữ giả hoặc nguyên screenshot làm UI. Mẫu ghi “Số điện thoại hoặc email” nhưng server chỉ hỗ trợ email: giữ nhãn Email, không thêm đăng nhập điện thoại. Nhãn Tiếng Việt hiện chưa có language switching: không tạo dropdown giả. Giữ logo PNG gốc đang có; khác biệt logo giữa ảnh không đủ quyền thay định danh/asset.

## C. Kiến trúc đề xuất

`ThemePreferences → ThemeRepository → ThemeViewModel (chỉ settings UI) → ThemeManager → Activity context → attrs/styles/drawables → Views`

- `AppTheme`: original/gradient/glass/soft; lưu ID chuỗi ổn định, không enum ordinal. `ColorMode`: system/light/dark là trục riêng.
- `ThemePreferences`: private SharedPreferences `taktak_appearance_v1`,keys style_id/color_mode/schema_version; unknown/corrupt → Original + System, không đụng session. Persist bằng commit trên executor, chỉ báo áp dụng thành công khi lưu đạt. Logout/login không clear file này; reinstall/clear-app-data không thuộc cam kết giữ lựa chọn.
- `ThemeRepository/ThemeViewModel`: settings-only, LiveData selection/loading/error, không sửa Repository/ViewModel auth/camera/upload hiện có.
- `ThemeManager.resolve(style,screenRole)`: Original trả đúng style cũ theo Login/Main/Preview/Viewer; theme mới dùng cùng Material3Expressive parent + role overlay khi cần.
- `ThemedActivity`: tích hợp tại các Activity hiện có, apply theme/local night mode **trước super.onCreate/inflate**. AuthFormActivity kế thừa base này; không tạo4bản sao Activity. Không dùng API callback chỉ có29+ làm đường duy nhất cho min26.
- Dần chuyển `@color/premium_*`, `@color/login_*` và root theme cố định sang `?attr/...`; **không ghi đè các color resource cũ toàn cục** vì sẽ làm mất Original. Adapters inflate từ parent.getContext(); dialogs dùng Activity/current themed context, không Application context.
- Reuse Material semantic attrs cho primary/surface/onSurface/error/outline, thêm attrs `ttBackgroundStyle`, `ttSurfaceStyle`, `ttGradientStart/Center/End`, `ttOnGradient`, `ttCornerField/Card`, `ttShadowStrength`, `ttPrimaryActionStyle`. Không tạo abstraction cho mọi pixel.
- Drawable/shader cache chỉ theo size +style+mode trong View; không cache mutable Drawable toàn cục dùng chung state, không giữ Activity trong singleton.

## D. Design tokens đề xuất (chưa là resources đã triển khai)

| Token | Original | Gradient | Glass | Soft |
|---|---|---|---|---|
| light base / dark base | #FFF8F7 / #131216 | #FFF4FA / #1B1435 | #E9F6FF / #09162F | #F5E9FF / #211A2D |
| light ink / dark ink | #30252F / #F9F1F4 | #28142F / #FFF6FF | #0F2040 / #F3F6FF | #3C2458 / #F2E9FF |
| light surface / dark surface | giữ resources cũ | #FFF9FD / #332647 | #F7FBFF / #111E3D | #FBF4FF / #2E243B |
| brand accent | Coral/Pink hiện tại | #FF8A7A,#FF4FA0,#9A45F5,#3579F6,#B78AFF | Cyan/Blue/Violet/Pink | #F8D7F5,#D7C4FF,#7151A6,#FF8CA5 |
| CTA | giữ cũ | Coral→Pink→Lavender,ink#24112D | gradient ngoài + lớp nền kiểm soát tương phản | capsule gradient dịu,pressed state rõ |
| depth | giữ cũ | radial shapes/shadow nhẹ | panel translucency + rim | raised/inset gradient + border |

Shared: spacing8/12/16/24/32dp; tap48dp trở lên; CTA56dp; field min56dp,corner20–24dp; card24–28dp; capsule50%; form max480dp. Sans-serif hệ thống hỗ trợ tiếng Việt; title28–36sp,body16sp,label14sp; ưu tiên wrap/scroll hơn font tự co nhỏ. Không dùng font/network dependency mới.

**Contrast tính ở audit:** Soft ink#3C2458 trên#F5E9FF=11.34:1; muted#7151A6=5.24:1. Chữ trắng trên coral#FF8A7A chỉ2.29:1, không dùng cho text thường. Ink#24112D trên ba stop CTA đề xuất#FF8A7A/#FF4FA0/#B78AFF lần lượt7.68/5.76/6.78:1. Đây chỉ kiểm tra cặp màu/stop đặc, **không** chứng minh toàn gradient/composite/accessibility đã PASS. Khi triển khai phải đo cả vùng nội suy, alpha trên nền sáng/tối, ripple vàdisabled/error/focus.

Glass light đề xuất panel alpha0.90,dark0.92 làm điểm bắt đầu; nếu tương phản không đạt thì tăng độ đặc hoặc thêm text-plate, không giữ alpha thấp chỉ để giống mẫu. OnSurface dựa trên panel thực tế, không mặc định trắng trên cyan. Original Login giữ fixed-dark như hiện tại để không thay fallback ngầm; ba style mới hỗ trợ light/dark riêng. Nếu muốn Original Login cũng sáng/tối cần duyệt riêng khác biệt này.

## E. Native rendering và giới hạn

- Gradient: XML GradientDrawable/RippleDrawable, Canvas Linear/RadialGradient cho décor tĩnh. Tạo shader khi size/config đổi, không tạo bitmap/blur mỗi frame. Dùng backdrop dưới nội dung và giảm alpha ở Camera, tuyệt đối không đặt tint/gradient lên PreviewView hoặc ảnh thật.
- Glass: mô phỏng frosted bằng lớp màu có alpha,gradient/rim và elevation nhỏ; không RenderScript/blur nặng/RenderEffect toàn cây. Không cần nâng API hoặc dependency. Có thể giống ngôn ngữ kính nhưng không hứa blur/3D chính xác như ảnh dựng.
- Soft: layer-list,viền và gradient nổi/lõm nhẹ, elevation nhỏ có clip bounds kiểm soát;pressed đổi fill/shadow có ripple. Border/focus vẫn hiện rõ, không dựa vào bóng để người dùng nhận biết nút.
- MaterialButton giữ MaterialShapeDrawable/tint/shape của nó. Không gán tùy tiện custom background rồi hứa còn đầy đủ Material states. CTA gradient AppCompatButton sẵn có tiếp tục đường drawable/ripple riêng, có token tương đương; buttons Material solid/tonal sử dụng Material attrs. Không ép mọi control thành custom View.
- Shared loading/empty/error/success có semantic color, text/icon riêng; đừng chỉ dùng màu báo trạng thái. TalkBack selection có tên/theme/đang chọn và checkmark; trang trí importantForAccessibility=no; focus/keyboard/touch48dp.
- Widget chỉ đề xuất đổi màu placeholder/khung/text đơn giản trong giới hạn RemoteViews. Giữ nguyên ảnh/bitmap budget/Worker và thông tin riêng tư; không thêm blur/custom shader/themed Activity view vào widget. Chưa triển khai widget theme trong01–02.

## F. Lưu lựa chọn và lifecycle an toàn

Chooser truy cập từ Hồ sơ→Cài đặt giao diện và nút gọn tại Login, không thêm tab.4card native có thumbnail vẽ từ cùng tokens, tên/mô tả/check; chọn preview chưa ghi ngay, **Áp dụng/Hủy** rõ. Mode System/Light/Dark là control riêng. Save failure giữ lựa chọn hiện hành và cho retry; không toast thành công giả.

Không restart process/task,không clear session,không gửi auth request vì đổi style. ThemeManager không ghi token/ảnh hoặc cache nghiệp vụ. Resource references trong các layout ép theme phải được chuyển có kiểm thử từng màn, không giả định manager đơn độc đủ.

**Áp dụng theo revision và safe lifecycle, không recreate tất cả trên onResume:**

1. Chooser không có draft nghiệp vụ: persist thành công rồi recreate chooser để phản hồi đầy đủ.
2. Parent Main ở trạng thái idle/safe: reconcile revision đúng một lần khi RESUMED và FragmentManager chưa save state; giữ tab. Nếu chưa chứng minh bảo toàn form/camera qua test, giữ pending thay vì ép recreate.
3. Preview/upload/capture,form auth có password hoặc profile đang sửa: không ép recreate. Store pending revision, hiển thị thông báo áp dụng khi kết thúc thao tác/mở màn an toàn; Activity mới nhận theme mới trước inflate. Không xóa hoặc đưa password vào Bundle để giữ theme.
4. `PreviewViewModel.onCleared` hiện dọn draft; SavedStateHandle không đồng nghĩa giữ worker/download/pending payload qua mọi thay đổi lifecycle. Không sửa cleanup hoặc idempotency để phục vụ theme. `MainActivity.clearPages` cũng có thể clear Fragment ViewModels: regression bắt buộc trước bật recreate Main.
5. AppCompat/system night mode vẫn có thể tạo configuration change do OS; kiểm thử riêng, không tuyên bố trì hoãn app-style là đã giải quyết mọi OS-recreate. Nếu cần đổi nghiệp vụ để giữ draft phải xin scope riêng, không lén sửa ViewModel.

## G. File dự kiến (chưa được tạo ở01)

- `ui/theme/AppTheme.java`, `ColorMode.java`, `ThemeManager.java`, `ThemePreferences.java`, `ThemeRepository.java`, `ThemeViewModel.java`, `ThemedActivity.java`, `ThemeSettingsActivity.java`.
- `res/values/theme_attrs.xml`, `theme_tokens.xml`, `theme_styles.xml`, `theme_strings.xml`; `values-night/theme_tokens.xml`; `layout/activity_theme_settings.xml`, `item_theme_option.xml`; native thumbnail/drawable selectors. File tách thêm chỉ khi thật cần, không mỗi theme nhân bản mọi màn.
- Tái sử dụng/refactor có chọn lọc LoginBackdropView/PremiumBackdropView và6style/color families hiện tại; giữ branches Original. Layout auth/Main/camera/preview/history/friends/profile/viewer và item layouts chuyển token đúng bước04–08.
- `MainActivity`, `AuthFormActivity`, `PreviewActivity`, `MomentViewerActivity` tích hợp apply-before-inflate; Login/Register/Recovery liên kết settings và themed visual state; ProfileFragment mở chooser. Manifest chỉ thêm Activity không exported. Không sửa networking/session/FCM/server/DB/business Repository/ViewModel.
- Test mới dự kiến ThemePreferencesTest,ThemeManagerTest,ThemeSelectionUiTest,ThemeScreensUiTest; sử dụng lại LoginReference/AccountRecovery/PremiumScreens/Camera/Preview/Session privacy tests phù hợp. Snapshot/token contracts cho Original phải cập nhật có chủ đích, không xóa assertion hiện tại để né regression.

## H. Roadmap và gate từng bước

| Bước | Phạm vi | Gate trước dừng |
|---|---|---|
|01 hiện tại | Audit/architecture/tokens/fileplan | Báo cáo,XML audit,baseline; không source change |
|02 cần duyệt | IDs/prefs/manager/resolver/test,Original fallback | Unknown ID,write/read,reload,settings tách token; chưa tự làm chooser |
|03 | Chooser4cards,preview,Apply/Cancel,entrypoints | Chọn/hủy/lưu lỗi,persistence,không logout; chỉ ThemeSettings mới có UI hoàn chỉnh |
|04 |3style Login +Original |4style×2mode,IME/validation/loading/eye/navigation,screenshot |
|05 | Register/Forgot/Verify/Change | Fielderrors,secret non-persistence,reset-only routing giữ nguyên |
|06 | Camera/Preview/recipients/navbar/viewer | Capture/draft/caption/selection/upload/retry/key/tab/rotation không mất |
|07 | Received/Sent/Friends/items | Theme context adapter/dialog,scroll/paging/images/errors |
|08 | Profile/settings/shared components | Unsaved name/avatar picker/session,dialogs/snackbar,contrast |
|09 | Regression/performance/consistency | Emulator đầy đủ; máy thật khi có,không tuyên bố OEM nếu chỉ AVD |

Sau mỗi bước build/test/báo cáo rồi dừng, không tự chuyển. Recovery A/Gmail blocker không được coi đã PASS vì chuyển UI task; phầnB validation vẫn chưa được duyệt.

## I. Tiêu chí nghiệm thu tương lai và kiểm tra01

Khi triển khai: assembleDebug +unit liên quan +lint/XML/ViewBinding; release verify giữ HTTPS. Matrix4style×2mode và System mode transition, viewport280/360/840dp,font1.0/1.5/2.0,TalkBack/focus/tap48dp. Persist qua app reopen/logout/login/reboot; compare session store trước/sau **không in token**,auth request count không tăng vì theme.4tab giữ nguyên; CTA/text không che ảnh thật. Không mất draft/key/recipients/progress khi chọn theme ở thời điểm nhạy cảm. Screenshot cùng viewport/background so với reference, nêu khác biệt thay vì tự chấm pixel-perfect.

Performance: baseline trước/sau cùng AVD/API,ít nhất5cold launches và nhiều lượt tab switch; theo dõi startup/frame/memory,không render loop/bitmapcache vô hạn. Gate đề xuất startup median không tăng quá10% hoặc100ms (ngưỡng thực tế cần chốt theo nhiễu máy đo),không jank kéo dài từ décor; không lấy headless AVD làm bằng chứng hiệu năng điện thoại tầm trung.

Phiên01 đã chạy: skill UI UX Pro Max `--design-system` và domainstyle; Python có sẵn, lỗi console cp1252 được sửa bằng PYTHONIOENCODING=utf-8 trong process. Chỉ lấy nguyên tắc contrast/depth,loại gợi ý landing/testimonials/CSS/webfonts không phù hợp native.92XML parse0invalid; tính contrast cặp màu nêu trên. AVD Medium_Phone read-only/headless,1.5GB/2core,1080×2400; ADB unauthorized đầu được giải quyết bằng restart daemon cùng môi trường (không xóa AVD/keys). Cài APK/test APK đã build ở phiên trước,không build mới hoặc sửa appsource trong01.

Baseline4Login tests lượt đầu có1FAIL “Physical IME visible”; AVD đang để show_ime_with_hard_keyboard=0. Đổi tạm1 chỉ trên AVD read-only,rồi chạy lại **4/4 PASS21.68giây**, sau đó trả setting về0. Log `.local/artifacts/theme-audit-baseline-retest.log`; ảnh tại `.local/artifacts/theme-audit-baseline-final/`, đã xem Login/IME. Artifacts ignored `.local/artifacts/theme-audit-*`. Baseline chỉ Original, **chưa có3theme mới nên không có screenshot/test PASS cho chúng**. Không test login/backend/Gmail/camera upload trong01.

Nguồn kỹ thuật: [Android themes/styles và thứ tự ưu tiên](https://developer.android.com/develop/ui/views/theming/themes), [giới hạn widget](https://developer.android.com/develop/ui/views/appwidgets/overview). Những cặp token/alpha/lifecycle ở trên là đề xuất cho phê duyệt, không phải quyết định đã triển khai.

**Dừng chờ duyệt UI-THEME-02 và xác nhận ánh xạ Gradient=ảnh3,Glass=ảnh2,Soft=ảnh1.**

Review cuối: hash317file source/script/deploy trước/sau ghi audit không đổi,không file bị mất; chỉ4filedocs thay đổi trong phiên. Git diff --check0,link local không thiếu,artifact được ignore. Đã đóng đúng AVD do phiên khởi chạy để giải phóng RAM,không xóa AVD hoặc dữ liệu người dùng.
