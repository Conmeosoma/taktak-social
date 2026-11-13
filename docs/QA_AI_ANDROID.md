# AI Android — 13/11/2026

Phạm vi được yêu cầu: tích hợp frontend chatbot theo Figma vào Taktak hiện có.
Frame được gửi 167:4481 là glyph thông tin; đã đọc ancestors và các frame
167:3843 (trống), 167:3798 (hội thoại), 167:3888 (mới), 167:3933/4935 (chat sáng/tối).
Đã đọc skill figma-design-to-code, figma-use và android_ui_verification.

## Triển khai

- Nút mở AI trong Tin nhắn, AiFragment con giữ navigation năm tab hiện có.
- AiModels/AiApi/AiRepository, nối qua AuthRepository và ApiClient dùng JWT/refresh hiện có.
- Timeout riêng AI 60/70 giây, không đổi timeout chat/media. Không key trên Android.
- AiViewModel/LiveData: danh sách, lịch sử phân trang, chat mới, gửi/thử lại,
  xóa; consent Google trước gửi, pending request bất biến giữ conversationId null
  và clientRequestId khi retry; không tự lặp 429/503. Nội dung chỉ render text.
- Payload giới hạn UTF-16 4000 khớp backend; chặn kết quả từ phiên cũ,
  xóa snapshot trong RAM khi session-cleared. Không lưu prompt trong saved state/disk.
- Logo tải local đúng asset Figma; màu sáng/tối riêng AI, font Inter sẵn có.
- Tiếng Việt + English; hi/ko/ja/es/zh dùng English fallback công khai, chưa dịch.

## Bằng chứng hiện có

- `:app:assembleDebug :app:assembleDebugAndroidTest :app:testDebugUnitTest :app:lintDebug`
  BUILD SUCCESSFUL 44s; 143 unit tests, 0 failure/error; lint 0 errors/94 warnings.
  Log `.local/artifacts/ai-usb-build.log`. Không tắt lint để bỏ qua thiếu resource.
- JDK discovery từng chọn JRE của extension Java thiếu jlink. Build thành công
  khi dùng JDK Android Studio, tắt auto-detect và no-daemon chỉ trong lệnh;
  không sửa cấu hình IDE của người dùng.
- USB Xiaomi 23129RAA4G Android15, 1080x2400; cài cả hai APK thành công.
- Hai lượt UI ban đầu FAIL khởi chạy MainActivity bằng startActivitySync timeout45s;
  foreground thực tế là launcher. Harness đổi sang shell launch để kiểm tra lại,
  chưa được coi là USB PASS. Xem log `ai-usb-test*.log`.

## Giới hạn phải nghiệm thu

Cập nhật cuối: harness shell launch + isDialog đã chạy USB **PASS1/1**,5,296s,
log `.local/artifacts/ai-usb-dialog-test.log`. Test kiểm tra Cancel không gửi,
consent đồng ý,503 rồi retry cùng key/null conversationId,2tin hiển thị,
DELETE204 và session-cleared xóa snapshot. Ảnh `ai-usb-empty.png`,
`ai-usb-retry.png`, `ai-usb-chat.png` trong `.local/artifacts/` đã lấy từ USB;
đã review ảnh trống/chat, logo không rỗng203394bytes. Ba lượt FAIL trước giữ
nguyên log để truy vết; PASS sau sửa không biến các lượt trước thành PASS.
Fixture RAM bảo toàn AuthGraph gốc trong finally, không đổi session store cá nhân.
Phần UI chức năng đã triển khai; toàn bộ visual/edgecases dưới đây còn IN PROGRESS.

Chưa tuyên bố pixel-exact: header thêm Back để quay về Tin nhắn, navigation dùng
component hiện hữu; card hội thoại dùng logo thay glyph của frame. Cần đối chiếu
screenshots, dark mode, font lớn, IME, recreate, pagination và disabled/rate-limit.
Các resource lỗi API hiện có, nhưng chưa có bằng chứng USB cho mọi nhánh.
Key pending chỉ giữ qua rotation bằng ViewModel; process death mất pending RAM:
cần tải lịch sử trước gửi lại để tránh tạo thêm lượt chat. Không tuyên bố exactly-once.

Backend AI và migration V7 chưa deploy VPS trong nhiệm vụ này. QA fixture không
phải bằng chứng production Gemini chạy từ app. Không tự gửi dữ liệu riêng tư,
commit/push/deploy hoặc sửa tài khoản cá nhân.
