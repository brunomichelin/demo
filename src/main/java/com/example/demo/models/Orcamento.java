package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.List;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Entity
public class Orcamento extends AbstractModel implements Serializable {

    @ManyToOne
    private Obra obra;

    private Long numero;
    private Boolean congelada;

    @OneToMany(mappedBy = "orcamento")
    @JsonIgnore
    private List<ItemOrcamento> itens;

    @Override
    public String toString() {
        return "Orcamento{" +
                "obra=" + obra +
                ", numero=" + numero +
                ", congelada=" + congelada +
                ", itens=" + itens +
                '}';
    }

}
