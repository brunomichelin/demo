package com.example.demo.biologia;

public final class TartarugaMarinha extends Reptil implements ReproducaoOvipara {

    private static final int TEMPO_INCUBACAO_DIAS = 60;

    public TartarugaMarinha() {
        super("Chelonia mydas", "Animalia", "mares tropicais e praias costeiras");
    }

    @Override
    public int getTempoIncubacaoDias() {
        return TEMPO_INCUBACAO_DIAS;
    }

    @Override
    public String emitirSom() {
        return "Emite sons de baixa frequência.";
    }

    @Override
    public String seMover() {
        return "Nada com as nadadeiras e se desloca na areia.";
    }
}
