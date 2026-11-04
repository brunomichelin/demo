package com.example.demo.biologia;

public abstract class Aracnideo extends SerVivo {

    protected Aracnideo(String nomeCientifico, String habitat) {
        super(nomeCientifico, "Animalia", habitat);
    }

    public int getQuantidadePatas() {
        return 8;
    }
}
