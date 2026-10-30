package com.example.demo.repositories;

import java.time.LocalDateTime;

public interface DocObraResumo {

    Long getId();

    String getNome();

    String getNomeArquivo();

    String getTipo();

    Long getTamanho();

    LocalDateTime getDataCriacao();
}
