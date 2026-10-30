package com.example.demo.beans;

import com.example.demo.dtos.TarefaRequest;
import com.example.demo.models.Etapa;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.Tarefa;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.config.ObraAuthorization;
import com.example.demo.services.impl.TarefaService;
import com.example.demo.services.impl.EtapaService;
import jakarta.annotation.PostConstruct;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("etapaDetailBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EtapaDetailBean {

    @Getter
    private Etapa etapa;

    @Getter
    private List<Tarefa> tarefas = new ArrayList<>();

    @Getter
    @Setter
    private Tarefa novaTarefa;

    @Getter
    @Setter
    private Long responsavelSelecionadoId;

    @Getter
    private List<Usuario> usuarios = new ArrayList<>();

    @Getter
    private final StatusCronEnum[] statusTarefaDisponiveis = {
            StatusCronEnum.PLANEJAMENTO, StatusCronEnum.EM_ANDAMENTO,
            StatusCronEnum.EM_BLOQUEIO, StatusCronEnum.CONCLUIDA
    };

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private TarefaService tarefaService;

    @Autowired
    private EtapaService etapaService;

    @Autowired
    private ObraAuthorization obraAuthorization;

    @PostConstruct
    public void iniciar() {
        prepararNovaTarefa();
    }

    public String carregar() {
        Map<String, String> parametros = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap();
        try {
            long etapaId = Long.parseLong(parametros.get("id"));
            etapa = etapaRepository.findById(etapaId).orElse(null);
            if (etapa == null) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Etapa não encontrada.");
                return "Etapa?faces-redirect=true";
            }
            if (!obraAuthorization.podeAcessarEtapa(etapaId,
                    SecurityContextHolder.getContext().getAuthentication())) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não está vinculado a esta obra.");
                return "Etapa?faces-redirect=true";
            }
            usuarios = usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        } catch (NumberFormatException | NullPointerException e) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Identificador da etapa inválido.");
            return "Etapa?faces-redirect=true";
        }

        Page<Tarefa> tarefasDaEtapa = tarefaRepository.findAllByEtapaIdOrderByPrioridade(
                etapa.getId(), Pageable.unpaged());
        tarefas = tarefasDaEtapa.getContent();
        return null;
    }

    public void prepararNovaTarefa() {
        novaTarefa = new Tarefa();
        novaTarefa.setStatus(StatusCronEnum.PLANEJAMENTO);
        responsavelSelecionadoId = null;
    }

    public void salvarTarefa() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (etapa == null || !podeCriarTarefas(authentication)
                || !obraAuthorization.podeAcessarEtapa(etapa.getId(), authentication)) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para criar tarefas nesta etapa.");
            return;
        }
        try {
            tarefaService.criar(etapa.getId(), new TarefaRequest(etapa.getId(), novaTarefa.getTitulo(),
                    novaTarefa.getDescricao(), responsavelSelecionadoId, novaTarefa.getDataInicioPrevista(),
                    novaTarefa.getDataFimPrevista(), novaTarefa.getPrioridade(), novaTarefa.getStatus(), null));
            Page<Tarefa> tarefasDaEtapa = tarefaRepository.findAllByEtapaIdOrderByPrioridade(
                    etapa.getId(), Pageable.unpaged());
            tarefas = tarefasDaEtapa.getContent();
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Tarefa cadastrada com sucesso.");
            prepararNovaTarefa();
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR,
                    exception.getReason() == null ? "Não foi possível criar a tarefa." : exception.getReason());
        }
    }

    public boolean isPodeAvancarStatus() {
        return isStatusManager() && etapa != null
                && (etapa.getStatus() == StatusCronEnum.PLANEJAMENTO
                || etapa.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || etapa.getStatus() == StatusCronEnum.EM_BLOQUEIO
                || etapa.getStatus() == StatusCronEnum.PAUSADA);
    }

    public boolean isPodeBloquearStatus() {
        return isStatusManager() && etapa != null
                && (etapa.getStatus() == StatusCronEnum.PLANEJAMENTO
                || etapa.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || etapa.getStatus() == StatusCronEnum.PAUSADA);
    }

    public boolean isPodeCancelarStatus() {
        return isStatusManager() && etapa != null
                && (etapa.getStatus() == StatusCronEnum.PLANEJAMENTO
                || etapa.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || etapa.getStatus() == StatusCronEnum.EM_BLOQUEIO
                || etapa.getStatus() == StatusCronEnum.PAUSADA);
    }

    public void avancarStatus() {
        if (etapa == null || etapa.getStatus() == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Não há um próximo status disponível.");
            return;
        }
        StatusCronEnum proximoStatus = switch (etapa.getStatus()) {
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
        if (etapa == null || !isStatusManager()
                || !obraAuthorization.podeAcessarEtapa(etapa.getId(), authentication)) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para alterar o status desta etapa.");
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
            etapa = etapaService.atualizarStatus(etapa.getId(), novoStatus);
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    "Status da etapa atualizado para " + novoStatus.getDescricao() + ".");
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR,
                    exception.getReason() == null ? "Não foi possível atualizar o status." : exception.getReason());
        }
    }

    public int getQuantidadeTarefas() {
        return tarefas.size();
    }

    public long getQuantidadeConcluidas() {
        return tarefas.stream()
                .filter(tarefa -> tarefa.getStatus() == StatusCronEnum.CONCLUIDA)
                .count();
    }

    public long getQuantidadeEmAndamento() {
        return tarefas.stream()
                .filter(tarefa -> tarefa.getStatus() == StatusCronEnum.EM_ANDAMENTO)
                .count();
    }

    public BigDecimal getPercentualConcluidas() {
        return calcularPercentual(getQuantidadeConcluidas());
    }

    public BigDecimal getPercentualEmAndamento() {
        return calcularPercentual(getQuantidadeEmAndamento());
    }

    private BigDecimal calcularPercentual(long quantidade) {
        if (tarefas.isEmpty()) {
            return BigDecimal.ZERO.setScale(1);
        }
        return BigDecimal.valueOf(quantidade)
                .multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(tarefas.size()), 1, RoundingMode.HALF_UP);
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

    private boolean podeCriarTarefas(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> switch (authority.getAuthority()) {
                    case "ROLE_ADMIN", "ROLE_GESTOR", "ROLE_MESTRE_OBRA", "ROLE_TECNICO_OBRA" -> true;
                    default -> false;
                });
    }

    private boolean isStatusManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_GESTOR".equals(authority.getAuthority()));
    }
}
