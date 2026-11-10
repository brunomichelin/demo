package com.example.demo.repositories;

import com.example.demo.models.Entrega;
import com.example.demo.dtos.EntregaListaResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    @Query("select e from Entrega e where e.etapa.id = :etapaId and e.dataDelecao is null")
    Optional<Entrega> findByEtapaId(Long etapaId);

    @Query("select e from Entrega e where e.etapa.obra.id = :obraId "
            + "and e.dataDelecao is null and e.etapa.dataDelecao is null order by e.etapa.ordem")
    List<Entrega> findAllByObraId(@Param("obraId") Long obraId);

    @Query("""
            select new com.example.demo.dtos.EntregaListaResponse(
                e.id, et.id, et.ordem, et.nome, e.dataReal, responsavel.nome, e.observacoes, e.status)
            from Entrega e
            join e.etapa et
            left join e.responsavel responsavel
            where et.obra.id = :obraId and et.dataDelecao is null
              and et.obra.dataDelecao is null and e.dataDelecao is null
            order by et.ordem
            """)
    List<EntregaListaResponse> listarResumosPorObra(@Param("obraId") Long obraId);
}
