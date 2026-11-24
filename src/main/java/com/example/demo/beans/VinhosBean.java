package com.example.demo.beans;

import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.models.Vinho;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.repositories.VinhoRepository;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.FileUploadEvent;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.primefaces.PrimeFaces;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.text.Normalizer;

@Component("vinhosBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class VinhosBean {

    private static final long TAMANHO_MAXIMO_FOTO = 2L * 1024 * 1024;
    private static final List<String> TIPOS = List.of(
            "Tinto", "Branco", "Rosé", "Espumante", "Fortificado", "Sobremesa");
    private static final List<String> CORPOS = List.of("Leve", "Médio", "Encorpado");

    @Getter
    private List<Vinho> vinhos = new ArrayList<>();

    @Getter
    private List<String> paises = new ArrayList<>();

    @Getter
    private List<String> uvas = new ArrayList<>();

    @Getter
    private List<Integer> safras = new ArrayList<>();

    @Getter
    private final List<String> tipos = TIPOS;

    @Getter
    private final List<String> corpos = CORPOS;

    @Getter
    @Setter
    private Vinho vinho;

    @Getter
    @Setter
    private String busca;

    @Getter
    @Setter
    private String filtroPais;

    @Getter
    @Setter
    private String filtroUva;

    @Getter
    @Setter
    private String filtroTipo;

    @Getter
    @Setter
    private Integer filtroSafra;

    @Getter
    @Setter
    private Integer filtroNota;

    @Getter
    @Setter
    private String filtroPreco;

    @Getter
    @Setter
    private String visualizacao = "all";

    @Getter
    private String erro;

    @Autowired
    private VinhoRepository vinhoRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostConstruct
    public void init() {
        carregar();
    }

    @Transactional(readOnly = true)
    public void carregar() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalStateException("Não foi possível identificar o usuário da adega.");
        }
        vinhos = vinhoRepository.findAllByUsuarioEmailIgnoreCaseAndDataDelecaoIsNullOrderByDataCriacaoDesc(
                authentication.getName());
        paises = opcoes(vinhos.stream().map(Vinho::getPais).toList());
        uvas = opcoes(vinhos.stream().map(Vinho::getUvas).toList());
        safras = vinhos.stream().map(Vinho::getSafra).filter(Objects::nonNull).distinct()
                .sorted(Comparator.reverseOrder()).toList();
    }

    public List<Vinho> getVinhosFiltrados() {
        String termo = normalizar(busca);
        return vinhos.stream().filter(item -> {
            if (visualizacao.equals("favorites") && !item.isFavorito()) return false;
            if (visualizacao.equals("wishlist") && !item.isQueroBeber()) return false;
            if (StringUtils.hasText(termo) && !normalizar(String.join(" ",
                    texto(item.getNome()), texto(item.getProdutor()), texto(item.getPais()),
                    texto(item.getRegiao()), texto(item.getDenominacao()), texto(item.getUvas()),
                    texto(item.getCorte()), texto(item.getTipo()), texto(item.getAparencia()),
                    texto(item.getAroma()), texto(item.getSaborFinal()), texto(item.getHarmonizacao()),
                    texto(item.getOcasiao()), texto(item.getCompanhia()), texto(item.getLocalCompra())))
                    .contains(termo)) return false;
            if (StringUtils.hasText(filtroPais) && !Objects.equals(item.getPais(), filtroPais)) return false;
            if (StringUtils.hasText(filtroUva) && (item.getUvas() == null
                    || !List.of(item.getUvas().split(",")).stream().map(String::trim).toList().contains(filtroUva))) {
                return false;
            }
            if (StringUtils.hasText(filtroTipo) && !Objects.equals(item.getTipo(), filtroTipo)) return false;
            if (filtroSafra != null && !Objects.equals(item.getSafra(), filtroSafra)) return false;
            if (filtroNota != null && (item.getNota() == null || item.getNota() < filtroNota)) return false;
            return dentroDaFaixa(item.getPrecoPago(), filtroPreco);
        }).toList();
    }

    public long getQuantidadeProvada() {
        return vinhos.stream().filter(item -> item.getDataDegustacao() != null).count();
    }

    public long getQuantidadeListaDesejos() {
        return vinhos.stream().filter(Vinho::isQueroBeber).count();
    }

    public String getNotaMedia() {
        return vinhos.stream().filter(item -> item.getDataDegustacao() != null && item.getNota() != null)
                .map(Vinho::getNota).mapToInt(Integer::intValue).average().stream()
                .mapToObj(valor -> BigDecimal.valueOf(valor).setScale(1, RoundingMode.HALF_UP)
                        .toString().replace('.', ',')).findFirst().orElse("—");
    }

    public BigDecimal getTotalInvestido() {
        return vinhos.stream().filter(item -> item.getDataDegustacao() != null)
                .map(Vinho::getPrecoPago).filter(Objects::nonNull).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public String getInsightPreferencia() {
        var avaliados = vinhos.stream().filter(item -> item.getDataDegustacao() != null && item.getNota() != null)
                .toList();
        if (avaliados.size() < 2) {
            return "Registre suas degustações e avaliações para revelar os estilos que mais combinam com você.";
        }
        return avaliados.stream().filter(item -> StringUtils.hasText(item.getUvas()))
                .collect(java.util.stream.Collectors.groupingBy(Vinho::getUvas,
                        java.util.stream.Collectors.averagingInt(Vinho::getNota)))
                .entrySet().stream().max(java.util.Map.Entry.comparingByValue())
                .map(entry -> "Você tem dado notas mais altas para " + entry.getKey() + ".")
                .orElse("Sua adega já conta com " + avaliados.size() + " degustações avaliadas.");
    }

    public List<String> getPaisesProvados() {
        return vinhos.stream().filter(item -> item.getDataDegustacao() != null)
                .map(Vinho::getPais).filter(StringUtils::hasText).distinct().sorted().toList();
    }

    public long quantidadePorPais(String pais) {
        return vinhos.stream().filter(item -> item.getDataDegustacao() != null)
                .filter(item -> Objects.equals(item.getPais(), pais)).count();
    }

    public void novoVinho() {
        vinho = new Vinho();
        vinho.setTipo(TIPOS.getFirst());
        vinho.setCorpo("Médio");
        vinho.setDecantado("Não");
        vinho.setComprariaNovamente("Talvez");
        erro = null;
    }

    public void editar(Vinho item) {
        if (item == null || vinhos.stream().noneMatch(cadastrado -> Objects.equals(cadastrado.getId(), item.getId()))) {
            mensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível localizar este vinho na sua adega.");
            return;
        }
        vinho = vinhoRepository.findByIdAndUsuarioEmailIgnoreCaseAndDataDelecaoIsNull(
                        item.getId(), usuarioAtual())
                .orElseThrow(() -> new IllegalStateException("O vinho selecionado não pertence à sua adega."));
        erro = null;
    }

    @Transactional
    public void salvar() {
        if (vinho == null || !StringUtils.hasText(vinho.getNome())) {
            erro = "Informe o nome do vinho.";
            return;
        }
        erro = validarCampos();
        if (erro != null) return;
        String email = usuarioAtual();
        boolean novo = vinho.getId() == null;
        if (novo) {
            Usuario usuario = usuarioRepository.findByEmailIgnoreCase(email)
                    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
            vinho.setUsuario(usuario);
            vinho.setDataCriacao(LocalDateTime.now());
            vinho.setCriadoPor(email);
            vinho.setStatusRegistro(StatusRegEnum.ATIVO);
        } else if (vinhos.stream().noneMatch(item -> Objects.equals(item.getId(), vinho.getId()))) {
            erro = "O vinho selecionado não pertence à sua adega.";
            return;
        } else {
            vinho.setDataAtualizacao(LocalDateTime.now());
        }
        vinho.setNome(vinho.getNome().trim());
        vinhoRepository.save(vinho);
        carregar();
        visualizacao = "all";
        erro = null;
        mensagem(FacesMessage.SEVERITY_INFO, novo ? "Vinho guardado na adega." : "Alterações salvas.");
        PrimeFaces.current().executeScript("PF('wineDialog').hide()");
    }

    @Transactional
    public void excluir(Vinho item) {
        Vinho existente = obterDaColecao(item);
        if (existente == null) return;
        existente.setDataDelecao(LocalDateTime.now());
        existente.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        vinhoRepository.save(existente);
        carregar();
        mensagem(FacesMessage.SEVERITY_INFO, "Vinho removido da adega.");
    }

    @Transactional
    public void alternarFavorito(Vinho item) {
        Vinho existente = obterDaColecao(item);
        if (existente == null) return;
        existente.setFavorito(!existente.isFavorito());
        existente.setDataAtualizacao(LocalDateTime.now());
        vinhoRepository.save(existente);
        carregar();
    }

    @Transactional
    public void alternarListaDesejos(Vinho item) {
        Vinho existente = obterDaColecao(item);
        if (existente == null) return;
        existente.setQueroBeber(!existente.isQueroBeber());
        existente.setDataAtualizacao(LocalDateTime.now());
        vinhoRepository.save(existente);
        carregar();
    }

    public void enviarFoto(FileUploadEvent event) {
        if (vinho == null || event.getFile() == null || event.getFile().getSize() == 0) {
            erro = "Selecione uma foto válida.";
            return;
        }
        if (event.getFile().getSize() > TAMANHO_MAXIMO_FOTO) {
            erro = "A imagem deve ter no máximo 2 MB.";
            return;
        }
        String tipo = event.getFile().getContentType();
        if (!List.of("image/png", "image/jpeg", "image/webp", "image/gif").contains(tipo)) {
            erro = "Escolha uma imagem PNG, JPEG, WebP ou GIF.";
            return;
        }
        vinho.setFoto("data:" + tipo + ";base64,"
                + Base64.getEncoder().encodeToString(event.getFile().getContent()));
        erro = null;
    }

    public void removerFoto() {
        if (vinho != null) vinho.setFoto(null);
    }

    public void limparFiltros() {
        busca = null;
        filtroPais = null;
        filtroUva = null;
        filtroTipo = null;
        filtroSafra = null;
        filtroNota = null;
        filtroPreco = null;
        visualizacao = "all";
    }

    public boolean provado(Vinho item) {
        return item != null && item.getDataDegustacao() != null;
    }

    public String estrelas(Integer nota) {
        int valor = nota == null ? 0 : Math.max(0, Math.min(5, nota));
        return "★".repeat(valor) + "☆".repeat(5 - valor);
    }

    private Vinho obterDaColecao(Vinho item) {
        if (item == null) {
            mensagem(FacesMessage.SEVERITY_WARN, "Selecione um vinho válido.");
            return null;
        }
        return vinhoRepository.findByIdAndUsuarioEmailIgnoreCaseAndDataDelecaoIsNull(item.getId(), usuarioAtual())
                .orElseGet(() -> {
                    mensagem(FacesMessage.SEVERITY_WARN, "O vinho não foi encontrado na sua adega.");
                    return null;
                });
    }

    private boolean dentroDaFaixa(BigDecimal preco, String faixa) {
        if (!StringUtils.hasText(faixa)) return true;
        BigDecimal valor = preco == null ? BigDecimal.ZERO : preco;
        return switch (faixa) {
            case "0-100" -> valor.signum() > 0 && valor.compareTo(BigDecimal.valueOf(100)) <= 0;
            case "100-250" -> valor.compareTo(BigDecimal.valueOf(100)) > 0
                    && valor.compareTo(BigDecimal.valueOf(250)) <= 0;
            case "250-500" -> valor.compareTo(BigDecimal.valueOf(250)) > 0
                    && valor.compareTo(BigDecimal.valueOf(500)) <= 0;
            case "500+" -> valor.compareTo(BigDecimal.valueOf(500)) > 0;
            default -> true;
        };
    }

    private String validarCampos() {
        if (maiorQue(vinho.getNome(), 100) || maiorQue(vinho.getProdutor(), 100)
                || maiorQue(vinho.getPais(), 60) || maiorQue(vinho.getRegiao(), 100)
                || maiorQue(vinho.getDenominacao(), 80) || maiorQue(vinho.getTipo(), 30)
                || maiorQue(vinho.getUvas(), 160) || maiorQue(vinho.getCorte(), 120)
                || maiorQue(vinho.getCorpo(), 30) || maiorQue(vinho.getMadeira(), 100)
                || maiorQue(vinho.getOcasiao(), 140) || maiorQue(vinho.getCompanhia(), 100)
                || maiorQue(vinho.getAparencia(), 200) || maiorQue(vinho.getAroma(), 300)
                || maiorQue(vinho.getSaborFinal(), 300) || maiorQue(vinho.getHarmonizacao(), 200)
                || maiorQue(vinho.getComprariaNovamente(), 20) || maiorQue(vinho.getLocalCompra(), 120)
                || maiorQue(vinho.getTemperaturaServico(), 40) || maiorQue(vinho.getDecantado(), 10)
                || maiorQue(vinho.getPotencialGuarda(), 80)) {
            return "Um ou mais campos excedem o tamanho permitido.";
        }
        if (vinho.getSafra() != null && (vinho.getSafra() < 1900 || vinho.getSafra() > 2100)) {
            return "Informe uma safra válida.";
        }
        if (vinho.getNota() != null && (vinho.getNota() < 1 || vinho.getNota() > 5)) {
            return "A nota deve ficar entre 1 e 5.";
        }
        if (vinho.getTeorAlcoolico() != null
                && (vinho.getTeorAlcoolico().signum() < 0
                || vinho.getTeorAlcoolico().compareTo(BigDecimal.valueOf(100)) > 0
                || !cabeDecimal(vinho.getTeorAlcoolico(), 5, 2))) {
            return "O teor alcoólico deve ficar entre 0 e 100.";
        }
        if (vinho.getPrecoPago() != null
                && (vinho.getPrecoPago().signum() < 0 || !cabeDecimal(vinho.getPrecoPago(), 10, 2))) {
            return "Informe um preço válido, com até 8 dígitos inteiros e 2 decimais.";
        }
        if (vinho.getFoto() != null && vinho.getFoto().length() > 2_800_000) {
            return "A foto deve ter no máximo 2 MB.";
        }
        return null;
    }

    private boolean maiorQue(String valor, int limite) {
        return valor != null && valor.length() > limite;
    }

    private boolean cabeDecimal(BigDecimal valor, int precisao, int escala) {
        return valor.scale() <= escala && valor.precision() - valor.scale() <= precisao - escala;
    }

    private List<String> opcoes(List<String> valores) {
        Set<String> opcoes = new LinkedHashSet<>();
        valores.stream().filter(StringUtils::hasText)
                .flatMap(valor -> List.of(valor.split(",")).stream())
                .map(String::trim).filter(StringUtils::hasText).sorted(String.CASE_INSENSITIVE_ORDER)
                .forEach(opcoes::add);
        return List.copyOf(opcoes);
    }

    private String usuarioAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !StringUtils.hasText(authentication.getName())) {
            throw new IllegalStateException("Usuário autenticado não encontrado.");
        }
        return authentication.getName();
    }

    private String normalizar(String valor) {
        return Normalizer.normalize(texto(valor), Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "").toLowerCase(Locale.forLanguageTag("pt-BR"));
    }

    private String texto(String valor) {
        return valor == null ? "" : valor;
    }

    private void mensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }
}
