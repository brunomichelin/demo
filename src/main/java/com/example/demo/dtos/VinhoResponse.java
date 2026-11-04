package com.example.demo.dtos;

import com.example.demo.models.Vinho;

import java.math.BigDecimal;
import java.time.LocalDate;

public record VinhoResponse(
        Long id,
        String name,
        String producer,
        Integer vintage,
        String country,
        String region,
        String denomination,
        String type,
        String grapes,
        String blend,
        BigDecimal alcohol,
        String body,
        String oak,
        LocalDate tastedAt,
        String occasion,
        String company,
        Integer rating,
        String appearance,
        String aroma,
        String palate,
        String pairing,
        String buyAgain,
        BigDecimal price,
        String retailer,
        String temperature,
        String decanted,
        String cellaring,
        String photo,
        boolean favorite,
        boolean wishlist
) {
    public static VinhoResponse from(Vinho vinho) {
        return new VinhoResponse(
                vinho.getId(),
                vinho.getNome(),
                vinho.getProdutor(),
                vinho.getSafra(),
                vinho.getPais(),
                vinho.getRegiao(),
                vinho.getDenominacao(),
                vinho.getTipo(),
                vinho.getUvas(),
                vinho.getCorte(),
                vinho.getTeorAlcoolico(),
                vinho.getCorpo(),
                vinho.getMadeira(),
                vinho.getDataDegustacao(),
                vinho.getOcasiao(),
                vinho.getCompanhia(),
                vinho.getNota(),
                vinho.getAparencia(),
                vinho.getAroma(),
                vinho.getSaborFinal(),
                vinho.getHarmonizacao(),
                vinho.getComprariaNovamente(),
                vinho.getPrecoPago(),
                vinho.getLocalCompra(),
                vinho.getTemperaturaServico(),
                vinho.getDecantado(),
                vinho.getPotencialGuarda(),
                vinho.getFoto(),
                vinho.isFavorito(),
                vinho.isQueroBeber()
        );
    }
}
