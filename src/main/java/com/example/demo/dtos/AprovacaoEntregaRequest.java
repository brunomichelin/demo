package com.example.demo.dtos;

import com.example.demo.models.StatusEntregaEnum;

public record AprovacaoEntregaRequest(StatusEntregaEnum status, String observacoes) {
}
