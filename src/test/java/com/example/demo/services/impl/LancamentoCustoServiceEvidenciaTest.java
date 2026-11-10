package com.example.demo.services.impl;

import com.example.demo.dtos.EvidenciaPagamentoResponse;
import com.example.demo.models.EvidenciaPagamento;
import com.example.demo.models.LancamentoCusto;
import com.example.demo.repositories.DocObraRepository;
import com.example.demo.repositories.EvidenciaPagamentoRepository;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.FornecedorRepository;
import com.example.demo.repositories.InsumoRepository;
import com.example.demo.repositories.LancamentoCustoRepository;
import com.example.demo.repositories.TarefaRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LancamentoCustoServiceEvidenciaTest {

    @Mock
    private LancamentoCustoRepository lancamentoRepository;
    @Mock
    private EtapaRepository etapaRepository;
    @Mock
    private InsumoRepository insumoRepository;
    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private FornecedorRepository fornecedorRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private DocObraRepository docObraRepository;
    @Mock
    private EvidenciaPagamentoRepository evidenciaRepository;

    @InjectMocks
    private LancamentoCustoService service;

    private LancamentoCusto lancamento;

    @BeforeEach
    void setUp() {
        lancamento = new LancamentoCusto();
        lancamento.setId(42L);
        when(lancamentoRepository.findById(42L)).thenReturn(Optional.of(lancamento));
    }

    @Test
    void salvaFotoAssociadaAoLancamentoComoBytesNoBanco() {
        byte[] foto = {(byte) 0xff, (byte) 0xd8, (byte) 0xff, 0x01};
        MockMultipartFile arquivo = new MockMultipartFile("arquivos", "../comprovante.jpg",
                "application/octet-stream", foto);
        EvidenciaPagamentoResponse resposta = new EvidenciaPagamentoResponse(9L, "comprovante.jpg", "image/jpeg", 4L);
        when(evidenciaRepository.save(any(EvidenciaPagamento.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenciaRepository.listarPorLancamento(42L)).thenReturn(List.of(resposta));

        List<EvidenciaPagamentoResponse> evidencias =
                service.adicionarEvidencias(42L, List.of(arquivo), "financeiro@example.com");

        ArgumentCaptor<EvidenciaPagamento> captor = ArgumentCaptor.forClass(EvidenciaPagamento.class);
        verify(evidenciaRepository).save(captor.capture());
        EvidenciaPagamento salva = captor.getValue();
        assertEquals(lancamento, salva.getLancamentoCusto());
        assertEquals("comprovante.jpg", salva.getNomeArquivo());
        assertEquals("image/jpeg", salva.getTipo());
        assertArrayEquals(foto, salva.getArquivo());
        assertEquals(List.of(resposta), evidencias);
    }

    @Test
    void rejeitaArquivosQueNaoSaoFotosSuportadas() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivos", "comprovante.pdf",
                "application/pdf", "%PDF".getBytes());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.adicionarEvidencias(42L, List.of(arquivo), "financeiro@example.com"));

        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void excluiEvidenciaComExclusaoLogicaERegistraAuditoria() {
        EvidenciaPagamento evidencia = new EvidenciaPagamento();
        evidencia.setId(9L);
        evidencia.setLancamentoCusto(lancamento);
        when(evidenciaRepository.buscarArquivo(42L, 9L)).thenReturn(Optional.of(evidencia));

        service.excluirEvidencia(42L, 9L);

        verify(evidenciaRepository).save(evidencia);
        verify(auditoriaService).registrar("evidencia_pagamento", 9L, "EXCLUSAO", "ATIVO", "EXCLUIDO");
        org.junit.jupiter.api.Assertions.assertNotNull(evidencia.getDataDelecao());
    }

    @Test
    void naoExcluiEvidenciaDeOutroLancamentoOuJaExcluida() {
        when(evidenciaRepository.buscarArquivo(42L, 9L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.excluirEvidencia(42L, 9L));

        assertEquals(404, erro.getStatusCode().value());
        verify(evidenciaRepository, never()).save(any(EvidenciaPagamento.class));
    }
}
