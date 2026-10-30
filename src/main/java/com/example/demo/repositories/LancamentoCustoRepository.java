package com.example.demo.repositories;

import com.example.demo.models.LancamentoCusto;
import com.example.demo.models.StatusLancamentoEnum;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;

public interface LancamentoCustoRepository extends JpaRepository<LancamentoCusto, Long> {
    List<LancamentoCusto> findByEtapaId(Long etapaId);
    Page<LancamentoCusto> findByEtapaId(Long etapaId, Pageable pageable);
    List<LancamentoCusto> findByEtapaIdAndStatus(Long etapaId, StatusLancamentoEnum status);
    @Query("select l from LancamentoCusto l where l.etapa.obra.id = :obraId")
    List<LancamentoCusto> findAllByObraId(@Param("obraId") Long obraId);
}
