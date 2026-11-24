package com.example.demo.beans;

import com.example.demo.dtos.ItemOrcamentoRequest;
import com.example.demo.models.Etapa;
import com.example.demo.models.Insumo;
import com.example.demo.models.ItemOrcamento;
import com.example.demo.models.Obra;
import com.example.demo.models.Orcamento;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.InsumoRepository;
import com.example.demo.repositories.ItemOrcamentoRepository;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.OrcamentoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Component("orcamentoBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class OrcamentoBean {

    private final OrcamentoService orcamentoService;
    private final ObraRepository obraRepository;
    private final EtapaRepository etapaRepository;
    private final InsumoRepository insumoRepository;
    private final ItemOrcamentoRepository itemRepository;
    private final UsuarioRepository usuarioRepository;

    private List<Obra> obras = new ArrayList<>();
    private List<Etapa> etapas = new ArrayList<>();
    private List<Insumo> insumos = new ArrayList<>();
    private List<Orcamento> versoes = new ArrayList<>();
    private List<ItemOrcamento> itens = new ArrayList<>();
    private Long obraSelecionadaId;
    private Long etapaSelecionadaId;
    private Long versaoSelecionadaId;
    private Long insumoSelecionadoId;
    private BigDecimal quantidade;
    private BigDecimal valorUnitario;
    private String mensagem;
    private boolean podeCongelar;
    private boolean podeGerenciar;
    private boolean podeTrocarObra;

    public OrcamentoBean(
            OrcamentoService orcamentoService,
            ObraRepository obraRepository,
            EtapaRepository etapaRepository,
            InsumoRepository insumoRepository,
            ItemOrcamentoRepository itemRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.orcamentoService = orcamentoService;
        this.obraRepository = obraRepository;
        this.etapaRepository = etapaRepository;
        this.insumoRepository = insumoRepository;
        this.itemRepository = itemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @PostConstruct
    public void iniciar() {
        Authentication authentication = autenticacao();
        podeCongelar = temPerfil(authentication, "ADMIN", "GESTOR");
        podeGerenciar = temPerfil(authentication, "ADMIN", "GESTOR", "ORCAMENTISTA");
        FacesContext facesContext = FacesContext.getCurrentInstance();
        String obraSolicitadaParametro = facesContext.getExternalContext()
                .getRequestParameterMap().get("obraId");
        podeTrocarObra = !StringUtils.hasText(obraSolicitadaParametro)
                || "index".equals(facesContext.getExternalContext()
                .getRequestParameterMap().get("origem"));
        obras = listarObrasAcessiveis(authentication);
        insumos = insumoRepository.findAll(Pageable.unpaged(Sort.by(Sort.Direction.ASC, "descricao")))
                .getContent();

        Long solicitada = converterId(obraSolicitadaParametro);
        obraSelecionadaId = obras.stream().anyMatch(obra -> Objects.equals(obra.getId(), solicitada))
                ? solicitada : obras.isEmpty() ? null : obras.getFirst().getId();
        carregarObra();
    }

    public void carregarObra() {
        if (!obraAcessivel(obraSelecionadaId)) {
            etapas = new ArrayList<>();
            versoes = new ArrayList<>();
            itens = new ArrayList<>();
            etapaSelecionadaId = null;
            versaoSelecionadaId = null;
            return;
        }
        try {
            etapas = etapaRepository.findByObraIdOrderByOrdemAsc(obraSelecionadaId);
            versoes = orcamentoService.listarVersoes(obraSelecionadaId);
            etapaSelecionadaId = etapas.isEmpty() ? null : etapas.getFirst().getId();
            versaoSelecionadaId = versoes.isEmpty() ? null : versoes.getLast().getId();
            carregarItens();
        } catch (ResponseStatusException exception) {
            reportarErro(exception);
        }
    }

    public void carregarItens() {
        if (!etapaAcessivel(etapaSelecionadaId) || !versaoAcessivel(versaoSelecionadaId)) {
            itens = new ArrayList<>();
            return;
        }
        try {
            itens = itemRepository.findByEtapaIdAndVersao(etapaSelecionadaId, versaoSelecionadaId,
                    Pageable.unpaged(Sort.by(Sort.Direction.ASC, "id"))).getContent();
        } catch (ResponseStatusException exception) {
            reportarErro(exception);
        }
    }

    public void criarVersao() {
        if (!podeGerenciar || !obraAcessivel(obraSelecionadaId)) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para criar uma versão.");
            return;
        }
        try {
            Orcamento nova = orcamentoService.criarVersao(obraSelecionadaId);
            carregarObra();
            versaoSelecionadaId = nova.getId();
            carregarItens();
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Versão criada.");
        } catch (ResponseStatusException exception) {
            reportarErro(exception);
        }
    }

    public void congelarVersao() {
        if (!podeCongelar || !versaoAcessivel(versaoSelecionadaId)) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para congelar esta versão.");
            return;
        }
        try {
            orcamentoService.congelar(versaoSelecionadaId);
            carregarObra();
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Versão congelada.");
        } catch (ResponseStatusException exception) {
            reportarErro(exception);
        }
    }

    public void adicionarItem() {
        if (!podeGerenciar) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Você não tem permissão para incluir itens.");
            return;
        }
        if (!etapaAcessivel(etapaSelecionadaId) || !versaoAcessivel(versaoSelecionadaId)) {
            adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione uma etapa e uma versão válidas.");
            return;
        }
        try {
            orcamentoService.adicionarItem(etapaSelecionadaId, versaoSelecionadaId,
                    new ItemOrcamentoRequest(insumoSelecionadoId, null, quantidade, valorUnitario));
            quantidade = null;
            valorUnitario = null;
            carregarItens();
            adicionarMensagem(FacesMessage.SEVERITY_INFO, "Item incluído.");
        } catch (ResponseStatusException exception) {
            reportarErro(exception);
        }
    }

    public boolean isVersaoCongelada() {
        return versoes.stream()
                .filter(versao -> Objects.equals(versao.getId(), versaoSelecionadaId))
                .findFirst().map(versao -> Boolean.TRUE.equals(versao.getCongelada()))
                .orElse(true);
    }

    public BigDecimal getTotalItens() {
        return itens.stream()
                .map(item -> item.getQuantidade().multiply(item.getValorUnitario()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String formatarMoeda(BigDecimal valor) {
        return valor == null ? "—" : String.format(java.util.Locale.forLanguageTag("pt-BR"), "R$ %,.2f", valor);
    }

    private boolean obraAcessivel(Long id) {
        return id != null && obras.stream().anyMatch(obra -> Objects.equals(obra.getId(), id));
    }

    private boolean etapaAcessivel(Long id) {
        return id != null && etapas.stream().anyMatch(etapa -> Objects.equals(etapa.getId(), id));
    }

    private boolean versaoAcessivel(Long id) {
        if (id == null) return false;
        Orcamento versao = versoes.stream().filter(item -> Objects.equals(item.getId(), id))
                .findFirst().orElse(null);
        return versao != null && Objects.equals(versao.getObra().getId(), obraSelecionadaId);
    }

    private List<Obra> listarObrasAcessiveis(Authentication authentication) {
        Sort sort = Sort.by(Sort.Direction.ASC, "nome");
        if (temPerfil(authentication, "ADMIN")) {
            return obraRepository.findAll(Pageable.unpaged(sort)).getContent();
        }
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        return obraRepository.findFiltered(null, null, null, null, usuario.getId(),
                Pageable.unpaged(sort)).getContent();
    }

    private boolean temPerfil(Authentication authentication, String... perfis) {
        if (authentication == null) return false;
        for (String perfil : perfis) {
            if (authentication.getAuthorities().stream()
                    .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + perfil))) {
                return true;
            }
        }
        return false;
    }

    private Authentication autenticacao() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalStateException("Não foi possível identificar o usuário autenticado.");
        }
        return authentication;
    }

    private Long converterId(String valor) {
        try {
            return valor == null ? null : Long.valueOf(valor);
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    private void reportarErro(ResponseStatusException exception) {
        mensagem = exception.getReason() == null ? "Não foi possível concluir a operação." : exception.getReason();
        adicionarMensagem(FacesMessage.SEVERITY_ERROR, mensagem);
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        mensagem = texto;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

    public List<Obra> getObras() { return obras; }
    public List<Etapa> getEtapas() { return etapas; }
    public List<Insumo> getInsumos() { return insumos; }
    public List<Orcamento> getVersoes() { return versoes; }
    public List<ItemOrcamento> getItens() { return itens; }
    public Long getObraSelecionadaId() { return obraSelecionadaId; }
    public void setObraSelecionadaId(Long id) { obraSelecionadaId = id; }
    public Long getEtapaSelecionadaId() { return etapaSelecionadaId; }
    public void setEtapaSelecionadaId(Long id) { etapaSelecionadaId = id; }
    public Long getVersaoSelecionadaId() { return versaoSelecionadaId; }
    public void setVersaoSelecionadaId(Long id) { versaoSelecionadaId = id; }
    public Long getInsumoSelecionadoId() { return insumoSelecionadoId; }
    public void setInsumoSelecionadoId(Long id) { insumoSelecionadoId = id; }
    public BigDecimal getQuantidade() { return quantidade; }
    public void setQuantidade(BigDecimal quantidade) { this.quantidade = quantidade; }
    public BigDecimal getValorUnitario() { return valorUnitario; }
    public void setValorUnitario(BigDecimal valorUnitario) { this.valorUnitario = valorUnitario; }
    public String getMensagem() { return mensagem; }
    public boolean isPodeCongelar() { return podeCongelar; }
    public boolean isPodeGerenciar() { return podeGerenciar; }
    public boolean isPodeTrocarObra() { return podeTrocarObra; }
}
