package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Insumo extends AbstractModel implements Serializable {

    private String codigo;
    private String descricao;
    private String tipo;
    private MedidaEnum unidade;
    @jakarta.persistence.Column(precision = 14, scale = 2)
    private BigDecimal precoReferencia;

    @ManyToOne
    private Fornecedor fornecedor;

    @OneToMany(mappedBy = "insumo")
    @JsonIgnore
    private List<ItemOrcamento> itensOrcamento;

    @OneToMany(mappedBy = "insumo")
    @JsonIgnore
    private List<LancamentoCusto> lancamentosCustos;

    public String toString() {
        return "Insumo{" +
                ", descricao='" + descricao + '\'' +
                ", tipo='" + tipo + '\'' +
                ", unidade='" + unidade + '\'' +
                ", precoReferencia='" + precoReferencia + '\'' +
                ", itensOrcamento='" + itensOrcamento + '\'' +
                ", lancamentosCustos='" + lancamentosCustos + '\'' +
                '}';
    }

}
