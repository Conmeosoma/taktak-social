# Live Moment — xác minh video và metadata

Cập nhật thao tác theo yêu cầu14/11/2026: không có mode Live riêng. Chạm
shutter chụp ảnh, giữ để quay, thả dừng sớm, tối đa3giây. Mic vẫn tùy chọn.
Video ngắn hơn3giây được chấp nhận nếu có dữ liệu video hợp lệ.
Xem [QA thao tác](QA_SHUTTER_HOLD_20261114.md).

Phạm vi sửa ngày 14/11/2026: kiểm tra video server, quyền microphone Android,
metadata của luồng gửi trực tiếp/Outbox và lỗi lint. Không triển khai VPS.

## Hợp đồng

- Clip MP4 tối đa 10 MiB, đúng một track H.264, tối đa một track AAC.
- Có hình thực sự giải mã được; không nhận file chỉ có chữ ký `ftyp`, container
  thiếu track, dữ liệu frame hỏng hoặc clip quá dài.
- Mục tiêu quay 3 giây; chấp nhận tối đa 3.300 ms để dung sai việc dừng recorder
  và frame/audio cuối. Không cắt ngắn clip quá dài để âm thầm chấp nhận.
- Video tối đa 1.920 pixel mỗi chiều và 2.073.600 pixel mỗi frame.
- Server đọc duration/audio từ media, bỏ qua các giá trị client tự khai báo.
  Android cũng đọc metadata thật bằng MediaMetadataRetriever trước gửi.
- Microphone chỉ bật khi người dùng chọn và đã cấp RECORD_AUDIO; không bắt buộc
  thiết bị có microphone. Quay không tiếng vẫn hoạt động.

## Chuẩn bị server

Cần FFmpeg và ffprobe trên PATH, hoặc đặt đường dẫn executable qua
`TAKTAK_FFMPEG_PATH` và `TAKTAK_FFPROBE_PATH`. Không đặt các biến này trên client.
Ubuntu có thể cài gói `ffmpeg` qua trình quản lý gói khi triển khai được duyệt.
Dùng bản có các cập nhật bảo mật của hệ điều hành. QA Windows đã chạy với
FFmpeg/ffprobe 9.0.2 trong `.tools/ffmpeg/`, không đưa binary vào Git.

Thiếu executable/timeout/hết slot xử lý trả 503 VIDEO_VALIDATION_UNAVAILABLE;
media không hợp lệ trả 415 INVALID_VIDEO. Không fallback sang kiểm tra chữ ký.
Không cần migration mới cho bản sửa validation này; V8/V9 của tính năng mở rộng
vẫn cần được xét riêng khi triển khai, không tự migrate production.

## Giới hạn xử lý và riêng tư

Tối đa hai request xác minh đồng thời trong mỗi JVM, mỗi process giới hạn 15 giây,
một decode thread; protocol chỉ file/pipe. File tạm nằm trong thư mục riêng, đặt
quyền 0700 nếu hỗ trợ POSIX, dọn trong finally. Không ghi stderr media vào log.
Full decode kiểm tra frame thay vì chỉ tin metadata container. Đăng ký rollback
cleanup JPEG trước khi ghi video để lỗi ghi video không để lại thumbnail rác.

Process timeout và giới hạn allocation/thread không thay thế memory/CPU sandbox
của hệ điều hành. Khi triển khai cần kiểm tra headroom VPS và cấu hình systemd
phù hợp; chưa load-test chi phí decode trong lượt này. Bản sửa giữ nguyên bytes
video được nhận, không transcode hoặc cam kết loại toàn bộ metadata video.

## Kiểm thử

Fixture `server/src/test/resources/live-video/` chỉ chứa clip màu xanh 160×160,
có hoặc không có track âm thanh im lặng, không có dữ liệu cá nhân.
`LiveVideoStorageTest` kiểm tra clip hợp lệ, audio, quá dài, cắt cụt, MP4 giả và
frame hỏng trong container còn metadata. `AdvancedFeaturesQaTest` kiểm tra thêm
upload lưu metadata thực bất chấp client gửi duration/audio sai.

Sau build/cài debug APK và test APK, chạy USB khi đã được phép chụp/quay cache:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File scripts/qa/advanced4-usb.ps1 -Serial <adb-serial>
```

Runner cấp CAMERA/RECORD_AUDIO tạm nếu thiếu, chạy tám ca, khôi phục quyền sau
khi instrumentation kết thúc. Không thu hồi quyền giữa suite vì Android sẽ kill
process. Test dùng session/API fixture trong RAM, không đổi tài khoản cá nhân,
không gửi ảnh/video ra Internet hoặc lưu gallery. Ảnh QA chỉ ở `.local/artifacts`.

Kết quả và giới hạn nghiệm thu đầy đủ tại [báo cáo QA](QA_ADVANCED4_20261114.md).
