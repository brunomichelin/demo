package com.example.demo.beans;

import com.example.demo.dtos.AprovacaoEntregaRequest;
import com.example.demo.dtos.EntregaListaResponse;
import com.example.demo.dtos.EntregaRequest;
import com.example.demo.dtos.EvidenciaEntregaResponse;
import com.example.demo.models.Entrega;
import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import com.example.demo.models.StatusEntregaEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.EntregaRepository;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.EntregaService;
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

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component("entregasBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class EntregasBean {

    @Getter
    private List<Obra> obras = new ArrayList<>();

    @Getter
    private List<Etapa> etapas = new ArrayList<>();

    @Getter
    private List<EntregaListaResponse> entregas = new ArrayList<>();

    @Getter
    private List<EvidenciaEntregaResponse> evidencias = new ArrayList<>();

    @Getter
    @Setter
    private Long obraSelecionadaId;

    @Getter
    @Setter
    private Long etapaSelecionadaId;

    @Getter
    private Entrega entregaSelecionada;

    @Getter
    @Setter
    private LocalDate dataReal;

    @Getter
    @Setter
    private String observacoes;

    @Getter
    private boolean podeRegistrar;

    @Getter
    private boolean podeDecidir;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private EntregaRepository entregaRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EntregaService entregaService;

    @PostConstruct
    public void init() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        podeRegistrar = temQualquerPerfil(authentication, "ADMIN", "GESTOR", "MESTRE_OBRA", "TECNICO_OBRA");
        podeDecidir = temQualquerPerfil(authentication, "ADMIN", "GESTOR");
        obras = listarObrasAcessiveis(authentication);

        String obraIdParametro = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("obraId");
        Long obraSolicitadaId = converterId(obraIdParametro);
        boolean obraAcessivel = obras.stream()
                .anyMatch(obra -> Objects.equals(obra.getId(), obraSolicitadaId));
        obraSelecionadaId = obraAcessivel ? obraSolicitadaId
                : obras.isEmpty() ? null : obras.getFirst().getId();
        carregarObra();

        Long etapaIdParametro = converterId(FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("etapaId"));
        if (etapas.stream().anyMatch(etapa -> Objects.equals(etapa.getId(), etapaIdParametro))) {
            etapaSelecionadaId = etapaIdParametro;
        } else {
            etapaSelecionadaId = etapas.isEmpty() ? null : etapas.getFirst().getId();
        }
        carregarEtapaSelecionada();
    }

    public void carregarObra() {
        if (obras.stream().noneMatch(obra -> Objects.equals(obra.getId(), obraSelecionadaId))) {
            etapas = new ArrayList<>();
            entregas = new ArrayList<>();
            etapaSelecionadaId = null;
            limparEntrega();
            return;
        }
        etapas = etapaRepository.findByObraIdOrderByOrdemAsc(obraSelecionadaId);
        etapaSelecionadaId = etapas.isEmpty() ? null : etapas.getFirst().getId();
        atualizarLista();
        carregarEtapaSelecionada();
    }

    public void carregarEtapaSelecionada() {
        Etapa etapa = etapas.stream()
                .filter(item -> Objects.equals(item.getId(), etapaSelecionadaId))
                .findFirst()
                .orElse(null);
        if (etapa == null) {
            limparEntrega();
            return;
        }
        entregaSelecionada = entregaRepository.findByEtapaId(etapa.getId()).orElse(null);
        if (entregaSelecionada == null) {
            dataReal = null;
            observacoes = null;
            evidencias = new ArrayList<>();
            return;
        }
        dataReal = entregaSelecionada.getDataReal();
        observacoes = entregaSelecionada.getObservacoes();
        evidencias = entregaService.listarEvidencias(etapa.getId());
    }

    public void selecionarEntrega(EntregaListaResponse item) {
        if (item == null || entregas.stream().noneMatch(entrega -> Objects.equals(entrega.id(), item.id()))) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível localizar a entrega selecionada.");
            return;
        }
        etapaSelecionadaId = item.etapaId();
        carregarEtapaSelecionada();
    }

    public void salvar() {
        if (!podeRegistrar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para registrar entregas.");
            return;
        }
        if (etapas.stream().noneMatch(etapa -> Objects.equals(etapa.getId(), etapaSelecionadaId))) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione uma etapa válida.");
            return;
        }
        boolean nova = entregaSelecionada == null;
        try {
            entregaService.registrar(etapaSelecionadaId, new EntregaRequest(dataReal, observacoes));
            carregarEtapaSelecionada();
            atualizarLista();
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    nova ? "Entrega registrada para aprovação." : "Entrega atualizada e enviada para nova aprovação.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void aprovar() {
        decidir(StatusEntregaEnum.APROVADA);
    }

    public void reprovar() {
        decidir(StatusEntregaEnum.REPROVADA);
    }

    public void excluir(EntregaListaResponse item) {
        if (!podeDecidir) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para excluir entregas.");
            return;
        }
        if (item == null || entregas.stream().noneMatch(entrega -> Objects.equals(entrega.id(), item.id()))) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível localizar a entrega selecionada.");
            return;
        }
        try {
            entregaService.excluir(item.etapaId());
            atualizarLista();
            if (Objects.equals(etapaSelecionadaId, item.etapaId())) {
                limparEntrega();
            }
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Entrega e anexos excluídos.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void enviarEvidencia(FileUploadEvent event) {
        if (!podeRegistrar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para anexar evidências.");
            return;
        }
        if (entregaSelecionada == null || etapaSelecionadaId == null) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Registre a entrega antes de anexar arquivos.");
            return;
        }
        try {
            entregaService.adicionarEvidencia(etapaSelecionadaId, event.getFile().getFileName(),
                    event.getFile().getContent(), usuarioAtual());
            evidencias = entregaService.listarEvidencias(etapaSelecionadaId);
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    "Arquivo " + event.getFile().getFileName() + " anexado à entrega.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public void excluirEvidencia(EvidenciaEntregaResponse evidencia) {
        if (!podeRegistrar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para excluir evidências.");
            return;
        }
        if (etapaSelecionadaId == null || entregaSelecionada == null || evidencia == null
                || evidencias.stream().noneMatch(item -> Objects.equals(item.id(), evidencia.id()))) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "O arquivo selecionado não está mais disponível.");
            return;
        }
        try {
            entregaService.excluirEvidencia(etapaSelecionadaId, evidencia.id());
            evidencias = entregaService.listarEvidencias(etapaSelecionadaId);
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Arquivo de evidência excluído.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    public boolean temEntrega(EntregaListaResponse item) {
        return item != null && item.id() != null;
    }

    private void decidir(StatusEntregaEnum status) {
        if (!podeDecidir) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para validar entregas.");
            return;
        }
        if (entregaSelecionada == null || etapaSelecionadaId == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione uma entrega registrada.");
            return;
        }
        try {
            entregaService.aprovar(etapaSelecionadaId, new AprovacaoEntregaRequest(status, observacoes));
            carregarEtapaSelecionada();
            atualizarLista();
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    status == StatusEntregaEnum.APROVADA ? "Entrega aprovada." : "Entrega reprovada.");
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, motivo(exception));
        }
    }

    private void atualizarLista() {
        entregas = obraSelecionadaId == null
                ? new ArrayList<>()
                : entregaService.listarPorObra(obraSelecionadaId);
    }

    private void limparEntrega() {
        entregaSelecionada = null;
        dataReal = null;
        observacoes = null;
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
