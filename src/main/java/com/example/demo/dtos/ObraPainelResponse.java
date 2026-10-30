package com.example.demo.dtos;

import com.example.demo.models.StatusCronEnum;
import com.example.demo.models.Usuario;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class ObraPainelResponse {

    private Long id;
    private String nome;
    private String endereco;
    private Double area;
    private BigDecimal bdi;
    private LocalDate dataInicioPrevista;
    private LocalDate dataFimPrevista;
    private StatusCronEnum status;
    private List<Usuario> clientes;
    private BigDecimal custoOrcadoTotal;
    private BigDecimal custoRealizadoTotal;
    private BigDecimal desvioTotal;
    private BigDecimal precoVendaEstimado;
    private BigDecimal percentualAvanco;

}
