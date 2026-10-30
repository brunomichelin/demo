package com.example.demo.dtos;

import com.example.demo.models.CargoEnum;

public record UsuarioRequest(
        String nome,
        String email,
        String senha,
        String telefone,
        CargoEnum papel
) {
}
