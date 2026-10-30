package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record RelatorioCustoEtapaResponse(
        Long obraId,
        String obra,
        LocalDateTime geradoEm,
        Long versaoOrcamentoId,
        List<Etapa> etapas,
        Totais totais
) {
    public record Etapa(
            Long etapaId,
            Long ordem,
            String nome,
            StatusCronEnum status,
            BigDecimal custoOrcado,
            BigDecimal custoRealizado,
            BigDecimal desvioValor,
            BigDecimal desvioPercentual,
            Tarefas tarefas,
            BigDecimal percentualConclusao,
            boolean alertaOrcamento
    ) {
    }

    public record Tarefas(
            int total,
            int aFazer,
            int emAndamento,
            int bloqueada,
            int concluida
    ) {
    }

    public record Totais(
            BigDecimal custoOrcado,
            BigDecimal custoRealizado,
            int totalTarefas
    ) {
    }
}
