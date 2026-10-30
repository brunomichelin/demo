package com.example.demo.dtos;

import com.example.demo.models.StatusEntregaEnum;

import java.time.LocalDate;

public record RelatorioPrazoEntregaResponse(
        Long etapaId,
        String etapa,
        LocalDate dataPrevista,
        LocalDate dataReal,
        Long diasAtraso,
        StatusEntregaEnum situacao
) {
}
