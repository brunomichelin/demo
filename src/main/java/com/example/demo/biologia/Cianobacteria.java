package com.example.demo.biologia;

public final class Cianobacteria extends Bacteria {

    public Cianobacteria() {
        super("Anabaena variabilis", "água doce e ambientes úmidos");
    }

    public String realizarFotossintese() {
        return "Realiza fotossíntese e libera oxigênio.";
    }
}
