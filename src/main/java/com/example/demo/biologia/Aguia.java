package com.example.demo.biologia;

public final class Aguia extends Oviparo {

    public Aguia() {
        super("Aquila chrysaetos", "Animalia", "montanhas e campos abertos", 42);
    }

    @Override
    public String emitirSom() {
        return "Emite um chamado agudo.";
    }

    @Override
    public String seMover() {
        return "Voa e caminha com as patas.";
    }
}
