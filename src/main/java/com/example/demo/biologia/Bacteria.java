package com.example.demo.biologia;

public abstract class Bacteria extends SerVivo {

    protected Bacteria(String nomeCientifico, String habitat) {
        super(nomeCientifico, "Bacteria", habitat);
    }

    public String reproduzirPorFissaoBinaria() {
        return "Reproduz-se principalmente por fissão binária.";
    }
}
