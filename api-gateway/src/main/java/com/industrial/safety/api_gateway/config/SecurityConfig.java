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
                        .pathMatchers("/eureka/**").permitAll()

                        // Course - GET público, escritura requiere rol
                        .pathMatchers(HttpMethod.GET, "/api/v1/course").permitAll()
                        .pathMatchers(HttpMethod.GET, "/api/v1/course/my-courses").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/course/**").permitAll()
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
                                Role.ALUMNO.name(), Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.GET, "/api/v1/storage/upload-url/cover").hasAnyRole(
                                Role.INSTRUCTOR.name(), Role.ADMINISTRADOR.name()
                        )

                        // Users - especificos primero, wildcards al final
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/register").permitAll()
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/change-password").authenticated()
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/by-email").hasAnyRole(
                                Role.ALUMNO.name(), Role.ADMINISTRADOR.name(), Role.INSTRUCTOR.name(),
                                Role.TRABAJADOR.name(), Role.MARKETING.name(), Role.JEFE_SEGURIDAD.name(),
                                Role.GERENCIA_GENERAL.name(), Role.LOGISTICA_ALMACEN.name()
                        )
                        .pathMatchers(HttpMethod.PUT, "/api/v1/users/admin/{id}").hasAnyRole(
                                Role.ADMINISTRADOR.name(),
                                Role.INSTRUCTOR.name(),
                                Role.ALUMNO.name(),
                                Role.TRABAJADOR.name()
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
                        .pathMatchers(HttpMethod.GET, "/api/v1/users/**").hasAnyRole(
                                Role.ADMINISTRADOR.name()
                        )
                        .pathMatchers(HttpMethod.POST, "/api/v1/users/**").hasAnyRole(
                                Role.ADMINISTRADOR.name()
                        )

                        .anyExchange().authenticated())
                .oauth2Login(Customizer.withDefaults())
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