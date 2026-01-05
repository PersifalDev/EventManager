package ru.haritonenko.eventmanager.user.security.configuration;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.AnonymousAuthenticationFilter;
import ru.haritonenko.eventmanager.user.security.custom.handler.CustomAccessDeniedHandler;
import ru.haritonenko.eventmanager.user.security.custom.authentification.CustomAuthenticationEntryPoint;
import ru.haritonenko.eventmanager.user.security.custom.service.CustomUserDetailsService;
import ru.haritonenko.eventmanager.user.security.jwt.filter.JwtTokenFilter;

@Configuration
@RequiredArgsConstructor
@EnableMethodSecurity
public class SecurityConfiguration {

    private final CustomUserDetailsService customUserDetailsService;
    private final CustomAuthenticationEntryPoint customAuthenticationEntryPoint;
    private final CustomAccessDeniedHandler customAccessDeniedHandler;

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, JwtTokenFilter jwtTokenFilter) {
        return http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/v3/api-docs/**",
                                "/v3/api-docs",
                                "/openapi.yaml"
                        ).permitAll()
                        .requestMatchers(HttpMethod.GET, "/users/{id}").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.POST, "/users").permitAll()
                        .requestMatchers(HttpMethod.POST, "/users/auth").permitAll()

                        .requestMatchers(HttpMethod.GET, "/locations").hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/locations/{id}").hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers(HttpMethod.POST, "/locations").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/locations/{id}").hasAuthority("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/locations/{id}").hasAuthority("ADMIN")

                        .requestMatchers(HttpMethod.POST, "/events").hasAuthority("USER")
                        .requestMatchers(HttpMethod.POST, "/events/registrations/**").hasAuthority("USER")
                        .requestMatchers(HttpMethod.POST, "/events/search").hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/events/{id}").hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers(HttpMethod.GET, "/events/my").hasAuthority("USER")
                        .requestMatchers(HttpMethod.GET, "/events/registrations/my").hasAuthority("USER")
                        .requestMatchers(HttpMethod.PUT, "/events/{id}").hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/events/{id}").hasAnyAuthority("ADMIN", "USER")
                        .requestMatchers(HttpMethod.DELETE, "/events/registrations/cancel/{id}").hasAuthority("USER")
                        .requestMatchers("/error", "/error/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(exception ->
                        exception
                                .authenticationEntryPoint(customAuthenticationEntryPoint)
                                .accessDeniedHandler(customAccessDeniedHandler)
                )
                .addFilterBefore(jwtTokenFilter, AnonymousAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationManager authenticationManager(
            AuthenticationConfiguration authenticationConfiguration
    ) {
        return authenticationConfiguration.getAuthenticationManager();
    }

    @Bean
    public AuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(customUserDetailsService);
        provider.setPasswordEncoder(passwordEncoder());
        return provider;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}
