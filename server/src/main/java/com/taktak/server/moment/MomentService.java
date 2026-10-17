package com.taktak.server.moment;

import com.taktak.server.storage.PrivateImageStorage;
import com.taktak.server.user.UserService;
import com.taktak.server.user.UserRepository;
import com.taktak.server.friend.FriendRequestRepository;
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
    private final UserRepository accounts;
    private final FriendRequestRepository relations;
    public MomentService(MomentRepository moments, PrivateImageStorage storage, UserService users, Clock clock,
            UserRepository accounts, FriendRequestRepository relations) {
        this.moments = moments; this.storage = storage; this.users = users; this.clock = clock;
        this.accounts = accounts; this.relations = relations;
    }

    @Transactional(rollbackFor = IOException.class)
    public UUID upload(UUID sender, MultipartFile image, String caption, List<String> recipients) throws IOException {
        return upload(sender, image, caption, recipients, null);
    }

    @Transactional(rollbackFor = IOException.class)
    public UUID upload(UUID sender, MultipartFile image, String caption, List<String> recipients, String idempotencyKeyRaw) throws IOException {
        users.getProfile(sender);
        UUID idempotencyKey = parseIdempotencyKey(idempotencyKeyRaw);
        var targets = parseRecipients(sender, recipients);
        if (caption.codePointCount(0, caption.length()) > 50 || caption.indexOf('\0') >= 0) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_CAPTION");
        }
        byte[] bytes = storage.sanitize(image);
        String payloadHash = idempotencyKey != null ? computePayloadHash(sender, caption, targets, bytes) : null;

        var participants = new java.util.ArrayList<>(targets); participants.add(sender);
        // Khóa tất cả người liên quan (bao gồm sender) theo thứ tự UUID PostgreSQL.
        if (accounts.lockAll(participants).size() != participants.size()) { throw unavailable(); }

        if (idempotencyKey != null) {
            var existing = moments.findBySenderIdAndIdempotencyKey(sender, idempotencyKey);
            if (existing.isPresent()) {
                var moment = existing.get();
                if (payloadHash.equals(moment.getPayloadHash())) {
                    return moment.getId();
                } else {
                    throw new MomentFailure(HttpStatus.CONFLICT, "IDEMPOTENCY_CONFLICT");
                }
            }
        }

        if (!targets.isEmpty()) {
            for (UUID target : targets) {
                if (relations.blocked(sender, target) || !relations.alreadyFriends(sender, target)) { throw unavailable(); }
            }
        }
        UUID id = UUID.randomUUID();
        String key = storage.write(id, bytes);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override public void afterCompletion(int status) {
                if (status != STATUS_COMMITTED) { storage.discard(key); }
            }
        });
        var created = clock.instant();
        moments.saveAndFlush(new Moment(id, sender, caption, key, bytes.length, created, idempotencyKey, payloadHash));
        for (UUID target : targets) { moments.addRecipient(id, target, created); }
        return id;
    }

    @Transactional(readOnly = true)
    public byte[] image(UUID viewer, UUID id) throws IOException {
        users.getProfile(viewer);
        Moment moment = moments.findById(id).filter(value -> value.getDeletedAt() == null
                && (value.getSenderId().equals(viewer) || moments.isRecipient(id, viewer)))
                .orElseThrow(() -> new MomentFailure(HttpStatus.NOT_FOUND, "MOMENT_NOT_FOUND"));
        return storage.read(moment.getFilePath());
    }

    private List<UUID> parseRecipients(UUID sender, List<String> raw) {
        if (raw == null || raw.isEmpty() || (raw.size() == 1 && "[]".equals(raw.get(0)))) { return List.of(); }
        if (raw.size() > 100) { throw new MomentFailure(HttpStatus.BAD_REQUEST, "TOO_MANY_RECIPIENTS"); }
        var ids = new java.util.LinkedHashSet<UUID>();
        for (String value : raw) {
            UUID id;
            try {
                id = UUID.fromString(value);
                if (!id.toString().equalsIgnoreCase(value)) { throw new IllegalArgumentException(); }
            } catch (IllegalArgumentException failure) { throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_RECIPIENTS"); }
            if (id.equals(sender) || !ids.add(id)) { throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_RECIPIENTS"); }
        }
        return List.copyOf(ids);
    }
    private MomentFailure unavailable() { return new MomentFailure(HttpStatus.BAD_REQUEST, "RECIPIENT_UNAVAILABLE"); }

    private UUID parseIdempotencyKey(String raw) {
        if (raw == null || raw.isBlank()) { return null; }
        String trimmed = raw.trim();
        try {
            UUID id = UUID.fromString(trimmed);
            if (!id.toString().equalsIgnoreCase(trimmed)) { throw new IllegalArgumentException(); }
            return id;
        } catch (IllegalArgumentException e) {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "INVALID_IDEMPOTENCY_KEY");
        }
    }

    public static String computePayloadHash(UUID sender, String caption, List<UUID> targets, byte[] bytes) {
        try {
            var md = java.security.MessageDigest.getInstance("SHA-256");
            md.update(sender.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update((byte) '\n');
            md.update(caption.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            md.update((byte) '\n');
            var sorted = new java.util.ArrayList<>(targets);
            sorted.sort(java.util.Comparator.comparing(UUID::toString));
            for (UUID t : sorted) {
                md.update(t.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                md.update((byte) ',');
            }
            md.update((byte) '\n');
            md.update(bytes);
            return java.util.HexFormat.of().formatHex(md.digest());
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    @Transactional(readOnly = true)
    public MomentDtos.MomentsPage listMoments(UUID viewerId, String type, int page) {
        users.getProfile(viewerId);
        if (page < 0 || page > 10_000) { throw new MomentFailure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR"); }
        List<Moment> found;
        if ("received".equals(type)) {
            found = moments.receivedMoments(viewerId, page * 20);
        } else if ("sent".equals(type)) {
            found = moments.sentMoments(viewerId, page * 20);
        } else {
            throw new MomentFailure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        var senders = accounts.findAllById(found.stream().map(Moment::getSenderId).toList());
        var items = found.stream().limit(20).map(moment -> {
            var sender = senders.stream().filter(u -> u.getId().equals(moment.getSenderId())).findFirst().orElseThrow();
            return new MomentDtos.MomentItem(moment.getId(), MomentDtos.PublicUser.from(sender), moment.getCaption(), moment.getCreatedAt());
        }).toList();
        return new MomentDtos.MomentsPage(items, page, found.size() > 20);
    }
}
