package com.example.demo.biologia;

public final class Leao extends Mamifero {

    public Leao() {
        super("Panthera leo", "Animalia", "savana africana", true);
    }

    @Override
    public String emitirSom() {
        return "Ruge.";
    }

    @Override
    public String seMover() {
        return "Caminha e corre sobre quatro patas.";
    }
}
