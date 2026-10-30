package com.example.demo.config;

import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.TokenService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    public SecurityConfig(TokenService tokenService, UsuarioRepository usuarioRepository) {
        this.jwtAuthenticationFilter = new JwtAuthenticationFilter(tokenService, usuarioRepository);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            AuthenticationProvider usuarioAuthenticationProvider
    ) throws Exception {
        return http
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED))
                .authorizeHttpRequests(authorize -> authorize
                        .requestMatchers(HttpMethod.POST, "/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/register").permitAll()
                        .requestMatchers(HttpMethod.POST, "/auth/refresh").permitAll()
                        .requestMatchers("/obras.xhtml", "/detailObra.xhtml")
                        .hasAnyRole("ADMIN", "GESTOR")
                        .requestMatchers("/Etapa.xhtml", "/detailEtapa.xhtml", "/Tarefa.xhtml", "/detailTarefa.xhtml")
                        .hasAnyRole("ADMIN", "GESTOR", "MESTRE_OBRA", "TECNICO_OBRA")
                        .requestMatchers("/orcamento.xhtml").hasAnyRole("ADMIN", "GESTOR", "ORCAMENTISTA")
                        .requestMatchers("/custos.xhtml").hasAnyRole("ADMIN", "GESTOR", "MESTRE_OBRA",
                                "TECNICO_OBRA", "FINANCEIRO")
                        .requestMatchers("/entregas.xhtml").hasAnyRole("ADMIN", "GESTOR", "MESTRE_OBRA",
                                "TECNICO_OBRA", "CLIENTE")
                        .requestMatchers("/relatorios.xhtml").hasAnyRole("ADMIN", "GESTOR", "FINANCEIRO",
                                "ORCAMENTISTA", "MESTRE_OBRA", "TECNICO_OBRA", "CLIENTE")
                        .requestMatchers("/desempenho.xhtml").hasAnyRole("ADMIN", "GESTOR", "MESTRE_OBRA",
                                "TECNICO_OBRA", "CLIENTE")
                        .requestMatchers("/usuarios.xhtml", "/auditoria.xhtml").hasRole("ADMIN")
                        .requestMatchers("/gestao.xhtml").hasAnyRole("ADMIN", "GESTOR", "ENGENHEIRO",
                                "ORCAMENTISTA", "MESTRE_OBRA", "TECNICO_OBRA", "FINANCEIRO", "CLIENTE")
                        .requestMatchers("/Insumo.xhtml").hasAnyRole("ADMIN", "ORCAMENTISTA")
                        .requestMatchers("/Fornecedor.xhtml").hasAnyRole("ADMIN", "ORCAMENTISTA", "FINANCEIRO")
                        .requestMatchers("/alunos.xhtml").hasRole("ADMIN")
                        .requestMatchers("/login.xhtml", "/cadastro.xhtml", "/jakarta.faces.resource/**", "/javax.faces.resource/**").permitAll()
                        .anyRequest().authenticated()
                )
                .authenticationProvider(usuarioAuthenticationProvider)
                .exceptionHandling(exceptions -> exceptions.authenticationEntryPoint((request, response, exception) -> {
                    String path = request.getRequestURI().substring(request.getContextPath().length());
                    if (path.endsWith(".xhtml") || path.endsWith(".jr")) {
                        String destination = request.getContextPath() + path;
                        if (request.getQueryString() != null) {
                            destination += "?" + request.getQueryString();
                        }
                        String redirect = "/login.xhtml?redirect="
                                + URLEncoder.encode(destination, StandardCharsets.UTF_8);
                        response.sendRedirect(request.getContextPath() + redirect);
                    } else {
                        response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Token ausente ou inválido");
                    }
                }))
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    public AuthenticationProvider usuarioAuthenticationProvider(
            UsuarioRepository usuarioRepository,
            PasswordEncoder passwordEncoder
    ) {
        return new UsuarioAuthenticationProvider(usuarioRepository, passwordEncoder);
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationProvider usuarioAuthenticationProvider) {
        return new ProviderManager(usuarioAuthenticationProvider);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

}
