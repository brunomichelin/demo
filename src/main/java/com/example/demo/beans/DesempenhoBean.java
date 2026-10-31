package com.example.demo.beans;

import com.example.demo.config.ObraAuthorization;
import com.example.demo.dtos.RelatorioCronogramaResponse;
import com.example.demo.dtos.RelatorioTarefasResponse;
import com.example.demo.models.ItemOrcamento;
import com.example.demo.models.LancamentoCusto;
import com.example.demo.models.Obra;
import com.example.demo.models.Orcamento;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.StatusLancamentoEnum;
import com.example.demo.models.Tarefa;
import com.example.demo.repositories.ItemOrcamentoRepository;
import com.example.demo.repositories.LancamentoCustoRepository;
import com.example.demo.repositories.OrcamentoRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.services.impl.ObraService;
import com.example.demo.services.impl.RelatorioService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.model.charts.ChartData;
import org.primefaces.model.charts.bar.BarChartDataSet;
import org.primefaces.model.charts.bar.BarChartModel;
import org.primefaces.model.charts.bar.BarChartOptions;
import org.primefaces.model.charts.axes.cartesian.CartesianScaleTitle;
import org.primefaces.model.charts.axes.cartesian.CartesianScales;
import org.primefaces.model.charts.axes.cartesian.linear.CartesianLinearAxes;
import org.primefaces.model.charts.donut.DonutChartDataSet;
import org.primefaces.model.charts.donut.DonutChartModel;
import org.primefaces.model.charts.donut.DonutChartOptions;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component("desempenhoBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class DesempenhoBean {

    @Getter
    private List<Obra> obras = new ArrayList<>();

    @Getter
    @Setter
    private Long obraId;

    @Getter
    private BarChartModel progressoPorEtapa;

    @Getter
    private DonutChartModel tarefasPorStatus;

    @Getter
    private BarChartModel cronograma;

    @Getter
    private BarChartModel orcamentoCustosPorEtapa;

    @Getter
    private BarChartModel orcamentoCustosPorTarefa;

    @Getter
    private boolean possuiTarefasFinanceiras;

    @Getter
    private int alturaGraficoFinanceiroTarefas;

    @Getter
    private Long numeroVersaoOrcamentoFinanceiro;

    @Getter
    private boolean possuiCronograma;

    @Getter
    private int itensSemPeriodoCronograma;

    @Getter
    private int alturaGraficoCronograma;

    @Getter
    private int totalEtapas;

    @Getter
    private int totalTarefas;

    @Getter
    private int tarefasConcluidas;

    @Getter
    private int tarefasEmAtencao;

    @Getter
    private int percentualAvanco;

    @Getter
    private boolean dadosCarregados;

    @Getter
    private boolean possuiEtapas;

    @Autowired
    private ObraService obraService;

    @Autowired
    private RelatorioService relatorioService;

    @Autowired
    private ObraAuthorization obraAuthorization;

    @Autowired
    private OrcamentoRepository orcamentoRepository;

    @Autowired
    private ItemOrcamentoRepository itemOrcamentoRepository;

    @Autowired
    private LancamentoCustoRepository lancamentoCustoRepository;

    @Autowired
    private TarefaRepository tarefaRepository;

    @PostConstruct
    public void init() {
        obras = obraService.listarObras(Pageable.unpaged()).getContent();
        String obraInformada = FacesContext.getCurrentInstance()
                .getExternalContext().getRequestParameterMap().get("obraId");
        if (obraInformada != null && !obraInformada.isBlank()) {
            try {
                obraId = Long.valueOf(obraInformada);
            } catch (NumberFormatException exception) {
                avisar("O identificador da obra informado é inválido.");
                return;
            }
            if (!obraDisponivel(obraId)) {
                obraId = null;
                avisar("A obra solicitada não está disponível para seu usuário.");
                return;
            }
        } else if (!obras.isEmpty()) {
            obraId = obras.get(0).getId();
        }

        if (obraId != null) {
            carregar();
        } else if (obras.isEmpty()) {
            avisar("Não há obras disponíveis para seu usuário.");
        }
    }

    public void carregar() {
        dadosCarregados = false;
        if (obraId == null || !obraDisponivel(obraId)
                || !obraAuthorization.podeAcessarObra(obraId,
                SecurityContextHolder.getContext().getAuthentication())) {
            obraId = null;
            avisar("Selecione uma obra à qual você tenha acesso.");
            return;
        }

        List<RelatorioTarefasResponse> relatorio = relatorioService.tarefasPorEtapa(obraId);
        totalEtapas = relatorio.size();
        totalTarefas = relatorio.stream().mapToInt(RelatorioTarefasResponse::total).sum();
        tarefasConcluidas = relatorio.stream().mapToInt(RelatorioTarefasResponse::concluida).sum();
        tarefasEmAtencao = relatorio.stream()
                .mapToInt(etapa -> etapa.emAndamento() + etapa.bloqueada()).sum();
        percentualAvanco = totalTarefas == 0 ? 0
                : (int) Math.round(tarefasConcluidas * 100.0 / totalTarefas);
        possuiEtapas = !relatorio.isEmpty();
        progressoPorEtapa = criarGraficoEtapas(relatorio);
        tarefasPorStatus = criarGraficoStatus(relatorio);
        criarGraficosFinanceiros(relatorio);
        criarGraficoCronograma(relatorioService.cronogramaPorEtapa(obraId));
        dadosCarregados = true;
    }

    private boolean obraDisponivel(Long id) {
        return obras.stream().anyMatch(obra -> obra.getId().equals(id));
    }

    private BarChartModel criarGraficoEtapas(List<RelatorioTarefasResponse> relatorio) {
        ChartData data = new ChartData();
        data.setLabels(relatorio.stream().map(RelatorioTarefasResponse::etapa).toList());

        BarChartDataSet dataset = new BarChartDataSet();
        dataset.setLabel("Conclusão (%)");
        dataset.setData(relatorio.stream()
                .map(etapa -> (Object) etapa.percentualConclusao())
                .toList());
        dataset.setBackgroundColor("rgba(234, 88, 12, 0.78)");
        dataset.setBorderColor("#c2410c");
        dataset.setBorderWidth(1);
        data.addChartDataSet(dataset);

        BarChartOptions options = new BarChartOptions();
        options.setIndexAxis("y");
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);

        return new BarChartModel(data, options);
    }

    private DonutChartModel criarGraficoStatus(List<RelatorioTarefasResponse> relatorio) {
        ChartData data = new ChartData();
        DonutChartDataSet dataset = new DonutChartDataSet();
        dataset.setData(List.of(
                relatorio.stream().mapToInt(RelatorioTarefasResponse::concluida).sum(),
                relatorio.stream().mapToInt(RelatorioTarefasResponse::emAndamento).sum(),
                relatorio.stream().mapToInt(RelatorioTarefasResponse::aFazer).sum(),
                relatorio.stream().mapToInt(RelatorioTarefasResponse::bloqueada).sum(),
                relatorio.stream().mapToInt(etapa -> Math.max(0, etapa.total()
                        - etapa.concluida() - etapa.emAndamento() - etapa.aFazer() - etapa.bloqueada())).sum()
        ));
        dataset.setBackgroundColor(List.of("#16a34a", "#ea580c", "#64748b", "#dc2626", "#cbd5e1"));
        dataset.setBorderColor(List.of("#ffffff", "#ffffff", "#ffffff", "#ffffff", "#ffffff"));
        dataset.setBorderWidth(List.of(2, 2, 2, 2, 2));
        data.setLabels(List.of("Concluídas", "Em andamento", "A fazer", "Bloqueadas", "Outras"));
        data.addChartDataSet(dataset);

        DonutChartOptions options = new DonutChartOptions();
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);
        return new DonutChartModel(data, options);
    }

    private void criarGraficosFinanceiros(List<RelatorioTarefasResponse> etapas) {
        Map<Long, BigDecimal> orcamentoPorEtapa = new HashMap<>();
        Map<Long, BigDecimal> custoPorEtapa = new HashMap<>();
        Map<Long, BigDecimal> orcamentoPorTarefa = new HashMap<>();
        Map<Long, BigDecimal> custoPorTarefa = new HashMap<>();

        List<Orcamento> versoes = orcamentoRepository.findByObraIdOrderByNumeroAsc(obraId);
        Orcamento versaoMaisRecente = versoes.isEmpty() ? null : versoes.get(versoes.size() - 1);
        numeroVersaoOrcamentoFinanceiro = versaoMaisRecente == null ? null : versaoMaisRecente.getNumero();
        if (versaoMaisRecente != null) {
            for (ItemOrcamento item : itemOrcamentoRepository.findByOrcamentoId(versaoMaisRecente.getId())) {
                BigDecimal total = valorTotal(item.getQuantidade(), item.getValorUnitario());
                orcamentoPorEtapa.merge(item.getEtapa().getId(), total, BigDecimal::add);
                if (item.getTarefa() != null) {
                    orcamentoPorTarefa.merge(item.getTarefa().getId(), total, BigDecimal::add);
                }
            }
        }

        for (LancamentoCusto lancamento : lancamentoCustoRepository.findAllByObraId(obraId)) {
            if (lancamento.getStatus() != StatusLancamentoEnum.VALIDADO) {
                continue;
            }
            BigDecimal total = valorTotal(lancamento.getQuantidade(), lancamento.getValorUnitario());
            custoPorEtapa.merge(lancamento.getEtapa().getId(), total, BigDecimal::add);
            if (lancamento.getTarefa() != null) {
                custoPorTarefa.merge(lancamento.getTarefa().getId(), total, BigDecimal::add);
            }
        }

        orcamentoCustosPorEtapa = criarGraficoFinanceiro(
                etapas.stream().map(RelatorioTarefasResponse::etapa).toList(),
                etapas.stream().map(etapa -> orcamentoPorEtapa.getOrDefault(etapa.etapaId(), BigDecimal.ZERO))
                        .toList(),
                etapas.stream().map(etapa -> custoPorEtapa.getOrDefault(etapa.etapaId(), BigDecimal.ZERO))
                        .toList());

        List<Tarefa> tarefas = tarefaRepository.findAllByObraId(obraId).stream()
                .sorted(Comparator.comparing((Tarefa tarefa) -> tarefa.getEtapa().getOrdem(),
                                Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(Tarefa::getTitulo, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        possuiTarefasFinanceiras = !tarefas.isEmpty();
        alturaGraficoFinanceiroTarefas = Math.max(300, tarefas.size() * 32 + 100);
        orcamentoCustosPorTarefa = criarGraficoFinanceiro(
                tarefas.stream().map(tarefa -> tarefa.getEtapa().getNome() + " - " + tarefa.getTitulo()).toList(),
                tarefas.stream().map(tarefa -> orcamentoPorTarefa.getOrDefault(tarefa.getId(), BigDecimal.ZERO))
                        .toList(),
                tarefas.stream().map(tarefa -> custoPorTarefa.getOrDefault(tarefa.getId(), BigDecimal.ZERO))
                        .toList());
    }

    private BarChartModel criarGraficoFinanceiro(
            List<String> labels,
            List<BigDecimal> orcados,
            List<BigDecimal> custos
    ) {
        ChartData data = new ChartData();
        data.setLabels(labels);

        BarChartDataSet orcadosDataset = new BarChartDataSet();
        orcadosDataset.setLabel("Orçado (versão mais recente)");
        orcadosDataset.setData(orcados.stream().map(valor -> (Object) valor).toList());
        orcadosDataset.setBackgroundColor("rgba(37, 99, 235, 0.78)");
        orcadosDataset.setBorderColor("#1d4ed8");
        orcadosDataset.setBorderWidth(1);
        data.addChartDataSet(orcadosDataset);

        BarChartDataSet custosDataset = new BarChartDataSet();
        custosDataset.setLabel("Custo validado");
        custosDataset.setData(custos.stream().map(valor -> (Object) valor).toList());
        custosDataset.setBackgroundColor("rgba(234, 88, 12, 0.78)");
        custosDataset.setBorderColor("#c2410c");
        custosDataset.setBorderWidth(1);
        data.addChartDataSet(custosDataset);

        BarChartOptions options = new BarChartOptions();
        options.setIndexAxis("y");
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);
        return new BarChartModel(data, options);
    }

    private BigDecimal valorTotal(BigDecimal quantidade, BigDecimal valorUnitario) {
        if (quantidade == null || valorUnitario == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return quantidade.multiply(valorUnitario).setScale(2, RoundingMode.HALF_UP);
    }

    private void criarGraficoCronograma(List<RelatorioCronogramaResponse> itens) {
        List<RelatorioCronogramaResponse> agendados = itens.stream()
                .filter(item -> item.inicioPrevisto() != null && item.fimPrevisto() != null
                        && !item.fimPrevisto().isBefore(item.inicioPrevisto()))
                .toList();
        possuiCronograma = !agendados.isEmpty();
        itensSemPeriodoCronograma = itens.size() - agendados.size();
        if (!possuiCronograma) {
            cronograma = null;
            alturaGraficoCronograma = 360;
            return;
        }

        LocalDate dataBase = agendados.stream()
                .map(RelatorioCronogramaResponse::inicioPrevisto)
                .min(LocalDate::compareTo)
                .orElseThrow();
        ChartData data = new ChartData();
        data.setLabels(agendados.stream().map(RelatorioCronogramaResponse::nome).toList());

        BarChartDataSet dataset = new BarChartDataSet();
        dataset.setLabel("Período previsto");
        dataset.setData(agendados.stream().map(item -> (Object) List.of(
                ChronoUnit.DAYS.between(dataBase, item.inicioPrevisto()),
                ChronoUnit.DAYS.between(dataBase, item.fimPrevisto()) + 1
        )).toList());
        dataset.setBackgroundColor(agendados.stream().map(this::corCronograma).toList());
        dataset.setBorderWidth(0);
        data.addChartDataSet(dataset);

        CartesianScaleTitle tituloEixo = new CartesianScaleTitle();
        tituloEixo.setDisplay(true);
        tituloEixo.setText("Dias desde " + dataBase.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        CartesianLinearAxes eixoHorizontal = new CartesianLinearAxes();
        eixoHorizontal.setBeginAtZero(true);
        eixoHorizontal.setScaleTitle(tituloEixo);
        CartesianScales escalas = new CartesianScales();
        escalas.addXAxesData(eixoHorizontal);

        BarChartOptions options = new BarChartOptions();
        options.setIndexAxis("y");
        options.setResponsive(true);
        options.setMaintainAspectRatio(false);
        options.setScales(escalas);
        cronograma = new BarChartModel(data, options);
        alturaGraficoCronograma = Math.max(360, agendados.size() * 32 + 100);
    }

    private String corCronograma(RelatorioCronogramaResponse item) {
        if (item.status() == StatusCronEnum.CANCELADA || item.status() == StatusCronEnum.PAUSADA) {
            return "#94a3b8";
        }
        if (item.status() == StatusCronEnum.CONCLUIDA) {
            if (item.dataConclusaoReal() == null) {
                return "#94a3b8";
            }
            return item.dataConclusaoReal().isAfter(item.fimPrevisto()) ? "#dc2626" : "#16a34a";
        }
        return LocalDate.now().isAfter(item.fimPrevisto()) ? "#dc2626" : "#ea580c";
    }

    private void avisar(String texto) {
        FacesContext.getCurrentInstance().addMessage(null,
                new FacesMessage(FacesMessage.SEVERITY_WARN, texto, null));
    }
}
