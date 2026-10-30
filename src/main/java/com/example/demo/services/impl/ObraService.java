package com.example.demo.services.impl;

import com.example.demo.dtos.ObraPainelResponse;
import com.example.demo.dtos.ObraRequest;
import com.example.demo.dtos.ObraResponse;
import com.example.demo.models.Obra;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.dtos.RelatorioCustoEtapaResponse;
import com.example.demo.models.StatusCronEnum;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.math.BigDecimal;
import java.util.List;

@Service
public class ObraService {

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private RelatorioService relatorioService;

    public Page<Obra> listarObras(Pageable pageable) {
        return listarObras(null, null, null, null, pageable);
    }

    public Page<Obra> listarObras(
            StatusCronEnum status,
            Long clienteId,
            LocalDate inicio,
            LocalDate fim,
            Pageable pageable
    ) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = isAdministrador(authentication) ? null : usuarioAtualId(authentication);
        return obraRepository.findFiltered(status, clienteId, inicio, fim, usuarioId, pageable);
    }

    public ObraResponse criarObra(ObraRequest obraRequest) {
        validarObra(obraRequest);
        List<Usuario> clientes = obraRequest.getClientes() == null ? List.of()
                : usuarioRepository.findAllById(obraRequest.getClientes().stream()
                .filter(java.util.Objects::nonNull).map(Usuario::getId).filter(java.util.Objects::nonNull).toList());
        if (obraRequest.getClientes() != null
                && clientes.size() != obraRequest.getClientes().stream().filter(java.util.Objects::nonNull)
                .map(Usuario::getId).filter(java.util.Objects::nonNull).distinct().count()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais clientes informados não existem.");
        }
        List<Usuario> vinculados = usuariosPorIds(obraRequest.getUsuariosVinculadosIds());
        incluirUsuarioAtual(vinculados);
        Obra obra = Obra.builder()
                .nome(obraRequest.getNome())
                .endereco(obraRequest.getEndereco())
                .area(obraRequest.getArea())
                .bdi(obraRequest.getBdi() == null ? BigDecimal.ZERO : obraRequest.getBdi())
                .dataInicioPrevista(obraRequest.getDataInicioPrevista())
                .dataFimPrevista(obraRequest.getDataFimPrevista())
                .status(obraRequest.getStatus())
                .clientes(clientes)
                .usuariosVinculados(vinculados)
                .build();

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        obra.setCriadoPor(authentication == null ? null : authentication.getName());
        obra.setDataCriacao(java.time.LocalDateTime.now());

        obra = obraRepository.save(obra);

        return ObraResponse.builder()
                .id(obra.getId())
                .nome(obra.getNome())
                .endereco(obra.getEndereco())
                .area(obra.getArea())
                .bdi(obra.getBdi())
                .dataInicioPrevista(obra.getDataInicioPrevista())
                .dataFimPrevista(obra.getDataFimPrevista())
                .status(obra.getStatus())
                .clientes(obra.getClientes())
                .build();
    }

    public ObraResponse buscarObraPorId(Long id) {
        return obraRepository.findWithClientesById(id)
                .filter(obra -> obra.getDataDelecao() == null)
                .map(this::validarAcesso)
                .map(obra -> ObraResponse.builder()
                        .id(obra.getId())
                        .nome(obra.getNome())
                        .endereco(obra.getEndereco())
                        .area(obra.getArea())
                        .bdi(obra.getBdi())
                        .dataInicioPrevista(obra.getDataInicioPrevista())
                        .dataFimPrevista(obra.getDataFimPrevista())
                        .status(obra.getStatus())
                        .clientes(obra.getClientes())
                        .build())
                .orElse(null);
    }

    public ObraResponse atualizarObra(Long id, ObraRequest obraRequest) {
        validarObra(obraRequest);
        return obraRepository.findWithClientesById(id)
                .filter(obra -> obra.getDataDelecao() == null)
                .map(this::validarAcesso)
                .map(obra -> {
                    obra.setNome(obraRequest.getNome());
                    obra.setEndereco(obraRequest.getEndereco());
                    obra.setArea(obraRequest.getArea());
                    obra.setBdi(obraRequest.getBdi() == null ? BigDecimal.ZERO : obraRequest.getBdi());
                    obra.setDataInicioPrevista(obraRequest.getDataInicioPrevista());
                    obra.setDataFimPrevista(obraRequest.getDataFimPrevista());
                    obra.setStatus(obraRequest.getStatus());
                    List<Usuario> clientes = obraRequest.getClientes() == null ? List.of()
                            : usuarioRepository.findAllById(obraRequest.getClientes().stream()
                            .filter(java.util.Objects::nonNull).map(Usuario::getId)
                            .filter(java.util.Objects::nonNull).toList());
                    if (obraRequest.getClientes() != null
                            && clientes.size() != obraRequest.getClientes().stream()
                            .filter(java.util.Objects::nonNull).map(Usuario::getId)
                            .filter(java.util.Objects::nonNull)
                            .distinct().count()) {
                        throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                                "Um ou mais clientes informados não existem.");
                    }
                    obra.setClientes(clientes);
                    if (obraRequest.getUsuariosVinculadosIds() != null) {
                        List<Usuario> vinculados = usuariosPorIds(obraRequest.getUsuariosVinculadosIds());
                        incluirUsuarioAtual(vinculados);
                        obra.setUsuariosVinculados(vinculados);
                    }
                    obra.setDataAtualizacao(LocalDateTime.now());

                    obra = obraRepository.save(obra);

                    return ObraResponse.builder()
                            .id(obra.getId())
                            .nome(obra.getNome())
                            .endereco(obra.getEndereco())
                            .area(obra.getArea())
                            .bdi(obra.getBdi())
                            .dataInicioPrevista(obra.getDataInicioPrevista())
                            .dataFimPrevista(obra.getDataFimPrevista())
                            .status(obra.getStatus())
                            .clientes(obra.getClientes())
                            .build();
                })
                .orElse(null);
    }

    public boolean deletarObra(Long id) {
        return obraRepository.findWithClientesById(id).filter(obra -> obra.getDataDelecao() == null)
                .map(this::validarAcesso).map(obra -> {
            obra.setDataDelecao(LocalDateTime.now());
            obra.setStatusRegistro(StatusRegEnum.EXCLUIDO);
            obraRepository.save(obra);
            return true;
        }).orElse(false);
    }

    @Transactional
    public Obra atualizarStatus(Long id, StatusCronEnum status) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (status == null || !podeGerenciarObra(authentication)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN,
                    "Você não tem permissão para alterar o status desta obra.");
        }
        Obra obra = obraRepository.findWithClientesById(id)
                .filter(item -> item.getDataDelecao() == null)
                .map(this::validarAcesso)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Obra não encontrada."));
        if (!transicaoDeStatusPermitida(obra.getStatus(), status)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "A transição solicitada não está disponível.");
        }
        obra.setStatus(status);
        obra.setDataAtualizacao(LocalDateTime.now());
        return obraRepository.save(obra);
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

    private void validarObra(ObraRequest request) {
        if (request == null || request.getNome() == null || request.getNome().isBlank()
                || request.getEndereco() == null || request.getEndereco().isBlank()
                || request.getArea() == null || request.getArea() <= 0
                || request.getStatus() == null
                || request.getDataInicioPrevista() == null || request.getDataFimPrevista() == null
                || request.getDataFimPrevista().isBefore(request.getDataInicioPrevista())
                || request.getBdi() != null && request.getBdi().compareTo(BigDecimal.ZERO) < 0
                || request.getArea() != null && request.getArea() <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe nome, datas válidas e valores de área/BDI não negativos.");
        }
    }

    private Obra validarAcesso(Obra obra) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAdministrador(authentication)) {
            return obra;
        }
        Long usuarioId = usuarioAtualId(authentication);
        boolean vinculado = obraRepository.usuarioClienteDaObra(obra.getId(), usuarioId)
                || obraRepository.usuarioVinculadoAObra(obra.getId(), usuarioId);
        if (!vinculado) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Você não está vinculado a esta obra.");
        }
        return obra;
    }

    private List<Usuario> usuariosPorIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new java.util.ArrayList<>();
        }
        List<Long> distintos = ids.stream().filter(java.util.Objects::nonNull).distinct().toList();
        List<Usuario> usuarios = new java.util.ArrayList<>(usuarioRepository.findAllById(distintos));
        if (usuarios.size() != distintos.size()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Um ou mais usuários vinculados não existem.");
        }
        return usuarios;
    }

    private void incluirUsuarioAtual(List<Usuario> vinculados) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (isAdministrador(authentication) || authentication == null) {
            return;
        }
        Usuario atual = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado inválido."));
        if (vinculados.stream().noneMatch(usuario -> usuario.getId().equals(atual.getId()))) {
            vinculados.add(atual);
        }
    }

    private Long usuarioAtualId(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Autenticação obrigatória.");
        }
        return usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                .map(Usuario::getId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário autenticado inválido."));
    }

    private boolean isAdministrador(Authentication authentication) {
        return authentication != null && authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"));
    }

    private boolean podeGerenciarObra(Authentication authentication) {
        return authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority())
                        || "ROLE_GESTOR".equals(authority.getAuthority()));
    }

    public ObraPainelResponse obterPainelObra(Long id) {
        return obraRepository.findWithClientesById(id).filter(obra -> obra.getDataDelecao() == null)
                .map(this::validarAcesso).map(obra -> {
            RelatorioCustoEtapaResponse custos = relatorioService.custosPorEtapa(id, null);
            java.math.BigDecimal avancoNumerador = custos.etapas().stream()
                    .map(etapa -> java.math.BigDecimal.valueOf(etapa.tarefas().concluida()))
                    .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
            java.math.BigDecimal percentualAvanco = custos.totais().totalTarefas() == 0
                    ? java.math.BigDecimal.ZERO.setScale(2)
                    : avancoNumerador.multiply(java.math.BigDecimal.valueOf(100))
                    .divide(java.math.BigDecimal.valueOf(custos.totais().totalTarefas()), 2,
                            java.math.RoundingMode.HALF_UP);
            java.math.BigDecimal percentualBdi = obra.getBdi() == null
                    ? java.math.BigDecimal.ZERO : obra.getBdi();
            java.math.BigDecimal precoVenda = custos.totais().custoOrcado()
                    .multiply(java.math.BigDecimal.ONE.add(percentualBdi
                            .divide(java.math.BigDecimal.valueOf(100), 4, java.math.RoundingMode.HALF_UP)))
                    .setScale(2, java.math.RoundingMode.HALF_UP);
            return ObraPainelResponse.builder()
                    .id(obra.getId())
                    .nome(obra.getNome())
                    .endereco(obra.getEndereco())
                    .area(obra.getArea())
                    .bdi(obra.getBdi())
                    .dataInicioPrevista(obra.getDataInicioPrevista())
                    .dataFimPrevista(obra.getDataFimPrevista())
                    .status(obra.getStatus())
                    .clientes(obra.getClientes())
                    .custoOrcadoTotal(custos.totais().custoOrcado())
                    .custoRealizadoTotal(custos.totais().custoRealizado())
                    .desvioTotal(custos.totais().custoRealizado().subtract(custos.totais().custoOrcado()))
                    .precoVendaEstimado(precoVenda)
                    .percentualAvanco(percentualAvanco)
                    .build();
        }).orElse(null);
    }
}
