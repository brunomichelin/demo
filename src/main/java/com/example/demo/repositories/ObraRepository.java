package com.example.demo.repositories;

import com.example.demo.models.Obra;
import io.micrometer.core.annotation.Counted;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import com.example.demo.models.StatusCronEnum;

import java.time.LocalDate;

import java.util.Optional;

public interface ObraRepository extends JpaRepository<Obra, Long> {

    @EntityGraph(attributePaths = "clientes")
    Optional<Obra> findWithClientesById(Long id);

    @Override
    @Counted(value = "obra_repositorio_find_all_count", description = "Contagem de chamadas para findAll")
    @Query("SELECT o FROM Obra o WHERE o.dataDelecao IS NULL")
    Page<Obra> findAll(Pageable pageable);

    @Counted(value = "obra_repositorio_find_all_order_by_titulo_count", description = "Contagem de chamadas para findAllOrderByTitulo")
    @Query("SELECT o FROM Obra o WHERE o.dataDelecao IS NULL ORDER BY o.nome ASC")
    Page<Obra> findAllOrderByTitulo(Pageable pageable);

    @Query("""
            select distinct o from Obra o left join o.clientes c
            where o.dataDelecao is null
              and (:status is null or o.status = :status)
              and (:clienteId is null or c.id = :clienteId)
              and (:inicio is null or o.dataInicioPrevista >= :inicio)
              and (:fim is null or o.dataFimPrevista <= :fim)
              and (:usuarioId is null or exists (
                  select vinculo.id from Obra vinculada join vinculada.usuariosVinculados vinculo
                  where vinculada.id = o.id and vinculo.id = :usuarioId
              ) or exists (
                  select cliente.id from Obra vinculadaCliente join vinculadaCliente.clientes cliente
                  where vinculadaCliente.id = o.id and cliente.id = :usuarioId
              ))
            """)
    Page<Obra> findFiltered(
            @Param("status") StatusCronEnum status,
            @Param("clienteId") Long clienteId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("usuarioId") Long usuarioId,
            Pageable pageable
    );

    @Override
    @Query("select (count(o) > 0) from Obra o where o.id = :id and o.dataDelecao is null")
    boolean existsById(@Param("id") Long id);

    @Override
    @Query("select o from Obra o where o.id = :id and o.dataDelecao is null")
    Optional<Obra> findById(@Param("id") Long id);

    @Query("select (count(o) > 0) from Obra o join o.clientes c "
            + "where o.id = :obraId and c.id = :usuarioId and o.dataDelecao is null")
    boolean usuarioClienteDaObra(@Param("obraId") Long obraId, @Param("usuarioId") Long usuarioId);

    @Query("select (count(o) > 0) from Obra o join o.usuariosVinculados v "
            + "where o.id = :obraId and v.id = :usuarioId and o.dataDelecao is null")
    boolean usuarioVinculadoAObra(@Param("obraId") Long obraId, @Param("usuarioId") Long usuarioId);

}