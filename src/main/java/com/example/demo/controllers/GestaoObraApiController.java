package com.example.demo.controllers;

import com.example.demo.dtos.*;
import com.example.demo.models.*;
import com.example.demo.repositories.EntregaRepository;
import com.example.demo.repositories.AuditoriaRepository;
import com.example.demo.repositories.ItemOrcamentoRepository;
import com.example.demo.repositories.LancamentoCustoRepository;
import com.example.demo.services.impl.EtapaService;
import com.example.demo.services.impl.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.ContentDisposition;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class GestaoObraApiController {
    private final OrcamentoService orcamentoService;
    private final LancamentoCustoService lancamentoService;
    private final EntregaService entregaService;
    private final RelatorioService relatorioService;
    private final ItemOrcamentoRepository itemRepository;
    private final LancamentoCustoRepository lancamentoRepository;
    private final EntregaRepository entregaRepository;
    private final RelatorioExportService exportService;
    private final TarefaService tarefaService;
    private final EtapaService etapaService;
    private final AuditoriaRepository auditoriaRepository;

    public GestaoObraApiController(
            OrcamentoService orcamentoService,
            LancamentoCustoService lancamentoService,
            EntregaService entregaService,
            RelatorioService relatorioService,
            ItemOrcamentoRepository itemRepository,
            LancamentoCustoRepository lancamentoRepository,
            EntregaRepository entregaRepository,
            RelatorioExportService exportService,
            TarefaService tarefaService,
            EtapaService etapaService,
            AuditoriaRepository auditoriaRepository
    ) {
        this.orcamentoService = orcamentoService;
        this.lancamentoService = lancamentoService;
        this.entregaService = entregaService;
        this.relatorioService = relatorioService;
        this.itemRepository = itemRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.entregaRepository = entregaRepository;
        this.exportService = exportService;
        this.tarefaService = tarefaService;
        this.etapaService = etapaService;
        this.auditoriaRepository = auditoriaRepository;
    }

    @GetMapping("/obras/{obraId}/orcamento/versoes")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public List<Orcamento> listarVersoes(@PathVariable Long obraId) {
        return orcamentoService.listarVersoes(obraId);
    }

    @GetMapping("/obras/{obraId}/etapas")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ENGENHEIRO','ORCAMENTISTA','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO','CLIENTE') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public org.springframework.data.domain.Page<Etapa> listarEtapas(
            @PathVariable Long obraId,
            org.springframework.data.domain.Pageable pageable
    ) {
        return etapaService.listarEtapasObra(obraId, pageable);
    }

    @PostMapping("/obras/{obraId}/etapas")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public ResponseEntity<List<EtapaResponse>> criarEtapas(
            @PathVariable Long obraId,
            @RequestBody List<EtapaRequest> request
    ) {
        return ResponseEntity.status(201).body(etapaService.criarEtapas(obraId, request));
    }

    @GetMapping("/etapas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public ResponseEntity<EtapaResponse> obterEtapa(@PathVariable Long id) {
        EtapaResponse etapa = etapaService.buscarEtapaPorId(id);
        return etapa == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(etapa);
    }

    @PutMapping("/etapas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public ResponseEntity<EtapaResponse> atualizarEtapa(
            @PathVariable Long id,
            @RequestBody EtapaRequest request
    ) {
        EtapaResponse etapa = etapaService.atualizarEtapa(id, request);
        return etapa == null ? ResponseEntity.notFound().build() : ResponseEntity.ok(etapa);
    }

    @PatchMapping("/etapas/{id}/ordem")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public Etapa reordenarEtapa(@PathVariable Long id, @RequestBody ReordenarEtapaRequest request) {
        return etapaService.reordenar(id, request.ordem());
    }

    @DeleteMapping("/etapas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public ResponseEntity<Void> excluirEtapa(@PathVariable Long id) {
        return etapaService.deletarEtapa(id) ? ResponseEntity.noContent().build()
                : ResponseEntity.notFound().build();
    }

    @PostMapping("/obras/{obraId}/orcamento/versoes")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public ResponseEntity<Orcamento> criarVersao(@PathVariable Long obraId) {
        return ResponseEntity.status(201).body(orcamentoService.criarVersao(obraId));
    }

    @PostMapping("/orcamento/versoes/{versaoId}/congelar")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarVersao(#versaoId, authentication)")
    public Orcamento congelarVersao(@PathVariable Long versaoId) {
        return orcamentoService.congelar(versaoId);
    }

    @GetMapping("/etapas/{etapaId}/itens-orcamento")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public org.springframework.data.domain.Page<ItemOrcamento> listarItensOrcamento(
            @PathVariable Long etapaId,
            @RequestParam(required = false) Long versaoId,
            org.springframework.data.domain.Pageable pageable
    ) {
        return itemRepository.findByEtapaIdAndVersao(etapaId, versaoId, pageable);
    }

    @PostMapping("/etapas/{etapaId}/itens-orcamento")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<ItemOrcamento> adicionarItem(
            @PathVariable Long etapaId,
            @RequestParam Long versaoId,
            @RequestBody ItemOrcamentoRequest request
    ) {
        return ResponseEntity.status(201)
                .body(orcamentoService.adicionarItem(etapaId, versaoId, request));
    }

    @GetMapping("/etapas/{etapaId}/lancamentos")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public org.springframework.data.domain.Page<LancamentoCusto> listarLancamentos(
            @PathVariable Long etapaId,
            org.springframework.data.domain.Pageable pageable
    ) {
        return lancamentoRepository.findByEtapaId(etapaId, pageable);
    }

    @PostMapping("/etapas/{etapaId}/lancamentos")
    @PreAuthorize("hasAnyRole('ADMIN','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<LancamentoCusto> registrarLancamento(
            @PathVariable Long etapaId,
            @RequestBody LancamentoCustoRequest request,
            org.springframework.security.core.Authentication authentication
    ) {
        boolean administrador = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
        return ResponseEntity.status(201)
                .body(lancamentoService.registrar(etapaId, request, administrador));
    }

    @PatchMapping("/lancamentos/{id}/validacao")
    @PreAuthorize("hasAnyRole('ADMIN','FINANCEIRO') and @obraAuthorization.podeAcessarLancamento(#id, authentication)")
    public LancamentoCusto validarLancamento(
            @PathVariable Long id,
            @RequestBody ValidacaoLancamentoRequest request
    ) {
        return lancamentoService.validar(id, request);
    }

    @GetMapping("/lancamentos/{id}/evidencias")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO') and @obraAuthorization.podeAcessarLancamento(#id, authentication)")
    public List<EvidenciaPagamentoResponse> listarEvidencias(@PathVariable Long id) {
        return lancamentoService.listarEvidencias(id);
    }

    @PostMapping(value = "/lancamentos/{id}/evidencias", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO') and @obraAuthorization.podeAcessarLancamento(#id, authentication)")
    public ResponseEntity<List<EvidenciaPagamentoResponse>> adicionarEvidencias(
            @PathVariable Long id,
            @RequestParam("arquivos") List<MultipartFile> arquivos,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(lancamentoService.adicionarEvidencias(id, arquivos, authentication.getName()));
    }

    @GetMapping("/lancamentos/{lancamentoId}/evidencias/{evidenciaId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO') and @obraAuthorization.podeAcessarLancamento(#lancamentoId, authentication)")
    public ResponseEntity<byte[]> obterEvidencia(
            @PathVariable Long lancamentoId,
            @PathVariable Long evidenciaId
    ) {
        EvidenciaPagamento evidencia = lancamentoService.buscarArquivoEvidencia(lancamentoId, evidenciaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidencia.getTipo()))
                .contentLength(evidencia.getTamanho())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(evidencia.getNomeArquivo(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(evidencia.getArquivo());
    }

    @DeleteMapping("/lancamentos/{lancamentoId}/evidencias/{evidenciaId}")
    @PreAuthorize("hasAnyRole('ADMIN','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO') and @obraAuthorization.podeAcessarLancamento(#lancamentoId, authentication)")
    public ResponseEntity<Void> excluirEvidencia(
            @PathVariable Long lancamentoId,
            @PathVariable Long evidenciaId
    ) {
        lancamentoService.excluirEvidencia(lancamentoId, evidenciaId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/etapas/{etapaId}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<Entrega> registrarEntrega(
            @PathVariable Long etapaId,
            @RequestBody EntregaRequest request
    ) {
        return ResponseEntity.status(201).body(entregaService.registrar(etapaId, request));
    }

    @PutMapping("/etapas/{etapaId}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public Entrega atualizarEntrega(
            @PathVariable Long etapaId,
            @RequestBody EntregaRequest request
    ) {
        return entregaService.registrar(etapaId, request);
    }

    @GetMapping("/etapas/{etapaId}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<Entrega> obterEntrega(@PathVariable Long etapaId) {
        return entregaRepository.findByEtapaId(etapaId).map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/obras/{obraId}/entregas")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public List<EntregaListaResponse> listarEntregas(@PathVariable Long obraId) {
        return entregaService.listarPorObra(obraId);
    }

    @GetMapping("/etapas/{etapaId}/entrega/evidencias")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public List<EvidenciaEntregaResponse> listarEvidenciasEntrega(@PathVariable Long etapaId) {
        return entregaService.listarEvidencias(etapaId);
    }

    @PostMapping(value = "/etapas/{etapaId}/entrega/evidencias",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<List<EvidenciaEntregaResponse>> adicionarEvidenciasEntrega(
            @PathVariable Long etapaId,
            @RequestParam("arquivos") List<MultipartFile> arquivos,
            org.springframework.security.core.Authentication authentication
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(entregaService.adicionarEvidencias(etapaId, arquivos, authentication.getName()));
    }

    @GetMapping("/etapas/{etapaId}/entrega/evidencias/{evidenciaId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<byte[]> obterEvidenciaEntrega(
            @PathVariable Long etapaId,
            @PathVariable Long evidenciaId
    ) {
        EvidenciaEntrega evidencia = entregaService.buscarArquivoEvidencia(etapaId, evidenciaId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(evidencia.getTipo()))
                .contentLength(evidencia.getTamanho())
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.inline()
                                .filename(evidencia.getNomeArquivo(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .body(evidencia.getArquivo());
    }

    @DeleteMapping("/etapas/{etapaId}/entrega/evidencias/{evidenciaId}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<Void> excluirEvidenciaEntrega(
            @PathVariable Long etapaId,
            @PathVariable Long evidenciaId
    ) {
        entregaService.excluirEvidencia(etapaId, evidenciaId);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/etapas/{etapaId}/entrega/aprovacao")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public Entrega aprovarEntrega(
            @PathVariable Long etapaId,
            @RequestBody AprovacaoEntregaRequest request
    ) {
        return entregaService.aprovar(etapaId, request);
    }

    @DeleteMapping("/etapas/{etapaId}/entrega")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<Void> excluirEntrega(@PathVariable Long etapaId) {
        entregaService.excluir(etapaId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/etapas/{etapaId}/tarefas")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO','CLIENTE') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public org.springframework.data.domain.Page<Tarefa> listarTarefas(
            @PathVariable Long etapaId,
            org.springframework.data.domain.Pageable pageable
    ) {
        return tarefaService.listar(etapaId, pageable);
    }

    @PostMapping("/etapas/{etapaId}/tarefas")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarEtapa(#etapaId, authentication)")
    public ResponseEntity<Tarefa> criarTarefa(
            @PathVariable Long etapaId,
            @RequestBody TarefaRequest request
    ) {
        return ResponseEntity.status(201).body(tarefaService.criar(etapaId, request));
    }

    @PutMapping("/tarefas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarTarefa(#id, authentication)")
    public Tarefa atualizarTarefa(@PathVariable Long id, @RequestBody TarefaRequest request) {
        return tarefaService.atualizar(id, request);
    }

    @PatchMapping("/tarefas/{id}/status")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA') and @obraAuthorization.podeAcessarTarefa(#id, authentication)")
    public Tarefa atualizarStatusTarefa(
            @PathVariable Long id,
            @RequestBody StatusTarefaRequest request
    ) {
        return tarefaService.atualizarStatus(id, request.status());
    }

    @DeleteMapping("/tarefas/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarTarefa(#id, authentication)")
    public ResponseEntity<Void> excluirTarefa(@PathVariable Long id) {
        tarefaService.excluir(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/obras/{obraId}/relatorios/custo-por-etapa")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','FINANCEIRO','CLIENTE') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public ResponseEntity<?> custoPorEtapa(
            @PathVariable Long obraId,
            @RequestParam(required = false) Long versaoId,
            @RequestParam(defaultValue = "json") String formato
    ) {
        RelatorioCustoEtapaResponse relatorio = relatorioService.custosPorEtapa(obraId, versaoId);
        return exportar(formato, "custos-por-etapa", relatorio,
                () -> exportService.custoPdf(relatorio), () -> exportService.custoXlsx(relatorio));
    }

    @GetMapping("/obras/{obraId}/relatorios/insumos-por-etapa")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public ResponseEntity<?> insumosPorEtapa(
            @PathVariable Long obraId,
            @RequestParam(required = false) Long versaoId,
            @RequestParam(defaultValue = "json") String formato
    ) {
        List<RelatorioInsumoResponse> relatorio = relatorioService.insumosPorEtapa(obraId, versaoId);
        return exportar(formato, "insumos-por-etapa", relatorio,
                () -> exportService.insumosPdf(relatorio), () -> exportService.insumosXlsx(relatorio));
    }

    @GetMapping("/obras/{obraId}/relatorios/tarefas-por-etapa")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public ResponseEntity<?> tarefasPorEtapa(
            @PathVariable Long obraId,
            @RequestParam(defaultValue = "json") String formato
    ) {
        List<RelatorioTarefasResponse> relatorio = relatorioService.tarefasPorEtapa(obraId);
        return exportar(formato, "tarefas-por-etapa", relatorio,
                () -> exportService.tarefasPdf(relatorio), () -> exportService.tarefasXlsx(relatorio));
    }

    @GetMapping("/obras/{obraId}/relatorios/prazos-entrega")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','CLIENTE') and @obraAuthorization.podeAcessarObra(#obraId, authentication)")
    public ResponseEntity<?> prazosEntrega(
            @PathVariable Long obraId,
            @RequestParam(defaultValue = "json") String formato
    ) {
        List<RelatorioPrazoEntregaResponse> relatorio = relatorioService.prazosEntrega(obraId);
        return exportar(formato, "prazos-entrega", relatorio,
                () -> exportService.prazosPdf(relatorio), () -> exportService.prazosXlsx(relatorio));
    }

    @GetMapping("/auditoria")
    @PreAuthorize("hasRole('ADMIN')")
    public org.springframework.data.domain.Page<Auditoria> consultarAuditoria(
            org.springframework.data.domain.Pageable pageable
    ) {
        return auditoriaRepository.findAllByOrderByDataHoraDesc(pageable);
    }

    private ResponseEntity<?> exportar(
            String formato,
            String nome,
            Object json,
            java.util.function.Supplier<byte[]> pdf,
            java.util.function.Supplier<byte[]> xlsx
    ) {
        return switch (formato.toLowerCase(java.util.Locale.ROOT)) {
            case "json" -> ResponseEntity.ok(json);
            case "pdf" -> arquivo(nome + ".pdf", MediaType.APPLICATION_PDF, pdf.get());
            case "xlsx" -> arquivo(nome + ".xlsx",
                    MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"),
                    xlsx.get());
            default -> ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(java.util.Map.of("detail", "Formato inválido. Use json, pdf ou xlsx."));
        };
    }

    private ResponseEntity<byte[]> arquivo(String nome, MediaType tipo, byte[] dados) {
        return ResponseEntity.ok()
                .contentType(tipo)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nome + "\"")
                .body(dados);
    }

    public record StatusTarefaRequest(StatusCronEnum status) {
    }

    public record ReordenarEtapaRequest(Long ordem) {
    }
}
