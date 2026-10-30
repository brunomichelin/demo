
package com.example.demo.services.impl;

import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;

@Service
public class TokenService {

    @Value("${api.security.token.secret:${API_SECURITY_TOKEN_SECRET:}}")
    private String secret;

    private Key getSigningKey() {
        if (secret == null || secret.getBytes(java.nio.charset.StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("Configure API_SECURITY_TOKEN_SECRET com pelo menos 32 bytes.");
        }
        return Keys.hmacShaKeyFor(secret.getBytes(java.nio.charset.StandardCharsets.UTF_8));
    }

    public String gerarToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuer("auth-api")
                .claim("tipo", "acesso")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 2)) // 2 horas
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String gerarRefreshToken(String email) {
        return Jwts.builder()
                .setSubject(email)
                .setIssuer("auth-api")
                .claim("tipo", "renovacao")
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + 1000L * 60 * 60 * 24 * 30))
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String validarToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer("auth-api")
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!"acesso".equals(claims.get("tipo", String.class))) {
            throw new MalformedJwtException("Token de acesso inválido.");
        }
        return claims.getSubject();
    }

    public String validarRefreshToken(String token) {
        var claims = Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .requireIssuer("auth-api")
                .build()
                .parseClaimsJws(token)
                .getBody();
        if (!"renovacao".equals(claims.get("tipo", String.class))) {
            throw new MalformedJwtException("Token de renovação inválido.");
        }
        return claims.getSubject();
    }
}
