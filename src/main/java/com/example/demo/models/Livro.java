package com.example.demo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@NoArgsConstructor
@Getter
@Setter
public class Livro extends AbstractModel {

    @Column(nullable = false, length = 300)
    private String titulo;

    @Column(length = 500)
    private String autores;

    @Column(length = 30)
    private String isbn;

    @Column(length = 200)
    private String editora;

    @Column(length = 20)
    private String anoLancamento;

    @Column(nullable = false, length = 150)
    private String donoUsuario;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private LivroStatus status;

    @Column(nullable = true)
    private boolean comprado;
}
