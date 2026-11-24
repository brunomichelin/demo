package com.example.demo.beans;

import com.example.demo.dtos.LivroBuscaRequest;
import com.example.demo.dtos.LivroCatalogoResponse;
import com.example.demo.dtos.LivroCatalogoResultado;
import com.example.demo.models.Livro;
import com.example.demo.models.LivroStatus;
import com.example.demo.repositories.LivroRepository;
import com.example.demo.services.impl.LivroCatalogoService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.model.SelectItem;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ResponseStatusException;

import java.text.Normalizer;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Component("livrosBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class LivrosBean {

    private final LivroRepository livroRepository;
    private final LivroCatalogoService catalogoService;
    private List<Livro> livros = new ArrayList<>();
    private List<LivroCatalogoResultado> resultados = new ArrayList<>();
    private String termoBusca;
    private String campoBusca = "titulo";
    private String filtroStatus = "TODOS";
    private String filtroCompra = "TODOS";
    private String ordenarLivros = "titulo";
    private String mensagemBusca;
    private String mensagemLista;
    private int paginaBusca = 1;
    private int totalPaginasBusca = 1;

    public LivrosBean(LivroRepository livroRepository, LivroCatalogoService catalogoService) {
        this.livroRepository = livroRepository;
        this.catalogoService = catalogoService;
    }

    @PostConstruct
    public void iniciar() {
        carregarLivros();
    }

    @Transactional(readOnly = true)
    public void carregarLivros() {
        livros = livroRepository.findAllByDonoUsuarioAndDataDelecaoIsNullOrderByDataCriacaoDesc(emailAtual());
    }

    public void buscarCatalogo() {
        if (!StringUtils.hasText(termoBusca)) {
            mensagemBusca = "Informe um termo para buscar no catálogo.";
            resultados.clear();
            return;
        }
        paginaBusca = 1;
        consultarCatalogo(false);
    }

    public void carregarMaisResultados() {
        if (paginaBusca < totalPaginasBusca) {
            consultarCatalogo(true);
        }
    }

    private void consultarCatalogo(boolean acrescentar) {
        try {
            LivroCatalogoResponse resposta = catalogoService.buscar(
                    new LivroBuscaRequest(termoBusca.trim(), acrescentar ? paginaBusca + 1 : 1, campoBusca));
            paginaBusca = resposta.pagina();
            totalPaginasBusca = resposta.totalPaginas();
            if (acrescentar) {
                resultados.addAll(resposta.resultados());
            } else {
                resultados = new ArrayList<>(resposta.resultados());
            }
            mensagemBusca = resultados.isEmpty()
                    ? "Nenhum livro encontrado. Tente outro termo ou campo de busca."
                    : resultados.size() + " livro(s) exibido(s) no catálogo.";
        } catch (ResponseStatusException exception) {
            mensagemBusca = detalhe(exception);
        }
    }

    public void limparBusca() {
        termoBusca = null;
        campoBusca = "titulo";
        resultados.clear();
        paginaBusca = 1;
        totalPaginasBusca = 1;
        mensagemBusca = null;
    }

    public void marcarLivro(LivroCatalogoResultado resultado, String status) {
        try {
            Livro existente = localizarLivroSalvo(resultado);
            if (existente != null) {
                existente.setStatus(converterStatus(status));
                existente.setDataAtualizacao(LocalDateTime.now());
                livroRepository.save(existente);
            } else {
                Livro livro = new Livro();
                livro.setTitulo(resultado.titulo().trim());
                livro.setAutores(limpar(resultado.autores()));
                livro.setIsbn(limpar(resultado.isbn()));
                livro.setEditora(limpar(resultado.editora()));
                livro.setAnoLancamento(limpar(resultado.ano()));
                livro.setStatus(converterStatus(status));
                livro.setDonoUsuario(emailAtual());
                livro.setDataCriacao(LocalDateTime.now());
                livroRepository.save(livro);
            }
            carregarLivros();
            mensagemLista = null;
            mensagemBusca = "Livro marcado como \"" + statusLegivel(status) + "\".";
        } catch (ResponseStatusException exception) {
            mensagemBusca = detalhe(exception);
        }
    }

    public void alterarStatus(Long id, LivroStatus status) {
        atualizarLivro(id, livro -> livro.setStatus(status));
    }

    public void alterarCompra(Long id, boolean comprado) {
        atualizarLivro(id, livro -> livro.setComprado(comprado));
    }

    private void atualizarLivro(Long id, java.util.function.Consumer<Livro> alteracao) {
        String dono = emailAtual();
        livroRepository.findByIdAndDonoUsuarioAndDataDelecaoIsNull(id, dono).ifPresentOrElse(livro -> {
            alteracao.accept(livro);
            livro.setDataAtualizacao(LocalDateTime.now());
            livroRepository.save(livro);
            carregarLivros();
            mensagemLista = null;
        }, () -> mensagemLista = "O livro não foi encontrado na sua biblioteca. Atualize a página.");
    }

    public void limparMensagemLista() {
        mensagemLista = null;
    }

    private Livro localizarLivroSalvo(LivroCatalogoResultado resultado) {
        if (StringUtils.hasText(resultado.isbn())) {
            String isbn = normalizarIsbn(resultado.isbn());
            Livro porIsbn = livros.stream()
                    .filter(livro -> StringUtils.hasText(livro.getIsbn())
                            && normalizarIsbn(livro.getIsbn()).equals(isbn))
                    .findFirst().orElse(null);
            if (porIsbn != null) return porIsbn;
        }
        return livros.stream()
                .filter(livro -> normalizar(livro.getTitulo()).equals(normalizar(resultado.titulo()))
                        && normalizar(livro.getAutores()).equals(normalizar(resultado.autores())))
                .findFirst().orElse(null);
    }

    private LivroStatus converterStatus(String status) {
        try {
            return LivroStatus.valueOf(status);
        } catch (IllegalArgumentException | NullPointerException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um status de leitura válido.");
        }
    }

    private String emailAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalStateException("Não foi possível identificar o usuário autenticado.");
        }
        return authentication.getName();
    }

    private String detalhe(ResponseStatusException exception) {
        return exception.getReason() == null ? "Não foi possível concluir a operação." : exception.getReason();
    }

    private String statusLegivel(String status) {
        return switch (status) {
            case "LIDO" -> "Já li";
            case "LENDO" -> "Lendo";
            default -> "Quero ler";
        };
    }

    private String limpar(String valor) {
        return StringUtils.hasText(valor) ? valor.trim() : null;
    }

    private String normalizarIsbn(String valor) {
        return valor.replaceAll("[^0-9Xx]", "").toUpperCase(Locale.ROOT);
    }

    private String normalizar(String valor) {
        if (valor == null) return "";
        return Normalizer.normalize(valor.trim(), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.forLanguageTag("pt-BR"));
    }

    public List<Livro> getLivrosFiltrados() {
        Comparator<Livro> comparador = switch (ordenarLivros == null ? "titulo" : ordenarLivros) {
            case "autores" -> Comparator.comparing(Livro::getAutores,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "editora" -> Comparator.comparing(Livro::getEditora,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
            case "anoLancamento" -> Comparator.comparing(this::anoOrdenacao,
                    Comparator.nullsLast(Comparator.reverseOrder()));
            default -> Comparator.comparing(Livro::getTitulo,
                    Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER));
        };
        return livros.stream()
                .filter(livro -> "TODOS".equals(filtroStatus) || livro.getStatus().name().equals(filtroStatus))
                .filter(livro -> "TODOS".equals(filtroCompra)
                        || ("COMPRADO".equals(filtroCompra) && livro.isComprado())
                        || ("NAO_COMPRADO".equals(filtroCompra) && !livro.isComprado()))
                .sorted(comparador.thenComparing(Livro::getTitulo, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)))
                .toList();
    }

    public long getTotalLidos() {
        return livros.stream().filter(livro -> livro.getStatus() == LivroStatus.LIDO).count();
    }

    public long getTotalLendo() {
        return livros.stream().filter(livro -> livro.getStatus() == LivroStatus.LENDO).count();
    }

    public long getTotalDesejados() {
        return livros.stream().filter(livro -> livro.getStatus() == LivroStatus.DESEJADO).count();
    }

    public boolean isTemMaisResultados() {
        return !resultados.isEmpty() && resultados.size() >= 100 && paginaBusca < totalPaginasBusca;
    }

    public String statusLegivel(LivroStatus status) {
        return statusLegivel(status.name());
    }

    public String classeStatus(LivroStatus status) {
        return status.name().toLowerCase(Locale.ROOT);
    }

    private Integer anoOrdenacao(Livro livro) {
        if (livro.getAnoLancamento() == null) return null;
        try {
            return Integer.parseInt(livro.getAnoLancamento());
        } catch (NumberFormatException exception) {
            return null;
        }
    }

    public List<SelectItem> getStatusDisponiveis() {
        return List.of(
                new SelectItem(LivroStatus.LIDO, "Já li"),
                new SelectItem(LivroStatus.LENDO, "Lendo"),
                new SelectItem(LivroStatus.DESEJADO, "Quero ler")
        );
    }

    public String getStatusTodos() { return "TODOS"; }
    public String getStatusLido() { return "LIDO"; }
    public String getStatusLendo() { return "LENDO"; }
    public String getStatusDesejado() { return "DESEJADO"; }
    public String getCompraTodos() { return "TODOS"; }
    public String getCompraSim() { return "COMPRADO"; }
    public String getCompraNao() { return "NAO_COMPRADO"; }

    public List<Livro> getLivros() { return livros; }
    public List<LivroCatalogoResultado> getResultados() { return resultados; }
    public String getTermoBusca() { return termoBusca; }
    public void setTermoBusca(String termoBusca) { this.termoBusca = termoBusca; }
    public String getCampoBusca() { return campoBusca; }
    public void setCampoBusca(String campoBusca) { this.campoBusca = campoBusca; }
    public String getFiltroStatus() { return filtroStatus; }
    public void setFiltroStatus(String filtroStatus) { this.filtroStatus = filtroStatus; }
    public String getFiltroCompra() { return filtroCompra; }
    public void setFiltroCompra(String filtroCompra) { this.filtroCompra = filtroCompra; }
    public String getOrdenarLivros() { return ordenarLivros; }
    public void setOrdenarLivros(String ordenarLivros) { this.ordenarLivros = ordenarLivros; }
    public String getMensagemBusca() { return mensagemBusca; }
    public String getMensagemLista() { return mensagemLista; }
}
