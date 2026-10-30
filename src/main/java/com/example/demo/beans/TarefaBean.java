package com.example.demo.beans;

import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import com.example.demo.models.Obra;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Tarefa;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.repositories.UsuarioRepository;
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

@Component("tarefaBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class TarefaBean {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "etapa.nome", "etapa.nome",
            "titulo", "titulo",
            "descricao", "descricao",
            "dataInicioPrevista", "dataInicioPrevista",
            "dataFimPrevista", "dataFimPrevista",
            "prioridade", "prioridade",
            "status", "status"
    );

    @Getter
    private List<Tarefa> tarefas = new ArrayList<>();

    @Getter
    private List<Etapa> etapas = new ArrayList<>();

    @Getter
    private List<Usuario> usuarios = new ArrayList<>();

    @Getter
    private final StatusCronEnum[] statusDisponiveis = {
            StatusCronEnum.PLANEJAMENTO, StatusCronEnum.EM_ANDAMENTO,
            StatusCronEnum.EM_BLOQUEIO, StatusCronEnum.CONCLUIDA
    };

    @Getter
    @Setter
    private Tarefa tarefa;

    @Getter
    @Setter
    private Long etapaSelecionadaId;

    @Getter
    @Setter
    private Long responsavelSelecionadoId;

    @Getter
    private boolean visualizacao;

    private Sort sort = Sort.by(Sort.Direction.ASC, "prioridade");

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObraRepository obraRepository;

    @PostConstruct
    public void init() {
        etapas = listarEtapasAcessiveis();
        usuarios = usuarioRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        listarTodas();
        novaTarefa();
    }

    public void listarTodas() {
        if (isAdministrador()) {
            tarefas = new ArrayList<>(tarefaRepository.findAll(Pageable.unpaged(sort)).getContent());
            return;
        }
        List<Long> etapaIds = etapas.stream().map(Etapa::getId).toList();
        tarefas = etapaIds.isEmpty() ? new ArrayList<>()
                : new ArrayList<>(tarefaRepository.findAllByEtapaIdIn(etapaIds, Pageable.unpaged(sort)));
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

    public int getQuantidadeTarefas() {
        return tarefas.size();
    }

    public void novaTarefa() {
        tarefa = new Tarefa();
        tarefa.setStatus(StatusCronEnum.PLANEJAMENTO);
        etapaSelecionadaId = null;
        responsavelSelecionadoId = null;
        visualizacao = false;
    }

    public void editarTarefa(Tarefa tarefaSelecionada) {
        prepararEdicao(tarefaSelecionada, false);
    }

    public void visualizarTarefa(Tarefa tarefaSelecionada) {
        prepararEdicao(tarefaSelecionada, true);
    }

    private void prepararEdicao(Tarefa tarefaSelecionada, boolean somenteLeitura) {
        tarefa = tarefaSelecionada;
        etapaSelecionadaId = tarefaSelecionada.getEtapa() == null
                ? null
                : tarefaSelecionada.getEtapa().getId();
        responsavelSelecionadoId = tarefaSelecionada.getResponsavel() == null
                ? null
                : tarefaSelecionada.getResponsavel().getId();
        visualizacao = somenteLeitura;
    }

    public void salvar() {
        boolean nova = tarefa.getId() == null;
        Etapa etapaSelecionada = etapaRepository.findById(etapaSelecionadaId).orElse(null);
        if (etapaSelecionada == null) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione uma etapa válida para a tarefa.");
            return;
        }
        tarefa.setEtapa(etapaSelecionada);

        Usuario responsavel = null;
        if (responsavelSelecionadoId != null) {
            responsavel = usuarioRepository.findById(responsavelSelecionadoId).orElse(null);
            if (responsavel == null) {
                FacesContext.getCurrentInstance().validationFailed();
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione um responsável válido.");
                return;
            }
        }
        tarefa.setResponsavel(responsavel);

        LocalDateTime agora = LocalDateTime.now();
        if (nova) {
            tarefa.setDataCriacao(agora);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                tarefa.setCriadoPor(authentication.getName());
            }
        } else {
            tarefa.setDataAtualizacao(agora);
        }

        tarefaRepository.save(tarefa);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                nova ? "Tarefa cadastrada com sucesso." : "Tarefa atualizada com sucesso.");
    }

    public void avancarStatus(Tarefa tarefaSelecionada) {
        if (tarefaSelecionada.getStatus() == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN,
                    "Não é possível avançar uma tarefa sem status.");
            return;
        }
        StatusCronEnum proximoStatus = switch (tarefaSelecionada.getStatus()) {
            case PLANEJAMENTO -> StatusCronEnum.EM_ANDAMENTO;
            case EM_ANDAMENTO -> StatusCronEnum.CONCLUIDA;
            case EM_BLOQUEIO, CONCLUIDA, CANCELADA, PAUSADA -> null;
        };
        if (proximoStatus == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN,
                    "Esta tarefa não possui um próximo status disponível.");
            return;
        }

        tarefaSelecionada.setStatus(proximoStatus);
        tarefaSelecionada.setDataAtualizacao(LocalDateTime.now());
        tarefaRepository.save(tarefaSelecionada);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                "Status da tarefa atualizado para " + proximoStatus.getDescricao() + ".");
    }

    public boolean podeAvancar(Tarefa tarefaSelecionada) {
        return tarefaSelecionada.getStatus() == StatusCronEnum.PLANEJAMENTO
                || tarefaSelecionada.getStatus() == StatusCronEnum.EM_ANDAMENTO;
    }

    public void excluir(Tarefa tarefaSelecionada) {
        tarefaSelecionada.setDataDelecao(LocalDateTime.now());
        tarefaSelecionada.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        tarefaRepository.save(tarefaSelecionada);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO, "Tarefa excluída com sucesso.");
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

    private List<Etapa> listarEtapasAcessiveis() {
        if (isAdministrador()) {
            return etapaRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(Usuario::getId)
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        List<Long> obraIds = obraRepository.findFiltered(null, null, null, null, usuarioId,
                Pageable.unpaged()).getContent().stream().map(Obra::getId).toList();
        return obraIds.isEmpty() ? new ArrayList<>()
                : etapaRepository.findAllByObraIdIn(obraIds, Pageable.unpaged(Sort.by(Sort.Direction.ASC, "nome")));
    }

    private boolean isAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

}
