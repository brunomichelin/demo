package com.example.demo.dtos;

import java.time.LocalDateTime;

public record ConversaResponse(
        Long id,
        Long outroUsuarioId,
        String outroUsuarioNome,
        String outroUsuarioEmail,
        String ultimaMensagem,
        LocalDateTime ultimaMensagemEm
) {
}
