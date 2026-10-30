package com.example.demo.dtos;

import java.math.BigDecimal;

public record ItemOrcamentoRequest(
        Long insumoId,
        Long tarefaId,
        BigDecimal quantidade,
        BigDecimal valorUnitario
) {
}
