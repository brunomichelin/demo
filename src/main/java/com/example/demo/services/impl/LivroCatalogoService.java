package com.example.demo.services.impl;

import com.example.demo.dtos.LivroBuscaRequest;
import com.example.demo.dtos.LivroCatalogoResponse;
import com.example.demo.dtos.LivroCatalogoResultado;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_GATEWAY;
import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class LivroCatalogoService {

    private static final String CATALOG_URL =
            "https://cblservicosprod2025.azurewebsites.net/api/search/isbn";
    private static final Map<String, String> SEARCH_FIELDS = Map.of(
            "titulo", "Title",
            "autor", "Authors",
            "isbn", "RowKey",
            "editora", "Imprint"
    );

    private final RestTemplate restTemplate;

    public LivroCatalogoService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public LivroCatalogoResponse buscar(LivroBuscaRequest request) {
        if (request == null || request.q() == null || request.q().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "Informe um termo para buscar no catálogo.");
        }
        String searchField = request.campo() == null ? null : SEARCH_FIELDS.get(request.campo());
        if (searchField == null) {
            throw new ResponseStatusException(BAD_REQUEST, "Selecione título, autor, ISBN ou editora.");
        }
        int page = request.page() == null ? 1 : request.page();
        if (page < 1) {
            throw new ResponseStatusException(BAD_REQUEST, "A página da busca deve ser maior que zero.");
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(List.of(MediaType.ALL));
        String termo = request.q().trim();
        if ("isbn".equals(request.campo())) {
            termo = termo.replaceAll("[^0-9Xx]", "");
            if (termo.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "Informe um ISBN válido para a busca.");
            }
        }
        Map<String, Object> payload = Map.of(
                "q", termo,
                "page", page,
                "searchFields", searchField,
                "orderDirection", "asc",
                "selos", List.of(),
                "autores", List.of(),
                "titulares", List.of(),
                "serviceAdmin", false
        );

        try {
            ResponseEntity<JsonNode> response = restTemplate.exchange(
                    CATALOG_URL,
                    HttpMethod.POST,
                    new HttpEntity<>(payload, headers),
                    JsonNode.class
            );
            return converterResposta(response.getBody(), page);
        } catch (RestClientException exception) {
            throw new ResponseStatusException(BAD_GATEWAY,
                    "Não foi possível consultar o catálogo da CBL no momento.", exception);
        }
    }

    private LivroCatalogoResponse converterResposta(JsonNode resposta, int paginaSolicitada) {
        JsonNode resultadosNode = resposta == null ? null : resposta.path("results");
        if (resultadosNode == null || !resultadosNode.isArray()) {
            throw new ResponseStatusException(BAD_GATEWAY, "O catálogo da CBL retornou uma resposta inválida.");
        }
        List<LivroCatalogoResultado> resultados = new ArrayList<>();
        for (JsonNode resultado : resultadosNode) {
            String titulo = texto(resultado, "Title");
            if (titulo == null || titulo.isBlank()) {
                continue;
            }
            String isbn = texto(resultado, "FormattedKey");
            if (isbn == null || isbn.isBlank()) {
                isbn = texto(resultado, "RowKey");
            }
            resultados.add(new LivroCatalogoResultado(
                    titulo,
                    textoLista(resultado.path("Authors")),
                    isbn,
                    texto(resultado, "Imprint"),
                    texto(resultado, "Subject"),
                    texto(resultado, "Ano")
            ));
        }
        return new LivroCatalogoResponse(
                resposta.path("count").asLong(resultados.size()),
                resposta.path("page").asInt(paginaSolicitada),
                resposta.path("totalPages").asInt(1),
                resultados
        );
    }

    private String texto(JsonNode objeto, String campo) {
        JsonNode valor = objeto.path(campo);
        return valor.isMissingNode() || valor.isNull() ? null : valor.asText();
    }

    private String textoLista(JsonNode valor) {
        if (valor == null || valor.isNull() || valor.isMissingNode()) {
            return null;
        }
        if (valor.isArray()) {
            List<String> itens = new ArrayList<>();
            valor.forEach(item -> {
                if (!item.isNull() && !item.asText().isBlank()) {
                    itens.add(item.asText());
                }
            });
            return itens.isEmpty() ? null : String.join(", ", itens);
        }
        return valor.asText();
    }
}
