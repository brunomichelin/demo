package com.example.demo.dtos;

import java.time.LocalDate;

public record EntregaRequest(LocalDate dataReal, String observacoes) {
}
