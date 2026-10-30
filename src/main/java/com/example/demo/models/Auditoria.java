package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class Auditoria extends AbstractModel implements Serializable {

    private String entidade;
    private Long entidadeId;
    private String acao;
    private String valorAnterior;
    private String valorNovo;

    @ManyToOne(optional = true)
    private Usuario usuario;

    private LocalDateTime dataHora;

    @Override
    public String toString() {
        return "Auditoria{" +
                "entidade='" + entidade + '\'' +
                ", acao='" + acao + '\'' +
                ", valorAnterior='" + valorAnterior + '\'' +
                ", valorNovo='" + valorNovo + '\'' +
                ", usuario=" + usuario +
                ", dataHora=" + dataHora +
                '}';
    }

}
