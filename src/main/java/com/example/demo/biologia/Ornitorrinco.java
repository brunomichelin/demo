package com.example.demo.biologia;

public final class Ornitorrinco extends Mamifero implements ReproducaoOvipara {

    private int tempoIncubacaoDias;

    public Ornitorrinco() {
        super("Ornithorhynchus anatinus", "Animalia", "rios e lagos da Austrália", true);
        setTempoIncubacaoDias(10);
    }

    @Override
    public int getTempoIncubacaoDias() {
        return tempoIncubacaoDias;
    }

    public void setTempoIncubacaoDias(int tempoIncubacaoDias) {
        if (tempoIncubacaoDias <= 0) {
            throw new IllegalArgumentException("O tempo de incubação deve ser maior que zero.");
        }
        this.tempoIncubacaoDias = tempoIncubacaoDias;
    }

    @Override
    public String emitirSom() {
        return "Emite vocalizações suaves e grunhidos.";
    }

    @Override
    public String seMover() {
        return "Nada usando as patas dianteiras como remos.";
    }
}
