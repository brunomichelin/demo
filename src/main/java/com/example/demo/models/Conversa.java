package com.example.demo.models;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Getter
@Setter
@NoArgsConstructor
public class Conversa extends AbstractModel implements Serializable {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuarioUm;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    private Usuario usuarioDois;

    @Column(length = 2000)
    private String ultimaMensagem;

    private LocalDateTime ultimaMensagemEm;
}
