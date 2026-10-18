package com.taktak.server;

import jakarta.servlet.http.*;
import java.io.IOException;
import java.net.URI;
import java.net.http.*;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;
import org.springframework.boot.autoconfigure.web.embedded.EmbeddedWebServerFactoryCustomizerAutoConfiguration;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.annotation.Bean;
import static org.assertj.core.api.Assertions.assertThat;

class TrustedProxyTest {
    @TestConfiguration(proxyBeanMethods = false)
    @ImportAutoConfiguration({ServletWebServerFactoryAutoConfiguration.class, EmbeddedWebServerFactoryCustomizerAutoConfiguration.class})
    static class ProxyFixture {
        @Bean ServletRegistrationBean<HttpServlet> probe() {
            return new ServletRegistrationBean<>(new HttpServlet() {
                @Override protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {
                    response.getWriter().write(request.getRemoteAddr());
                }
            }, "/probe");
        }
    }

    private String remoteAddress(String trustedRegex) throws Exception {
        var app = new SpringApplication(ProxyFixture.class);
        app.setAdditionalProfiles("prod");
        String[] arguments = trustedRegex == null
                ? new String[]{"--server.port=0", "--server.address=127.0.0.1"}
                : new String[]{"--server.port=0", "--server.address=127.0.0.1", "--server.tomcat.remoteip.internal-proxies="+trustedRegex};
        try (var context = (ServletWebServerApplicationContext) app.run(arguments)) {
            var request = HttpRequest.newBuilder(URI.create("http://127.0.0.1:"+context.getWebServer().getPort()+"/probe"))
                    .header("X-Forwarded-For", "198.51.100.42").GET().build();
            return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString()).body();
        }
    }
    @Test void productionLoopbackProxyRestoresClientAddressForQuota() throws Exception {
        assertThat(remoteAddress(null)).isEqualTo("198.51.100.42");
    }
    @Test void untrustedPeerCannotSpoofClientAddress() throws Exception {
        assertThat(remoteAddress("192\\.0\\.2\\.1")).isEqualTo("127.0.0.1");
    }
}
