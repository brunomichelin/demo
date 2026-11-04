package com.example.demo.biologia;

public final class BacteriofagoLambda extends Virus {

    public BacteriofagoLambda() {
        super("Bacteriófago lambda", "bactéria Escherichia coli");
    }

    @Override
    public String replicar() {
        return "Usa a estrutura da célula hospedeira para produzir novas partículas virais.";
    }
}
