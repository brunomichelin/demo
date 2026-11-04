package com.example.demo.biologia;

public final class AmebaProteus extends Protozoario {

    public AmebaProteus() {
        super("Amoeba proteus", "água doce e ambientes aquáticos");
    }

    @Override
    public String seMover() {
        return "Desloca-se por pseudópodes.";
    }
}
