package com.example.demo.repositories;

import com.example.demo.models.DocObra;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocObraRepository extends JpaRepository<DocObra, Long> {

    @Query("select d.id as id, d.nome as nome, d.nomeArquivo as nomeArquivo, d.tipo as tipo, "
            + "d.tamanho as tamanho, d.dataCriacao as dataCriacao from DocObra d "
            + "where d.obra.id = :obraId and d.dataDelecao is null order by d.id desc")
    List<DocObraResumo> findByObraIdOrderByIdDesc(@Param("obraId") Long obraId);

    @Query("select d from DocObra d where d.id = :id and d.obra.id = :obraId and d.dataDelecao is null")
    Optional<DocObra> findByIdAndObraId(@Param("id") Long id, @Param("obraId") Long obraId);

    @Query(value = "SELECT lo_get(d.arquivo) FROM doc_obra d WHERE d.id = :documentoId AND d.obra_id = :obraId AND d.data_delecao IS NULL",
            nativeQuery = true)
    Optional<byte[]> findArquivoByIdAndObraId(
            @Param("documentoId") Long documentoId,
            @Param("obraId") Long obraId
    );
}
