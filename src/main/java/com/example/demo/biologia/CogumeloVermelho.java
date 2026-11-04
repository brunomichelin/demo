package com.example.demo.biologia;

public final class CogumeloVermelho extends Fungo {

    public CogumeloVermelho() {
        super("Amanita muscaria", "florestas temperadas, associado a árvores");
    }

    @Override
    public String obterNutrientes() {
        return "Obtém nutrientes em associação com as raízes de árvores.";
    }
}
