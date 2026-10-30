package com.example.demo.models;

public enum StatusLancamentoEnum {

    PENDENTE(1L, "Pendente"),
    VALIDADO(2L, "Validado"),
    REJEITADO(3L, "Rejeitado");

    private final Long id;
    private final String descricao;

    StatusLancamentoEnum(Long id, String descricao) {
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
