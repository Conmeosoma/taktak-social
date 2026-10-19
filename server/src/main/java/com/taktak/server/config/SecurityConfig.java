package com.taktak.server.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import java.util.Set;
import org.springframework.core.annotation.Order;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {
    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    @Bean
    @Order(1)
    SecurityFilterChain publicAuthChain(HttpSecurity http, SecurityErrorHandler errors) throws Exception {
        Set<String> endpoints = Set.of("/api/auth/register", "/api/auth/login", "/api/auth/refresh", "/api/auth/logout",
                "/api/auth/verification/request", "/api/auth/verification/confirm",
                "/api/auth/forgot-password", "/api/auth/reset-password");
        // Không chạy bearer filter ở auth endpoints: refresh dùng credential trong body,
        // kể cả khi interceptor client gửi kèm access JWT đã hết hạn.
        return defaults(http, errors).securityMatcher(request ->
                ("GET".equals(request.getMethod()) && ("/actuator/health".equals(request.getServletPath())
                        || "/api/auth/check-username".equals(request.getServletPath())))
                || ("POST".equals(request.getMethod()) && endpoints.contains(request.getServletPath())))
                .authorizeHttpRequests(requests -> requests.anyRequest().permitAll()).build();
    }

    @Bean
    @Order(2)
    SecurityFilterChain protectedApiChain(HttpSecurity http, SecurityErrorHandler errors) throws Exception {
        return defaults(http, errors)
                .oauth2ResourceServer(resource -> resource.jwt(jwt -> { })
                        .authenticationEntryPoint(errors).accessDeniedHandler(errors))
                .authorizeHttpRequests(requests -> requests.requestMatchers("/api/**").authenticated()
                        .anyRequest().denyAll()).build();
    }

    private HttpSecurity defaults(HttpSecurity http, SecurityErrorHandler errors) throws Exception {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .httpBasic(AbstractHttpConfigurer::disable)
                .formLogin(AbstractHttpConfigurer::disable)
                .logout(AbstractHttpConfigurer::disable)
                .requestCache(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(handler -> handler.authenticationEntryPoint(errors).accessDeniedHandler(errors));
    }
}
