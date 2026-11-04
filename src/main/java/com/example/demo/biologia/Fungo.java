package com.example.demo.biologia;

public abstract class Fungo extends SerVivo {

    protected Fungo(String nomeCientifico, String habitat) {
        super(nomeCientifico, "Fungi", habitat);
    }

    public String obterNutrientes() {
        return "Absorve nutrientes do ambiente.";
    }
}
