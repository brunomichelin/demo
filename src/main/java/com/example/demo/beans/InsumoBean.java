package com.example.demo.beans;

import com.example.demo.models.Insumo;
import com.example.demo.models.MedidaEnum;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Fornecedor;
import com.example.demo.models.HistoricoPrecoInsumo;
import com.example.demo.repositories.InsumoRepository;
import com.example.demo.repositories.FornecedorRepository;
import com.example.demo.repositories.HistoricoPrecoInsumoRepository;
import com.example.demo.services.impl.HistoricoPrecoInsumoService;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("insumoBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class InsumoBean {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "descricao", "descricao",
            "codigo", "codigo",
            "tipo", "tipo",
            "unidade", "unidade",
            "precoReferencia", "precoReferencia"
    );

    @Getter
    private List<Insumo> insumos = new ArrayList<>();

    @Getter
    private List<Fornecedor> fornecedores = new ArrayList<>();

    @Getter
    private List<HistoricoPrecoInsumo> historicoPrecos = new ArrayList<>();

    @Getter
    private final String[] tiposDisponiveis = {"Material", "Mão de obra", "Equipamento", "Serviço"};

    @Getter
    @Setter
    private Long fornecedorSelecionadoId;

    @Getter
    private final MedidaEnum[] unidadesDisponiveis = MedidaEnum.values();

    @Getter
    @Setter
    private Insumo insumo;

    @Getter
    private boolean visualizacao;

    private Sort sort = Sort.by(Sort.Direction.ASC, "descricao");

    @Autowired
    private InsumoRepository insumoRepository;

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @Autowired
    private HistoricoPrecoInsumoRepository historicoPrecoRepository;

    @Autowired
    private HistoricoPrecoInsumoService historicoPrecoService;

    @PostConstruct
    public void init() {
        listarTodas();
        fornecedores = fornecedorRepository.findAll(Pageable.unpaged(Sort.by(Sort.Direction.ASC, "razaoSocial")))
                .getContent();
        novoInsumo();
    }

    public void listarTodas() {
        insumos = new ArrayList<>(insumoRepository.findAll(Pageable.unpaged(sort)).getContent());
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

    public int getQuantidadeInsumos() {
        return insumos.size();
    }

    public void novoInsumo() {
        insumo = new Insumo();
        fornecedorSelecionadoId = null;
        visualizacao = false;
    }

    public void editarInsumo(Insumo insumoSelecionado) {
        prepararEdicao(insumoSelecionado, false);
    }

    public void visualizarInsumo(Insumo insumoSelecionado) {
        prepararEdicao(insumoSelecionado, true);
    }

    public void visualizarHistoricoPrecos(Insumo insumoSelecionado) {
        insumo = insumoSelecionado;
        historicoPrecos = historicoPrecoRepository.findByInsumoIdOrderByRegistradoEmDesc(insumoSelecionado.getId());
    }

    private void prepararEdicao(Insumo insumoSelecionado, boolean somenteLeitura) {
        insumo = insumoSelecionado;
        fornecedorSelecionadoId = insumoSelecionado.getFornecedor() == null ? null
                : insumoSelecionado.getFornecedor().getId();
        visualizacao = somenteLeitura;
    }

    @Transactional
    public void salvar() {
        if (insumo.getCodigo() == null || insumo.getCodigo().isBlank()
                || insumo.getPrecoReferencia() == null || insumo.getPrecoReferencia().signum() <= 0) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Informe código e preço de referência maior que zero.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }
        boolean codigoDuplicado = insumos.stream().anyMatch(cadastrado ->
                !cadastrado.getId().equals(insumo.getId())
                        && cadastrado.getCodigo() != null
                        && cadastrado.getCodigo().equalsIgnoreCase(insumo.getCodigo().trim()));
        if (codigoDuplicado) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Já existe um insumo cadastrado com este código.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }
        Fornecedor fornecedor = fornecedorSelecionadoId == null ? null
                : fornecedorRepository.findById(fornecedorSelecionadoId).orElse(null);
        if (fornecedorSelecionadoId != null && fornecedor == null) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Selecione um fornecedor válido.");
            FacesContext.getCurrentInstance().validationFailed();
            return;
        }
        insumo.setFornecedor(fornecedor);
        boolean novo = insumo.getId() == null;
        java.math.BigDecimal precoAnterior = novo ? null : insumoRepository.findById(insumo.getId())
                .map(Insumo::getPrecoReferencia).orElse(null);
        LocalDateTime agora = LocalDateTime.now();
        if (novo) {
            insumo.setDataCriacao(agora);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                insumo.setCriadoPor(authentication.getName());
            }
        } else {
            insumo.setDataAtualizacao(agora);
        }

        Insumo salvo = insumoRepository.save(insumo);
        historicoPrecoService.registrar(salvo, precoAnterior, salvo.getPrecoReferencia());
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                novo ? "Insumo cadastrado com sucesso." : "Insumo atualizado com sucesso.");
    }

    public void excluir(Insumo insumoSelecionado) {
        insumoSelecionado.setDataDelecao(LocalDateTime.now());
        insumoSelecionado.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        insumoRepository.save(insumoSelecionado);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO, "Insumo excluído com sucesso.");
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
