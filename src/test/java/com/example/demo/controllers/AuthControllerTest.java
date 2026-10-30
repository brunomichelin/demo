package com.example.demo.controllers;

import com.example.demo.dtos.CadastroRequest;
import com.example.demo.models.CargoEnum;
import com.example.demo.models.StatusRegEnum;
import com.example.demo.models.Usuario;
import com.example.demo.repositories.UsuarioRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthControllerTest {
    private UsuarioRepository usuarioRepository;
    private AuthController controller;

    @BeforeEach
    void setUp() {
        usuarioRepository = mock(UsuarioRepository.class);
        PasswordEncoder passwordEncoder = mock(PasswordEncoder.class);
        when(passwordEncoder.encode("senha-segura")).thenReturn("hash");
        controller = new AuthController();
        ReflectionTestUtils.setField(controller, "usuarioRepository", usuarioRepository);
        ReflectionTestUtils.setField(controller, "passwordEncoder", passwordEncoder);
    }

    @Test
    void cadastroMantemPapelSolicitadoPendenteDeAprovacao() {
        CadastroRequest request = new CadastroRequest();
        request.setNome("Pessoa Teste");
        request.setEmail("pessoa@example.com");
        request.setSenha("senha-segura");
        request.setPapel(CargoEnum.ORCAMENTISTA);
        when(usuarioRepository.existsByEmailIgnoreCase("pessoa@example.com")).thenReturn(false);

        controller.register(request);

        ArgumentCaptor<Usuario> captor = ArgumentCaptor.forClass(Usuario.class);
        verify(usuarioRepository).save(captor.capture());
        assertEquals(CargoEnum.ORCAMENTISTA, captor.getValue().getPapel());
        assertEquals(StatusRegEnum.PENDENTE, captor.getValue().getStatusRegistro());
    }
}
