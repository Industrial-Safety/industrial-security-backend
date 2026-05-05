package com.industrial.safety.api_gateway.config;

import com.industrial.safety.api_gateway.enums.Role;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
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
                .authorizeExchange(authorizeExchangeSpec -> authorizeExchangeSpec
                        .pathMatchers("/eureka/**").permitAll()
                        .pathMatchers("/api/v1/storage/**").hasRole(Role.INSTRUCTOR.name())
                        .pathMatchers(HttpMethod.POST,"/api/v1/course/**").hasRole(Role.INSTRUCTOR.name())
                        .pathMatchers(HttpMethod.GET,"/api/v1/course/**").hasAnyRole(
                                Role.ALUMNO.name(),
                                Role.MARKETING.name(),
                                Role.TRABAJADOR.name()
                        ).anyExchange().authenticated())
                .oauth2ResourceServer(o->o
                        .jwt(jwtSpec -> jwtSpec.jwtAuthenticationConverter(reactiveJwtAuthenticationConverterAdapter())));
        return serverHttpSecurity.build();
    }
    private ReactiveJwtAuthenticationConverterAdapter reactiveJwtAuthenticationConverterAdapter(){
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
    }
}
