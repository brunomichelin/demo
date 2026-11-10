package com.example.demo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class EvidenciaEntrega extends AbstractModel implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Entrega entrega;

    @Column(nullable = false, length = 255)
    private String nomeArquivo;

    @Column(nullable = false, length = 80)
    private String tipo;

    @Column(nullable = false)
    private Long tamanho;

    @Column(nullable = false, columnDefinition = "bytea")
    private byte[] arquivo;
}
