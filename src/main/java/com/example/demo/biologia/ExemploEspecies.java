package com.example.demo.biologia;

import java.util.List;

public final class ExemploEspecies {

    private ExemploEspecies() {
    }

    public static void main(String[] args) {
        List<SerVivo> especies = List.of(
                new Leao(),
                new Aguia(),
                new Ornitorrinco(),
                new SapoCururu(),
                new PeixePalhaco(),
                new TartarugaMarinha(),
                new Araucaria(),
                new CogumeloVermelho(),
                new AranhaCaranguejeira(),
                new BorboletaMonarca(),
                new Lactobacilo(),
                new Cianobacteria(),
                new ArqueaHalofila(),
                new AmebaProteus(),
                new Paramecio()
        );

        for (SerVivo especie : especies) {
            System.out.printf(
                    "%s | Reino: %s | Habitat: %s%n",
                    especie.getNomeCientifico(),
                    especie.getReino(),
                    especie.getHabitat()
            );
            System.out.println("Som: " + especie.emitirSom());
            System.out.println("Locomoção: " + especie.seMover());

            if (especie instanceof Mamifero mamifero) {
                System.out.println("Possui pelos: " + mamifero.isTemPelos());
                System.out.println(mamifero.amamentar());
            }
            if (especie instanceof ReproducaoOvipara oviparo) {
                System.out.println(oviparo.botarOvo());
            }
            if (especie instanceof Anfibio anfibio) {
                System.out.println(anfibio.respirar());
            }
            if (especie instanceof Peixe peixe) {
                System.out.println(peixe.respirar());
                System.out.println("Ambiente: " + peixe.getAmbienteAquatico());
            }
            if (especie instanceof Reptil reptil) {
                System.out.println(reptil.regularTemperaturaCorporal());
            }
            if (especie instanceof Planta planta) {
                System.out.println(planta.realizarFotossintese());
            }
            if (especie instanceof Fungo fungo) {
                System.out.println(fungo.obterNutrientes());
            }
            if (especie instanceof Aracnideo aracnideo) {
                System.out.println("Número de patas: " + aracnideo.getQuantidadePatas());
            }
            if (especie instanceof Inseto inseto) {
                System.out.println("Número de patas: " + inseto.getQuantidadePatas());
                if (inseto instanceof BorboletaMonarca borboleta) {
                    System.out.println(borboleta.desenvolver());
                }
            }
            if (especie instanceof Bacteria bacteria) {
                System.out.println(bacteria.reproduzirPorFissaoBinaria());
                if (bacteria instanceof Cianobacteria cianobacteria) {
                    System.out.println(cianobacteria.realizarFotossintese());
                }
            }
            if (especie instanceof Arquea arquea) {
                System.out.println(arquea.reproduzir());
                if (arquea instanceof ArqueaHalofila halofila) {
                    System.out.println(halofila.adaptarAoAmbiente());
                }
            }
            System.out.println();
        }

        Virus virus = new BacteriofagoLambda();
        System.out.printf(
                "%s | Hospedeiro: %s | %s%n",
                virus.getNome(),
                virus.getHospedeiro(),
                virus.getClassificacaoBiologica()
        );
        System.out.println(virus.replicar());
    }
}
