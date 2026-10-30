package com.example.demo.dtos;

import com.example.demo.models.CargoEnum;
import com.example.demo.models.StatusRegEnum;

public record UsuarioResponse(
        Long id,
        String nome,
        String email,
        String telefone,
        CargoEnum papel,
        StatusRegEnum status
) {
}
