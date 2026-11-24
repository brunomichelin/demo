package com.example.demo.beans;

import com.example.demo.config.ObraAuthorization;
import com.example.demo.dtos.RelatorioCustoEtapaResponse;
import com.example.demo.dtos.RelatorioInsumoResponse;
import com.example.demo.dtos.RelatorioPrazoEntregaResponse;
import com.example.demo.dtos.RelatorioTarefasResponse;
import com.example.demo.models.Obra;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.services.impl.RelatorioExportService;
import com.example.demo.services.impl.RelatorioService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

@Component("relatoriosBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class RelatoriosBean {
    private final ObraRepository obraRepository;
    private final UsuarioRepository usuarioRepository;
    private final ObraAuthorization obraAuthorization;
    private final RelatorioService relatorioService;
    private final RelatorioExportService exportService;
    private final ObjectMapper objectMapper;

    private List<Obra> obras = new ArrayList<>();
    private List<SelectItem> tiposRelatorio = new ArrayList<>();
    private Long obraId;
    private Long versaoId;
    private String tipoRelatorio;
    private String formato = "json";
    private String resultado = "Selecione um relatório e escolha “Visualizar na tela” para ver os dados aqui.";
    private String mensagem;
    private StreamedContent arquivo;

    public RelatoriosBean(
            ObraRepository obraRepository,
            UsuarioRepository usuarioRepository,
            ObraAuthorization obraAuthorization,
            RelatorioService relatorioService,
            RelatorioExportService exportService,
            ObjectMapper objectMapper
    ) {
        this.obraRepository = obraRepository;
        this.usuarioRepository = usuarioRepository;
        this.obraAuthorization = obraAuthorization;
        this.relatorioService = relatorioService;
        this.exportService = exportService;
        this.objectMapper = objectMapper;
    }

    @PostConstruct
    public void iniciar() {
        Authentication authentication = autenticacao();
        obras = listarObrasAcessiveis(authentication);
        String obraSolicitada = FacesContext.getCurrentInstance().getExternalContext()
                .getRequestParameterMap().get("obraId");
        Long solicitada = converterId(obraSolicitada);
        obraId = obras.stream().anyMatch(obra -> Objects.equals(obra.getId(), solicitada))
                ? solicitada : obras.isEmpty() ? null : obras.getFirst().getId();
        tiposRelatorio = obterTiposPermitidos(authentication);
        tipoRelatorio = tiposRelatorio.isEmpty() ? null : (String) tiposRelatorio.getFirst().getValue();
        if (obras.isEmpty()) mensagem = "Você não está vinculado a nenhuma obra.";
    }

    @Transactional(readOnly = true)
    public void gerar() {
        arquivo = null;
        if (!obraAcessivel(obraId)) {
            erro("Selecione uma obra à qual você tenha acesso.");
            return;
        }
        if (tiposRelatorio.stream().noneMatch(item -> Objects.equals(item.getValue(), tipoRelatorio))) {
            erro("Você não tem permissão para gerar este relatório.");
            return;
        }
        try {
            Object relatorio = obterRelatorio();
            if ("json".equalsIgnoreCase(formato)) {
                resultado = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(relatorio);
                mensagem = "Relatório gerado.";
            } else {
                prepararArquivo(relatorio);
            }
        } catch (IllegalArgumentException exception) {
            erro(exception.getMessage());
        } catch (ResponseStatusException exception) {
            erro(exception.getReason());
        } catch (JacksonException exception) {
            erro("Não foi possível formatar o relatório.");
        }
    }

    private Object obterRelatorio() {
        return switch (tipoRelatorio) {
            case "custo-por-etapa" -> relatorioService.custosPorEtapa(obraId, versaoId);
            case "insumos-por-etapa" -> relatorioService.insumosPorEtapa(obraId, versaoId);
            case "tarefas-por-etapa" -> relatorioService.tarefasPorEtapa(obraId);
            case "prazos-entrega" -> relatorioService.prazosEntrega(obraId);
            default -> throw new IllegalArgumentException("Tipo de relatório inválido.");
        };
    }

    @SuppressWarnings("unchecked")
    private void prepararArquivo(Object relatorio) {
        byte[] conteudo;
        String nome;
        String contentType;
        if ("pdf".equalsIgnoreCase(formato)) {
            conteudo = switch (tipoRelatorio) {
                case "custo-por-etapa" -> exportService.custoPdf((RelatorioCustoEtapaResponse) relatorio);
                case "insumos-por-etapa" -> exportService.insumosPdf((List<RelatorioInsumoResponse>) relatorio);
                case "tarefas-por-etapa" -> exportService.tarefasPdf((List<RelatorioTarefasResponse>) relatorio);
                case "prazos-entrega" -> exportService.prazosPdf((List<RelatorioPrazoEntregaResponse>) relatorio);
                default -> throw new IllegalArgumentException("Tipo de relatório inválido.");
            };
            contentType = "application/pdf";
            nome = tipoRelatorio + ".pdf";
        } else if ("xlsx".equalsIgnoreCase(formato)) {
            conteudo = switch (tipoRelatorio) {
                case "custo-por-etapa" -> exportService.custoXlsx((RelatorioCustoEtapaResponse) relatorio);
                case "insumos-por-etapa" -> exportService.insumosXlsx((List<RelatorioInsumoResponse>) relatorio);
                case "tarefas-por-etapa" -> exportService.tarefasXlsx((List<RelatorioTarefasResponse>) relatorio);
                case "prazos-entrega" -> exportService.prazosXlsx((List<RelatorioPrazoEntregaResponse>) relatorio);
                default -> throw new IllegalArgumentException("Tipo de relatório inválido.");
            };
            contentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
            nome = tipoRelatorio + ".xlsx";
        } else {
            throw new IllegalArgumentException("Formato de arquivo inválido.");
        }
        arquivo = DefaultStreamedContent.builder()
                .name(nome)
                .contentType(contentType)
                .stream(() -> new ByteArrayInputStream(conteudo))
                .build();
        mensagem = "Relatório pronto para download.";
    }

    private List<SelectItem> obterTiposPermitidos(Authentication authentication) {
        List<SelectItem> tipos = new ArrayList<>();
        adicionarTipo(tipos, authentication, "custo-por-etapa", "Custos por etapa",
                "ADMIN", "GESTOR", "FINANCEIRO", "CLIENTE");
        adicionarTipo(tipos, authentication, "insumos-por-etapa", "Insumos por etapa",
                "ADMIN", "GESTOR", "ORCAMENTISTA");
        adicionarTipo(tipos, authentication, "tarefas-por-etapa", "Tarefas por etapa",
                "ADMIN", "GESTOR", "MESTRE_OBRA", "TECNICO_OBRA", "CLIENTE");
        adicionarTipo(tipos, authentication, "prazos-entrega", "Prazos de entrega",
                "ADMIN", "GESTOR", "CLIENTE");
        return tipos;
    }

    private void adicionarTipo(List<SelectItem> tipos, Authentication authentication, String valor,
                               String rotulo, String... papeis) {
        if (temPerfil(authentication, papeis)) tipos.add(new SelectItem(valor, rotulo));
    }

    private List<Obra> listarObrasAcessiveis(Authentication authentication) {
        Sort sort = Sort.by(Sort.Direction.ASC, "nome");
        Pageable pageable = PageRequest.of(0, 100, sort);
        if (temPerfil(authentication, "ADMIN")) return obraRepository.findAll(pageable).getContent();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        return obraRepository.findFiltered(null, null, null, null, usuario.getId(), pageable).getContent();
    }

    private boolean obraAcessivel(Long id) {
        return id != null && obras.stream().anyMatch(obra -> Objects.equals(obra.getId(), id))
                && obraAuthorization.podeAcessarObra(id, autenticacao());
    }

    private boolean temPerfil(Authentication authentication, String... papeis) {
        return authentication != null && Arrays.stream(papeis).anyMatch(papel -> authentication.getAuthorities()
                .stream().anyMatch(authority -> authority.getAuthority().equals("ROLE_" + papel)));
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

    public void limparResultado() {
        resultado = "Selecione um relatório e escolha “Visualizar na tela” para ver os dados aqui.";
        mensagem = null;
        arquivo = null;
    }

    private void erro(String texto) {
        mensagem = texto == null ? "Não foi possível gerar o relatório." : texto;
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_ERROR, mensagem, null));
    }

    public List<Obra> getObras() { return obras; }
    public List<SelectItem> getTiposRelatorio() { return tiposRelatorio; }
    public Long getObraId() { return obraId; }
    public void setObraId(Long obraId) { this.obraId = obraId; }
    public Long getVersaoId() { return versaoId; }
    public void setVersaoId(Long versaoId) { this.versaoId = versaoId; }
    public String getTipoRelatorio() { return tipoRelatorio; }
    public void setTipoRelatorio(String tipoRelatorio) { this.tipoRelatorio = tipoRelatorio; }
    public String getFormato() { return formato; }
    public void setFormato(String formato) { this.formato = formato; }
    public String getResultado() { return resultado; }
    public String getMensagem() { return mensagem; }
    public StreamedContent getArquivo() { return arquivo; }
    public boolean isArquivoDisponivel() { return arquivo != null; }
}
