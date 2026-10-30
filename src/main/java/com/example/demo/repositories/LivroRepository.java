package com.example.demo.repositories;

import com.example.demo.models.Livro;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface LivroRepository extends JpaRepository<Livro, Long> {
    List<Livro> findAllByDonoUsuarioAndDataDelecaoIsNullOrderByDataCriacaoDesc(String donoUsuario);

    Optional<Livro> findByIdAndDonoUsuarioAndDataDelecaoIsNull(Long id, String donoUsuario);

    boolean existsByDonoUsuarioAndIsbnIgnoreCaseAndDataDelecaoIsNull(String donoUsuario, String isbn);
}
