package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.FetchType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class DocObra extends AbstractModel implements Serializable {

    private String nome;
    private String nomeArquivo;
    @Lob
    private byte[] arquivo;
    private String tipo;
    private Long tamanho;

    @ManyToOne(fetch = FetchType.LAZY)
    private Obra obra;

    @Override
    public String toString() {
        return "DocObra{" +
                "nome='" + nome + '\'' +
                ", nomeArquivo='" + nomeArquivo + '\'' +
                ", tipo='" + tipo + '\'' +
                ", tamanho=" + tamanho +
                '}';
    }

}
