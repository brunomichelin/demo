package com.example.demo.dtos;

import com.example.demo.models.StatusEntregaEnum;

import java.time.LocalDate;

public record EntregaListaResponse(
        Long id,
        Long etapaId,
        Long etapaOrdem,
        String etapaNome,
        LocalDate dataReal,
        String responsavelNome,
        String observacoes,
        StatusEntregaEnum status
) {
}
