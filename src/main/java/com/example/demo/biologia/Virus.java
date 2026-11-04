package com.example.demo.biologia;

import java.util.Objects;

public abstract class Virus {

    private final String nome;
    private final String hospedeiro;

    protected Virus(String nome, String hospedeiro) {
        this.nome = textoObrigatorio(nome, "nome");
        this.hospedeiro = textoObrigatorio(hospedeiro, "hospedeiro");
    }

    public String getNome() {
        return nome;
    }

    public String getHospedeiro() {
        return hospedeiro;
    }

    public String getClassificacaoBiologica() {
        return "Agente acelular; não é classificado como ser vivo.";
    }

    public abstract String replicar();

    private static String textoObrigatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " não pode ser nulo");
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ficar em branco");
        }
        return valor.trim();
    }
}
