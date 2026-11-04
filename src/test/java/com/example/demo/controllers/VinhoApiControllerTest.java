package com.example.demo.controllers;

import com.example.demo.dtos.VinhoRequest;
import com.example.demo.dtos.VinhoResponse;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.models.Vinho;
import com.example.demo.repositories.UsuarioRepository;
import com.example.demo.repositories.VinhoRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VinhoApiControllerTest {

    @Mock
    private VinhoRepository vinhoRepository;

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private VinhoApiController controller;

    @Test
    void criarVinculaVinhoAoUsuarioAutenticado() {
        Usuario usuario = new Usuario();
        usuario.setEmail("vinho@example.com");
        when(usuarioRepository.findByEmailIgnoreCase("vinho@example.com")).thenReturn(Optional.of(usuario));
        when(vinhoRepository.save(any(Vinho.class))).thenAnswer(invocation -> {
            Vinho vinho = invocation.getArgument(0);
            vinho.setId(42L);
            return vinho;
        });

        VinhoResponse response = controller.criar(
                request("Malbec de Altura"),
                new UsernamePasswordAuthenticationToken("vinho@example.com", null)
        );

        ArgumentCaptor<Vinho> captor = ArgumentCaptor.forClass(Vinho.class);
        verify(vinhoRepository).save(captor.capture());
        assertEquals(usuario, captor.getValue().getUsuario());
        assertEquals(StatusRegEnum.ATIVO, captor.getValue().getStatusRegistro());
        assertEquals("vinho@example.com", captor.getValue().getCriadoPor());
        assertEquals(42L, response.id());
        assertEquals("Malbec de Altura", response.name());
    }

    @Test
    void atualizarNaoPermiteAcessarVinhoDeOutroUsuario() {
        when(vinhoRepository.findByIdAndUsuarioEmailIgnoreCaseAndDataDelecaoIsNull(7L, "outro@example.com"))
                .thenReturn(Optional.empty());

        assertThrows(ResponseStatusException.class, () -> controller.atualizar(
                7L,
                request("Vinho alterado"),
                new UsernamePasswordAuthenticationToken("outro@example.com", null)
        ));
    }

    private VinhoRequest request(String nome) {
        return new VinhoRequest(nome, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null,
                null, null, null, null, null, null, false, false);
    }
}
