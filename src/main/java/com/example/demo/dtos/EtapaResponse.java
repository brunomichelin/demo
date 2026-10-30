package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;

@Data
@Builder
public class EtapaResponse {

    private Long id;
    private Long ordem;
    private String nome;
    private String descricao;
    private LocalDate dataInicioPrevista;
    private LocalDate dataEntregaPrevista;
    private StatusCronEnum status = StatusCronEnum.PLANEJAMENTO;

}
