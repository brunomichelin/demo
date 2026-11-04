package com.example.demo.biologia;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EspeciesTest {

    @Test
    void especiesImplementamComportamentosPolimorficos() {
        List<SerVivo> especies = List.of(
                new Leao(), new Aguia(), new Ornitorrinco(),
                new SapoCururu(), new PeixePalhaco(), new TartarugaMarinha(),
                new Araucaria(), new CogumeloVermelho(),
                new AranhaCaranguejeira(), new BorboletaMonarca(),
                new Lactobacilo(), new Cianobacteria(), new ArqueaHalofila(),
                new AmebaProteus(), new Paramecio()
        );

        assertTrue(especies.stream().allMatch(especie -> !especie.emitirSom().isBlank()));
        assertTrue(especies.stream().allMatch(especie -> !especie.seMover().isBlank()));
        assertTrue(new Leao().emitirSom().contains("Ruge"));
        assertTrue(new Aguia().emitirSom().contains("chamado agudo"));
        assertTrue(new AranhaCaranguejeira().seMover().contains("oito patas"));
        assertTrue(new BorboletaMonarca().seMover().contains("Voa"));
    }

    @Test
    void microorganismosRepresentamBacteriasArqueasEProtozoarios() {
        Lactobacilo lactobacilo = new Lactobacilo();
        Cianobacteria cianobacteria = new Cianobacteria();
        ArqueaHalofila arquea = new ArqueaHalofila();
        AmebaProteus ameba = new AmebaProteus();
        Paramecio paramecio = new Paramecio();

        assertEquals("Bacteria", lactobacilo.getReino());
        assertTrue(lactobacilo.reproduzirPorFissaoBinaria().contains("fissão binária"));
        assertTrue(cianobacteria.realizarFotossintese().contains("oxigênio"));
        assertEquals("Archaea", arquea.getReino());
        assertTrue(arquea.adaptarAoAmbiente().contains("sal"));
        assertEquals("Protista", ameba.getReino());
        assertTrue(ameba.seMover().contains("pseudópodes"));
        assertTrue(paramecio.seMover().contains("cílios"));
    }

    @Test
    void virusEModeladoComoAgenteAcelularForaDeSerVivo() {
        BacteriofagoLambda virus = new BacteriofagoLambda();

        assertFalse(SerVivo.class.isAssignableFrom(Virus.class));
        assertTrue(virus.getClassificacaoBiologica().contains("acelular"));
        assertTrue(virus.getHospedeiro().contains("Escherichia coli"));
        assertTrue(virus.replicar().contains("célula hospedeira"));
    }

    @Test
    void novasEspeciesModelamPlantasFungosAracnideosEInsetos() {
        Araucaria araucaria = new Araucaria();
        CogumeloVermelho cogumelo = new CogumeloVermelho();
        AranhaCaranguejeira aranha = new AranhaCaranguejeira();
        BorboletaMonarca borboleta = new BorboletaMonarca();

        assertEquals("Plantae", araucaria.getReino());
        assertTrue(araucaria.realizarFotossintese().contains("luz"));
        assertEquals("Fungi", cogumelo.getReino());
        assertTrue(cogumelo.obterNutrientes().contains("raízes"));
        assertEquals(8, aranha.getQuantidadePatas());
        assertEquals(6, borboleta.getQuantidadePatas());
        assertTrue(borboleta.desenvolver().contains("crisálida"));
        assertTrue(araucaria.seMover().contains("Não apresenta"));
        assertTrue(cogumelo.emitirSom().contains("Não emite"));
    }

    @Test
    void novasEspeciesRepresentamTiposBiologicosEContratosOviparos() {
        SapoCururu sapo = new SapoCururu();
        PeixePalhaco peixe = new PeixePalhaco();
        TartarugaMarinha tartaruga = new TartarugaMarinha();

        assertTrue(sapo.respirar().contains("pele"));
        assertTrue(sapo.botarOvo().contains("3 dias"));
        assertEquals("água salgada", peixe.getAmbienteAquatico());
        assertTrue(peixe.respirar().contains("brânquias"));
        assertTrue(tartaruga.regularTemperaturaCorporal().contains("ambiente"));
        assertTrue(tartaruga.botarOvo().contains("60 dias"));
    }

    @Test
    void ornitorrincoCombinaCaracteristicasDeMamiferoEOviparo() {
        Ornitorrinco ornitorrinco = new Ornitorrinco();

        assertTrue(ornitorrinco.isTemPelos());
        assertEquals(10, ornitorrinco.getTempoIncubacaoDias());
        assertTrue(ornitorrinco.botarOvo().contains("10 dias"));
        assertTrue(ornitorrinco.amamentar().contains("leite"));
    }

    @Test
    void settersMantemOsDadosEncapsuladosEValidos() {
        Leao leao = new Leao();
        leao.setHabitat("reserva natural");

        assertEquals("reserva natural", leao.getHabitat());
        assertFalse(leao.getHabitat().isBlank());
    }
}
