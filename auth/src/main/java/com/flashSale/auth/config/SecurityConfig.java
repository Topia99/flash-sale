package com.flashSale.auth.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
            http
                // M2 阶段：纯 API，先关 CSRF（否则 POST /register 可能 403）
                .csrf(csrf -> csrf.disable())

                // 不用 session（后面 JWT 也是 stateless）
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // 放行注册 / 登录 / 健康检查
                        .requestMatchers(HttpMethod.POST, "/auth/register", "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.GET, "/ping", "/actuator/health").permitAll()
                        // 其他先都放行 or 先拦住看你节奏
                        .anyRequest().authenticated()
                )

                // 不启用默认表单登录页面
                .httpBasic(Customizer.withDefaults());

        return http.build();
    }
}
