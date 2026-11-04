package com.example.demo.biologia;

public final class Lactobacilo extends Bacteria {

    public Lactobacilo() {
        super("Lactobacillus acidophilus", "intestino humano e alimentos fermentados");
    }

    @Override
    public String emitirSom() {
        return "Não emite sons audíveis.";
    }

    @Override
    public String seMover() {
        return "Algumas cepas podem se deslocar com flagelos.";
    }
}
