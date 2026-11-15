package com.codecool.flightapi.security;

import io.github.bucket4j.TimeMeter;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher;
import org.springframework.security.web.session.DisableEncodeUrlFilter;
import org.springframework.security.web.util.matcher.AndRequestMatcher;
import org.springframework.security.web.util.matcher.RequestMatcher;
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    /**
     * {@code GET /api/v1/flights} with any search parameter. {@code FlightController} sends these requests
     * to the search (400 unless all parameters are given) and only parameterless ones to the full list.
     */
    private static final RequestMatcher FLIGHT_SEARCH = new AndRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.GET, "/api/v1/flights"),
            request -> Stream.of("from", "to", "date").anyMatch(name -> request.getParameter(name) != null));

    private final ProblemDetailAuthenticationEntryPoint unauthorizedHandler;
    private final ProblemDetailAccessDeniedHandler accessDeniedHandler;
    private final JwtAuthenticationConverter jwtAuthenticationConverter;
    private final RateLimitProperties rateLimitProperties;

    @Qualifier("handlerExceptionResolver") private final HandlerExceptionResolver exceptionResolver;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Built by Spring from the {@code UserDetailsService} and {@link PasswordEncoder} beans.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }

    /**
     * The application's filter chain. Authorization is deny-by-default: every request must match one
     * of the rules below, otherwise it is rejected. Rules are evaluated top to bottom.
     *
     * <ul>
     *   <li>Public: login/registration/refresh/logout (login and registration rate-limited per client IP, see {@link AuthRateLimitFilter}), error page, health/info probes, and the flight search and
     *       flight details the frontend shows before login.
     *   <li>Any signed-in user: every other {@code GET /api/v1/**}.
     *   <li>ADMIN: the full flight list and every write ({@code POST/PUT/DELETE /api/v1/**}).
     *   <li>Everything else: denied.
     * </ul>
     *
     * <p>Dev-only tooling (H2 console, Swagger UI) has its own chain in {@link DevToolsSecurityConfig}.
     */
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        // first in the chain: rejected attempts cost neither JSON parsing nor a password hash
        http.addFilterBefore(
                        new AuthRateLimitFilter(rateLimitProperties, exceptionResolver, TimeMeter.SYSTEM_NANOTIME),
                        DisableEncodeUrlFilter.class)
                .cors(Customizer.withDefaults())
                // stateless bearer-token API: no session, so no CSRF token to protect
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(
                        ex -> ex.authenticationEntryPoint(unauthorizedHandler).accessDeniedHandler(accessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        // --- public ---
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/v1/auth/register",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/logout")
                        .permitAll()
                        .requestMatchers("/error")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/actuator/health", "/actuator/health/**", "/actuator/info")
                        .permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights/{id}")
                        .permitAll()
                        .requestMatchers(FLIGHT_SEARCH)
                        .permitAll()
                        // --- admin-only reads ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/flights")
                        .hasRole("ADMIN")
                        // --- any signed-in user ---
                        .requestMatchers(HttpMethod.GET, "/api/v1/**")
                        .authenticated()
                        // --- writes ---
                        .requestMatchers(HttpMethod.POST, "/api/v1/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/v1/**")
                        .hasRole("ADMIN")
                        .requestMatchers(HttpMethod.DELETE, "/api/v1/**")
                        .hasRole("ADMIN")
                        // --- deny by default ---
                        .anyRequest()
                        .denyAll())
                // Bearer tokens are validated by the JwtDecoder from JwtConfig
                .oauth2ResourceServer(
                        oauth2 -> oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter))
                                .authenticationEntryPoint(unauthorizedHandler)
                                .accessDeniedHandler(accessDeniedHandler));

        return http.build();
    }
}
