package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;

import java.time.LocalDate;

public record RelatorioCronogramaResponse(
        String nome,
        LocalDate inicioPrevisto,
        LocalDate fimPrevisto,
        LocalDate dataConclusaoReal,
        StatusCronEnum status
) {
}
