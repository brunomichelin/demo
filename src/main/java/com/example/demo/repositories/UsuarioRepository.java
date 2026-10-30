package com.example.demo.repositories;

import com.example.demo.models.Usuario;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.List;

public interface UsuarioRepository extends JpaRepository<Usuario, Long> {
    boolean existsByEmailIgnoreCase(String email);

    @Query("select u from Usuario u where lower(u.email) = lower(:email) and u.dataDelecao is null")
    Optional<Usuario> findByEmailIgnoreCase(@Param("email") String email);

    @Override
    @Query("select u from Usuario u where u.dataDelecao is null")
    Page<Usuario> findAll(Pageable pageable);

    @Override
    @Query("select u from Usuario u where u.dataDelecao is null")
    List<Usuario> findAll();

    @Override
    @Query("select u from Usuario u where u.dataDelecao is null")
    List<Usuario> findAll(Sort sort);

    @Override
    @Query("select u from Usuario u where u.id = :id and u.dataDelecao is null")
    Optional<Usuario> findById(@Param("id") Long id);

}
