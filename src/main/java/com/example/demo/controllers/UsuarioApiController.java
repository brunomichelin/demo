package com.example.demo.controllers;

import com.example.demo.dtos.UsuarioRequest;
import com.example.demo.dtos.UsuarioResponse;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.UsuarioRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.regex.Pattern;

@RestController
@RequestMapping("/api/v1/usuarios")
@PreAuthorize("hasRole('ADMIN')")
public class UsuarioApiController {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");
    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;

    public UsuarioApiController(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public Page<UsuarioResponse> listar(Pageable pageable) {
        return usuarioRepository.findAll(pageable).map(this::response);
    }

    @GetMapping("/{id}")
    @Transactional(readOnly = true)
    public UsuarioResponse buscar(@PathVariable Long id) {
        return response(obter(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public UsuarioResponse criar(@RequestBody UsuarioRequest request) {
        validar(request, true);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (usuarioRepository.existsByEmailIgnoreCase(email)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
        }
        Usuario usuario = new Usuario();
        preencher(usuario, request, true);
        usuario.setDataCriacao(LocalDateTime.now());
        usuario.setStatusRegistro(StatusRegEnum.ATIVO);
        return response(usuarioRepository.save(usuario));
    }

    @PutMapping("/{id}")
    @Transactional
    public UsuarioResponse atualizar(@PathVariable Long id, @RequestBody UsuarioRequest request) {
        validar(request, false);
        Usuario usuario = obter(id);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        usuarioRepository.findByEmailIgnoreCase(email)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new ResponseStatusException(HttpStatus.CONFLICT, "Este e-mail já está cadastrado.");
                });
        preencher(usuario, request, false);
        usuario.setDataAtualizacao(LocalDateTime.now());
        return response(usuarioRepository.save(usuario));
    }

    @PatchMapping("/{id}/aprovar")
    @Transactional
    public UsuarioResponse aprovar(@PathVariable Long id) {
        Usuario usuario = obter(id);
        if (usuario.getStatusRegistro() != StatusRegEnum.PENDENTE) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Somente cadastros pendentes podem ser aprovados.");
        }
        usuario.setStatusRegistro(StatusRegEnum.ATIVO);
        usuario.setDataAtualizacao(LocalDateTime.now());
        return response(usuarioRepository.save(usuario));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void excluir(@PathVariable Long id) {
        Usuario usuario = obter(id);
        usuario.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        usuario.setDataDelecao(LocalDateTime.now());
        usuario.setDataAtualizacao(LocalDateTime.now());
        usuarioRepository.save(usuario);
    }

    private void preencher(Usuario usuario, UsuarioRequest request, boolean novo) {
        usuario.setNome(request.nome().trim());
        usuario.setEmail(request.email().trim().toLowerCase(Locale.ROOT));
        usuario.setTelefone(request.telefone());
        usuario.setPapel(request.papel());
        if (StringUtils.hasText(request.senha())) {
            usuario.setSenhaHash(passwordEncoder.encode(request.senha()));
        } else if (novo) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma senha com pelo menos 8 caracteres.");
        }
    }

    private void validar(UsuarioRequest request, boolean novo) {
        if (request == null || !StringUtils.hasText(request.nome()) || !StringUtils.hasText(request.email())
                || !EMAIL.matcher(request.email().trim()).matches() || request.papel() == null
                || (novo && (!StringUtils.hasText(request.senha()) || request.senha().length() < 8))
                || (!novo && StringUtils.hasText(request.senha()) && request.senha().length() < 8)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe nome, e-mail válido, papel e senha de pelo menos 8 caracteres.");
        }
    }

    private Usuario obter(Long id) {
        return usuarioRepository.findById(id)
                .filter(usuario -> usuario.getStatusRegistro() != StatusRegEnum.EXCLUIDO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
    }

    private UsuarioResponse response(Usuario usuario) {
        return new UsuarioResponse(usuario.getId(), usuario.getNome(), usuario.getEmail(), usuario.getTelefone(),
                usuario.getPapel(), usuario.getStatusRegistro());
    }
}
