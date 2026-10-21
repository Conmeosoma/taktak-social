package com.taktak.server.chat;

import com.taktak.server.chat.ChatDtos.Message;
import com.taktak.server.chat.ChatDtos.Peer;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ChatRepository {
    record ConversationRow(UUID id, UUID first, UUID second, UUID lastMessageId, Instant createdAt, Instant updatedAt) {
        UUID peer(UUID actor) { return first.equals(actor) ? second : first; }
        boolean contains(UUID actor) { return first.equals(actor) || second.equals(actor); }
    }
    private final JdbcTemplate jdbc;
    public ChatRepository(JdbcTemplate jdbc) { this.jdbc=jdbc; }
    private ConversationRow conversation(ResultSet r, int i) throws SQLException {
        return new ConversationRow(r.getObject("id",UUID.class),r.getObject("user1_id",UUID.class),r.getObject("user2_id",UUID.class),
                r.getObject("last_message_id",UUID.class),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant());
    }
    private Message message(ResultSet r,int i) throws SQLException {
        Timestamp read=r.getTimestamp("read_at");
        return new Message(r.getObject("id",UUID.class),r.getObject("conversation_id",UUID.class),r.getObject("sender_id",UUID.class),
                r.getObject("recipient_id",UUID.class),r.getObject("client_message_id",UUID.class),r.getString("message_type"),
                r.getString("content"),r.getObject("moment_id",UUID.class),r.getTimestamp("created_at").toInstant(),read==null?null:read.toInstant());
    }
    Optional<ConversationRow> find(UUID id) { return jdbc.query("select * from conversations where id=?",this::conversation,id).stream().findFirst(); }
    ConversationRow open(UUID first,UUID second,Instant now) {
        jdbc.update("insert into conversations(id,user1_id,user2_id,created_at,updated_at) values(?,least(?::uuid,?::uuid),greatest(?::uuid,?::uuid),?,?) on conflict(user1_id,user2_id) do nothing",
                UUID.randomUUID(),first,second,first,second,Timestamp.from(now),Timestamp.from(now));
        return jdbc.query("select * from conversations where user1_id=least(?::uuid,?::uuid) and user2_id=greatest(?::uuid,?::uuid)",this::conversation,first,second,first,second).get(0);
    }
    record InboxRow(ConversationRow conversation, Peer peer, Message lastMessage, long unreadCount) { }
    List<InboxRow> list(UUID actor,int offset) {
        return jdbc.query("""
                select c.*, p.id as peer_id,p.username as peer_username,p.display_name as peer_name,
                    m.id as last_id,m.sender_id as last_sender,m.recipient_id as last_recipient,
                    m.client_message_id as last_client,m.message_type as last_type,m.content as last_content,
                    m.moment_id as last_moment,m.created_at as last_created,m.read_at as last_read,
                    (select count(*) from messages um where um.conversation_id=c.id and um.recipient_id=? and um.read_at is null) as unread_count
                from conversations c
                join users p on p.id=case when c.user1_id=? then c.user2_id else c.user1_id end
                left join messages m on m.id=c.last_message_id
                where (c.user1_id=? or c.user2_id=?)
                and exists(select 1 from friendships f where f.user_low_id=c.user1_id and f.user_high_id=c.user2_id)
                and not exists(select 1 from blocks b where (b.blocker_id=c.user1_id and b.blocked_id=c.user2_id)
                    or (b.blocker_id=c.user2_id and b.blocked_id=c.user1_id))
                order by c.updated_at desc,c.id desc limit 21 offset ?
                """,(r,i)-> {
                    var row=conversation(r,i);
                    Timestamp read=r.getTimestamp("last_read");
                    Message last=r.getObject("last_id")==null?null:new Message(r.getObject("last_id",UUID.class),row.id(),
                            r.getObject("last_sender",UUID.class),r.getObject("last_recipient",UUID.class),r.getObject("last_client",UUID.class),
                            r.getString("last_type"),r.getString("last_content"),r.getObject("last_moment",UUID.class),
                            r.getTimestamp("last_created").toInstant(),read==null?null:read.toInstant());
                    return new InboxRow(row,new Peer(r.getObject("peer_id",UUID.class),r.getString("peer_username"),r.getString("peer_name"),null),last,r.getLong("unread_count"));
                },actor,actor,actor,actor,offset);
    }
    Optional<Message> findMessage(UUID id) { return jdbc.query("select * from messages where id=?",this::message,id).stream().findFirst(); }
    Optional<Message> retry(UUID sender,UUID key) {
        return jdbc.query("select * from messages where sender_id=? and client_message_id=?",this::message,sender,key).stream().findFirst();
    }
    boolean lockMoment(UUID id) {
        return !jdbc.query("select id from moments where id=? for update",(r,i)->r.getObject(1,UUID.class),id).isEmpty();
    }
    List<Message> messages(UUID conversation,int offset) {
        return jdbc.query("select * from messages where conversation_id=? order by created_at desc,id desc limit 51 offset ?",this::message,conversation,offset);
    }
    void insert(Message m) {
        jdbc.update("insert into messages(id,conversation_id,sender_id,recipient_id,client_message_id,message_type,content,moment_id,created_at) values(?,?,?,?,?,?,?,?,?)",
                m.id(),m.conversationId(),m.senderId(),m.recipientId(),m.clientMessageId(),m.messageType(),m.content(),m.momentId(),Timestamp.from(m.createdAt()));
        jdbc.update("update conversations set last_message_id=?,updated_at=? where id=?",m.id(),Timestamp.from(m.createdAt()),m.conversationId());
    }
    long unread(UUID conversation,UUID actor) {
        return jdbc.queryForObject("select count(*) from messages where conversation_id=? and recipient_id=? and read_at is null",Long.class,conversation,actor);
    }
    long totalUnread(UUID actor) {
        return jdbc.queryForObject("""
                select count(*) from messages m join conversations c on c.id=m.conversation_id
                where m.recipient_id=? and m.read_at is null
                and exists(select 1 from friendships f where f.user_low_id=c.user1_id and f.user_high_id=c.user2_id)
                and not exists(select 1 from blocks b where (b.blocker_id=c.user1_id and b.blocked_id=c.user2_id)
                    or (b.blocker_id=c.user2_id and b.blocked_id=c.user1_id))
                """,Long.class,actor);
    }
    void read(UUID conversation,UUID actor,Message anchor,Instant now) {
        jdbc.update("update messages set read_at=? where conversation_id=? and recipient_id=? and read_at is null and (created_at,id)<=(?,?::uuid)",
                Timestamp.from(now),conversation,actor,Timestamp.from(anchor.createdAt()),anchor.id());
    }
}
