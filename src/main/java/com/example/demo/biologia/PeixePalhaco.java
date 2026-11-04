package com.example.demo.biologia;

public final class PeixePalhaco extends Peixe {

    public PeixePalhaco() {
        super("Amphiprion ocellaris", "Animalia", "recifes do Indo-Pacífico", "água salgada");
    }

    @Override
    public String emitirSom() {
        return "Produz pequenos estalos e cliques.";
    }

    @Override
    public String seMover() {
        return "Nada usando as nadadeiras.";
    }
}
