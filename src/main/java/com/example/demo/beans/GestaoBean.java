package com.example.demo.beans;

import com.example.demo.models.Obra;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;

@Component("gestaoBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class GestaoBean {
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private List<Obra> obras = new ArrayList<>();
    private String mensagem;
    private boolean podeDetalhes;
    private boolean podeOrcamento;
    private boolean podeCustos;
    private boolean podeEntregas;
    private boolean podeRelatorios;
    private boolean podeDesempenho;

    public GestaoBean(ObraRepository obraRepository, UsuarioRepository usuarioRepository) {
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostConstruct
    public void iniciar() {
        Authentication authentication = autenticacao();
        podeDetalhes = temPerfil(authentication, "ADMIN", "GESTOR");
        podeOrcamento = temPerfil(authentication, "ADMIN", "GESTOR", "ORCAMENTISTA");
        podeCustos = temPerfil(authentication, "ADMIN", "GESTOR", "MESTRE_OBRA", "TECNICO_OBRA", "FINANCEIRO");
        podeEntregas = temPerfil(authentication, "ADMIN", "GESTOR", "MESTRE_OBRA", "TECNICO_OBRA", "CLIENTE");
        podeRelatorios = temPerfil(authentication, "ADMIN", "GESTOR", "FINANCEIRO", "ORCAMENTISTA",
                "MESTRE_OBRA", "TECNICO_OBRA", "CLIENTE");
        podeDesempenho = temPerfil(authentication, "ADMIN", "GESTOR", "MESTRE_OBRA", "TECNICO_OBRA", "CLIENTE");

        Long usuarioId = null;
        if (!temPerfil(authentication, "ADMIN")) {
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
            usuarioId = usuario.getId();
        }
        obras = obraRepository.findFiltered(null, null, null, null, usuarioId,
                PageRequest.of(0, 100, Sort.by(Sort.Direction.ASC, "nome"))).getContent();

        if (obras.isEmpty()) mensagem = "Você ainda não está vinculado a nenhuma obra.";
    }

    private Authentication autenticacao() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalStateException("Não foi possível identificar o usuário autenticado.");
        }
        return authentication;
    }

    private boolean temPerfil(Authentication authentication, String... papeis) {
        for (String papel : papeis) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + papel))) return true;
        }
        return false;
    }

    public List<Obra> getObras() { return obras; }
    public String getMensagem() { return mensagem; }
    public boolean isPodeDetalhes() { return podeDetalhes; }
    public boolean isPodeOrcamento() { return podeOrcamento; }
    public boolean isPodeCustos() { return podeCustos; }
    public boolean isPodeEntregas() { return podeEntregas; }
    public boolean isPodeRelatorios() { return podeRelatorios; }
    public boolean isPodeDesempenho() { return podeDesempenho; }
}
