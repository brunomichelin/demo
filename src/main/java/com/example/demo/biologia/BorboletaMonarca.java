package com.example.demo.biologia;

public final class BorboletaMonarca extends Inseto {

    public BorboletaMonarca() {
        super("Danaus plexippus", "jardins, campos e áreas com plantas hospedeiras");
    }

    @Override
    public String emitirSom() {
        return "Não emite sons audíveis para humanos.";
    }

    @Override
    public String seMover() {
        return "Voa com dois pares de asas.";
    }

    public String desenvolver() {
        return "Passa por metamorfose completa: ovo, lagarta, crisálida e adulto.";
    }
}
