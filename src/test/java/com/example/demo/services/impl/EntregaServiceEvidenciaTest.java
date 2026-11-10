package com.example.demo.services.impl;

import com.example.demo.dtos.EvidenciaEntregaResponse;
import com.example.demo.models.StatusEntregaEnum;
import com.example.demo.models.Entrega;
import com.example.demo.models.EvidenciaEntrega;
import com.example.demo.repositories.EvidenciaEntregaRepository;
import com.example.demo.repositories.EntregaRepository;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.repositories.UsuarioRepository;
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
class EntregaServiceEvidenciaTest {

    @Mock
    private EtapaRepository etapaRepository;
    @Mock
    private EntregaRepository entregaRepository;
    @Mock
    private TarefaRepository tarefaRepository;
    @Mock
    private UsuarioRepository usuarioRepository;
    @Mock
    private AuditoriaService auditoriaService;
    @Mock
    private EvidenciaEntregaRepository evidenciaRepository;

    @InjectMocks
    private EntregaService service;

    private Entrega entrega;

    @BeforeEach
    void setUp() {
        entrega = new Entrega();
        entrega.setId(7L);
        when(entregaRepository.findByEtapaId(12L)).thenReturn(Optional.of(entrega));
    }

    @Test
    void salvaVideoAssociadoAEntregaComoBytesNoBanco() {
        byte[] video = {0x1a, 0x45, (byte) 0xdf, (byte) 0xa3, 0x01};
        MockMultipartFile arquivo = new MockMultipartFile("arquivos", "../vistoria.webm",
                "application/octet-stream", video);
        EvidenciaEntregaResponse response = new EvidenciaEntregaResponse(21L, "vistoria.webm", "video/webm", 5L);
        when(evidenciaRepository.save(any(EvidenciaEntrega.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(evidenciaRepository.listarPorEntrega(7L)).thenReturn(List.of(response));

        List<EvidenciaEntregaResponse> evidencias =
                service.adicionarEvidencias(12L, List.of(arquivo), "tecnico@example.com");

        ArgumentCaptor<EvidenciaEntrega> captor = ArgumentCaptor.forClass(EvidenciaEntrega.class);
        verify(evidenciaRepository).save(captor.capture());
        EvidenciaEntrega salva = captor.getValue();
        assertEquals(entrega, salva.getEntrega());
        assertEquals("vistoria.webm", salva.getNomeArquivo());
        assertEquals("video/webm", salva.getTipo());
        assertArrayEquals(video, salva.getArquivo());
        assertEquals(List.of(response), evidencias);
    }

    @Test
    void rejeitaArquivosQueNaoSaoFotosOuVideosSuportados() {
        MockMultipartFile arquivo = new MockMultipartFile("arquivos", "arquivo.pdf",
                "application/pdf", "%PDF".getBytes());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.adicionarEvidencias(12L, List.of(arquivo), "tecnico@example.com"));

        assertEquals(400, erro.getStatusCode().value());
    }

    @Test
    void excluirEntregaTambemRemoveOsAnexosVinculados() {
        entrega.setStatus(StatusEntregaEnum.PENDENTE);

        service.excluir(12L);

        verify(evidenciaRepository).excluirPorEntregaId(7L);
        verify(entregaRepository).delete(entrega);
        verify(auditoriaService).registrar("entrega", 7L, "EXCLUSAO", "PENDENTE", null);
    }

    @Test
    void excluiEvidenciaIndividualComExclusaoLogica() {
        EvidenciaEntrega evidencia = new EvidenciaEntrega();
        evidencia.setId(21L);
        evidencia.setEntrega(entrega);
        when(evidenciaRepository.buscarArquivo(7L, 21L)).thenReturn(Optional.of(evidencia));

        service.excluirEvidencia(12L, 21L);

        verify(evidenciaRepository).save(evidencia);
        verify(auditoriaService).registrar("evidencia_entrega", 21L, "EXCLUSAO", "ATIVO", "EXCLUIDO");
        org.junit.jupiter.api.Assertions.assertNotNull(evidencia.getDataDelecao());
    }

    @Test
    void naoExcluiEvidenciaDeOutraEntregaOuJaExcluida() {
        when(evidenciaRepository.buscarArquivo(7L, 21L)).thenReturn(Optional.empty());

        ResponseStatusException erro = assertThrows(ResponseStatusException.class,
                () -> service.excluirEvidencia(12L, 21L));

        assertEquals(404, erro.getStatusCode().value());
        verify(evidenciaRepository, never()).save(any(EvidenciaEntrega.class));
    }
}
