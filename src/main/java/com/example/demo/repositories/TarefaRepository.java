package com.example.demo.repositories;

import com.example.demo.models.Tarefa;
import io.micrometer.core.annotation.Counted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TarefaRepository extends JpaRepository<Tarefa, Long> {

    @Counted(value = "tarefa_repositorio_find_all_order_by_titulo_count", description = "Contagem de chamadas para findAllOrderByTitulo")
    @Query("SELECT o FROM Tarefa o WHERE o.dataDelecao IS NULL AND o.etapa.dataDelecao IS NULL ORDER BY o.titulo ASC")
    Page<Tarefa> findAllOrderByTitulo(Pageable pageable);

    @Counted(value = "tarefa_repositorio_find_all_by_etapa_id_order_by_prioridade_count", description = "Contagem de chamadas para findAllByEtapaIdOrderByPrioridade")
    @Query("SELECT o FROM Tarefa o where o.etapa.id = :idEtapa and o.dataDelecao is null and o.etapa.dataDelecao is null ORDER BY o.prioridade ASC")
    Page<Tarefa> findAllByEtapaIdOrderByPrioridade(Long idEtapa, Pageable pageable);

    @Query("select t from Tarefa t where t.etapa.id = :etapaId and t.dataDelecao is null and t.etapa.dataDelecao is null")
    List<Tarefa> findByEtapaId(@Param("etapaId") Long etapaId);

    @Query("select t from Tarefa t where t.etapa.id = :etapaId and t.dataDelecao is null and t.etapa.dataDelecao is null")
    Page<Tarefa> findPageByEtapaId(@Param("etapaId") Long etapaId, Pageable pageable);

    @Query("select t from Tarefa t where t.etapa.obra.id = :obraId and t.dataDelecao is null and t.etapa.dataDelecao is null")
    List<Tarefa> findAllByObraId(@Param("obraId") Long obraId);

    @Query("select t from Tarefa t where t.etapa.id in :etapaIds and t.dataDelecao is null and t.etapa.dataDelecao is null")
    List<Tarefa> findAllByEtapaIdIn(@Param("etapaIds") java.util.List<Long> etapaIds, Pageable pageable);

    @Override
    @Query("select t from Tarefa t where t.dataDelecao is null and t.etapa.dataDelecao is null")
    Page<Tarefa> findAll(Pageable pageable);

    @Override
    @Query("select t from Tarefa t where t.id = :id and t.dataDelecao is null")
    java.util.Optional<Tarefa> findById(@Param("id") Long id);

}