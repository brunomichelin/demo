package com.example.demo.dtos;

import java.time.LocalDateTime;

public record MensagemResponse(
        Long id,
        String remetenteNome,
        String conteudo,
        LocalDateTime enviadaEm,
        boolean minhaMensagem
) {
}
