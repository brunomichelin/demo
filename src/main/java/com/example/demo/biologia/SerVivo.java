package com.example.demo.biologia;

import java.util.Objects;

public abstract class SerVivo {

    private String nomeCientifico;
    private String reino;
    private String habitat;

    protected SerVivo(String nomeCientifico, String reino, String habitat) {
        setNomeCientifico(nomeCientifico);
        setReino(reino);
        setHabitat(habitat);
    }

    public String getNomeCientifico() {
        return nomeCientifico;
    }

    public void setNomeCientifico(String nomeCientifico) {
        this.nomeCientifico = textoObrigatorio(nomeCientifico, "nome científico");
    }

    public String getReino() {
        return reino;
    }

    public void setReino(String reino) {
        this.reino = textoObrigatorio(reino, "reino");
    }

    public String getHabitat() {
        return habitat;
    }

    public void setHabitat(String habitat) {
        this.habitat = textoObrigatorio(habitat, "habitat");
    }

    public String emitirSom() {
        return "Não emite sons audíveis.";
    }

    public String seMover() {
        return "Não apresenta locomoção própria.";
    }

    private static String textoObrigatorio(String valor, String campo) {
        Objects.requireNonNull(valor, campo + " não pode ser nulo");
        if (valor.isBlank()) {
            throw new IllegalArgumentException(campo + " não pode ficar em branco");
        }
        return valor.trim();
    }
}
