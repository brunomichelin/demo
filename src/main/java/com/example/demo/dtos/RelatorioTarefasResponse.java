package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;

public record RelatorioTarefasResponse(
        Long etapaId,
        String etapa,
        int total,
        int aFazer,
        int emAndamento,
        int bloqueada,
        int concluida,
        double percentualConclusao
) {
}
