package com.example.demo.repositories;

import com.example.demo.models.Mensagem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MensagemRepository extends JpaRepository<Mensagem, Long> {

    List<Mensagem> findByConversa_IdAndDataDelecaoIsNullOrderByEnviadaEmAscIdAsc(Long conversaId);
}
