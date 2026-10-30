package com.example.demo.repositories;

import com.example.demo.models.Auditoria;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface AuditoriaRepository extends JpaRepository<Auditoria, Long> {
    Page<Auditoria> findAllByOrderByDataHoraDesc(Pageable pageable);
}
