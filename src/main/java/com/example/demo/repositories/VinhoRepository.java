package com.example.demo.repositories;

import com.example.demo.models.Vinho;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VinhoRepository extends JpaRepository<Vinho, Long> {
    List<Vinho> findAllByUsuarioEmailIgnoreCaseAndDataDelecaoIsNullOrderByDataCriacaoDesc(String email);

    Optional<Vinho> findByIdAndUsuarioEmailIgnoreCaseAndDataDelecaoIsNull(Long id, String email);
}
