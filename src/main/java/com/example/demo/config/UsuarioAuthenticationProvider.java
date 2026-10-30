package com.example.demo.config;

import com.example.demo.models.Usuario;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.repositories.UsuarioRepository;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Locale;

public class UsuarioAuthenticationProvider implements AuthenticationProvider {

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioAuthenticationProvider(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Authentication authenticate(Authentication authentication) {
        String email = authentication.getName().trim().toLowerCase(Locale.ROOT);
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new BadCredentialsException("E-mail ou senha inválidos."));
        if (usuario.getStatusRegistro() == StatusRegEnum.INATIVO
                || usuario.getStatusRegistro() == StatusRegEnum.EXCLUIDO
                || usuario.getStatusRegistro() == StatusRegEnum.PENDENTE) {
            throw new BadCredentialsException("Usuário inativo.");
        }

        String senha = authentication.getCredentials() == null
                ? ""
                : authentication.getCredentials().toString();
        if (usuario.getSenhaHash() == null || !passwordEncoder.matches(senha, usuario.getSenhaHash())) {
            throw new BadCredentialsException("E-mail ou senha inválidos.");
        }

        List<SimpleGrantedAuthority> authorities = usuario.getPapel() == null
                ? List.of()
                : List.of(new SimpleGrantedAuthority("ROLE_" + usuario.getPapel().name()));
        return UsernamePasswordAuthenticationToken.authenticated(usuario.getEmail(), null, authorities);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return UsernamePasswordAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
