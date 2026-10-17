# Taktak — M3E-02: Register

Ngày:17/10/2026,Asia/Saigon. **PASS local/USB**, người dùng duyệt riêng “màn2 — Register”. Dừng trước M3E-03 Camera. Nhánh `chore/push-current-project`,HEAD5b6b520; có17 thay đổi của M3E-01 chưa commit trước phiên, được đọc và bảo toàn. Không commit/push/deploy.

## UNDERSTAND → PLAN

Đối chiếu AGENTS,TAKTAK_PROJECT,PROJECT_DECISIONS,PROJECT_STATUS,TASKS,SESSION_HANDOFF và QA_M3E_LOGIN với source/test/Git. Giữ Java/XML/ViewBinding,Material1.14.0 và design system đã duyệt; không dependency/SDK/backend/DB/API/JWT/FCM hoặc thay đổi nghiệp vụ đăng ký.

Tái sử dụng UI UX Pro Max từ audit và android_ui_verification đã đọc: native labels/48dp targets,contrast theo foundation,light/night,scroll khi bàn phím/chữ lớn; dùng hierarchy và ảnh thật USB. Không dùng CSS/Compose/Kotlin. Giữ một form5trường,không wizard/tính năng mới. Theme opt-in Register; không thay application/Main/Camera/Friends theme.

## IMPLEMENT

- `Theme.TakTak.M3.Register` kế thừa foundation Expressive màn1 và gắn riêng RegisterActivity trong Manifest.
- `activity_register.xml`: cùng brand badge/Coral/Cream/Lavender,headline32sp/body16sp,field bo20dp,CTA56dp/action48dp và max-width480dp. ConstraintLayout + NestedScrollView,form dài có thể cuộn khi viewport thấp hoặc font lớn.
- Nhóm “Thông tin của bạn” và “Bảo mật tài khoản”; helper/label/autofill/IME/password-toggle giữ đủ,icon vector person/username cùng nét với mail/lock. Hai heading mới được đánh dấu accessibility bằng ViewCompat trong RegisterActivity; đây là thay đổi Java duy nhất của màn2.
- Progress slot cố định,notice Lavender live-region sát CTA. Giữ ID binding,validator,normalization,loading lock,API thật,password clear khi success/recreate và điều hướng về Login. Không tự login sau đăng ký.
- Sau khi Login/Register đều đã chuyển M3E,đã tìm mọi tham chiếu và **gỡ `res/values/auth_styles.xml` với6style Taktak.Auth đã hết sử dụng**. File cũ có trong Git HEAD5b6b520 nên khôi phục được; không xóa thay đổi M3E-01 hoặc dữ liệu người dùng. Không suppress lint.

## VERIFY

Máy thật USB8e90fdff,23129RAA4G/Android15 API35,1080×2400. Baseline trước sửa được chụp từ Login→Register hiện có bằng node/bounds; không chạm phiên cá nhân. APK cài `install -r`,không clear data. Debug dùng127.0.0.1:8080 qua adb reverse,backend smoke với PostgreSQL/JWT tạm riêng. Không dùng/reset DB dev.

| Kiểm tra thực chạy | Kết quả | Artifacts ignored dưới `.local/artifacts/m3e-register/` |
|---|---|---|
| Build đầu:debug/test/release +unit/lint | BUILD SUCCESSFUL41s;28unit đạt;lint0errors/9warnings gồm6style vừa hết dùng | `gradle.txt` |
| Build sau dọn style:debug/test/release +unit/lint | **BUILD SUCCESSFUL30s**;lint debug/release0errors/3warnings cũ;unit task thực chạy | `gradle-final.txt` |
| Test APK/lint sau chỉnh ảnh bao gồm action cuối form | **BUILD SUCCESSFUL22s** | `test-build-final.txt` |
| Unit XML hiện tại | **28/28**,0failures/errors/skipped | app/build/test-results/testDebugUnitTest |
| RegisterM3Test,offline flag bật | **4/4 PASS57.490s**,không skipped; lượt đầu cũng4/4 PASS54.381s | `usb-final.txt`, lịch sử `usb-initial.txt` |
| Regression LoginM3Test3 ca chọn lọc +LauncherAuthAcceptanceTest2 +CameraLayoutTest1 +FriendsLayoutTest1 | **7/7 PASS29.510s** | `usb-regression.txt` |

Bốn ca Register:

1. Inflate/measure native ở light/dark,font1.5×,viewport280×560/360×720/840×420dp;hiển thị error dài trên cả5field và network notice. Không tràn ngang/clipping text;form≤480dp,CTA≥56/action≥48;content dài hơn viewport và vẫn giữ action cuối trong scroll content. Đây là mô phỏng context,không xoay máy thật hoặc test tablet thật.
2. Chuyển night-mode riêng Activity thật,chụp đầu/cuối form;validation rỗng,sai username/email,confirmation có dấu cách khác password;loading khóa7control;toggle confirmation thật rồi recreate. Password/confirmation rỗng sau recreate,name giữ nguyên;hai heading được đánh dấu. Không đổi night-mode/animation hệ thống;không chụp screenshot lúc password đang hiện rõ.
3. Mở IME thật ở field cuối;local visible bounds đầy đủ và global bottom không dưới mép bàn phím. Đóng IME,cuộn tới Login action,đi/về Login↔Register và store vẫn rỗng.
4. Điền dữ liệu QA duy nhất,mật khẩu ngẫu nhiên chỉ RAM,email/username uppercase có khoảng trắng để kiểm tra normalization. Host tắt riêng HTTP connector smoke qua stdin→UI báo network error và không có phiên. Host bật lại→người dùng bấm retry bằng submit→success;hai password xóa,Register vẫn mở,không tự lưu token. Raw API login200 chứng minh tài khoản thật đã tạo,raw refresh được revoke bằng logout204,chỉ trong RAM. Gửi lại form cùng username/email→409/message conflict,store vẫn rỗng. Không log password/token hoặc đưa credential vào argv/file.

Regression Login vẫn dùng test màn1,không sửa test cũ. 28unit gồm AuthSession12,FriendRepository8,AuthInputValidator7,Example1. Không cộng các lượt lặp thành ca độc lập. Không chạy lại87backend tests vì không đổi backend; smoke chỉ test-compile/start để phục vụ kiểm thử Android. Lint3warnings cũ:PluralsCandidate và black/white unused.

Ảnh trước `.local/artifacts/m3e-register/before.png`;10 ảnh cuối trong `final-screens/`:light-top/light-bottom,dark-top/dark-bottom,loading,keyboard,return-login,offline,success,conflict. Đã xem trực tiếp light đầu/lượt đầu và dark-bottom/keyboard/success cuối;không có overlay. Lượt đầu ảnh “bottom” chưa bao gồm action Login vì submit đã trong viewport;test được chỉnh cuộn tới switchButton rồi chụp và chạy lại trên APK cuối. Không chỉnh sửa ảnh hoặc tắt kiểm tra visibility.

Lệnh build tại `android-client/` sau khi chọn JDK qua `scripts/Use-TaktakJava.ps1 -Target Android`:

```powershell
./gradlew.bat "-Dorg.gradle.java.installations.paths=$env:JAVA_HOME" '-Dorg.gradle.java.installations.auto-detect=false' '-PTAKTAK_DEBUG_BASE_URL=http://127.0.0.1:8080/' '-PTAKTAK_RELEASE_BASE_URL=https://api.example.invalid/' --offline --no-daemon assembleDebug assembleDebugAndroidTest testDebugUnitTest lintDebug assembleRelease lintRelease --console=plain
```

Native command được chạy với ErrorActionPreference=Continue quanh Gradle,kiểm tra exit code/BUILD SUCCESSFUL;SDK/cache cần quyền ngoài sandbox. Release dùng HTTPS placeholder,chưa runtime/signing/deploy. Để lặp ca offline cần smoke backend riêng và host điều khiển stdin;chạy với `-e taktakRegisterOffline true -e taktakQaRun <unique-run>` rồi đợi marker `M3_REGISTER_READY_OFFLINE_<unique-run>`,tắt/bật connector trong cửa sổ chờ45s. Không áp dụng kịch bản này lên server dev/production đang dùng chung.

## REVIEW / HANDOFF

9 file Android trong phạm vi màn2:

- Sửa Manifest,RegisterActivity,activity_register.xml và foundation m3_styles.xml đã có từ màn1.
- Thêm register_strings.xml,ic_m3_person.xml,ic_m3_username.xml và RegisterM3Test.java.
- Gỡ auth_styles.xml đã hết tham chiếu.

5 tài liệu:QA_M3E_REGISTER mới,PROJECT_STATUS/TASKS/PROJECT_DECISIONS/SESSION_HANDOFF cập nhật. Baseline SHA256 trước sửa gồm191file tracked và untracked được lưu ignored ở `baseline.json`;đối chiếu khi review để giữ thay đổi màn1. Không đổi Login layout/activity/test hoặc AuthFormActivity trong màn2;không đổi ViewModel/Repository/API/server/SDK/Firebase. Các thay đổi cũ trong Manifest/docs/m3_styles được mở rộng sau khi đã đọc,không ghi đè toàn bộ.

Review cuối xác nhận8file có sẵn sửa,5file mới,1file style cũ gỡ,đúng14file của màn2. **56file server giữ SHA256**,các file Login/ViewModel/Repository/SDK không đổi so với baseline. 13file hiện có được quét mẫu credentials/conflict/whitespace:không phát hiện;6XML parse hợp lệ,77link local hợp lệ,git diff --check đạt,index rỗng. Firebase JSON giữ hash đã xác nhận từ màn1,không đưa nội dung key vào báo cáo;artifacts/credentials vẫn ignored. Đây là kiểm tra chọn lọc,không phải audit tuyệt đối mọi lịch sử Git.

Cleanup thực tế: backend nhận Enter và dừng **exit0**,media0row/0file/0recipient,cổng8080 không lắng nghe;raw QA refresh revoke và regression fixture logout. Reverse8080 gỡ,cold launcher1452ms về Login rồi mở Register bằng UI;hierarchy xác nhận displayNameInput,handoff.png chụp thật. Điện thoại để ở Register mới cho người dùng xem. Không đổi cấu hình hệ thống,xóa app data/DB/cluster hoặc push/commit. Test data/artifacts giữ ignored trên disk.

Giới hạn:chưa runtime API26/API37 deny local-network,tablet/landscape thật,font2×,TalkBack toàn luồng,load/HTTPS runtime/release signing. Không suy rộng context font1.5× thành nghiệm thu trên mọi thiết bị. Backend QA đã dừng;muốn đăng ký/dùng app dev tiếp cần backend/devDB và reverse phù hợp. Không có thao tác thủ công bắt buộc để hoàn tất màn2.

**M3E-02 PASS. Đề xuất đúng màn tiếp theo:M3E-03 — Camera + trạng thái phiên;chờ người dùng xác nhận,không chuyển5A hoặc thêm chức năng camera mới.**
