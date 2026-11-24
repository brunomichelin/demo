package com.example.demo.beans;

import com.example.demo.models.Auditoria;
import com.example.demo.repositories.AuditoriaRepository;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;

@Component("auditoriaBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class AuditoriaBean {
    private static final int TAMANHO_PAGINA = 50;
    private final AuditoriaRepository auditoriaRepository;
    private List<Auditoria> eventos = List.of();
    private int pagina;
    private int totalPaginas;
    private String mensagem;

    public AuditoriaBean(AuditoriaRepository auditoriaRepository) {
        this.auditoriaRepository = auditoriaRepository;
    }

    @PostConstruct
    public void iniciar() {
        carregar();
    }

    public void anterior() {
        if (pagina > 0) {
            pagina--;
            carregar();
        }
    }

    public void proxima() {
        if (pagina + 1 < totalPaginas) {
            pagina++;
            carregar();
        }
    }

    private void carregar() {
        if (!exigirAdministrador()) {
            eventos = List.of();
            return;
        }
        Page<Auditoria> resultado = auditoriaRepository.findAllByOrderByDataHoraDesc(
                PageRequest.of(pagina, TAMANHO_PAGINA));
        eventos = resultado.getContent();
        totalPaginas = resultado.getTotalPages();
        mensagem = null;
    }

    private boolean exigirAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean permitido = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!permitido) {
            mensagem = "Acesso restrito a administradores.";
            FacesContext.getCurrentInstance().addMessage(null,
                    new FacesMessage(FacesMessage.SEVERITY_ERROR, mensagem, null));
        }
        return permitido;
    }

    public List<Auditoria> getEventos() { return eventos; }
    public int getPagina() { return pagina; }
    public int getTotalPaginas() { return totalPaginas; }
    public String getMensagem() { return mensagem; }
    public boolean isTemAnterior() { return pagina > 0; }
    public boolean isTemProxima() { return pagina + 1 < totalPaginas; }
}
