package com.toolrent.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.oidc.web.logout.OidcClientInitiatedLogoutSuccessHandler;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import static org.springframework.security.config.Customizer.withDefaults;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http,
                                            ClientRegistrationRepository repo) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                        // NO STATELESS mientras se use login browser
                        //.sessionManagement(s -> s.sessionCreationPolicy(STATELESS))


                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                "/swagger-ui/**",
                                "/swagger-ui/index.html",
                                "/v3/api-docs/**",
                                "/login/oauth2/code/keycloak",
                                "/error"               // SpringBoot devuelve 401/403 aquí
                        ).permitAll()
                        .requestMatchers("/users/**").hasRole("ADMIN")
                        .requestMatchers("/tools/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .requestMatchers("/customers/**").hasAnyRole("ADMIN", "EMPLOYEE")
                        .anyRequest().authenticated())

                .oauth2Login(withDefaults())
                .logout(logout -> logout
                        .logoutSuccessHandler(oidcLogoutSuccessHandler(repo)));

        http.oauth2ResourceServer(oauth2 ->
                oauth2.jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthConverter())));

        return http.build();
    }

    /* extrae roles de Keycloak */
    private JwtAuthenticationConverter jwtAuthConverter() {
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(jwt -> {
            Collection<GrantedAuthority> authorities = new ArrayList<>();

            // ➜ cast SEGURO con instanceof
            Object realmAccessObj = jwt.getClaims().get("realm_access");
            if (realmAccessObj instanceof Map<?, ?> realmAccess) {
                Object rolesObj = realmAccess.get("roles");
                if (rolesObj instanceof List<?> roles) {
                    roles.forEach(r -> authorities.add(new SimpleGrantedAuthority("ROLE_" + r)));
                }
            }
            return authorities;
        });
        return converter;
    }

    /* logout global */
    private OidcClientInitiatedLogoutSuccessHandler oidcLogoutSuccessHandler(ClientRegistrationRepository repo) {
        OidcClientInitiatedLogoutSuccessHandler handler =
                new OidcClientInitiatedLogoutSuccessHandler(repo);
        handler.setPostLogoutRedirectUri("http://localhost:8090/swagger-ui.html");
        return handler;
    }

    public static String getCurrentUsername() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Jwt jwt) {
            return jwt.getClaimAsString("preferred_username"); // Keycloak default
        }
        return "system"; // fallback
    }
}