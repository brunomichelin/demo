package com.example.demo.biologia;

public abstract class Oviparo extends SerVivo implements ReproducaoOvipara {

    private int tempoIncubacaoDias;

    protected Oviparo(
            String nomeCientifico,
            String reino,
            String habitat,
            int tempoIncubacaoDias
    ) {
        super(nomeCientifico, reino, habitat);
        setTempoIncubacaoDias(tempoIncubacaoDias);
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
}
