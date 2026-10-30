package com.example.demo.repositories;

import com.example.demo.models.Aluno;
import io.micrometer.core.annotation.Counted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface AlunoRepositorio extends JpaRepository<Aluno, Long> {

    @Counted(value = "aluno_repositorio_find_all_order_by_matricula_count", description = "Contagem de chamadas para findAllOrderByMatricula")
    @Query("SELECT a FROM Aluno a ORDER BY a.matricula ASC")
    Page<Aluno> findAllOrderByMatricula(Pageable pageable);

}