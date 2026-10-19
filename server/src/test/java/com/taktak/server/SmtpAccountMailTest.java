package com.taktak.server;

import com.taktak.server.auth.SmtpAccountMail;
import java.net.*;
import java.io.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class SmtpAccountMailTest {
    @Test void missingConfigurationFailsInsteadOfPretendingToSend() {
        var sender=new SmtpAccountMail("",587,"","","");
        assertThatThrownBy(() -> sender.send("qa@example.test","Test","Test"))
                .isInstanceOf(org.springframework.mail.MailSendException.class);
    }
    @Test void serverWithoutStartTlsNeverReceivesCredentialsOrMailBody() throws Exception {
        try (var socket=new ServerSocket(0,1,InetAddress.getByName("127.0.0.1"))) {
            socket.setSoTimeout(10000);
            var pool=Executors.newSingleThreadExecutor();
            try {
                var commands=pool.submit(() -> {
                    var received=new ArrayList<String>();
                    try (var client=socket.accept()) {
                        client.setSoTimeout(5000);
                        var writer=new PrintWriter(client.getOutputStream(),true);
                        var reader=new BufferedReader(new InputStreamReader(client.getInputStream(),java.nio.charset.StandardCharsets.US_ASCII));
                        writer.print("220 localhost test SMTP\r\n"); writer.flush();
                        String line;
                        while ((line=reader.readLine())!=null) {
                            received.add(line);
                            if (line.startsWith("QUIT")) { writer.print("221 bye\r\n"); writer.flush(); break; }
                            writer.print("250 localhost\r\n"); writer.flush();
                        }
                    }
                    return received;
                });
                String password=UUID.randomUUID().toString();
                var sender=new SmtpAccountMail("127.0.0.1",socket.getLocalPort(),"qa@example.test",password,"qa@example.test");
                assertThatThrownBy(() -> sender.send("qa@example.test","Test","private-body"))
                        .isInstanceOf(org.springframework.mail.MailException.class);
                assertThat(commands.get(10,TimeUnit.SECONDS)).allMatch(line -> !line.startsWith("AUTH") && !line.startsWith("DATA")
                        && !line.contains(password) && !line.contains("private-body"));
            } finally { pool.shutdownNow(); }
        }
    }
}
