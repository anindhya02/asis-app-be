package io.propenuy.asis_app_be.security;

import io.propenuy.asis_app_be.restdto.response.BaseResponseDTO;
import io.propenuy.asis_app_be.security.jwt.JwtTokenFilter;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpMethod;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final JwtTokenFilter jwtTokenFilter;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.disable())

            .sessionManagement(session ->
                session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
            )

            .authorizeHttpRequests(auth -> auth
                // ================= PUBLIC =================
                .requestMatchers("/api/auth/**").permitAll()

                // ================= ROLE BASED =================
                .requestMatchers("/api/users/**").hasAuthority("ADMIN")
                .requestMatchers(HttpMethod.POST, "/api/income-transactions/**").hasAuthority("PENGURUS")
                .requestMatchers(HttpMethod.DELETE, "/api/income-transactions/**").hasAuthority("KETUA YAYASAN")
                .requestMatchers("/api/income-transactions/**").hasAnyAuthority("PENGURUS", "KETUA YAYASAN")
                .requestMatchers("/api/activities/*/replies").hasAnyAuthority("DONATUR", "PENGURUS")
                .requestMatchers("/api/replies/**").hasAnyAuthority("DONATUR", "PENGURUS")
                .requestMatchers(HttpMethod.POST, "/api/expense-transactions/**").hasAuthority("PENGURUS")
                .requestMatchers(HttpMethod.GET, "/api/expense-transactions/**").hasAnyAuthority("ADMIN", "PENGURUS", "KETUA YAYASAN")
                .requestMatchers(HttpMethod.PUT, "/api/expense-transactions/**").hasAnyAuthority("ADMIN", "PENGURUS", "KETUA YAYASAN")
                .requestMatchers(HttpMethod.DELETE, "/api/expense-transactions/**").hasAnyAuthority("ADMIN", "PENGURUS", "KETUA YAYASAN")
                .requestMatchers("/api/activities/**").hasAnyAuthority("ADMIN", "PENGURUS")
                .requestMatchers("/api/payment-requests/**").hasAnyAuthority("PENGURUS", "KETUA YAYASAN")
                .requestMatchers("/api/payment-requests-review/**").hasAnyAuthority("KETUA YAYASAN")
                .requestMatchers("/api/donasi/**").hasAnyAuthority("ADMIN", "DONATUR")
                .requestMatchers("/api/laporan/**").hasAnyAuthority("ADMIN", "KETUA YAYASAN")
                .requestMatchers("/api/mis/**").hasAnyAuthority("PENGURUS", "KETUA YAYASAN")

                // ================= DEFAULT =================
                .anyRequest().authenticated()
            )

            .exceptionHandling(ex -> ex
                .authenticationEntryPoint((request, response, authException) -> {
                    response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                    response.setContentType("application/json");

                    BaseResponseDTO<Object> body = BaseResponseDTO.builder()
                            .status("error")
                            .message("Unauthorized - Silakan login terlebih dahulu")
                            .data(null)
                            .build();

                    new ObjectMapper().writeValue(response.getOutputStream(), body);
                })
                .accessDeniedHandler((request, response, accessDeniedException) -> {
                    response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                    response.setContentType("application/json");

                    BaseResponseDTO<Object> body = BaseResponseDTO.builder()
                            .status("error")
                            .message("Forbidden - Anda tidak memiliki akses")
                            .data(null)
                            .build();

                    new ObjectMapper().writeValue(response.getOutputStream(), body);
                })
            )

            .addFilterBefore(jwtTokenFilter,
                    UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    // ================= CORS =================
    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(
            "http://localhost:5173",
            "http://localhost:5174",
            "https://asis-app-fe-2.vercel.app"
        ));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    // ================= AUTH MANAGER =================
    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    // ================= PASSWORD ENCODER =================
    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
