package com.example.demo.controllers;

import io.jsonwebtoken.JwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.server.ResponseStatusException;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDetail> tratarStatus(ResponseStatusException exception) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(
                exception.getStatusCode(),
                exception.getReason() == null ? "A solicitação não pôde ser processada." : exception.getReason());
        problema.setTitle("Erro na solicitação");
        return ResponseEntity.status(exception.getStatusCode()).body(problema);
    }

    @ExceptionHandler(JwtException.class)
    public ResponseEntity<ProblemDetail> tratarJwt(JwtException exception) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.UNAUTHORIZED,
                "Token inválido ou expirado.");
        problema.setTitle("Não autenticado");
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(problema);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDetail> tratarArgumento(IllegalArgumentException exception) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST,
                exception.getMessage() == null ? "Parâmetro inválido." : exception.getMessage());
        problema.setTitle("Parâmetro inválido");
        return ResponseEntity.badRequest().body(problema);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ProblemDetail> tratarArquivoMuitoGrande(MaxUploadSizeExceededException exception) {
        ProblemDetail problema = ProblemDetail.forStatusAndDetail(HttpStatus.PAYLOAD_TOO_LARGE,
                "O envio excede o limite permitido. Fotos podem ter até 10 MB e vídeos até 50 MB cada.");
        problema.setTitle("Arquivos muito grandes");
        return ResponseEntity.status(HttpStatus.PAYLOAD_TOO_LARGE).body(problema);
    }
}
