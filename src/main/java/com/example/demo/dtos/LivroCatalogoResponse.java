package com.example.demo.dtos;

import java.util.List;

public record LivroCatalogoResponse(
        long total,
        int pagina,
        int totalPaginas,
        List<LivroCatalogoResultado> resultados
) {
}
