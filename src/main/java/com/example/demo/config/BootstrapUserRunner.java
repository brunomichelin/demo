package com.example.demo.config;

import com.example.demo.models.CargoEnum;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.UsuarioRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Locale;

@Component
public class BootstrapUserRunner implements ApplicationRunner {

    private final UsuarioRepository usuarioRepository;
    private final String email;
    private final String nome;
    private final String senhaHash;
    private final String papel;
    private final String telefone;

    public BootstrapUserRunner(
            UsuarioRepository usuarioRepository,
            @Value("${APP_BOOTSTRAP_USER_EMAIL:michelin.brunohenrique@hotmail.com}") String email,
            @Value("${APP_BOOTSTRAP_USER_NAME:Bruno Henrique Michelin Silva}") String nome,
            @Value("${APP_BOOTSTRAP_USER_PASSWORD_HASH:$2a$10$/BUyBIDr3MpXYl63Ba5IiOYBh9N5GL/UdS6w6/ju0ha9/flHv.Ubq}") String senhaHash,
            @Value("${APP_BOOTSTRAP_USER_ROLE:ADMIN}") String papel,
            @Value("${APP_BOOTSTRAP_USER_PHONE:11937361806}") String telefone
    ) {
        this.usuarioRepository = usuarioRepository;
        this.email = email;
        this.nome = nome;
        this.senhaHash = senhaHash;
        this.papel = papel;
        this.telefone = telefone;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!StringUtils.hasText(email)) {
            return;
        }
        validarConfiguracao();

        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
            return;
        }

        Usuario usuario = new Usuario();
        usuario.setEmail(emailNormalizado);
        usuario.setNome(nome.trim());
        usuario.setSenhaHash(senhaHash);
        usuario.setPapel(CargoEnum.valueOf(papel.trim().toUpperCase(Locale.ROOT)));
        usuario.setTelefone(StringUtils.hasText(telefone) ? telefone.trim() : null);
        usuario.setStatusRegistro(StatusRegEnum.ATIVO);
        usuario.setDataCriacao(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    private void validarConfiguracao() {
        if (!StringUtils.hasText(nome) || !StringUtils.hasText(senhaHash) || !StringUtils.hasText(papel)) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_USER_NAME, APP_BOOTSTRAP_USER_PASSWORD_HASH e APP_BOOTSTRAP_USER_ROLE "
                            + "são obrigatórias quando APP_BOOTSTRAP_USER_EMAIL está configurada.");
        }
        if (!email.trim().matches("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")) {
            throw new IllegalStateException("APP_BOOTSTRAP_USER_EMAIL deve conter um e-mail válido.");
        }
        if (!senhaHash.matches("^\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}$")) {
            throw new IllegalStateException("APP_BOOTSTRAP_USER_PASSWORD_HASH deve conter um hash BCrypt válido.");
        }
        try {
            CargoEnum.valueOf(papel.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            throw new IllegalStateException(
                    "APP_BOOTSTRAP_USER_ROLE deve corresponder a um papel válido da aplicação.", exception);
        }
    }
}
