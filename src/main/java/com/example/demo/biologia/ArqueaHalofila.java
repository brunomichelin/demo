package com.example.demo.biologia;

public final class ArqueaHalofila extends Arquea {

    public ArqueaHalofila() {
        super("Haloferax volcanii", "lagos salgados e ambientes de alta salinidade");
    }

    public String adaptarAoAmbiente() {
        return "Tolera concentrações muito elevadas de sal.";
    }
}
