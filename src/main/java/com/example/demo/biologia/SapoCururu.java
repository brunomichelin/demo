package com.example.demo.biologia;

public final class SapoCururu extends Anfibio implements ReproducaoOvipara {

    private static final int TEMPO_INCUBACAO_DIAS = 3;

    public SapoCururu() {
        super("Rhinella marina", "Animalia", "áreas úmidas e margens de lagoas");
    }

    @Override
    public int getTempoIncubacaoDias() {
        return TEMPO_INCUBACAO_DIAS;
    }

    @Override
    public String emitirSom() {
        return "Emite um coaxar grave.";
    }

    @Override
    public String seMover() {
        return "Salta e caminha pelo solo.";
    }
}
