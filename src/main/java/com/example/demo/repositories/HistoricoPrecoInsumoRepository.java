package com.example.demo.repositories;

import com.example.demo.models.HistoricoPrecoInsumo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HistoricoPrecoInsumoRepository extends JpaRepository<HistoricoPrecoInsumo, Long> {
    List<HistoricoPrecoInsumo> findByInsumoIdOrderByRegistradoEmDesc(Long insumoId);
}
