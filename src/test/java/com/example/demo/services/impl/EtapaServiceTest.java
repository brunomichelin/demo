package com.example.demo.services.impl;

import com.example.demo.models.Etapa;
import com.example.demo.models.Entrega;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.StatusEntregaEnum;
import com.example.demo.models.Tarefa;
import com.example.demo.repositories.EntregaRepository;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.TarefaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class EtapaServiceTest {
    private TarefaRepository tarefaRepository;
    private EntregaRepository entregaRepository;
    private EtapaService service;
    private Etapa etapa;

    @BeforeEach
    void setUp() {
        tarefaRepository = mock(TarefaRepository.class);
        entregaRepository = mock(EntregaRepository.class);
        service = new EtapaService(mock(EtapaRepository.class), tarefaRepository, entregaRepository);
        etapa = new Etapa();
        etapa.setId(7L);
    }

    @Test
    void naoConcluiEtapaComTarefaPendente() {
        Tarefa tarefa = new Tarefa();
        tarefa.setStatus(StatusCronEnum.EM_ANDAMENTO);
        when(tarefaRepository.findByEtapaId(7L)).thenReturn(List.of(tarefa));

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.validarPodeConcluir(etapa, StatusCronEnum.CONCLUIDA));

        assertEquals(422, erro.getStatusCode().value());
    }

    @Test
    void exigeEntregaAprovadaParaConcluirEtapa() {
        when(tarefaRepository.findByEtapaId(7L)).thenReturn(List.of());
        when(entregaRepository.findByEtapaId(7L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.validarPodeConcluir(etapa, StatusCronEnum.CONCLUIDA));

        assertEquals(422, erro.getStatusCode().value());
    }

    @Test
    void permiteConcluirComTarefasEEntregaAprovada() {
        Entrega entrega = new Entrega();
        entrega.setStatus(StatusEntregaEnum.APROVADA);
        when(tarefaRepository.findByEtapaId(7L)).thenReturn(List.of());
        when(entregaRepository.findByEtapaId(7L)).thenReturn(Optional.of(entrega));

        assertDoesNotThrow(() -> service.validarPodeConcluir(etapa, StatusCronEnum.CONCLUIDA));
    }
}
