package com.example.demo.beans;

import com.example.demo.models.DocObra;
import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import com.example.demo.models.Orcamento;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.Usuario;
import com.example.demo.models.Usuario;
import com.example.demo.dtos.EtapaRequest;
import com.example.demo.repositories.DocObraRepository;
import com.example.demo.repositories.DocObraResumo;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.OrcamentoRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.config.ObraAuthorization;
import com.example.demo.services.impl.EtapaService;
import com.example.demo.services.impl.ObraService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.primefaces.model.file.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.io.ByteArrayInputStream;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Component("obraDetailBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ObraDetailBean {

    @Getter
    private Obra obra;

    @Getter
    @Setter
    private String obraId;

    @Getter
    private List<Etapa> etapas = new ArrayList<>();

    @Getter
    private List<Usuario> clientes = new ArrayList<>();

    @Getter
    private List<Orcamento> orcamentos = new ArrayList<>();

    @Getter
    private List<DocObraResumo> documentos = new ArrayList<>();

    @Getter
    @Setter
    private DocObra documento;

    @Getter
    @Setter
    private UploadedFile arquivoUpload;

    @Getter
    @Setter
    private EtapaRequest novaEtapa;

    @Getter
    private final StatusCronEnum[] statusEtapaDisponiveis = {
            StatusCronEnum.PLANEJAMENTO, StatusCronEnum.EM_ANDAMENTO,
            StatusCronEnum.EM_BLOQUEIO, StatusCronEnum.CONCLUIDA, StatusCronEnum.CANCELADA
    };

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private EtapaRepository etapaRepository;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private DocObraRepository docObraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private ObraAuthorization obraAuthorization;

    @Autowired
    private EtapaService etapaService;

    @Autowired
    private ObraService obraService;

    @PostConstruct
    public void iniciar() {
        documento = new DocObra();
        prepararNovaEtapa();
    }

    public String carregar() {
        try {
            long id = Long.parseLong(obraId);
            obra = obraRepository.findWithClientesById(id).orElse(null);
            if (obra == null) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Obra não encontrada.");
                return "obras?faces-redirect=true";
            }
            if (!temAcesso(obra)) {
                adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não está vinculado a esta obra.");
                return "obras?faces-redirect=true";
            }
        } catch (NumberFormatException | NullPointerException e) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Identificador da obra inválido.");
            return "obras?faces-redirect=true";
        }

        clientes = obra.getClientes() == null ? new ArrayList<>() : obra.getClientes();
        etapas = etapaRepository.findByObraIdOrderByOrdemAsc(obra.getId());
        orcamentos = orcamentoRepository.findByObraIdOrderByNumeroAsc(obra.getId());
        listarDocumentos();
        return null;
    }

    public void novoDocumento() {
        documento = new DocObra();
        arquivoUpload = null;
    }

    public void prepararNovaEtapa() {
        novaEtapa = new EtapaRequest();
        novaEtapa.setStatus(StatusCronEnum.PLANEJAMENTO);
        novaEtapa.setOrdem(etapas.stream().map(Etapa::getOrdem)
                .filter(java.util.Objects::nonNull).max(Long::compareTo).orElse(0L) + 1);
    }

    public void salvarEtapa() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (obra == null || !podeGerenciarEtapas(authentication)
                || !obraAuthorization.podeAcessarObra(obra.getId(), authentication)) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para criar etapas nesta obra.");
            return;
        }
        if (novaEtapa == null || novaEtapa.getNome() == null || novaEtapa.getNome().isBlank()
                || novaEtapa.getOrdem() == null || novaEtapa.getOrdem() <= 0) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Informe o nome e uma ordem positiva para a etapa.");
            return;
        }
        if (etapas.stream().anyMatch(etapa -> novaEtapa.getOrdem().equals(etapa.getOrdem()))) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "A ordem já está sendo usada por outra etapa da obra.");
            return;
        }
        if (novaEtapa.getDataInicioPrevista() != null && novaEtapa.getDataEntregaPrevista() != null
                && novaEtapa.getDataEntregaPrevista().isBefore(novaEtapa.getDataInicioPrevista())) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR,
                    "A entrega prevista não pode anteceder o início previsto.");
            return;
        }
        try {
            etapaService.criarEtapas(obra.getId(), List.of(novaEtapa));
            etapas = etapaRepository.findByObraIdOrderByOrdemAsc(obra.getId());
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Etapa cadastrada com sucesso.");
            prepararNovaEtapa();
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR,
                    exception.getReason() == null ? "Não foi possível criar a etapa." : exception.getReason());
        }
    }

    public boolean isPodeAvancarStatus() {
        return isStatusManager() && obra != null
                && (obra.getStatus() == StatusCronEnum.PLANEJAMENTO
                || obra.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || obra.getStatus() == StatusCronEnum.EM_BLOQUEIO
                || obra.getStatus() == StatusCronEnum.PAUSADA);
    }

    public boolean isPodeBloquearStatus() {
        return isStatusManager() && obra != null
                && (obra.getStatus() == StatusCronEnum.PLANEJAMENTO
                || obra.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || obra.getStatus() == StatusCronEnum.PAUSADA);
    }

    public boolean isPodeCancelarStatus() {
        return isStatusManager() && obra != null
                && (obra.getStatus() == StatusCronEnum.PLANEJAMENTO
                || obra.getStatus() == StatusCronEnum.EM_ANDAMENTO
                || obra.getStatus() == StatusCronEnum.EM_BLOQUEIO
                || obra.getStatus() == StatusCronEnum.PAUSADA);
    }

    public void avancarStatus() {
        if (obra == null || obra.getStatus() == null) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Não há um próximo status disponível.");
            return;
        }
        StatusCronEnum proximoStatus = switch (obra.getStatus()) {
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
        if (!podeAcessarParaAlterarStatus()) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para alterar o status desta obra.");
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
            obra = obraService.atualizarStatus(obra.getId(), novoStatus);
            adicionarMensagem(FacesMessage.SEVERITY_INFO,
                    "Status da obra atualizado para " + novoStatus.getDescricao() + ".");
        } catch (org.springframework.web.server.ResponseStatusException exception) {
            FacesContext.getCurrentInstance().validationFailed();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR,
                    exception.getReason() == null ? "Não foi possível atualizar o status." : exception.getReason());
        }
    }

    private boolean podeAcessarParaAlterarStatus() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return obra != null && isStatusManager()
                && obraAuthorization.podeAcessarObra(obra.getId(), authentication);
    }

    private boolean isStatusManager() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return podeGerenciarEtapas(authentication);
    }

    public void editarDocumento(DocObraResumo documentoSelecionado) {
        documento = docObraRepository.findByIdAndObraId(documentoSelecionado.getId(), obra.getId())
                .orElseThrow(() -> new IllegalArgumentException("Documento não pertence a esta obra."));
        arquivoUpload = null;
    }

    public void salvarDocumento() {
        if (obra == null || documento == null || documento.getNome() == null
                || documento.getNome().isBlank()) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Informe o nome do documento.");
            return;
        }
        boolean novo = documento.getId() == null;
        if (novo && (arquivoUpload == null || arquivoUpload.getSize() == 0)) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione um arquivo para o documento.");
            return;
        }

        if (arquivoUpload != null && arquivoUpload.getSize() > 0) {
            documento.setNomeArquivo(arquivoUpload.getFileName());
            documento.setTipo(arquivoUpload.getContentType());
            documento.setTamanho(arquivoUpload.getSize());
            documento.setArquivo(arquivoUpload.getContent());
        }

        documento.setObra(obra);
        LocalDateTime agora = LocalDateTime.now();
        if (novo) {
            documento.setDataCriacao(agora);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                documento.setCriadoPor(authentication.getName());
            }
        } else {
            documento.setDataAtualizacao(agora);
        }
        docObraRepository.save(documento);
        listarDocumentos();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                novo ? "Documento cadastrado com sucesso." : "Documento atualizado com sucesso.");
        novoDocumento();
    }

    public void excluirDocumento(DocObraResumo documentoSelecionado) {
        var documentoEncontrado = docObraRepository.findByIdAndObraId(
                documentoSelecionado.getId(), obra.getId());
        if (documentoEncontrado.isPresent()) {
            DocObra documento = documentoEncontrado.get();
            documento.setDataDelecao(LocalDateTime.now());
            documento.setStatusRegistro(StatusRegEnum.EXCLUIDO);
            docObraRepository.save(documento);
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Documento excluído com sucesso.");
        } else {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Documento não encontrado para esta obra.");
        }
        listarDocumentos();
    }

    public StreamedContent baixarDocumento(DocObraResumo resumo) {
        byte[] arquivo = docObraRepository.findArquivoByIdAndObraId(resumo.getId(), obra.getId())
                .orElseThrow(() -> new IllegalArgumentException("Arquivo do documento não encontrado."));
        return DefaultStreamedContent.builder()
                .name(resumo.getNomeArquivo())
                .contentType(resumo.getTipo() == null ? "application/octet-stream" : resumo.getTipo())
                .stream(() -> new ByteArrayInputStream(arquivo))
                .build();
    }

    private void listarDocumentos() {
        documentos = docObraRepository.findByObraIdOrderByIdDesc(obra.getId());
    }

    private boolean temAcesso(Obra obra) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return obraAuthorization.podeAcessarObra(obra.getId(), authentication);
    }

    private boolean podeGerenciarEtapas(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_GESTOR".equals(authority.getAuthority()));
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
