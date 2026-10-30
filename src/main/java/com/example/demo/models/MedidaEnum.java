package com.example.demo.models;

public enum MedidaEnum {

    UN(1L, "Unidade"),
    M2(2L, "m²"),
    M3(3L, "m³"),
    L(4L, "Litros"),
    KG(5L, "Quilogramas"),
    H(6L, "Horas"),
    DIA(7L, "Dias"),
    SEMANA(8L, "Semanas"),
    MES(9L, "Meses"),
    ANO(10L, "Anos");

    private final Long id;
    private final String descricao;

    MedidaEnum(Long id, String descricao) {
        this.id = id;
        this.descricao = descricao;
    }

    public Long getId() {
        return id;
    }

    public String getDescricao() {
        return descricao;
    }
    
}
