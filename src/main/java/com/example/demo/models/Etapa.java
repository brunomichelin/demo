package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDate;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@Entity
public class Etapa extends AbstractModel implements Serializable {

    @ManyToOne
    private Obra obra;

    private Long ordem;
    private String nome;
    private String descricao;
    private LocalDate dataInicioPrevista;
    private LocalDate dataEntregaPrevista;
    private StatusCronEnum status;

    @OneToMany(mappedBy = "etapa")
    @JsonIgnore
    private List<Tarefa> tarefas;

    @OneToMany(mappedBy = "etapa")
    @JsonIgnore
    private List<ItemOrcamento> itensOrcamento;

    @OneToMany(mappedBy = "etapa")
    @JsonIgnore
    private List<LancamentoCusto> lancamentosCustos;

    @OneToOne(mappedBy = "etapa")
    @JsonIgnore
    private Entrega entrega;

    public String toString() {
        return "Etapa{" +
                ", ordem='" + ordem + '\'' +
                ", nome='" + nome + '\'' +
                ", descricao='" + descricao + '\'' +
                ", dataInicioPrevista='" + dataInicioPrevista + '\'' +
                ", dataEntregaPrevista='" + dataEntregaPrevista + '\'' +
                ", status='" + status + '\'' +
                ", tarefas='" + tarefas + '\'' +
                ", itensOrcamento='" + itensOrcamento + '\'' +
                ", lancamentosCustos='" + lancamentosCustos + '\'' +
                ", entrega='" + entrega + '\'' +
                '}';
    }

}
