package com.example.demo.dtos;

import com.example.demo.models.MedidaEnum;

import java.math.BigDecimal;

public record InsumoApiRequest(
        String codigo,
        String descricao,
        String tipo,
        MedidaEnum unidade,
        BigDecimal precoReferencia,
        Long fornecedorId
) {
}
