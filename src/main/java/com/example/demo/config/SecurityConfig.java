package com.example.demo.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable()) // API calling ke liye CSRF disable karna zaroori hai
            .authorizeHttpRequests(auth -> auth
                // In URLs par kisi ko bhi aane do (Frontend files aur Auth APIs)
                .requestMatchers("/api/login", "/api/register", "/h2-console/**", "/", "/index.html", "/*.css", "/*.js").permitAll()
                // Baaki sabke liye rule hata dete hain taaki aapka frontend bina error ke chale
                .anyRequest().permitAll() 
            )
            .headers(headers -> headers.frameOptions(frame -> frame.disable())); // H2 Database Console ke liye
        
        return http.build();
    }
}