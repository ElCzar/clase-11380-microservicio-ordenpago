package com.orden_pago.demo.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Stream;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
public class SecurityConfig {

        @Bean
        public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(csrf -> csrf
                                                .disable()
                                )
                                .headers(headers -> headers
                                                .frameOptions(frameOptions -> frameOptions.sameOrigin())
                                )
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authorizeHttpRequests(authz -> authz
                                                // Endpoints públicos
                                                .requestMatchers("/actuator/**").permitAll()
                                                .requestMatchers("/h2-console/**").permitAll()

                                                // Endpoints protegidos
                                                .requestMatchers("/api/cart/**").authenticated()
                                                .requestMatchers("/api/payment/**").authenticated()

                                                .anyRequest().authenticated())
                                .oauth2ResourceServer(oauth2 -> oauth2
                                                .jwt(jwt -> jwt.jwtAuthenticationConverter(jwtAuthenticationConverter())));

                return http.build();
        }

        @Bean
        public JwtAuthenticationConverter jwtAuthenticationConverter() {
                JwtAuthenticationConverter converter = new JwtAuthenticationConverter();

                converter.setJwtGrantedAuthoritiesConverter(jwt -> {
                        JwtGrantedAuthoritiesConverter defaultConverter = new JwtGrantedAuthoritiesConverter();
                        Collection<GrantedAuthority> authorities = defaultConverter.convert(jwt);

                        // Extract realm roles from Keycloak JWT
                        Map<String, Object> realmAccess = jwt.getClaimAsMap("realm_access");
                        @SuppressWarnings("unchecked")
                        Collection<String> realmRoles = realmAccess != null
                                        ? (Collection<String>) realmAccess.get("roles")
                                        : Collections.emptyList();

                        // Combine Spring Security authorities with Keycloak roles
                        return Stream.concat(
                                        authorities != null ? authorities.stream() : Stream.empty(),
                                        realmRoles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)))
                                        .toList();
                });

                return converter;
        }

}
