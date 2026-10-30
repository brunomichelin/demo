package com.example.demo.repositories;

import com.example.demo.models.Insumo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface InsumoRepository extends JpaRepository<Insumo, Long> {
    boolean existsByCodigoIgnoreCase(String codigo);

    @Override
    @Query("select i from Insumo i where i.dataDelecao is null")
    Page<Insumo> findAll(Pageable pageable);

    @Override
    @Query("select i from Insumo i where i.id = :id and i.dataDelecao is null")
    Optional<Insumo> findById(@Param("id") Long id);
}
