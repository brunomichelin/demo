package com.example.demo.beans;

import com.example.demo.models.Fornecedor;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.repositories.FornecedorRepository;
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

@Component("fornecedorBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class FornecedorBean {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "razaoSocial", "razaoSocial",
            "cnpj", "cnpj",
            "telefone", "telefone",
            "email", "email"
    );

    @Getter
    private List<Fornecedor> fornecedores = new ArrayList<>();

    @Getter
    @Setter
    private Fornecedor fornecedor;

    @Getter
    private boolean visualizacao;

    private Sort sort = Sort.by(Sort.Direction.ASC, "razaoSocial");

    @Autowired
    private FornecedorRepository fornecedorRepository;

    @PostConstruct
    public void init() {
        listarTodas();
        novoFornecedor();
    }

    public void listarTodas() {
        fornecedores = new ArrayList<>(fornecedorRepository.findAll(Pageable.unpaged(sort)).getContent());
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

    public int getQuantidadeFornecedores() {
        return fornecedores.size();
    }

    public void novoFornecedor() {
        fornecedor = new Fornecedor();
        visualizacao = false;
    }

    public void editarFornecedor(Fornecedor fornecedorSelecionado) {
        prepararEdicao(fornecedorSelecionado, false);
    }

    public void visualizarFornecedor(Fornecedor fornecedorSelecionado) {
        prepararEdicao(fornecedorSelecionado, true);
    }

    private void prepararEdicao(Fornecedor fornecedorSelecionado, boolean somenteLeitura) {
        fornecedor = fornecedorSelecionado;
        visualizacao = somenteLeitura;
    }

    public void salvar() {
        boolean novo = fornecedor.getId() == null;
        LocalDateTime agora = LocalDateTime.now();
        if (novo) {
            fornecedor.setDataCriacao(agora);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                fornecedor.setCriadoPor(authentication.getName());
            }
        } else {
            fornecedor.setDataAtualizacao(agora);
        }

        fornecedorRepository.save(fornecedor);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                novo ? "Fornecedor cadastrado com sucesso." : "Fornecedor atualizado com sucesso.");
    }

    public void excluir(Fornecedor fornecedorSelecionado) {
        fornecedorSelecionado.setDataDelecao(LocalDateTime.now());
        fornecedorSelecionado.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        fornecedorRepository.save(fornecedorSelecionado);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO, "Fornecedor excluído com sucesso.");
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
