package com.example.demo.biologia;

public abstract class Mamifero extends SerVivo {

    private boolean temPelos;

    protected Mamifero(
            String nomeCientifico,
            String reino,
            String habitat,
            boolean temPelos
    ) {
        super(nomeCientifico, reino, habitat);
        this.temPelos = temPelos;
    }

    public boolean isTemPelos() {
        return temPelos;
    }

    public void setTemPelos(boolean temPelos) {
        this.temPelos = temPelos;
    }

    public String amamentar() {
        return getNomeCientifico() + " alimenta os filhotes com leite.";
    }
}
