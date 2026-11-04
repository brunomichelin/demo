package com.example.demo.controllers;

import com.example.demo.dtos.VinhoPreferenciasRequest;
import com.example.demo.dtos.VinhoRequest;
import com.example.demo.dtos.VinhoResponse;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.models.Vinho;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.repositories.VinhoRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.regex.Pattern;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/v1/vinhos")
public class VinhoApiController {

    private static final int MAX_FOTO_LENGTH = 2_800_000;
    private static final Pattern FOTO_BASE64 = Pattern.compile("^data:image/(png|jpeg|webp|gif);base64,[A-Za-z0-9+/=]+$");

    private final VinhoRepository vinhoRepository;
    private final UsuarioRepository usuarioRepository;

    public VinhoApiController(VinhoRepository vinhoRepository, UsuarioRepository usuarioRepository) {
        this.vinhoRepository = vinhoRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @GetMapping
    @Transactional(readOnly = true)
    public List<VinhoResponse> listar(Authentication authentication) {
        return vinhoRepository.findAllByUsuarioEmailIgnoreCaseAndDataDelecaoIsNullOrderByDataCriacaoDesc(
                        authentication.getName())
                .stream()
                .map(VinhoResponse::from)
                .toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Transactional
    public VinhoResponse criar(@RequestBody VinhoRequest request, Authentication authentication) {
        validar(request);
        String email = authentication.getName();
        Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Usuário autenticado não encontrado."));
        Vinho vinho = new Vinho();
        vinho.setUsuario(usuario);
        preencher(vinho, request);
        vinho.setDataCriacao(LocalDateTime.now());
        vinho.setCriadoPor(email);
        vinho.setStatusRegistro(StatusRegEnum.ATIVO);
        return VinhoResponse.from(vinhoRepository.save(vinho));
    }

    @PutMapping("/{id}")
    @Transactional
    public VinhoResponse atualizar(
            @PathVariable Long id,
            @RequestBody VinhoRequest request,
            Authentication authentication
    ) {
        validar(request);
        Vinho vinho = obterVinho(id, authentication.getName());
        preencher(vinho, request);
        vinho.setDataAtualizacao(LocalDateTime.now());
        return VinhoResponse.from(vinhoRepository.save(vinho));
    }

    @PatchMapping("/{id}/preferencias")
    @Transactional
    public VinhoResponse atualizarPreferencias(
            @PathVariable Long id,
            @RequestBody VinhoPreferenciasRequest request,
            Authentication authentication
    ) {
        if (request == null || (request.favorito() == null && request.queroBeber() == null)) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe ao menos uma preferência para atualizar.");
        }
        Vinho vinho = obterVinho(id, authentication.getName());
        if (request.favorito() != null) {
            vinho.setFavorito(request.favorito());
        }
        if (request.queroBeber() != null) {
            vinho.setQueroBeber(request.queroBeber());
        }
        vinho.setDataAtualizacao(LocalDateTime.now());
        return VinhoResponse.from(vinhoRepository.save(vinho));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void excluir(@PathVariable Long id, Authentication authentication) {
        Vinho vinho = obterVinho(id, authentication.getName());
        vinho.setDataDelecao(LocalDateTime.now());
        vinho.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        vinhoRepository.save(vinho);
    }

    private void validar(VinhoRequest request) {
        if (request == null || !StringUtils.hasText(request.nome())) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe o nome do vinho.");
        }
        if (tooLong(request.nome(), 100) || tooLong(request.produtor(), 100)
                || tooLong(request.pais(), 60) || tooLong(request.regiao(), 100)
                || tooLong(request.denominacao(), 80) || tooLong(request.tipo(), 30)
                || tooLong(request.uvas(), 160) || tooLong(request.corte(), 120)
                || tooLong(request.corpo(), 30) || tooLong(request.madeira(), 100)
                || tooLong(request.ocasiao(), 140) || tooLong(request.companhia(), 100)
                || tooLong(request.aparencia(), 200) || tooLong(request.aroma(), 300)
                || tooLong(request.saborFinal(), 300) || tooLong(request.harmonizacao(), 200)
                || tooLong(request.comprariaNovamente(), 20) || tooLong(request.localCompra(), 120)
                || tooLong(request.temperaturaServico(), 40) || tooLong(request.decantado(), 10)
                || tooLong(request.potencialGuarda(), 80)) {
            throw new ResponseStatusException(BAD_REQUEST, "Um ou mais campos excedem o tamanho permitido.");
        }
        if (request.safra() != null && (request.safra() < 1900 || request.safra() > 2100)) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe uma safra válida.");
        }
        if (request.nota() != null && (request.nota() < 1 || request.nota() > 5)) {
            throw new ResponseStatusException(BAD_REQUEST, "A nota deve ficar entre 1 e 5.");
        }
        if (request.teorAlcoolico() != null
                && (request.teorAlcoolico().signum() < 0 || request.teorAlcoolico().doubleValue() > 100
                || !fitsDecimal(request.teorAlcoolico(), 5, 2))) {
            throw new ResponseStatusException(BAD_REQUEST, "O teor alcoólico deve ficar entre 0 e 100.");
        }
        if (request.precoPago() != null && request.precoPago().signum() < 0) {
            throw new ResponseStatusException(BAD_REQUEST, "O preço não pode ser negativo.");
        }
        if (request.precoPago() != null && !fitsDecimal(request.precoPago(), 10, 2)) {
            throw new ResponseStatusException(BAD_REQUEST, "O preço deve ter no máximo 8 dígitos inteiros e 2 decimais.");
        }
        if (request.foto() != null && (!StringUtils.hasText(request.foto())
                || request.foto().length() > MAX_FOTO_LENGTH
                || !FOTO_BASE64.matcher(request.foto()).matches())) {
            throw new ResponseStatusException(BAD_REQUEST,
                    "A foto deve ser PNG, JPEG, WebP ou GIF e ter no máximo 2 MB.");
        }
    }

    private void preencher(Vinho vinho, VinhoRequest request) {
        vinho.setNome(request.nome().trim());
        vinho.setProdutor(limpar(request.produtor()));
        vinho.setSafra(request.safra());
        vinho.setPais(limpar(request.pais()));
        vinho.setRegiao(limpar(request.regiao()));
        vinho.setDenominacao(limpar(request.denominacao()));
        vinho.setTipo(limpar(request.tipo()));
        vinho.setUvas(limpar(request.uvas()));
        vinho.setCorte(limpar(request.corte()));
        vinho.setTeorAlcoolico(request.teorAlcoolico());
        vinho.setCorpo(limpar(request.corpo()));
        vinho.setMadeira(limpar(request.madeira()));
        vinho.setDataDegustacao(request.dataDegustacao());
        vinho.setOcasiao(limpar(request.ocasiao()));
        vinho.setCompanhia(limpar(request.companhia()));
        vinho.setNota(request.nota());
        vinho.setAparencia(limpar(request.aparencia()));
        vinho.setAroma(limpar(request.aroma()));
        vinho.setSaborFinal(limpar(request.saborFinal()));
        vinho.setHarmonizacao(limpar(request.harmonizacao()));
        vinho.setComprariaNovamente(limpar(request.comprariaNovamente()));
        vinho.setPrecoPago(request.precoPago());
        vinho.setLocalCompra(limpar(request.localCompra()));
        vinho.setTemperaturaServico(limpar(request.temperaturaServico()));
        vinho.setDecantado(limpar(request.decantado()));
        vinho.setPotencialGuarda(limpar(request.potencialGuarda()));
        vinho.setFoto(limpar(request.foto()));
        if (request.favorito() != null) {
            vinho.setFavorito(request.favorito());
        }
        if (request.queroBeber() != null) {
            vinho.setQueroBeber(request.queroBeber());
        }
    }

    private Vinho obterVinho(Long id, String email) {
        return vinhoRepository.findByIdAndUsuarioEmailIgnoreCaseAndDataDelecaoIsNull(id, email)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "Vinho não encontrado."));
    }

    private String limpar(String value) {
        return StringUtils.hasText(value) ? value.trim() : null;
    }

    private boolean tooLong(String value, int maxLength) {
        return value != null && value.length() > maxLength;
    }

    private boolean fitsDecimal(BigDecimal value, int precision, int scale) {
        return value.scale() <= scale && value.precision() - value.scale() <= precision - scale;
    }
}
