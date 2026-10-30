package com.example.demo.controllers;

import com.example.demo.models.Fornecedor;
import com.example.demo.models.Insumo;
import com.example.demo.dtos.InsumoApiRequest;
import com.example.demo.repositories.FornecedorRepository;
import com.example.demo.repositories.InsumoRepository;
import com.example.demo.repositories.HistoricoPrecoInsumoRepository;
import com.example.demo.services.impl.HistoricoPrecoInsumoService;
import com.example.demo.models.HistoricoPrecoInsumo;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import com.example.demo.models.StatusRegEnum;

@RestController
@RequestMapping("/api/v1")
public class CatalogoApiController {
    private final InsumoRepository insumoRepository;
    private final FornecedorRepository fornecedorRepository;
    private final HistoricoPrecoInsumoRepository historicoPrecoRepository;
    private final HistoricoPrecoInsumoService historicoPrecoService;

    public CatalogoApiController(
            InsumoRepository insumoRepository,
            FornecedorRepository fornecedorRepository,
            HistoricoPrecoInsumoRepository historicoPrecoRepository,
            HistoricoPrecoInsumoService historicoPrecoService
    ) {
        this.insumoRepository = insumoRepository;
        this.fornecedorRepository = fornecedorRepository;
        this.historicoPrecoRepository = historicoPrecoRepository;
        this.historicoPrecoService = historicoPrecoService;
    }

    @GetMapping("/insumos")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA','FINANCEIRO','MESTRE_OBRA','TECNICO_OBRA')")
    public Page<Insumo> listarInsumos(Pageable pageable) {
        return insumoRepository.findAll(pageable);
    }

    @PostMapping("/insumos")
    @PreAuthorize("hasAnyRole('ADMIN','ORCAMENTISTA')")
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public Insumo criarInsumo(@RequestBody InsumoApiRequest request) {
        validarInsumo(request);
        if (insumoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O código informado já está cadastrado.");
        }
        Insumo insumo = new Insumo();
        preencherInsumo(insumo, request);
        insumo.setDataCriacao(LocalDateTime.now());
        Insumo salvo = insumoRepository.save(insumo);
        historicoPrecoService.registrar(salvo, null, salvo.getPrecoReferencia());
        return salvo;
    }

    @PutMapping("/insumos/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORCAMENTISTA')")
    @Transactional
    public Insumo atualizarInsumo(@PathVariable Long id, @RequestBody InsumoApiRequest request) {
        validarInsumo(request);
        Insumo insumo = insumoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo não encontrado."));
        if ((insumo.getCodigo() == null || !insumo.getCodigo().equalsIgnoreCase(request.codigo().trim()))
                && insumoRepository.existsByCodigoIgnoreCase(request.codigo().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "O código informado já está cadastrado.");
        }
        BigDecimal precoAnterior = insumo.getPrecoReferencia();
        preencherInsumo(insumo, request);
        insumo.setDataAtualizacao(LocalDateTime.now());
        Insumo salvo = insumoRepository.save(insumo);
        historicoPrecoService.registrar(salvo, precoAnterior, salvo.getPrecoReferencia());
        return salvo;
    }

    @GetMapping("/insumos/{id}/historico-precos")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA','FINANCEIRO')")
    public java.util.List<HistoricoPrecoInsumo> historicoPrecos(@PathVariable Long id) {
        obterInsumo(id);
        return historicoPrecoRepository.findByInsumoIdOrderByRegistradoEmDesc(id);
    }

    @DeleteMapping("/insumos/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirInsumo(@PathVariable Long id) {
        Insumo insumo = obterInsumo(id);
        insumo.setDataDelecao(LocalDateTime.now());
        insumo.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        insumoRepository.save(insumo);
    }

    @GetMapping("/fornecedores")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ORCAMENTISTA','FINANCEIRO')")
    public Page<Fornecedor> listarFornecedores(Pageable pageable) {
        return fornecedorRepository.findAll(pageable);
    }

    @PostMapping("/fornecedores")
    @PreAuthorize("hasAnyRole('ADMIN','ORCAMENTISTA','FINANCEIRO')")
    @ResponseStatus(HttpStatus.CREATED)
    public Fornecedor criarFornecedor(@RequestBody Fornecedor request) {
        validarFornecedor(request);
        request.setId(null);
        request.setDataCriacao(LocalDateTime.now());
        return fornecedorRepository.save(request);
    }

    @PutMapping("/fornecedores/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','ORCAMENTISTA','FINANCEIRO')")
    public Fornecedor atualizarFornecedor(@PathVariable Long id, @RequestBody Fornecedor request) {
        validarFornecedor(request);
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado."));
        fornecedor.setRazaoSocial(request.getRazaoSocial());
        fornecedor.setCnpj(request.getCnpj());
        fornecedor.setTelefone(request.getTelefone());
        fornecedor.setEmail(request.getEmail());
        fornecedor.setDataAtualizacao(LocalDateTime.now());
        return fornecedorRepository.save(fornecedor);
    }

    @DeleteMapping("/fornecedores/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void excluirFornecedor(@PathVariable Long id) {
        Fornecedor fornecedor = fornecedorRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado."));
        fornecedor.setDataDelecao(LocalDateTime.now());
        fornecedor.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        fornecedorRepository.save(fornecedor);
    }

    private void validarInsumo(InsumoApiRequest insumo) {
        if (insumo == null || !StringUtils.hasText(insumo.codigo()) || !StringUtils.hasText(insumo.descricao())
                || !StringUtils.hasText(insumo.tipo()) || insumo.unidade() == null
                || insumo.precoReferencia() == null || insumo.precoReferencia().compareTo(BigDecimal.ZERO) <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe código, descrição, tipo, unidade e preço de referência maior que zero.");
        }
        if (!java.util.Set.of("Material", "Mão de obra", "Equipamento", "Serviço")
                .contains(insumo.tipo().trim())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "O tipo de insumo informado não é válido.");
        }
    }

    private void preencherInsumo(Insumo insumo, InsumoApiRequest request) {
        insumo.setCodigo(request.codigo().trim());
        insumo.setDescricao(request.descricao().trim());
        insumo.setTipo(request.tipo().trim());
        insumo.setUnidade(request.unidade());
        insumo.setPrecoReferencia(request.precoReferencia());
        Fornecedor fornecedor = request.fornecedorId() == null ? null : fornecedorRepository.findById(
                request.fornecedorId()).orElseThrow(() ->
                new ResponseStatusException(HttpStatus.NOT_FOUND, "Fornecedor não encontrado."));
        insumo.setFornecedor(fornecedor);
    }

    private void validarFornecedor(Fornecedor fornecedor) {
        if (fornecedor == null || !StringUtils.hasText(fornecedor.getRazaoSocial())
                || !StringUtils.hasText(fornecedor.getCnpj())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Informe a razão social e o CNPJ do fornecedor.");
        }
    }

    private Insumo obterInsumo(Long id) {
        return insumoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Insumo não encontrado."));
    }
}
