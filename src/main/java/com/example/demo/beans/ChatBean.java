package com.example.demo.beans;

import com.example.demo.dtos.ConversaResponse;
import com.example.demo.dtos.MensagemResponse;
import com.example.demo.dtos.UsuarioChatResponse;
import com.example.demo.services.impl.ChatService;
import jakarta.annotation.PostConstruct;
import jakarta.faces.application.FacesMessage;
import jakarta.faces.context.FacesContext;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Objects;

@Component("chatBean")
@Scope(value = "view", proxyMode = ScopedProxyMode.TARGET_CLASS)
public class ChatBean {

    private final ChatService chatService;
    private List<ConversaResponse> conversas = List.of();
    private List<UsuarioChatResponse> usuarios = List.of();
    private List<MensagemResponse> mensagens = List.of();
    private ConversaResponse conversaSelecionada;
    private Long usuarioSelecionadoId;
    private String novaMensagem;

    public ChatBean(ChatService chatService) {
        this.chatService = chatService;
    }

    @PostConstruct
    public void iniciar() {
        usuarios = chatService.listarUsuarios(emailAtual());
        carregarConversas();
    }

    public void abrirConversa() {
        try {
            conversaSelecionada = chatService.abrirConversa(usuarioSelecionadoId, emailAtual());
            usuarioSelecionadoId = null;
            carregarConversas();
            carregarMensagens();
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível iniciar a conversa.",
                    detalhe(exception));
        }
    }

    public void selecionarConversa(Long conversaId) {
        conversaSelecionada = conversas.stream()
                .filter(conversa -> Objects.equals(conversa.id(), conversaId))
                .findFirst()
                .orElse(null);
        if (conversaSelecionada == null) {
            mensagens = List.of();
            return;
        }
        novaMensagem = null;
        try {
            carregarMensagens();
        } catch (ResponseStatusException exception) {
            mensagens = List.of();
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível carregar a conversa.",
                    detalhe(exception));
        }
    }

    public void enviarMensagem() {
        try {
            if (conversaSelecionada == null) {
                adicionarMensagem(FacesMessage.SEVERITY_WARN, "Selecione uma conversa.",
                        "Escolha uma conversa antes de enviar uma mensagem.");
                return;
            }
            chatService.enviarMensagem(conversaSelecionada.id(), novaMensagem, emailAtual());
            novaMensagem = null;
            carregarConversas();
            carregarMensagens();
        } catch (ResponseStatusException exception) {
            adicionarMensagem(FacesMessage.SEVERITY_ERROR, "Não foi possível enviar a mensagem.",
                    detalhe(exception));
        }
    }

    public void atualizar() {
        carregarConversas();
        if (conversaSelecionada != null) {
            conversaSelecionada = conversas.stream()
                    .filter(conversa -> Objects.equals(conversa.id(), conversaSelecionada.id()))
                    .findFirst()
                    .orElse(null);
            if (conversaSelecionada != null) {
                carregarMensagens();
            } else {
                mensagens = List.of();
            }
        }
    }

    private void carregarConversas() {
        conversas = chatService.listarConversas(emailAtual());
    }

    private void carregarMensagens() {
        mensagens = chatService.listarMensagens(conversaSelecionada.id(), emailAtual());
    }

    private String emailAtual() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()
                || authentication.getName() == null || authentication.getName().isBlank()) {
            throw new IllegalStateException("Não foi possível identificar o usuário autenticado.");
        }
        return authentication.getName();
    }

    private String detalhe(ResponseStatusException exception) {
        return exception.getReason() == null ? "Tente novamente." : exception.getReason();
    }

    private void adicionarMensagem(FacesMessage.Severity severidade, String resumo, String detalhe) {
        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(severidade, resumo, detalhe));
    }

    public List<ConversaResponse> getConversas() {
        return conversas;
    }

    public List<UsuarioChatResponse> getUsuarios() {
        return usuarios;
    }

    public List<MensagemResponse> getMensagens() {
        return mensagens;
    }

    public ConversaResponse getConversaSelecionada() {
        return conversaSelecionada;
    }

    public Long getUsuarioSelecionadoId() {
        return usuarioSelecionadoId;
    }

    public void setUsuarioSelecionadoId(Long usuarioSelecionadoId) {
        this.usuarioSelecionadoId = usuarioSelecionadoId;
    }

    public String getNovaMensagem() {
        return novaMensagem;
    }

    public void setNovaMensagem(String novaMensagem) {
        this.novaMensagem = novaMensagem;
    }
}
