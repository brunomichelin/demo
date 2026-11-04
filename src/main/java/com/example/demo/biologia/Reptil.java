package com.example.demo.biologia;

public abstract class Reptil extends SerVivo {

    protected Reptil(String nomeCientifico, String reino, String habitat) {
        super(nomeCientifico, reino, habitat);
    }

    public String regularTemperaturaCorporal() {
        return "A temperatura corporal varia conforme o ambiente.";
    }
}
