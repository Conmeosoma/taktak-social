package com.taktak.server.ai;
import java.util.List;
public interface AiProvider {
    record Turn(String role,String text) {
        @Override public String toString(){return "Turn[redacted]";}
    }
    record Answer(String text,int inputTokens,int outputTokens) {
        @Override public String toString(){return "Answer[redacted]";}
    }
    Answer generate(List<Turn> context);
    String availability();
}
