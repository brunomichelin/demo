package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class ItemOrcamento extends AbstractModel implements Serializable {

    @ManyToOne
    @JsonIgnore
    private Orcamento orcamento;

    @ManyToOne
    private Etapa etapa;

    @ManyToOne(optional = true)
    private Tarefa tarefa;

    @ManyToOne
    private Insumo insumo;

    @jakarta.persistence.Column(precision = 14, scale = 3)
    private BigDecimal quantidade;

    @jakarta.persistence.Column(precision = 14, scale = 2)
    private BigDecimal valorUnitario;

    @Override
    public String toString() {
        return "ItemOrcamento{" +
                "orcamento=" + orcamento +
                ", etapa=" + etapa +
                ", tarefa=" + tarefa +
                ", insumo=" + insumo +
                ", quantidade=" + quantidade +
                ", valorUnitario=" + valorUnitario +
                '}';
    }

}
