package com.example.demo.config;

import com.example.demo.service.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService userDetailsService;

    public SecurityConfig(CustomUserDetailsService userDetailsService) {
        this.userDetailsService = userDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(userDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .headers()
                .frameOptions().disable()
                .and()

                .authorizeHttpRequests(auth -> auth
                        // ✅ NAJWAŻNIEJSZE - Static resources PIERWSZE!
                        .antMatchers(
                                "/",
                                "/index.html",
                                "/static/**",
                                "/assets/**",
                                "/*.js",
                                "/*.css",
                                "/*.ico",
                                "/*.png",
                                "/*.svg",
                                "/*.woff",
                                "/*.woff2",
                                "/favicon.ico",
                                "/vite.svg"
                        ).permitAll()

                        // React Router paths - SpaController obsługuje
                        .antMatchers(
                                "/dashboard",
                                "/dashboard/**",
                                "/tasks",
                                "/tasks/**",
                                "/projects",
                                "/projects/**",
                                "/teams",
                                "/teams/**",
                                "/profile",
                                "/profile/**",
                                "/login",
                                "/register",
                                "/settings",
                                "/settings/**"
                        ).permitAll()

                        // Public endpoints
                        .antMatchers("/registration", "/kontakt").permitAll()
                        .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .antMatchers("/files/**").permitAll()

                        // API - public
                        .antMatchers("/api/v1/auth/login", "/api/v1/auth/register").permitAll()
                        .antMatchers("/api/v1/chatbot/**").permitAll()

                        // API - authenticated
                        .antMatchers("/api/v1/auth/**").authenticated()
                        .antMatchers("/api/**").authenticated()

                        // Admin
                        .antMatchers("/admin/**").hasAuthority("SUPER_ADMIN")

                        // Reszta
                        .anyRequest().permitAll()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/dashboard", true)
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/login?logout")
                        .permitAll()
                )
                .csrf().ignoringAntMatchers(
                        "/teams/**",
                        "/projects/**",
                        "/proposals/**",
                        "/tasks/**",
                        "/status-requests/**",
                        "/notifications/**",
                        "/admin/**",
                        "/api/**"
                );

        return http.build();
    }
}