package com.example.demo.biologia;

public abstract class Inseto extends SerVivo {

    protected Inseto(String nomeCientifico, String habitat) {
        super(nomeCientifico, "Animalia", habitat);
    }

    public int getQuantidadePatas() {
        return 6;
    }
}
