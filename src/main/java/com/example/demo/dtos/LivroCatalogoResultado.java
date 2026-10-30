package com.example.demo.dtos;

public record LivroCatalogoResultado(
        String titulo,
        String autores,
        String isbn,
        String editora,
        String assunto,
        String ano
) {
}
