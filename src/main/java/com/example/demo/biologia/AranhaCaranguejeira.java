package com.example.demo.biologia;

public final class AranhaCaranguejeira extends Aracnideo {

    public AranhaCaranguejeira() {
        super("Grammostola pulchra", "campos e áreas de vegetação do Brasil");
    }

    @Override
    public String seMover() {
        return "Caminha sobre oito patas.";
    }
}
