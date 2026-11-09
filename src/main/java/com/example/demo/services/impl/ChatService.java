package com.example.demo.services.impl;

import com.example.demo.dtos.ConversaResponse;
import com.example.demo.dtos.MensagemResponse;
import com.example.demo.dtos.UsuarioChatResponse;
import com.example.demo.models.Conversa;
import com.example.demo.models.Mensagem;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.ConversaRepository;
import com.example.demo.repositories.MensagemRepository;
import com.example.demo.repositories.UsuarioRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
public class ChatService {

    private final ConversaRepository conversaRepository;
    private final MensagemRepository mensagemRepository;
    private final UsuarioRepository usuarioRepository;

    public ChatService(
            ConversaRepository conversaRepository,
            MensagemRepository mensagemRepository,
            UsuarioRepository usuarioRepository
    ) {
        this.conversaRepository = conversaRepository;
        this.mensagemRepository = mensagemRepository;
        this.usuarioRepository = usuarioRepository;
    }

    @Transactional(readOnly = true)
    public List<UsuarioChatResponse> listarUsuarios(String email) {
        return usuarioRepository.findUsuariosAtivosParaChat(email, StatusRegEnum.ATIVO).stream()
                .map(usuario -> new UsuarioChatResponse(usuario.getId(), usuario.getNome(), usuario.getEmail()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversaResponse> listarConversas(String email) {
        return conversaRepository.findAllVisiveisPara(email).stream()
                .map(conversa -> paraResposta(conversa, email))
                .toList();
    }

    @Transactional
    public ConversaResponse abrirConversa(Long usuarioId, String email) {
        if (usuarioId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Selecione um usuário.");
        }
        Usuario atual = buscarUsuarioAtivo(email);
        Usuario outro = usuarioRepository.findById(usuarioId)
                .filter(usuario -> usuario.getStatusRegistro() == StatusRegEnum.ATIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Usuário não encontrado."));
        if (atual.getId().equals(outro.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Não é possível iniciar uma conversa consigo mesmo.");
        }

        Conversa conversa = conversaRepository.findBetweenUsuarios(atual.getId(), outro.getId())
                .orElseGet(() -> {
                    Conversa nova = new Conversa();
                    nova.setUsuarioUm(atual);
                    nova.setUsuarioDois(outro);
                    nova.setDataCriacao(LocalDateTime.now());
                    nova.setCriadoPor(atual.getEmail());
                    nova.setStatusRegistro(StatusRegEnum.ATIVO);
                    return conversaRepository.save(nova);
                });
        return paraResposta(conversa, email);
    }

    @Transactional(readOnly = true)
    public List<MensagemResponse> listarMensagens(Long conversaId, String email) {
        Conversa conversa = buscarConversaVisivel(conversaId, email);
        return mensagemRepository.findByConversa_IdAndDataDelecaoIsNullOrderByEnviadaEmAscIdAsc(conversa.getId())
                .stream()
                .map(mensagem -> new MensagemResponse(
                        mensagem.getId(),
                        mensagem.getRemetente().getNome(),
                        mensagem.getConteudo(),
                        mensagem.getEnviadaEm(),
                        mensagem.getRemetente().getEmail().equalsIgnoreCase(email)
                ))
                .toList();
    }

    @Transactional
    public MensagemResponse enviarMensagem(Long conversaId, String conteudo, String email) {
        if (conteudo == null || conteudo.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem não pode ficar vazia.");
        }
        String texto = conteudo.trim();
        if (texto.length() > 2000) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "A mensagem deve ter no máximo 2000 caracteres.");
        }

        Conversa conversa = buscarConversaVisivel(conversaId, email);
        Usuario remetente = buscarUsuarioAtivo(email);
        LocalDateTime agora = LocalDateTime.now();
        Mensagem mensagem = new Mensagem();
        mensagem.setConversa(conversa);
        mensagem.setRemetente(remetente);
        mensagem.setConteudo(texto);
        mensagem.setEnviadaEm(agora);
        mensagem.setDataCriacao(agora);
        mensagem.setCriadoPor(remetente.getEmail());
        mensagem.setStatusRegistro(StatusRegEnum.ATIVO);
        mensagemRepository.save(mensagem);

        conversa.setUltimaMensagem(texto);
        conversa.setUltimaMensagemEm(agora);
        conversa.setDataAtualizacao(agora);
        return new MensagemResponse(mensagem.getId(), remetente.getNome(), texto, agora, true);
    }

    private Usuario buscarUsuarioAtivo(String email) {
        return usuarioRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .filter(usuario -> usuario.getStatusRegistro() == StatusRegEnum.ATIVO)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Usuário não está ativo."));
    }

    private Conversa buscarConversaVisivel(Long conversaId, String email) {
        return conversaRepository.findVisivelPara(conversaId, email)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Conversa não encontrada."));
    }

    private ConversaResponse paraResposta(Conversa conversa, String email) {
        Usuario outro = conversa.getUsuarioUm().getEmail().equalsIgnoreCase(email)
                ? conversa.getUsuarioDois()
                : conversa.getUsuarioUm();
        return new ConversaResponse(
                conversa.getId(),
                outro.getId(),
                outro.getNome(),
                outro.getEmail(),
                conversa.getUltimaMensagem(),
                conversa.getUltimaMensagemEm()
        );
    }
}
