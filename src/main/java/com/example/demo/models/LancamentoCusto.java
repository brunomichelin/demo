package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class LancamentoCusto extends AbstractModel implements Serializable {

    @ManyToOne
    private Etapa etapa;

    @ManyToOne
    private Tarefa tarefa;

    @ManyToOne
    private Insumo insumo;

    @ManyToOne
    private Fornecedor fornecedor;

    @jakarta.persistence.Column(precision = 14, scale = 3)
    private BigDecimal quantidade;

    @jakarta.persistence.Column(precision = 14, scale = 2)
    private BigDecimal valorUnitario;
    private LocalDate dataLancamento;
    private String numeroDocumento;
    private String justificativa;

    @ManyToOne
    @JsonIgnore
    private DocObra documento;
    private StatusLancamentoEnum status;

    @Override
    public String toString() {
        return "LancamentoCusto{" +
                "etapa=" + etapa +
                ", tarefa=" + tarefa +
                ", insumo=" + insumo +
                ", fornecedor=" + fornecedor +
                ", quantidade=" + quantidade +
                ", valorUnitario=" + valorUnitario +
                ", dataLancamento=" + dataLancamento +
                ", documento=" + documento +
                ", status=" + status +
                '}';
    }

}
