package com.example.demo.beans;

import com.example.demo.models.CargoEnum;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.UsuarioRepository;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import jakarta.faces.model.SelectItem;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

@Component("usuariosBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class UsuariosBean {
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder passwordEncoder;
    private List<Usuario> usuarios = List.of();
    private final List<SelectItem> papeis = Arrays.stream(CargoEnum.values())
            .map(cargo -> new SelectItem(cargo, cargo.getDescricao()))
            .toList();
    private Long usuarioId;
    private String nome;
    private String email;
    private String telefone;
    private String senha;
    private CargoEnum papel;
    private String mensagem;

    public UsuariosBean(UsuarioRepository usuarioRepository, PasswordEncoder passwordEncoder) {
        this.usuarioRepository = usuarioRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @PostConstruct
    public void iniciar() {
        exigirAdministrador();
        carregar();
        limpar();
    }

    @Transactional
    public void salvar() {
        if (!exigirAdministrador() || !validar()) {
            return;
        }
        String emailNormalizado = email.trim().toLowerCase(Locale.ROOT);
        Usuario usuario;
        boolean novo = usuarioId == null;
        if (novo) {
            if (usuarioRepository.existsByEmailIgnoreCase(emailNormalizado)) {
                informar(FacesMessage.SEVERITY_ERROR, "Este e-mail já está cadastrado.");
                return;
            }
            usuario = new Usuario();
            usuario.setDataCriacao(LocalDateTime.now());
            usuario.setStatusRegistro(StatusRegEnum.ATIVO);
        } else {
            usuario = obter(usuarioId);
            if (usuario == null) return;
            boolean emailEmUso = usuarioRepository.findByEmailIgnoreCase(emailNormalizado)
                    .filter(outro -> !outro.getId().equals(usuarioId))
                    .isPresent();
            if (emailEmUso) {
                informar(FacesMessage.SEVERITY_ERROR, "Este e-mail já está cadastrado.");
                return;
            }
            usuario.setDataAtualizacao(LocalDateTime.now());
        }
        usuario.setNome(nome.trim());
        usuario.setEmail(emailNormalizado);
        usuario.setTelefone(telefone);
        usuario.setPapel(papel);
        if (StringUtils.hasText(senha)) {
            usuario.setSenhaHash(passwordEncoder.encode(senha));
        }
        usuarioRepository.save(usuario);
        carregar();
        limpar();
        informar(FacesMessage.SEVERITY_INFO, novo ? "Usuário criado." : "Usuário atualizado.");
    }

    @Transactional
    public void aprovar(Usuario selecionado) {
        if (!exigirAdministrador()) return;
        Usuario usuario = obter(selecionado == null ? null : selecionado.getId());
        if (usuario == null) return;
        if (usuario.getStatusRegistro() != StatusRegEnum.PENDENTE) {
            informar(FacesMessage.SEVERITY_ERROR, "Somente cadastros pendentes podem ser aprovados.");
            return;
        }
        usuario.setStatusRegistro(StatusRegEnum.ATIVO);
        usuario.setDataAtualizacao(LocalDateTime.now());
        usuarioRepository.save(usuario);
        carregar();
        informar(FacesMessage.SEVERITY_INFO, "Cadastro aprovado.");
    }

    @Transactional
    public void excluir(Usuario selecionado) {
        if (!exigirAdministrador()) return;
        Usuario usuario = obter(selecionado == null ? null : selecionado.getId());
        if (usuario == null) return;
        usuario.setStatusRegistro(StatusRegEnum.EXCLUIDO);
        usuario.setDataDelecao(LocalDateTime.now());
        usuario.setDataAtualizacao(LocalDateTime.now());
        usuarioRepository.save(usuario);
        carregar();
        informar(FacesMessage.SEVERITY_INFO, "Usuário excluído.");
    }

    public void editar(Usuario usuario) {
        if (usuario == null || !exigirAdministrador()) return;
        usuarioId = usuario.getId();
        nome = usuario.getNome();
        email = usuario.getEmail();
        telefone = usuario.getTelefone();
        papel = usuario.getPapel();
        senha = null;
        mensagem = null;
    }

    public void limpar() {
        usuarioId = null;
        nome = null;
        email = null;
        telefone = null;
        senha = null;
        papel = CargoEnum.CLIENTE;
    }

    public String statusLegivel(StatusRegEnum status) {
        return status == null ? "-" : status.getDescricao();
    }

    public String statusCss(StatusRegEnum status) {
        return status == null ? "inativo" : status.name().toLowerCase(Locale.ROOT);
    }

    private boolean validar() {
        if (!StringUtils.hasText(nome) || !StringUtils.hasText(email)
                || !EMAIL.matcher(email.trim()).matches() || papel == null
                || (usuarioId == null && (!StringUtils.hasText(senha) || senha.length() < 8))
                || (usuarioId != null && StringUtils.hasText(senha) && senha.length() < 8)) {
            informar(FacesMessage.SEVERITY_ERROR,
                    "Informe nome, e-mail válido, papel e senha de pelo menos 8 caracteres.");
            return false;
        }
        return true;
    }

    private Usuario obter(Long id) {
        if (id == null) {
            informar(FacesMessage.SEVERITY_ERROR, "Usuário não encontrado.");
            return null;
        }
        return usuarioRepository.findById(id)
                .filter(usuario -> usuario.getStatusRegistro() != StatusRegEnum.EXCLUIDO)
                .orElseGet(() -> {
                    informar(FacesMessage.SEVERITY_ERROR, "Usuário não encontrado.");
                    return null;
                });
    }

    private void carregar() {
        usuarios = usuarioRepository.findAll(org.springframework.data.domain.Sort.by("nome"));
    }

    private boolean exigirAdministrador() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        boolean permitido = authentication != null && authentication.isAuthenticated()
                && authentication.getAuthorities().stream()
                .anyMatch(authority -> "ROLE_ADMIN".equals(authority.getAuthority()));
        if (!permitido) informar(FacesMessage.SEVERITY_ERROR, "Acesso restrito a administradores.");
        return permitido;
    }

    private void informar(FacesMessage.Severity severidade, String texto) {
        mensagem = texto;
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, texto, null));
    }

    public List<Usuario> getUsuarios() { return usuarios; }
    public List<SelectItem> getPapeis() { return papeis; }
    public Long getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Long usuarioId) { this.usuarioId = usuarioId; }
    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefone() { return telefone; }
    public void setTelefone(String telefone) { this.telefone = telefone; }
    public String getSenha() { return senha; }
    public void setSenha(String senha) { this.senha = senha; }
    public CargoEnum getPapel() { return papel; }
    public void setPapel(CargoEnum papel) { this.papel = papel; }
    public String getMensagem() { return mensagem; }

}
