package com.taktak.server.ai;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
@Component
public final class GeminiKeyManager {
    static final class Key {
        final String id,group,secret; final boolean enabled;
        boolean invalid; Instant cooldown=Instant.EPOCH,lastSuccess; int failures;
        String lastError="NONE";
        Key(String id,String group,String secret,boolean enabled){this.id=id;this.group=group;this.secret=secret;this.enabled=enabled;}
        @Override public String toString(){return "Key[redacted]";}
    }
    private final List<Key> keys=new ArrayList<>();
    private final Map<String,Instant> groups=new HashMap<>();
    private final Clock clock;
    @org.springframework.beans.factory.annotation.Autowired
    public GeminiKeyManager(AiSettings settings,ObjectMapper mapper,Clock clock) {
        this.clock=clock;
        if(!settings.enabled || settings.credentials.isBlank())return;
        try {
            Path file=Path.of(settings.credentials);
            if(Files.size(file)>32768)throw new IllegalArgumentException();
            var root=mapper.readTree(Files.readString(file));
            for(var node:root.path("keys")) {
                if(keys.size()>=16)break;
                String secret=node.path("key").asText(),group=node.path("quotaGroup").asText("unclassified");
                if(secret.matches("[A-Za-z0-9_-]{20,255}") && !group.isBlank() && group.length()<=80)keys.add(new Key("key-"+keys.size(),group,secret,node.path("enabled").asBoolean(true)));
            }
        } catch(Exception ignored) { keys.clear(); }
        if(keys.isEmpty())org.slf4j.LoggerFactory.getLogger(getClass()).warn("AI credentials unavailable; non-AI modules remain operational.");
    }
    GeminiKeyManager(Clock clock,Key... values){this.clock=clock;keys.addAll(List.of(values));}
    public synchronized boolean configured(){return keys.stream().anyMatch(k->k.enabled&&!k.invalid);}
    public synchronized boolean available(){try{select();return true;}catch(AiFailure unavailable){return false;}}
    synchronized Key select() {
        Instant now=clock.instant();
        return keys.stream().filter(k->k.enabled&&!k.invalid&&!k.cooldown.isAfter(now)
                && !groups.getOrDefault(k.group,Instant.EPOCH).isAfter(now)).findFirst()
                .orElseThrow(()->new AiFailure("AI_TEMPORARILY_UNAVAILABLE",HttpStatus.SERVICE_UNAVAILABLE,60));
    }
    synchronized void success(Key key){key.failures=0;key.lastError="NONE";key.lastSuccess=clock.instant();}
    synchronized void fail(Key key,String category,long seconds,boolean group,boolean invalid){
        key.failures++;key.lastError=category;key.invalid=invalid;key.cooldown=clock.instant().plusSeconds(seconds);
        if(group)groups.merge(key.group,key.cooldown,(a,b)->a.isAfter(b)?a:b);
    }
}
