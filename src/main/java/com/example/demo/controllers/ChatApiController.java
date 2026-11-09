package com.example.demo.controllers;

import com.example.demo.dtos.ConversaResponse;
import com.example.demo.dtos.MensagemResponse;
import com.example.demo.dtos.NovaConversaRequest;
import com.example.demo.dtos.NovaMensagemRequest;
import com.example.demo.dtos.UsuarioChatResponse;
import com.example.demo.services.impl.ChatService;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.util.List;

@RestController
@RequestMapping("/api/v1/chats")
public class ChatApiController {

    private final ChatService chatService;

    public ChatApiController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/users")
    public List<UsuarioChatResponse> listarUsuarios(Principal principal) {
        return chatService.listarUsuarios(principal.getName());
    }

    @GetMapping
    public List<ConversaResponse> listarConversas(Principal principal) {
        return chatService.listarConversas(principal.getName());
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ConversaResponse abrirConversa(@RequestBody(required = false) NovaConversaRequest request, Principal principal) {
        return chatService.abrirConversa(request == null ? null : request.usuarioId(), principal.getName());
    }

    @GetMapping("/{conversaId}/messages")
    public List<MensagemResponse> listarMensagens(@PathVariable Long conversaId, Principal principal) {
        return chatService.listarMensagens(conversaId, principal.getName());
    }

    @PostMapping("/{conversaId}/messages")
    @ResponseStatus(HttpStatus.CREATED)
    public MensagemResponse enviarMensagem(
            @PathVariable Long conversaId,
            @RequestBody(required = false) NovaMensagemRequest request,
            Principal principal
    ) {
        return chatService.enviarMensagem(
                conversaId,
                request == null ? null : request.conteudo(),
                principal.getName()
        );
    }
}
