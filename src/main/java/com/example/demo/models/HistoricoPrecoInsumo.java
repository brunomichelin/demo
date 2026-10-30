package com.example.demo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
public class HistoricoPrecoInsumo extends AbstractModel {
    @ManyToOne(optional = false)
    private Insumo insumo;

    @Column(precision = 14, scale = 2)
    private BigDecimal precoAnterior;

    @Column(nullable = false, precision = 14, scale = 2)
    private BigDecimal precoNovo;

    @Column(nullable = false)
    private LocalDateTime registradoEm;

    private String alteradoPor;
}
