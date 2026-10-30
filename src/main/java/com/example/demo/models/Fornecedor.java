package com.example.demo.models;

import jakarta.persistence.Entity;
import jakarta.persistence.OneToMany;
import jakarta.persistence.ManyToMany;
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
public class Fornecedor extends AbstractModel implements Serializable {

    private String razaoSocial;
    private String cnpj;
    private String telefone;
    private String email;

    @OneToMany(mappedBy = "fornecedor")
    @JsonIgnore
    private List<LancamentoCusto> lancamentosCustos;

    @OneToMany(mappedBy = "fornecedor")
    @JsonIgnore
    private List<Insumo> insumos;

    public String toString() {
        return "Fornecedor{" +
                ", razaoSocial='" + razaoSocial + '\'' +
                ", cnpj='" + cnpj + '\'' +
                ", telefone='" + telefone + '\'' +
                ", email='" + email + '\'' +
                ", lancamentosCustos='" + lancamentosCustos + '\'' +
                '}';
    }

}
