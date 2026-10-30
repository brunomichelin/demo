package com.example.demo.repositories;

import com.example.demo.models.Orcamento;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OrcamentoRepository extends JpaRepository<Orcamento, Long> {

    List<Orcamento> findByObraIdOrderByNumeroAsc(Long obraId);
}
