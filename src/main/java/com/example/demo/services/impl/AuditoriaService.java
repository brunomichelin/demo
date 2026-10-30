package com.example.demo.services.impl;

import com.example.demo.models.Auditoria;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.AuditoriaRepository;
import com.example.demo.repositories.UsuarioRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class AuditoriaService {
    private final AuditoriaRepository auditoriaRepository;
    private final UsuarioRepository usuarioRepository;

    public AuditoriaService(AuditoriaRepository auditoriaRepository, UsuarioRepository usuarioRepository) {
        this.auditoriaRepository = auditoriaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    public void registrar(String entidade, Long entidadeId, String acao, String anterior, String novo) {
        Auditoria auditoria = new Auditoria();
        auditoria.setEntidade(entidade);
        auditoria.setEntidadeId(entidadeId);
        auditoria.setAcao(acao);
        auditoria.setValorAnterior(anterior);
        auditoria.setValorNovo(novo);
        auditoria.setDataHora(LocalDateTime.now());
        auditoria.setUsuario(usuarioAtual());
        auditoriaRepository.save(auditoria);
    }

    private Usuario usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }
}
