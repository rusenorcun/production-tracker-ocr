package com.example.uretimveri.config;

import com.example.uretimveri.repository.UserRepository;

import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.userdetails.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {


    private final AccessDenied accessDeniedHandler = new AccessDenied();

    @Bean
    SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        
        http
            .cors(c -> {})
            .exceptionHandling(ex -> ex.accessDeniedHandler(accessDeniedHandler))
            
            // CSRF
            .csrf(csrf -> csrf
                .ignoringRequestMatchers(
                    req -> "POST".equalsIgnoreCase(req.getMethod()) && "/api/slabs/recognize".equals(req.getServletPath()),
                    req -> "POST".equalsIgnoreCase(req.getMethod()) && "/api/slabs/save".equals(req.getServletPath()),
                    req -> "POST".equalsIgnoreCase(req.getMethod()) && "/api/slabs/recognize/callback".equals(req.getServletPath()),
                    req -> "GET".equalsIgnoreCase(req.getMethod()) && "/api/slabs/recognize".equals(req.getServletPath()),
                    req -> "GET".equalsIgnoreCase(req.getMethod()) && "/api/slabs/save".equals(req.getServletPath()),
                    req -> "GET".equalsIgnoreCase(req.getMethod()) && "/api/slabs/recognize/callback".equals(req.getServletPath())
                )
                .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
            )

            // Yetkiler
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/", "/index", "/login", "/doLogin",
                        "/register", "/doRegister",
                        "/css/**", "/js/**", "/images/**", "/webjars/**", "/img/**",
                        "/test", "/test-upload.html", "/test-upload").permitAll()

                .requestMatchers(HttpMethod.POST, "/api/slabs/recognize").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/slabs/recognize/callback").permitAll()

                .requestMatchers("/admin/**").hasRole("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/products/bulk-delete").hasRole("ADMIN")

                .requestMatchers(HttpMethod.GET, "/api/**").hasAnyRole("USER", "OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/**").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PUT,  "/api/**").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.PATCH,"/api/**").hasAnyRole("OPERATOR", "ADMIN")
                .requestMatchers(HttpMethod.DELETE, "/api/**").hasAnyRole("OPERATOR", "ADMIN")

                .anyRequest().authenticated()
            )

            // Login
            .formLogin(form -> form
                .loginPage("/login")
                .loginProcessingUrl("/doLogin")
                .usernameParameter("username")
                .passwordParameter("password")
                .successHandler((request, response, authentication) -> {
                    request.getSession().setAttribute("success", "Giriş başarılı. Hoş geldiniz!");
                    response.sendRedirect("/");
                })
                .failureUrl("/login?error")
                .permitAll()
            )

            // Logout
            .logout(logout -> logout
                .logoutUrl("/logout")
                .logoutSuccessUrl("/login?logout")
                .invalidateHttpSession(true)
                .deleteCookies("JSESSIONID", "XSRF-TOKEN")
            )

            // Session
            .sessionManagement(sess -> sess
                .maximumSessions(1)
                .maxSessionsPreventsLogin(false)
            );

        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }

    @Bean
    UserDetailsService userDetailsService(UserRepository userRepo) {
        return username -> userRepo.findByUsername(username)
            .map(u -> User.withUsername(u.getUsername())
                .password(u.getPassword())
                .roles(u.getPermission()) // "ADMIN", "OPERATOR", "USER" gibi
                .build())
            .orElseThrow(() -> new UsernameNotFoundException("Kullanıcı yok: " + username));
    }
    //UZAK BAĞLANTI AYARLARI
    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        var cfg = new CorsConfiguration();
        cfg.setAllowedOriginPatterns(List.of("*"));   // DEV ortamı için
        cfg.setAllowedMethods(List.of("GET","POST","PUT","DELETE","PATCH","OPTIONS"));
        cfg.setAllowedHeaders(List.of("*"));
        cfg.setAllowCredentials(true);
        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

}
