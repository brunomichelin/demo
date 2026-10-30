package com.example.demo.controllers;

import com.example.demo.dtos.EtapaRequest;
import com.example.demo.dtos.EtapaResponse;
import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import com.example.demo.services.impl.EtapaService;
import com.example.demo.services.impl.ObraService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.security.access.prepost.PreAuthorize;

import java.util.List;

@RestController
@RequestMapping({"/etapas", "/api/v1/etapas"})
public class EtapaController {

    @Autowired
    private EtapaService etapaService;

    @GetMapping("/obra/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarObra(#id, authentication)")
    public Page<Etapa> listarEtapasObra(@PathVariable Long id, Pageable pageable) {
        return etapaService.listarEtapasObra(id, pageable);
    }

    @PostMapping("/obra/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarObra(#id, authentication)")
    public ResponseEntity<Void> criarEtapasObra(@PathVariable Long id, @RequestBody List<EtapaRequest> etapaRequest) {
        etapaService.criarEtapas(id, etapaRequest);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR','MESTRE_OBRA','TECNICO_OBRA','CLIENTE') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public ResponseEntity<EtapaResponse> buscarEtapaPorId(@PathVariable Long id) {
        EtapaResponse etapa = etapaService.buscarEtapaPorId(id);
        if (etapa != null) {
            return ResponseEntity.ok(etapa);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public ResponseEntity<EtapaResponse> atualizarEtapa(@PathVariable Long id, @RequestBody EtapaRequest etapaRequest) {
        EtapaResponse etapa = etapaService.atualizarEtapa(id, etapaRequest);
        if (etapa != null) {
            return ResponseEntity.ok(etapa);
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','GESTOR') and @obraAuthorization.podeAcessarEtapa(#id, authentication)")
    public ResponseEntity<Void> deletarEtapa(@PathVariable Long id) {
        boolean deletado = etapaService.deletarEtapa(id);
        if (deletado) {
            return ResponseEntity.noContent().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /*
    @PatchMapping("/{id}/ordem")
    public ResponseEntity<OrdemEtapaResponse> reordenarEtapasObra(@RequestBody List<Etapa> novaOrdem, @PathVariable Long id) {
        OrdemEtapaResponse painelResponse = etapaService.reordenarEtapas(id, novaOrdem);
        if (painelResponse != null) {
            return ResponseEntity.ok(painelResponse);
        } else {
            return ResponseEntity.notFound().build();
        }
    }
    */

}
