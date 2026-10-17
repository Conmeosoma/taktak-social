package com.taktak.server.storage;

import com.taktak.server.moment.MomentFailure;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class PrivateImageStorage {
    public static final int MAX_BYTES = 5 * 1024 * 1024;
    private final Path root;

    public PrivateImageStorage(@Value("${taktak.storage.root}") String root) {
        this.root = Path.of(root).toAbsolutePath().normalize();
    }

    public byte[] sanitize(MultipartFile file) throws IOException {
        if (file.isEmpty()) { throw new MomentFailure(HttpStatus.BAD_REQUEST, "IMAGE_REQUIRED"); }
        if (file.getSize() > MAX_BYTES) { throw new MomentFailure(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE"); }
        if (!"image/jpeg".equalsIgnoreCase(file.getContentType())) { throw unsupported(); }
        byte[] input;
        try (var stream = file.getInputStream()) { input = stream.readNBytes(MAX_BYTES + 1); }
        if (input.length > MAX_BYTES) { throw new MomentFailure(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE"); }
        if (input.length < 3 || (input[0] & 255) != 255 || (input[1] & 255) != 216 || (input[2] & 255) != 255) {
            throw unsupported();
        }
        // Decode có giới hạn trước khi cấp phát bitmap; re-encode loại EXIF/GPS và dữ liệu đính kèm.
        try (var source = new javax.imageio.stream.MemoryCacheImageInputStream(new ByteArrayInputStream(input))) {
            var readers = ImageIO.getImageReaders(source);
            if (!readers.hasNext()) { throw unsupported(); }
            var reader = readers.next();
            try {
                reader.setInput(source, true, true);
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (!"JPEG".equalsIgnoreCase(reader.getFormatName()) || width < 1 || height < 1
                        || width > 4096 || height > 4096 || (long) width * height > 16_000_000) { throw unsupported(); }
                BufferedImage image = reader.read(0);
                try (var output = new ByteArrayOutputStream()) {
                    if (!ImageIO.write(image, "jpeg", output)) { throw unsupported(); }
                    byte[] result = output.toByteArray();
                    if (result.length > MAX_BYTES) { throw new MomentFailure(HttpStatus.PAYLOAD_TOO_LARGE, "IMAGE_TOO_LARGE"); }
                    return result;
                } finally { image.flush(); }
            } finally { reader.dispose(); }
        } catch (javax.imageio.IIOException | IllegalArgumentException malformed) { throw unsupported(); }
    }

    public String write(UUID id, byte[] bytes) throws IOException {
        Files.createDirectories(root);
        boolean posix = Files.getFileStore(root).supportsFileAttributeView("posix");
        if (posix) { Files.setPosixFilePermissions(root, PosixFilePermissions.fromString("rwx------")); }
        Path pending = posix ? Files.createTempFile(root, "pending-", ".tmp",
                PosixFilePermissions.asFileAttribute(PosixFilePermissions.fromString("rw-------")))
                : Files.createTempFile(root, "pending-", ".tmp");
        String name = id + ".jpg";
        try {
            Files.write(pending, bytes);
            Files.move(pending, resolve(name));
            return name;
        } finally { Files.deleteIfExists(pending); }
    }

    public byte[] read(String name) throws IOException {
        Path file = resolve(name);
        if (!Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            throw new MomentFailure(HttpStatus.NOT_FOUND, "MOMENT_NOT_FOUND");
        }
        try (var input = Files.newInputStream(file)) {
            byte[] bytes = input.readNBytes(MAX_BYTES + 1);
            if (bytes.length > MAX_BYTES) { throw new IOException("Private image exceeds limit."); }
            return bytes;
        }
    }

    public void discard(String name) {
        try { Files.deleteIfExists(resolve(name)); }
        catch (IOException failure) {
            org.slf4j.LoggerFactory.getLogger(PrivateImageStorage.class).error("Private image rollback cleanup failed.");
        }
    }

    private Path resolve(String name) {
        if (!name.matches("[a-f0-9]{8}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{4}-[a-f0-9]{12}\\.jpg")) {
            throw new IllegalArgumentException("Invalid private storage key.");
        }
        return root.resolve(name);
    }
    private MomentFailure unsupported() { return new MomentFailure(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "INVALID_IMAGE"); }
}
