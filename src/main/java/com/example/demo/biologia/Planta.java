package com.example.demo.biologia;

public abstract class Planta extends SerVivo {

    protected Planta(String nomeCientifico, String habitat) {
        super(nomeCientifico, "Plantae", habitat);
    }

    public String realizarFotossintese() {
        return "Produz açúcares usando luz, água e gás carbônico.";
    }
}
