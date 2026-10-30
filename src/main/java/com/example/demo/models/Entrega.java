package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
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
public class Entrega extends AbstractModel implements Serializable {

    @OneToOne
    @JsonIgnore
    private Etapa etapa;

    private LocalDate dataReal;

    @ManyToOne
    private Usuario responsavel;
    private String observacoes;
    private StatusEntregaEnum status;

    @ManyToOne
    private Usuario aprovadoPor;

    private java.time.LocalDateTime dataAprovacao;

    @Override
    public String toString() {
        return "Entrega{" +
                "etapa=" + etapa +
                ", dataReal=" + dataReal +
                ", responsavel=" + responsavel +
                ", observacoes='" + observacoes + '\'' +
                ", status=" + status +
                '}';
    }

}
