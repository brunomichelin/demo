package com.example.demo.services.impl;

import com.example.demo.dtos.*;
import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import com.example.demo.repositories.EtapaRepository;
import com.example.demo.repositories.TarefaRepository;
import com.example.demo.repositories.EntregaRepository;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.StatusEntregaEnum;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.time.LocalDateTime;
import com.example.demo.models.StatusRegEnum;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import java.util.stream.Collectors;

@Service
public class EtapaService {
    private final EtapaRepository etapaRepository;
    private final TarefaRepository tarefaRepository;
    private final EntregaRepository entregaRepository;

    public EtapaService(
            EtapaRepository etapaRepository,
            TarefaRepository tarefaRepository,
            EntregaRepository entregaRepository
    ) {
        this.etapaRepository = etapaRepository;
        this.tarefaRepository = tarefaRepository;
        this.entregaRepository = entregaRepository;
    }

    public Page<Etapa> listarEtapasObra(Long idObra, Pageable pageable) {
        return etapaRepository.findAllByObraIdOrderByOrdem(idObra, pageable);
    }

    public List<EtapaResponse> criarEtapas(Long idObra, List<EtapaRequest> etapaRequests) {
        List<Etapa> existentes = etapaRepository.findByObraIdOrderByOrdemAsc(idObra);
        if (etapaRequests == null || etapaRequests.stream().anyMatch(request ->
                request.getOrdem() == null || request.getOrdem() <= 0 || request.getNome() == null
                        || request.getNome().isBlank())
                || etapaRequests.stream().map(EtapaRequest::getOrdem).distinct().count() != etapaRequests.size()
                || etapaRequests.stream().anyMatch(request -> existentes.stream()
                .anyMatch(etapa -> etapa.getOrdem().equals(request.getOrdem())))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Cada etapa deve ter nome e uma ordem positiva única dentro da obra.");
        }
        Obra obra = new Obra();
        obra.setId(idObra);

        List<Etapa> etapas = etapaRequests.stream()
                .map(request -> Etapa.builder()
                        .nome(request.getNome())
                        .ordem(request.getOrdem())
                        .descricao(request.getDescricao())
                        .dataInicioPrevista(request.getDataInicioPrevista())
                        .dataEntregaPrevista(request.getDataEntregaPrevista())
                        .status(request.getStatus())
                        .obra(obra)
                        .build())
                .collect(Collectors.toList());

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        String username = authentication.getName();

        etapas.forEach(etapa -> {
            etapa.setCriadoPor(username);
            etapa.setDataCriacao(java.time.LocalDateTime.now());
        });

        etapaRepository.saveAll(etapas);

        return etapas.stream()
                .map(etapa -> EtapaResponse.builder()
                        .id(etapa.getId())
                        .ordem(etapa.getOrdem())
                        .nome(etapa.getNome())
                        .descricao(etapa.getDescricao())
                        .dataInicioPrevista(etapa.getDataInicioPrevista())
                        .dataEntregaPrevista(etapa.getDataEntregaPrevista())
                        .status(etapa.getStatus())
                        .build())
                .toList();
    }

    public EtapaResponse buscarEtapaPorId(Long id) {
        return etapaRepository.findById(id)
                .map(etapa -> EtapaResponse.builder()
                        .id(etapa.getId())
                        .ordem(etapa.getOrdem())
                        .nome(etapa.getNome())
                        .descricao(etapa.getDescricao())
                        .dataInicioPrevista(etapa.getDataInicioPrevista())
                        .dataEntregaPrevista(etapa.getDataEntregaPrevista())
                        .status(etapa.getStatus())
                        .build())
                .orElse(null);
    }

    public EtapaResponse atualizarEtapa(Long id, EtapaRequest etapaRequest) {
        if (etapaRequest == null || etapaRequest.getOrdem() == null || etapaRequest.getOrdem() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe uma ordem positiva para a etapa.");
        }
        return etapaRepository.findById(id)
                .map(etapa -> {
                    boolean ordemEmUso = etapaRepository.findByObraIdOrderByOrdemAsc(etapa.getObra().getId()).stream()
                            .anyMatch(outra -> !outra.getId().equals(id)
                                    && outra.getOrdem().equals(etapaRequest.getOrdem()));
                    if (ordemEmUso) {
                        throw new ResponseStatusException(HttpStatus.CONFLICT,
                                "A ordem já está sendo usada por outra etapa da obra.");
                    }
                    validarPodeConcluir(etapa, etapaRequest.getStatus());
                    etapa.setOrdem(etapaRequest.getOrdem());
                    etapa.setNome(etapaRequest.getNome());
                    etapa.setDescricao(etapaRequest.getDescricao());
                    etapa.setDataInicioPrevista(etapaRequest.getDataInicioPrevista());
                    etapa.setDataEntregaPrevista(etapaRequest.getDataEntregaPrevista());
                    etapa.setStatus(etapaRequest.getStatus());
                    etapa.setDataAtualizacao(LocalDateTime.now());

                    etapa = etapaRepository.save(etapa);

                    return EtapaResponse.builder()
                            .id(etapa.getId())
                            .nome(etapa.getNome())
                            .descricao(etapa.getDescricao())
                            .dataInicioPrevista(etapa.getDataInicioPrevista())
                            .dataEntregaPrevista(etapa.getDataEntregaPrevista())
                            .status(etapa.getStatus())
                            .build();
                })
                .orElse(null);
    }

    public boolean deletarEtapa(Long id) {
        return etapaRepository.findById(id).map(etapa -> {
            etapa.setDataDelecao(LocalDateTime.now());
            etapa.setStatusRegistro(StatusRegEnum.EXCLUIDO);
            etapaRepository.save(etapa);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Etapa atualizarStatus(Long id, StatusCronEnum status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean podeGerenciar = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_GESTOR".equals(authority.getAuthority()));
        if (!podeGerenciar) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para alterar o status desta etapa.");
        }
        if (status == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Informe um status válido.");
        }
        Etapa etapa = etapaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        if (!transicaoDeStatusPermitida(etapa.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A transição solicitada não está disponível.");
        }
        validarPodeConcluir(etapa, status);
        etapa.setStatus(status);
        etapa.setDataAtualizacao(LocalDateTime.now());
        return etapaRepository.save(etapa);
    }

    private boolean transicaoDeStatusPermitida(StatusCronEnum atual, StatusCronEnum proximo) {
        if (atual == null) {
            return false;
        }
        return switch (atual) {
            case PLANEJAMENTO -> proximo == StatusCronEnum.EM_ANDAMENTO
                    || proximo == StatusCronEnum.EM_BLOQUEIO || proximo == StatusCronEnum.CANCELADA;
            case EM_ANDAMENTO -> proximo == StatusCronEnum.CONCLUIDA
                    || proximo == StatusCronEnum.EM_BLOQUEIO || proximo == StatusCronEnum.CANCELADA;
            case EM_BLOQUEIO -> proximo == StatusCronEnum.EM_ANDAMENTO || proximo == StatusCronEnum.CANCELADA;
            case PAUSADA -> proximo == StatusCronEnum.EM_ANDAMENTO
                    || proximo == StatusCronEnum.EM_BLOQUEIO || proximo == StatusCronEnum.CANCELADA;
            case CONCLUIDA, CANCELADA -> false;
        };
    }

    public Etapa reordenar(Long id, Long novaOrdem) {
        if (novaOrdem == null || novaOrdem <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A ordem deve ser um número positivo.");
        }
        Etapa etapa = etapaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Etapa não encontrada."));
        boolean ordemEmUso = etapaRepository.findByObraIdOrderByOrdemAsc(etapa.getObra().getId()).stream()
                .anyMatch(outra -> !outra.getId().equals(id) && outra.getOrdem().equals(novaOrdem));
        if (ordemEmUso) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "A ordem já está sendo usada por outra etapa da obra.");
        }
        etapa.setOrdem(novaOrdem);
        etapa.setDataAtualizacao(LocalDateTime.now());
        return etapaRepository.save(etapa);
    }

    public void validarPodeConcluir(Etapa etapa, StatusCronEnum status) {
        if (status != StatusCronEnum.CONCLUIDA) {
            return;
        }
        boolean tarefasPendentes = tarefaRepository.findByEtapaId(etapa.getId()).stream()
                .anyMatch(tarefa -> tarefa.getStatus() != StatusCronEnum.CONCLUIDA);
        if (tarefasPendentes) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A etapa só pode ser concluída quando todas as tarefas estiverem concluídas.");
        }
        boolean entregaAprovada = entregaRepository.findByEtapaId(etapa.getId())
                .map(entrega -> entrega.getStatus() == StatusEntregaEnum.APROVADA)
                .orElse(false);
        if (!entregaAprovada) {
            throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                    "A etapa só pode ser concluída após a aprovação da entrega.");
        }
    }

}
