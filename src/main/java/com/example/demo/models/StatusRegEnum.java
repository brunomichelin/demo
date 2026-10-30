package com.example.demo.models;

public enum StatusRegEnum {

    ATIVO("Ativo"),
    INATIVO("Inativo"),
    EXCLUIDO("Excluído"),
    PENDENTE("Aguardando aprovação");

    private final String descricao;

    StatusRegEnum(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }

}
