package com.example.demo.models;

public enum CargoEnum {

    ADMIN(1L, "Administrador"),
    GESTOR(2L, "Gerente de Obras"),
    ENGENHEIRO(3L, "Engenheiro de Obras"),
    ORCAMENTISTA(4L, "Orçamentista de Obras"),
    MESTRE_OBRA(5L, "Mestre de Obras"),
    TECNICO_OBRA(6L, "Técnico de Obras"),
    FINANCEIRO(7L, "Financeiro"),
    CLIENTE(8L, "Cliente");

    private final Long id;
    private final String descricao;

    CargoEnum(Long id, String descricao) {
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
