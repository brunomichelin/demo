package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;

import java.time.LocalDate;

public record TarefaRequest(
        Long etapaId,
        String titulo,
        String descricao,
        Long responsavelId,
        LocalDate dataInicioPrevista,
        LocalDate dataFimPrevista,
        Long prioridade,
        StatusCronEnum status,
        Long predecessoraId
) {
}
