package com.example.demo.services.impl;

import com.example.demo.dtos.AprovacaoEntregaRequest;
import com.example.demo.dtos.EntregaListaResponse;
import com.example.demo.dtos.EvidenciaEntregaResponse;
import com.example.demo.dtos.EntregaRequest;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.List;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EntregaService {
    private final EtapaRepository etapaRepository;
    private final EntregaRepository entregaRepository;
    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;
    private final EvidenciaEntregaRepository evidenciaRepository;

    private static final long TAMANHO_MAXIMO_FOTO = 10 * 1024 * 1024;
    private static final long TAMANHO_MAXIMO_VIDEO = 50 * 1024 * 1024;
    private static final long TAMANHO_MAXIMO_ENVIO = 100 * 1024 * 1024;
    private static final int QUANTIDADE_MAXIMA_ARQUIVOS = 10;

    public EntregaService(
            EtapaRepository etapaRepository,
            EntregaRepository entregaRepository,
            TarefaRepository tarefaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService,
            EvidenciaEntregaRepository evidenciaRepository
    ) {
        this.etapaRepository = etapaRepository;
        this.entregaRepository = entregaRepository;
        this.tarefaRepository = tarefaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
        this.evidenciaRepository = evidenciaRepository;
    }

    @Transactional
    public Entrega registrar(Long etapaId, EntregaRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe os dados da entrega.");
        }
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        Entrega entrega = entregaRepository.findByEtapaId(etapaId).orElseGet(Entrega::new);
        boolean novaEntrega = entrega.getId() == null;
        StatusEntregaEnum statusAnterior = entrega.getStatus();
        if (statusAnterior == StatusEntregaEnum.APROVADA) {
            etapa.setStatus(StatusCronEnum.EM_ANDAMENTO);
            etapa.setDataAtualizacao(LocalDateTime.now());
            etapaRepository.save(etapa);
        }
        entrega.setEtapa(etapa);
        entrega.setDataReal(request.dataReal() == null ? LocalDate.now() : request.dataReal());
        entrega.setObservacoes(request.observacoes());
        entrega.setStatus(StatusEntregaEnum.PENDENTE);
        entrega.setResponsavel(usuarioAtual());
        entrega.setAprovadoPor(null);
        entrega.setDataAprovacao(null);
        entrega.setDataAtualizacao(LocalDateTime.now());
        if (novaEntrega) {
            entrega.setDataCriacao(LocalDateTime.now());
        }
        Entrega salva = entregaRepository.save(entrega);
        auditoriaService.registrar("entrega", salva.getId(), novaEntrega ? "REGISTRO" : "EDICAO",
                statusAnterior == null ? null : statusAnterior.name(),
                "status=PENDENTE, dataReal=" + salva.getDataReal());
        return salva;
    }

    @Transactional
    public Entrega aprovar(Long etapaId, AprovacaoEntregaRequest request) {
        if (request == null || (request.status() != StatusEntregaEnum.APROVADA
                && request.status() != StatusEntregaEnum.REPROVADA)) {
            throw erro(HttpStatus.BAD_REQUEST, "A situação deve ser APROVADA ou REPROVADA.");
        }
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        Entrega entrega = entregaRepository.findByEtapaId(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Entrega não registrada."));
        if (request.status() == StatusEntregaEnum.APROVADA
                && tarefaRepository.findByEtapaId(etapaId).stream()
                .anyMatch(tarefa -> tarefa.getStatus() != StatusCronEnum.CONCLUIDA)) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A entrega não pode ser aprovada enquanto houver tarefas não concluídas.");
        }
        StatusEntregaEnum anterior = entrega.getStatus();
        entrega.setStatus(request.status());
        if (request.observacoes() != null) {
            entrega.setObservacoes(request.observacoes());
        }
        entrega.setAprovadoPor(usuarioAtual());
        entrega.setDataAprovacao(LocalDateTime.now());
        entrega.setDataAtualizacao(LocalDateTime.now());
        if (request.status() == StatusEntregaEnum.APROVADA) {
            etapa.setStatus(StatusCronEnum.CONCLUIDA);
            etapa.setDataAtualizacao(LocalDateTime.now());
            etapaRepository.save(etapa);
        } else if (etapa.getStatus() == StatusCronEnum.CONCLUIDA) {
            etapa.setStatus(StatusCronEnum.EM_ANDAMENTO);
            etapa.setDataAtualizacao(LocalDateTime.now());
            etapaRepository.save(etapa);
        }
        Entrega salva = entregaRepository.save(entrega);
        auditoriaService.registrar("entrega", salva.getId(), "APROVACAO", anterior.name(),
                salva.getStatus().name());
        return salva;
    }

    @Transactional(readOnly = true)
    public List<EntregaListaResponse> listarPorObra(Long obraId) {
        return entregaRepository.listarResumosPorObra(obraId);
    }

    @Transactional
    public void excluir(Long etapaId) {
        Entrega entrega = buscarEntrega(etapaId);
        evidenciaRepository.excluirPorEntregaId(entrega.getId());
        Etapa etapa = entrega.getEtapa();
        if (entrega.getStatus() == StatusEntregaEnum.APROVADA
                && etapa.getStatus() == StatusCronEnum.CONCLUIDA) {
            etapa.setStatus(StatusCronEnum.EM_ANDAMENTO);
            etapa.setDataAtualizacao(LocalDateTime.now());
            etapaRepository.save(etapa);
        }
        entregaRepository.delete(entrega);
        auditoriaService.registrar("entrega", entrega.getId(), "EXCLUSAO",
                entrega.getStatus() == null ? null : entrega.getStatus().name(), null);
    }

    @Transactional(readOnly = true)
    public List<EvidenciaEntregaResponse> listarEvidencias(Long etapaId) {
        Entrega entrega = buscarEntrega(etapaId);
        return evidenciaRepository.listarPorEntrega(entrega.getId());
    }

    @Transactional
    public List<EvidenciaEntregaResponse> adicionarEvidencias(
            Long etapaId,
            List<MultipartFile> arquivos,
            String usuario
    ) {
        Entrega entrega = buscarEntrega(etapaId);
        if (arquivos == null || arquivos.isEmpty() || arquivos.size() > QUANTIDADE_MAXIMA_ARQUIVOS) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "Selecione de 1 a " + QUANTIDADE_MAXIMA_ARQUIVOS + " fotos ou vídeos por envio.");
        }
        long tamanhoEnvio = arquivos.stream().filter(arquivo -> arquivo != null)
                .mapToLong(MultipartFile::getSize).sum();
        if (tamanhoEnvio > TAMANHO_MAXIMO_ENVIO) {
            throw erro(HttpStatus.PAYLOAD_TOO_LARGE, "O tamanho total dos arquivos não pode exceder 100 MB.");
        }

        for (MultipartFile arquivo : arquivos) {
            if (arquivo == null || arquivo.isEmpty()) {
                throw erro(HttpStatus.BAD_REQUEST, "Um dos arquivos enviados está vazio.");
            }
            salvarEvidencia(entrega, arquivo.getOriginalFilename(), lerArquivo(arquivo), usuario);
        }
        return evidenciaRepository.listarPorEntrega(entrega.getId());
    }

    @Transactional
    public List<EvidenciaEntregaResponse> adicionarEvidencia(
            Long etapaId,
            String nomeArquivo,
            byte[] conteudo,
            String usuario
    ) {
        Entrega entrega = buscarEntrega(etapaId);
        salvarEvidencia(entrega, nomeArquivo, conteudo, usuario);
        return evidenciaRepository.listarPorEntrega(entrega.getId());
    }

    @Transactional(readOnly = true)
    public EvidenciaEntrega buscarArquivoEvidencia(Long etapaId, Long evidenciaId) {
        Entrega entrega = buscarEntrega(etapaId);
        return evidenciaRepository.buscarArquivo(entrega.getId(), evidenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Arquivo de evidência não encontrado."));
    }

    @Transactional
    public void excluirEvidencia(Long etapaId, Long evidenciaId) {
        Entrega entrega = buscarEntrega(etapaId);
        EvidenciaEntrega evidencia = evidenciaRepository.buscarArquivo(entrega.getId(), evidenciaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Arquivo de evidência não encontrado."));
        evidencia.setDataDelecao(LocalDateTime.now());
        evidencia.setDataAtualizacao(LocalDateTime.now());
        evidenciaRepository.save(evidencia);
        auditoriaService.registrar("evidencia_entrega", evidencia.getId(), "EXCLUSAO", "ATIVO", "EXCLUIDO");
    }

    private Entrega buscarEntrega(Long etapaId) {
        return entregaRepository.findByEtapaId(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Entrega não registrada."));
    }

    private byte[] lerArquivo(MultipartFile arquivo) {
        try {
            return arquivo.getBytes();
        } catch (IOException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Não foi possível ler um dos arquivos enviados.", exception);
        }
    }

    private String detectarTipo(byte[] arquivo) {
        if (arquivo.length >= 8
                && (arquivo[0] & 0xff) == 0x89 && arquivo[1] == 0x50 && arquivo[2] == 0x4e
                && arquivo[3] == 0x47 && arquivo[4] == 0x0d && arquivo[5] == 0x0a
                && arquivo[6] == 0x1a && arquivo[7] == 0x0a) {
            return "image/png";
        }
        if (arquivo.length >= 3 && (arquivo[0] & 0xff) == 0xff
                && (arquivo[1] & 0xff) == 0xd8 && (arquivo[2] & 0xff) == 0xff) {
            return "image/jpeg";
        }
        if (arquivo.length >= 12 && arquivo[0] == 'R' && arquivo[1] == 'I'
                && arquivo[2] == 'F' && arquivo[3] == 'F'
                && arquivo[8] == 'W' && arquivo[9] == 'E'
                && arquivo[10] == 'B' && arquivo[11] == 'P') {
            return "image/webp";
        }
        if (arquivo.length >= 12 && arquivo[4] == 'f' && arquivo[5] == 't'
                && arquivo[6] == 'y' && arquivo[7] == 'p') {
            return arquivo[8] == 'q' && arquivo[9] == 't' ? "video/quicktime" : "video/mp4";
        }
        if (arquivo.length >= 4 && (arquivo[0] & 0xff) == 0x1a
                && (arquivo[1] & 0xff) == 0x45 && (arquivo[2] & 0xff) == 0xdf
                && (arquivo[3] & 0xff) == 0xa3) {
            return "video/webm";
        }
        return null;
    }

    private String nomeSeguro(String nomeOriginal) {
        if (nomeOriginal == null || nomeOriginal.isBlank()) {
            return "evidencia";
        }
        String nome = nomeOriginal.replace('\\', '/');
        nome = nome.substring(nome.lastIndexOf('/') + 1)
                .replaceAll("[\\p{Cntrl}]", "")
                .trim();
        return nome.isEmpty() ? "evidencia" : nome.substring(0, Math.min(nome.length(), 255));
    }

    private void salvarEvidencia(Entrega entrega, String nomeOriginal, byte[] conteudo, String usuario) {
        if (conteudo == null || conteudo.length == 0) {
            throw erro(HttpStatus.BAD_REQUEST, "O arquivo enviado está vazio.");
        }
        String tipo = detectarTipo(conteudo);
        if (tipo == null) {
            throw erro(HttpStatus.BAD_REQUEST,
                    "Envie fotos JPEG, PNG ou WebP, ou vídeos MP4, MOV ou WebM.");
        }
        long limite = tipo.startsWith("video/") ? TAMANHO_MAXIMO_VIDEO : TAMANHO_MAXIMO_FOTO;
        if (conteudo.length > limite) {
            throw erro(HttpStatus.PAYLOAD_TOO_LARGE, tipo.startsWith("video/")
                    ? "Cada vídeo deve ter no máximo 50 MB."
                    : "Cada foto deve ter no máximo 10 MB.");
        }

        EvidenciaEntrega evidencia = new EvidenciaEntrega();
        evidencia.setEntrega(entrega);
        evidencia.setNomeArquivo(nomeSeguro(nomeOriginal));
        evidencia.setTipo(tipo);
        evidencia.setTamanho((long) conteudo.length);
        evidencia.setArquivo(conteudo);
        evidencia.setDataCriacao(LocalDateTime.now());
        evidencia.setCriadoPor(usuario);
        evidencia.setStatusRegistro(StatusRegEnum.ATIVO);
        EvidenciaEntrega salva = evidenciaRepository.save(evidencia);
        auditoriaService.registrar("evidencia_entrega", salva.getId(), "UPLOAD", null,
                "entrega=" + entrega.getId() + ", arquivo=" + salva.getNomeArquivo());
    }

    private Usuario usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getName() == null) {
            return null;
        }
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName()).orElse(null);
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
