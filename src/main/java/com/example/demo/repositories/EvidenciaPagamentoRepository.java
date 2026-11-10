package com.example.demo.repositories;

import com.example.demo.models.EvidenciaPagamento;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvidenciaPagamentoRepository extends JpaRepository<EvidenciaPagamento, Long> {

    @Query("""
            select new com.example.demo.dtos.EvidenciaPagamentoResponse(
                e.id, e.nomeArquivo, e.tipo, e.tamanho)
            from EvidenciaPagamento e
            where e.lancamentoCusto.id = :lancamentoId and e.dataDelecao is null
            order by e.id
            """)
    List<com.example.demo.dtos.EvidenciaPagamentoResponse> listarPorLancamento(
            @Param("lancamentoId") Long lancamentoId
    );

    @Query("""
            select e from EvidenciaPagamento e
            where e.id = :evidenciaId and e.lancamentoCusto.id = :lancamentoId
              and e.dataDelecao is null
            """)
    Optional<EvidenciaPagamento> buscarArquivo(
            @Param("lancamentoId") Long lancamentoId,
            @Param("evidenciaId") Long evidenciaId
    );
}
