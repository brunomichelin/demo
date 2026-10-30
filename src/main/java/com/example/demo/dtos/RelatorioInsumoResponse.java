package com.example.demo.dtos;

import java.math.BigDecimal;

public record RelatorioInsumoResponse(
        Long etapaId,
        String etapa,
        Long insumoId,
        String insumo,
        String tipo,
        String unidade,
        BigDecimal quantidadeOrcada,
        BigDecimal quantidadeRealizada,
        BigDecimal valorOrcado,
        BigDecimal valorRealizado
) {
}
