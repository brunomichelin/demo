package com.example.demo.repositories;

import com.example.demo.models.Etapa;
import com.example.demo.models.Obra;
import io.micrometer.core.annotation.Counted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Pageable;

public interface EtapaRepository extends JpaRepository<Etapa, Long> {

    @Counted(value = "etapa_repositorio_find_all_order_by_nome_count", description = "Contagem de chamadas para findAllOrderByNome")
    @Query("SELECT o FROM Etapa o WHERE o.dataDelecao IS NULL AND o.obra.dataDelecao IS NULL ORDER BY o.nome ASC")
    Page<Etapa> findAllOrderByNome(Pageable pageable);

    @Counted(value = "etapa_repositorio_find_all_by_obra_id_order_by_ordem_count", description = "Contagem de chamadas para findAllByObraIdOrderByOrdem")
    @Query("SELECT o FROM Etapa o where o.obra.id = :idObra and o.dataDelecao is null and o.obra.dataDelecao is null ORDER BY o.ordem ASC")
    Page<Etapa> findAllByObraIdOrderByOrdem(Long idObra, Pageable pageable);

    @Query("select e from Etapa e where e.obra.id = :obraId and e.dataDelecao is null and e.obra.dataDelecao is null order by e.ordem asc")
    List<Etapa> findByObraIdOrderByOrdemAsc(Long obraId);

    @Query("select e from Etapa e where e.obra.id in :obraIds and e.dataDelecao is null and e.obra.dataDelecao is null")
    List<Etapa> findAllByObraIdIn(@Param("obraIds") List<Long> obraIds, Pageable pageable);

    @Override
    @Query("select e from Etapa e where e.dataDelecao is null and e.obra.dataDelecao is null")
    Page<Etapa> findAll(Pageable pageable);

    @Override
    @Query("select e from Etapa e where e.id = :id and e.dataDelecao is null and e.obra.dataDelecao is null")
    Optional<Etapa> findById(@Param("id") Long id);

}