package com.example.demo.controllers;

import com.example.demo.dtos.ObraPainelResponse;
import com.example.demo.dtos.ObraRequest;
import com.example.demo.dtos.ObraResponse;
import com.example.demo.models.Obra;
import com.example.demo.services.impl.ObraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;
import com.example.demo.models.StatusCronEnum;

import java.time.LocalDate;

@RestController
@RequestMapping({"/obras", "/api/v1/obras"})
public class ObraController {

    @Autowired
    private ObraService obraService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ENGENHEIRO','ORCAMENTISTA','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO','CLIENTE')")
    public Page<Obra> listarObras(
            @RequestParam(required = false) StatusCronEnum status,
            @RequestParam(required = false) Long clienteId,
            @RequestParam(required = false) LocalDate inicio,
            @RequestParam(required = false) LocalDate fim,
            Pageable pageable
    ) {
        return obraService.listarObras(status, clienteId, inicio, fim, pageable);
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<ObraResponse> criarObra(@RequestBody ObraRequest obraRequest) {
        ObraResponse obraResponse = obraService.criarObra(obraRequest);
        return ResponseEntity.ok(obraResponse);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ENGENHEIRO','ORCAMENTISTA','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO','CLIENTE')")
    public ResponseEntity<ObraResponse> buscarObraPorId(@PathVariable Long id) {
        ObraResponse obraResponse = obraService.buscarObraPorId(id);
        if (obraResponse != null) {
            return ResponseEntity.ok(obraResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR')")
    public ResponseEntity<ObraResponse> atualizarObra(@PathVariable Long id, @RequestBody ObraRequest obraRequest) {
        ObraResponse obraResponse = obraService.atualizarObra(id, obraRequest);
        if (obraResponse != null) {
            return ResponseEntity.ok(obraResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deletarObra(@PathVariable Long id) {
        boolean deletado = obraService.deletarObra(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/painel")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','ENGENHEIRO','ORCAMENTISTA','MESTRE_OBRA','TECNICO_OBRA','FINANCEIRO','CLIENTE')")
    public ResponseEntity<ObraPainelResponse> obterPainelObra(@PathVariable Long id) {
        ObraPainelResponse painelResponse = obraService.obterPainelObra(id);
        if (painelResponse != null) {
            return ResponseEntity.ok(painelResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

}
