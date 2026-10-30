package com.example.demo.models;

import jakarta.persistence.Entity;
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
public class Usuario extends AbstractModel implements Serializable {

    private String nome;
    private String email;

    @JsonIgnore
    private String senhaHash;
    private CargoEnum papel;
    private String telefone;

    @ManyToMany(mappedBy = "clientes")
    @JsonIgnore
    private List<Obra> obras;

    @ManyToMany(mappedBy = "usuariosVinculados")
    @JsonIgnore
    private List<Obra> obrasVinculadas;

    public String toString() {
        return "Usuario{" +
                ", nome='" + nome + '\'' +
                ", email='" + email + '\'' +
                ", senhaHash='" + senhaHash + '\'' +
                ", papel='" + papel + '\'' +
                ", telefone='" + telefone + '\'' +
                ", obras='" + obras + '\'' +
                '}';
    }

}
