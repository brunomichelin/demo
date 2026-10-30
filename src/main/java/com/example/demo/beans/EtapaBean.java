package com.example.demo.beans;

import com.example.demo.models.*;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.EtapaService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("etapaBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EtapaBean {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "ordem", "ordem",
            "obra.nome", "obra.nome",
            "nome", "nome",
            "descricao", "descricao",
            "dataInicioPrevista", "dataInicioPrevista",
            "dataEntregaPrevista", "dataEntregaPrevista",
            "status", "status"
    );

    @Getter
    private List<Etapa> etapas = new ArrayList<>();

    @Getter
    private List<Obra> obras = new ArrayList<>();

    @Getter
    private final StatusCronEnum[] statusDisponiveis = {
            StatusCronEnum.PLANEJAMENTO, StatusCronEnum.EM_ANDAMENTO,
            StatusCronEnum.EM_BLOQUEIO, StatusCronEnum.CONCLUIDA, StatusCronEnum.CANCELADA
    };

    @Getter
    @Setter
    private Etapa etapa;

    @Getter
    @Setter
    private Long obraSelecionadaId;

    @Getter
    private boolean visualizacao;

    private Sort sort = Sort.by(Sort.Direction.ASC, "ordem");

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EtapaService etapaService;

    @PostConstruct
    public void init() {
        obras = listarObrasAcessiveis();
        listarTodas();
        novaEtapa();
    }

    public void listarTodas() {
        if (isAdministrador()) {
            etapas = new ArrayList<>(etapaRepository.findAll(Pageable.unpaged(sort)).getContent());
            return;
        }
        List<Long> obraIds = obras.stream().map(Obra::getId).toList();
        etapas = obraIds.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(etapaRepository.findAllByObraIdIn(obraIds, Pageable.unpaged(sort)));
    }

    public void ordenar(SortEvent event) {
        SortMeta sortMeta = event.getSortBy().values().stream()
                .filter(meta -> !meta.getOrder().isUnsorted())
                .findFirst()
                .orElse(null);

        if (sortMeta == null) {
            return;
        }

        String field = SORTABLE_FIELDS.get(sortMeta.getField());
        if (field == null) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível ordenar essa coluna.");
            return;
        }

        Sort.Direction direction = sortMeta.getOrder() == SortOrder.DESCENDING
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        sort = Sort.by(direction, field);
        listarTodas();
    }

    public int getQuantidadeEtapas() {
        return etapas.size();
    }

    public void novaEtapa() {
        etapa = new Etapa();
        etapa.setStatus(StatusCronEnum.PLANEJAMENTO);
        obraSelecionadaId = null;
        visualizacao = false;
    }

    public void editarEtapa(Etapa etapaSelecionada) {
        prepararEdicao(etapaSelecionada, false);
    }

    public void visualizarEtapa(Etapa etapaSelecionada) {
        prepararEdicao(etapaSelecionada, true);
    }

    private void prepararEdicao(Etapa etapaSelecionada, boolean somenteLeitura) {
        etapa = etapaSelecionada;
        obraSelecionadaId = etapaSelecionada.getObra() == null
                ? null
                : etapaSelecionada.getObra().getId();
        visualizacao = somenteLeitura;
    }

    public void salvar() {
        try {
            etapaService.validarPodeConcluir(etapa, etapa.getStatus());
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, exception.getReason());
            return;
        }
        boolean nova = etapa.getId() == null;
        Obra obraSelecionada = obraRepository.findById(obraSelecionadaId).orElse(null);
        if (obraSelecionada == null) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione uma obra válida para a etapa.");
            return;
        }
        etapa.setObra(obraSelecionada);

        LocalDateTime agora = LocalDateTime.now();
        if (nova) {
            etapa.setDataCriacao(agora);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                etapa.setCriadoPor(authentication.getName());
            }
        } else {
            etapa.setDataAtualizacao(agora);
        }

        etapaRepository.save(etapa);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                nova ? "Etapa cadastrada com sucesso." : "Etapa atualizada com sucesso.");
    }

    public void avancarStatus(Etapa etapaSelecionada) {
        if (etapaSelecionada.getStatus() == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN,
                    "Não é possível avançar uma etapa sem status.");
            return;
        }
        StatusCronEnum proximoStatus = switch (etapaSelecionada.getStatus()) {
            case PLANEJAMENTO -> StatusCronEnum.EM_ANDAMENTO;
            case EM_ANDAMENTO -> StatusCronEnum.CONCLUIDA;
            case EM_BLOQUEIO, CONCLUIDA, CANCELADA, PAUSADA -> null;
        };
        if (proximoStatus == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN,
                    "Esta tarefa não possui um próximo status disponível.");
            return;
        }

        etapaSelecionada.setStatus(proximoStatus);
        etapaSelecionada.setDataAtualizacao(LocalDateTime.now());
        etapaRepository.save(etapaSelecionada);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                "Status da tarefa atualizado para " + proximoStatus.getDescricao() + ".");
    }

    public boolean podeAvancar(Etapa etapaSelecionada) {
        return etapaSelecionada.getStatus() == StatusCronEnum.PLANEJAMENTO
                || etapaSelecionada.getStatus() == StatusCronEnum.EM_ANDAMENTO;
    }

    public void excluir(Etapa etapaSelecionada) {
        etapaSelecionada.setDataDelecao(LocalDateTime.now());
        etapaSelecionada.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        etapaRepository.save(etapaSelecionada);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO, "Etapa excluída com sucesso.");
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

    private List<Obra> listarObrasAcessiveis() {
        if (isAdministrador()) {
            return obraRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(Usuario::getId)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        return obraRepository.findFiltered(null, null, null, null, usuarioId,
                Pageable.unpaged(Sort.by(Sort.Direction.ASC, "nome"))).getContent();
    }

    private boolean isAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }
}
