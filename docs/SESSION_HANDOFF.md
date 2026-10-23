# Taktak — Bàn giao phiên khôi phục

## Chốt APK/source theo yêu cầu — 23/10/2026

User cho phép build APK và commit trực tiếp main, không push. Đóng gói debug
ký bằng debug certificate, không tự tạo release keystore hoặc đổi BASE_URL.
Build/unit/lint check20s đạt; unit up-to-date với135 kết quả trước đó, lint0/85.
Giữ QA22+1 USB đã ghi nhận; không chạy lại trên điện thoại ở lượt đóng gói.
Không stage Word/BTL/diagrams/scripts báo cáo, cache Python hoặc secrets.
Giữ các hạn chế PARTIAL, không tuyên bố toàn dự án hoàn tất production.
[Thông tin APK/phạm vi commit](QA_FINAL_APK_20261023.md); đối chiếu Git log sau chốt.

## Video intro có tiếng — mới nhất23/10/2026

Người dùng cấp MP4/M4A và cho phép branded intro video. Đã bundle MP4 nguyên
253.483byte (AAC khớp M4A), dùng một MediaPlayer/TextureView; bỏ renderer520ms
và icon hệ thống để tránh hai logo. Settings Profile lưuFull/Quick+sound, preview/
skip; lifecycle release và session gate giữ nguyên. Build/JVM135/lint0error85warning.
USB22/22 PASS125,346s, cold riêng1/1 PASS7,192s; âm thanh được người dùng xác
nhận nghe/không lệch rõ. Session guard không đổi, prefs fixture khôi phục.
CODE COMPLETE/Android15 verified, matrix PARTIAL; xem
[QA_BRANDED_INTRO_20261023.md](QA_BRANDED_INTRO_20261023.md). Không commit/push.
Các đoạn bên dưới là lịch sử renderer cũ, không phải runtime hiện tại.

## Splash native — mới nhất23/10/2026

User đổi yêu cầu sang prompt splash; contrast Friends/History chưa sửa. Đã tích
hợp Starting theme/core-splashscreen1.2.0 vào MainActivity và exit520ms với logo
PNG sẵn có, không SplashActivity/đợi auth. Sáu test cuối6/6 AVD PASS15,483s,
hồi quy List/Chat10/10 PASS47,131s; JVM127/lint0error83warning, build cuối71s.
USB tiếp theo6/6 PASS8,281s, đã thấy đủ accents. Lượt đầu3/6 lỗi vì MIUI chặn
background start. Hồi quy List/Chat10/10 USB PASS76,832s; phiên giữ nguyên.
Chỉ đổi SplashRoutingUiTest sang shell launcher, build29s.
**PARTIAL** vì PNG có góc trắng và accents/brand sát logo; chưa sửa production.
Đừng đánh dấu DONE. Phiên cá nhân giữ nguyên. Xem
[QA_SPLASH_20261023.md](QA_SPLASH_20261023.md), ưu tiên điều tra/verify exit thực,
logo bounds/spacing, API26–30 và routing expired/logout trước nghiệm thu toàn phần.
Không commit/push.

## QA máy thật — mới nhất 23/10/2026

USB Xiaomi Android15: build check11s/72 up-to-date, install-r app/test Success,
28/28 PASS118,724s, không skipped. APK hash BF7C032D…62AA8E87 khớp local;
phiên mã hóa byte-for-byte không đổi, appearance được suite khôi phục.
Đã lưu36 ảnh fixture local và review các mẫu cần thiết, không đưa ảnh cá nhân vào Git.
Friends/History light còn muted text/icon nhạt, Recap mẫu chưa đổi. Chỉ QA và
docs, không sửa source/backend/commit/push/deploy. Lượt này không rerun JVM/lint.
[Chi tiết và giới hạn](QA_USB_UI_20261023.md). Ưu tiên tiếp theo: contrast hai màn này.

## Lint/UI — mới nhất 22/10/2026

Build35s/JVM127/lint0 error hai biến thể đạt; còn83/90 warning. Bổ sung1.038
bản dịch, guard2.256 chuỗi; sửa icon/contrast Chat, History tab font2, badge giả
và mô tả privacy. Giữ sửa atomic Attach trước đó; thêm diagnostic List và test
performClick. Người dùng đổi sang AVD khi USB mất; Medium_Phone bị khóa nên
tạo Taktak_Ui_QA riêng, không wipe/mở khóa AVD cũ. Runtime cuối **28/28 PASS,
105,929s**, đã review Chat/Inbox/New Chat và ảnh Friends; còn icon/hint Friends
light nhạt. Chi tiết ở
[hồ sơ QA](QA_UI_LINT_20261022.md); không claim USB hoặc toàn UI PASS.
Giữ file báo cáo Word/diagrams/scripts untracked; không commit/push/deploy.

## Sửa đồng bộ test — mới nhất22/10/2026

ChatThemeUiTest đợi canSend và dispatch Attach cùng main-loop, không retry
request/nới assertion; production giữ nguyên. Build23s/cài-r test APK đạt,
USB18/18 PASS90,45s, phiên không đổi. 127 JVM result0 failure/error, không claim
rerun nếu task up-to-date. Hồ sơ QA giữ FAIL trước sửa để đối chiếu. Không
commit/push; các hạn chế lint/visual/E2E thật vẫn còn.

## APK/USB — 22/10/2026, mới nhất

Diagnostic bổ sung: busyAtAction=true, cuối busy=false/expired=false/error0/
choices0. Test dispatch sau kiểm tra idle, bị ChatViewModel.run bỏ action vì
busy; UI thật có guard disable Attach. Đã xác định race harness của ca lỗi,
chưa sửa đồng bộ/test hoặc production. Diagnostic1/1FAIL22,805s. Xem hồ sơ QA.

App debug build20:49:19 đã cài-r USB, SHA khớp; JVM127/127, build48s/76 task.
USB17/18; attachment Chat dedup timeout, riêng1/1 cũngFAIL. Chưa xác định chắc
root cause; nghi race busy/action, cần diagnostic trước sửa. Phiên giữ nguyên
trong QA, không gửi dữ liệu cá nhân. Không sửa Java/XML, commit/push/deploy.
[Hồ sơ](QA_LATEST_APK_USB_20261022.md). Không coi build thành công là toàn QA đạt.

## Production rollout mới nhất — 21/10/2026

Người dùng duyệt cài script/SQL/unit. Đã cài restore.sh, reconcile-restore.sql,
drop-in30-restore-quarantine.conf và daemon-reload, không restart/restore/SQL.
HealthUP4 mẫu/PID84167 giữ nguyên; base unit/JAR/env không đổi, guard đã load.
Backup `/var/taktak/maintenance-backups/restore-security-20261021-aJiHvBQh`.
Chi tiết/hash/giới hạn tại [QA_RESTORE_PRODUCTION_ROLLOUT.md](QA_RESTORE_PRODUCTION_ROLLOUT.md).
Không cần thao tác thủ công ngay; restore/mở traffic sau restore cần duyệt
riêng, không tự chuyển gate hoặc commit/push. Mục trước rollout bên dưới là lịch sử.

## RESTORE-SECURITY-01 — bàn giao mới nhất 21/10/2026

Restore mới không autostart; yêu cầu loaded ExecStartPre, marker quarantine,
SQL max(snapshot,current)+1, giữ current credential/identity, revoke refresh
và bỏ recovery. Unknown account/overflow fail, rollback vẫn giữ cách ly.
SQL+HTTP local4/4, shell12/12, native QA9→10/ảnh/checksum/systemd guard đạt.
Backend full verify mới191/191, không failure/error/skipped, BUILD SUCCESS2:28.
Không sửa live script/unit/JAR hoặc restore DB chính. QA resources còn giữ,
chi tiết [QA_RESTORE_SECURITY.md](QA_RESTORE_SECURITY.md). Mọi production
rollout/mở lại traffic cần duyệt riêng; không commit/push hoặc tự chuyển gate.

## Bàn giao mới nhất — nghiệm thu tổng hợp 21/10/2026

**FAIL tổng thể**, xem [QA_ACCEPTANCE_ALL.md](QA_ACCEPTANCE_ALL.md).
Gmail đủ3 thư đã được người dùng xác nhận. SSH đã vào được, phát hiện FCMfalse/
thiếu credential; theo duyệt đã cài credential600/service user, bật FCM,
service cuối lượtactive/healthUP. Lượt timeout30s tự rollback rồi tăng wait120s
theo startup56–58s và chạy lại đạt; không đổi JAR/schema. USB thật1/1 đạt70,674s
sau sửa harness application Context. Bind grant thu hồi/QA logout. Ảnh logout
trống: visual chưa đầy đủ. Backend187/JVM127/USB matrix2+Chat fixture6 đạt.
Lint179 lỗi chưa sửa. Native backup/restore DB+ảnh QA đạt nhưng version9→7:
**không dùng restore hiện tại để mở snapshot ra public**. DB chính không restore.
Chưa crash outbox/mixedAPK/binary rollback/long-load/alert routing. Cần duyệt
sửa restore an toàn và regression token runtime; không suy ra mọi gate DONE.
Không tự commit/push/rotate secret hoặc purge tài nguyên QA còn giữ.

## Bàn giao USB vận hành — 21/10/2026

Người dùng đã duyệt chuyển tài khoản/tạo QA. USB Xiaomi Android15:9/9 cache/reply,
3/3 HTTPS read-only đạt; FCM thật thất bại sau foreground_upload (90s không
created event,100,628s tổng). Chưa có bằng chứng widget/background/delete qua
gate delivery. Đã thu hồi bind grant user0 và logout QA; không restore phiên
cá nhân tự động, người dùng đăng nhập lại. Không deploy/sửa VPS/commit/push.
Mới thêm OperationalConnectivityTest; sửa RealFcmWidgetAcceptanceTest để
foreground MIUI, timeout300s, marker không PII và cleanup ảnh thuộc test.
Cleanup ảnh bổ sung chỉ build, chưa chạy runtime; ảnh synthetic/tài khoản QA
cũ có thể còn backend, không purge theo prefix. SSH IP có host key đúng nhưng
key đăng nhập bị từ chối. Cần SSH key local hợp lệ để đọc outbox/FCM log trước
khi kết luận nguyên nhân hoặc sửa server. [Báo cáo](QA_OPERATIONAL_USB.md).

## Hiện hành — TRIỂN KHAI VPS THẬT & HARDENING-01, 21/10/2026

- **Triển khai máy chủ VPS thật (`remote.io.vn` / 14.250.250.68:11918)**:
  - Tự động sao lưu database PostgreSQL an toàn: `/var/taktak/backups/taktak_pre_v6_*.dump`.
  - Tải lên và thay thế file JAR mới 118MB (`taktak-server-0.0.1-SNAPSHOT.jar`), sao lưu bản cũ `taktak-server.jar.bak`.
  - Khởi động lại dịch vụ `taktak.service`, Flyway tự động migrate schema thành công lên `version 6 - durable push outbox` (0.138s).
  - Xác nhận HTTP healthcheck `{"status":"UP"}` 200 OK.
  - Xác nhận header Telemetry Correlation ID (`X-Request-ID`) phản hồi chuẩn xác.
- **Mã nguồn & Kiểm thử local/AVD**:
  - Đã có V6/outbox, retry lease+backoff, telemetry không nội dung riêng tư, cache không vượt quyền/phiên và tối ưu inbox 64→4 statement/request.
  - Backend verify cuối 187/187; targeted migration+budget 2/2; JVM 127/127; AVD 17/17 (145.302s).
  - Chi tiết hồ sơ và hướng dẫn bảo trì: [QA_SYSTEM_HARDENING.md](QA_SYSTEM_HARDENING.md), [HUONG_DAN_SU_DUNG_VPS_UBUNTU.md](HUONG_DAN_SU_DUNG_VPS_UBUNTU.md).

## Bổ sung USB thật — MOMENT-REPLY-01, 21/10/2026

Người dùng chuẩn bị điện thoại và cho phép QA ảnh/bình luận trong RAM, không
camera, logout hay gửi tin thật. Xiaomi 23129RAA4G Android15, 1080×2400:
**7/7 PASS, 36,257s**, runner PID26533 kết thúc 0 failed/ignored, không có
FATAL EXCEPTION trong PID này. Build test APK cuối BUILD SUCCESSFUL17s.
Skill android_ui_verification hướng dẫn review ảnh/trạng thái; không chạy AVD.

Chỉ sửa MomentReplyUiTest và cập nhật bốn tài liệu QA/trạng thái trong lượt này.
Harness thêm opt-in USB, kiểm tra session byte-for-byte và phục hồi appearance;
MIUI chặn ActivityScenario từ nền nên đưa Main host với fixture đã inject lên
foreground trước mỗi launch, không đổi bảo mật/cấu hình OEM hoặc code ứng dụng.
Hai lượt đầu bị chặn/dừng chưa PASS; lượt cuối đủ7/7. Cài -r giữ dữ liệu;
fingerprint session trước cài/sau QA trùng nhau. Không lộ hash/token trong báo cáo.

Log .local/artifacts/moment-reply-usb-final-2.log; build
moment-reply-usb-harness-build-2.log; 7 ảnh tại moment-reply-usb-images đã review.
Ảnh fixture tím là có chủ đích. Ghi nhận icon back Chat khó nhận biết và tương
phản timestamp/input thấp ở vài theme; chưa mở rộng sửa UI ngoài phạm vi.
Không claim VPS hai tài khoản/FCM cloud PASS, không migrate DB/commit/push/deploy.
Đề xuất duy nhất: duyệt nghiệm thu staging end-to-end với backend V5/client mới.

## Hiện hành — MOMENT-REPLY-01 PASS local/AVD (20/10/2026)

Người dùng duyệt tính năng người nhận bình luận ảnh vào chat riêng với chủ ảnh.
Không còn giới hạn XML-only của lượt audit cho tính năng mới này; vẫn giữ stack,
định danh và dirty/untracked đã kiểm tra. Dùng skill backend-development-feature-
development điều phối server/data/security review và android_ui_verification
cho AVD; không mở rộng analytics/deploy hoặc chỉnh lỗi audit ngoài phạm vi.

Đã triển khai server MOMENT_REPLY/V5 mới, owner-directed grant/friend/block,
validation1..2000Unicode/keyretry/pushonce/redaction; Android composer ở Viewer,
ReplyVM+Repo pending/recreate/guard, Chat reference/text tách nhau và image retry.
Không sửa V1–V4, không migrate DB cá nhân/production; không gửi FCM/email thật.
Toàn bộ file/hợp đồng ở [QA_MOMENT_REPLY.md](QA_MOMENT_REPLY.md), [CHAT_API.md](CHAT_API.md).

Bằng chứng: full Maven170tests BUILD SUCCESS3m03s; Android122JVM, build52s
debug/test/release unsigned đạt; AVD18/18 mới+hồi quy190,294s; reply bản cuối7/7
28,388s; test guard process/generation tách riêng1/1 0,041s. Fixture RAM riêng,
AVD API37 headless1536MB/2core dùng userdata QA, không tài khoản cá nhân. Đã
review ảnh reply/IME/retry/thu hồi, tắt AVD sau QA. Lint vẫn179/90 debug và179/98
release do bản dịch có sẵn; không khẳng định toàn app/production PASS.

Chưa có nghiệm thu Android→VPS hai tài khoản hoặc FCM cloud reply; khi triển khai
phải cập nhật backend V5 và client hiểu MOMENT_REPLY đồng bộ. Không tự commit,
push, deploy hoặc mở bước mới. Đề xuất duyệt nghiệm thu staging end-to-end riêng.
Lỗi audit List/Chat flaky cũ vẫn ghi nhận; lần hồi quy Chat đạt không xóa lịch sử lỗi.

## Lịch sử — UI/UX audit, sửa resource và chờ nghiệm thu (20/10/2026)

### Bàn giao sau nghiệm thu bằng AVD

Người dùng đã cho phép “tự chạy máy ảo”. Medium_Phone API37 headless 1536MB/2core
dùng userdata QA riêng qr-avd-data, RUNNING_UNLOCKED; không wipe AVD gốc hay dùng
phiên cá nhân. Skill android_ui_verification dùng cho kiểm thử/ảnh/log.
Sửa thêm sáu resource: Login minHeight/links, Preview top bar và lưới công cụ,
login_themes ánh xạ input semantic, badge item History và tab wrap_content.
Không sửa Java/test Java. ID và 109 production Java giữ nguyên.

Build cuối **34s đạt**, 118 JVM tests thực sự chạy không lỗi. UI bộ tổng31/34
(383,796s); targeted11/12 (98,440s); Camera permission riêng1/1 (10,012s).
Login/Media matrix đạt sau sửa, Auth/chooser/Profile tương tác đạt. List matrix
vẫn Text fits height: TextView [none] trên APK bàn giao. Chat attachment timeout
hai lần bộ tổng nhưng đạt khi chạy riêng; chưa điều tra đủ, không gọi đã sửa.
Lint debug179/90, release179/98; giữ lỗi bản dịch và không baseline/suppress.
Không claim production/WCAG/TalkBack/RTL/widget launcher/optical hardware PASS.
Ảnh/log và lý do các lượt lỗi tại [UI_UX_AUDIT.md](UI_UX_AUDIT.md).

Trạng thái **IN PROGRESS, chưa đạt nghiệm thu toàn bộ**, không còn bị chặn bởi USB.
Đề xuất duyệt chỉ test Java diagnostic cho List và điều tra tính ổn định Chat;
giữ nguyên nghiệp vụ/Repository/ViewModel, không tự mở rộng quyền sửa Java.
Không commit/push/deploy. AVD tắt sau QA để trả RAM; không cần người dùng thao tác
điện thoại để tiếp tục chẩn đoán AVD. Phần bên dưới là snapshot trước lượt AVD.

Người dùng yêu cầu audit toàn Android UI/UX và tự sửa an toàn, chỉ resource XML,
không đổi ID/Java/nghiệp vụ. Đã đọc prompt/tài liệu và dùng skill accessibility
audit + android_ui_verification; bảo toàn dirty/untracked có sẵn.

Đã sửa 28 layout, bốn file values và thêm chín resource mới; báo cáo chi tiết
tại [UI_UX_AUDIT.md](UI_UX_AUDIT.md). Đối chiếu 29 layout không đổi ID;
109 production Java giữ nguyên hash. Build debug/test/release unsigned đạt
19s (`.local/artifacts/ui-audit-build-final.log`). Báo cáo JVM: 118 test,
0 failures/errors; task cuối UP-TO-DATE nên không coi là lần chạy test mới.
Lint cuối debug 179 errors/90 warnings, release 179/98; 22 NewApi đã hết,
179 MissingTranslation cũ còn nguyên, không suppress hoặc giả bản dịch.

**Chưa nghiệm thu UI runtime sau sửa.** ADB không nhận điện thoại; chưa cài APK,
chạy instrumentation/TalkBack hay chụp ảnh mới. Không dùng kết quả QR/Chat cũ
để xác nhận UI resource mới. Không tự mở emulator hoặc tác động tài khoản.
Hành động tối thiểu tiếp theo: cắm lại USB, mở khóa, chấp nhận USB debugging
để chạy hồi quy UI bằng fixture QA an toàn. Các vấn đề cần Java/thiết kế widget
và độ phủ còn thiếu đã ghi rõ trong báo cáo; không tự mở rộng phạm vi.
Trạng thái chung **IN PROGRESS**; nghiệm thu USB **BLOCKED**. Không commit/push/deploy.

## Lịch sử — QR PASS nghiệm thu QA USB + AVD (20/10/2026)

Điện thoại đã được cắm lại. Cài app/test giữ dữ liệu; USB Android 15 đạt
7/7 regression/UI cuối (20,863s, `qr-usb-final-regression.log`) + gallery
thật 1/1 (12,990s, `qr-usb-gallery-final.log`) + optical/torch 1/1 (6,881s,
`qr-usb-optical-final.log`). Hai phase dùng QR khác username/UUID để chống
false-positive từ trạng thái READY cũ; không inject callback/URI/payload.
Log combined từng OK không dùng nghiệm thu vì review ảnh phát hiện rủi ro
assertion cũ. Session bytes giữ nguyên, không gửi lời mời thật. Đã xóa đúng
hai ảnh QA tạm trong Downloads sau đối chiếu hash; bản sao còn local.
Ảnh chứng minh cuối `qr-usb-gallery-final.png`, `qr-usb-optical-final.png`.
Chỉ sửa test/fixture và docs trong lượt USB, build test cuối 13s. Không
commit/push/deploy/backend/DB. Lint ngoài QR còn 201 lỗi, không claim toàn
dự án/production PASS. Không cần người dùng thao tác thêm cho nghiệm thu QR.

### Hồ sơ AVD trước lượt USB

Người dùng đã cho phép “tự chạy máy ảo” khi USB mất kết nối. Skill
android_ui_verification dùng AVD Medium_Phone headless 1536 MB/2 core/API 37,
userdata QA riêng vì AVD gốc khóa PIN/RUNNING_LOCKED. Không thử PIN/reset/wipe
dữ liệu gốc, không dùng tài khoản cá nhân. Bảo toàn mọi dirty/untracked.

Đã đạt 7/7 regression/UI, 44,265s (`qr-avd-final-retry.log`) + permission
deny/grant 1/1, 14,942s (`qr-avd-permission.log`); session bytes giữ nguyên,
mọi friend request ở fixture RAM, share bị chặn tại chooser. Sửa thêm lỗi
My QR dismiss giữ scan pause bằng isMyCodeShown(); harness chờ picker thật
trước Back, không bỏ assertion. 8 ảnh tại `.local/artifacts/qr-avd-final-images`.
118 JVM và build APK debug/test/release đạt (`qr-avd-fixed-build.log`).

Ở thời điểm AVD chưa kiểm tra optical/torch/picker trên điện thoại; các mục
này đã bổ sung phía trên. Delivery share vẫn không thuộc lượt QA. Không claim production
PASS. Chi tiết đầy đủ, log lượt lỗi và ca còn thiếu tại [QA_QR.md](QA_QR.md).
Không commit/push/deploy/backend/DB.

## Lịch sử — CHAT-01–05 PASS local/USB (20/10/2026)

Quyền mới nhất: triển khai CHAT-01 tới CHAT-05 cùng lượt, dùng điện thoại USB, không emulator/commit/push/deploy. Đã đọc đặc tả/decisions/Git/source, giữ nguyên toàn bộ dirty/untracked có sẵn. Skill backend-development-feature-development tổ chức phần backend/UI/QA; android_ui_verification dùng cho kiểm thử native trên USB.

Đã có: server/chat, V4 migration, 11 ca integration chat; Android ChatModels/ChatApi/ChatRepository/ChatViewModel, Main 5 tab, Inbox/New Chat/phòng chat, private moment, read/retry/draft, foreground sync và FCM/notification không text. Bảo vệ bản nháp theo process/thế hệ phiên; không cấp thêm quyền ảnh khi chat.

Bằng chứng: backend full 163/163; targeted cuối 11/11; Android 115 JVM/build/lint; ChatThemeUiTest USB 6/6; ChatCloudAcceptanceTest qua Firebase thật 1/1, notification permission đã xác nhận granted và assertion thông báo chung đạt. 14 ảnh trong `.local/artifacts/chat-usb-images`. Hồi quy Media/List/Profile chạy lại 11/11 PASS (167,056s), sau sửa helper mở Main qua shell foreground và chờ đúng lifecycle/instance trên MIUI; không bỏ assertion, không sửa production Viewer. Log lỗi đầu giữ ở `chat-usb-regression.log`; log đạt `chat-usb-regression-final.log`.

Server cloud QA dùng cluster PostgreSQL mới, hai tài khoản QA, chỉ loopback. Đã dừng sau test, không ảnh/avatar/file media, bỏ đúng adb reverse 58143. Không thay JWT/DB user, không gửi thư. Quyền thông báo bật theo xác nhận của người dùng. Không deploy V4 lên VPS; không đổi BASE_URL đang có.

Bản APK bàn giao cuối build 37s đạt và đã cài app/test qua USB; ChatThemeUiTest cuối **6/6 PASS (35,025s)** (`chat-usb-final-acceptance.log`). Một lượt kiểm tra lại trước đó timeout Main cold-start; đã sửa riêng harness clean màn QA sau fixture + NEW_TASK|CLEAR_TOP + assert shell launch, giữ timeout/assertion và chạy lại đủ sáu ca. Không thay production để che lỗi kiểm thử. Lint còn 36 warning debug/44 release, 0 error.

Danh sách và giới hạn chi tiết: [QA_CHAT.md](QA_CHAT.md), [CHAT_API.md](CHAT_API.md). Dừng bàn giao, không triển khai tính năng mới. Đề xuất tiếp theo duy nhất: người dùng duyệt cập nhật backend/V4 lên môi trường đang dùng và nghiệm thu hai thiết bị; chưa có quyền thực hiện deploy đó.

## Lịch sử — Kế hoạch triển khai Module Tin nhắn & Chat khoảnh khắc (CHAT-01 -> CHAT-05), 20/10/2026

**Đã hoàn thành đưa Module Tin nhắn vào kế hoạch triển khai của dự án Taktak.**
- Yêu cầu người dùng: Đưa module Tin nhắn (dựa trên thiết kế tham chiếu 5 màn hình: Hộp thư đến, Khung chat 1-1, Xử lý IME insets, Soạn tin mới chọn bạn bè, Trạng thái rỗng "Chưa có tin nhắn") vào kế hoạch triển khai chính thức theo chuẩn `AGENTS.md`.
- Thay đổi cấu trúc điều hướng: Bottom Navigation mở rộng từ 4 tab thành **5 tab**: Camera (0) | Lịch sử (1) | **Tin nhắn (2)** | Bạn bè (3) | Hồ sơ (4).
- Phân rã 5 tiểu bước:
  1. `CHAT-01`: Backend Database & REST API (Spring Boot 3, Flyway Migration `V4__direct_messaging.sql`, Conversations & Messages tables/entities/repositories/services/controllers, phân quyền Friend/Block, FCM push data-only `chat_message`, Spring Boot Integration tests).
  2. `CHAT-02`: Android Navigation 5 Tabs & Messages Inbox UI (Màn 1 & 5). Menu `main_tabs.xml` 5 items, `MainActivity` & `MainPagerAdapter` 5 fragments, `MessagesFragment` & `fragment_messages.xml`, danh sách hội thoại, avatar bạn bè, last message, relative time, unread badge, tìm kiếm, nút `+`, empty state + CTA "Tìm bạn ngay", 4 theme.
  3. `CHAT-03`: Android New Chat / Select Friend UI (Màn 4). `NewChatActivity` & `activity_new_chat.xml`, danh sách bạn bè, tìm kiếm bạn, khởi tạo chat 1-1, 4 theme.
  4. `CHAT-04`: Android Chat Room UI & Send Text/Moment (Màn 2 & 3). `ChatActivity` & `activity_chat.xml`, bong bóng chat text/moment, trạng thái "Đã xem", thanh input bar gửi text/ảnh khoảnh khắc, WindowInsets bàn phím ảo IME không che nội dung, 4 theme.
  5. `CHAT-05`: Realtime Sync & FCM Integration & UI Automation Tests. Lắng nghe FCM `chat_message` qua `PushEventBus` và `TaktakMessagingService`, đồng bộ realtime khi đang mở chat, cập nhật unread badge, Local Notification khi chạy ngầm, bộ test tự động `ChatThemeUiTest` trên AVD qua 4 theme.
- Tài liệu đã cập nhật: `docs/TASKS.md`, `docs/PROJECT_DECISIONS.md`, `docs/SESSION_HANDOFF.md`.
- Trạng thái: **Sẵn sàng triển khai tiểu bước CHAT-01 (Backend Database & REST API)**. Dừng chờ người dùng phê duyệt bắt đầu CHAT-01.

## Lịch sử — Nghiệm thu UI-THEME-08 (Profile screen, Avatar upload & Logout 4 theme), 20/10/2026

**Đã hoàn thành và nghiệm thu UI-THEME-08: PASS TOÀN DIỆN.**
- Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Hồ sơ cá nhân (`fragment_profile.xml`), bao gồm thông tin tài khoản, chỉnh sửa tên hiển thị (display name), tải lên ảnh đại diện (avatar upload), chuyển giao diện (Theme settings) và đăng xuất an toàn (logout).
- Triển khai:
  - Theme tokens & styles: Bổ sung `<attr name="ttMediaOutlineStyle" format="reference" />` và style `@style/Widget.TakTakMedia.Outline` trong `res/values/media_themes.xml`; cấu hình trên 4 theme và khai báo fallback an toàn trong `Base.Theme.TakTak` (`res/values/themes.xml`).
  - Layout: `fragment_profile.xml` gỡ bỏ hardcoded `@style/Theme.TakTak.Premium`, liên kết động với tokens (`ttMediaHeadlineStyle`, `ttMediaTonalStyle`, `ttMediaFieldStyle`, `ttMediaInputStyle`, `ttMediaPrimaryStyle`, `ttMediaOutlineStyle`, `colorPrimary`, `colorOnPrimary`).
  - Fixture: `MediaThemeFixture` hỗ trợ mock UserApi (`me`, `update`, `uploadAvatar`, `avatar`) và DeviceApi (`devices`) để test tải hồ sơ, đổi avatar, sửa tên và đăng xuất không phụ thuộc mạng ngoại vi.
  - Tests: Viết mới `ProfileThemeUiTest.java` (3 test cases), cập nhật assertions trong `MediaThemeUiTest.java` xác minh cả 4 tab chính của `MainActivity` đều áp dụng context theme động.
- Kiểm tra thực tế:
  - Android Emulator AVD `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM):
    - `ProfileThemeUiTest`: **3/3 tests PASS (24.209s)** (72 tổ hợp layout bounds/WCAG contrast >= 4.5:1, chỉnh sửa tên hiển thị trên 4 theme & khôi phục sau recreate, tải avatar JPEG & đăng xuất an toàn xóa phiên).
    - `MediaThemeUiTest`: **5/5 tests PASS (114.147s)** (kiểm tra hồi quy Media và xác nhận `fragment_profile` cùng `fragment_history`, `fragment_friends` đều áp dụng context theme động).
    - `ListThemeUiTest`: **4/4 tests PASS (29.027s)** (kiểm tra hồi quy Lists).
  - Android JVM: **105/105 tests PASS** (27s).
  - Server Spring Boot: **152/152 tests PASS** (1m 39s).
- Emulator AVD đã được đóng an toàn sau khi hoàn tất kiểm thử, giải phóng RAM.
- Báo cáo chi tiết: [QA_THEME_PROFILE.md](QA_THEME_PROFILE.md).
- Không commit/push/deploy.

## Lịch sử — Nghiệm thu UI-THEME-07 (History, Friends & List Items 4 theme), 19/10/2026

**Đã hoàn thành và nghiệm thu UI-THEME-07: PASS TOÀN DIỆN.**
- Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho tab Lịch sử (Đã nhận / Đã gửi), tab Bạn bè, các danh sách (Moments, Invitations, Friends, Search results, Section headers, Footers) và hộp thoại xác nhận.
- Triển khai:
  - Theme tokens & styles: Bổ sung 7 attributes/tokens trong `res/values/media_themes.xml` (`ttMediaPrimaryStyle`, `ttMediaTonalStyle`, `ttMediaCardBackground`, `ttMediaTabIndicator`, `ttMediaTabSelectedColor`, `ttMediaTabUnselectedColor`, `ttMediaDividerColor`, `ttMediaNoticeBackground`) và fallback đầy đủ trong `res/values/themes.xml`.
  - Drawables: `res/drawable/media_tab_indicator.xml`.
  - Layouts: `fragment_history.xml`, `fragment_friends.xml`, `item_friend.xml`, `item_friends_section.xml`, `item_history_footer.xml` đã gỡ bỏ hardcoded theme, liên kết động với tokens.
  - Sửa lỗi layout: `item_history_moment.xml` tối ưu `momentSenderUsername` với `layout_weight="1"` và `singleLine="true"`, giải quyết dứt điểm lỗi tràn biên ngang trên viewport hẹp 280dp với font trợ năng 2.0x.
  - Fixture: `MediaThemeFixture.java` cập nhật mock `MomentModels.PublicUser`, đầy đủ các trường bắt buộc của `FriendModels.Invitation` và các mock mutations bạn bè.
- Kiểm tra thực tế:
  - Android Emulator AVD `Medium_Phone` (Android 17 / API 37, headless 1.5GB RAM):
    - `ListThemeUiTest`: **4/4 tests PASS (31.607s)** (144 tổ hợp layout, chuyển tab Đã nhận / Đã gửi & khôi phục khi đổi theme, danh sách bạn bè & tìm kiếm & hộp thoại MaterialAlertDialog, tương tác mở MomentViewerActivity).
    - `MediaThemeUiTest`: **5/5 tests PASS (116.985s)** (hồi quy Media đạt 100%).
  - Android JVM: **105/105 tests PASS** (12s).
  - Server Spring Boot: **152/152 tests PASS** (2m 01s).
- Emulator AVD đã được đóng an toàn sau khi hoàn tất kiểm thử, giải phóng RAM.
- Báo cáo chi tiết: [QA_THEME_LISTS.md](QA_THEME_LISTS.md).
- Không commit/push/deploy. Trạng thái: **Sẵn sàng cho UI-THEME-08 (Profile screen, Avatar upload & Logout bốn theme)**. Dừng chờ chỉ đạo tiếp theo của người dùng.

## Hiện hành — Nghiệm thu UI-THEME-06 (Media bốn theme: Camera / Preview / Navigation / Viewer), 19/10/2026

**Đã hoàn thành và nghiệm thu UI-THEME-06: PASS TOÀN DIỆN.**
- Phạm vi: Triển khai 4 theme (Original, Gradient, Glass, Soft) cho 3 screen roles: `MAIN`, `PREVIEW`, `VIEWER`.
- Triển khai:
  - Theme tokens & styles: `res/values/media_themes.xml` hỗ trợ đầy đủ 4 phong cách và Light/Dark mode.
  - Native background: `MediaBackdropView.java` vẽ shader 60fps, không chụp/blur bitmap.
  - ThemeManager: Mở rộng ScreenRole cho `MAIN`, `PREVIEW`, `VIEWER`.
  - Layouts: `activity_main.xml` (bottom navigation 4 tabs, theme styling), `fragment_camera_page.xml` / `fragment_camera.xml` (CameraX 1:1, shutter/controls), `activity_preview.xml` (bản nháp 1:1, caption, recipients, retry retention), `activity_moment_viewer.xml` (top bar, 1:1 photo card, caption card, responsive ScrollView chống nghẽn layout ở mọi viewport và fontScale 2.0x).
- Kiểm tra thực tế:
  - Android Emulator AVD `Medium_Phone` (Android 17 / API 37): `MediaThemeUiTest` **5/5 tests PASS (138.247s)**. 144 tổ hợp layout đạt chuẩn WCAG tương phản >= 4.5:1, không tràn viền/cắt chữ; draft/caption/recipients/retry/survive recreation đạt; camera navigation & settings đạt; 1 real QA capture 1:1 & retake cache discard đạt; viewer dialog/revocation 403 ẩn ảnh riêng tư đạt.
  - Android JVM: **105/105 tests PASS** (14 suites).
  - Server Spring Boot: **152/152 tests PASS**.
- Emulator AVD đã được đóng an toàn sau khi hoàn tất kiểm thử, giải phóng RAM.
- Chi tiết file, kết quả và bằng chứng: [QA_THEME_MEDIA.md](QA_THEME_MEDIA.md).
- Không commit/push/deploy. Dừng chờ chỉ đạo tiếp theo của người dùng.

## Hiện hành — Đóng nghiệm thu Feature B, 19/10/2026

**Đã hoàn thành và đóng nghiệm thu Feature B: PASS.** Đã triển khai và xác minh đầy đủ 7 hạng mục validation:
1. Username min length: Ít nhất 3 ký tự (tối đa 255), chỉ ký tự `[a-z0-9_]`.
2. Username không bắt đầu/kết thúc bằng `_`, không có `__` liên tiếp; regex `^(?!.*__)[a-z0-9][a-z0-9_]{1,253}[a-z0-9]$`.
3. Display name tối thiểu 1 ký tự sau strip (bảo đảm bằng `@NotBlank @Size(max = 255)` và `AuthInputValidator.displayNameError`).
4. Phân biệt mã lỗi 409 PostgreSQL (SQLState 23505): `EMAIL_CONFLICT` (*"Email đã được sử dụng."*) vs `USERNAME_CONFLICT` (*"Tên người dùng đã được sử dụng."*), fallback `ACCOUNT_CONFLICT`. Android client ánh xạ lỗi và tự động highlight/focus trường bị trùng tương ứng.
5. Inline validation: Lắng nghe `onFocusChangeListener` trên cả 5 trường (`displayName`, `username`, `email`, `password`, `confirmPassword`); TextWatcher tự động xóa lỗi cũ khi bắt đầu gõ.
6. Password strength indicator: `LinearProgressIndicator` + `MaterialTextView` trong `activity_register.xml` đánh giá 3 mức (Yếu/Trung bình/Mạnh) tương thích cả 4 theme.
7. Username availability check: Endpoint `GET /api/auth/check-username?username=...` không yêu cầu xác thực, `CacheControl.noStore()`. Android debounce 400ms qua `Handler`.
Kiểm tra thực tế:
- Server verify: **152/152 tests PASS** (gồm kiểm thử concurrency, unique constraint, integration curl, recovery, token).
- Android JVM: **105/105 tests PASS** (14 test suites, bao gồm `AuthInputValidatorTest` và `AuthSessionTest`).
- Android Lint: `lintDebug` **0 errors**.
- Android Build: `assembleDebug` **BUILD SUCCESSFUL**.
Không commit/push/deploy. Trạng thái: **Sẵn sàng chuyển sang UI-THEME-06 (Camera / Preview / recipients / navigation / viewer 4 theme)**.


## Hiện hành — UI-THEME-05, 19/10/2026

**Hoàn tất UI05 và dừng: PASS trong phạm vi UI.** Người dùng duyệt Register/Recovery và USB; không mở AVD. Trước đó04PASS, dirty changes cũ giữ nguyên.9main files,6test files và5hồ sơ liên quan được tạo/sửa như [QA_THEME_AUTH.md](QA_THEME_AUTH.md); không quy mọi Git diff cũ vào05. Thêm AuthBackdropView và auth_themes.xml, AUTH đủ4theme; giữ Original renderer/Premium, tái dùng3theme Login; không thay API/VM/Repository/backend/DB/JWT/FCM/widget. Không commit/push/deploy.

Build cuối .local/artifacts/theme05-build-hint-fixed.log SUCCESS45s,101/101JVM,lint0errors/21debug+29release warnings (tăng1debug unused legacy Register style, không suppress/xóa). Batch cuối .local/artifacts/theme05-usb-hint-fixed-final.log **8/8,110.116s**,Xiaomi23129RAA4G/Android15/1080×2400:32lượt form,144layout/font/night tổ hợp,text/hint contrast,loading/expiry/secret non-persistence/eye/IME. Appearance restored và session bytes unchanged đều assert đạt.38ảnh chuẩn tại .local/artifacts/theme05-usb-final/theme05/; parent chứa ảnh pull trước đó, không dùng nhầm. Review ảnh USB đã phát hiện hint Register sáng nhạt và sửa/test lại.

Lượt đầu3/8lỗi mở màn từ nền MIUI, đã sửa helper foreground launcher; lượt sau7/8do session bytes đổi trong startup launcher có luồng kiểm tra/refresh, không đọc token hoặc khôi phục token cũ, không bỏ assertion. Lặp cùng APK8/8rồi mới sửa hint; bản cuối8/8trên đúng APK hint mới. Chi tiết log/giới hạn ở QA, không coi các lượt lỗi làPASS. Appearance local cuối Gradient/System/schema1/revision1, không ép về map rỗng từng quan sát ở lượt trước.

103XML0invalid,diffcheck0,scoped key/JWT patterns0findings,index rỗng,artifacts ignored; không có baseline hash bền vững sau interruption để tuyên bố kiểm hash toàn repo. Không gửi Gmail/đăng ký/reset thật hoặc kiểm thu hồi phiên server trong05; blocker App Password cũ giữ nguyên. Không đổi settings màn hình/IME của điện thoại,APK đã cập nhật giữ dữ liệu. Không cần thao tác thủ công ngoài mở app xem UI. Chỉ đề xuất **UI-THEME-06 — Camera/Preview/recipients/navigation/viewer**, chờ duyệt.

## Hiện hành — UI-THEME-04, 19/10/2026

**Đã hoàn tất và dừng tại04: PASS phạm vi Login bốn theme.** Trước phiên03PASS, source/dirty worktree có sẵn được bảo toàn. Thêm5resource main, sửa8main file đúng phạm vi cùng4test và hồ sơ; chi tiết [QA_THEME_LOGIN.md](QA_THEME_LOGIN.md). Login dùng manager trước inflate; Original fixed-dark, ba kiểu mới sáng/tối; pending đổi theme không làm mất draft/loading hoặc ghi file phiên. Các màn khác vẫn Original, chưa làm05.

Build cuối .local/artifacts/theme04-build-contrast-fixed.log SUCCESS39s; debug/test/release, **101/101 JVM**, lint0errors/20debug+29release warnings. Bộ UI cuối .local/artifacts/theme04-ui-final-retry.log **20/20,124.809s** (5LoginTheme+7chooser+4prefs+4Original), Login72layout/font/night và chooser72tổ hợp.11ảnh ở .local/artifacts/theme04-final-screens/; đã xem đủ8theme sáng/tối và Glass/IME. Lượt trước19/20 do CTA Gradient sáng4.208:1 đã sửa màu riêng; cold instrumentation2lượt LOW_MEMORY được lưu log, không tính PASS. Không skip assertion.

Snapshot308file trước phiên:8main cũ đổi,5main mới,0xóa; server/src83file và deploy/scripts/API/Repository/ViewModel/session/FCM/widget giữ nguyên hash.102resource XML hợp lệ, diffcheck0, quét mẫu key/JWT phạm vi04 không phát hiện, index rỗng, artifacts ignored. Đã hoàn trả IME/density, đóng AVD QA để giải phóng RAM; không xóa AVD. Không dùng điện thoại, tài khoản thật hoặc Gmail; giới hạn OS-mode transition/OEM/TalkBack/performance/API thật ghi rõ trong QA. Không commit/push/deploy. Không cần người dùng thao tác cho04; **đề xuất05 — Register/Recovery bốn theme và chờ duyệt**. Blocker Gmail/App Password trước đây chưa thay đổi.

## Hiện hành — UI-THEME-03,19/10/2026

User duyệt riêng03. Đã triển khai ThemeSettingsActivity/ViewModel/PreviewView,4palette chooser native/DayNight, preview/mode/draft/Apply/Cancel/Back/loading/error/retry, entrypoints Login/Profile. Không áp theme lên màn nghiệp vụ hoặc recreate parent; resolver02 vẫn fallback Original. Chi tiết files/contracts/tests tại [QA_THEME_CHOOSER.md](QA_THEME_CHOOSER.md). Worktree dirty cũ giữ nguyên; hash300file trước/sau chỉ6file cũ đổi có chủ đích+8main files mới, không backend/scripts/deploy/VM nghiệp vụ/API/JWT/FCM. Không commit/push/deploy.

**PASS03.** App review build58s,fulltasks với helper39s,test APK cuối23s;101/101JVM,lint0errors/20debug+29release warnings cũ. Batch cuối **15/15PASS110.614s**,log `.local/artifacts/theme03-ui-isolated-retry.log` (7chooser+4prefs+4Login,gồm physicalIME),72layout/font/night tổ hợp đạt. Source app không đổi sau sửa decor background/icon system bars; helper LoginReferenceTest sửa trả đúng instance +chờ focus và không gọi launcher trong từng ca AVD để tránh session routing cướp focus,giữ workaround máy thật. Không bỏ assertion/skip test.

Lượt trước gặp LOW_MEMORY/SystemUI ANR,14/15vìIME/focus; giữ toàn bộ log fail trong QA,không gọi làPASS. Dùng AVD Medium_Phone cold/read-only/headless2GB/720x1280/density320; dừng tạm7app phụ của bản sao AVD,không SystemUI/GMS,không clear data; không build song song.10ảnh chooser+5Login/IME cuối tại `.local/artifacts/theme03-final-screens/`,đã xem light/dark system bars từ app review. Đã hoàn trảIME0/densityreset và đóng đúng AVD của phiên,không xóa AVD.97XML0invalid,diffcheck0,sourcehash300chỉ6mainfilecũđổi,backend/scripts/deploy/VMnghiệp vụgiữ nguyên. Chi tiết QA và scope thực tế ở link trên. Chưa test điện thoại/OEM/API account thật/OS transition/reboot/performance; Profile entry kiểm tra source/layout,Login quay về giữ form/sessionbytes đã test. Không cần thao tác thủ công03. Đề xuất **UI-THEME-04** rồi dừng chờ xác nhận. Gmail blocker giữ nguyên,không resume recovery hoặc tự đổi bước.

## Hiện hành — UI-THEME-02,19/10/2026

User duyệt riêng02. Đã thêm package ui.theme gồm AppTheme/ColorMode/ThemeSelection/ThemeStore/ThemePreferences/ThemeRepository/ThemeManager và3file tests. File private taktak_appearance_v1 tách auth/device; ID ổn định, schema/revision, no-op, synchronous commit worker, IOException/rollback, unknown/corrupt fallback không ghi, future schema không overwrite. Resolver giữ requested và effective riêng: Original resources đúng5role,3style mới availability=false, không alias giả UI. Apply-before-create là entrypoint chưa gọi production; không sửa Activity/Manifest/layout/resources, không thêm chooser/ViewModel chưa có consumer. Không tự recreate/password/draft/session.

VERIFY bản cuối `.local/artifacts/theme02-final-build.log`: BUILD SUCCESSFUL59s, debug/test/release/unit/lint; **93/93JVM**,0fail/error/skipped;lint0errors,20debug/29release warnings,0new-theme findings. AVD Medium_Phone read-only/headless API37/1080x2400/1536MB/2core: batch **8/8PASS23.635s** (4prefs+4Login), sau đó probe write1/1→force-stop app/test→read1/1 ở process khác PASS. Session isolation chỉ file sentinel QA, không login/logout API hoặc đổi tài khoản thật.5ảnh Login đã pull,đã xem login.png. IME0→1→0,AVD của phiên được đóng sau QA, không xóa AVD. Logs theme02-device/process-write/process-read,ảnh theme02-login;ignored.

REVIEW hash293file main/server/deploy/scripts trước/sau:0file cũ sửa/mất,chỉ thêm7main themefiles. Giữ dirty cũ HEADf73b43d/nhánh chore/push-current-project; không commit/push/deploy. Danh sách/contract/task kiểm thử/giới hạn tại [QA_THEME_FOUNDATION.md](QA_THEME_FOUNDATION.md). Không tuyên bố3theme visual/chooser/runtime/reboot/performance/OEM đã đạt. Gmail App Password vẫn blocker riêng, không resume recovery. Đề xuất **UI-THEME-03**, dừng chờ xác nhận. Mục01 dưới đây là lịch sử trước user duyệt02; ánh xạ3ảnh chưa triển khai visual nên vẫn chờ chốt.

## Hiện hành — UI-THEME-01,19/10/2026

User gửi prompt Multi-theme3ảnh,chỉ duyệt01audit và cho phép tự chạy emulator vì không có điện thoại. Đọc AGENTS/README/đặc tả/quyết định/status/tasks/handoff,theme/resources/source UI/draft/widget; worktree dirty cũ được giữ. Skill ui-ux-pro-max đã chạy CLI --design-system/domainstyle với Python có sẵn (sửa cp1252 bằng envUTF8),không áp gợi ý landing/web/Compose. android_ui_verification dùng AVD Medium_Phone read-only/headless1536MB/2core,không dùng USB.

Đầu ra [UI_THEME_AUDIT.md](UI_THEME_AUDIT.md): A–I audit/palette/tokens/architecture/fileplan/Native effects/persistence/lifecycle/01–09/gates. Prompt đảo thứ tự ảnh1/3; đề xuất theo visual Gradient=ảnh3,Glass=ảnh2,Soft=ảnh1 chờ user chốt. Original giữ đúng theme cũ theo screenRole (Login fixed-dark),3style mới có light/dark riêng. Cần chuyển hard-code/root overlays chứ không chỉ setTheme. Không ép recreate Preview/worker/draft/password; pending revision khi unsafe,không sửa ViewModel nghiệp vụ.

VERIFY:92resource XML0invalid; contrast cặp màu đề xuất đã tính,không giả toàn gradient PASS. AVD1080x2400,cài APK/testAPK cũ; ADB unauthorized đầu giải quyết bằng restart daemon cùng environment không xóa keys/AVD. Baseline4Login tests đầu1FAIL doIME setting0,đổi tạm1 và chạy lại **4/4 PASS21.68giây**,đã trả0. `.local/artifacts/theme-audit-baseline-retest.log`,ảnh `.local/artifacts/theme-audit-baseline-final/` đã xem Login/IME. Không build mới,không có screenshot/test cho3theme mới; không regression auth/backend/camera trongaudit. Chỉ4filedocs tạo/cập nhật,không commit/push/deploy. Dừng chờ user duyệt02; Recovery/Gmail vẫn chờ App Password,không tự chuyển B.

Review cuối hash317file source/script/deploy giữ nguyên trước/sauauditdocs,0file mất;diffcheck0,link local0missing,artifactsignored. AVD của phiên đã được đóng bằng emulator console để giải phóngRAM; không xóa/reset AVD.

## Hiện hành — Recovery A,19/10/2026

### Kết quả tiếp nối USB/Gmail (mới nhất)

Người dùng cung cấp fileSMTP local ignored và xác nhận USB; đã cài APK/test APK giữ origin/dữ liệu. Lượt đầu9test/1FAIL tìm ra lỗi LiveData idle xóa expired notice. Sửa renderer, thêm IME+unit retry; build cuối **79/79 JVM + assemble debug/test/release/lint PASS** (`recovery-usb-fix-build.log`). Batch cuối **10/10 USB PASS32.942giây** (`recovery-usb-retest.log`),6Recovery+4Login,5screenshot đã pull/xem. Không logout/reset tài khoản cá nhân/không emulator. Backend production không sửa trong lượt này;151/151 regression/JAR là bằng chứng lượt trước.

Gmail bị auto-review chặn trước lần gửi; sau user xác nhận email nhận cho3thưQA mới chạy test thật. Verification/request503 ở thư đầu; connection-only probe không gửi thư cũng FAILAuthenticationFailedException. **0SMTP accept được xác nhận**, không giả delivery. User trả lời chưa có App Password, cần hướng dẫn Google. Đã thêm `GmailRecoveryAcceptanceIT` opt-in và helperPowerShell parse credential local không evaluate/log, chỉ class-error an toàn; credentials không bị sửa. Script syntax/Gitignore/diffcheck đạt. Tài khoản QA chỉ ở PostgreSQL nhúng riêng, cluster/context đã đóng theo try-with-resources.

**BLOCKED riêng Gmail App Password/inbox**; UI gate đã đóng. Hành động tối thiểu: user tạo App Password cho đúng MAIL_USERNAME, thay MAIL_PASSWORD trong filelocal rồi báo xong. Chạy Connection trước, sau khi đạt mới Acceptance tới đúng recipient user đã duyệt; không cần hỏi lại nội dung3emailQA nhưng không gửi credentials vào chat. Chưa SMTP accept/inbox nên chưa chuyển B hoặc đánh dấu DONE toàn bộ A. Các mô tả USB chưa chạy bên dưới là lịch sử trước lượt tiếp nối.

Người dùng duyệt bổ sung verification và revoke ngay mọi phiên sau reset. Đã đọc yêu cầu/đặc tả/source auth/Git, dùng auth-implementation-patterns và android_ui_verification; giữ worktree UI cũ tại HEADf73b43d. Backend migrationV3, security_version/DB-check JWT, user-row-lock login/refresh/reset, hash-only temporary/verification/reset credential và SMTP STARTTLS bounded. Android thêm Forgot/Verification/Change Java/XML, ViewBinding/Repository/ViewModel; reset token chỉ memory, không full session/Intent/Bundle. Chi tiết API/policy/config/file: [ACCOUNT_RECOVERY.md](ACCOUNT_RECOVERY.md).

VERIFY: backend45/45 chọn lọc,145/145 rồi bản cuối **151/151,0fail/error/skipped + JAR**. Gồm13test recovery qua HTTP/PostgreSQL,2SMTP; race reset/reset,temporary-login,refresh/reset,old-login/reset,JWT legacy,policy,expiry/replay và notification failure đều có assertion. Log cuối `.local/artifacts/recovery-backend-final.log`. Android bản cuối **78/78JVM,0fail/error/skipped; assemble debug/test/release và lint debug/release PASS** (`.local/artifacts/recovery-android-reviewed-build.log`,52giây); lint0errors,20debug/29release warnings. Hardening không nhận mixed reset/full-session response và clear vault khi clear session đã build/test lại. Quét mẫu credentials phạm vi source/docs0findings, git diff --check0, index rỗng, V1/V2 không diff; artifacts ignored. Không tuyên bố audit toàn bộ secret hoặc UI chưa chạy.

Điện thoại8e90fdff ban đầu nhận USB1080x2400 nhưng khóa/Dozing, rồi mất kết nối; hai lượt `adb devices` (sandbox và ngoài sandbox) đều rỗng. Lượt `adb install -r` trảdevice-not-found, chưa thay APK/phiên trên máy; **chưa chạy5UI recovery/4Login regression hoặc chụp ảnh mới**. Không mở emulator/tự mở khóa/đăng xuất/reset tài khoản cá nhân. Không có MAIL_* trong environment; đã hỏi config local, chưa được cung cấp. Gmail/inbox thật chưa kiểm tra, fixture mail không tính delivery thật.

Trạng thái **BLOCKED nghiệm thu toàn bộ A** vì cần Gmail SMTP và phone USB mở khóa. Không staged/commit/push/deploy, không truy cập DB production, V1/V2 không sửa. Tiếp tục chỉ A khi người dùng cung cấp điều kiện, không chuyển B. Hành động tối thiểu: cắm lại USB/mở Taktak và cung cấp đường dẫn file SMTP local ignored (không gửi mật khẩu); dùng tài khoản QA riêng để nghiệm thu email/reset. Đối chiếu [ACCOUNT_RECOVERY.md](ACCOUNT_RECOVERY.md) trước chạy; giữ API origin cá nhân khi cài APK, không chạy AuthUiTest có logout setup trên điện thoại cá nhân.

## Hiện hành — Redesign cả5 màn theo ảnh,18/10/2026

Người dùng gửi5 ảnh nhưng prompt vẫn Login-only; đã hỏi và người dùng xác nhận redesign cả5 Register/Camera/History/Friends/Profile. Đây là phạm vi duyệt hiện tại, không tự mở roadmap/nghiệp vụ. HEAD `f73b43d`, giữ thay đổi Login chưa commit. Dùng ui-ux-pro-max và android_ui_verification. Không mở emulator, chỉ USB; người dùng đồng ý hướng camera an toàn.

Thêm theme/tokens Premium sáng/tối, background canvas nhẹ, gradient/outlined CTA và navbar nổi; sửa5 màn, cards/empty vector, vòng avatar/camera picker cùng handler cũ. Giữ MVVM/data/API/JWT/FCM/widget/backend/schema. Sửa lỗi XML ảnh Lịch sử height0 bằng ratio trên ImageView con; có assert nonzero/square và ảnh JWT thật hiện đúng. Scope/chi tiết/file tại [QA_PREMIUM_FIVE_SCREENS.md](QA_PREMIUM_FIVE_SCREENS.md).

VERIFY: assemble debug/test/release,lint debug/release PASS;72JVM0fail;lint0errors,18debug/27release warnings. Batch cuối **7/7 USB PASS37.632s**, gồm3Premium và4Login;54 tổ hợp layout/theme/font1.5,empty layout,Received/Sent,notice validation search,Register/eye/loading/IME. Các lượt thất bại và fixture corrections ghi riêng, không skip. Có8 ảnh UI ở `.local/artifacts/premium-five-final-screenshots/` và thêm camera-live.png chụp sau Surface ổn định3giây, đã xem xác nhận preview thật. Không nhấn shutter/upload/đổi avatar/save/logout hay thay quan hệ bạn bè trên tài khoản cá nhân; không đổi settings/clear data/đổi API origin.

Khác ảnh mẫu: vector thay asset inbox3D; font OEM; không blur/glass3D; Camera giữ1:1 và cuộn nếu thiếu chiều cao; nội dung thật có thể khác empty/avatar trong ảnh. PASS chỉ build/unit/UI đã chạy, không nghiệm thu lại toàn mutation/backend/FCM/widget/production. Đã cài APK mới, dừng để người dùng duyệt5 màn; không commit/push/deploy.

## Lịch sử — Login theo ảnh tham chiếu,18/10/2026

Người dùng duyệt prompt pixel-precise Login rồi cung cấp asset PNG và yêu cầu tắt emulator/chạy điện thoại USB do laptop lag. Dùng ui-ux-pro-max + android_ui_verification; đọc đặc tả/quyết định/source trước sửa. HEAD đầu phiên `f73b43d`,worktree sạch. Chỉ Login/asset/resources/test, ngoài ra escape đường dẫn Windows trong gradle.properties để sửa lỗi lint có sẵn, giữ API origin/JDK/dependencies. Không đổi backend/DB/AuthRepository/ViewModel/session/FCM hay redesign Register.

Đã cài app giữ dữ liệu/API origin trên điện thoại Android15,1080×2400/font1.0. Theme tối riêng,background trang trí edge-to-edge,logo gốc không sửa bytes,TextInputLayout/eye,gradient CTA/ripple/loading,Register routing. Recovery/language chưa có nghiệp vụ nên không tạo hành động giả. Lint debug/release0errors/15warnings,assemble debug/test/release và72JVM PASS;4/4 UI-only USB PASS16.897s,5 ảnh tại `.local/artifacts/login-reference-usb-final-screenshots/` đã kéo về và xem Login/IME cuối. Các lượt lỗi AAPT/API26/fixture và5-test emulator đầu có2fail được ghi rõ, không tính PASS toàn batch.

Emulator đã tắt theo yêu cầu,smoke server/cluster fixture dừng sạch0rows/files. Không tự logout hoặc thay tài khoản cá nhân; UI tests cuối không gửi credentials hợp lệ. API/retry trên APK cuối ở điện thoại **NOT VERIFIED**, không suy ra toàn bộ Login hay production đã nghiệm thu. Chi tiết/file/lệnh/bằng chứng/khác biệt: [QA_LOGIN_REFERENCE.md](QA_LOGIN_REFERENCE.md). Không commit/push/deploy; không chuyển Register. Hành động tiếp theo tối thiểu nếu cần đóng cổng API USB: người dùng tự đăng nhập kiểm tra trên điện thoại hoặc cho phép phiên QA riêng, không cung cấp mật khẩu vào chat.

## Hiện hành — Hồ sơ/avatar và Firebase/widget thật trên AVD, 18/10/2026

Người dùng duyệt ba hạng mục sau báo cáo phần thiếu. Đã tạo ProfileFragment Java/XML theo foundation M3E, ProfileRepository/ViewModel, chọn/upload/tải avatar riêng tư; hiển thị avatar ở Bạn bè/Lịch sử/Viewer. Backend có POST avatar, GET self/friend, giữ PUT me chỉ sửa displayName, row lock chống tên/avatar cập nhật đồng thời. Tái sử dụng cột V1 và storage sanitizer; không migration/dependency mới. Backup/restore hỗ trợ avatars.

Backend cuối **136/136 tests PASS + JAR**; Android **72/72 JVM + build/lint debug/release PASS, 0 errors/9 warnings mỗi variant**. Batch mới **6/6 AVD PASS**:5 Hồ sơ/avatar/dark mode +1 FCM/widget qua Firebase thật, không fake delivery. Có4 ảnh RemoteViews QA host và5 ảnh Profile/Friends. Hồi quy cuối sau chỉnh avatar placeholder **10/10 PASS** (2 profile,2 history,6 privacy). Shell contract có avatar **11/11**, Bash syntax10file và PowerShell smoke script parse đạt. Smoke server đã dừng exit0, đối chiếu4 moment rows/files và4 avatar rows/files; gỡ reverse/quyền bind widget QA trên emulator.

Admin JSON local đã khớp project Android, Git ignore, không in key/token. Người dùng yêu cầu **emulator trước**, nên không cài/test/sửa phiên hoặc widget trên điện thoại thật đang sử dụng. FCM cloud thật tới AVD và Worker cập nhật RemoteViews khi app background đã xác minh; **chưa nghiệm thu USB/launcher OEM, Doze, force-stop hoặc VPS**. Không commit/push/deploy; toàn bộ bản sửa review trước được giữ. Xem [QA_PROFILE_AVATAR_FCM.md](QA_PROFILE_AVATAR_FCM.md) cho files/logs/ảnh/lệnh và giới hạn. Bước tiếp theo duy nhất là nghiệm thu cùng luồng trên điện thoại thật khi người dùng sẵn sàng.

## Lịch sử — sửa lỗi review 5C–8, 18/10/2026

Kết quả: backend verify **130/130**, Android JVM **69/69**, build/lint debug/release **PASS (0 errors, 9 warnings/variant)**; **22 ca Android khác nhau** đã đạt qua batch, không bỏ qua 7 lỗi login của lượt gộp (đã chạy lại đạt); **11 contract** và **10 Bash syntax** đạt. Riêng privacy 6/6, History 2/2 trên APK cuối. Logs và 2 screenshot fixture: `.local/artifacts/fixes-20261018/`, ignored. Git diff check/link/credential-pattern scan đạt trong phạm vi đã ghi.

Smoke server báo `14 rows / 14 files / 8 recipients`, đóng sạch cluster test; gỡ reverse, tắt emulator đã mở. Điện thoại USB xuất hiện ở lần kiểm tra cuối sau nghiệm thu emulator; chưa cài APK/chạy test hoặc thay đổi phiên trên điện thoại. Không dữ liệu người dùng bị xóa. Môi trường tiếp theo cần operator phê duyệt staging Ubuntu/DB test và FCM credentials/thiết bị nếu muốn nghiệm thu tích hợp thật; xem giới hạn ở báo cáo. Dừng tại đây, không tự triển khai bước tiếp theo.

Người dùng yêu cầu sửa các lỗi quan trọng đã tìm thấy. Bắt đầu ở `chore/push-current-project`, HEAD `6a7f51e`, tracked worktree sạch. Không đổi stack/package, không sửa migration, không chạm DB/VPS người dùng; không commit/push/deploy.

Đã sửa thứ tự xóa file sau commit, cache/viewer/gallery fail-closed, generation guard khi render widget/history, FCM recorder/config và callback lifecycle; sửa backup/restore/update/TLS và trusted proxy/Compose. Báo cáo kết quả/lệnh/log ở [QA_REVIEW_FIXES.md](QA_REVIEW_FIXES.md), vận hành ở [runbook](../deploy/MAINTENANCE.md).

Các mục 8C bên dưới là **hồ sơ lịch sử bị thay thế ở những kết luận mâu thuẫn**; không dùng nhận định “100%/production-ready” hoặc cờ `rollback.sh --restore-db` cũ làm hướng dẫn hiện hành. Profile chưa triển khai. Chưa nghiệm thu VPS/Docker/ACME/FCM thật.

## Hoàn tất 8C — Kiểm thử cuối, bảo mật client-server & Quy trình Update/Rollback (Final Verification & Rollback Procedures) — 18/10/2026

**PASS TOÀN DIỆN; HOÀN TẤT 100% ROADMAP TAKTAK MVP (GIAI ĐOẠN 0 ĐẾN 8).** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Bộ đôi Script DevOps Update & Rollback (`deploy/scripts/`)**:
  - `update.sh`: Tự động sao lưu snapshot DB trước khi update -> sao lưu file JAR hiện tại thành `taktak-server.jar.bak` -> nạp file JAR mới -> khởi động lại dịch vụ `taktak.service` -> thăm dò healthcheck `/actuator/health` trong 30 giây -> **tự động Rollback** về file JAR cũ nếu healthcheck thất bại.
  - `rollback.sh`: Cho phép quản trị viên quay lui khẩn cấp về file JAR trước đó, có tùy chọn khôi phục snapshot database đồng bộ qua `--restore-db <BACKUP_TIMESTAMP>` và kiểm tra lại healthcheck sau khi quay lui.
- **Tài liệu Triển khai & Vận hành Toàn diện (`deploy/README.md`)**:
  - Sơ đồ kiến trúc Mermaid đầy đủ: Android Client -> UFW -> Nginx Reverse Proxy (HTTPS / Rate Limit) -> Spring Boot (127.0.0.1:8080) -> PostgreSQL (127.0.0.1:5432) & Private Storage (`/var/taktak/private-images`, `chmod 700`) -> FCM Push.
  - Bảng ma trận mạng, cổng và quyền hạn thư mục.
  - Hướng dẫn triển khai lần đầu từ A-Z.
  - Sổ tay xử lý sự cố (Troubleshooting Runbook) chi tiết 5 kịch bản: Lỗi 502 Bad Gateway Nginx, Lỗi kết nối PostgreSQL, Lỗi chứng chỉ SSL Let's Encrypt, Đĩa cứng VPS đầy, Mã lỗi 429 Too Many Requests do Rate Limiter.
- **Kiểm thử An toàn Bảo mật Client-Server & APK Release**:
  - Kiểm tra an toàn cấu hình Android: `usesCleartextTraffic="false"`, `allowBackup="false"`, `BASE_URL` bắt buộc HTTPS qua tham số `-PTAKTAK_RELEASE_BASE_URL`.
  - Quét an toàn mã nguồn gói phát hành: Không rò rỉ bất kỳ JWT secret, mật khẩu DB, hay Firebase service account credentials nào vào APK hay Git.
  - Xây dựng thành công bản phát hành Android: `assembleRelease lintRelease` đạt **0 lint errors**, sinh file `app-release-unsigned.apk` (11.8 MB).
- **Kiểm thử Toàn bộ Test Suite & Nghiệm thu 11 Kịch bản Cốt lõi**:
  - Đối chiếu và nghiệm thu toàn bộ 11 ca kiểm thử thực hành trong mục 11 đặc tả [TAKTAK_PROJECT.md](TAKTAK_PROJECT.md): AUTH-01 đến AUTH-04, SETUP-01, NETWORK-01, SELF-01, FRIEND-01, FRIEND-02, MOMENT-01 đến MOMENT-03.
  - Toàn bộ 7 script bash trong `deploy/scripts/` đều PASS cú pháp (`bash -n`).
  - Lập báo cáo nghiệm thu toàn diện [QA_FINAL_VERIFICATION.md](QA_FINAL_VERIFICATION.md).

### Kiểm thử và bằng chứng:
- **Backend Tests**: `mvn test` đạt **123/123 tests PASS (100%)**, 0 failures, 0 errors, 0 skipped.
- **Backend Packaging**: `mvn package -DskipTests` **BUILD SUCCESS**, đóng gói runnable JAR `taktak-server-0.0.1-SNAPSHOT.jar`.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **66/66 tests PASS (100%)**, 0 failures, 0 errors.
- **Android Release Build & Lint**: `assembleRelease lintRelease` **BUILD SUCCESS in 31s**, **0 lint errors**.
- **Bash Script Syntax**: Kiểm tra toàn bộ 7 script bash (`setup-vps.sh`, `setup-ssl.sh`, `setup-ufw.sh`, `backup.sh`, `restore.sh`, `update.sh`, `rollback.sh`) đạt **0 errors, exit code 0**.

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- **Hoàn tất 100% Roadmap Taktak MVP (Toàn bộ Giai đoạn 0, 1, 2, 3, 4, 5, 6, 7, 8 đã hoàn thành xuất sắc)**.
- Dừng chờ chỉ đạo tiếp theo từ người dùng.

## Hoàn tất 8B — Nginx Reverse Proxy, TLS Let's Encrypt & Tường lửa UFW (Nginx Reverse Proxy & SSL/TLS) — 18/10/2026

**PASS; dừng chờ duyệt Giai đoạn 8C.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Cấu hình Nginx Reverse Proxy (`deploy/nginx/taktak.conf`)**:
  - Chuyển tiếp kết nối tới Spring Boot `127.0.0.1:8080` kèm đầy đủ tiêu đề ủy quyền (`Host`, `X-Real-IP`, `X-Forwarded-For`, `X-Forwarded-Proto`, `X-Forwarded-Port`).
  - Hỗ trợ Let's Encrypt ACME challenge tại `/.well-known/acme-challenge/` và tự động chuyển hướng 301 toàn bộ HTTP port 80 sang HTTPS port 443 (`https://$host$request_uri`).
  - Cấu hình TLS chuẩn bảo mật cao: hỗ trợ `TLSv1.2` và `TLSv1.3`, bộ mã hóa mạnh (ECDHE/DHE), bật HSTS (`max-age=31536000; includeSubDomains`), bổ sung các Security Headers (`X-Frame-Options DENY`, `X-Content-Type-Options nosniff`, `X-XSS-Protection "1; mode=block"`, `Referrer-Policy strict-origin-when-cross-origin`).
  - Giới hạn tải lên `client_max_body_size 10M` và timeout kết nối/đọc/ghi 30s-60s.
  - Cơ chế Rate Limiting phân tầng chống tấn công DoS và Brute-force:
    - `/api/`: Zone `taktak_api` giới hạn 10 req/s, burst 20 nodelay.
    - `/api/auth/login` và `/api/auth/register`: Zone `taktak_auth` giới hạn 5 req/phút, burst 5 nodelay.
    - Trả về mã lỗi HTTP `429 Too Many Requests` khi vượt ngưỡng.
  - Bảo vệ thư mục riêng tư: Tuyệt đối không phục vụ static file từ `/var/taktak/private-images`, chặn toàn bộ dotfiles (`.env`, `.git`...).
- **Script tự động hóa TLS Let's Encrypt (`deploy/scripts/setup-ssl.sh`)**:
  - Cài đặt Nginx, Certbot và module Nginx plugin; tự động sinh file cấu hình theo domain quản trị; cấp phát chứng chỉ HTTPS Let's Encrypt qua lệnh `certbot --nginx`; kiểm tra gia hạn tự động qua `certbot renew --dry-run`.
- **Script cấu hình tường lửa UFW (`deploy/scripts/setup-ufw.sh`)**:
  - Thiết lập chính sách mặc định `default deny incoming` và `default allow outgoing`.
  - Mở các cổng dịch vụ cần thiết: 22 (SSH), 80 (HTTP), 443 (HTTPS).
  - Khóa tuyệt đối cổng 8080 (Spring Boot) và cổng 5432 (PostgreSQL) khỏi truy cập từ ngoài internet.
- **Cấu hình Xoay vòng Nhật ký (`deploy/logrotate/taktak`)**:
  - Xoay vòng hàng ngày, lưu 14 ngày, nén gzip cho `/var/log/taktak/*.log` và `/var/log/nginx/taktak_*.log`.
- **Giải pháp Sao lưu & Khôi phục Dữ liệu Toàn diện (`backup.sh` & `restore.sh`)**:
  - `deploy/scripts/backup.sh`: Tự động dump database PostgreSQL, nén thư mục ảnh `/var/taktak/private-images`, sao lưu an toàn file `.env`, phân quyền `chmod 700`, tự động dọn dẹp các bản backup cũ quá 14 ngày.
  - `deploy/scripts/restore.sh`: Script khôi phục an toàn với xác nhận người dùng, dừng dịch vụ, phục hồi database, giải nén ảnh, thiết lập lại quyền `chmod 700 taktak:taktak`, khởi động lại dịch vụ và kiểm tra healthcheck.
  - `deploy/systemd/taktak-backup.service` & `deploy/systemd/taktak-backup.timer`: Lập lịch chạy backup tự động mỗi ngày vào 02:00 AM.
- **Cập nhật script cài đặt tổng thể & Tài liệu**:
  - Cập nhật `deploy/scripts/setup-vps.sh` tích hợp toàn bộ các bước 8A & 8B.
  - Cập nhật tài liệu hướng dẫn vận hành [DEPLOYMENT_GUIDE.md](DEPLOYMENT_GUIDE.md).
  - Lập báo cáo nghiệm thu [QA_NGINX_SSL_BACKUP.md](QA_NGINX_SSL_BACKUP.md).

### Kiểm thử và bằng chứng:
- **Bash Syntax Verification**: `"C:\Program Files\Git\bin\bash.exe" -n` kiểm tra toàn bộ 5 script bash (`setup-vps.sh`, `setup-ssl.sh`, `setup-ufw.sh`, `backup.sh`, `restore.sh`): **0 syntax errors, exit code 0**.
- **Backend Tests**: `mvn clean test` đạt **123/123 tests PASS**, 0 failures, 0 errors, 0 skipped.
- **Backend Packaging**: `mvn package -DskipTests` **BUILD SUCCESS**, tạo thành công file runnable JAR `taktak-server-0.0.1-SNAPSHOT.jar`.

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- Hoàn tất toàn bộ Giai đoạn 8B.
- **Đề xuất duy nhất**: **Giai đoạn 8C — Kiểm thử cuối, bảo mật client-server & Quy trình Update/Rollback (Final Verification & Rollback Procedures)**. Dừng chờ phê duyệt.

## Hoàn tất 8A — Chuẩn bị môi trường VPS Ubuntu & Cấu hình dịch vụ (VPS Ubuntu Setup & System Services) — 18/10/2026

**PASS; dừng chờ duyệt Giai đoạn 8B.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Cấu hình Spring Boot Production (`server/src/main/resources/application-prod.yml`)**:
  - Kích hoạt qua profile `prod` (`spring.profiles.active=prod`).
  - Toàn bộ tham số nhạy cảm và secrets nạp hoàn toàn qua biến môi trường: `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `SERVER_PORT` (mặc định 8080), `JWT_ACCESS_SECRET`, `IMAGE_STORAGE_ROOT`, `FCM_ENABLED`, `GOOGLE_APPLICATION_CREDENTIALS`.
  - Database: Bắt buộc `ddl-auto=validate`, vô hiệu hóa tuyệt đối việc tự động thay đổi schema, quản lý tiến hóa cơ sở dữ liệu qua Flyway migration.
  - Bảo mật: Ẩn server header Tomcat, ẩn hoàn toàn exception stacktrace ra bên ngoài (`server.error.include-stacktrace: never`), bật endpoint `/actuator/health` phục vụ kiểm tra trạng thái dịch vụ (healthcheck).
- **Mẫu biến môi trường sản phẩm (`deploy/.env.example`)**:
  - Cung cấp đầy đủ danh mục biến môi trường với placeholder rõ ràng, không hardcode bất kỳ credential, password, hay secret thật nào.
- **Dịch vụ systemd (`deploy/systemd/taktak.service`)**:
  - Chạy ứng dụng dưới user hệ thống không có quyền root (`User=taktak`, `Group=taktak`).
  - Thiết lập cơ chế tự khởi động lại khi crash (`Restart=always`, `RestartSec=10`).
  - Áp dụng các cờ sandbox hardening nghiêm ngặt của Linux: `NoNewPrivileges=true`, `ProtectSystem=strict`, `ProtectHome=true`, `PrivateTmp=true`, `ReadWritePaths=/var/taktak`.
- **Giải pháp container hóa Docker (`deploy/docker/Dockerfile` & `deploy/docker/docker-compose.yml`)**:
  - `Dockerfile`: Multi-stage build tối ưu kích thước dựa trên Eclipse Temurin 17 JRE Alpine; tạo user/group `taktak` (UID/GID 10001); chạy non-root.
  - `docker-compose.yml`: Triển khai 2 service `db` (Postgres 16 Alpine) và `app` trên private bridge network nội bộ; service `db` không expose port ra host; service `app` chỉ bind vào loopback cục bộ `127.0.0.1:8080:8080`; tự động kiểm tra sức khỏe dịch vụ qua Docker healthcheck.
- **Thư mục lưu trữ ảnh riêng tư ngoài Web Root**:
  - Thư mục `/var/taktak/private-images` được bảo vệ bằng quyền `chmod 700`, thuộc sở hữu `taktak:taktak`. Nginx không bao giờ mount thư mục này vào web root; mọi yêu cầu tải ảnh đều phải qua API Spring Boot để kiểm tra quyền truy cập (sender/recipients) và quan hệ bạn bè.
- **Script tự động hóa cài đặt VPS (`deploy/scripts/setup-vps.sh`)**:
  - Tự động hóa toàn bộ quy trình: cài đặt OpenJDK 17, PostgreSQL, UFW; tạo user `taktak`; tạo thư mục lưu trữ ảnh riêng tư với phân quyền `chmod 700`; cấu hình PostgreSQL lắng nghe `127.0.0.1`; cấu hình tường lửa UFW (cho phép port 22 SSH, chặn trực tiếp 8080 và 5432).
- **Tài liệu hướng dẫn triển khai (`docs/DEPLOYMENT_GUIDE.md`)**:
  - Hướng dẫn chi tiết từng bước cho cả 2 phương án triển khai (Phương án A: Native Systemd Service; Phương án B: Docker Compose), kèm các lệnh vận hành (start, stop, logs, healthcheck, backup database).
- **Báo cáo nghiệm thu (`docs/QA_VPS_SETUP.md`)**:
  - Ghi nhận chi tiết kết quả kiểm tra cấu hình, test backend, và tiêu chí nghiệm thu.

### Kiểm thử và bằng chứng:
- **Backend Tests**: `.\mvnw.cmd clean test` đạt **123/123 tests PASS**, 0 failures, 0 errors, 0 skipped.
- **Backend Package**: `.\mvnw.cmd package -DskipTests` **BUILD SUCCESS**, tạo thành công runnable JAR `server/target/taktak-server-0.0.1-SNAPSHOT.jar`.
- **Cấu hình & Tường lửa**: Kiểm tra cú pháp systemd unit, Dockerfile, docker-compose.yml, script bash setup-vps.sh; đảm bảo nguyên tắc bảo mật tối thượng (không phơi bày port DB/App ra internet, secrets qua ENV).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- Hoàn tất toàn bộ Giai đoạn 8A.
- **Đề xuất duy nhất**: **Giai đoạn 8B — Nginx Reverse Proxy, TLS Let's Encrypt & Tường lửa UFW**. Dừng chờ phê duyệt.

## Hoàn tất 7C — Tải ảnh vào bộ sưu tập thiết bị (Save to Device Gallery) — 18/10/2026

**PASS local/AVD; dừng chờ duyệt Giai đoạn 8A.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Android Client (Java / Material 3)**:
  - Khai báo quyền: `<uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE" android:maxSdkVersion="28" />` trong `AndroidManifest.xml` (tương thích ngược Android 8–9; Android 10+ dùng Scoped Storage không cần quyền runtime).
  - Lớp tiện ích `GallerySaver`: Lưu ảnh JPEG / Bitmap vào `MediaStore` (`Pictures/Taktak`). Trên Android 10+ (API 29+) sử dụng `RELATIVE_PATH` và cờ `IS_PENDING = 1` trong lúc ghi để tránh file hỏng; trên Android 8–9 (API 26–28) ghi vào thư mục ngoài rồi quét MediaStore; có cơ chế rollback dọn dẹp Uri nếu ghi thất bại.
  - Vector icon tải xuống: `ic_download.xml` (24dp, chuẩn Material Icon).
  - Chuỗi giao diện tiếng Việt trong `history_strings.xml`: Tiêu đề lưu ảnh, thông báo thành công ("Đã lưu ảnh vào Bộ sưu tập."), thông báo lỗi ("Không thể lưu ảnh vào máy. Vui lòng thử lại."), và thông báo quyền bộ nhớ.
  - Giao diện xem toàn màn hình: Bổ sung nút lưu ảnh `viewerSaveButton` trên thanh tiêu đề `viewerTopBar` trong `MomentViewerActivity` cho cả người gửi lẫn người nhận.
  - Hành động người dùng (User Action): Ảnh riêng tư và khoảnh khắc nhận được tuyệt đối không tự động xuất ra Gallery khi chưa có hành động nhấn nút từ người dùng.
  - Luồng lưu ảnh an toàn: Chạy trên worker thread, ưu tiên lấy bytes JPEG gốc tải từ server hoặc nén từ RAM cache nếu thiết bị đang offline, hiển thị Toast trạng thái tiếng Việt mượt mà.
  - Quản lý quyền runtime: Sử dụng `ActivityResultLauncher<String>` xin quyền `WRITE_EXTERNAL_STORAGE` khi chạy trên Android 9 trở xuống (API <= 28).

### Kiểm thử và bằng chứng:
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **66/66 tests PASS**, 0 failures, 0 errors.
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://taktak.example.com/` **BUILD SUCCESSFUL**, **0 errors, 0 lint warnings**.
- **Kiểm thử trên Android Emulator `Medium_Phone` (Android 17 / API 37)** qua ADB reverse port 8080:
  - 12/12 tests trong `DeviceAcceptanceTest` PASS (100% success rate):
    - `saveMomentToGalleryOnUserActionSavesValidJpegToMediaStore` (3.493s): Người dùng kích hoạt lưu ảnh -> lưu thành công JPEG vào MediaStore -> mở stream đọc lại từ ContentResolver giải mã được Bitmap đúng kích thước gốc 80×80 -> dọn dẹp Uri thành công.
    - `emptyJpegBytesThrowsIOExceptionOnGallerySave` (1.561s): Kiểm tra trường hợp byte rỗng ném IOException.
    - `momentDeletionBySenderRevokesRecipientAccessImmediately` (4.594s)
    - `momentDeletionNetworkFailurePreservesLocalCache` (2.308s)
    - `sentMomentsEndToEndWithBackend` (4.133s)
    - `momentViewerActivityRendersAndCachesImage` (2.707s)
    - `widgetUpdaterRendersAllStatesAndDecodesBitmap` (1.929s)
    - `latestMomentEndToEndWithBackend` (5.989s)
    - `multiDeviceRegistrationForSameAccount` (23.240s)
    - `deviceRegistrationAndUnregistrationEndToEnd` (2.215s)
    - `pushEventBusDispatchesEventsToListeners` (1.615s)
    - `momentUploadAndDeleteEndToEnd` (2.405s)
- **Dọn dẹp môi trường**:
  - Smoke server dừng an toàn, đóng cổng 8080.
  - Quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`. Xem [QA_SAVE_TO_GALLERY.md](QA_SAVE_TO_GALLERY.md).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- Hoàn tất toàn bộ Giai đoạn 7.
- **Đề xuất duy nhất**: **Giai đoạn 8A — Chuẩn bị môi trường VPS Ubuntu & Cấu hình dịch vụ (VPS Ubuntu Setup & System Services)**. Dừng chờ phê duyệt.

## Hoàn tất 7B — Xóa khoảnh khắc phía máy chủ & Thu hồi quyền tức thời (Server-side Delete & Revocation) — 18/10/2026

**PASS local/AVD; dừng chờ duyệt Giai đoạn 7C.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Backend Spring Boot 3**:
  - Endpoint `DELETE /api/moments/{id}`: Xác thực JWT subject, kiểm tra quyền sở hữu của sender; nếu non-sender gọi xóa sẽ lập tức trả về `403 FORBIDDEN`.
  - Thu hồi quyền tức thời: Cập nhật soft delete `deleted_at = now()`, xóa an toàn file ảnh khỏi đĩa private VPS `storage.discard(filePath)` khiến mọi request `GET /{id}/image` lập tức trả về `404 NOT_FOUND` cho cả sender và recipients.
  - Sau commit transaction, phát FCM data payload `{type: "moment_deleted", momentId: id}` tới tất cả thiết bị của người nhận để kích hoạt đồng bộ nền tức thì.
  - Các truy vấn `latestMoment`, `listMoments(received)`, và `listMoments(sent)` tự động loại bỏ các ảnh đã xóa mềm.
- **Android Client (Java / Material 3)**:
  - Vector icon xóa: `ic_delete.xml` (24dp, chuẩn Material Icon).
  - Chuỗi giao diện tiếng Việt trong `history_strings.xml`: Tiêu đề xác nhận, nội dung cảnh báo, nút xóa/hủy, và thông báo lỗi kết nối mạng.
  - Giao diện xem toàn màn hình: Thêm nút `viewerDeleteButton` trên `viewerTopBar` của `MomentViewerActivity`, hiển thị khi `isSender == true`.
  - Luồng xóa an toàn: Khi người dùng bấm xóa, hiển thị hộp thoại `MaterialAlertDialogBuilder` xác nhận. Khi xác nhận, gọi luồng nền `MomentRepository.delete(momentId)`.
  - Vô hiệu hóa cache an toàn: Khi xóa thành công, loại bỏ khỏi `HistoryImageCache`, phát `PushEventBus.emitMomentDeleted(momentId)` (cập nhật UI danh sách Đã gửi và Đã nhận tức thì 0 ms), kích hoạt `WidgetUpdater.enqueueOneTimeWork(context)` để widget đồng bộ về ảnh tiếp theo hoặc placeholder rỗng, và đóng màn hình xem.
  - Xử lý Offline / Lỗi mạng an toàn: Nếu thiết bị mất mạng hoặc server không phản hồi, hiển thị dialog thông báo lỗi kết nối mạng, khôi phục tương tác và **tuyệt đối không xóa local cache hay danh sách hiển thị** khi xóa thất bại.
  - Màn hình lịch sử: `HistoryFragment` truyền cờ `EXTRA_IS_SENDER = (activeTab == HistoryViewModel.Tab.SENT)` khi mở `MomentViewerActivity`.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **123/123 tests PASS**, 0 failures, 0 errors. Các integration test xóa khoảnh khắc trong `MomentPushIntegrationTest` bao phủ: xóa thành công, thu hồi quyền tức thời (404), xóa file đĩa, phát FCM, chặn non-sender (403), xóa 2 lần (404), và loại trừ ảnh đã xóa mềm khỏi latest/list.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **66/66 tests PASS**, 0 failures, 0 errors. Bổ sung unit tests cho `MomentRepository.delete()` trong `MomentCaptionTest` (thành công, từ chối invalid UUID, không có session, server error 403/404).
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://taktak.example.com/` **BUILD SUCCESSFUL**, **0 errors**.
- **Kiểm thử trên Android Emulator `Medium_Phone` (Android 17 / API 37)** qua ADB reverse port 8080:
  - 10/10 tests trong `DeviceAcceptanceTest` PASS (100% success rate):
    - `momentDeletionBySenderRevokesRecipientAccessImmediately` (2.206s): Sender gửi khoảnh khắc cho Bob; Bob nhận được và tải được ảnh; Bob thử xóa bị từ chối 403; Sender xóa thành công; cả Bob và Sender tải ảnh đều lập tức nhận 404; ảnh biến mất khỏi danh sách `sent` của Sender và `received` của Bob.
    - `momentDeletionNetworkFailurePreservesLocalCache` (0.855s): Kiểm tra kịch bản offline/lỗi mạng, gọi xóa thất bại, xác nhận cache RAM `HistoryImageCache` và dữ liệu local không bị xóa sai lệch.
    - `sentMomentsEndToEndWithBackend` (1.830s)
    - `momentViewerActivityRendersAndCachesImage` (0.844s)
    - `widgetUpdaterRendersAllStatesAndDecodesBitmap` (0.942s)
    - `latestMomentEndToEndWithBackend` (1.938s)
    - `multiDeviceRegistrationForSameAccount` (3.670s)
    - `deviceRegistrationAndUnregistrationEndToEnd` (0.819s)
    - `pushEventBusDispatchesEventsToListeners` (0.820s)
    - `momentUploadAndDeleteEndToEnd` (0.868s)
- **Dọn dẹp môi trường**:
  - Smoke server dừng an toàn, đóng cổng 8080.
  - Quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`. Xem [QA_MOMENT_DELETION.md](QA_MOMENT_DELETION.md).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- **Đề xuất duy nhất**: **Giai đoạn 7C — Tải ảnh vào bộ sưu tập thiết bị (Save to Device Gallery)**. Dừng chờ phê duyệt.

## Hoàn tất 7A — Màn lịch sử Đã gửi & Xem toàn màn hình (Sent Moments History & Full-screen Viewer) — 18/10/2026

**PASS local/AVD; dừng chờ duyệt Giai đoạn 7B.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Backend Spring Boot 3**:
  - Hỗ trợ endpoint `GET /api/moments?type=sent&page=0`: truy vấn danh sách khoảnh khắc người dùng đã gửi (`m.sender_id = :senderId AND m.deleted_at IS NULL`), sắp xếp mới nhất lên đầu `created_at DESC, id DESC`, phân trang 20 items/trang kèm cờ `hasNext`, trả về `200 OK` với `MomentsPage`.
  - Quyền tải ảnh có xác thực: người gửi được cấp quyền tải trực tiếp ảnh của chính mình qua Bearer JWT `GET /api/moments/{id}/image`.
- **Android Client (Java / Material 3)**:
  - Repository: Mở rộng `MomentRepository.sent(int page)` và `MomentRepository.list(String type, int page)`.
  - Quản lý đa tab: `HistoryFragment` tích hợp `TabLayout` gồm 2 tab "Đã nhận" và "Đã gửi". Tiêu đề (`historyTitle`) và trạng thái rỗng (`historyEmptyTitle`, `historyEmptyDetail`) tự động chuyển đổi theo tab, bảo đảm tương thích ngược 100% với các test 5E và 5A.
  - Quản lý trạng thái: `HistoryViewModel` hỗ trợ `enum Tab { RECEIVED, SENT }`, quản lý phân trang độc lập cho 2 tab, tự động cập nhật khi có push `moment_created` hoặc `moment_deleted`.
  - Bộ nhớ đệm dùng chung: Xây dựng `HistoryImageCache` an toàn đa luồng bằng `LruCache` max 20 MB.
  - Xem toàn màn hình: Xây dựng `MomentViewerActivity` hiển thị chi tiết người gửi (avatar, displayName, @username, timestamp tương đối), ảnh 1:1 chất lượng cao (hiển thị tức thì 0 ms từ RAM cache hoặc tải qua mạng), khung chú thích đầy đủ, hỗ trợ đóng và back cử chỉ.
  - Card khoảnh khắc: Thêm hiệu ứng chạm ripple và `OnMomentClickListener` trong `HistoryAdapter` để mở `MomentViewerActivity`.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **123/123 tests PASS**, 0 failures, 0 errors. Bổ sung `MomentIntegrationTest.listMomentsSentTabReturnsSentMomentsWithPaginationAndSorting`.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **62/62 tests PASS**, 0 failures, 0 errors. Bổ sung unit tests cho `MomentRepository.sent()` trong `MomentCaptionTest`.
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://taktak.example.com/` **BUILD SUCCESSFUL**, **0 errors**.
- **Kiểm thử trên Android Emulator `Medium_Phone` (Android 17 / API 37)** qua ADB reverse port 8080:
  - 8/8 tests trong `DeviceAcceptanceTest` PASS (100% success rate):
    - `sentMomentsEndToEndWithBackend` (2.936s): Bob upload 1 ảnh riêng tư và 1 ảnh gửi Alice; Bob gọi `sent(0)` thấy cả 2 ảnh sắp xếp DESC; Bob tải được ảnh của chính mình; Alice gọi `sent(0)` thấy 0 ảnh, gọi `received(0)` thấy 1 ảnh (loại trừ ảnh riêng tư).
    - `momentViewerActivityRendersAndCachesImage` (1.049s): Kiểm tra ghi/đọc cache `HistoryImageCache`, định dạng thời gian `HistoryTimeFormatter`.
    - `widgetUpdaterRendersAllStatesAndDecodesBitmap` (1.657s)
    - `latestMomentEndToEndWithBackend` (2.474s)
    - `multiDeviceRegistrationForSameAccount` (12.364s)
    - `deviceRegistrationAndUnregistrationEndToEnd` (1.529s)
    - `pushEventBusDispatchesEventsToListeners` (1.108s)
    - `momentUploadAndDeleteEndToEnd` (2.189s)
- **Dọn dẹp môi trường**:
  - Smoke server dừng an toàn, đóng cổng 8080.
  - Quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`. Xem [QA_SENT_HISTORY_VIEWER.md](QA_SENT_HISTORY_VIEWER.md).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- **Đề xuất duy nhất**: **Giai đoạn 7B — Xóa khoảnh khắc phía máy chủ & Thu hồi quyền tức thời (Server-side Delete & Revocation)**. Dừng chờ phê duyệt.

## Hoàn tất 6C — AppWidgetProvider 2×2 & WorkManager Widget Sync — 18/10/2026

**PASS local/AVD; dừng chờ duyệt Giai đoạn 7A.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Backend Endpoint `GET /api/moments/latest` (Spring Boot)**:
  - Cung cấp endpoint bảo mật yêu cầu JWT Bearer, truy vấn khoảnh khắc nhận được mới nhất chưa bị xóa (`deleted_at IS NULL`) mà viewer được phép xem (`moment_recipients.user_id = :viewerId`).
  - Trả về `200 OK` kèm `MomentItem` (id, sender, caption, createdAt) hoặc `204 No Content` khi chưa có khoảnh khắc. `Cache-Control: no-store`.
  - Khi khoảnh khắc mới nhất bị xóa, tự động rơi về khoảnh khắc hợp lệ kế trước hoặc trả về 204.
- **Android Client App Widget 2×2 & RemoteViews Chuẩn M3 (Java)**:
  - Layout widget 2×2 (`widget_taktak_2x2.xml`, `widget_taktak_2x2_info.xml`): Khung ảnh bo góc 24dp, phủ dải gradient chân ảnh, tên người gửi (`displayName`/`username`), chú thích ảnh; trạng thái rỗng (placeholder camera *"Chưa có ảnh nào"*); trạng thái đăng xuất (ổ khóa *"Chưa đăng nhập"*); chạm mở `MainActivity`.
  - An toàn IPC Binder: Tuân thủ RemoteViews whitelist (`ImageView` cho gradient), thuật toán `decodeSampledBitmap` downsample ảnh an toàn xuống max 512×512 RGB_565 (< 512 KB) loại bỏ hoàn toàn lỗi `TransactionTooLargeException`.
  - `TaktakAppWidgetProvider`: `onUpdate` **chỉ enqueue WorkManager**, tuyệt đối không gọi mạng trực tiếp trong BroadcastReceiver.
  - `WidgetUpdateWorker`: Tải khoảnh khắc mới nhất qua JWT Bearer, giải mã Bitmap an toàn và cập nhật RemoteViews.
  - Kích hoạt thời gian thực & dự phòng: `TaktakMessagingService` nhận FCM data message (`moment_created`, `moment_deleted`) enqueue `OneTimeWorkRequest` (`taktak_widget_sync`); widget kích hoạt `PeriodicWorkRequest` 15 phút làm dự phòng. `SessionViewModel` tự động render trạng thái đăng xuất khi signOut.
  - Mở rộng client: `MomentApi.latest()`, `MomentRepository.latest()`.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **122/122 tests PASS**, 0 failures, 0 errors, 0 skipped. Gồm 4 integration tests mới trong `MomentPushIntegrationTest` (204 no content, 200 ok mới nhất, bỏ qua ảnh đã xóa, 401 unauthenticated). Executable JAR repackage thành công.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **60/60 tests PASS**, 0 failures, 0 errors (gồm 4 unit tests mới cho `MomentRepository.latest()`).
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.taktak.test/` **BUILD SUCCESSFUL**, **0 errors**.
- **Kiểm thử trên Android Emulator `Medium_Phone` (Android 17 / API 37)** qua ADB reverse port 8080:
  - 6/6 tests trong `DeviceAcceptanceTest` PASS (100% success rate):
    - `widgetUpdaterRendersAllStatesAndDecodesBitmap` (0.977s): Render an toàn các trạng thái LoggedOut, Empty, Loaded Moment; kiểm tra decodeSampledBitmap 800×800 -> 400×400 (<= 512); enqueue OneTime và Periodic Work.
    - `latestMomentEndToEndWithBackend` (2.545s): Alice kết bạn với User, Alice upload ảnh gửi cho User, User gọi `latest()` nhận đúng moment; Alice xóa ảnh, User gọi `latest()` tự động nhận null.
    - `multiDeviceRegistrationForSameAccount` (8.141s)
    - `deviceRegistrationAndUnregistrationEndToEnd` (1.032s)
    - `pushEventBusDispatchesEventsToListeners` (0.902s)
    - `momentUploadAndDeleteEndToEnd` (1.725s)
- **Dọn dẹp môi trường**:
  - Smoke server dừng exit 0: `SMOKE_MEDIA_ROWS=0 FILES=0 RECIPIENTS=1`.
  - Cổng 8080 đã đóng, quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`. Xem [QA_WIDGET_SYNC.md](QA_WIDGET_SYNC.md).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- **Đề xuất duy nhất**: **Giai đoạn 7A — Màn lịch sử Đã gửi & Xem toàn màn hình (Sent Moments History & Full-screen Viewer)**. Dừng chờ phê duyệt.

## Hoàn tất 6B — Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (Push Notification Dispatching) — 18/10/2026

**PASS local/AVD; dừng chờ duyệt Giai đoạn 6C.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Backend Bắn thông báo đẩy & Vòng đời khoảnh khắc (Spring Boot)**:
  - Cơ chế sau commit (`TransactionSynchronization.afterCommit`): Gửi push notification qua FCM chỉ khi giao dịch DB đã commit thành công; lỗi gửi FCM không rollback giao dịch DB.
  - Sự kiện `moment_created`: Bắn data message `{type: "moment_created", momentId: "..."}` tới toàn bộ thiết bị active của người nhận (`targets`).
  - Bảo mật & Cô lập: Ảnh chỉ mình tôi (`recipientIds=[]`) và upload gửi lại cùng `Idempotency-Key` (idempotency replay) tuyệt đối không gửi push. Tuyệt đối không gửi URL ảnh, caption hoặc bearer token qua FCM.
  - Endpoint `DELETE /api/moments/{id}`: Chỉ chủ sở hữu (sender) được phép xóa (`403 FORBIDDEN` với người khác). Đánh dấu `deleted_at = now()` (soft delete), xóa file ảnh khỏi private storage trên VPS, và phát FCM `{type: "moment_deleted", momentId: "..."}` tới người nhận. Sau khi xóa, request tải ảnh trả về `404 NOT_FOUND` cho cả sender lẫn recipient, và ảnh bị ẩn ngay khỏi danh sách lịch sử.
  - Tự động dọn dẹp stale token: Xóa token FCM hết hạn (`UNREGISTERED`, `INVALID_ARGUMENT`) khỏi bảng `device_tokens`.
- **Android Client Đón nhận sự kiện & Đồng bộ UI (Java)**:
  - `PushEventBus`: Singleton thread-safe quản lý đăng ký/hủy đăng ký và phát sự kiện `onMomentCreated` và `onMomentDeleted`.
  - `TaktakMessagingService`: `onMessageReceived` bóc tách `type` và `momentId` từ `remoteMessage.getData()` và emit vào `PushEventBus`.
  - `HistoryViewModel`: Lắng nghe `PushEventBus` — tự động `refresh()` khi có `moment_created`; xóa khoảnh khắc và RAM cache tức thì khi có `moment_deleted`.
  - `MomentApi` & `MomentRepository`: Thêm endpoint và hàm `delete(id)`.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **118/118 tests PASS**, 0 failures, 0 errors, 0 skipped. Gồm 7 integration tests mới trong `MomentPushIntegrationTest` kiểm thử đa thiết bị, chỉ mình tôi, idempotency replay, xóa ảnh, 403 không phải chủ sở hữu, 404 xóa lại, và dọn dẹp stale token. Executable JAR được repackage thành công.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **56/56 tests PASS**, 0 failures, 0 errors (gồm 4 tests mới trong `PushEventBusTest` và 3 tests mới cho DELETE trong `MomentCaptionTest`).
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease -PTAKTAK_RELEASE_BASE_URL=https://api.taktak.test/` **BUILD SUCCESSFUL**, **0 errors**.
- **Kiểm thử trên Android Emulator `Medium_Phone` (Android 17 / API 37)** qua ADB reverse port 8080:
  - 4/4 tests trong `DeviceAcceptanceTest` PASS (100%): gồm `pushEventBusDispatchesEventsToListeners` (1.716s) và `momentUploadAndDeleteEndToEnd` (2.193s).
- **Dọn dẹp môi trường**:
  - Smoke server dừng exit 0: `SMOKE_MEDIA_ROWS=0 FILES=0 RECIPIENTS=0`.
  - Cổng 8080 đã đóng, quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`. Xem [QA_PUSH_DISPATCHING.md](QA_PUSH_DISPATCHING.md).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- **Đề xuất duy nhất**: **Giai đoạn 6C — AppWidgetProvider 2×2 & WorkManager Widget Sync**. Dừng chờ phê duyệt.

## Hoàn tất 6A — Đăng ký thiết bị & FCM Token (Device & FCM Registration) — 18/10/2026

**PASS local/AVD; dừng chờ duyệt Giai đoạn 6B.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Backend API Quản lý Thiết bị & Firebase Admin SDK**:
  - `POST /api/devices` với Bearer JWT: Đăng ký/cập nhật `deviceId` và `fcmToken`. Xử lý chuyển nhượng token (reassignment) tự động xóa token cũ nếu được tái sử dụng trên thiết bị/tài khoản khác, bảo đảm ràng buộc `uq_device_tokens_fcm_token`.
  - `DELETE /api/devices/{deviceId}`: Hủy đăng ký thiết bị khi người dùng đăng xuất (idempotent, trả về `204 No Content`).
  - `GET /api/devices`: Liệt kê các thiết bị active của người dùng hiện tại (`200 OK`).
  - `FirebaseConfig`: Tự động nạp Google credentials hoặc kích hoạt graceful fallback an toàn nếu không tìm thấy file cấu hình trong môi trường test/local.
  - Interface `PushNotificationSender` và triển khai `FirebasePushNotificationSender` chuẩn bị cho 6B.
- **Android Client Quản lý Device & FCM**:
  - `DeviceIdProvider` / `AppDeviceIdProvider`: Tạo và lưu trữ UUID ngẫu nhiên bền vững trong SharedPreferences (`Context.MODE_PRIVATE`).
  - `DeviceModels`, `DeviceApi`, `DeviceRepository`: Triển khai các phương thức `register(token)` và `unregister()`.
  - `TaktakMessagingService`: Kế thừa `FirebaseMessagingService`, xử lý `onNewToken` (tự động đồng bộ với backend nếu đã đăng nhập) và `onMessageReceived` (chuẩn bị xử lý lightweight data payload ở 6B).
  - `SessionViewModel`: Đồng bộ FCM token tự động khi đăng nhập thành công (`syncFcmToken`); unregister thiết bị trên server trước khi xóa phiên local khi đăng xuất (`logout`).
  - `AndroidManifest.xml`: Khai báo quyền `POST_NOTIFICATIONS`, khai báo `TaktakMessagingService`, bật `firebase_messaging_auto_init_enabled`.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **111/111 tests PASS**, 0 failures, 0 errors, 0 skipped. Gồm 7 integration tests mới trong `DeviceIntegrationTest` kiểm thử đăng ký, re-register cập nhật token, đa thiết bị cho cùng user, chuyển nhượng token giữa các tài khoản, unregister thiết bị, 401 unauthenticated và 400 validation. Executable JAR được build và repackage thành công.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **49/49 tests PASS**, 0 failures, 0 errors (gồm 4 unit tests mới trong `DeviceRepositoryTest`).
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease` **BUILD SUCCESSFUL**, **0 errors**.
- **Kiểm thử trên Android Emulator `Medium_Phone` (Android 17 / API 37)** qua ADB reverse port 8080:
  - `deviceRegistrationAndUnregistrationEndToEnd`: PASS (0.745 s) — Đăng ký thiết bị với token ban đầu, làm mới với token mới, và hủy đăng ký thành công.
  - `multiDeviceRegistrationForSameAccount`: PASS (2.988 s) — Đăng ký đồng thời 2 thiết bị (phone + tablet) cho cùng tài khoản, unregister thiết bị phụ độc lập.
  - Tổng: 2/2 tests PASS (100% success rate).
- **Dọn dẹp môi trường**:
  - Smoke server dừng exit 0: `SMOKE_MEDIA_ROWS=0 FILES=0 RECIPIENTS=0`.
  - Cổng 8080 đã đóng, quy tắc ADB reverse đã gỡ bỏ, emulator dừng an toàn qua `adb emu kill`. Xem [QA_DEVICE_REGISTRATION.md](QA_DEVICE_REGISTRATION.md).

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- **Đề xuất duy nhất**: **Giai đoạn 6B — Bắn thông báo đẩy khi tạo/xóa khoảnh khắc (Push Notification Dispatching)**. Dừng chờ phê duyệt.

## Hoàn tất 5E — Lịch sử ảnh đã nhận (Received Moments History) — 18/10/2026

### Triển khai:
- **Backend API Lịch sử & Phân trang**:
  - `GET /api/moments?type=received&page=0` với Bearer JWT xác thực người dùng.
  - Native query tận dụng index `ix_moment_recipients_user_created` trên `moment_recipients (recipient_id, created_at DESC)` kết nối với `moments` và `users` (sender).
  - Phân trang cố định `pageSize = 20`, lấy `LIMIT 21 OFFSET (page * 20)` để tính `hasNext` không cần đếm toàn bảng.
  - Cô lập bảo mật tuyệt đối: ảnh chỉ mình tôi (`recipientIds=[]`), khoảnh khắc người lạ, hoặc ảnh đã xóa mềm không bao giờ xuất hiện trong danh sách nhận.
  - DTOs `PublicUser`, `MomentItem`, `MomentsPage` chỉ trả về thông tin công khai (id, username, displayName).
- **Android Client Material 3 Expressive History**:
  - Kết nối Tab 1 của ViewPager2 tới `HistoryFragment` hoàn chỉnh (thay thế `PendingTabFragment`).
  - M3E AppBar, thanh tiến trình `LinearProgressIndicator` (indeterminate), trạng thái rỗng (empty state) khi chưa có khoảnh khắc nào, và trạng thái lỗi/thử lại (retry).
  - Thẻ khoảnh khắc `item_history_moment.xml`: Avatar ký tự đầu, tên hiển thị, username, thời gian tương đối ("Vừa xong", "X phút trước"...), khung ảnh vuông tỷ lệ 1:1, chú thích tối đa 50 ký tự Unicode.
  - Tải ảnh qua JWT Bearer token `GET /api/moments/{id}/image`, giải mã Bitmap trên luồng worker và cache RAM 20MB qua `LruCache<String, Bitmap>` trong `HistoryViewModel`.
  - Nút làm mới (Refresh) trên AppBar tải lại dữ liệu tức thì.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **104/104 tests PASS**, 0 failures, 0 errors, 0 skipped. Gồm 5 integration tests kiểm thử nhận khoảnh khắc mới nhất trước, phân trang trang 0 và trang 1, cô lập ảnh chỉ mình tôi, cô lập người lạ, và validation các tham số query `type`/`page`. Executable JAR được build và repackage thành công.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **45/45 tests PASS**, 0 failures, 0 errors (gồm 3 unit tests mới trong `MomentCaptionTest` kiểm thử API contract, session guard và mapping DTO).
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease` **BUILD SUCCESSFUL** trong 32 giây, **0 errors**.
- **Kiểm thử máy thật Xiaomi Redmi Note 13 4G (Android 15 / API 35)** qua USB reverse port 8080:
  - `historyTabShowsEmptyStateWhenNoMomentsReceived`: PASS (3.801 s) — Tab Lịch sử hiển thị chính xác tiêu đề, view rỗng khi chưa có ảnh, RecyclerView ẩn, bấm làm mới thành công.
  - `receivedMomentAppearsInHistoryWithSenderCaptionAndDownloadedImage`: PASS (7.069 s) — Alice gửi khoảnh khắc cho Bob, Bob chuyển sang Tab Lịch sử, khoảnh khắc hiển thị đầy đủ tên Alice, chú thích, ảnh JPEG tải thành công qua JWT với Drawable hợp lệ; xoay màn hình/recreate Activity bảo toàn danh sách.
  - `MainCameraAcceptanceTest#tabsRecreationBackAndLogoutNeverKeepCameraBoundOffscreen`: PASS (35 s) — Kiểm tra hồi quy 4 tab đảm bảo CameraX unbind khi chuyển sang Tab Lịch sử, không rò rỉ tài nguyên.
- **Kiểm tra nhất quán dữ liệu (Data Consistency)**:
  - Smoke server dừng exit 0: `SMOKE_MEDIA_ROWS=3 FILES=3 RECIPIENTS=3`.
  - Tuyệt đối không có duplicate moment trong DB và không có duplicate/orphaned file trên đĩa VPS. Port 8080 đã đóng, quy tắc reverse đã gỡ bỏ.

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- Cổng 8080 đã đóng, quy tắc ADB reverse đã dọn dẹp sạch sẽ.
- **Đề xuất duy nhất**: **Giai đoạn 6 — Firebase Cloud Messaging (FCM) & Đồng bộ thời gian thực**. Dừng chờ phê duyệt.

## Hoàn tất 5D — Upload Progress, Retry & Idempotency Key — 18/10/2026

**PASS local/USB; dừng chờ duyệt riêng 5E.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, bảo toàn các thay đổi có sẵn trong worktree.

### Triển khai:
- **Backend Idempotency bền vững qua PostgreSQL**:
  - Không sửa `V1__init_schema.sql`. Thêm migration `V2__moment_idempotency.sql` bổ sung cột `idempotency_key UUID` và `payload_hash VARCHAR(64)` cho bảng `moments`.
  - Tạo partial unique index `uq_moments_sender_idempotency ON moments (sender_id, idempotency_key) WHERE idempotency_key IS NOT NULL`.
  - Fingerprint SHA-256 tính từ: `sender_id` + `caption` (UTF-8) + danh sách `recipientIds` (sắp xếp tăng dần theo UUID) + raw bytes của ảnh JPEG.
  - Xử lý replay: Nếu cùng key và cùng fingerprint, trả về ngay `201 Created` với `{"id":"<momentId ban đầu>"}`, không tạo bản ghi mới và không lưu ảnh mới lên đĩa VPS.
  - Xử lý conflict: Cùng key nhưng payload khác trả về `409 IDEMPOTENCY_CONFLICT`.
  - Xử lý format: Key sai UUID trả về `400 INVALID_IDEMPOTENCY_KEY`.
  - Khóa bi quan `UserRepository.lockAll(participants)` tuần tự hóa các request đồng thời từ cùng tài khoản sender.
- **Android Progress & Retry**:
  - `CountingRequestBody` theo dõi số byte ghi thực tế của OkHttp để tính tiến trình 0–100%.
  - Phân biệt tiến trình truyền dữ liệu (0–99%, determinate progress bar hiển thị %) với máy chủ xử lý (100%, indeterminate progress bar "Máy chủ đang xử lý và xác thực ảnh...").
  - Lưu `idempotencyKey` trong `SavedStateHandle` của `PreviewViewModel` để bảo toàn key khi Activity bị recreate hoặc process death.
  - Khi người dùng chỉnh sửa nội dung bản nháp (caption hoặc recipients), key cũ được xóa để sinh key mới cho lần gửi độc lập.
  - Khi gặp lỗi mạng, giữ nguyên key và nội dung bản nháp để retry gửi lại đúng dữ liệu ban đầu. Lỗi non-retryable (400, 401, 409) mở khóa cho phép sửa và xóa key.

### Kiểm thử và bằng chứng:
- **Backend**: `mvn verify` đạt **99/99 tests PASS**, 0 failures, 0 errors, 0 skipped. Gồm 4 integration tests kiểm thử replay không duplicate row/file, conflict 409 khi đổi caption/ảnh, validation 400 key sai UUID, và concurrent uploads trả cùng ID. Flyway migration V2 và index DB được kiểm thử tự động.
- **Android Unit Tests**: `./gradlew.bat testDebugUnitTest` đạt **42/42 tests PASS**, 0 failures, 0 errors (gồm 9 tests trong `MomentCaptionTest` kiểm tra header `Idempotency-Key`, `CountingRequestBody` listener, validation và 409 handling).
- **Android Build & Lint**: `assembleDebug assembleRelease lintDebug lintRelease` **BUILD SUCCESSFUL**, 0 errors.
- **Kiểm thử máy thật Xiaomi Redmi Note 13 4G (Android 15 / API 35)** qua USB reverse port 8080:
  - `detailedPhotosHaveBoundedBytesDimensionsAndDoNotUpscaleSmallPhotos`: PASS (1.770 s).
  - `privateSaveUsesExplicitConfirmationAndDownloadsJwtImage`: PASS (7.025 s) — chụp ảnh thật, gửi riêng tư với progress bar, server lưu và tải lại đúng byte bằng JWT.
  - `realCameraSendsSelectedFriendsAndDeniesUnselectedFriend`: PASS (10.596 s) — gửi ảnh cho bạn bè có Idempotency-Key header, tải lại thành công cho người nhận.
- **Kiểm tra nhất quán dữ liệu (Data Consistency)**:
  - Smoke server dừng exit 0: `SMOKE_MEDIA_ROWS=2 FILES=2 RECIPIENTS=2`.
  - **Xác nhận**: Tuyệt đối không có duplicate moment trong DB và không có duplicate/orphaned file trên đĩa VPS.

### Trạng thái Git & Điểm dừng:
- Giữ HEAD `ed9fde6`, worktree giữ nguyên các file chưa commit, không commit/push/deploy.
- Cổng 8080 đã đóng, quy tắc ADB reverse đã dọn dẹp sạch sẽ.
- **Đề xuất duy nhất**: **5E — Lịch sử ảnh đã nhận (Received Moments History)**. Dừng chờ phê duyệt.

## Hoàn tất 5C — Gửi ảnh cho bạn bè — 17/10/2026

**PASS local/USB; dừng chờ duyệt riêng 5D.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, 28 mục dirty/untracked của 5B đã kiểm tra và bảo toàn. Đã đọc AGENTS, toàn bộ đặc tả/roadmap, quyết định, hồ sơ và source. Dùng android_ui_verification trên USB Xiaomi Android 15/API35, 1080×2400 sau xác nhận camera an toàn của người dùng.

Triển khai: Preview gửi selected hoặc Tất cả đủ trang API; multipart repeated UUID, snapshot caption/recipients và generation guard; giữ private-confirm riêng. Server kiểm tra toàn bộ 1–100 người nhận, friendship/block hai chiều, khóa users theo thứ tự PostgreSQL cùng FriendService, lưu moment/recipients cùng timestamp/transaction. GET JWT chỉ cho sender/recipient của moment chưa xóa. Giữ 5 MiB/file, 6 MiB/request, 4096/cạnh, 16 MP; kiểm tra MIME/magic bytes/decode, bỏ EXIF, tên UUID/private storage/rollback. Container giới hạn hữu hạn 104 parts. Nén cạnh 1280/1024/768 và quality 92→52, không upscale/pad. Không đổi stack/JWT/FCM/schema V1 hoặc DB dev.

Kiểm thử cuối: **95/95 backend**, JAR đạt 53.718 s; Android debug/test/release/lint đạt 29 s, 0 errors/3 warnings cũ; **39/39 JVM** buộc chạy lại; **15 ca USB** đạt (11 Preview/5C, 1 gửi bạn bè offline/retry, 3 nav/lifecycle/owner-only regression), không skipped. Kiểm tra ảnh thật gửi hai bạn, đủ 21 bạn qua hai trang, B/C/JWT/block/stale selection/phiên/layout/IME. Xem [QA_MOMENT_SHARING.md](QA_MOMENT_SHARING.md) và [MOMENT_API.md](MOMENT_API.md); logs/screens ignored ở `.local/artifacts/5c/`.

Lỗi ban đầu đã sửa và retest: mặc định container 50 parts chặn biên 100; snapshot pg_stat_activity trong test chờ khóa; PowerShell dừng ở javac Note; một GET fixture gặp EOF keep-alive. Thêm GET retry tối đa một lần, có test chứng minh không retry HTTP/POST hoặc nới auth. Resume đang gửi không enqueue đọc lại draft đã dọn. Không nhận suite từng fail là PASS.

Cleanup: smoke dừng exit 0, **8 moments/8 files/47 recipients**; draft cache 0 file, port 8080 không listener, reverse gỡ, CAMERA granted. Cold Main 1532 ms → Login; hierarchy có emailInput, không previewImage. Không xóa dữ liệu/gallery/DB dev, không commit/push/deploy. HEAD giữ nguyên, index rỗng. Không có thao tác thủ công bắt buộc để nghiệm thu 5C; dùng app dev tiếp cần backend/kết nối vì smoke đã dừng.

Giới hạn: chưa idempotency 5D hoặc khôi phục known ID qua full process death; retry POST chưa rõ kết quả có thể tạo thêm moment. Chưa History 5E, B được kiểm chứng bằng API/JWT, không giả UI. Chưa hai điện thoại vật lý/HTTPS runtime/API26/API37/FCM/widget/deploy. **Đề xuất duy nhất 5D; chờ xác nhận, chưa triển khai.**

## Hoàn tất 5B — Chụp cache và Preview —17/10/2026

**PASS local/USB; chờ duyệt riêng5C.** Trước phiên: nhánh `chore/push-current-project`, HEAD `ed9fde6`, worktree sạch. Commit/push này thuộc yêu cầu riêng trước5B. Đã đọc AGENTS/toàn đặc tả/roadmap/quyết định/hồ sơ/source; dùng android_ui_verification trên USB/API35 sau xác nhận người dùng hướng cả hai camera an toàn.

Triển khai: cache internal UUID/generation có kiểm tra phiên/TTL/byte bounds; Camera → rotation/mirror/crop → cache → Preview. Preview Java/XML/ViewBinding/M3E có session gate, chụp lại, caption50code point, Tất cả/nhiều bạn và API page20/DiffUtil/SavedStateHandle. Có xác nhận lưu owner-only/caption → POST[] → GETJWT; gửi bạn bè chờ5C. Tách Capture/Preview ViewModel, gỡ MomentViewModel và overlay/retry upload cũ khỏi Camera sau kiểm tra tham chiếu; khôi phục source từ Git được. Giữ backend/DB/JWT/FCM/SDK/dependency.

Xác minh: full build cuối29s debug/test/release/lint; bổ sung hai JVM về caption multipart/validation/cross-session, **35/35 đạt** trong lượt unit17s,0fail/error/skipped. Lint0errors/3warnings cũ. **32 ca USB duy nhất đạt sau sửa/retest**:11Preview,18Camera/Friends/auth/ảnh/layout,2quyền,1ảnh thật offline/retry/JWT. Hai ca Friends cần host/thiết bị thứ hai skipped, không tínhPASS. Không gọi các suite từngfail làPASS; trạng thái từng method được đối chiếu sau retest. Log/ảnh ignored `.local/artifacts/5b/`, chi tiết [QA_PREVIEW.md](QA_PREVIEW.md).

Lỗi đã xử lý: Glide onDestroy tạo manager quá muộn → giữ RequestManager từonCreate; quota fixture21tài khoản → chờ Retry-After; dialog click lúc friends thay layout → chờ friends idle; native shellswipe không đổi tab → Espresso đồng bộ UI, giữassertion nav/pager; CameraAllow bị runner kế tiếp gián đoạn → chạy riêng lại đạt. Không nới timeout/limiter/assertion hoặc sửa backend.

Bàn giao: ảnh thật/sáng/tối/IME/offline/JWT/Login đã xem, không overlay. Cache draft cuối0file. Smoke dừngexit0,7media/7privatefile/0recipient,cổng8080đóng; không xóa DBdev/ảnhserver/gallery. Fixtures logout/revoke,reversegỡ,CAMERAgranted,coldlaunch1489ms→Login,khôngpreviewImage trong hierarchy. Index rỗng/HEADed9fde6 giữ nguyên; review XML/link/diff/credential-pattern. Không commit/push/deploy5B.

Giới hạn: chưa gửi ảnh bạn bè5C/idempotency5D/History5E/FCM/widget; chưa API26/API37 networkdialog,TalkBack full,font2×,tablet thật/physical rotation/fullprocessdeath/HTTPSruntime/signing. Cache bịOS thu hồi sẽ báo chụp lại. Backend87test không chạy lại vì source giữ nguyên. Không có thao tác thủ công bắt buộc cho5B; dùngappdevtiếp cần backend/devDB/kết nối vì smoke đã dừng. **Đề xuất duy nhất5C — hoàn thiện media private và người nhận thật; chờ duyệt.**

## Hoàn tất 5A — Bốn tab và CameraX —17/10/2026

**PASS local/USB; dừng chờ duyệt riêng5B.** Người dùng duyệt5A sau bốn màn M3E, xác nhận hai camera hướng an toàn, cài APK và đóng Zalo. Trước phiên: nhánh `chore/push-current-project`,HEAD5b6b520,48mục dirty/untracked của các màn trước được bảo toàn; snapshot201file ignored. Đã đọc AGENTS/toàn đặc tả/roadmap/quyết định/hồ sơ/source, dùng UI UX Pro Max và android_ui_verification;Python3.12.10 sẵn có,giữ stack/dependency/JavaXML/M3E1.14.0.

IMPLEMENT: MainPagerAdapter/ViewPager2 + BottomNavigationView đúng Camera/Lịch sử/Bạn bè/Hồ sơ,click/vuốt/Back/restoretab/IME/insets; private page bị gỡ trước session recheck,Camera không bind từ restored Fragment khi chưa xác minh phiên. CameraPage chứa CameraFragment hiện có; Friends thật giữ4C; History/Profile chỉ thông báo chưa triển khai. Camera trước/sau,flash Off/Auto/On theo phần cứng,khóa thao tác khi busy; nhả use case riêng ở pause/destroy,ticket chống async bind muộn,OrientationEventListener targetRotation,Activity ViewModel giữ lens/flash trong RAM. MomentVM chỉ thêm guard captureFailed sau cleared; callback dùng model đúng lượt. Không sửa Repository/API/JWT/backend/DB/FCM/dependency hoặc triển khai5B.

VERIFY: fullbuild cuối19s đạt debug/test/release+lint;33JVM thực chạy ở các lượt build đầu/sau,0failure/error/skipped,cuối reuse task;lint0errors/3warnings cũ. **33ca USB duy nhất đạt**:6MainCamera47.243s,7Friends/layout51.651s,2permission Deny/Allow4.923s/6.964s,15auth/M3/media/layout regression88.075s,1chụp thật/offline/retry/JWT43.807s,2sessionoffline/revoked52.625s. Không cộng baseline/lượt lặp hoặc các lượt fail. MIUI chặn testAPK→retrySuccess; sai tên class ở lệnh regression→sửa tên/rerun; host phục hồi connector muộn làm sessionoffline+cleanup fail→khôi phục đúng thời điểm/rerun2/2. Không nới assertion/timeout hoặc sửa production để né harness. Xem [QA_MAIN_CAMERA.md](QA_MAIN_CAMERA.md),log/ảnh ignored `.local/artifacts/5a/`.

REVIEW/CLEANUP: ảnh Camera/History/rear-upload/landscape/offline/Login cuối xem trực tiếp,không overlay;ảnh front không đưa Git. 33ca gồm chụp cả hai camera/flashON,byte hiển thị khớp GET server,và rotation JPEG bốn góc;landscape là forced Activity configuration trên phone,không nhận là người dùng xoay vật lý. Snapshot xác nhận13file cũ sửa/17file mới/0xóa trong5A (25Android+5docs),84server/data/build/manifest giữ hash;preexistingM3E giữ nguyên ngoài các file mở rộng đã đọc. Quét credential/conflict/whitespace/XML/link/diff,index rỗng;không commit/push/deploy. Smoke riêng dừng exit0,7media/7privatefile/0recipient,cổng8080 đóng;không xóa DB/ảnh/data app. Fixture logout/revoke,reverse8080 gỡ,CAMERA granted,coldMain launch1981ms→Login,handoff.xml không pager/privateimage.

GIỚI HẠN: chưa API26/API37 local-network dialog,TalkBack full,font2×,tablet thật,physical-rotation capture/full process-death suite,HTTPS runtime/signing/FCM;87backend tests không chạy lại vì source không đổi. History/Profile chưa chức năng;mirror/cache/Preview/caption/chọn bạn chưa làm. Không có thao tác thủ công bắt buộc cho5A. Muốn dùng app dev tiếp cần backend/devDB/kết nối phù hợp vì smoke đã dừng. **Đề xuất duy nhất5B — cache/crop/mirror khi cần và màn Preview/chụp lại/caption/chọn bạn; chờ duyệt, không triển khai.**

## Hoàn tất M3E-04 — Bạn bè/Friends —17/10/2026

**PASS local/USB; dừng chờ duyệt riêng 5A.** Người dùng duyệt “màn 4 — Bạn bè/Friends”. Trước phiên: M3E-03 đã đạt; nhánh `chore/push-current-project`, HEAD5b6b520, dirty/untracked màn1/2/3 được đọc và bảo toàn, snapshot198file ignored. Không commit/push/deploy hoặc tạo màn History/Profile/widget mới.

UNDERSTAND/PLAN: đọc AGENTS/toàn đặc tả/roadmap/quyết định/hồ sơ/source/test. Dùng UI UX Pro Max và android_ui_verification;Python3.12.10 sẵn,CLI lỗi cp1252 được sửa bằng output UTF-8 process. Giữ palette/font/Material1.14.0/JavaXML/ViewBinding và mọi API/VM/Repository/JWT/FCM/backend/DB/dependency.

IMPLEMENT: Friends scoped theme,max480dp,Coordinator/AppBar header cuộn với RecyclerView,card24dp/avatarLavender,actionExpressive/tonal/semanticnotice/progress slot. Dialog dùng root themed context;Main chỉ đổi Friends root surface M3E. Adapter/Glide/session/logic giữ nguyên. Thêm searchvector và FriendsM3Test3;gỡ hai typography cũ hết dùng khỏi camera_styles sau tìm reference,khôi phục từGit được;không xóa file.

VERIFY: baseline2/2 trước sửa33.904s;build đầu30s,build cuối26s,28unit thực chạy0fail/error/skip;lint debug/release0errors/3warnings cũ sau dọn3warning mới. Test APK cuối12s. **20/20ca USB đạt**:4M3/layout23.898s (sáng/tối/font1.5/3viewport mô phỏng,real swipe collapse/expand,rendererbusy,acceptAPI thật/notice,IME/search),5Friendscore49.375s,1offline/retry45.426s,10auth/Camera regression48.375s. Các lượt UI/compile fail đã ghi nguyên nhân/sửa/retest,không bỏ ca hoặc nới assertion:restore sau đổi theme/globalrect TIL,lỗiUiDevice dependency dùng lại shellADB,swipe sát vùnggesture làmrờiForeground→clip viewport/vuốt1/4–3/4. Không thay production auth hoặc cài đặt hệ thống. Chi tiết [QA_M3E_FRIENDS.md](QA_M3E_FRIENDS.md),artifacts `.local/artifacts/m3e-friends/`.

REVIEW/CLEANUP: đã xem trực tiếp ảnh trước/sau, sáng/tối/cuộn/thành công/bàn phím và dialog chặn/offline; không overlay. Smoke QA riêng dừng exit0,0media/0file/0recipient; QA logout/revoke, night-mode app khôi phục, reverse8080 gỡ, cold launch1312ms về Login/no friendsRecycler, handoff.xml/png ignored. Không clear app data/xóa DB dev/ảnh hoặc đổi bảo mật điện thoại. Đúng12file có sẵn sửa/3mới/0xóa (10Android+5docs);56server giữ SHA256. Quét15file credential-pattern/conflict/whitespace đạt,7XML/84link hợp lệ,diff --check đạt,index rỗng,artifacts/Firebase JSON ignored. Bảo toàn thay đổi3màn trước; không chạy lại87backend vì không đổi source.

GIỚI HẠN: chưa API26/API37 local-network dialog, TalkBack toàn luồng, font2×, tablet/landscape thật, UI page>20/UI429 cooldown, hai thiết bị chạy lại, HTTPS runtime/signing/FCM. Không cần thao tác thủ công để hoàn tất màn4; dùng app dev tiếp cần backend/dev DB/kết nối vì smoke đã dừng. **Bốn màn có sẵn đã redesign; đề xuất duy nhất quay lại 5A — hoàn thiện điều hướng bốn tab và CameraX, cần người dùng duyệt riêng. Dừng.**

## Hoàn tất M3E-03 — Camera + trạng thái phiên —17/10/2026

**PASS local/USB; dừng chờ duyệt M3E-04 — Bạn bè/Friends.** Người dùng duyệt riêng màn3, xác nhận camera hướng đồ vật đủ sáng. Nhánh `chore/push-current-project`, HEAD5b6b520;25 mục dirty/untracked màn1/2 có sẵn được bảo toàn, snapshot195file lưu ignored. Không commit/push/deploy hoặc chuyển5A.

UNDERSTAND/PLAN: đọc đặc tả/AGENTS/quyết định/hồ sơ/source/test; dùng UI UX Pro Max và android_ui_verification để ưu tiên ảnh/shutter, contrast/target, light/dark, context màn hẹp/font1.5 và ảnh thật USB. Giữ Material1.14.0/Java/XML/ViewBinding, không đổi VM/Repository/API/JWT/FCM/backend/DB/dependency.

IMPLEMENT: activity_main/fragment_camera max480dp, ảnh vuông bo28dp, shutter Coral Expressive88dp ngay dưới ảnh; session card checking/error/permission/retry. Scoped theme trên cây View Camera/phiên giữ Activity/Friends theme cũ. MainActivity chỉ presentation/insets/heading; CameraFragment chỉ đổi màu renderer. Thêm token/selector/string và CameraM3Test3 ca; gỡ hai selector shutter cũ cùng style/string/màu hết tham chiếu, khôi phục từ Git được. Không flash/đổi camera/tab/caption hoặc chức năng mới.

VERIFY: build debug/test/release+unit/lint đầu31s,28unit thực chạy PASS; test-build15s; build cuối20s(unit up-to-date),lint0errors/3warnings cũ. **20/20ca USB đạt**: CameraM3Test3/46.038s, CameraDeny1/4.699s, Allow1/6.473s, chụp thật/offline/retry/JWT1/45.119s, Camera UI/privateAPI/crop/layout4/15.935s, auth/Login/Register/FriendsLayout9/48.007s, navigation Friends/recreate/back/logout1/9.520s. Baseline UI1/14.684s trước sửa không cộng thành ca nghiệm thu mới. Test mới CHECKING/busy là renderer-only; permission mạng mô phỏng check(false) trên API35, không nhận là API37 dialog. Camera Deny/Allow và offline HTTP/I/O/API ảnh là thật. Lượt harness đo detached FCV lỗi đã sửa bằng slot cùng context/layout params rồi retest, không thay production/nới assertion. Xem [QA_M3E_CAMERA.md](QA_M3E_CAMERA.md), `.local/artifacts/m3e-camera/`.

REVIEW/CLEANUP: xem trực tiếp ảnh trước/sau sáng/tối/busy/offline/server và session offline/recovered; không overlay, ảnh đủ sáng, bytes tải khớp server. Backend QA riêng dừng exit0,2media/2privatefile/0recipient,cổng8080 đóng; không xóa test data/DBdev/app data. Fixture logout/revoke,reverse8080 gỡ,CAMERA granted,coldlaunch1334ms về Login;hierarchy không privateimage vàhandoff.png. Thay17Android+5docs, tách khỏi thay đổi màn1/2 bằng baseline;56server/VM/Repo/SDK/auth screens giữ hash,không sửaFirebase. Review20file hiện có/12XML/81link local đạt,mẫucredential/conflict/whitespace không phát hiện,diff --check đạt,index rỗng,artifacts/Firebase JSON ignored. Không kiểm thử lại87backend vì source không đổi.

GIỚI HẠN: chưa API26/API37 local-network dialog,TalkBack toàn luồng,font2×,tablet/landscape thật,HTTPSruntime/signing/FCM. Không có thao tác thủ công bắt buộc để hoàn tất màn3. Backend smoke đã dừng; dùng app dev tiếp cần backend/devDB/kết nối phù hợp. **Đề xuất duy nhất M3E-04 Friends; chờ xác nhận, không triển khai tiếp.**

## Hoàn tất M3E-02 — Register —17/10/2026

**PASS local/USB;dừng chờ duyệt M3E-03 Camera + trạng thái phiên.** Người dùng yêu cầu riêng màn2 sau Login. Nhánhchore/push-current-project,HEAD5b6b520;17thay đổi M3E-01 chưa commit trước phiên được bảo toàn,baseline191file tracked/untracked lưu ignored. Không commit/push/deploy hoặc chuyển5A.

UNDERSTAND/PLAN: đọc AGENTS/đặc tả/quyết định/hồ sơ/source/test;tiếp tục UI UX Pro Max và android_ui_verification đã dùng trong audit/màn1. Giữ Material1.14.0/JavaXML/ViewBinding và mọi nghiệp vụ/backend/API/JWT/FCM. Form5trường,max480dp,scroll/IME,font lớn và theme riêng.

IMPLEMENT: theme Register trong Manifest/m3_styles,layout nhóm identity/security,two heading ViewCompat trong RegisterActivity,2vector+2string;thêm RegisterM3Test. Giữ ID binding/validator/password clear/no auto-login. Gỡ auth_styles.xml6style đã tìm không còn tham chiếu,khôi phục được từ Git. Login layout/activity/test,AuthFormActivity và code các màn khác giữ nguyên so với đầu phiên.

VERIFY: full debug/test/release+unit/lint **BUILD SUCCESSFUL30s**,0errors/3warnings cũ;28unit thực chạy,0fail/error/skip. Test APK/lint cuối22s. **4/4Register USB57.490s**:font1.5/lightdark/3viewport mô phỏng,longform/validation/loading/toggle/recreate/IME,offline thật→retry→API tạo tài khoản→rawlogin200/revoke204→duplicate409,noautosession. Lượt đầu4/4 cũng đạt54.381s;chỉnh ảnh bottom để cuộn đến action Login rồi cài APK cuối và chạy lại. **7/7regression29.510s**:3LoginM3 +2LauncherAuth +CameraLayout/FriendsLayout. Không chạy lại87backend tests,không tính lượt lặp thành ca mới. Xem [QA_M3E_REGISTER.md](QA_M3E_REGISTER.md),`.local/artifacts/m3e-register/`.

REVIEW/CLEANUP: ảnh đầu/cuối form sáng/tối,keyboard/success đã xem trực tiếp,không overlay. Backend QA tạm dừng exit0,0media/cổng8080 đóng;QA tokens chỉ RAM/rawrevoke,không phiên cá nhân. Reverse gỡ,coldlauncher1452ms vềLogin rồi mởRegister bằng node/bounds;hierarchy vàhandoff.png xác nhậnRegister. Không đổi cấu hình máy/xóa app data hoặc DBdev. Thay9Android+5docs trong phạm vi màn2,các thay đổi màn1 có sẵn giữ nguyên trừ mở rộng Manifest/m3_styles/docs đã đọc. SHA256 so baseline:8file sửa/5mới/1style gỡ,56server giữ nguyên;13file quét credential-pattern/conflict/whitespace đạt,6XML/77link hợp lệ,diff --check đạt,index rỗng. Chưa API26/API37deny/TalkBack toàn luồng/tablet/landscape thật/font2×/HTTPSruntime/signing. **Không cần thao tác thủ công để hoàn tất màn2;chờ xác nhận Camera.**

## Hoàn tất M3E-01 — Foundation + Login —17/10/2026

**PASS local/USB; dừng chờ duyệt M3E-02 Register.** Người dùng đã duyệt audit/design system M3E rồi yêu cầu riêng màn1. Trước phiên:4C hoàn tất, commit5b6b520 đã được commit/push trên `chore/push-current-project` theo yêu cầu riêng; worktree sạch. Không commit/push thay đổi phiên này.

UNDERSTAND/PLAN: đã đọc đặc tả/AGENTS/quyết định/source/test và sử dụng UI UX Pro Max từ audit, android_ui_verification cho USB. Material1.14.0 đã có; chỉ opt-in Login vào Expressive, không nâng dependency hoặc thay backend/DB/API/JWT/FCM/ViewModel/Repository.

IMPLEMENT: thêm8 resource files m3 colors light/night,dimens/styles,brand/notice backgrounds,mail/lock vectors; sửa activity_login và theme riêng Login trong Manifest; AuthFormActivity chỉ thêm cutout vào insets. Giữ LoginActivity và binding IDs, validator/loading/navigation. Thêm LoginM3Test, không sửa test cũ. Form max480dp,corner20dp,headline32/body16,CTA56/action48,progress slot,notice Lavender. Register giữ theme/layout cũ, chỉ dùng chung cải thiện cutout.

VERIFY: debug/test/release build +lint PASS,0errors/3warnings cũ;28unit **chạy lại** PASS29s. **4/4 Login USB66.627s** đủ light/dark,font1.5/ba viewport mô phỏng,validation/loading/toggle/recreate,IME/Register,offline thật/retry/đúng profile/logout. **4/4 regression USB14.377s** launcher/auth/phiên/logout +CameraLayout/FriendsLayout. Fixture mới với ownership guard; PostgreSQL/JWT tạm, USB reverse, không tài khoản cá nhân. Hai lỗi harness đầu (error icon và local/global rect), một race chuyển Activity lúc logout đã sửa/retest; không nới assertion API hoặc sửa nghiệp vụ. Ảnh đầu có Zalo, người dùng đóng, ảnh cuối logo sạch. Không nhận suite từng fail hoặc test cache là mới PASS. Chi tiết/lệnh tại [QA_M3E_LOGIN.md](QA_M3E_LOGIN.md), artifacts ignored `.local/artifacts/m3e-login/`.

REVIEW/CLEANUP: xem trực tiếp ảnh cuối light/keyboard/offline. Smoke dừng exit0,0moment/0file/0recipient,cổng8080 đóng; QA logout về Login và marker riêng xóa trong test. Điện thoại ngắt USB sau kiểm thử,adb devices rỗng cả hai môi trường nên chưa xác minh cold launch/gỡ reverse cuối phiên; không ghi các thao tác đó PASS. Không đổi cấu hình máy,không xóa dữ liệu cá nhân/cluster test. Chưa API26/TalkBack đầy đủ/tablet/landscape thật/HTTPS runtime/release signing; viewport rộng là đo layout theo context. Backend87 test không chạy lại vì source giữ nguyên. Thay12Android +5docs;17file kiểm tra,10XML hợp lệ,74link local hợp lệ,diff --check/credential-pattern/conflict/whitespace đạt;Git xác nhận chỉ đúng3source cũ và4docs cũ sửa,index rỗng. **Không có thao tác thủ công bắt buộc để hoàn tất màn1; chờ xác nhận Register.**

## Hoàn tất4C —17/10/2026

**4C PASS local/USB/AVD; dừng trước5A, không commit/push/merge/deploy.** Người dùng duyệt riêng FriendsFragment nối API qua Repository/ViewModel. Trước phiên:4A/4B server đã đạt, Android chưa có Friends UI; dirty/untracked từ các bước trước được bảo toàn. Đã đọc toàn bộ đặc tả/AGENTS và đối chiếu source/quyết định/Git. Nhánh thực tế `chore/push-current-project`, HEAD8766f26, không phải main như tài liệu cũ; reflog xác minh nhánh này được checkout trước HEAD, không tự đổi nhánh.

UNDERSTAND/PLAN: tái dùng API4A/4B và JWT/refresh, chỉ UI Friends; một RecyclerView có search/incoming/friends, empty/loading/error/retry/page. Camera→Friends entry tối thiểu, không làm bốn tab của5A. Avatar local qua Glide vì backend chưa có avatar API; không đổi quyết định kỹ thuật hoặc thêm URL public.

IMPLEMENT: FriendModels/FriendApi/FriendRepository/FriendsViewModel/FriendsFragment/FriendsAdapter,3 layout,avatar/strings và3 file test. Sửa đúng4 file cũ:MainActivity,ApiClient,AuthRepository,activity_main. Repository kiểm tra generation/DTO, ViewModel I/O worker +LiveData snapshot, tránh publish dữ liệu cũ/loop khi đổi tài khoản. Gửi mời/accept/reject/unfriend/block;confirm hủy/chặn, page20/dedupe,tải lại không tự replay mutation. Main gỡ Camera khi mở Friends và dùng cùng cổng phiên/logout. Không sửa server/schema/dependency/Firebase/README/spec.

VERIFY: **28/28 JVM**,**Maven verify87/87 +JAR**,0 failures/errors/skipped. Android debug/test/release build và lint đạt,0 errors/3 cảnh báo cũ. APK cuối sau chỉnh header màn hẹp: **6/6 USB PASS52.116s** (5 ca core +FriendsLayout hai theme/font1.5), trước đó5/5 core49.743s. **7/7 regression USB16.209s**: launcher/auth/mã hóa/refresh/logout và CameraLayout. **Hai vai hai thiết bị PASS** A/USB14.307s+B/AVD38.971s: mời/accept/quan hệ/chặn, B gửi API404 và C không có quan hệ. AVD4 core đã đạt; ca account switch từng bị quota khi chạy song song, chạy lại cùng network test **2/2 PASS73.804s**; connector offline26s/online, giữ phiên và retry thành công. Không cộng số lượt lặp thành ca duy nhất, không gọi suite có failure là PASS. Chi tiết/ảnh/log tại [QA_FRIENDS.md](QA_FRIENDS.md), `.local/artifacts/4c/` ignored.

REVIEW: phát hiện/sửa crash Glide sau Activity destroyed bằng RequestManager theo Fragment; recreate/expired/logout đã chạy lại đạt. Chặn mở Activity ngầm MIUI được xử lý trong harness bằng shell launcher, không đổi auth production. Viewport/popup animation được kiểm tra bằng Espresso visible90%; MIUI install bị chặn/PiP che dialog/AVD System UI ANR/ADB khác user/quota login đều có bằng chứng và retest, không nới limiter/focus/assertion hoặc bỏ ca. Header dùng weight tránh tràn ngang màn hẹp. Snapshot xác nhận8 file cũ sửa/15 file mới (18 Android +5 docs),không xóa file;51 file server và66 file Android cũ khác giữ hash.23 file quét mẫu credential/conflict/trailing whitespace đạt,73 link local hợp lệ,git diff --check đạt. Spec/Firebase JSON giữ hash,index rỗng/HEAD8766f26;JAR không chứa test harness/EmbeddedPostgres,artifacts được ignore. Đây là kiểm tra chọn lọc,không audit tuyệt đối mọi lịch sử Git.

CLEANUP: smoke dừng **exit0**, metadata3 moment/3 private file/0recipient khớp; không mở/xem/xóa ảnh. Fixture logout, phone cold launch1667ms về Login (dump có emailInput/không friendsRecycler); reverse8080 gỡ cả hai thiết bị; AVD của phiên QA đã tắt. Không thay timeout/animation/network/security điện thoại, không reset/xóa DB hoặc app data. Các DB/storage/artifacts tạm vẫn ignored trên disk, không dùng làm tài khoản dev bền vững.

GIỚI HẠN: avatar mặc định local, chưa API avatar/UI quản lý unblock; chưa UI pagination hơn20, UI429 hết cooldown, runtime API26/HTTPS release/TalkBack/load test. Toàn bộ recipient-aware media giai đoạn4 chưa được nhận PASS: server3D vẫn từ chối mọi recipient khác rỗng, phải kiểm tra block riêng khi triển khai5C; không thêm gửi ảnh bạn bè ở4C.

**Không còn blocker cho4C hoặc bước thủ công bắt buộc.** Muốn dùng app dev tiếp cần khởi động backend/dev DB và reverse phù hợp (smoke đã tắt); tài khoản QA không phải tài khoản thật. **Đề xuất duy nhất5A — hoàn thiện điều hướng bốn tab và CameraX; chờ người dùng phê duyệt, chưa triển khai.**

## Hoàn tất4B —17/10/2026

**4B PASS backend local; dừng trước4C, không commit/push/deploy.** Người dùng duyệt riêng accept/reject, friendship, hủy bạn và block. Trước phiên:4A PASS65 test, friend service đã có send/search/incoming; main8766f26/worktree dirty từ các phiên trước. Đã đọc toàn bộ đặc tả, AGENTS, đối chiếu quyết định/trạng thái/Git/schema/source; giữ thay đổi có sẵn.

UNDERSTAND/PLAN: dùng schema V1 và lockPair UUID PostgreSQL của4A cho tất cả mutation; chỉ receiver phản hồi request. Transaction canonical friendship/status, block xóa friendship/reject pending cùng commit; public list/page và idempotency. Không Android, Friends UI4C, gửi ảnh bạn bè5C, FCM hoặc deploy.

IMPLEMENT: sửa5 file friend/FriendRequest, FriendRequestRepository, FriendDtos, FriendService, FriendController; thêm FriendRelationIntegrationTest. PUT request action accept/reject; GET friends page20/public DTO; DELETE friends/{id} là target userId; POST/DELETE blocks chỉ hướng actor. Respond kiểm tra receiver qua projection trước khóa users, sau đó đọc/khóa entity request để không dùng status cũ. Retry cùng action200 không đổi timestamp/không resurrect sau unfriend, trái action409; block pending rejected/giữ lịch sử, unblock không restore. Native bind SQL/JPA trong transaction, không migration/dependency/JWT hoặc Android.

VERIFY: Maven verify **87/87 PASS**,0 failures/errors/skipped,46.821s +executable JAR;22 test4B mới4.648s và65 regression4A/auth/profile/media chạy lại. Các ca gồm UUID high-bit normalization/receiver-only/DTO/input/JWT/page/block hai chiều,12 accept retry/accept-reject, accept-block4cặp×12calls, send-block/unfriend-accept, chờ users lock thật rồi rejectcommit, rollback cả request/friendship và block/cleanup. First21ca18PASS/3FAIL do assertion containsOnly yêu cầu mọi outcome và pg_stat_activity snapshot cũ; sửa test dùng tập outcome hợp lệ kèm6block204/invariants, clear statistics snapshot, không nới timeout hoặc bỏ ca. Retest21/21 đạt13.786s, sau đó thêm ca unfriend/accept và full87/87 đạt. Log ignored `.local/artifacts/4b/`; chi tiết [FRIEND_RELATIONS_API.md](FRIEND_RELATIONS_API.md).

REVIEW/HANDOFF: cluster/storage mới dưới server/target, HTTP loopback port ngẫu nhiên, context/PG đóng trong @AfterAll; không DB dev/VPS hoặc phone/USB. Block và quan hệ4A/4B đã phối hợp, nhưng media vẫn owner-only3D, không nhận đã nghiệm thu recipient-aware5C hoặc UI/hai thiết bị toàn giai đoạn4. Không benchmark production/distributed limiter. Không có blocker/thao tác thủ công bắt buộc cho4B.

Hồ sơ: tạo FRIEND_RELATIONS_API.md; cập nhật FRIEND_API (note lịch sử4A), API_ERRORS, PROJECT_DECISIONS, PROJECT_STATUS, TASKS, SESSION_HANDOFF. Snapshot cuối:chỉ5 source friend đổi/1 test mới,70 file Android/V1/source khác giữ nguyên, không xóa file.13 file quét credential/conflict/whitespace và80 link local đạt;git diff --check đạt,JAR không chứa test classes,index rỗng/HEAD8766f26,spec/Firebase hash giữ nguyên,artifact/cluster ignored. Đây là kiểm tra chọn lọc không audit toàn lịch sử. Giữ spec/README/schema/dependency/Firebase config/Android và thay đổi cũ. **Đề xuất duy nhất4C — FriendsFragment nối API qua Repository/ViewModel; chờ người dùng duyệt.**

## Hoàn tất4A —17/10/2026

**4A PASS backend local; dừng trước4B, không commit/push/deploy.** Người dùng duyệt riêng search username và friend requests server. Trước phiên: UI-CAMERA/3D PASS, chưa có package friend hoặc API search. Đã đọc toàn bộ đặc tả/quyết định và đối chiếu trạng thái/Git/source; main8766f26, bảo toàn dirty/untracked các phiên trước.

UNDERSTAND/PLAN: tái sử dụng bảng/index V1, JWT/error format/JPA; chỉ search, gửi và list pending đến. Không accept/reject/friendship mutation/block API/Android/FCM. Search public DTO, lọc block hai chiều/rate limit; gửi mời chống race; integration test HTTP/PostgreSQL thật và regression.

IMPLEMENT: friend/FriendRequest, FriendRequestRepository, FriendDtos, FriendService, FriendController, FriendFailure, SearchRateLimiter, SearchThrottled; sửa UserRepository thêm native search/lockPair, ApiExceptionHandler thêm lỗi friends/search429. Search prefix1..255, literal underscore, max20,30/user+60/IP/60s,10.000 key single JVM, remoteAddr không tin XFF. POST receiverId, sender JWT, khóa users ORDER BY UUID PostgreSQL trước kiểm tra block/pending/friendship, unique V1 bảo vệ thêm. GET incoming page20/hasNext/sort createdAt-id và block hai chiều. Không sửa V1/dependency/JWT hoặc Android.

VERIFY: lượt cuối Maven verify **65/65 PASS,0 failures/errors/skipped**,44.283s, executable JAR build/repackage;14 FriendIntegrationTest(4.428s)+5 SearchRateLimiterTest(0.595s)+46 regression. Gửi12 request đồng thời hai chiều chỉ1x201/11x409; DB unique, rollback/retry, DTO/mass assignment/isolation, block cả hai chiều, pagination, JWT thiếu/giả/expired/orphan và quota/XFF đều kiểm tra thật. Các lỗi log rollback/duplicate ở các ca negative là mong đợi, không lỗi test. Lượt đầu fixture orphan gọi TokenService.issue tạo refresh cho user không tồn tại gây1 ERROR do FK; sửa chỉ test dùng JwtEncoder ký JWT orphan, giữ FK/JWT/401. Chi tiết tại [FRIEND_API.md](FRIEND_API.md); log local ignored `.local/artifacts/4a/`.

REVIEW/HANDOFF: test chạy cluster/storage tạm trong server/target, loopback port ngẫu nhiên, context/PostgreSQL đóng trong @AfterAll; không tác động DB dev/VPS. Android không đổi nên không build/chạy USB lại, không nhận QA cũ là vừa chạy. Chưa benchmark dataset production, chưa distributed/trusted-proxy quota; prefix20/offset incoming có giới hạn đã ghi rõ. Race với block/accept API4B chưa tồn tại không nhận là đã nghiệm thu;4B phải cùng quy ước khóa cặp. Không có blocker4A.

File hồ sơ: tạo FRIEND_API.md; cập nhật API_ERRORS, PROJECT_DECISIONS, PROJECT_STATUS, TASKS, SESSION_HANDOFF. Source/test4A chỉ các file nêu trên và hai test mới FriendIntegrationTest/SearchRateLimiterTest. Review cuối:snapshot chỉ2 source cũ sửa/10 source-test mới,70 file Android và V1 giữ nguyên;18 file quét credential/conflict/whitespace đạt,74 link local hợp lệ,git diff --check đạt. JAR có FriendController/không test classes; index rỗng/HEAD8766f26, spec/Firebase hash giữ nguyên, artifacts/cluster ignored. Đây không phải audit toàn lịch sử. Giữ toàn bộ thay đổi UI/auth/media cũ; không sửa spec/README/migration/Firebase config hoặc credentials. Không cần thao tác thủ công để hoàn tất4A. **Đề xuất duy nhất4B — chấp nhận/từ chối, friendship/hủy bạn và block; chờ người dùng duyệt.**

## Hoàn tất UI-CAMERA — 17/10/2026

**PASS local/USB cho redesign Camera đang hoạt động sau3D. Dừng; chưa duyệt4A, không commit/push/deploy.** Người dùng đã duyệt rõ yêu cầu ui-ux-pro-max và cài Python nếu thiếu, giữ Java/XML/ViewBinding/Material/CameraX/API/JWT/phiên; không flash/front camera/tab/tính năng mới. Các mục3D trở xuống là lịch sử.

UNDERSTAND: đọc đầy đủ AGENTS, đặc tả, quyết định, trạng thái/tasks/handoff, attachment và source; Git main8766f26, worktree đã dirty/untracked từ3B/3C/3D. Snapshot SHA-256102 file Android/server trước sửa để phân biệt thay đổi phiên này. PLAN: theme riêng Main/Camera, preview/shutter là trọng tâm, semantic status và quyền, xác minh build/test/USB trước–sau.

IMPLEMENT: cài Python3.12.10 per-user bằng winget, xác minh executable, chạy thật design-system/domain CLI của ui-ux-pro-max. Loại Claymorphism generator vì trái yêu cầu; chọn soft minimal coral/warm background, sáng/tối. Thêm theme Camera riêng, preview vuông bo24dp, shutter88dp/tint disabled, status card16dp, retry sát lỗi, header/logout gọn. Chỉ sửa presentation; API, Repository, ViewModel, JPEG, auth/session và server giữ nguyên. android_ui_verification hướng dẫn calibrate/bounds/STREAMING, quyền theo node và screenshot, không tranh UiAutomation.

VERIFY trên APK cuối: debug/test/release +lint **BUILD SUCCESSFUL20giây**,20 unit0 failures/errors; lint debug/release0 errors/3 warnings cũ. Máy23129RAA4G/API35/1080×2400/density440: Deny1/1(5.913s), từ chối rồi cấp lại1/1(10.249s), suite chọn lọc11/11(45.375s), offline/retry1/1(48.397s). Tổng14 successful executions/13 ca duy nhất; không phải chạy toàn suite mặc định hoặc Maven46 test lịch sử. Có layout fragment320dp/chữ150% bằng ConfigurationContext riêng, preview sáng/tối, busy/progress/disabled, ảnh thật round-trip khớp byte JWT, auth/restore/logout và private API regression. Cold signed-out1881ms về Login, theme Auth không đổi.

REVIEW: đã xem ảnh cuối tại `.local/artifacts/ui-camera/after-verified/`; before/before-clear có PiP ở header, ghi rõ hạn chế baseline. Sửa helper Allow bị predicate click2lần; ghi các lượt FAIL/host restore trễ và retest trong [QA_CAMERA_REDESIGN.md](QA_CAMERA_REDESIGN.md), không bỏ qua kiểm thử. Màu12cặp chính>=4.5:1; không nhận đã audit TalkBack toàn diện. Không runtime API26/tablet/release HTTPS hoặc mọi góc xoay.

File: MainActivity.java, CameraFragment.java, activity_main.xml, fragment_camera.xml, CameraMomentAcceptanceTest.java; mới CameraLayoutTest.java, camera_colors(light/night), camera_styles, camera_strings, shutter tint/icon selectors và ic_camera_shutter. Tạo QA_CAMERA_REDESIGN; cập nhật PROJECT_STATUS/TASKS/SESSION_HANDOFF/UI_REDESIGN_REVIEW. Không sửa quyết định/spec/README/dependency/migration/Firebase config/server. Chi tiết đường dẫn và bằng chứng trong QA_CAMERA_REDESIGN.

HANDOFF: fixture tự logout; CAMERA trả granted, không đổi theme/font scale/timeout hệ thống. Gỡ đúng reverse tcp8080; backend tạm exit0,10moment/10file/0recipient, cổng8080 đóng, ảnh fixture giữ ignored. App debug vẫn cài, đã về Login; muốn dùng lại cần khởi động backend/dev config và reverse. Review cuối:97/102 file snapshot giữ nguyên, chỉ5 file source/test/layout sửa và8 file mới;40 file server không đổi. Parse22 XML,18 file quét credential/conflict/whitespace,56 link local và git diff --check đạt; index rỗng/HEAD8766f26, đặc tả/Firebase hash giữ nguyên. Đây không phải audit toàn bộ lịch sử Git. Không có blocker nghiệm thu UI. **Đề xuất duy nhất khi muốn tiếp tục roadmap:4A — search username/friend requests backend; chờ người dùng duyệt.**

## Hoàn tất3D —16/10/2026

**Trạng thái mới nhất:3D PASS backend local và điện thoại Android15. Chờ duyệt4A; không tự chuyển bước/redesign hoặc commit/push/merge/deploy.** Các phần3C trở xuống là lịch sử. Trước phiên:3C đã PASS, Camera còn placeholder, chưa có media API. Đã đọc toàn bộ đặc tả và đối chiếu AGENTS/quyết định/Git/source; giữ dirty/untracked3B/3C/UI review, HEAD main8766f26.

UNDERSTAND/PLAN: xây đúng lát cắt CameraX sau → JPEG riêng tư → POST moments với danh sách rỗng → GET ảnh JWT và hiển thị dữ liệu server. Không flash/front camera/chọn bạn/caption UI/lịch sử/FCM/widget. IMPLEMENT: dùng dependency/schema có sẵn, thêm storage private/transaction cleanup, kiểm tra chủ sở hữu mỗi GET; Repository/ViewModel dùng transport/refresh phiên hiện có và generation guard. Skill android_ui_verification giúp chờ lifecycle/streaming, kiểm tra quyền, screenshot và không tranh UiAutomation; không chuyển stack hoặc redesign.

VERIFY cuối: Maven verify **46/46 PASS**,38.120s, gồm7 media +39 regression; JAR build. Android debug/test/release +lint BUILD SUCCESSFUL lượt cuối18s,0 errors/3 warnings cũ;20 unit0 failures/errors (đã chạy lại khi đổi Java, lượt cuối UP-TO-DATE). Máy thật23129RAA4G/API35/1080×2400:4 ca media mới PASS (chụp thật/offline/retry38.076s;Deny5.409s;2 API/processing2.480s),6 auth regression17.591s. Sau người dùng xác nhận hướng camera vào đồ vật đủ sáng, chụp lại **1/1 PASS8.684s**, đã xem ảnh có đồ vật; byte hiển thị khớp GET lại. Tổng11 successful executions/10 ca duy nhất ở các lượt cuối, **không phải toàn bộ31 test mặc định đã chạy**. Chi tiết/lỗi trước đó tại [QA_SELF_MOMENT.md](QA_SELF_MOMENT.md).

REVIEW: JWT thiếu/sai401,B404, ảnh không public/no-store, không recipient/FCM, file giả/quá lớn/pixel bomb/rollback được kiểm tra. Không xóa ảnh local cá nhân; photo processing/Glide không xuất gallery hoặc cache ảnh này. Phạm vi min3D: known id retry GET, unknown POST response có thể tạo thêm moment hoàn chỉnh; UI cảnh báo, idempotency ở5D chưa triển khai. Camera trước/flash/thiết bị xoay mọi hướng/API26 runtime/release HTTPS/VPS/load test chưa chạy; không suy rộng PASS.

File3D (không liệt kê lại các chỉnh sửa3B/3C đã tồn tại):

- Android mới: ui/camera/CameraFragment.java, PhotoProcessor.java; viewmodel/MomentViewModel.java; data/remote/MomentApi.java; data/repository/MomentRepository.java; res/layout/fragment_camera.xml; androidTest/CameraMomentAcceptanceTest.java.
- Android sửa có kiểm tra: MainActivity.java, data/remote/ApiClient.java, data/repository/AuthRepository.java, AndroidManifest.xml, res/layout/activity_main.xml, res/values/strings.xml.
- Server mới: moment/Moment.java, MomentRepository.java, MomentService.java, MomentController.java, MomentFailure.java; storage/PrivateImageStorage.java; test/MomentIntegrationTest.java.
- Server sửa: common/ApiExceptionHandler.java, application.yml (multipart5/6 MiB), test/LocalSmokeServer.java (storage test riêng, offline/online connector và kiểm tra count). Không migration/dependency mới.
- Hồ sơ mới: MOMENT_API.md, QA_SELF_MOMENT.md. Cập nhật PROJECT_STATUS, TASKS, PROJECT_DECISIONS, SESSION_HANDOFF; thêm ghi chú lịch sử cho QA_SESSION_ROUTING/UI_REDESIGN_REVIEW để không nhận Camera placeholder là trạng thái hiện tại.

HANDOFF/cleanup: cold signed-out **COLD1722ms** → Login/no serverImage; secure preferences không còn entry token, chỉ kiểm tra số lượng không in giá trị. Thu4 screenshot quyền/preview/offline/server và ảnh đủ sáng vào `.local/artifacts/3d/` ignored, không đưa ảnh chụp PC vào Git. Quyền CAMERA riêng Taktak đã kiểm tra denied/granted và cuối trả về granted; không thay bảo mật USB hoặc timeout/theme hệ thống. Gỡ đúng reverse do phiên tạo; runner exit0, cuối **4moment/4file/0recipient**, DB test riêng đã đóng. Ảnh fixture trong server/target được giữ local ignored làm artifact; không xóa dữ liệu cá nhân/reset DB.

Review cuối: git diff --check PASS,67 link local hợp lệ; quét20 file chọn lọc theo mẫu credential/conflict/trailing whitespace đạt (không phải audit toàn lịch sử). Hash đặc tả/Firebase giữ nguyên, index rỗng/HEAD8766f26; migration/README/dependency không đổi, cổng8080 đóng. Test APK/lint debug compile thêm11s PASS sau chỉnh helper min26/API37 trong androidTest; không đổi production sau các ca máy thậtAPI35, chưa runtime nhánhAPI37.

Muốn dùng app debug tiếp: backend smoke đã dừng, app vẫn có URL127.0.0.1:8080 từ build property; cần khởi động backend/dev config và USB reverse. Tài khoản QA thuộc DB tạm, không có mật khẩu cố định để bàn giao; DB/JWT/storage dev bền vững cần cấu hình riêng. Không còn blocker nghiệm thu3D. **Đề xuất duy nhất4A — search username và friend requests server; chờ người dùng duyệt.**

## Hoàn tất nghiệm thu 3C sau “thử lại lần nữa” — 16/10/2026

**Trạng thái mới nhất: 3C PASS; chờ duyệt 3D, không tự chuyển bước.** Các mục BLOCKED/thiếu ADB/cài bị chặn bên dưới là lịch sử. Đã khôi phục tài liệu/Git/source, hash đặc tả không đổi; giữ mọi thay đổi dirty/untracked 3B/3C/UI review. Test APK đã cài `-r` Success, không đổi thiết lập bảo mật hoặc xóa dữ liệu điện thoại.

Plan/implementation: suite cũ kẹt khi ActivityScenario mở Login (START result102); runner được chủ động ngắt, không gán thành crash app hoặc 25 test máy thật PASS. Thêm riêng `LauncherAuthAcceptanceTest.java`, mở exported launcher qua UiAutomation, fixture từ chối ghi đè phiên cá nhân và chỉ cleanup phiên do nó sở hữu. Production source không đổi trong lần thử này. Skill android_ui_verification hướng dẫn chờ state/screenshot, không tranh UiAutomation; skill auth hướng dẫn giữ credential trong bộ nhớ, kiểm tra persistence/revoke/fail-closed.

Verify mới: `assembleDebugAndroidTest lintDebug` BUILD SUCCESSFUL 32s, lint 0 errors/3 warnings cũ. Máy thật 23129RAA4G/Android15/API35/1080×2400: login launcher **1/1 PASS 7.467s**, restore/network retry/logout **1/1 PASS 15.505s**, AuthBackendTest **4/4 PASS 3.458s**. Host force-stop/launcher **COLD 1667ms** khôi phục Camera; sau logout **COLD 1818ms** về Login, không Camera. Xác minh secure preferences không còn entry phiên, không in credential. Xem 5 screenshot; ảnh có PiP ứng dụng khác nên chỉ giữ local ignored. Bằng chứng/giới hạn tại [QA_SESSION_ROUTING.md](QA_SESSION_ROUTING.md); không coi 20 unit/25 AVD lịch sử là vừa chạy, không tuyên bố chạy toàn bộ 27 test sau bổ sung.

File thay đổi lần này: test launcher mới và QA_SESSION_ROUTING, PROJECT_STATUS, TASKS, PROJECT_DECISIONS, SESSION_HANDOFF. Không sửa server/migration/Firebase/Gradle/README hoặc quyết định kỹ thuật. Artifact `.local/artifacts/3c-physical/`: launcher-login.txt, launcher-restore-logout.txt, backend-auth.txt, cold-start.xml, after-logout.xml và screenshots; instrumentation.txt là lần suite cũ bị ngắt, không phải test PASS.

Cleanup: logout fixture, giữ APK/data và cài đặt cá nhân; gỡ đúng reverse tcp:8080 do phiên tạo, smoke server exit0 và cổng8080 đóng, runner đóng cluster test riêng. Không reset DB cá nhân, commit/push/merge/deploy. App debug hiện dùng URL127.0.0.1:8080 từ property build; backend/reverse đã dừng nên muốn đăng nhập dev phải khởi động backend và reverse phù hợp. Tài khoản QA thuộc DB tạm, không phải tài khoản dev bền vững.

Review cuối: git diff --check PASS (chỉ cảnh báo LF/CRLF), 57 link local trong 5 hồ sơ hợp lệ; quét mẫu credential/conflict chọn lọc 6 file đạt, không phải audit toàn lịch sử. Hash Firebase JSON và đặc tả giữ nguyên; Git index rỗng, HEAD vẫn 8766f26; server/README/Gradle không có thay đổi. Artifact log/ảnh đã xác minh bị ignore.

Không còn blocker nghiệm thu 3C. API26, release HTTPS runtime, chờ JWT hết hạn thật, dialog Deny và toàn bộ suite 27 test chưa chạy; không suy rộng PASS. Camera vẫn placeholder; **đề xuất duy nhất: duyệt 3D — CameraX → upload ảnh chỉ mình tôi → tải lại bằng JWT**. Không triển khai trước xác nhận.

## Thử lại cài APK test 3C sau “tiếp tục” — 16/10/2026

Điện thoại vẫn ADB device. Install streaming và không streaming đều trả INSTALL_FAILED_USER_RESTRICTED, không có instrumentation mới đã chạy. Đọc dumpsys user thấy no_install_unknown_sources nhưng không tự suy ra phạm vi profile/căn nguyên; không thay restriction hoặc tắt xác minh bảo mật. Phiên này chỉ bổ sung QA_SESSION_ROUTING và bàn giao, giữ mọi source/thay đổi trước; không khởi động backend/reverse mới. 3C tiếp tục BLOCKED nghiệm thu máy thật. Người dùng cần kiểm tra quyền Install via USB của com.taktak.app.test; nếu đã cho phép mà vẫn bị từ chối, gửi ảnh mục quyền/thông báo cài đặt để chẩn đoán, không tiếp tục lặp install mù. Chưa commit/push/deploy hoặc chuyển bước.

## Tiếp tục nghiệm thu 3C trên điện thoại — 16/10/2026, khoảng 22:44–22:45

Yêu cầu mới nhất thay thế redesign: hoàn tất nghiệm thu 3C bằng USB. Đã đọc hồ sơ/source/skill kiểm tra UI và auth, đối chiếu Git và hash đặc tả không đổi so với bản đã đọc đầy đủ. Giữ mọi thay đổi dirty/untracked 3B/3C/UI review. Điện thoại 23129RAA4G/Android15/API35/1080×2400 kết nối và mở khóa, run-as xác nhận chưa có file phiên auth; không đọc credential hoặc clear dữ liệu cá nhân.

Build debug/test APK với property URL 127.0.0.1:8080 PASS 14 giây; backend/Postgres smoke tạm + USB reverse riêng. App install-r Success, không uninstall/clear data. APK test com.taktak.app.test bị điện thoại từ chối hai lần INSTALL_FAILED_USER_RESTRICTED: Install canceled by user; chưa chạy instrumentation, không coi là test fail hoặc PASS. Không tự đổi cài đặt bảo mật để vượt chặn. Cold launcher máy thật COLD/Status ok/1572ms → Login (UI dump/emailInput) PASS; screenshot dark mode đã xem. Login thật, persistence/retry/logout trên máy thật vẫn chưa nghiệm thu.

Chỉ cập nhật QA_SESSION_ROUTING, PROJECT_STATUS, TASKS và mục bàn giao này; không sửa Android/server/Gradle/theme/DB/Firebase. Đã gỡ đúng reverse do phiên tạo, smoke server exit0/thu hồi cluster test riêng; giữ app debug vừa cập nhật trên phone. Không commit/push/deploy hoặc chuyển 3D/redesign. **3C BLOCKED do quyền cài APK test**, không còn do thiếu USB. Hành động tối thiểu: người dùng cho phép cài APK kiểm thử trên điện thoại rồi nhắn tiếp tục; không cần duyệt lại triển khai 3C. Các trạng thái thiếu ADB/redesign/3C local bên dưới là lịch sử.

## Yêu cầu UI/UX mới nhất — đánh giá Camera, 16/10/2026

Người dùng yêu cầu `ui-ux-pro-max` và chỉ redesign Camera/tokens cần thiết; nếu Camera chưa hoạt động phải báo và đề xuất màn hiện có. Đã đọc skill và tài liệu/source, xác nhận Camera hiện chỉ placeholder 3C, chưa PreviewView/CameraFragment/ImageCapture hoặc CAMERA permission. **BLOCKED triển khai redesign Camera**, không tự xây camera hoặc đổi sang Auth khi chưa duyệt. Đề xuất duy nhất: **redesign Login hiện có trước**, chờ xác nhận. Không thay kết quả/nghiệm thu roadmap 3C hoặc tự mở 3D.

CLI design-system đã thử nhưng Python chưa có (Windows Store alias); không giả có output generator. Đọc CSV skill bằng PowerShell làm fallback, đối chiếu Android accessibility và đo palette contrast; không cài phần mềm. Đề xuất, evidence và giới hạn tại [UI_REDESIGN_REVIEW.md](UI_REDESIGN_REVIEW.md). Touch target theo Android 48dp thay vì sao chép px/web từ skill; không chuyển stack. Thiết bị ADB đã xuất hiện trở lại trong phiên kiểm tra chỉ đọc, nhưng auth máy thật chưa được thử; không cài APK/đụng dữ liệu điện thoại trong yêu cầu UI này.

Chỉ thêm UI_REDESIGN_REVIEW và mục bàn giao này; giữ source/UI/tokens và mọi thay đổi cũ. XML resource hiện tại 14/14 parse PASS; assembleDebug baseline BUILD SUCCESSFUL 11 giây/39 task UP-TO-DATE, không gọi đây là rebuild hoặc redesign PASS. Không có ảnh trước/sau hoặc test runtime mới. Không commit/push/deploy; dừng chờ xác nhận màn Login.

Ngày: 16/10/2026 (Asia/Saigon). Nhánh khi kiểm tra mới nhất: `main`. HEAD: `8766f26` — `chore(taktak): Sync current project changes`. Các HEAD và quyền thực hiện trong những phiên bên dưới là lịch sử.

**Trạng thái mới nhất:** người dùng duyệt riêng **3C — điều hướng phiên và logout UI**. **Triển khai/local PASS, BLOCKED nghiệm thu máy thật** vì điện thoại đã ngắt ADB; chưa DONE toàn bộ 3C. Build/lint debug/release, 20 unit + 25 emulator test và cold-process restore/logout PASS. Chi tiết tại [QA_SESSION_ROUTING.md](QA_SESSION_ROUTING.md). Không tự chuyển 3D hoặc commit/push/deploy. Những phần 3B trở xuống là lịch sử.

## Bàn giao mới nhất — 3C, 16/10/2026

UNDERSTAND: đọc đặc tả, quy tắc, quyết định, hồ sơ và source; HEAD vẫn 8766f26/main. Đầu phiên có 6 hồ sơ modified và QA_ANDROID_AUTH untracked từ 3B; giữ nguyên nội dung cũ khi bổ sung trạng thái mới. Dùng skill auth-implementation-patterns cho lifecycle phiên và android_ui_verification cho kiểm tra UI/IME/runtime.

PLAN/IMPLEMENT: MainActivity trở thành session gate/Camera destination placeholder; SessionViewModel worker + LiveData, check me/refresh trước Camera, retry/logout khi lỗi; AuthRepository thêm hasSession/logout. Login dọn task sau thành công; logout dọn task và báo rõ chỉ local khi server không xác nhận. AuthForm renderer giữ thông báo điều hướng bằng whitelist resource thay vì bị initial LiveData che. Không thêm CameraX/camera permission/FCM, bốn tab đầy đủ hoặc backend mới.

File code thay đổi: MainActivity, AuthRepository, AuthFormActivity, LoginActivity, AuthViewModel, activity_main.xml, strings.xml; thêm SessionViewModel. Test: sửa AuthUiTest/ExampleInstrumentedTest, thêm SessionRoutingTest (8 case). Docs 3C: QA_SESSION_ROUTING mới, PROJECT_STATUS/TASKS/PROJECT_DECISIONS/SESSION_HANDOFF/QA_ANDROID_AUTH cập nhật. AUTH_API và QA_AUTH_UI còn dirty từ 3B, không nhận là mới sửa trong 3C. README/server/migration/dependency/Firebase JSON không sửa.

VERIFY: build debug/test APK/release và lint debug/release PASS, 0 errors/3 warnings cũ. 20 unit test chạy thực (không failures/errors/skipped). Lần instrumentation đầu 21 test/4 failures: ba lỗi notice do LiveData và một IME chưa visible; sửa renderer, test chờ IME thực và giữ viewport assertion. Bản cuối **25/25 PASS, 122.828 giây**, 0 failed/ignored. Fixture cold start riêng **1/1 PASS**, sau đó ADB force-stop/cold launcher có Camera từ session disk, đã dump/xem ảnh; logout UI → Back → force-stop/cold launcher về Login, không Camera. Có test timeout socket thật, connection refusal/retry hồi phục, revoke/no loop, clear storage lỗi qua recreate rồi logout retry.

REVIEW: không mang token/password trong UI state/Intent/log; protected API vẫn kiểm tra ở server. Logout không giả clear thành công khi storage lỗi; offline không hứa server revoke, device unregister thuộc bước 6. Hai lần runner chết trước test được xác định từ ApplicationExitInfo của đúng PID 12849/19014: **LOW_MEMORY**, không Java crash; cold launch thường và suite lại đạt khi có foreground task. Giữ log FAIL/crash và thông tin môi trường, không suppress/nâng stack/xóa data. Screenshot/command và giới hạn tại QA_SESSION_ROUTING.

HANDOFF: phần code/local của 3C đạt; nghiệm thu auth Android thật còn **BLOCKED** vì ADB hiện không còn điện thoại, chỉ có AVD. Không lấy 4 bootstrap test máy thật cũ làm bằng chứng auth mới. Người dùng chỉ cần **cắm lại USB, mở khóa và cấp USB debugging nếu được hỏi**; tiếp tục xác minh trong 3C đã duyệt, không cần duyệt lại triển khai. Trước thao tác phải kiểm tra/bảo toàn phiên và dữ liệu cá nhân, không chạy suite fixture logout trên phone tùy tiện. Chưa triển khai 3D; sau 3C mới đề xuất/chờ duyệt lát cắt ảnh owner-only.

Kiểm tra cuối 3C: release manifest chặn cleartext, không quyền debug local-network/CAMERA, auth activities non-exported; BuildConfig chỉ URL HTTPS placeholder. Firebase JSON hash giữ nguyên, không diff server/README/Gradle/dependency, index rỗng và HEAD vẫn 8766f26. Quét 6 hồ sơ 3C theo mẫu credential/conflict và resolve 56 link local PASS; source auth/data/ViewModel/Main không có logging/mẫu credential đã chọn; không Kotlin source. `git diff --check` PASS, chỉ cảnh báo LF→CRLF; không coi quét chọn lọc là audit toàn lịch sử. Backend và AVD của phiên exit 0, không listener 8080/5554/5555; runner thu hồi cluster test riêng, không xóa dữ liệu người dùng. Sau cleanup ADB không có thiết bị; blocker nghiệm thu điện thoại giữ nguyên.

## Bàn giao mới nhất — hoàn tất 3B, 16/10/2026

UNDERSTAND: đọc đặc tả/quyết định/hồ sơ và source, kiểm tra Git. HEAD 8766f26 đã chứa code 3B và test nhưng hồ sơ ghi 3A; working tree sạch. Đây là trạng thái thực tế khác với phiên dang dở trước, không tự reset/checkout hoặc tạo lại code. Emulator/backend của phiên trước không còn chạy; đã khởi động lại tài nguyên test riêng. Skill auth-implementation-patterns dùng để rà token lifecycle, skill android_ui_verification để xác minh runtime/UI.

PLAN/IMPLEMENT: giữ source đang có, chạy build/unit/lint và emulator integration với backend/Postgres tạm. Thu ảnh, review security/scope, cập nhật QA_ANDROID_AUTH, PROJECT_STATUS, TASKS, PROJECT_DECISIONS, AUTH_API, QA_AUTH_UI và SESSION_HANDOFF. Không sửa README, server, migration, dependency hoặc JSON Firebase.

VERIFY: build debug/test APK/release và lint cả hai variant PASS. Chạy riêng unit --rerun-tasks: 20/20, 0 failures/errors/skipped. AVD Medium_Phone read-only/API 37/1080×2400 qua 10.0.2.2:8080: OK (17 tests), 52.376 giây, gồm 9 UI + 4 session/backend + 4 bootstrap. Đã xem 3 ảnh login/register thành công và login lỗi. Backend smoke health 200, DB test PostgreSQL thật; không chạy lại suite Maven 39 test. Lint 0 errors/3 warnings, deprecation giữ nguyên stack; chi tiết/giới hạn và cách chạy tại QA_ANDROID_AUTH.

REVIEW: tách auth/protected client, bearer đúng origin/path, không follow redirect, một refresh đồng bộ/một replay, generation guard, encrypted storage/backup exclusion và không plaintext fallback. Đã có test cho race 401, revoke/replay, 503/network/storage/ciphertext lỗi. Đọc lại store/client không phải bằng chứng đóng/mở app route; forcing invalid access không phải chờ JWT hết hạn thật. Auth UI mới chưa chạy trên điện thoại; điện thoại kết nối không bị cài/uninstall/xóa app data trong phiên này.

Môi trường: sandbox không nhìn thấy AVD và Maven Wrapper không chạy được; scoped execution ngoài sandbox với cùng công cụ local đã chạy được. ADB unauthorized do popup Allow USB debugging trên chính AVD, xử lý sau khi xem screenshot; không đổi settings điện thoại hoặc firewall. Chạy instrumentation bằng serial AVD cụ thể, không connectedDebugAndroidTest mặc định. Thử adb mở trực tiếp auth activity bị từ chối đúng vì non-exported; không nới manifest để chữa. Cleanup tài nguyên của phiên được xác minh ở cuối hồ sơ.

HANDOFF: **3B PASS local**, không có blocker cho phạm vi 3B. Đề xuất duy nhất **3C — routing launcher theo phiên, đích Camera và logout UI/xử lý lỗi**; cần người dùng xác nhận trước khi triển khai. 3D bắt buộc sau 3C; chưa camera thực tế/FCM/avatar/deploy. Không tự commit/push/merge/deploy. Các mục cũ bên dưới được giữ làm lịch sử, không thay quyền mới nhất.

Kiểm tra cuối phiên tiếp tục 3B: manifest/BuildConfig release, auth non-exported và 3 backup/transfer exclusion PASS; quét 7 hồ sơ không thấy mẫu key/JWT/private-key/conflict, 66 link local hợp lệ. Quét source data/ViewModel không thấy logging hoặc mẫu credential đã chọn; đây không phải audit bí mật toàn lịch sử Git. Không có Kotlin source, `git diff --check` PASS (chỉ cảnh báo LF→CRLF). Source Android/server/README không có diff so với HEAD, Firebase JSON giữ nguyên SHA-256 đã đối chiếu, staged diff rỗng, HEAD vẫn 8766f26. Chỉ 7 file docs thay đổi/thêm mới trong phiên tiếp tục.

Cleanup đã xác minh: backend smoke và emulator của phiên exit 0, không listener 8080/49347/57371/5554/5555; runner thu hồi hai cluster smoke tạm của phiên (lần đầu stdin đóng, lần sau PTY chạy test). Không xóa database người dùng/AVD/artifact. Điện thoại vẫn kết nối ADB, không bị cài/uninstall/clear data; không có ADB server riêng 5038 được tạo trong phiên tiếp tục này.

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
