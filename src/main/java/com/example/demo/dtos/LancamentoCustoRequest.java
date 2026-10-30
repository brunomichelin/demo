package com.example.demo.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LancamentoCustoRequest(
        Long insumoId,
        Long fornecedorId,
        BigDecimal quantidade,
        BigDecimal valorUnitario,
        LocalDate dataLancamento,
        String numeroDocumento,
        String justificativa,
        Long documentoId
) {
}
