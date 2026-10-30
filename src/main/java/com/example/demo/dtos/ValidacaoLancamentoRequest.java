package com.example.demo.dtos;

import com.example.demo.models.StatusLancamentoEnum;

public record ValidacaoLancamentoRequest(StatusLancamentoEnum status, String justificativa) {
}
