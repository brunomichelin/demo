package com.example.demo.dtos;

public record LivroRequest(
        String titulo,
        String autores,
        String isbn,
        String editora,
        String anoLancamento,
        String status
) {
}
