package com.example.demo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Mensagem extends AbstractModel implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Conversa conversa;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario remetente;

    @Column(nullable = false, length = 2000)
    private String conteudo;

    @Column(nullable = false)
    private LocalDateTime enviadaEm;

    @PrePersist
    void definirDataEnvio() {
        if (enviadaEm == null) {
            enviadaEm = LocalDateTime.now();
        }
    }
}
