package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.Usuario;
import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class EtapaRequest {

    private Long ordem;
    private String nome;
    private String descricao;
    private LocalDate dataInicioPrevista;
    private LocalDate dataEntregaPrevista;
    private StatusCronEnum status = StatusCronEnum.PLANEJAMENTO;

}
