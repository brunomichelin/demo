package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Column;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
public class Obra extends AbstractModel implements Serializable {

    private String nome;
    private String endereco;
    private Double area;
    @Column(precision = 5, scale = 2)
    private BigDecimal bdi;
    private LocalDate dataInicioPrevista;
    private LocalDate dataFimPrevista;
    private StatusCronEnum status;

    @ManyToMany
    private List<Usuario> clientes;

    @ManyToMany
    @jakarta.persistence.JoinTable(
            name = "obra_usuario_vinculado",
            joinColumns = @jakarta.persistence.JoinColumn(name = "obra_id"),
            inverseJoinColumns = @jakarta.persistence.JoinColumn(name = "usuario_id")
    )
    @JsonIgnore
    private List<Usuario> usuariosVinculados;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<Etapa> etapas;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<DocObra> documentos;

    @OneToMany(mappedBy = "obra")
    @JsonIgnore
    private List<Orcamento> orcamentos;

    public String toString() {
        return "Obra{" +
                ", nome='" + nome + '\'' +
                ", endereco='" + endereco + '\'' +
                ", area='" + area + '\'' +
                ", bdi='" + bdi + '\'' +
                ", dataInicioPrevista='" + dataInicioPrevista + '\'' +
                ", dataFimPrevista='" + dataFimPrevista + '\'' +
                ", status='" + status + '\'' +
                '}';
    }

}
