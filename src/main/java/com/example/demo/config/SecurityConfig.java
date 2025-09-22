// src/main/java/com/example/demo/config/SecurityConfig.java - ROZSZERZONY
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
                .authorizeHttpRequests(auth -> auth
                        .antMatchers("/registration", "/login", "/kontakt").permitAll()
                        .antMatchers("/css/**", "/js/**", "/images/**").permitAll()
                        .antMatchers("/files/**").permitAll()

                        // NOWE - Dostęp do panelu administratora tylko dla super adminów
                        .antMatchers("/admin/**").hasAuthority("SUPER_ADMIN")

                        .antMatchers("/teams/**").authenticated()
                        .antMatchers("/projects/**").authenticated()
                        .antMatchers("/proposals/**").authenticated()
                        .antMatchers("/tasks/**").authenticated()
                        .antMatchers("/status-requests/**").authenticated()
                        .antMatchers("/notifications/**").authenticated()
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/login")
                        .defaultSuccessUrl("/tasks/dashboard", true)
                        .successHandler((request, response, authentication) -> {
                            // NOWE - Custom success handler
                            System.out.println("Użytkownik zalogował się: " + authentication.getName());

                            // Możesz tutaj dodać logikę aktualizacji lastLogin
                            // userService.updateLastLogin(authentication.getName());

                            response.sendRedirect("/tasks/dashboard");
                        })
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
                        "/admin/**"  // NOWE - Wyłącz CSRF dla panelu admina
                );
        return http.build();
    }
}