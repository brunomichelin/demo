package com.example.demo.services.impl;

import com.example.demo.dtos.LivroBuscaRequest;
import com.example.demo.dtos.LivroCatalogoResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.http.HttpMethod.POST;

class LivroCatalogoServiceTest {

    @Test
    void buscaPorIsbnEConverteOsCamposDoCatalogo() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        server.expect(requestTo("https://cblservicosprod2025.azurewebsites.net/api/search/isbn"))
                .andExpect(method(POST))
                .andExpect(content().json("""
                        {
                          "q": "9788551011553",
                          "page": 1,
                          "searchFields": "RowKey",
                          "orderDirection": "asc",
                          "selos": [],
                          "autores": [],
                          "titulares": [],
                          "serviceAdmin": false
                        }
                        """))
                .andRespond(withSuccess("""
                        {
                          "count": 1,
                          "page": 1,
                          "totalPages": 1,
                          "results": [{
                            "RowKey": "9788551011553",
                            "FormattedKey": "978-85-510-1155-3",
                            "Title": "Como matar a borboleta-azul",
                            "Authors": ["Monica Baumgarten de Bolle"],
                            "Imprint": "Intrínseca",
                            "Subject": "Ciência política",
                            "Ano": "2023"
                          }]
                        }
                        """, MediaType.APPLICATION_JSON));

        LivroCatalogoResponse resposta = new LivroCatalogoService(restTemplate)
                .buscar(new LivroBuscaRequest("978-85-510-1155-3", 1, "isbn"));

        assertEquals(1, resposta.resultados().size());
        assertEquals("Como matar a borboleta-azul", resposta.resultados().get(0).titulo());
        assertEquals("Monica Baumgarten de Bolle", resposta.resultados().get(0).autores());
        assertEquals("978-85-510-1155-3", resposta.resultados().get(0).isbn());
        assertEquals("Intrínseca", resposta.resultados().get(0).editora());
        server.verify();
    }
}
