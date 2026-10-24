package com.manh.job.gateway.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;

@Component
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private static final String TOKEN_PREFIX = "Bearer ";
    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";
    private static final String USER_EMAIL_HEADER = "X-User-Email";

    private final SecretKey secretKey;

    public JwtAuthGlobalFilter(@Value("${jwt.secret}") String jwtSecret) {
        this.secretKey = Keys.hmacShaKeyFor(jwtSecret.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();
        HttpMethod method = request.getMethod();

        // Never trust client-provided identity headers
        ServerHttpRequest.Builder mutate = request.mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove(USER_EMAIL_HEADER);
                });

        if (isPublic(path, method)) {
            return chain.filter(exchange.mutate().request(mutate.build()).build());
        }

        String authHeader = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authHeader == null || !authHeader.startsWith(TOKEN_PREFIX)) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            String token = authHeader.substring(TOKEN_PREFIX.length());
            Claims claims = Jwts.parser()
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            Object userId = claims.get("userId");
            Object authorities = claims.get("authorities");
            Object email = claims.get("email");

            if (userId != null) {
                mutate.header(USER_ID_HEADER, String.valueOf(userId));
            }
            if (authorities != null) {
                mutate.header(USER_ROLE_HEADER, String.valueOf(authorities));
            }
            if (email != null) {
                mutate.header(USER_EMAIL_HEADER, String.valueOf(email));
            }

            return chain.filter(exchange.mutate().request(mutate.build()).build());
        } catch (Exception ex) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private boolean isPublic(String path, HttpMethod method) {
        if (path.startsWith("/auth/")) {
            return true;
        }

        if (method == HttpMethod.GET) {
            if (pathEquals(path, "/api/jobs") || path.matches("/api/jobs/\\d+")
                    || path.startsWith("/api/jobs/company/")) {
                return true;
            }
            if (pathEquals(path, "/api/companies") || path.matches("/api/companies/\\d+")) {
                return true;
            }
            if (path.startsWith("/api/job-categories")
                    || path.startsWith("/api/job-skills")
                    || path.startsWith("/api/job-tags")) {
                return true;
            }
            if (path.matches("/api/users/\\d+")) {
                return true;
            }
        }
        return false;
    }

    private boolean pathEquals(String path, String expected) {
        return expected.equals(path) || (expected + "/").equals(path);
    }

    @Override
    public int getOrder() {
        return -100;
    }
}
