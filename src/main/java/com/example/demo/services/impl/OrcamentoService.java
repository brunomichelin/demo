package com.example.demo.services.impl;

import com.example.demo.dtos.ItemOrcamentoRequest;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class OrcamentoService {
    private final OrcamentoRepository orcamentoRepository;
    private final ObraRepository obraRepository;
    private final EtapaRepository etapaRepository;
    private final TarefaRepository tarefaRepository;
    private final InsumoRepository insumoRepository;
    private final ItemOrcamentoRepository itemRepository;
    private final AuditoriaService auditoriaService;

    public OrcamentoService(
            OrcamentoRepository orcamentoRepository,
            ObraRepository obraRepository,
            EtapaRepository etapaRepository,
            TarefaRepository tarefaRepository,
            InsumoRepository insumoRepository,
            ItemOrcamentoRepository itemRepository,
            AuditoriaService auditoriaService
    ) {
        this.orcamentoRepository = orcamentoRepository;
        this.obraRepository = obraRepository;
        this.etapaRepository = etapaRepository;
        this.tarefaRepository = tarefaRepository;
        this.insumoRepository = insumoRepository;
        this.itemRepository = itemRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional(readOnly = true)
    public List<Orcamento> listarVersoes(Long obraId) {
        validarObra(obraId);
        return orcamentoRepository.findByObraIdOrderByNumeroAsc(obraId);
    }

    @Transactional
    public Orcamento criarVersao(Long obraId) {
        Obra obra = validarObra(obraId);
        List<Orcamento> versoes = orcamentoRepository.findByObraIdOrderByNumeroAsc(obraId);
        Orcamento anterior = versoes.isEmpty() ? null : versoes.get(versoes.size() - 1);
        Orcamento versao = new Orcamento();
        versao.setObra(obra);
        versao.setNumero(anterior == null ? 1 : anterior.getNumero() + 1);
        versao.setCongelada(false);
        versao.setDataCriacao(LocalDateTime.now());
        Orcamento salva = orcamentoRepository.save(versao);
        auditoriaService.registrar("orcamento", salva.getId(), "CRIACAO_VERSAO", null,
                "numero=" + salva.getNumero());
        return salva;
    }

    @Transactional
    public ItemOrcamento adicionarItem(Long etapaId, Long orcamentoId, ItemOrcamentoRequest request) {
        if (request == null || request.insumoId() == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um insumo válido.");
        }
        validarPositivo(request.quantidade(), "A quantidade deve ser maior que zero.");
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        Orcamento orcamento = orcamentoRepository.findById(orcamentoId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Versão de orçamento não encontrada."));
        if (Boolean.TRUE.equals(orcamento.getCongelada())) {
            throw erro(HttpStatus.CONFLICT, "A versão congelada não pode ser alterada.");
        }
        if (!orcamento.getObra().getId().equals(etapa.getObra().getId())) {
            throw erro(HttpStatus.BAD_REQUEST, "A etapa e o orçamento devem pertencer à mesma obra.");
        }
        Insumo insumo = insumoRepository.findById(request.insumoId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Insumo não encontrado."));
        BigDecimal valor = request.valorUnitario() == null ? insumo.getPrecoReferencia() : request.valorUnitario();
        validarPositivo(valor, "O preço unitário deve ser maior que zero.");

        Tarefa tarefa = null;
        if (request.tarefaId() != null) {
            tarefa = tarefaRepository.findById(request.tarefaId())
                    .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
            if (!etapa.getId().equals(tarefa.getEtapa().getId())) {
                throw erro(HttpStatus.BAD_REQUEST, "A tarefa deve pertencer à etapa informada.");
            }
        }

        ItemOrcamento item = new ItemOrcamento();
        item.setOrcamento(orcamento);
        item.setEtapa(etapa);
        item.setTarefa(tarefa);
        item.setInsumo(insumo);
        item.setQuantidade(request.quantidade());
        item.setValorUnitario(valor);
        item.setDataCriacao(LocalDateTime.now());
        ItemOrcamento salvo = itemRepository.save(item);
        auditoriaService.registrar("item_orcamento", salvo.getId(), "INCLUSAO", null,
                "etapa=" + etapaId + ", insumo=" + insumo.getId() + ", quantidade=" + request.quantidade()
                        + ", valorUnitario=" + valor);
        return salvo;
    }

    @Transactional
    public Orcamento congelar(Long id) {
        Orcamento orcamento = orcamentoRepository.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Versão de orçamento não encontrada."));
        boolean anterior = Boolean.TRUE.equals(orcamento.getCongelada());
        orcamento.setCongelada(true);
        orcamento.setDataAtualizacao(LocalDateTime.now());
        Orcamento salvo = orcamentoRepository.save(orcamento);
        if (!anterior) {
            auditoriaService.registrar("orcamento", salvo.getId(), "CONGELAMENTO", "congelada=false",
                    "congelada=true");
        }
        return salvo;
    }

    private Obra validarObra(Long id) {
        return obraRepository.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Obra não encontrada."));
    }

    private void validarPositivo(BigDecimal valor, String mensagem) {
        if (valor == null || valor.signum() <= 0) {
            throw erro(HttpStatus.BAD_REQUEST, mensagem);
        }
    }

    static ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
