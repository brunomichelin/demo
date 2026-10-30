package com.example.demo.models;

public enum StatusCronEnum {

    PLANEJAMENTO(1L, "Planejamento"),
    EM_ANDAMENTO(2L, "Em andamento"),
    EM_BLOQUEIO(3L, "Em bloqueio"),
    CONCLUIDA(4L, "Concluída"),
    CANCELADA(5L, "Cancelada"),
    PAUSADA(6L, "Pausada");

    private final Long id;
    private final String descricao;

    StatusCronEnum(Long id, String descricao) {
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
