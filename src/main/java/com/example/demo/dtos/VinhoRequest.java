package com.example.demo.dtos;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VinhoRequest(
        String nome,
        String produtor,
        Integer safra,
        String pais,
        String regiao,
        String denominacao,
        String tipo,
        String uvas,
        String corte,
        BigDecimal teorAlcoolico,
        String corpo,
        String madeira,
        LocalDate dataDegustacao,
        String ocasiao,
        String companhia,
        Integer nota,
        String aparencia,
        String aroma,
        String saborFinal,
        String harmonizacao,
        String comprariaNovamente,
        BigDecimal precoPago,
        String localCompra,
        String temperaturaServico,
        String decantado,
        String potencialGuarda,
        String foto,
        Boolean favorito,
        Boolean queroBeber
) {
}
