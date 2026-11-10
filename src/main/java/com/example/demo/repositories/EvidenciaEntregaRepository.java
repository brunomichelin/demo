package com.example.demo.repositories;

import com.example.demo.dtos.EvidenciaEntregaResponse;
import com.example.demo.models.EvidenciaEntrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EvidenciaEntregaRepository extends JpaRepository<EvidenciaEntrega, Long> {

    @Modifying
    @Query("delete from EvidenciaEntrega e where e.entrega.id = :entregaId")
    int excluirPorEntregaId(@Param("entregaId") Long entregaId);

    @Query("""
            select new com.example.demo.dtos.EvidenciaEntregaResponse(
                e.id, e.nomeArquivo, e.tipo, e.tamanho)
            from EvidenciaEntrega e
            where e.entrega.id = :entregaId and e.dataDelecao is null
            order by e.id
            """)
    List<EvidenciaEntregaResponse> listarPorEntrega(@Param("entregaId") Long entregaId);

    @Query("""
            select e from EvidenciaEntrega e
            where e.id = :evidenciaId and e.entrega.id = :entregaId
              and e.dataDelecao is null
            """)
    Optional<EvidenciaEntrega> buscarArquivo(
            @Param("entregaId") Long entregaId,
            @Param("evidenciaId") Long evidenciaId
    );
}
