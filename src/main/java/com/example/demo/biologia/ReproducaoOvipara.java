package com.example.demo.biologia;

public interface ReproducaoOvipara {

    String getNomeCientifico();

    int getTempoIncubacaoDias();

    default String botarOvo() {
        return "O ovo de " + getNomeCientifico()
                + " tem incubação de " + getTempoIncubacaoDias() + " dias.";
    }
}
