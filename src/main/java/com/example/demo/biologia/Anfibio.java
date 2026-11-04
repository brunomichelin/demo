package com.example.demo.biologia;

public abstract class Anfibio extends SerVivo {

    protected Anfibio(String nomeCientifico, String reino, String habitat) {
        super(nomeCientifico, reino, habitat);
    }

    public String respirar() {
        return "Respira pela pele e pelos pulmões.";
    }
}
