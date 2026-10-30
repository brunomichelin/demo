package com.example.demo.controllers;

import com.example.demo.dtos.CadastroRequest;
import com.example.demo.dtos.LoginRequest;
import com.example.demo.dtos.LoginResponse;
import com.example.demo.models.Usuario;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthenticationManager authenticationManager;

    @Autowired
    private TokenService tokenService;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @PostMapping("/register")
    public ResponseEntity<String> register(@RequestBody CadastroRequest request) {
        if (request == null
                || !StringUtils.hasText(request.getNome())
                || !StringUtils.hasText(request.getEmail())
                || !request.getEmail().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")
                || request.getSenha() == null
                || request.getSenha().length() < 8
                || request.getPapel() == null) {
            return ResponseEntity.badRequest()
                    .body("Informe nome, e-mail válido, papel e uma senha com pelo menos 8 caracteres.");
        }

        String email = request.getEmail().trim().toLowerCase(java.util.Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                    .body("Já existe uma conta cadastrada com este e-mail.");
        }

        Usuario usuario = new Usuario();
        usuario.setNome(request.getNome().trim());
        usuario.setEmail(email);
        usuario.setTelefone(request.getTelefone());
        usuario.setSenhaHash(passwordEncoder.encode(request.getSenha()));
        usuario.setPapel(request.getPapel());
        usuario.setStatusRegistro(StatusRegEnum.PENDENTE);
        usuarioRepository.save(usuario);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body("Cadastro recebido. O papel solicitado ficará disponível após aprovação do administrador.");
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(request.getEmail(), request.getSenha());
        Authentication auth = this.authenticationManager.authenticate(usernamePassword);
        String token = tokenService.gerarToken(auth.getName());
        String refreshToken = tokenService.gerarRefreshToken(auth.getName());
        ResponseCookie tokenCookie = ResponseCookie.from("access_token", token)
                .httpOnly(true)
                .sameSite("Strict")
                .path("/")
                .maxAge(2 * 60 * 60)
                .build();
        ResponseCookie refreshCookie = criarCookieRefresh(refreshToken);
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, tokenCookie.toString())
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(new LoginResponse(token));
    }

    @PostMapping("/refresh")
    public ResponseEntity<LoginResponse> refresh(HttpServletRequest request) {
        String refreshToken = obterCookie(request, "refresh_token");
        if (refreshToken == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        String email = tokenService.validarRefreshToken(refreshToken);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .filter(u -> u.getStatusRegistro() != StatusRegEnum.INATIVO
                        && u.getStatusRegistro() != StatusRegEnum.EXCLUIDO
                        && u.getStatusRegistro() != StatusRegEnum.PENDENTE)
                .orElseThrow(() -> new org.springframework.security.authentication.BadCredentialsException(
                        "Usuário inativo ou inexistente."));
        String token = tokenService.gerarToken(usuario.getEmail());
        String novoRefresh = tokenService.gerarRefreshToken(usuario.getEmail());
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, ResponseCookie.from("access_token", token)
                        .httpOnly(true).sameSite("Strict").path("/").maxAge(2 * 60 * 60).build().toString())
                .header(HttpHeaders.SET_COOKIE, criarCookieRefresh(novoRefresh).toString())
                .body(new LoginResponse(token));
    }

    private ResponseCookie criarCookieRefresh(String token) {
        return ResponseCookie.from("refresh_token", token)
                .httpOnly(true)
                .sameSite("Strict")
                .path("/auth/refresh")
                .maxAge(60L * 60 * 24 * 30)
                .build();
    }

    private String obterCookie(HttpServletRequest request, String nome) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (nome.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

}
