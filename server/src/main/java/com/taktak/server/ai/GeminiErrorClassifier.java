package com.taktak.server.ai;
import java.util.Locale;
final class GeminiErrorClassifier {
    record Error(String category,boolean retry,boolean group,boolean invalid,long cooldown) { }
    static Error classify(int status,String body,long retryAfter) {
        String b=body.toLowerCase(Locale.ROOT);
        if(status==429){
            boolean daily=b.contains("perday")||b.contains("per_day")||b.contains("daily");
            boolean billing=b.contains("billing")||b.contains("spend");
            return new Error(daily?"DAILY":billing?"BILLING":b.contains("token")?"TOKEN_RATE":"RATE",false,true,false,
                    Math.max(retryAfter,daily?86400:billing?3600:60));
        }
        if(status==401 || status==403){
            boolean invalid=b.contains("api_key_invalid")||b.contains("api key not valid")||b.contains("reported as leaked");
            return new Error(invalid?"INVALID_KEY":b.contains("billing")?"BILLING":"PERMISSION",false,!invalid,invalid,3600);
        }
        if(status==503 || status==500 || status==502 || status==504)return new Error("SERVICE",true,false,false,Math.max(1,retryAfter));
        return new Error(status==404?"MODEL":"PAYLOAD",false,false,false,status==404?3600:0);
    }
}
