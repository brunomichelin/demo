package com.example.demo.services.impl;

import com.example.demo.models.HistoricoPrecoInsumo;
import com.example.demo.models.Insumo;
import com.example.demo.repositories.HistoricoPrecoInsumoRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class HistoricoPrecoInsumoService {
    private final HistoricoPrecoInsumoRepository historicoRepository;

    public HistoricoPrecoInsumoService(HistoricoPrecoInsumoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void registrar(Insumo insumo, BigDecimal precoAnterior, BigDecimal precoNovo) {
        if (precoAnterior != null && precoAnterior.compareTo(precoNovo) == 0) {
            return;
        }
        HistoricoPrecoInsumo historico = new HistoricoPrecoInsumo();
        historico.setInsumo(insumo);
        historico.setPrecoAnterior(precoAnterior);
        historico.setPrecoNovo(precoNovo);
        historico.setRegistradoEm(LocalDateTime.now());
        historico.setDataCriacao(historico.getRegistradoEm());
        var authentication = org.springframework.security.core.context.SecurityContextHolder.getContext()
                .getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && !"anonymousUser".equals(authentication.getName())) {
            historico.setAlteradoPor(authentication.getName());
        }
        historicoRepository.save(historico);
    }
}
