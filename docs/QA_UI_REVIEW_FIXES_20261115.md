# Sửa các phát hiện UI/UX — 15/11/2026

Trạng thái: **IN PROGRESS**, chưa nghiệm thu toàn bộ. Phạm vi người dùng duyệt: “fix all”.
Skill đã dùng: `android_ui_verification`. Không commit, push, deploy hoặc thay backend.

## Đã triển khai

- Preview đọc và kiểm tra Poll; gửi bạn bè qua Outbox, phân biệt xếp hàng với gửi thành công. Luồng riêng tư vẫn upload và tải ảnh bằng JWT.
- Payload và thumbnail Live được sao chép vào vùng lưu riêng của Outbox; giữ idempotency key khi thử lại, checkpoint ảnh trước Poll, dọn cả thumbnail khi hoàn tất. DB insert lỗi không được báo xếp hàng thành công.
- Danh tính đã xác minh được cache theo generation để xếp hàng không cần gọi profile qua mạng. Đổi phiên vô hiệu hóa cache này.
- Người nhận đặt trước công cụ sửa ảnh; số người chọn nằm cạnh CTA. Sửa tương phản chữ người nhận theo theme.
- Snapshot lệnh vẽ trên UI; giải mã, composite, nén JPEG và ghi AtomicFile chạy ở worker. Lỗi ghi chặn gửi; không áp dụng lại overlay đã ghi.
- Live có nút xem clip; ẩn công cụ sửa JPEG. Dừng playback khi rời màn.
- Camera dùng ba nút biểu tượng 48dp với content description, tooltip và trạng thái trợ năng. Placeholder và microphone co giãn khi tăng font.
- Palette màu và sticker có vùng chạm tối thiểu 48dp, nhãn và trạng thái chọn.
- Xác minh email theo thứ tự yêu cầu mã → nhập mã/xác nhận. Giữ nguyên hợp đồng mã xác minh của backend.
- Friends giảm độ nổi của QR/refresh; Profile dùng hàng cài đặt nhẹ hơn. Năm tab luôn có nhãn.
- AI dùng màu semantic của theme hiện tại; preview theme có chữ, trường và nút mẫu.
- Ẩn Recap dữ liệu mẫu, bỏ thông báo reaction widget giả và nhãn hết hạn 24 giờ không có dữ liệu server. Widget vẫn dùng RemoteViews.
- Bổ sung bản dịch cho bảy locale hiện có.

## Kiểm tra thực tế

Log ở `.local/artifacts/`:

- `ui-review-final-build.log`: build debug/test APK, unit test và lint. JVM **144/144 PASS**; lint **0 lỗi, 126 cảnh báo**.
- `ui-review-forms-lists-qa.log`: **11/11 PASS**, Auth/Recovery, Friends/History, Profile; theme và cỡ chữ, hộp thoại.
- `ui-review-final-device.log`: suite 22 ca **FAIL**, có lỗi ma trận font, lỗi cleanup fixture và timeout Camera. Không coi suite này PASS. Các ca Preview gửi/Poll/retry, AI, widget, chooser và phần Outbox khác có kết quả đạt trong log.
- `ui-review-matrix-final.log`: sau sửa nút mic, chạy riêng ma trận bố cục **1/1 PASS**.
- `ui-review-live-isolated.log`: chạy riêng Camera/Live vẫn **FAIL**, timeout chờ Preview. Chưa xác định dứt điểm nguyên nhân; không nới assertion.
- `git diff --check`: đạt tại lượt review.

Ảnh đã xem: `.local/artifacts/ui-review-20261115/camera.png`, `preview.png`.
Ảnh Preview cho thấy lỗi tương phản hàng người nhận; đã sửa XML sau khi xem, cần chụp lại bản cuối để nghiệm thu visual.

## Còn phải hoàn tất

1. Điều tra timeout Camera/Live trên AVD, kiểm tra lifecycle, nguồn camera/mic và timing của harness; chạy lại ca có/không mic, thả dừng và playback.
2. Chụp lại Preview cuối, xác minh tên người nhận sáng/tối; chạy lại suite tổng hợp sau sửa để loại trừ ảnh hưởng giữa các fixture.
3. Chưa QA máy thật, TalkBack, launcher widget thật hoặc gửi E2E lên VPS trong lượt này. Không coi fixture trong RAM là nghiệm thu production.
4. Lint còn cảnh báo (gồm tài nguyên không còn được dùng); không có cam kết đã sạch toàn bộ cảnh báo.

Các file báo cáo DOCX/scripts untracked và thay đổi server xuất hiện trong lúc làm được giữ nguyên, không thuộc gói sửa này.
