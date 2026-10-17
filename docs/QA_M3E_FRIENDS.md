# Taktak — M3E-04: Bạn bè / Friends

Ngày: 17/10/2026, Asia/Saigon. **PASS local/USB** trong phạm vi người dùng duyệt riêng “màn 4 — Bạn bè/Friends”. Nhánh `chore/push-current-project`, HEAD `5b6b520`. Worktree có thay đổi chưa commit của M3E-01/02/03; snapshot SHA256 **198 file tracked/untracked có sẵn** được tạo trước sửa tại `.local/artifacts/m3e-friends/baseline.json` (ignored). Không commit/push/deploy hoặc triển khai 5A.

## UNDERSTAND → PLAN

Đã đọc AGENTS, toàn bộ TAKTAK_PROJECT/roadmap, các quyết định, hồ sơ trạng thái/bàn giao và source Friends/layout/adapter/ViewModel/test. Friends 4C đã hoạt động với search, incoming, accept/reject, friends, unfriend, block và pagination; không tạo lại nghiệp vụ. Giữ Java/XML/ViewBinding, Material Components **1.14.0** đã khóa; không đổi backend, DB/migration, API/JWT/FCM, Repository/ViewModel, SDK hoặc dependency.

Dùng **UI UX Pro Max** cho tính nhất quán, phân cấp, contrast và touch target; **android_ui_verification** cho đo thiết bị, hierarchy/bounds, thao tác và ảnh thật. Python3.12.10 đã có. CLI design-system lần đầu lỗi encoding cp1252; đặt PYTHONIOENCODING=utf-8 trong process rồi chạy lại được. Gợi ý landing-page/CSS/font tải ngoài không phù hợp native và không được áp dụng; giữ hệ màu Coral/Cream/Lavender và font hệ thống đã duyệt.

Kế hoạch: giữ một RecyclerView tái sử dụng hàng, phân nhóm rõ kết quả/lời mời/danh sách bạn; header thu gọn khi cuộn để không chiếm cố định chiều cao ở viewport nhỏ; tối đa480dp. Chỉ thay presentation, rồi build/unit/lint, API/UI regression và kiểm tra ảnh trước/sau trên USB.

## IMPLEMENT

- `fragment_friends.xml`: scoped Theme.TakTak.M3.Friends, ConstraintLayout giới hạn480dp, CoordinatorLayout/AppBarLayout và RecyclerView scrolling behavior. Header chứa các action cũ, headline32sp, ô username bo20dp có icon search; Tìm filled và Tải lại tonal cao≥56dp. Header thu gọn/mở lại theo nested scroll, không chuyển danh sách sang ScrollView hoặc tắt tái sử dụng hàng.
- `item_friend.xml`: MaterialCardView bo24dp, identity/avatar Lavender48dp, tên và username tự xuống dòng; primary Expressive56dp, secondary tonal và tùy chọn text≥48dp. Giữ binding ID và đủ action theo role; không thêm chip trạng thái hoặc thao tác mới.
- `item_friends_section.xml`: heading TitleLarge, body và nút tải thêm tonal; giữ section heading accessibility của adapter. Avatar vẫn drawable local qua Glide, không dựng API/avatar URL mới.
- `FriendsFragment.java`: progress slot cố định (INVISIBLE khi idle), notice success/error dùng semantic token kèm text; dialog xác nhận lấy context từ root M3E thay vì Activity theme cũ. Không đổi search/mutation/session/lifecycle logic.
- `MainActivity.java`: root surface M3E cả Camera/Friends để mép hệ thống/viewport rộng không lệch nền; mọi session gate/navigation giữ nguyên. Activity base theme chưa đổi; Camera/phiên/Friends opt-in trên cây View, Login/Register giữ theme riêng.
- Mở rộng `m3_styles.xml` bằng Friends theme, thêm vector search. Sau tìm mọi tham chiếu, gỡ **hai TextAppearance Camera cũ** vừa hết dùng khỏi camera_styles.xml; không xóa file. Các style này khôi phục từ Git được. Bỏ root background Friends vẽ thừa vì Main đã cung cấp surface, không suppress lint.
- Thêm `FriendsM3Test` ba ca với ownership guard; không thêm test hook trong production, không sửa test cũ hoặc nới assertion nghiệp vụ.

## VERIFY

USB `8e90fdff`, Xiaomi23129RAA4G, Android15/API35, **1080×2400** đo bằng wm size. Ban đầu điện thoại ở launcher; QA mở Taktak bằng shell launcher hiện có, không điều khiển ứng dụng khác. APK cài `install -r` thành công, không clear data. Fixture kiểm tra ownership trước thay phiên, dùng tài khoản riêng/password ngẫu nhiên trong RAM, logout/revoke sau test.

Smoke backend `Start-LocalSmokeServer.ps1 -Port 8080` dùng PostgreSQL/JWT/storage tạm riêng; debug loopback qua USB reverse. Host tắt/bật riêng HTTP connector bằng stdin để kiểm tra offline, không thay Wi-Fi/firewall/server dev hoặc production. Không chạy lại87 backend tests vì source backend giữ nguyên; test-compile/start smoke không được gọi là backend suite PASS mới.

| Kiểm tra thực chạy | Kết quả | Artifact ignored dưới `.local/artifacts/m3e-friends/` |
|---|---|---|
| Baseline APK trước sửa: empty/navigation và invitations lifecycle | 2/2 PASS,33.904s; chỉ là baseline | `baseline-ui.txt`, `before/` |
| Build debug/test/release +unit/lint đầu | BUILD SUCCESSFUL30s,0errors/6warnings | `gradle.txt` |
| Build sau dọn style/overdraw và sửa harness | **BUILD SUCCESSFUL26s**,unit thực chạy | `gradle-final.txt` |
| Unit XML cuối | **28/28**,0failures/errors/skipped | `app/build/test-results/testDebugUnitTest` |
| Lint debug/release cuối | **0errors/3warnings cũ**: PluralsCandidate và black/white unused | `app/build/reports/lint-results-*.txt` |
| Test APK/lint sau chốt swipe trong viewport | BUILD SUCCESSFUL12s | `test-build-viewport.txt` |
| FriendsM3Test3 +FriendsLayoutTest1 cuối | **4/4 PASS,23.898s**,không skipped | `m3-ui-viewport.txt` |
| Năm ca FriendsAcceptanceTest core chọn rõ method | **5/5 PASS,49.375s** | `friends-core.txt` |
| Friends offline/retry,flag bật và host điều khiển connector | **1/1 PASS,45.426s** | `offline-retry.txt` |
| Regression LauncherAuth2 +CameraM3 layout1 +CameraLayout1 +LoginM3 3 +RegisterM3 3 | **10/10 PASS,48.375s** | `auth-camera-regression.txt` |

**Tổng20 ca instrumentation đạt trên APK redesign**, không cộng baseline hoặc lượt lặp thành ca mới. Không chạy nguyên suite chứa ca hai thiết bị/offline thiếu điều kiện rồi nhận skipped là PASS. Các lần thất bại dưới đây không được tính vào kết quả đạt.

Ba ca mới:

1. Native inflate/measure light/dark,font1.5×,viewport280×560/360×800/840×420dp; card với tên dài có dấu và username tổng hợp dài để stress layout, không gửi chuỗi này lên API. Không tràn ngang/cắt text, content≤480dp,list giữ viewport≥100dp và search/refresh≥56dp. Năm cặp màu surface/text/Lavender/error/success contrast≥4.5. Đây là context mô phỏng, không tablet/landscape thật hoặc font hệ thống đã đổi.
2. API tạo hai tài khoản QA và lời mời thật → UI incoming sáng/tối. Swipe thật qua shell input trong **vùng nội dung đã đo**, xác nhận AppBar top<0 rồi swipe ngược top=0, ảnh trước/sau cuộn. Renderer busy bằng reflection trong test kiểm tra progress và khóa search/query/refresh; không nhận là request mạng đang chậm thật. Sau đó accept qua ViewModel/API thật → incoming rỗng,friend có1,notice success đúng text/màu. Night mode chỉ thay trong app và khôi phục trong finally.
3. Mở IME thật tại username, đảm bảo toàn bộ EditText trên bàn phím; IME Search gọi API và hiển thị empty result đúng. TextInputEditText có thể mở rộng global rect tới TextInputLayout, nên so containment với vị trí/kích thước EditText thật, không so hai chiều cao khác nghĩa.

Năm ca core giữ nguyên kiểm tra empty/invalid/no-result,recreate/back/logout; gửi/accept/reject/unfriend/block và cancel/confirm; duplicate409/refresh không replay; revoke refresh về Login; đổi tài khoản bỏ snapshot cũ. Row/popup/dialog được click bằng Espresso với điều kiện visibility sẵn có, không tắt animation hay nới target. Offline test giữ phiên mã hóa, notice lỗi rồi Tải lại hồi phục khi server online; không lặp mutation.

Regression Camera là đo layout/theme boundary, không chụp/upload ảnh thật mới trong màn4. Không nhận kết quả3D/M3E03 lịch sử là vừa chạy lại. Login/Register chọn ba method không có offline flag; launcher test xác minh auth/encrypted session/logout thật.

## Lỗi QA đã xử lý

- UI lượt đầu4ca/2FAIL45.979s: sau đổi theme, test chờ Camera button trong khi Friends đã được restore; phép đo IME so global parent rect với child height. Sửa harness theo lifecycle và hình học thật, chạy lại4/4 đạt17.180s trước bổ sung swipe; không sửa auth/navigation để qua test.
- Test swipe ban đầu dùng UiDevice nhưng dự án không có dependency uiautomator,compile FAIL18s. Chuyển sang shell `input swipe` có sẵn; **không thêm thư viện**. Build lại đạt12s (`test-build-swipe.txt`).
- Các lượt swipe đầu1FAIL do Taktak không còn RESUMED sau gesture; bổ sung phép chờ recreate chưa giải quyết được (`m3-ui-swipe.txt`, `m3-ui-verified.txt`). Tọa độ sát mép dưới có thể kích hoạt cử chỉ hệ thống (suy luận từ app rời foreground sau swipe). Sửa helper intersect bounds với cửa sổ/insets và swipe từ1/4 đến3/4 viewport; log chỉ rect hình học, không PII. **4/4 cuối23.898s đạt**, AppBar collapse/expand thật cả hai theme. Không bỏ ca swipe hoặc thay cài đặt gesture hệ thống.
- Lint đầu6warnings: bỏ root background redundant và gỡ hai typography hết tham chiếu; cuối trở lại3warnings cũ,không baseline/suppress.

Ảnh trước `before/incoming.png`, `before/search-result.png`, `before/unfriend-confirm.png`; các ảnh tương ứng thuộc hai ca baseline vừa chạy. Những file lịch sử khác pull cùng thư mục không được tính baseline mới. Ảnh cuối `viewport-screens/` có9file: light/dark incoming,light/dark scrolled,light/dark busy,success,keyboard,search-empty. Đã xem trực tiếp light incoming,dark scrolled,success và ảnh IME lượt trước. `after-core/` có incoming/search/friends/conflict/unfriend-confirm/block-confirm/blocked-hidden/expired/account-switch/logout và offline/retry; đã xem block-confirm và offline,không overlay. Không chỉnh ảnh hoặc đưa ảnh/tài khoản QA vào Git.

Lệnh build ở `android-client/`,chọn JDK bằng `Use-TaktakJava.ps1 -Target Android`:

```powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

Release HTTPS placeholder chỉ xác minh build/lint,không endpoint production/signing/deploy. Native exit code được đối chiếu BUILD SUCCESSFUL,không coi javac stderr Note là failure. Offline test chọn `FriendsAcceptanceTest#networkFailureKeepsSessionAndRefreshRecovers` với `-e taktakFriendsOffline true`; đọc marker FRIENDS_READY_FOR_DISCONNECT/FRIENDS_READY_FOR_RESTORE lọc theo PID hiện tại rồi điều khiển stdin smoke. Không chạy uiautomator dump khi instrumentation đang giữ UiAutomation.

## REVIEW → HANDOFF

**10file Android** trong màn4: sửa MainActivity,FriendsFragment,fragment_friends,item_friend,item_friends_section,friend_avatar,m3_styles,camera_styles; thêm ic_m3_search và FriendsM3Test. **5tài liệu**: QA này mới,PROJECT_STATUS/TASKS/PROJECT_DECISIONS/SESSION_HANDOFF cập nhật. Không xóa file trong màn4; các deletion cũ màn1/2/3 vẫn được bảo toàn,không nhận là deletion mới.

FriendsAdapter/Glide requests,ViewModel/Repository/API/EncryptedSessionStore/CameraX/JWT/Firebase/server/DB/migration/dependency/Manifest và Login/Register/Camera layout giữ nguyên so với baseline. Base Main theme chưa migrate toàn cục; từng cây View đã opt-in M3E. Hai style typography cũ vừa hết dùng được gỡ khỏi file có sẵn sau khi đọc/tìm reference,khôi phục được từ Git.

Review cuối xác nhận đúng **12file có sẵn sửa +3file mới,0file xóa** trong màn4;56file server giữ SHA256. Quét15file hiện có không phát hiện mẫu API key/private key/JWT thực,conflict marker hoặc trailing whitespace;7XML parse hợp lệ,84link local hợp lệ,git diff --check đạt,index rỗng. Credentials/Firebase JSON và artifacts vẫn ignored. Đây là kiểm tra mẫu trong phạm vi,không audit tuyệt đối toàn bộ lịch sử Git.

Cleanup: smoke backend dừng **exit0**,0media/0privatefile/0recipient; không chụp/xóa ảnh hoặc DB dev. QA sessions logout/revoke,reverse8080 gỡ,night-mode app khôi phục; không thay camera permission/cài đặt bảo mật/animation/hệ thống. Cold launch **1312ms** về Login, hierarchy có emailInput và không friendsRecycler; `handoff.xml/png` ignored. Điện thoại để Login,không phiên QA hoặc server nền.

Giới hạn: chưa API26/API37 local-network dialog,TalkBack toàn luồng,font2×,tablet/landscape thật,UI phân trang hơn20,UI429 hết cooldown,hai thiết bị chạy lại,HTTPSruntime/signing/FCM. Pagination/Retry-After/generation vẫn có unit/regression,không suy rộng sang E2E chưa chạy. Không cần thao tác thủ công để hoàn tất màn4; dùng app dev tiếp cần backend/dev DB và kết nối phù hợp vì smoke đã dừng.

**M3E-04 PASS local/USB. Bốn màn đã có (Login/Register/Camera/Friends) đã redesign; không có History/Profile/widget mới được tạo. Đề xuất duy nhất: quay lại tiểu bước5A — hoàn thiện điều hướng bốn tab và CameraX theo roadmap, cần người dùng duyệt riêng. Dừng, không triển khai5A hoặc màn mới.**
