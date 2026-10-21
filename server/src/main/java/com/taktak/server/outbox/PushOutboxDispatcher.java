package com.taktak.server.outbox;

import com.taktak.server.device.PushNotificationSender;
import com.taktak.server.friend.FriendRequestRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** At-least-once: crash sau FCM acceptance có thể gửi trùng; client phải đồng bộ idempotent. */
@Service
public class PushOutboxDispatcher {
    private final JdbcTemplate jdbc;
    private final PushNotificationSender sender;
    private final FriendRequestRepository friends;
    private final TransactionTemplate transactions;
    private final Clock clock;
    private final int maxAttempts;
    record Event(UUID id, String type, UUID resource, UUID conversation, UUID actor, UUID recipient, int attempts, UUID lease) {}

    public PushOutboxDispatcher(JdbcTemplate jdbc, PushNotificationSender sender, FriendRequestRepository friends,
            PlatformTransactionManager manager, Clock clock, @Value("${taktak.outbox.max-attempts:8}") int maxAttempts) {
        this.jdbc=jdbc; this.sender=sender; this.friends=friends; this.transactions=new TransactionTemplate(manager);
        this.clock=clock; this.maxAttempts=Math.max(1,Math.min(30,maxAttempts));
    }

    public int dispatchBatch() {
        int processed=0;
        for(int i=0;i<25;i++) {
            Event event=claim();
            if(event==null) break;
            try { dispatch(event); }
            catch(RuntimeException failure) { finish(event,"PENDING","DISPATCH_FAILURE"); }
            processed++;
        }
        return processed;
    }

    private Event claim() {
        return transactions.execute(status -> {
            var now=Timestamp.from(clock.instant());
            var rows=jdbc.query("""
                    SELECT * FROM push_outbox WHERE
                    (status='PENDING' AND available_at<=?) OR (status='PROCESSING' AND lease_until<=?)
                    ORDER BY available_at,created_at,id LIMIT 1 FOR UPDATE SKIP LOCKED
                    """,(rs,n)->new Event(rs.getObject("id",UUID.class),rs.getString("event_type"),
                    rs.getObject("resource_id",UUID.class),rs.getObject("conversation_id",UUID.class),
                    rs.getObject("sender_id",UUID.class),rs.getObject("recipient_id",UUID.class),rs.getInt("attempts"),UUID.randomUUID()),now,now);
            if(rows.isEmpty()) return null;
            var event=rows.get(0);
            jdbc.update("UPDATE push_outbox SET status='PROCESSING',lease_id=?,lease_until=? WHERE id=?",
                    event.lease(),Timestamp.from(clock.instant().plusSeconds(120)),event.id());
            return event;
        });
    }

    private boolean eligible(Event event) {
        if("chat_message".equals(event.type())) {
            if(friends.blocked(event.actor(),event.recipient()) || !friends.alreadyFriends(event.actor(),event.recipient())) return false;
            return Boolean.TRUE.equals(jdbc.queryForObject("""
                    SELECT EXISTS(SELECT 1 FROM messages WHERE id=? AND conversation_id=? AND sender_id=? AND recipient_id=?)
                    """,Boolean.class,event.resource(),event.conversation(),event.actor(),event.recipient()));
        }
        boolean deleted="moment_deleted".equals(event.type());
        if(!deleted && (friends.blocked(event.actor(),event.recipient()) || !friends.alreadyFriends(event.actor(),event.recipient()))) return false;
        // Deleted signal is an invalidation, still needed after unfriend/block; contains no content.
        return Boolean.TRUE.equals(jdbc.queryForObject("""
                SELECT EXISTS(SELECT 1 FROM moments m JOIN moment_recipients r ON r.moment_id=m.id
                WHERE m.id=? AND m.sender_id=? AND r.user_id=? AND (m.deleted_at IS NOT NULL)=?)
                """,Boolean.class,event.resource(),event.actor(),event.recipient(),deleted));
    }

    private void dispatch(Event event) {
        if(!eligible(event)) { finish(event,"CANCELLED",null);return; }
        var tokens=jdbc.queryForList("SELECT fcm_token FROM device_tokens WHERE user_id=?",String.class,event.recipient());
        if(tokens.isEmpty()) { finish(event,"CANCELLED","NO_REGISTRATION"); return; }
        Map<String,String> data="chat_message".equals(event.type())
                ?Map.of("type",event.type(),"conversationId",event.conversation().toString(),"messageId",event.resource().toString())
                :Map.of("type",event.type(),"momentId",event.resource().toString());
        boolean retry=false,disabled=false,rejected=false,accepted=false;
        for(var token:tokens) {
            // Re-check ownership immediately before each send: logout/reassignment must not use a stale token snapshot.
            if(!Boolean.TRUE.equals(jdbc.queryForObject("SELECT EXISTS(SELECT 1 FROM device_tokens WHERE user_id=? AND fcm_token=?)",
                    Boolean.class,event.recipient(),token))) continue;
            var result=sender.deliver(token,data);
            accepted|=result==PushNotificationSender.Delivery.ACCEPTED;
            if(result==PushNotificationSender.Delivery.UNREGISTERED)
                jdbc.update("DELETE FROM device_tokens WHERE user_id=? AND fcm_token=?",event.recipient(),token);
            retry|=result==PushNotificationSender.Delivery.RETRY;
            disabled|=result==PushNotificationSender.Delivery.DISABLED;
            rejected|=result==PushNotificationSender.Delivery.REJECTED;
        }
        finish(event,retry||disabled?"PENDING":rejected?"DEAD":accepted?"ACCEPTED":"CANCELLED",disabled?"FCM_DISABLED":retry?"TRANSIENT":rejected?"REJECTED":accepted?null:"NO_VALID_REGISTRATION");
    }

    private void finish(Event event,String state,String error) {
        int attempts=event.attempts()+1;
        if("PENDING".equals(state) && attempts>=maxAttempts) state="DEAD";
        long backoff=Math.min(3600,5L << Math.min(attempts-1,10));
        int changed=jdbc.update("""
                UPDATE push_outbox SET status=?,attempts=?,last_error=?,available_at=?,
                completed_at=?,lease_id=NULL,lease_until=NULL WHERE id=? AND lease_id=? AND status='PROCESSING'
                """,state,attempts,error,Timestamp.from(clock.instant().plusSeconds(backoff)),
                "PENDING".equals(state)?null:Timestamp.from(clock.instant()),event.id(),event.lease());
        if(changed==1 && "DEAD".equals(state))
            org.slf4j.LoggerFactory.getLogger(PushOutboxDispatcher.class).warn("Push outbox exhausted; reason={}, attempts={}",error,attempts);
    }
}
