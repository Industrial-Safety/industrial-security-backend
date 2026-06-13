package com.industrial.safety.api_gateway.config;

import com.industrial.safety.api_gateway.enums.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.ReactiveJwtAuthenticationConverterAdapter;
import org.springframework.security.web.server.SecurityWebFilterChain;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {
    @Bean
    public SecurityWebFilterChain securityWebFilterChain(ServerHttpSecurity serverHttpSecurity){
        serverHttpSecurity.csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec
                        // Actuator expuesto para diagnóstico local (gateway routes, health, etc.)
                        .pathMatchers("/actuator/**").permitAll()

                        // Course - GET público, escritura requiere rol
                        .pathMatchers(HttpMethod.GET, "/api/v1/course").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/course/my-courses").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/course/**").permitAll()
                        // Marketing puede crear y consultar solicitudes de precio
                        .pathMatchers(HttpMethod.POST, "/api/v1/course/price-requests").hasAnyRole(
                                Role.MARKETING.name(), Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/course/price-requests/**").hasAnyRole(
                                Role.MARKETING.name(), Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.POST, "/api/v1/course/**").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.PUT, "/api/v1/course/**").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/course/**").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )

                        // Storage
                        .pathMatchers(HttpMethod.GET, "/api/v1/storage/upload-url").hasAnyRole(
                                Role.ALUMNO.name(), Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name(),
                                Role.TRABAJADOR.name(), Role.MARKETING.name(), Role.JEFE_SEGURIDAD.name(),
                                Role.GERENCIA_GENERAL.name(), Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/storage/upload-url/cover").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )

                        // Users - especificos primero, wildcards al final
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/change-password").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/by-dni").hasAnyRole(
                                Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/by-email").hasAnyRole(
                                Role.ALUMNO.name(), Role.ADMINISTRADOR.name(), Role.INSTRUCTOR.name(),
                                Role.TRABAJADOR.name(), Role.MARKETING.name(), Role.JEFE_SEGURIDAD.name(),
                                Role.GERENCIA_GENERAL.name(), Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers(HttpMethod.PUT, "/api/v1/users/admin/{id}").hasAnyRole(
                                Role.ADMINISTRADOR.name(),
                                Role.INSTRUCTOR.name(),
                                Role.ALUMNO.name(),
                                Role.TRABAJADOR.name(),
                                Role.MARKETING.name(),
                                Role.JEFE_SEGURIDAD.name(),
                                Role.GERENCIA_GENERAL.name(),
                                Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/{id}").hasAnyRole(
                                Role.ALUMNO.name(), Role.ADMINISTRADOR.name(), Role.INSTRUCTOR.name(),
                                Role.TRABAJADOR.name(), Role.MARKETING.name(), Role.JEFE_SEGURIDAD.name(),
                                Role.GERENCIA_GENERAL.name(), Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers(HttpMethod.PUT, "/api/v1/users/{id}").hasAnyRole(
                                Role.ALUMNO.name(), Role.ADMINISTRADOR.name(), Role.INSTRUCTOR.name(),
                                Role.TRABAJADOR.name(), Role.MARKETING.name(), Role.JEFE_SEGURIDAD.name(),
                                Role.GERENCIA_GENERAL.name(), Role.LOGISTICA_ALMACEN.name()
                        )
                        // Lista de usuarios (combo box de TRABAJADOR para el jefe de seguridad)
                        .pathMatchers(HttpMethod.GET, "/api/v1/users").hasAnyRole(
                                Role.ADMINISTRADOR.name(), Role.JEFE_SEGURIDAD.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole(
                                Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/**").hasAnyRole(
                                Role.ADMINISTRADOR.name()
                        )

                        // Orders - cualquier usuario autenticado (el rol se asigna async tras OAuth signup)
                        // Asignación masiva de cursos: solo administrador (antes del matcher genérico)
                        .pathMatchers(HttpMethod.POST, "/api/v1/orders/admin/**").hasRole(
                                Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.POST, "/api/v1/orders").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/v1/orders/**").authenticated()
                        .pathMatchers(HttpMethod.DELETE, "/api/v1/orders/**").authenticated()

                        // Payments - webhook MP es público (entra sin JWT), recibos también públicos
                        // (el link del email tiene que abrirse sin login)
                        .pathMatchers(HttpMethod.POST, "/api/v1/payments/webhook").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/payments/receipts/**").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/payments/**").authenticated()

                        // Exams - instructor crea, alumno toma
                        .pathMatchers(HttpMethod.POST, "/api/v1/exams/parse").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.POST, "/api/v1/exams").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        // Ranking - visible solo a trabajadores (antes del wildcard de exams)
                        .pathMatchers(HttpMethod.GET, "/api/v1/exams/ranking").hasRole(Role.TRABAJADOR.name())
                        .pathMatchers(HttpMethod.GET, "/api/v1/exams/**").authenticated()
                        .pathMatchers(HttpMethod.POST, "/api/v1/exams/*/attempts").hasAnyRole(
                                Role.ALUMNO.name(), Role.TRABAJADOR.name()
                        )

                        // Certificates - solo el propio alumno
                        .pathMatchers(HttpMethod.GET, "/api/v1/certificates/**").authenticated()

                        // Purchase - entregas propias accesibles al trabajador
                        .pathMatchers(HttpMethod.GET, "/api/v1/purchase/epp/deliveries").hasAnyRole(
                                Role.LOGISTICA_ALMACEN.name(), Role.TRABAJADOR.name()
                        )
                        // Gerencia puede ver y aprobar/rechazar solicitudes de compra
                        .pathMatchers(HttpMethod.GET, "/api/v1/purchase/requests", "/api/v1/purchase/requests/**").hasAnyRole(
                                Role.LOGISTICA_ALMACEN.name(), Role.GERENCIA_GENERAL.name()
                        )
                        // Gerencia tambien puede ver el inventario (dashboard de compras)
                        .pathMatchers(HttpMethod.GET, "/api/v1/purchase/inventory", "/api/v1/purchase/inventory/**").hasAnyRole(
                                Role.LOGISTICA_ALMACEN.name(), Role.GERENCIA_GENERAL.name()
                        )
                        .pathMatchers(HttpMethod.PUT, "/api/v1/purchase/requests/**").hasAnyRole(
                                Role.LOGISTICA_ALMACEN.name(), Role.GERENCIA_GENERAL.name()
                        )
                        // Marketing puede crear solicitudes de compra
                        .pathMatchers(HttpMethod.POST, "/api/v1/purchase/requests").hasAnyRole(
                                Role.MARKETING.name(), Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers("/api/v1/purchase/**").hasRole(Role.LOGISTICA_ALMACEN.name())

                        // WebSocket - el browser no puede enviar Bearer en el handshake inicial
                        .pathMatchers("/ws/**", "/ws-sockjs/**").permitAll()

                        // Safety - incidentes de seguridad industrial
                        .pathMatchers(HttpMethod.POST, "/api/v1/incidents").permitAll()
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/incidents/*/review").hasAnyRole(
                                Role.JEFE_SEGURIDAD.name(), Role.ADMINISTRADOR.name()
                        )
                        // Apelaciones: el trabajador apela; el jefe que aprobó las resuelve
                        .pathMatchers(HttpMethod.POST, "/api/v1/incidents/*/appeal").hasRole(
                                Role.TRABAJADOR.name()
                        )
                        .pathMatchers(HttpMethod.PATCH, "/api/v1/incidents/*/appeal/resolve").hasAnyRole(
                                Role.JEFE_SEGURIDAD.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/incidents/appeals").hasAnyRole(
                                Role.JEFE_SEGURIDAD.name(), Role.ADMINISTRADOR.name()
                        )
                        // El trabajador ve sus propias infracciones y su puntaje (antes del wildcard)
                        .pathMatchers(HttpMethod.GET, "/api/v1/incidents/mine").hasRole(Role.TRABAJADOR.name())
                        .pathMatchers(HttpMethod.GET, "/api/v1/safety-score/me").hasRole(Role.TRABAJADOR.name())
                        .pathMatchers(HttpMethod.GET, "/api/v1/incidents/**").hasAnyRole(
                                Role.JEFE_SEGURIDAD.name(), Role.ADMINISTRADOR.name(), Role.GERENCIA_GENERAL.name()
                        )

                        .anyExchange().authenticated())
                .oauth2ResourceServer(o -> o
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverterAdapter())));
        return serverHttpSecurity.build();
    }

    /*private ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverterAdapter(){
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter(); //creamos el converitor
        converter.setJwtGrantedAuthoritiesConverter(j->
        { Map<String,Object> realmAcess =( Map<String,Object>) j.getClaims().get("realm_access"); //navegando buscado el token creando el premiso y traformand
            if(realmAcess ==null || realmAcess.isEmpty()) {
                return Collections.emptyList();   //validamos epero si pasamos
            }
            Collection<String> roles = (Collection<String>) realmAcess.get("roles"); //creamoslos permisos
            return  roles.stream()
                    .map( role -> new SimpleGrantedAuthority("ROLE_" + role))
                    .collect(Collectors.toSet()); //tranformamos concantenando
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter); //devolvemos el envoltorio
    }*/
    private ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverterAdapter(){
        JwtAuthenticationConverter converter = new JwtAuthenticationConverter();
        converter.setJwtGrantedAuthoritiesConverter(j -> {
            Map<String, Object> realmAccess = (Map<String, Object>) j.getClaims().get("realm_access");
            if (realmAccess == null || realmAccess.isEmpty()) {
                return Collections.emptyList();
            }
            Collection<String> roles = (Collection<String>) realmAccess.get("roles");
            return roles.stream()
                    .map(role -> new SimpleGrantedAuthority(
                            role.startsWith("ROLE_") ? role : "ROLE_" + role
                    ))
                    .collect(Collectors.toSet());
        });
        return new ReactiveJwtAuthenticationConverterAdapter(converter);
    }
}