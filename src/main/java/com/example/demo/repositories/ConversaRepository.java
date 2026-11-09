package com.example.demo.repositories;

import com.example.demo.models.Conversa;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ConversaRepository extends JpaRepository<Conversa, Long> {

    @Query("""
            select c from Conversa c
            where c.dataDelecao is null
              and (lower(c.usuarioUm.email) = lower(:email) or lower(c.usuarioDois.email) = lower(:email))
            order by coalesce(c.ultimaMensagemEm, c.dataCriacao) desc
            """)
    List<Conversa> findAllVisiveisPara(@Param("email") String email);

    @Query("""
            select c from Conversa c
            where c.id = :id and c.dataDelecao is null
              and (lower(c.usuarioUm.email) = lower(:email) or lower(c.usuarioDois.email) = lower(:email))
            """)
    Optional<Conversa> findVisivelPara(@Param("id") Long id, @Param("email") String email);

    @Query("""
            select c from Conversa c
            where c.dataDelecao is null
              and ((c.usuarioUm.id = :usuarioUmId and c.usuarioDois.id = :usuarioDoisId)
                or (c.usuarioUm.id = :usuarioDoisId and c.usuarioDois.id = :usuarioUmId))
            """)
    Optional<Conversa> findBetweenUsuarios(
            @Param("usuarioUmId") Long usuarioUmId,
            @Param("usuarioDoisId") Long usuarioDoisId
    );
}
