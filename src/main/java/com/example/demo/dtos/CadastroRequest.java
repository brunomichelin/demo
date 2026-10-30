package com.example.demo.dtos;

import com.example.demo.models.CargoEnum;
import lombok.Data;

@Data
public class CadastroRequest {

    private String nome;
    private String email;
    private String telefone;
    private String senha;
    private CargoEnum papel;
}
