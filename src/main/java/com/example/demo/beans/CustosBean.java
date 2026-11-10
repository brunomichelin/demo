package com.example.demo.beans;

import com.example.demo.dtos.EvidenciaPagamentoResponse;
import com.example.demo.dtos.LancamentoCustoRequest;
import com.example.demo.dtos.ValidacaoLancamentoRequest;
import com.example.demo.models.Etapa;
import com.example.demo.models.Insumo;
import com.example.demo.models.LancamentoCusto;
import com.example.demo.models.Obra;
import com.example.demo.models.StatusLancamentoEnum;
import com.example.demo.models.Tarefa;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.InsumoRepository;
import com.example.demo.repositories.LancamentoCustoRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.LancamentoCustoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.FileUploadEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component("custosBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class CustosBean {

    @Getter
    private List<Obra> obras = new ArrayList<>();

    @Getter
    private List<Etapa> etapas = new ArrayList<>();

    @Getter
    private List<Tarefa> tarefas = new ArrayList<>();

    @Getter
    private List<Insumo> insumos = new ArrayList<>();

    @Getter
    private List<LancamentoCusto> lancamentos = new ArrayList<>();

    @Getter
    private List<EvidenciaPagamentoResponse> evidencias = new ArrayList<>();

    @Getter
    @Setter
    private Long obraSelecionadaId;

    @Getter
    @Setter
    private Long etapaSelecionadaId;

    @Getter
    @Setter
    private Long tarefaSelecionadaId;

    @Getter
    @Setter
    private Long insumoSelecionadoId;

    @Getter
    @Setter
    private BigDecimal quantidade;

    @Getter
    @Setter
    private BigDecimal valorUnitario;

    @Getter
    @Setter
    private LocalDate dataLancamento;

    @Getter
    @Setter
    private String numeroDocumento;

    @Getter
    @Setter
    private String justificativa;

    @Getter
    private LancamentoCusto lancamentoSelecionado;

    @Getter
    private boolean podeRegistrar;

    @Getter
    private boolean podeValidar;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private LancamentoCustoRepository lancamentoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private LancamentoCustoService lancamentoService;

    @PostConstruct
    public void init() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        podeRegistrar = temQualquerPerfil(authentication, "ADMIN", "MESTRE_OBRA", "TECNICO_OBRA", "FINANCEIRO");
        podeValidar = temQualquerPerfil(authentication, "ADMIN", "FINANCEIRO");
        obras = listarObrasAcessiveis(authentication);
        insumos = insumoRepository.findAll(Pageable.unpaged(Sort.by(Sort.Direction.ASC, "descricao")))
                .getContent();

        Long obraSolicitadaId = converterId(FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("obraId"));
        boolean obraAcessivel = obras.stream()
                .anyMatch(obra -> Objects.equals(obra.getId(), obraSolicitadaId));
        obraSelecionadaId = obraAcessivel ? obraSolicitadaId
                : obras.isEmpty() ? null : obras.getFirst().getId();
        carregarObra();

        Long etapaSolicitadaId = converterId(FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("etapaId"));
        if (etapas.stream().anyMatch(etapa -> Objects.equals(etapa.getId(), etapaSolicitadaId))) {
            etapaSelecionadaId = etapaSolicitadaId;
            carregarEtapa();
        }
    }

    public void carregarObra() {
        if (obras.stream().noneMatch(obra -> Objects.equals(obra.getId(), obraSelecionadaId))) {
            etapas = new ArrayList<>();
            tarefas = new ArrayList<>();
            lancamentos = new ArrayList<>();
            limparFormulario();
            return;
        }
        etapas = etapaRepository.findByObraIdOrderByOrdemAsc(obraSelecionadaId);
        etapaSelecionadaId = etapas.isEmpty() ? null : etapas.getFirst().getId();
        carregarEtapa();
    }

    public void carregarEtapa() {
        if (etapas.stream().noneMatch(etapa -> Objects.equals(etapa.getId(), etapaSelecionadaId))) {
            tarefas = new ArrayList<>();
            lancamentos = new ArrayList<>();
            limparFormulario();
            return;
        }
        tarefas = tarefaRepository.findByEtapaId(etapaSelecionadaId);
        lancamentos = lancamentoRepository.findByEtapaId(etapaSelecionadaId,
                Pageable.unpaged(Sort.by(Sort.Direction.DESC, "dataLancamento", "id"))).getContent();
        limparFormulario();
    }

    public void registrar() {
        if (!podeRegistrar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para registrar custos.");
            return;
        }
        if (etapas.stream().noneMatch(etapa -> Objects.equals(etapa.getId(), etapaSelecionadaId))) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione uma etapa válida.");
            return;
        }
        try {
            LancamentoCustoRequest request = new LancamentoCustoRequest(
                    insumoSelecionadoId, null, quantidade, valorUnitario, dataLancamento,
                    numeroDocumento, justificativa, null, tarefaSelecionadaId);
            boolean administrador = temQualquerPerfil(
                    SecurityContextHolder.getContext().getAuthentication(), "ADMIN");
            lancamentoService.registrar(etapaSelecionadaId, request, administrador);
            carregarEtapa();
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Lançamento registrado para validação.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void selecionarLancamento(LancamentoCusto item) {
        if (item == null || lancamentos.stream().noneMatch(lancamento ->
                Objects.equals(lancamento.getId(), item.getId()))) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível localizar o lançamento.");
            return;
        }
        lancamentoSelecionado = item;
        try {
            evidencias = lancamentoService.listarEvidencias(item.getId());
        } catch (ResponseStatusException exception) {
            evidencias = new ArrayList<>();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void validar(LancamentoCusto item, StatusLancamentoEnum status) {
        if (!podeValidar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para validar custos.");
            return;
        }
        if (item == null || item.getStatus() != StatusLancamentoEnum.PENDENTE
                || status == null || status == StatusLancamentoEnum.PENDENTE) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Somente lançamentos pendentes podem ser validados.");
            return;
        }
        try {
            lancamentoService.validar(item.getId(),
                    new ValidacaoLancamentoRequest(status, item.getJustificativa()));
            carregarEtapa();
            if (lancamentoSelecionado != null
                    && Objects.equals(lancamentoSelecionado.getId(), item.getId())) {
                selecionarLancamento(lancamentos.stream()
                        .filter(lancamento -> Objects.equals(lancamento.getId(), item.getId()))
                        .findFirst().orElse(null));
            }
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    status == StatusLancamentoEnum.VALIDADO ? "Lançamento validado." : "Lançamento rejeitado.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void validarLancamento(LancamentoCusto item) {
        validar(item, StatusLancamentoEnum.VALIDADO);
    }

    public void rejeitarLancamento(LancamentoCusto item) {
        validar(item, StatusLancamentoEnum.REJEITADO);
    }

    public String classeStatus(LancamentoCusto item) {
        if (item == null || item.getStatus() == null) {
            return "costs-status-pendente";
        }
        return switch (item.getStatus()) {
            case VALIDADO -> "costs-status-validado";
            case REJEITADO -> "costs-status-rejeitado";
            case PENDENTE -> "costs-status-pendente";
        };
    }

    public boolean pendente(LancamentoCusto item) {
        return item != null && item.getStatus() == StatusLancamentoEnum.PENDENTE;
    }

    public void enviarEvidencia(FileUploadEvent event) {
        if (!podeRegistrar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para anexar fotos.");
            return;
        }
        if (lancamentoSelecionado == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione um lançamento antes de anexar fotos.");
            return;
        }
        try {
            evidencias = lancamentoService.adicionarEvidencia(
                    lancamentoSelecionado.getId(), event.getFile().getFileName(),
                    event.getFile().getContent(), usuarioAtual());
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    "Foto " + event.getFile().getFileName() + " anexada ao lançamento.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void excluirEvidencia(EvidenciaPagamentoResponse evidencia) {
        if (!podeRegistrar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para excluir fotos.");
            return;
        }
        if (lancamentoSelecionado == null || evidencia == null
                || evidencias.stream().noneMatch(item -> Objects.equals(item.id(), evidencia.id()))) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "A foto selecionada não está mais disponível.");
            return;
        }
        try {
            lancamentoService.excluirEvidencia(lancamentoSelecionado.getId(), evidencia.id());
            evidencias = lancamentoService.listarEvidencias(lancamentoSelecionado.getId());
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Foto excluída.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    private void limparFormulario() {
        tarefaSelecionadaId = null;
        insumoSelecionadoId = null;
        quantidade = null;
        valorUnitario = null;
        dataLancamento = LocalDate.now();
        numeroDocumento = null;
        justificativa = null;
        lancamentoSelecionado = null;
        evidencias = new ArrayList<>();
    }

    private List<Obra> listarObrasAcessiveis(Authentication authentication) {
        Sort sort = Sort.by(Sort.Direction.ASC, "nome");
        if (temQualquerPerfil(authentication, "ADMIN")) {
            return obraRepository.findAll(Pageable.unpaged(sort)).getContent();
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        return obraRepository.findFiltered(null, null, null, null, usuario.getId(),
                Pageable.unpaged(sort)).getContent();
    }

    private boolean temQualquerPerfil(Authentication authentication, String... perfis) {
        if (authentication == null) {
            return false;
        }
        for (String perfil : perfis) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + perfil))) {
                return true;
            }
        }
        return false;
    }

    private Long converterId(String valor) {
        try {
            return valor == null ? null : Long.valueOf(valor);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return authentication == null ? null : authentication.getName();
    }

    private String motivo(ResponseStatusException exception) {
        return exception.getReason() == null ? "Não foi possível concluir a operação." : exception.getReason();
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
