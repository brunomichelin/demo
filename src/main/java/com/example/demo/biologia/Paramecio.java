package com.example.demo.biologia;

public final class Paramecio extends Protozoario {

    public Paramecio() {
        super("Paramecium caudatum", "poças, lagoas e águas ricas em matéria orgânica");
    }

    @Override
    public String seMover() {
        return "Nada usando cílios que cobrem a célula.";
    }
}
