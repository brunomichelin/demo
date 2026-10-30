package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Tarefa extends AbstractModel implements Serializable {

    @ManyToOne
    private Etapa etapa;

    private String titulo;
    private String descricao;

    @ManyToOne
    private Usuario responsavel;

    @ManyToOne
    @JsonIgnore
    private Tarefa predecessora;

    private java.time.LocalDateTime statusAtualizadoEm;
    private String statusAtualizadoPor;

    private LocalDate dataInicioPrevista;
    private LocalDate dataFimPrevista;

    private Long prioridade;
    private StatusCronEnum status;

    public String toString() {
        return "Tarefa{" +
                ", titulo='" + titulo + '\'' +
                ", descricao='" + descricao + '\'' +
                ", responsavel='" + responsavel + '\'' +
                ", dataInicioPrevista='" + dataInicioPrevista + '\'' +
                ", dataFimPrevista='" + dataFimPrevista + '\'' +
                ", prioridade='" + prioridade + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

}
