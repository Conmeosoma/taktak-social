package com.taktak.server.friend;

import com.taktak.server.auth.AuthFailure;
import com.taktak.server.friend.FriendDtos.*;
import com.taktak.server.user.User;
import com.taktak.server.user.UserRepository;
import java.time.Clock;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class FriendService {
    private final UserRepository users;
    private final FriendRequestRepository requests;
    private final SearchRateLimiter limiter;
    private final Clock clock;
    public FriendService(UserRepository users, FriendRequestRepository requests, SearchRateLimiter limiter, Clock clock) {
        this.users = users; this.requests = requests; this.limiter = limiter; this.clock = clock;
    }

    @Transactional(readOnly = true)
    public List<PublicUser> search(UUID actor, String query, String ip) {
        currentUser(actor);
        limiter.check(actor, ip);
        String normalized = query.strip().toLowerCase(Locale.ROOT);
        if (query.length() > 255 || !normalized.matches("[a-z0-9_]{1,255}")) {
            throw new FriendFailure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        }
        return users.searchVisible(actor, normalized.replace("_", "!_") + "%").stream().map(PublicUser::from).toList();
    }

    @Transactional
    public RequestResponse send(UUID actor, UUID receiver) {
        User sender = currentUser(actor);
        lockPair(actor, receiver, "FRIEND_REQUEST_SELF");
        if (requests.blocked(actor, receiver)) {
            throw new FriendFailure(HttpStatus.NOT_FOUND, "FRIEND_TARGET_UNAVAILABLE");
        }
        if (requests.alreadyFriends(actor, receiver) || requests.pending(actor, receiver)) {
            throw new FriendFailure(HttpStatus.CONFLICT, "FRIEND_REQUEST_CONFLICT");
        }
        try {
            return RequestResponse.from(requests.saveAndFlush(new FriendRequest(actor, receiver, clock.instant())), sender);
        } catch (DataIntegrityViolationException failure) {
            for (Throwable cause = failure; cause != null; cause = cause.getCause()) {
                if (cause instanceof java.sql.SQLException sql && "23505".equals(sql.getSQLState())) {
                    throw new FriendFailure(HttpStatus.CONFLICT, "FRIEND_REQUEST_CONFLICT");
                }
            }
            throw failure;
        }
    }

    @Transactional(readOnly = true)
    public RequestsPage incoming(UUID actor, int page) {
        currentUser(actor);
        if (page < 0 || page > 10_000) { throw new FriendFailure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR"); }
        var found = requests.incoming(actor, page * 20);
        var visible = found.stream().limit(20).toList();
        var senders = users.findAllById(visible.stream().map(FriendRequest::getSenderId).toList()).stream()
                .collect(Collectors.toMap(User::getId, user -> user));
        return new RequestsPage(visible.stream().map(request -> RequestResponse.from(request,
                senders.get(request.getSenderId()))).toList(), page, found.size() > 20);
    }

    private User currentUser(UUID actor) { return users.findById(actor).orElseThrow(AuthFailure::new); }

    @Transactional
    public RequestResponse respond(UUID actor, UUID id, String action) {
        currentUser(actor);
        String desired = switch (action) {
            case "accept" -> "accepted";
            case "reject" -> "rejected";
            default -> throw new FriendFailure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
        };
        var participants = requests.participants(id).orElseThrow(this::requestNotFound);
        if (!actor.equals(participants.getReceiverId())) { throw requestNotFound(); }
        var pair = lockPair(actor, participants.getSenderId(), "FRIEND_REQUEST_SELF");
        // Đọc/khóa request SAU khóa users; không dùng status từ snapshot trước khi chờ.
        var request = requests.forResponse(id).orElseThrow(this::requestNotFound);
        if (!actor.equals(request.getReceiverId())) { throw requestNotFound(); }
        if (requests.blocked(actor, request.getSenderId())) {
            throw new FriendFailure(HttpStatus.NOT_FOUND, "FRIEND_TARGET_UNAVAILABLE");
        }
        if (!"pending".equals(request.getStatus()) && !desired.equals(request.getStatus())) {
            throw new FriendFailure(HttpStatus.CONFLICT, "FRIEND_REQUEST_ALREADY_RESPONDED");
        }
        if ("pending".equals(request.getStatus())) {
            var now = clock.instant();
            request.respond(desired, now);
            requests.saveAndFlush(request);
            if ("accepted".equals(desired)) { requests.addFriendship(request.getSenderId(), actor, now); }
        }
        // Retry một quyết định đã hoàn tất không được tạo lại friendship sau unfriend.
        User sender = pair.stream().filter(user -> user.getId().equals(request.getSenderId())).findFirst().orElseThrow();
        return RequestResponse.from(request, sender);
    }

    @Transactional(readOnly = true)
    public FriendsPage friends(UUID actor, int page) {
        currentUser(actor);
        if (page < 0 || page > 10_000) { throw new FriendFailure(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR"); }
        var found = requests.friends(actor, page * 20);
        return new FriendsPage(found.stream().limit(20).map(PublicUser::from).toList(), page, found.size() > 20);
    }

    @Transactional
    public void unfriend(UUID actor, UUID target) {
        currentUser(actor);
        lockPair(actor, target, "FRIEND_SELF");
        requests.removeFriendship(actor, target);
    }

    @Transactional
    public void block(UUID actor, UUID target) {
        currentUser(actor);
        lockPair(actor, target, "BLOCK_SELF");
        var now = clock.instant();
        requests.addBlock(actor, target, now);
        requests.removeFriendship(actor, target);
        requests.rejectPendingPair(actor, target, now);
    }

    @Transactional
    public void unblock(UUID actor, UUID target) {
        currentUser(actor);
        lockPair(actor, target, "BLOCK_SELF");
        requests.removeOwnBlock(actor, target);
    }

    private List<User> lockPair(UUID actor, UUID target, String selfCode) {
        if (actor.equals(target)) { throw new FriendFailure(HttpStatus.BAD_REQUEST, selfCode); }
        // Mọi mutation quan hệ cùng khóa theo thứ tự UUID PostgreSQL, không UUID.compareTo Java.
        var pair = users.lockPair(actor, target);
        if (pair.stream().noneMatch(user -> user.getId().equals(actor))) { throw new AuthFailure(); }
        if (pair.size() != 2) { throw new FriendFailure(HttpStatus.NOT_FOUND, "FRIEND_TARGET_UNAVAILABLE"); }
        return pair;
    }

    private FriendFailure requestNotFound() { return new FriendFailure(HttpStatus.NOT_FOUND, "FRIEND_REQUEST_NOT_FOUND"); }
}
