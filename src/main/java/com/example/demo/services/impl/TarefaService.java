package com.example.demo.services.impl;

import com.example.demo.dtos.TarefaRequest;
import com.example.demo.models.*;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class TarefaService {
    private final TarefaRepository tarefaRepository;
    private final EtapaRepository etapaRepository;
    private final UsuarioRepository usuarioRepository;

    public TarefaService(
            TarefaRepository tarefaRepository,
            EtapaRepository etapaRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.tarefaRepository = tarefaRepository;
        this.etapaRepository = etapaRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public org.springframework.data.domain.Page<Tarefa> listar(
            Long etapaId,
            org.springframework.data.domain.Pageable pageable
    ) {
        if (etapaRepository.findById(etapaId).isEmpty()) {
            throw erro(HttpStatus.NOT_FOUND, "Etapa não encontrada.");
        }
        return tarefaRepository.findPageByEtapaId(etapaId, pageable);
    }

    @Transactional
    public Tarefa criar(Long etapaId, TarefaRequest request) {
        Tarefa tarefa = new Tarefa();
        atualizarCampos(tarefa, etapaId, request);
        tarefa.setDataCriacao(LocalDateTime.now());
        definirStatusAuditoria(tarefa, tarefa.getStatus());
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public Tarefa atualizar(Long tarefaId, TarefaRequest request) {
        Tarefa tarefa = buscar(tarefaId);
        StatusCronEnum statusAnterior = tarefa.getStatus();
        atualizarCampos(tarefa, request.etapaId(), request);
        if (statusAnterior != tarefa.getStatus()) {
            definirStatusAuditoria(tarefa, tarefa.getStatus());
        }
        tarefa.setDataAtualizacao(LocalDateTime.now());
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public Tarefa atualizarStatus(Long tarefaId, StatusCronEnum status) {
        if (status == null) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe um status válido.");
        }
        Tarefa tarefa = buscar(tarefaId);
        if ((status == StatusCronEnum.EM_ANDAMENTO || status == StatusCronEnum.CONCLUIDA)
                && tarefa.getPredecessora() != null
                && tarefa.getPredecessora().getStatus() != StatusCronEnum.CONCLUIDA) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A tarefa predecessora precisa estar concluída antes desta tarefa.");
        }
        tarefa.setStatus(status);
        definirStatusAuditoria(tarefa, status);
        tarefa.setDataAtualizacao(LocalDateTime.now());
        return tarefaRepository.save(tarefa);
    }

    @Transactional
    public void excluir(Long tarefaId) {
        Tarefa tarefa = buscar(tarefaId);
        tarefa.setDataDelecao(LocalDateTime.now());
        tarefa.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        tarefaRepository.save(tarefa);
    }

    private void atualizarCampos(Tarefa tarefa, Long etapaId, TarefaRequest request) {
        if (request == null || etapaId == null || !StringUtils.hasText(request.titulo())) {
            throw erro(HttpStatus.BAD_REQUEST, "Informe a etapa e o título da tarefa.");
        }
        Etapa etapa = etapaRepository.findById(etapaId)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        tarefa.setEtapa(etapa);
        tarefa.setTitulo(request.titulo().trim());
        tarefa.setDescricao(request.descricao());
        tarefa.setDataInicioPrevista(request.dataInicioPrevista());
        tarefa.setDataFimPrevista(request.dataFimPrevista());
        tarefa.setPrioridade(request.prioridade());
        if (request.dataInicioPrevista() != null && request.dataFimPrevista() != null
                && request.dataFimPrevista().isBefore(request.dataInicioPrevista())) {
            throw erro(HttpStatus.BAD_REQUEST, "A data final não pode anteceder a data inicial.");
        }
        if (request.status() != null) {
            if (!java.util.Set.of(StatusCronEnum.PLANEJAMENTO, StatusCronEnum.EM_ANDAMENTO,
                    StatusCronEnum.EM_BLOQUEIO, StatusCronEnum.CONCLUIDA, StatusCronEnum.CANCELADA)
                    .contains(request.status())) {
                throw erro(HttpStatus.BAD_REQUEST, "O status informado não é válido para tarefas.");
            }
            tarefa.setStatus(request.status());
        } else if (tarefa.getStatus() == null) {
            tarefa.setStatus(StatusCronEnum.PLANEJAMENTO);
        }

        Usuario responsavel = request.responsavelId() == null ? null : usuarioRepository.findById(request.responsavelId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Responsável não encontrado."));
        tarefa.setResponsavel(responsavel);

        Tarefa predecessora = request.predecessoraId() == null ? null : tarefaRepository
                .findById(request.predecessoraId())
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Tarefa predecessora não encontrada."));
        if (predecessora != null) {
            if (!predecessora.getEtapa().getObra().getId().equals(etapa.getObra().getId())) {
                throw erro(HttpStatus.BAD_REQUEST, "A predecessora deve pertencer à mesma obra.");
            }
            if (tarefa.getId() != null && predecessora.getId().equals(tarefa.getId())) {
                throw erro(HttpStatus.BAD_REQUEST, "Uma tarefa não pode depender dela mesma.");
            }
            verificarCiclo(tarefa, predecessora);
        }
        tarefa.setPredecessora(predecessora);
        validarPredecessora(tarefa);
    }

    private void validarPredecessora(Tarefa tarefa) {
        if ((tarefa.getStatus() == StatusCronEnum.EM_ANDAMENTO || tarefa.getStatus() == StatusCronEnum.CONCLUIDA)
                && tarefa.getPredecessora() != null
                && tarefa.getPredecessora().getStatus() != StatusCronEnum.CONCLUIDA) {
            throw erro(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A tarefa predecessora precisa estar concluída antes desta tarefa.");
        }
    }

    private void verificarCiclo(Tarefa tarefa, Tarefa predecessora) {
        Set<Long> visitadas = new HashSet<>();
        Tarefa atual = predecessora;
        while (atual != null && visitadas.add(atual.getId())) {
            if (tarefa.getId() != null && tarefa.getId().equals(atual.getId())) {
                throw erro(HttpStatus.BAD_REQUEST, "A dependência formaria um ciclo entre tarefas.");
            }
            atual = atual.getPredecessora();
        }
    }

    private void definirStatusAuditoria(Tarefa tarefa, StatusCronEnum status) {
        tarefa.setStatus(status);
        tarefa.setStatusAtualizadoEm(LocalDateTime.now());
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null) {
            tarefa.setStatusAtualizadoPor(authentication.getName());
        }
    }

    private Tarefa buscar(Long id) {
        return tarefaRepository.findById(id)
                .orElseThrow(() -> erro(HttpStatus.NOT_FOUND, "Tarefa não encontrada."));
    }

    private ResponseStatusException erro(HttpStatus status, String mensagem) {
        return new ResponseStatusException(status, mensagem);
    }
}
