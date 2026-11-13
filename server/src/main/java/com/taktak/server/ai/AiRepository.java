package com.taktak.server.ai;
import static com.taktak.server.ai.AiDtos.*;
import java.util.*;
import java.time.Instant;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
@Repository
public class AiRepository {
    final JdbcTemplate db;
    public AiRepository(JdbcTemplate db){this.db=db;}
    void lock(UUID user){
        try {
            if(db.queryForList("select id from users where id=? for update nowait",user).isEmpty())throw new com.taktak.server.auth.AuthFailure();
        } catch(org.springframework.dao.DataAccessException failure) {
            for(Throwable cause=failure;cause!=null;cause=cause.getCause())
                if(cause instanceof java.sql.SQLException sql && "55P03".equals(sql.getSQLState()))
                    throw new AiFailure("AI_BUSY",org.springframework.http.HttpStatus.CONFLICT,2);
            throw failure;
        }
    }
    Conversation create(UUID user){
        if(db.queryForObject("select count(*) from ai_conversations where user_id=?",Integer.class,user)>=100)throw new AiFailure("AI_STORAGE_LIMIT",org.springframework.http.HttpStatus.CONFLICT);
        UUID id=UUID.randomUUID();db.update("insert into ai_conversations(id,user_id,title) values(?,?,?)",id,user,"Taktak AI");return find(user,id);
    }
    Conversation find(UUID user,UUID id){return db.query("select * from ai_conversations where id=? and user_id=?",(r,i)->new Conversation(r.getObject("id",UUID.class),r.getString("title"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),id,user).stream().findFirst().orElseThrow(()->new AiFailure("AI_CONVERSATION_NOT_FOUND",org.springframework.http.HttpStatus.NOT_FOUND));}
    List<Conversation> list(UUID user,int offset){return db.query("select * from ai_conversations where user_id=? order by updated_at desc,id desc limit 21 offset ?",(r,i)->new Conversation(r.getObject("id",UUID.class),r.getString("title"),r.getTimestamp("created_at").toInstant(),r.getTimestamp("updated_at").toInstant()),user,offset);}
    List<Message> messages(UUID conversation,int limit,int offset){return db.query("select * from ai_messages where conversation_id=? order by created_at desc, role asc,id desc limit ? offset ?",(r,i)->new Message(r.getObject("id",UUID.class),conversation,r.getString("role"),r.getString("content"),r.getTimestamp("created_at").toInstant(),r.getInt("input_tokens"),r.getInt("output_tokens")),conversation,limit,offset);}
    Message replay(UUID user,UUID key,String hash){
        var rows=db.queryForList("select * from ai_requests where user_id=? and client_request_id=?",user,key);
        if(rows.isEmpty())return null;var row=rows.get(0);
        if(!hash.equals(row.get("request_hash")))throw new AiFailure("AI_IDEMPOTENCY_CONFLICT",org.springframework.http.HttpStatus.CONFLICT);
        return messages((UUID)row.get("conversation_id"),1000,0).stream().filter(m->m.id().equals(row.get("message_id"))).findFirst().orElseThrow();
    }
    Message save(UUID user,UUID conversation,UUID request,String hash,String input,AiProvider.Answer answer){
        ensureCapacity(conversation);
        Instant now=Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);UUID outputId=UUID.randomUUID();
        db.update("insert into ai_messages(id,conversation_id,client_request_id,role,content,created_at) values(?,?,?,'user',?,?)",UUID.randomUUID(),conversation,request,input,java.sql.Timestamp.from(now));
        db.update("insert into ai_messages(id,conversation_id,client_request_id,role,content,created_at,input_tokens,output_tokens) values(?,?,?,'assistant',?,?,?,?)",outputId,conversation,request,answer.text(),java.sql.Timestamp.from(now.plusNanos(1000)),answer.inputTokens(),answer.outputTokens());
        db.update("insert into ai_requests(user_id,client_request_id,conversation_id,request_hash,message_id) values(?,?,?,?,?)",user,request,conversation,hash,outputId);
        db.update("update ai_conversations set updated_at=? where id=?",java.sql.Timestamp.from(now),conversation);
        return new Message(outputId,conversation,"assistant",answer.text(),now.plusNanos(1000),answer.inputTokens(),answer.outputTokens());
    }
    void ensureCapacity(UUID conversation){
        if(db.queryForObject("select count(*) from ai_messages where conversation_id=?",Integer.class,conversation)>998)
            throw new AiFailure("AI_STORAGE_LIMIT",org.springframework.http.HttpStatus.CONFLICT);
    }
    void delete(UUID user,UUID id){find(user,id);db.update("delete from ai_conversations where id=? and user_id=?",id,user);}
}
