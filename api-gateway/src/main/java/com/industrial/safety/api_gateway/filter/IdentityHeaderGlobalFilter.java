package com.industrial.safety.api_gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

/**
 * Liga la identidad downstream al token de Keycloak.
 *
 * Problema que resuelve: los servicios internos confían en la cabecera {@code X-User-Id}
 * para saber "de qué usuario" son los datos. Esa cabecera la pone el cliente y es
 * falsificable → un usuario autenticado podría poner el id de OTRO y acceder a sus datos
 * (IDOR / acceso horizontal).
 *
 * Este filtro, DESPUÉS de que Spring Security validó el JWT:
 *   - si hay token: sobrescribe X-User-Id con el {@code sub} del token (id de Keycloak)
 *     y descarta cualquier valor que haya enviado el cliente.
 *   - si NO hay token (rutas públicas): elimina X-User-Id entrante (anti-spoofing).
 *
 * Además, si el cliente mandaba un X-User-Id distinto al del token, lo registra como WARN
 * (sirve para detectar si el frontend usaba otro identificador).
 */
@Slf4j
@Component
public class IdentityHeaderGlobalFilter implements GlobalFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String clientProvided = exchange.getRequest().getHeaders().getFirst(USER_ID_HEADER);

        return ReactiveSecurityContextHolder.getContext()
                .map(SecurityContext::getAuthentication)
                .filter(auth -> auth instanceof JwtAuthenticationToken)
                .map(auth -> ((JwtAuthenticationToken) auth).getToken().getSubject())
                .flatMap(sub -> {
                    if (clientProvided != null && !clientProvided.equals(sub)) {
                        log.warn("X-User-Id del cliente ('{}') NO coincide con el sub del token ('{}'). "
                                + "Se usa el del token.", clientProvided, sub);
                    }
                    return chain.filter(withUserId(exchange, sub));
                })
                .switchIfEmpty(Mono.defer(() -> {
                    // Ruta pública / sin token: nunca dejar pasar un X-User-Id puesto por el cliente.
                    if (clientProvided != null) {
                        log.warn("Petición sin token con X-User-Id del cliente ('{}'); se elimina.", clientProvided);
                    }
                    return chain.filter(stripUserId(exchange));
                }));
    }

    private ServerWebExchange withUserId(ServerWebExchange exchange, String userId) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.set(USER_ID_HEADER, userId))
                .build();
        return exchange.mutate().request(request).build();
    }

    private ServerWebExchange stripUserId(ServerWebExchange exchange) {
        ServerHttpRequest request = exchange.getRequest().mutate()
                .headers(h -> h.remove(USER_ID_HEADER))
                .build();
        return exchange.mutate().request(request).build();
    }

    @Override
    public int getOrder() {
        // Después de la cadena de seguridad (que puebla el SecurityContext) y antes del ruteo.
        return Ordered.HIGHEST_PRECEDENCE + 100;
    }
}
