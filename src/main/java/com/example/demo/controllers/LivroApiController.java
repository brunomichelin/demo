package com.example.demo.controllers;

import com.example.demo.dtos.LivroBuscaRequest;
import com.example.demo.dtos.LivroCatalogoResponse;
import com.example.demo.dtos.LivroRequest;
import com.example.demo.models.Livro;
import com.example.demo.models.LivroStatus;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.repositories.LivroRepository;
import com.example.demo.services.impl.LivroCatalogoService;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CONFLICT;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/livros")
public class LivroApiController {

    private final LivroRepository livroRepository;
    private final LivroCatalogoService catalogoService;

    public LivroApiController(LivroRepository livroRepository, LivroCatalogoService catalogoService) {
        this.livroRepository = livroRepository;
        this.catalogoService = catalogoService;
    }

    @PostMapping("/catalogo")
    public LivroCatalogoResponse buscarCatalogo(@RequestBody LivroBuscaRequest request) {
        return catalogoService.buscar(request);
    }

    @GetMapping
    public List<Livro> listar(Authentication authentication) {
        return livroRepository.findAllByDonoUsuarioAndDataDelecaoIsNullOrderByDataCriacaoDesc(
                authentication.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public Livro criar(@RequestBody LivroRequest request, Authentication authentication) {
        validar(request);
        String dono = authentication.getName();
        String isbn = limpar(request.isbn());
        validarIsbnUnico(dono, isbn);

        Livro livro = new Livro();
        preencher(livro, request, dono);
        livro.setDataCriacao(LocalDateTime.now());
        return livroRepository.save(livro);
    }

    @PutMapping("/{id}")
    public Livro atualizar(@PathVariable Long id, @RequestBody LivroRequest request, Authentication authentication) {
        validar(request);
        Livro livro = obterLivro(id, authentication.getName());
        String isbn = limpar(request.isbn());
        if (isbn != null && !isbn.equalsIgnoreCase(livro.getIsbn())) {
            validarIsbnUnico(authentication.getName(), isbn);
        }
        preencher(livro, request, authentication.getName());
        livro.setDataAtualizacao(LocalDateTime.now());
        return livroRepository.save(livro);
    }

    @PatchMapping("/{id}/status")
    public Livro atualizarStatus(
            @PathVariable Long id,
            @RequestBody LivroRequest request,
            Authentication authentication
    ) {
        LivroStatus status = converterStatus(request == null ? null : request.status());
        Livro livro = obterLivro(id, authentication.getName());
        livro.setStatus(status);
        livro.setDataAtualizacao(LocalDateTime.now());
        return livroRepository.save(livro);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void excluir(@PathVariable Long id, Authentication authentication) {
        Livro livro = obterLivro(id, authentication.getName());
        livro.setDataDelecao(LocalDateTime.now());
        livro.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        livroRepository.save(livro);
    }

    private void validar(LivroRequest request) {
        if (request == null || !StringUtils.hasText(request.titulo())) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe o título do livro.");
        }
        if (request.titulo().trim().length() > 300
                || tamanho(request.autores()) > 500
                || tamanho(request.isbn()) > 30
                || tamanho(request.editora()) > 200
                || tamanho(request.anoLancamento()) > 20) {
            throw new ResponseStatusException(BAD_REQUEST, "Um ou mais campos excedem o tamanho permitido.");
        }
        converterStatus(request.status());
    }

    private void preencher(Livro livro, LivroRequest request, String dono) {
        livro.setTitulo(request.titulo().trim());
        livro.setAutores(limpar(request.autores()));
        livro.setIsbn(limpar(request.isbn()));
        livro.setEditora(limpar(request.editora()));
        livro.setAnoLancamento(limpar(request.anoLancamento()));
        livro.setStatus(converterStatus(request.status()));
        livro.setDonoUsuario(dono);
    }

    private LivroStatus converterStatus(String status) {
        if ("LIDO".equals(status)) {
            return LivroStatus.LIDO;
        }
        if ("LENDO".equals(status)) {
            return LivroStatus.LENDO;
        }
        if ("DESEJADO".equals(status)) {
            return LivroStatus.DESEJADO;
        }
        throw new ResponseStatusException(BAD_REQUEST, "Selecione se o livro foi lido, está sendo lido ou é desejado.");
    }

    private void validarIsbnUnico(String dono, String isbn) {
        if (isbn != null && livroRepository.existsByDonoUsuarioAndIsbnIgnoreCaseAndDataDelecaoIsNull(dono, isbn)) {
            throw new ResponseStatusException(CONFLICT, "Este ISBN já está na sua biblioteca.");
        }
    }

    private Livro obterLivro(Long id, String dono) {
        return livroRepository.findByIdAndDonoUsuarioAndDataDelecaoIsNull(id, dono)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Livro não encontrado."));
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private int tamanho(String valor) {
        return valor == null ? 0 : valor.length();
    }
}
