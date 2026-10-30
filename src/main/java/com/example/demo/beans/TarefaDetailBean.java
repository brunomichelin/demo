package com.example.demo.beans;

import com.example.demo.models.Tarefa;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.config.ObraAuthorization;
import com.example.demo.services.impl.TarefaService;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

@Component("tarefaDetailBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TarefaDetailBean {

    @Getter
    private Tarefa tarefa;

    @Getter
    @Setter
    private String tarefaId;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private ObraAuthorization obraAuthorization;

    @Autowired
    private TarefaService tarefaService;

    public String carregar() {
        try {
            tarefa = tarefaRepository.findById(Long.parseLong(tarefaId)).orElse(null);
            if (tarefa == null) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Tarefa não encontrada.");
                return "Tarefa?faces-redirect=true";
            }
            if (!obraAuthorization.podeAcessarTarefa(tarefa.getId(),
                    SecurityContextHolder.getContext().getAuthentication())) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não está vinculado a esta obra.");
                return "Tarefa?faces-redirect=true";
            }
        } catch (NumberFormatException | NullPointerException e) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Identificador da tarefa inválido.");
            return "Tarefa?faces-redirect=true";
        }
        return null;
    }

    public boolean isPodeAvancarStatus() {
        return isStatusManager() && tarefa != null
                && (tarefa.getStatus() == StatusCronEnum.PLANEJAMENTO
                || tarefa.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || tarefa.getStatus() == StatusCronEnum.EM_BLOQUEIO
                || tarefa.getStatus() == StatusCronEnum.PAUSADA);
    }

    public boolean isPodeBloquearStatus() {
        return isStatusManager() && tarefa != null
                && (tarefa.getStatus() == StatusCronEnum.PLANEJAMENTO
                || tarefa.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || tarefa.getStatus() == StatusCronEnum.PAUSADA);
    }

    public boolean isPodeCancelarStatus() {
        return isStatusManager() && tarefa != null
                && (tarefa.getStatus() == StatusCronEnum.PLANEJAMENTO
                || tarefa.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || tarefa.getStatus() == StatusCronEnum.EM_BLOQUEIO
                || tarefa.getStatus() == StatusCronEnum.PAUSADA);
    }

    public void avancarStatus() {
        if (tarefa == null || tarefa.getStatus() == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Não há um próximo status disponível.");
            return;
        }
        StatusCronEnum proximoStatus = switch (tarefa.getStatus()) {
            case PLANEJAMENTO -> StatusCronEnum.EM_ANDAMENTO;
            case EM_ANDAMENTO -> StatusCronEnum.CONCLUIDA;
            case EM_BLOQUEIO -> StatusCronEnum.EM_ANDAMENTO;
            case PAUSADA -> StatusCronEnum.EM_ANDAMENTO;
            case CONCLUIDA, CANCELADA -> null;
        };
        atualizarStatus(proximoStatus);
    }

    public void bloquearStatus() {
        atualizarStatus(StatusCronEnum.EM_BLOQUEIO);
    }

    public void cancelarStatus() {
        atualizarStatus(StatusCronEnum.CANCELADA);
    }

    private void atualizarStatus(StatusCronEnum novoStatus) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (tarefa == null || !isStatusManager()
                || !obraAuthorization.podeAcessarTarefa(tarefa.getId(), authentication)) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para alterar o status desta tarefa.");
            return;
        }
        if (novoStatus == null || novoStatus == StatusCronEnum.EM_BLOQUEIO && !isPodeBloquearStatus()
                || novoStatus == StatusCronEnum.CANCELADA && !isPodeCancelarStatus()
                || novoStatus != StatusCronEnum.EM_BLOQUEIO && novoStatus != StatusCronEnum.CANCELADA
                && !isPodeAvancarStatus()) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Não há uma transição de status disponível.");
            return;
        }
        try {
            tarefa = tarefaService.atualizarStatus(tarefa.getId(), novoStatus);
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    "Status da tarefa atualizado para " + novoStatus.getDescricao() + ".");
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR,
                    exception.getReason() == null ? "Não foi possível atualizar o status." : exception.getReason());
        }
    }

    private boolean isStatusManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> switch (authority.getAuthority()) {
                    case "ROLE_ADMIN", "ROLE_GESTOR", "ROLE_MESTRE_OBRA", "ROLE_TECNICO_OBRA" -> true;
                    default -> false;
                });
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
