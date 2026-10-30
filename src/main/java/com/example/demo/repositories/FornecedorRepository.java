package com.example.demo.repositories;

import com.example.demo.models.Fornecedor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FornecedorRepository extends JpaRepository<Fornecedor, Long> {
    @Override
    @Query("select f from Fornecedor f where f.dataDelecao is null")
    Page<Fornecedor> findAll(Pageable pageable);

    @Override
    @Query("select f from Fornecedor f where f.id = :id and f.dataDelecao is null")
    Optional<Fornecedor> findById(@Param("id") Long id);
}
