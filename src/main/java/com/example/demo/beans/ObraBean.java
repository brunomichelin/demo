package com.example.demo.beans;

import com.example.demo.models.Obra;
import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.StatusRegEnum;
import java.math.BigDecimal;
import com.example.demo.models.Tarefa;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.ObraRepository;
import com.example.demo.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import lombok.Getter;
import lombok.Setter;
import org.primefaces.event.data.SortEvent;
import org.primefaces.model.SortMeta;
import org.primefaces.model.SortOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component("obraBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ObraBean {

    private static final Map<String, String> SORTABLE_FIELDS = Map.of(
            "nome", "nome",
            "endereco", "endereco",
            "area", "area",
            "dataInicioPrevista", "dataInicioPrevista",
            "dataFimPrevista", "dataFimPrevista",
            "status", "status"
    );

    @Getter
    private List<Obra> obras = new ArrayList<>();

    private Sort sort = Sort.by(Sort.Direction.ASC, "nome");

    @Getter
    private List<Usuario> usuarios = new ArrayList<>();

    @Getter
    private final StatusCronEnum[] statusDisponiveis = {
            StatusCronEnum.PLANEJAMENTO, StatusCronEnum.EM_ANDAMENTO,
            StatusCronEnum.PAUSADA, StatusCronEnum.CONCLUIDA, StatusCronEnum.CANCELADA
    };

    @Getter
    @Setter
    private Obra obra;

    @Getter
    @Setter
    private List<Long> clientesSelecionados = new ArrayList<>();

    @Getter
    private boolean visualizacao;

    @Autowired
    private ObraRepository obraRepository;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostConstruct
    public void init() {
        listarTodas();
        usuarios = usuarioRepository.findAll();
        novaObra();
    }

    public void listarTodas() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        Long usuarioId = null;
        if (authentication != null && authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            usuarioId = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                    .map(Usuario::getId)
                    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
        }
        obras = new ArrayList<>(obraRepository.findFiltered(null, null, null, null, usuarioId,
                Pageable.unpaged(sort)).getContent());
    }

    public void ordenar(SortEvent event) {
        SortMeta sortMeta = event.getSortBy().values().stream()
                .filter(meta -> !meta.getOrder().isUnsorted())
                .findFirst()
                .orElse(null);

        if (sortMeta == null) {
            return;
        }

        String field = SORTABLE_FIELDS.get(sortMeta.getField());
        if (field == null) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível ordenar essa coluna.");
            return;
        }

        Sort.Direction direction = sortMeta.getOrder() == SortOrder.DESCENDING
                ? Sort.Direction.DESC
                : Sort.Direction.ASC;
        sort = Sort.by(direction, field);
        listarTodas();
    }

    public int getQuantidadeObras() {
        return obras.size();
    }

    public void novaObra() {
        obra = new Obra();
        obra.setStatus(StatusCronEnum.PLANEJAMENTO);
        obra.setBdi(BigDecimal.ZERO);
        clientesSelecionados = new ArrayList<>();
        visualizacao = false;
    }

    public void editarObra(Obra obraSelecionada) {
        prepararEdicao(obraSelecionada, false);
    }

    public void visualizarObra(Obra obraSelecionada) {
        prepararEdicao(obraSelecionada, true);
    }

    private void prepararEdicao(Obra obraSelecionada, boolean somenteLeitura) {
        obra = obraRepository.findWithClientesById(obraSelecionada.getId())
                .orElseThrow(() -> new IllegalArgumentException(
                        "Obra não encontrada: " + obraSelecionada.getId()));
        clientesSelecionados = obra.getClientes() == null
                ? new ArrayList<>()
                : obra.getClientes().stream().map(Usuario::getId).toList();
        visualizacao = somenteLeitura;
    }

    public void salvar() {
        boolean nova = obra.getId() == null;
        if (clientesSelecionados == null || clientesSelecionados.isEmpty()) {
            obra.setClientes(new ArrayList<>());
        } else {
            obra.setClientes(new ArrayList<>(usuarioRepository.findAllById(clientesSelecionados)));
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())
                && authentication.getAuthorities().stream()
                .noneMatch(authority -> authority.getAuthority().equals("ROLE_ADMIN"))) {
            Usuario usuarioAtual = usuarioRepository.findByEmailIgnoreCase(authentication.getName())
                    .orElseThrow(() -> new IllegalStateException("Usuário autenticado não encontrado."));
            List<Usuario> vinculados = obra.getUsuariosVinculados() == null
                    ? new ArrayList<>() : new ArrayList<>(obra.getUsuariosVinculados());
            if (vinculados.stream().noneMatch(usuario -> usuario.getId().equals(usuarioAtual.getId()))) {
                vinculados.add(usuarioAtual);
            }
            obra.setUsuariosVinculados(vinculados);
        }

        LocalDateTime agora = LocalDateTime.now();
        if (nova) {
            obra.setDataCriacao(agora);
            if (authentication != null && authentication.isAuthenticated()
                    && !"anonymousUser".equals(authentication.getName())) {
                obra.setCriadoPor(authentication.getName());
            }
        } else {
            obra.setDataAtualizacao(agora);
        }

        obraRepository.save(obra);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO,
                nova ? "Obra cadastrada com sucesso." : "Obra atualizada com sucesso.");
    }

    public void excluir(Obra obraSelecionada) {
        obraSelecionada.setDataDelecao(LocalDateTime.now());
        obraSelecionada.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        obraRepository.save(obraSelecionada);
        listarTodas();
        adicionarMensagem(FacesMessage.SEVERITY_INFO, "Obra excluída com sucesso.");
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String texto) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

}
