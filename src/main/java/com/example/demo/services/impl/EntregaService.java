package com.example.demo.services.impl;

import com.example.demo.dtos.AprovacaoEntregaRequest;
import com.example.demo.dtos.EntregaRequest;
import com.example.demo.models.*;
import com.example.demo.repositories.*;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
public class EntregaService {
    private final EtapaRepository etapaRepository;
    private final EntregaRepository entregaRepository;
    private final TarefaRepository tarefaRepository;
    private final UsuarioRepository usuarioRepository;
    private final AuditoriaService auditoriaService;

    public EntregaService(
            EtapaRepository etapaRepository,
            EntregaRepository entregaRepository,
            TarefaRepository tarefaRepository,
            UsuarioRepository usuarioRepository,
            AuditoriaService auditoriaService
    ) {
        this.etapaRepository = etapaRepository;
        this.entregaRepository = entregaRepository;
        this.tarefaRepository = tarefaRepository;
        this.usuarioRepository = usuarioRepository;
        this.auditoriaService = auditoriaService;
    }

    @Transactional
    public Entrega registrar(Long etapaId, EntregaRequest request) {
        if (request == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe os dados da entrega.");
        }
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        Entrega entrega = entregaRepository.findByEtapaId(etapaId).orElseGet(Entrega::new);
        entrega.setEtapa(etapa);
        entrega.setDataReal(request.dataReal() == null ? LocalDate.now() : request.dataReal());
        entrega.setObservacoes(request.observacoes());
        entrega.setStatus(StatusEntregaEnum.PENDENTE);
        entrega.setResponsavel(usuarioAtual());
        entrega.setDataAtualizacao(LocalDateTime.now());
        if (entrega.getId() == null) {
            entrega.setDataCriacao(LocalDateTime.now());
        }
        Entrega salva = entregaRepository.save(entrega);
        auditoriaService.registrar("entrega", salva.getId(), "REGISTRO", null,
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
