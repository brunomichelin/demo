package com.example.demo.services.impl;

import com.example.demo.dtos.LancamentoCustoRequest;
import com.example.demo.dtos.ValidacaoLancamentoRequest;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import com.example.demo.repositories.DocObraRepository;
import com.example.demo.models.DocObra;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
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

    public LancamentoCustoService(
            LancamentoCustoRepository lancamentoRepository,
            EtapaRepository etapaRepository,
            InsumoRepository insumoRepository,
            TarefaRepository tarefaRepository,
            FornecedorRepository fornecedorRepository,
            AuditoriaService auditoriaService,
            DocObraRepository docObraRepository
    ) {
        this.lancamentoRepository = lancamentoRepository;
        this.etapaRepository = etapaRepository;
        this.insumoRepository = insumoRepository;
        this.tarefaRepository = tarefaRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.auditoriaService = auditoriaService;
        this.docObraRepository = docObraRepository;
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

    private void positivo(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
