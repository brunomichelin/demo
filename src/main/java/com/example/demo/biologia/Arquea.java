package com.example.demo.biologia;

public abstract class Arquea extends SerVivo {

    protected Arquea(String nomeCientifico, String habitat) {
        super(nomeCientifico, "Archaea", habitat);
    }

    public String reproduzir() {
        return "Pode reproduzir-se por divisão celular.";
    }
}
