package com.example.demo.repositories;

import com.example.demo.models.Entrega;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface EntregaRepository extends JpaRepository<Entrega, Long> {
    Optional<Entrega> findByEtapaId(Long etapaId);

    @Query("select e from Entrega e where e.etapa.obra.id = :obraId and e.etapa.dataDelecao is null")
    List<Entrega> findAllByObraId(@Param("obraId") Long obraId);
}
