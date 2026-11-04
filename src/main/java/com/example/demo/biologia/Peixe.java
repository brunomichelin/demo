package com.example.demo.biologia;

public abstract class Peixe extends SerVivo {

    private final String ambienteAquatico;

    protected Peixe(String nomeCientifico, String reino, String habitat, String ambienteAquatico) {
        super(nomeCientifico, reino, habitat);
        this.ambienteAquatico = ambienteAquatico;
    }

    public String getAmbienteAquatico() {
        return ambienteAquatico;
    }

    public String respirar() {
        return "Respira por brânquias.";
    }
}
