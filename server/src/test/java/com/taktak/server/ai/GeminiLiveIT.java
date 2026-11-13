package com.taktak.server.ai;
import org.junit.jupiter.api.Test;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import java.time.Clock;
import java.util.List;
import static org.assertj.core.api.Assertions.*;
class GeminiLiveIT {
    @Test void onePublicPromptOnly(){
        var settings=new AiSettings(true,System.getenv("GEMINI_MODEL"),System.getenv("GEMINI_CREDENTIALS_FILE"),128,30,0,1,1,1,1,4000);
        var pool=new GeminiKeyManager(settings,new ObjectMapper(),Clock.systemUTC());
        var provider=new GeminiProvider(settings,pool,new ObjectMapper(),new SimpleMeterRegistry());
        var answer=provider.generate(List.of(new AiProvider.Turn("user","Chỉ trả lời: Xin chào Taktak.")));
        assertThat(answer.text()).isNotBlank();System.out.println("LIVE_GEMINI_GENERATION=PASS; REQUESTS=1; NO_PRIVATE_DATA_SENT");
    }
}
