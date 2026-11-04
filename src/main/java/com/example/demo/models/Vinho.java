package com.example.demo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "vinho_pessoal")
@NoArgsConstructor
@Getter
@Setter
public class Vinho extends AbstractModel {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "usuario_id", nullable = false)
    private Usuario usuario;

    @Column(nullable = false, length = 100)
    private String nome;

    @Column(length = 100)
    private String produtor;

    private Integer safra;

    @Column(length = 60)
    private String pais;

    @Column(length = 100)
    private String regiao;

    @Column(length = 80)
    private String denominacao;

    @Column(length = 30)
    private String tipo;

    @Column(length = 160)
    private String uvas;

    @Column(length = 120)
    private String corte;

    @Column(precision = 5, scale = 2)
    private BigDecimal teorAlcoolico;

    @Column(length = 30)
    private String corpo;

    @Column(length = 100)
    private String madeira;

    private LocalDate dataDegustacao;

    @Column(length = 140)
    private String ocasiao;

    @Column(length = 100)
    private String companhia;

    private Integer nota;

    @Column(length = 200)
    private String aparencia;

    @Column(length = 300)
    private String aroma;

    @Column(length = 300)
    private String saborFinal;

    @Column(length = 200)
    private String harmonizacao;

    @Column(length = 20)
    private String comprariaNovamente;

    @Column(precision = 10, scale = 2)
    private BigDecimal precoPago;

    @Column(length = 120)
    private String localCompra;

    @Column(length = 40)
    private String temperaturaServico;

    @Column(length = 10)
    private String decantado;

    @Column(length = 80)
    private String potencialGuarda;

    @Column(columnDefinition = "TEXT")
    private String foto;

    @Column(nullable = false)
    private boolean favorito;

    @Column(nullable = false)
    private boolean queroBeber;
}
