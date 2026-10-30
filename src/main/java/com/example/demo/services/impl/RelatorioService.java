package com.example.demo.services.impl;

import com.example.demo.dtos.RelatorioCustoEtapaResponse;
import com.example.demo.dtos.RelatorioCronogramaResponse;
import com.example.demo.dtos.RelatorioInsumoResponse;
import com.example.demo.dtos.RelatorioTarefasResponse;
import com.example.demo.dtos.RelatorioPrazoEntregaResponse;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
public class RelatorioService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    @Value("${obra.limite-alerta-orcamento:0.90}")
    private BigDecimal limiteAlertaOrcamento;

    private final ObraRepository obraRepository;
    private final EtapaRepository etapaRepository;
    private final OrcamentoRepository orcamentoRepository;
    private final ItemOrcamentoRepository itemRepository;
    private final LancamentoCustoRepository lancamentoRepository;
    private final TarefaRepository tarefaRepository;
    private final EntregaRepository entregaRepository;

    public RelatorioService(
            ObraRepository obraRepository,
            EtapaRepository etapaRepository,
            OrcamentoRepository orcamentoRepository,
            ItemOrcamentoRepository itemRepository,
            LancamentoCustoRepository lancamentoRepository,
            TarefaRepository tarefaRepository,
            EntregaRepository entregaRepository
    ) {
        this.obraRepository = obraRepository;
        this.etapaRepository = etapaRepository;
        this.orcamentoRepository = orcamentoRepository;
        this.itemRepository = itemRepository;
        this.lancamentoRepository = lancamentoRepository;
        this.tarefaRepository = tarefaRepository;
        this.entregaRepository = entregaRepository;
    }

    @Transactional(readOnly = true)
    public RelatorioCustoEtapaResponse custosPorEtapa(Long obraId, Long versaoId) {
        Obra obra = obraRepository.findById(obraId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra não encontrada."));
        Orcamento versao = selecionarVersao(obraId, versaoId);
        List<Etapa> etapas = etapaRepository.findByObraIdOrderByOrdemAsc(obraId);
        Map<Long, BigDecimal> orcados = new HashMap<>();
        if (versao != null) {
            for (ItemOrcamento item : itemRepository.findByOrcamentoId(versao.getId())) {
                orcados.merge(item.getEtapa().getId(), multiplicar(item.getQuantidade(), item.getValorUnitario()),
                        BigDecimal::add);
            }
        }
        Map<Long, BigDecimal> realizados = new HashMap<>();
        for (LancamentoCusto lancamento : lancamentoRepository.findAllByObraId(obraId)) {
            if (lancamento.getStatus() == StatusLancamentoEnum.VALIDADO) {
                realizados.merge(lancamento.getEtapa().getId(),
                        multiplicar(lancamento.getQuantidade(), lancamento.getValorUnitario()), BigDecimal::add);
            }
        }
        Map<Long, List<Tarefa>> tarefas = tarefaRepository.findAllByObraId(obraId).stream()
                .collect(Collectors.groupingBy(tarefa -> tarefa.getEtapa().getId()));

        BigDecimal totalOrcado = ZERO;
        BigDecimal totalRealizado = ZERO;
        int totalTarefas = 0;
        List<RelatorioCustoEtapaResponse.Etapa> linhas = new ArrayList<>();
        for (Etapa etapa : etapas) {
            BigDecimal orcado = moeda(orcados.getOrDefault(etapa.getId(), ZERO));
            BigDecimal realizado = moeda(realizados.getOrDefault(etapa.getId(), ZERO));
            BigDecimal desvio = moeda(realizado.subtract(orcado));
            BigDecimal percentualDesvio = orcado.signum() == 0 ? null
                    : desvio.multiply(BigDecimal.valueOf(100)).divide(orcado, 2, RoundingMode.HALF_UP);
            List<Tarefa> tarefasEtapa = tarefas.getOrDefault(etapa.getId(), List.of());
            int fazer = contar(tarefasEtapa, StatusCronEnum.PLANEJAMENTO);
            int andamento = contar(tarefasEtapa, StatusCronEnum.EM_ANDAMENTO);
            int bloqueada = contar(tarefasEtapa, StatusCronEnum.EM_BLOQUEIO);
            int concluida = contar(tarefasEtapa, StatusCronEnum.CONCLUIDA);
            int quantidade = tarefasEtapa.size();
            BigDecimal percentualConclusao = quantidade == 0 ? ZERO
                    : BigDecimal.valueOf(concluida * 100L).divide(BigDecimal.valueOf(quantidade), 2,
                    RoundingMode.HALF_UP);
            linhas.add(new RelatorioCustoEtapaResponse.Etapa(
                    etapa.getId(), etapa.getOrdem(), etapa.getNome(), etapa.getStatus(), orcado, realizado, desvio,
                    percentualDesvio,
                    new RelatorioCustoEtapaResponse.Tarefas(quantidade, fazer, andamento, bloqueada, concluida),
                    percentualConclusao,
                    orcado.signum() > 0 && realizado.compareTo(orcado.multiply(limiteAlertaOrcamento)) >= 0));
            totalOrcado = totalOrcado.add(orcado);
            totalRealizado = totalRealizado.add(realizado);
            totalTarefas += quantidade;
        }
        return new RelatorioCustoEtapaResponse(obra.getId(), obra.getNome(), LocalDateTime.now(),
                versao == null ? null : versao.getId(), linhas,
                new RelatorioCustoEtapaResponse.Totais(moeda(totalOrcado), moeda(totalRealizado), totalTarefas));
    }

    @Transactional(readOnly = true)
    public List<RelatorioTarefasResponse> tarefasPorEtapa(Long obraId) {
        List<Etapa> etapas = listarEtapas(obraId);
        Map<Long, List<Tarefa>> porEtapa = tarefaRepository.findAllByObraId(obraId).stream()
                .collect(Collectors.groupingBy(tarefa -> tarefa.getEtapa().getId()));
        return etapas.stream().map(etapa -> {
            List<Tarefa> tarefas = porEtapa.getOrDefault(etapa.getId(), List.of());
            int total = tarefas.size();
            int concluida = contar(tarefas, StatusCronEnum.CONCLUIDA);
            return new RelatorioTarefasResponse(etapa.getId(), etapa.getNome(), total,
                    contar(tarefas, StatusCronEnum.PLANEJAMENTO),
                    contar(tarefas, StatusCronEnum.EM_ANDAMENTO),
                    contar(tarefas, StatusCronEnum.EM_BLOQUEIO),
                    concluida,
                    total == 0 ? 0 : BigDecimal.valueOf(concluida * 100L)
                            .divide(BigDecimal.valueOf(total), 2, RoundingMode.HALF_UP).doubleValue());
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<RelatorioCronogramaResponse> cronogramaPorEtapa(Long obraId) {
        List<Etapa> etapas = listarEtapas(obraId);
        Map<Long, List<Tarefa>> tarefasPorEtapa = tarefaRepository.findAllByObraId(obraId).stream()
                .collect(Collectors.groupingBy(tarefa -> tarefa.getEtapa().getId()));
        Map<Long, Entrega> entregasPorEtapa = entregaRepository.findAllByObraId(obraId).stream()
                .collect(Collectors.toMap(entrega -> entrega.getEtapa().getId(), Function.identity()));

        List<RelatorioCronogramaResponse> cronograma = new ArrayList<>();
        for (Etapa etapa : etapas) {
            Entrega entrega = entregasPorEtapa.get(etapa.getId());
            LocalDate dataConclusaoEtapa = etapa.getStatus() == StatusCronEnum.CONCLUIDA && entrega != null
                    ? entrega.getDataReal() : null;
            cronograma.add(new RelatorioCronogramaResponse("Etapa: " + etapa.getNome(),
                    etapa.getDataInicioPrevista(), etapa.getDataEntregaPrevista(), dataConclusaoEtapa,
                    etapa.getStatus()));

            tarefasPorEtapa.getOrDefault(etapa.getId(), List.of()).stream()
                    .sorted(Comparator.comparing(Tarefa::getDataInicioPrevista,
                                    Comparator.nullsLast(Comparator.naturalOrder()))
                            .thenComparing(Tarefa::getTitulo, Comparator.nullsLast(Comparator.naturalOrder())))
                    .map(tarefa -> new RelatorioCronogramaResponse("    " + tarefa.getTitulo(),
                            tarefa.getDataInicioPrevista(), tarefa.getDataFimPrevista(),
                            tarefa.getStatus() == StatusCronEnum.CONCLUIDA && tarefa.getStatusAtualizadoEm() != null
                                    ? tarefa.getStatusAtualizadoEm().toLocalDate() : null,
                            tarefa.getStatus()))
                    .forEach(cronograma::add);
        }
        return cronograma;
    }

    @Transactional(readOnly = true)
    public List<RelatorioInsumoResponse> insumosPorEtapa(Long obraId, Long versaoId) {
        List<Etapa> etapas = listarEtapas(obraId);
        Orcamento versao = selecionarVersao(obraId, versaoId);
        Map<Long, Etapa> etapasPorId = etapas.stream().collect(Collectors.toMap(Etapa::getId, Function.identity()));
        Map<ChaveInsumo, AcumuladoInsumo> acumulados = new TreeMap<>(Comparator
                .comparing(ChaveInsumo::etapaId).thenComparing(ChaveInsumo::insumoId));
        if (versao != null) {
            for (ItemOrcamento item : itemRepository.findByOrcamentoId(versao.getId())) {
                ChaveInsumo chave = new ChaveInsumo(item.getEtapa().getId(), item.getInsumo().getId());
                AcumuladoInsumo valor = acumulados.computeIfAbsent(chave, ignored -> new AcumuladoInsumo());
                valor.quantidadeOrcada = valor.quantidadeOrcada.add(item.getQuantidade());
                valor.valorOrcado = valor.valorOrcado.add(multiplicar(item.getQuantidade(), item.getValorUnitario()));
                valor.item = item;
            }

        }
        for (LancamentoCusto lancamento : lancamentoRepository.findAllByObraId(obraId)) {
            if (lancamento.getStatus() != StatusLancamentoEnum.VALIDADO) {
                continue;
            }
            ChaveInsumo chave = new ChaveInsumo(lancamento.getEtapa().getId(), lancamento.getInsumo().getId());
            AcumuladoInsumo valor = acumulados.computeIfAbsent(chave, ignored -> new AcumuladoInsumo());
            valor.quantidadeRealizada = valor.quantidadeRealizada.add(lancamento.getQuantidade());
            valor.valorRealizado = valor.valorRealizado.add(
                    multiplicar(lancamento.getQuantidade(), lancamento.getValorUnitario()));
            valor.insumo = lancamento.getInsumo();
        }
        return acumulados.entrySet().stream().map(entry -> {
            AcumuladoInsumo acumulado = entry.getValue();
            ItemOrcamento item = acumulado.item;
            Insumo insumo = acumulado.insumo != null ? acumulado.insumo : item.getInsumo();
            Etapa etapa = etapasPorId.get(entry.getKey().etapaId());
            return new RelatorioInsumoResponse(etapa.getId(), etapa.getNome(), insumo.getId(),
                    insumo.getDescricao(), insumo.getTipo(),
                    insumo.getUnidade() == null ? null : insumo.getUnidade().name(),
                    quantidade(acumulado.quantidadeOrcada), quantidade(acumulado.quantidadeRealizada),
                    moeda(acumulado.valorOrcado), moeda(acumulado.valorRealizado));
        }).toList();
    }

    @Transactional(readOnly = true)
    public List<RelatorioPrazoEntregaResponse> prazosEntrega(Long obraId) {
        List<Etapa> etapas = listarEtapas(obraId);
        Map<Long, Entrega> entregas = entregaRepository.findAllByObraId(obraId).stream()
                .collect(Collectors.toMap(entrega -> entrega.getEtapa().getId(), Function.identity()));
        LocalDate hoje = LocalDate.now();
        return etapas.stream().map(etapa -> {
            Entrega entrega = entregas.get(etapa.getId());
            LocalDate dataPrevista = etapa.getDataEntregaPrevista();
            LocalDate dataReal = entrega == null ? null : entrega.getDataReal();
            LocalDate referencia = dataReal == null ? hoje : dataReal;
            Long atraso = dataPrevista == null ? null
                    : Math.max(0, ChronoUnit.DAYS.between(dataPrevista, referencia));
            return new RelatorioPrazoEntregaResponse(etapa.getId(), etapa.getNome(), dataPrevista,
                    dataReal, atraso, entrega == null ? null : entrega.getStatus());
        }).toList();
    }

    private List<Etapa> listarEtapas(Long obraId) {
        if (!obraRepository.existsById(obraId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra não encontrada.");
        }
        return etapaRepository.findByObraIdOrderByOrdemAsc(obraId);
    }

    private Orcamento selecionarVersao(Long obraId, Long versaoId) {
        List<Orcamento> versoes = orcamentoRepository.findByObraIdOrderByNumeroAsc(obraId);
        if (versaoId != null) {
            return versoes.stream().filter(v -> v.getId().equals(versaoId)).findFirst()
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                            "Versão de orçamento não encontrada para esta obra."));
        }
        return versoes.isEmpty() ? null : versoes.get(versoes.size() - 1);
    }

    private int contar(List<Tarefa> tarefas, StatusCronEnum status) {
        return (int) tarefas.stream().filter(tarefa -> tarefa.getStatus() == status).count();
    }

    private BigDecimal multiplicar(BigDecimal quantidade, BigDecimal valor) {
        return quantidade.multiply(valor).setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal moeda(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal quantidade(BigDecimal valor) {
        return valor.setScale(3, RoundingMode.HALF_UP);
    }

    private record ChaveInsumo(Long etapaId, Long insumoId) {
    }

    private static class AcumuladoInsumo {
        private BigDecimal quantidadeOrcada = BigDecimal.ZERO;
        private BigDecimal quantidadeRealizada = BigDecimal.ZERO;
        private BigDecimal valorOrcado = BigDecimal.ZERO;
        private BigDecimal valorRealizado = BigDecimal.ZERO;
        private ItemOrcamento item;
        private Insumo insumo;
    }
}
