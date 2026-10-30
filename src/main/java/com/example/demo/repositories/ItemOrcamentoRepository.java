package com.example.demo.repositories;

import com.example.demo.models.ItemOrcamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface ItemOrcamentoRepository extends JpaRepository<ItemOrcamento, Long> {
    List<ItemOrcamento> findByOrcamentoId(Long orcamentoId);
    List<ItemOrcamento> findByEtapaId(Long etapaId);

    Page<ItemOrcamento> findByEtapaId(Long etapaId, Pageable pageable);

    @Query("select i from ItemOrcamento i where i.etapa.id = :etapaId "
            + "and (:versaoId is null or i.orcamento.id = :versaoId)")
    Page<ItemOrcamento> findByEtapaIdAndVersao(
            @Param("etapaId") Long etapaId,
            @Param("versaoId") Long versaoId,
            Pageable pageable
    );

    @Query("select i from ItemOrcamento i where i.orcamento.obra.id = :obraId")
    List<ItemOrcamento> findAllByObraId(@Param("obraId") Long obraId);
}
