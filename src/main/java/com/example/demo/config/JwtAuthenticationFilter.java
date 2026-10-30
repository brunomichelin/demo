package com.example.demo.config;

import com.example.demo.services.impl.TokenService;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.models.StatusRegEnum;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String TOKEN_COOKIE = "access_token";

    private final TokenService tokenService;
    private final UsuarioRepository usuarioRepository;

    public JwtAuthenticationFilter(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.tokenService = tokenService;
        this.usuarioRepository = usuarioRepository;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String token = obterToken(request);
        if (token != null) {
            try {
                String email = tokenService.validarToken(token);
                if (email != null && !email.isBlank()) {
                    usuarioRepository.findByEmailIgnoreCase(email)
                            .filter(usuario -> usuario.getStatusRegistro() != StatusRegEnum.INATIVO
                                    && usuario.getStatusRegistro() != StatusRegEnum.EXCLUIDO
                                    && usuario.getStatusRegistro() != StatusRegEnum.PENDENTE)
                            .ifPresent(usuario -> {
                        List<org.springframework.security.core.authority.SimpleGrantedAuthority> authorities =
                                usuario.getPapel() == null ? List.of()
                                        : List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority(
                                        "ROLE_" + usuario.getPapel().name()));
                        var authentication = new UsernamePasswordAuthenticationToken(email, null, authorities);
                        authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                        SecurityContextHolder.getContext().setAuthentication(authentication);
                    });
                }
            } catch (JwtException | IllegalArgumentException exception) {
                SecurityContextHolder.clearContext();
            }
        }

        filterChain.doFilter(request, response);
    }

    private String obterToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (TOKEN_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }
}
