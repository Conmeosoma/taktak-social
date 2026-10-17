package com.taktak.server.moment;

import com.taktak.server.storage.PrivateImageStorage;
import com.taktak.server.user.UserService;
import java.io.IOException;
import java.time.Clock;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

@Service
public class MomentService {
    private final MomentRepository moments;
    private final PrivateImageStorage storage;
    private final UserService users;
    private final Clock clock;
    public MomentService(MomentRepository moments, PrivateImageStorage storage, UserService users, Clock clock) {
        this.moments = moments; this.storage = storage; this.users = users; this.clock = clock;
    }

    @Transactional(rollbackFor = IOException.class)
    public UUID upload(UUID sender, MultipartFile image, String caption, List<String> recipients) throws IOException {
        users.getProfile(sender);
        // 3D chỉ nhận rỗng: bỏ field hoặc một field literal []. Không âm thầm bỏ người nhận.
        if (recipients != null && !recipients.isEmpty() && !(recipients.size() == 1 && "[]".equals(recipients.get(0)))) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "RECIPIENTS_NOT_SUPPORTED");
        }
        if (caption.codePointCount(0, caption.length()) > 50 || caption.indexOf('\0') >= 0) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_CAPTION");
        }
        byte[] bytes = storage.sanitize(image);
        UUID id = UUID.randomUUID();
        String key = storage.write(id, bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) { storage.discard(key); }
            }
        });
        moments.saveAndFlush(new Moment(id, sender, caption, key, bytes.length, clock.instant()));
        return id;
    }

    @Transactional(readOnly = true)
    public byte[] image(UUID viewer, UUID id) throws IOException {
        users.getProfile(viewer);
        Moment moment = moments.findById(id).filter(value -> value.getDeletedAt() == null
                && value.getSenderId().equals(viewer)).orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "MOMENT_NOT_FOUND"));
        return storage.read(moment.getFilePath());
    }
}
