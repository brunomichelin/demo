package com.example.demo.models;

import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.MappedSuperclass;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@MappedSuperclass
public abstract class AbstractModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private LocalDateTime dataCriacao;
    private LocalDateTime dataAtualizacao;
    private String criadoPor;
    private LocalDateTime dataDelecao;
    private StatusRegEnum statusRegistro;

}
