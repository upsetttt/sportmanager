package vn.sannhaminh.config;

<<<<<<< HEAD
import java.util.List;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
=======
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
<<<<<<< HEAD
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
=======
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
<<<<<<< HEAD

import vn.sannhaminh.security.JwtAuthFilter;
=======
import vn.sannhaminh.security.JwtAuthFilter;
import java.util.List;
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1

@Configuration
@EnableMethodSecurity
public class SecurityConfig {
<<<<<<< HEAD
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of("http://localhost:5173", "http://127.0.0.1:5173"));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwtFilter) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .exceptionHandling(errors -> errors.authenticationEntryPoint(
                new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                .requestMatchers("/api/auth/register", "/api/auth/login", "/api/health", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/categories", "/api/venues", "/api/venues/page", "/api/venues/*").permitAll()
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/api/owner/**").hasRole("OWNER")
                .requestMatchers("/api/player/**").hasRole("PLAYER")
                .requestMatchers("/api/bookings/**").hasRole("PLAYER")
                .anyRequest().authenticated())
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .build();
=======
    @Bean PasswordEncoder passwordEncoder() { return new BCryptPasswordEncoder(); }
    @Bean CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration c = new CorsConfiguration(); c.setAllowedOrigins(List.of("http://localhost:5173"));
        c.setAllowedMethods(List.of("GET","POST","PUT","PATCH","DELETE","OPTIONS"));
        c.setAllowedHeaders(List.of("Authorization","Content-Type")); c.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource s = new UrlBasedCorsConfigurationSource(); s.registerCorsConfiguration("/**", c); return s;
    }
    @Bean SecurityFilterChain filterChain(HttpSecurity http, JwtAuthFilter jwt) throws Exception {
        return http.csrf(csrf -> csrf.disable()).cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(a -> a
                .requestMatchers("/api/auth/**", "/api/health", "/error").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/venues/**").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(jwt, UsernamePasswordAuthenticationFilter.class).build();
>>>>>>> 52f75fe7e53e845a7a69ee99bc0c0647ef9481d1
    }
}
