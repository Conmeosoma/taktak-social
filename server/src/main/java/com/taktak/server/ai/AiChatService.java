package com.taktak.server.ai;
import static com.taktak.server.ai.AiDtos.*;
import java.util.*;
import java.util.concurrent.*;
import java.time.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.http.HttpStatus;
@Service
public class AiChatService {
    private final AiRepository repo;private final AiProvider provider;private final AiSettings settings;
    private final TransactionTemplate tx;private final Semaphore permits;private final Clock clock;
    private final Map<UUID,Integer> users=new HashMap<>();private long minute=-1,day=-1;private int minuteCount,dayCount;
    public AiChatService(AiRepository repo,AiProvider provider,AiSettings settings,PlatformTransactionManager manager,Clock clock){
        this.repo=repo;this.provider=provider;this.settings=settings;this.clock=clock;tx=new TransactionTemplate(manager);tx.setTimeout(settings.timeout+10);permits=new Semaphore(settings.concurrency);
    }
    private synchronized void admit(UUID user){
        long now=clock.instant().getEpochSecond();
        if(minute!=now/60){minute=now/60;minuteCount=0;users.clear();}
        if(day!=now/86400){day=now/86400;dayCount=0;}
        if(users.size()>=10000||users.getOrDefault(user,0)>=settings.perUser||minuteCount>=settings.perMinute||dayCount>=settings.perDay)
            throw new AiFailure("AI_RATE_LIMITED",HttpStatus.TOO_MANY_REQUESTS,dayCount>=settings.perDay?86400-now%86400:60-now%60);
        users.merge(user,1,Integer::sum);minuteCount++;dayCount++;
    }
    public Status status(){return new Status(provider.availability(),"Nội dung chat AI và lịch sử gần đây được gửi đến Google Gemini để xử lý. Không gửi ảnh/chat riêng của Taktak. Không mã hóa đầu cuối.");}
    public Conversation create(UUID user){admit(user);return tx.execute(s->{repo.lock(user);return repo.create(user);});}
    public Page<Conversation> list(UUID user,int page){var rows=repo.list(user,offset(page));return new Page<>(rows.stream().limit(20).toList(),rows.size()>20);}
    public Page<Message> messages(UUID user,UUID id,int page){repo.find(user,id);var rows=repo.messages(id,21,offset(page));return new Page<>(rows.stream().limit(20).toList(),rows.size()>20);}
    public void delete(UUID user,UUID id){tx.executeWithoutResult(s->{repo.lock(user);repo.delete(user,id);});}
    private int offset(int page){if(page<0||page>10000)throw new AiFailure("AI_INVALID_PAGE",HttpStatus.BAD_REQUEST);return page*20;}
    public Message chat(UUID user,ChatRequest request){
        if(!Boolean.TRUE.equals(request.shareWithGoogle())||request.message()==null||request.message().isBlank()||request.message().length()>4000||request.clientRequestId()==null)
            throw new AiFailure("AI_INVALID_REQUEST",HttpStatus.BAD_REQUEST);
        if(!permits.tryAcquire())throw new AiFailure("AI_BUSY",HttpStatus.TOO_MANY_REQUESTS,2);
        try {
            return tx.execute(s->{
                repo.lock(user);
                String hash=hash(request.message()+"\u0000"+request.conversationId());
                var replay=repo.replay(user,request.clientRequestId(),hash);if(replay!=null)return replay;
                if(!provider.availability().equals("CONFIGURED"))throw new AiFailure("AI_"+provider.availability(),HttpStatus.SERVICE_UNAVAILABLE);
                UUID id=request.conversationId()==null?repo.create(user).id():repo.find(user,request.conversationId()).id();
                repo.ensureCapacity(id);
                admit(user);
                var context=new ArrayList<AiProvider.Turn>();
                int remaining=settings.contextChars-request.message().length();
                // Include only complete recent turns; a user message always starts the context.
                var recent=repo.messages(id,16,0);
                for(int i=0;i+1<recent.size();i+=2){var assistant=recent.get(i);var question=recent.get(i+1);
                    int size=assistant.content().length()+question.content().length();if(size>remaining)break;
                    context.add(0,new AiProvider.Turn("assistant",assistant.content()));context.add(0,new AiProvider.Turn("user",question.content()));remaining-=size;
                }
                context.add(new AiProvider.Turn("user",request.message()));
                var answer=provider.generate(List.copyOf(context));
                return repo.save(user,id,request.clientRequestId(),hash,request.message(),answer);
            });
        } catch(RuntimeException failure){
            for(Throwable cause=failure;cause!=null;cause=cause.getCause()){
                if(cause instanceof java.sql.SQLException sql && "55P03".equals(sql.getSQLState()))throw new AiFailure("AI_BUSY",HttpStatus.CONFLICT,2);
            }
            if(failure instanceof org.springframework.dao.PessimisticLockingFailureException)throw new AiFailure("AI_BUSY",HttpStatus.CONFLICT,2);
            throw failure;
        }
        finally {permits.release();}
    }
    private String hash(String value){try{return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));}catch(Exception e){throw new IllegalStateException();}}
}
