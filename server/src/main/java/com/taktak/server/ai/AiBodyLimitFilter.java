package com.taktak.server.ai;
import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.*;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
@Component
public class AiBodyLimitFilter extends OncePerRequestFilter {
    @Override protected void doFilterInternal(HttpServletRequest request,HttpServletResponse response,FilterChain chain)throws IOException,ServletException {
        if(!request.getRequestURI().equals("/api/ai/chat")||!request.getMethod().equals("POST")){chain.doFilter(request,response);return;}
        byte[] data=request.getInputStream().readNBytes(24577);
        if(data.length>24576){response.setStatus(413);response.setContentType("application/json");response.setCharacterEncoding("UTF-8");response.setHeader("Cache-Control","no-store");response.getWriter().write("{\"timestamp\":\""+java.time.Instant.now()+"\",\"status\":413,\"code\":\"AI_REQUEST_TOO_LARGE\",\"message\":\"Yêu cầu AI quá lớn.\",\"path\":\"/api/ai/chat\"}");return;}
        chain.doFilter(new HttpServletRequestWrapper(request){
            @Override public ServletInputStream getInputStream(){var in=new ByteArrayInputStream(data);return new ServletInputStream(){
                public int read(){return in.read();}public boolean isFinished(){return in.available()==0;}public boolean isReady(){return true;}
                public void setReadListener(ReadListener listener){throw new UnsupportedOperationException();}
            };}
            @Override public BufferedReader getReader(){return new BufferedReader(new InputStreamReader(getInputStream(),java.nio.charset.StandardCharsets.UTF_8));}
        },response);
    }
}
