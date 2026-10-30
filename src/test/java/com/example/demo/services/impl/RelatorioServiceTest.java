package com.example.demo.services.impl;

import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class RelatorioServiceTest {
    private ObraRepository obraRepository;
    private EtapaRepository etapaRepository;
    private OrcamentoRepository orcamentoRepository;
    private ItemOrcamentoRepository itemRepository;
    private LancamentoCustoRepository lancamentoRepository;
    private TarefaRepository tarefaRepository;
    private EntregaRepository entregaRepository;
    private RelatorioService service;

    @BeforeEach
    void setUp() {
        obraRepository = mock(ObraRepository.class);
        etapaRepository = mock(EtapaRepository.class);
        orcamentoRepository = mock(OrcamentoRepository.class);
        itemRepository = mock(ItemOrcamentoRepository.class);
        lancamentoRepository = mock(LancamentoCustoRepository.class);
        tarefaRepository = mock(TarefaRepository.class);
        entregaRepository = mock(EntregaRepository.class);
        service = new RelatorioService(obraRepository, etapaRepository, orcamentoRepository,
                itemRepository, lancamentoRepository, tarefaRepository, entregaRepository);
        ReflectionTestUtils.setField(service, "limiteAlertaOrcamento", new BigDecimal("0.90"));
    }

    @Test
    void calculaCustosStatusDesvioConclusaoEIgnoraLancamentosPendentes() {
        Obra obra = new Obra();
        obra.setId(12L);
        obra.setNome("Residencial Aurora");
        Etapa etapa = etapa(31L, obra);
        Orcamento orcamento = new Orcamento();
        orcamento.setId(7L);
        orcamento.setNumero(1L);
        ItemOrcamento item = new ItemOrcamento();
        item.setEtapa(etapa);
        item.setQuantidade(new BigDecimal("100"));
        item.setValorUnitario(new BigDecimal("1000"));
        LancamentoCusto validado = lancamento(etapa, "104000.50", StatusLancamentoEnum.VALIDADO);
        LancamentoCusto pendente = lancamento(etapa, "5000.00", StatusLancamentoEnum.PENDENTE);

        when(obraRepository.findById(12L)).thenReturn(Optional.of(obra));
        when(orcamentoRepository.findByObraIdOrderByNumeroAsc(12L)).thenReturn(List.of(orcamento));
        when(etapaRepository.findByObraIdOrderByOrdemAsc(12L)).thenReturn(List.of(etapa));
        when(itemRepository.findByOrcamentoId(7L)).thenReturn(List.of(item));
        when(lancamentoRepository.findAllByObraId(12L)).thenReturn(List.of(validado, pendente));
        when(tarefaRepository.findAllByObraId(12L)).thenReturn(tarefas(etapa, 7, 2, 1));

        var report = service.custosPorEtapa(12L, null);
        var linha = report.etapas().getFirst();
        assertEquals(new BigDecimal("100000.00"), linha.custoOrcado());
        assertEquals(new BigDecimal("104000.50"), linha.custoRealizado());
        assertEquals(new BigDecimal("4000.50"), linha.desvioValor());
        assertEquals(new BigDecimal("4.00"), linha.desvioPercentual());
        assertEquals(10, linha.tarefas().total());
        assertEquals(7, linha.tarefas().concluida());
        assertEquals(new BigDecimal("70.00"), linha.percentualConclusao());
        assertEquals(7L, report.versaoOrcamentoId());
        assertEquals(true, linha.alertaOrcamento());
    }

    @Test
    void retornaTotaisZeroEPercentualZeroParaEtapaSemTarefasOuOrcamento() {
        Obra obra = new Obra();
        obra.setId(4L);
        Etapa etapa = etapa(9L, obra);
        when(obraRepository.findById(4L)).thenReturn(Optional.of(obra));
        when(orcamentoRepository.findByObraIdOrderByNumeroAsc(4L)).thenReturn(List.of());
        when(etapaRepository.findByObraIdOrderByOrdemAsc(4L)).thenReturn(List.of(etapa));
        when(lancamentoRepository.findAllByObraId(4L)).thenReturn(List.of());
        when(tarefaRepository.findAllByObraId(4L)).thenReturn(List.of());

        var report = service.custosPorEtapa(4L, null);
        var linha = report.etapas().getFirst();
        assertEquals(BigDecimal.ZERO.setScale(2), linha.custoOrcado());
        assertEquals(BigDecimal.ZERO.setScale(2), linha.custoRealizado());
        assertEquals(null, linha.desvioPercentual());
        assertEquals(BigDecimal.ZERO.setScale(2), linha.percentualConclusao());
        assertEquals(0, linha.tarefas().total());
        assertEquals(false, linha.alertaOrcamento());
    }

    @Test
    void cronogramaIncluiEtapasETarefasComDatasPrevistasEConclusaoReal() {
        Etapa etapa = etapa(21L, new Obra());
        etapa.setDataInicioPrevista(LocalDate.of(2026, 1, 5));
        etapa.setDataEntregaPrevista(LocalDate.of(2026, 1, 20));
        etapa.setStatus(StatusCronEnum.CONCLUIDA);

        Tarefa tarefa = tarefa(etapa, StatusCronEnum.CONCLUIDA);
        tarefa.setTitulo("Concretar fundação");
        tarefa.setDataInicioPrevista(LocalDate.of(2026, 1, 7));
        tarefa.setDataFimPrevista(LocalDate.of(2026, 1, 12));
        tarefa.setStatusAtualizadoEm(LocalDateTime.of(2026, 1, 11, 10, 0));

        Entrega entrega = new Entrega();
        entrega.setEtapa(etapa);
        entrega.setDataReal(LocalDate.of(2026, 1, 19));

        when(obraRepository.existsById(12L)).thenReturn(true);
        when(etapaRepository.findByObraIdOrderByOrdemAsc(12L)).thenReturn(List.of(etapa));
        when(tarefaRepository.findAllByObraId(12L)).thenReturn(List.of(tarefa));
        when(entregaRepository.findAllByObraId(12L)).thenReturn(List.of(entrega));

        var cronograma = service.cronogramaPorEtapa(12L);

        assertEquals(2, cronograma.size());
        assertEquals("Etapa: Fundação", cronograma.get(0).nome());
        assertEquals(LocalDate.of(2026, 1, 5), cronograma.get(0).inicioPrevisto());
        assertEquals(LocalDate.of(2026, 1, 20), cronograma.get(0).fimPrevisto());
        assertEquals(LocalDate.of(2026, 1, 19), cronograma.get(0).dataConclusaoReal());
        assertEquals("    Concretar fundação", cronograma.get(1).nome());
        assertEquals(LocalDate.of(2026, 1, 12), cronograma.get(1).fimPrevisto());
        assertEquals(LocalDate.of(2026, 1, 11), cronograma.get(1).dataConclusaoReal());
    }

    private Etapa etapa(Long id, Obra obra) {
        Etapa etapa = new Etapa();
        etapa.setId(id);
        etapa.setObra(obra);
        etapa.setOrdem(1L);
        etapa.setNome("Fundação");
        etapa.setStatus(StatusCronEnum.EM_ANDAMENTO);
        return etapa;
    }

    private LancamentoCusto lancamento(Etapa etapa, String total, StatusLancamentoEnum status) {
        LancamentoCusto lancamento = new LancamentoCusto();
        lancamento.setEtapa(etapa);
        lancamento.setQuantidade(BigDecimal.ONE);
        lancamento.setValorUnitario(new BigDecimal(total));
        lancamento.setStatus(status);
        lancamento.setDataLancamento(LocalDate.now());
        return lancamento;
    }

    private List<Tarefa> tarefas(Etapa etapa, int concluidas, int andamento, int bloqueadas) {
        java.util.ArrayList<Tarefa> tarefas = new java.util.ArrayList<>();
        for (int i = 0; i < concluidas; i++) {
            tarefas.add(tarefa(etapa, StatusCronEnum.CONCLUIDA));
        }
        for (int i = 0; i < andamento; i++) {
            tarefas.add(tarefa(etapa, StatusCronEnum.EM_ANDAMENTO));
        }
        for (int i = 0; i < bloqueadas; i++) {
            tarefas.add(tarefa(etapa, StatusCronEnum.EM_BLOQUEIO));
        }
        return tarefas;
    }

    private Tarefa tarefa(Etapa etapa, StatusCronEnum status) {
        Tarefa tarefa = new Tarefa();
        tarefa.setEtapa(etapa);
        tarefa.setStatus(status);
        return tarefa;
    }
}
