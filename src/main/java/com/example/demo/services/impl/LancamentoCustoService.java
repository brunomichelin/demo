package com.example.demo.services.impl;

import com.example.demo.dtos.LancamentoCustoRequest;
import com.example.demo.dtos.ValidacaoLancamentoRequest;
import com.example.demo.dtos.EvidenciaPagamentoResponse;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import com.example.demo.repositories.DocObraRepository;
import com.example.demo.models.DocObra;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.io.IOException;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class LancamentoCustoService {
    private final LancamentoCustoRepository lancamentoRepository;
    private final EtapaRepository etapaRepository;
    private final InsumoRepository insumoRepository;
    private final TarefaRepository tarefaRepository;
    private final FornecedorRepository fornecedorRepository;
    private final AuditoriaService auditoriaService;
    private final DocObraRepository docObraRepository;
    private final EvidenciaPagamentoRepository evidenciaRepository;

    private static final long TAMANHO_MAXIMO_FOTO = 10 * 1024 * 1024;
    private static final int QUANTIDADE_MAXIMA_FOTOS = 10;

    public LancamentoCustoService(
            LancamentoCustoRepository lancamentoRepository,
            EtapaRepository etapaRepository,
            InsumoRepository insumoRepository,
            TarefaRepository tarefaRepository,
            FornecedorRepository fornecedorRepository,
            AuditoriaService auditoriaService,
            DocObraRepository docObraRepository,
            EvidenciaPagamentoRepository evidenciaRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
        this.etapaRepository = etapaRepository;
        this.insumoRepository = insumoRepository;
        this.tarefaRepository = tarefaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.auditoriaService = auditoriaService;
        this.docObraRepository = docObraRepository;
        this.evidenciaRepository = evidenciaRepository;
    }

    @Transactional
    public LancamentoCusto registrar(Long etapaId, LancamentoCustoRequest request, boolean administrador) {
        if (request == null || request.insumoId() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um insumo válido.");
        }
        positivo(request.quantidade(), "A quantidade deve ser maior que zero.");
        positivo(request.valorUnitario(), "O valor unitário deve ser maior que zero.");
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        boolean obraCancelada = etapa.getObra().getStatus() == StatusCronEnum.CANCELADA;
        boolean etapaConcluida = etapa.getStatus() == StatusCronEnum.CONCLUIDA;
        if ((obraCancelada || etapaConcluida)
                && (!administrador || !StringUtils.hasText(request.justificativa()))) {
            throw erro(HttpStatus.CONFLICT,
                    "Não é permitido lançar custos em etapa concluída ou obra cancelada sem perfil administrador e justificativa.");
        }
        Insumo insumo = insumoRepository.findById(request.insumoId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Insumo não encontrado."));
        Tarefa tarefa = request.tarefaId() == null ? null : tarefaRepository.findById(request.tarefaId())
                .filter(item -> item.getEtapa().getId().equals(etapa.getId()))
                .orElseThrow(() -> erro(HttpStatus.BAD_REQUEST,
                        "A tarefa informada não pertence à etapa do lançamento."));
        Fornecedor fornecedor = request.fornecedorId() == null ? null : fornecedorRepository.findById(request.fornecedorId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Fornecedor não encontrado."));
        DocObra documento = request.documentoId() == null ? null : docObraRepository
                .findByIdAndObraId(request.documentoId(), etapa.getObra().getId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND,
                        "Documento não encontrado ou não pertence à obra da etapa."));

        LancamentoCusto lancamento = new LancamentoCusto();
        lancamento.setEtapa(etapa);
        lancamento.setTarefa(tarefa);
        lancamento.setInsumo(insumo);
        lancamento.setFornecedor(fornecedor);
        lancamento.setDocumento(documento);
        lancamento.setQuantidade(request.quantidade());
        lancamento.setValorUnitario(request.valorUnitario());
        lancamento.setDataLancamento(request.dataLancamento() == null ? LocalDate.now() : request.dataLancamento());
        lancamento.setNumeroDocumento(request.numeroDocumento());
        lancamento.setJustificativa(request.justificativa());
        lancamento.setStatus(StatusLancamentoEnum.PENDENTE);
        lancamento.setDataCriacao(LocalDateTime.now());
        LancamentoCusto salvo = lancamentoRepository.save(lancamento);
        auditoriaService.registrar("lancamento_custo", salvo.getId(), "CRIACAO", null,
                "status=PENDENTE, quantidade=" + salvo.getQuantidade() + ", valorUnitario=" + salvo.getValorUnitario());
        return salvo;
    }

    @Transactional
    public LancamentoCusto validar(Long id, ValidacaoLancamentoRequest request) {
        if (request == null || (request.status() != StatusLancamentoEnum.VALIDADO
                && request.status() != StatusLancamentoEnum.REJEITADO)) {
            throw erro(HttpStatus.BAD_REQUEST, "O status deve ser VALIDADO ou REJEITADO.");
        }
        LancamentoCusto lancamento = lancamentoRepository.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Lançamento não encontrado."));
        if (lancamento.getStatus() != StatusLancamentoEnum.PENDENTE) {
            throw erro(HttpStatus.CONFLICT, "Somente lançamentos pendentes podem ser validados.");
        }
        StatusLancamentoEnum anterior = lancamento.getStatus();
        lancamento.setStatus(request.status());
        lancamento.setJustificativa(request.justificativa());
        lancamento.setDataAtualizacao(LocalDateTime.now());
        LancamentoCusto salvo = lancamentoRepository.save(lancamento);
        auditoriaService.registrar("lancamento_custo", salvo.getId(), "VALIDACAO", anterior.name(),
                salvo.getStatus().name());
        return salvo;
    }

    @Transactional(readOnly = true)
    public List<EvidenciaPagamentoResponse> listarEvidencias(Long lancamentoId) {
        buscarLancamento(lancamentoId);
        return evidenciaRepository.listarPorLancamento(lancamentoId);
    }

    @Transactional
    public List<EvidenciaPagamentoResponse> adicionarEvidencias(
            Long lancamentoId,
            List<MultipartFile> arquivos,
            String usuario
    ) {
        LancamentoCusto lancamento = buscarLancamento(lancamentoId);
        if (arquivos == null || arquivos.isEmpty() || arquivos.size() > QUANTIDADE_MAXIMA_FOTOS) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "Selecione de 1 a " + QUANTIDADE_MAXIMA_FOTOS + " fotos por envio.");
        }

        for (MultipartFile arquivo : arquivos) {
            validarFoto(arquivo);
            byte[] conteudo;
            try {
                conteudo = arquivo.getBytes();
            } catch (IOException exception) {
                throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Não foi possível ler uma das fotos enviadas.", exception);
            }

            salvarEvidencia(lancamento, arquivo.getOriginalFilename(), conteudo, usuario);
        }
        return evidenciaRepository.listarPorLancamento(lancamentoId);
    }

    @Transactional
    public List<EvidenciaPagamentoResponse> adicionarEvidencia(
            Long lancamentoId,
            String nomeArquivo,
            byte[] conteudo,
            String usuario
    ) {
        LancamentoCusto lancamento = buscarLancamento(lancamentoId);
        if (conteudo == null || conteudo.length == 0) {
            throw erro(HttpStatus.BAD_REQUEST, "O arquivo enviado está vazio.");
        }
        if (conteudo.length > TAMANHO_MAXIMO_FOTO) {
            throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "Cada foto deve ter no máximo 10 MB.");
        }
        String tipo = detectarTipo(conteudo);
        if (tipo == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie fotos nos formatos JPEG, PNG ou WebP.");
        }
        salvarEvidencia(lancamento, nomeArquivo, conteudo, usuario);
        return evidenciaRepository.listarPorLancamento(lancamentoId);
    }

    @Transactional(readOnly = true)
    public EvidenciaPagamento buscarArquivoEvidencia(Long lancamentoId, Long evidenciaId) {
        buscarLancamento(lancamentoId);
        return evidenciaRepository.buscarArquivo(lancamentoId, evidenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Foto de evidência não encontrada."));
    }

    @Transactional
    public void excluirEvidencia(Long lancamentoId, Long evidenciaId) {
        buscarLancamento(lancamentoId);
        EvidenciaPagamento evidencia = evidenciaRepository.buscarArquivo(lancamentoId, evidenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Foto de evidência não encontrada."));
        evidencia.setDataDelecao(LocalDateTime.now());
        evidencia.setDataAtualizacao(LocalDateTime.now());
        evidenciaRepository.save(evidencia);
        auditoriaService.registrar("evidencia_pagamento", evidencia.getId(), "EXCLUSAO", "ATIVO", "EXCLUIDO");
    }

    private LancamentoCusto buscarLancamento(Long lancamentoId) {
        return lancamentoRepository.findById(lancamentoId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Lançamento não encontrado."));
    }

    private void validarFoto(MultipartFile arquivo) {
        if (arquivo == null || arquivo.isEmpty()) {
            throw erro(HttpStatus.BAD_REQUEST, "Um dos arquivos enviados está vazio.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO_FOTO) {
            throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "Cada foto deve ter no máximo 10 MB.");
        }
        String tipo = detectarTipo(arquivo);
        if (tipo == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Envie fotos nos formatos JPEG, PNG ou WebP.");
        }
    }

    private String detectarTipo(MultipartFile arquivo) {
        try {
            return detectarTipo(arquivo.getBytes());
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não foi possível ler uma das fotos enviadas.", exception);
        }
    }

    private String detectarTipo(byte[] arquivo) {
        if (arquivo.length >= 8
                && (arquivo[0] & 0xff) == 0x89 && arquivo[1] == 0x50 && arquivo[2] == 0x4e
                && arquivo[3] == 0x47 && arquivo[4] == 0x0d && arquivo[5] == 0x0a
                && arquivo[6] == 0x1a && arquivo[7] == 0x0a) {
            return "image/png";
        }
        if (arquivo.length >= 3 && (arquivo[0] & 0xff) == 0xff
                && (arquivo[1] & 0xff) == 0xd8 && (arquivo[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (arquivo.length >= 12 && arquivo[0] == 'R' && arquivo[1] == 'I'
                && arquivo[2] == 'F' && arquivo[3] == 'F'
                && arquivo[8] == 'W' && arquivo[9] == 'E'
                && arquivo[10] == 'B' && arquivo[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "foto";
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        return nome.isEmpty() ? "foto" : nome.substring(0, Math.min(nome.length(), 255));
    }

    private void salvarEvidencia(
            LancamentoCusto lancamento,
            String nomeOriginal,
            byte[] conteudo,
            String usuario
    ) {
        EvidenciaPagamento evidencia = new EvidenciaPagamento();
        evidencia.setLancamentoCusto(lancamento);
        evidencia.setNomeArquivo(nomeSeguro(nomeOriginal));
        evidencia.setTipo(detectarTipo(conteudo));
        evidencia.setTamanho((long) conteudo.length);
        evidencia.setArquivo(conteudo);
        evidencia.setDataCriacao(LocalDateTime.now());
        evidencia.setCriadoPor(usuario);
        evidencia.setStatusRegistro(StatusRegEnum.ATIVO);
        EvidenciaPagamento salva = evidenciaRepository.save(evidencia);
        auditoriaService.registrar("evidencia_pagamento", salva.getId(), "UPLOAD", null,
                "lancamento=" + lancamento.getId() + ", arquivo=" + salva.getNomeArquivo());
    }

    private void positivo(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
