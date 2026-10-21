package com.taktak.server.chat;

import com.taktak.server.auth.AuthFailure;
import com.taktak.server.chat.ChatDtos.*;
import com.taktak.server.outbox.PushOutbox;
import com.taktak.server.friend.FriendRequestRepository;
import com.taktak.server.moment.Moment;
import com.taktak.server.moment.MomentRepository;
import com.taktak.server.user.UserRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ChatService {
    private final ChatRepository chats;
    private final UserRepository users;
    private final FriendRequestRepository friends;
    private final MomentRepository moments;
    private final PushOutbox outbox;
    private final Clock clock;
    public ChatService(ChatRepository chats,UserRepository users,FriendRequestRepository friends,MomentRepository moments,
            PushOutbox outbox,Clock clock) {
        this.chats=chats;this.users=users;this.friends=friends;this.moments=moments;this.outbox=outbox;this.clock=clock;
    }
    private ChatFailure missing() { return new ChatFailure(HttpStatus.NOT_FOUND,"CHAT_UNAVAILABLE"); }
    private ChatFailure invalid() { return new ChatFailure(HttpStatus.BAD_REQUEST,"VALIDATION_ERROR"); }
    private void actor(UUID actor) { if(!users.existsById(actor)) throw new AuthFailure(); }
    private void page(int page) { if(page<0 || page>10000) throw invalid(); }
    private void pair(UUID actor,UUID peer,boolean lock) {
        if(actor.equals(peer)) throw invalid();
        if(lock && users.lockPair(actor,peer).size()!=2) throw missing();
        if(friends.blocked(actor,peer) || !friends.alreadyFriends(actor,peer)) throw missing();
    }
    private ChatRepository.ConversationRow allowed(UUID actor,UUID id,boolean lock) {
        actor(actor);
        var row=chats.find(id).filter(c->c.contains(actor)).orElseThrow(this::missing);
        pair(actor,row.peer(actor),lock);
        // Re-read after serializing with sends/relationship mutations.
        return lock?chats.find(id).orElseThrow(this::missing):row;
    }
    private Conversation dto(UUID actor,ChatRepository.ConversationRow c) {
        var peer=users.findById(c.peer(actor)).orElseThrow(this::missing);
        return new Conversation(c.id(),new Peer(peer.getId(),peer.getUsername(),peer.getDisplayName(),null),
                c.lastMessageId()==null?null:safe(chats.findMessage(c.lastMessageId()).orElseThrow()),chats.unread(c.id(),actor),c.updatedAt());
    }
    private boolean grant(Moment m,UUID viewer) { return m.getSenderId().equals(viewer)||moments.isRecipient(m.getId(),viewer); }
    private Message safe(Message m) {
        boolean reply="MOMENT_REPLY".equals(m.messageType());
        if(!reply && !"MOMENT".equals(m.messageType())) return m;
        var moment=m.momentId()==null?null:moments.findById(m.momentId()).orElse(null);
        boolean visible=moment!=null && moment.getDeletedAt()==null && grant(moment,m.senderId()) && grant(moment,m.recipientId());
        return new Message(m.id(),m.conversationId(),m.senderId(),m.recipientId(),m.clientMessageId(),m.messageType(),
                reply?m.content():(visible?moment.getCaption():null),visible?m.momentId():null,m.createdAt(),m.readAt());
    }
    @Transactional(readOnly=true)
    public ConversationsPage list(UUID actor,int page) {
        actor(actor);page(page);var rows=chats.list(actor,page*20);
        // Peer/last-message/unread được lấy cùng trang; safe vẫn xác minh quyền ảnh hiện tại.
        return new ConversationsPage(rows.stream().limit(20).map(row -> new Conversation(row.conversation().id(),row.peer(),
                row.lastMessage()==null?null:safe(row.lastMessage()),row.unreadCount(),row.conversation().updatedAt())).toList(),page,rows.size()>20,chats.totalUnread(actor));
    }
    @Transactional
    public Conversation open(UUID actor,UUID peer) { actor(actor);pair(actor,peer,true);return dto(actor,chats.open(actor,peer,clock.instant())); }
    @Transactional(readOnly=true)
    public Conversation conversation(UUID actor,UUID id) { return dto(actor,allowed(actor,id,false)); }
    @Transactional(readOnly=true)
    public MessagesPage messages(UUID actor,UUID id,int page) {
        page(page);allowed(actor,id,false);var rows=chats.messages(id,page*50);
        return new MessagesPage(rows.stream().limit(50).map(this::safe).toList(),page,rows.size()>50);
    }
    @Transactional
    public Message send(UUID actor,UUID id,SendRequest request) {
        var c=allowed(actor,id,true);
        String content=request.content();
        boolean reply="MOMENT_REPLY".equals(request.messageType());
        if("TEXT".equals(request.messageType()) || reply) {
            if(content==null||content.isBlank()||content.indexOf('\0')>=0||content.codePointCount(0,content.length())>2000
                    || (reply?request.momentId()==null:request.momentId()!=null)) throw invalid();
        } else if("MOMENT".equals(request.messageType())) {
            if(request.momentId()==null || content!=null) throw invalid();
            if(!chats.lockMoment(request.momentId())) throw missing();
            var moment=moments.findById(request.momentId()).filter(m->m.getDeletedAt()==null && grant(m,actor)&&grant(m,c.peer(actor))).orElseThrow(this::missing);
            content=moment.getCaption();
        } else throw invalid();
        var retry=chats.retry(actor,request.clientMessageId());
        if(retry.isPresent()) {
            var prior=retry.get();
            if(!prior.conversationId().equals(id)||!prior.messageType().equals(request.messageType())||!Objects.equals(prior.content(),content)||!Objects.equals(prior.momentId(),request.momentId()))
                throw new ChatFailure(HttpStatus.CONFLICT,"CHAT_IDEMPOTENCY_CONFLICT");
            return safe(prior);
        }
        if(reply) {
            if(!chats.lockMoment(request.momentId())) throw missing();
            moments.findById(request.momentId()).filter(m->m.getDeletedAt()==null
                    && m.getSenderId().equals(c.peer(actor)) && grant(m,actor)).orElseThrow(this::missing);
        }
        Instant now=clock.instant().truncatedTo(ChronoUnit.MICROS);
        if(!now.isAfter(c.updatedAt())) now=c.updatedAt().plus(1,ChronoUnit.MICROS);
        var message=new Message(UUID.randomUUID(),id,actor,c.peer(actor),request.clientMessageId(),request.messageType(),content,request.momentId(),now,null);
        chats.insert(message);
        outbox.enqueue("chat_message",message.id(),id,actor,c.peer(actor));
        return message;
    }
    @Transactional
    public void read(UUID actor,UUID id,UUID anchorId) {
        allowed(actor,id,true);
        var anchor=chats.findMessage(anchorId).filter(m->m.conversationId().equals(id)).orElseThrow(this::missing);
        chats.read(id,actor,anchor,clock.instant());
    }
}
