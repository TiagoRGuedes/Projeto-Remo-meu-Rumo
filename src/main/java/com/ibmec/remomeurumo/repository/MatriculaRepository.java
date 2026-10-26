package com.ibmec.remomeurumo.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.StatusMatricula;

public interface MatriculaRepository extends JpaRepository<Matricula, Long> {

    Optional<Matricula> findByParticipanteIdAndAtividadeId(Long participanteId, Long atividadeId);

    boolean existsByParticipanteIdAndAtividadeId(Long participanteId, Long atividadeId);

    boolean existsByAtividadeId(Long atividadeId);

    boolean existsByParticipanteId(Long participanteId);

    long countByAtividadeIdAndStatus(Long atividadeId, StatusMatricula status);

    long countByStatus(StatusMatricula status);

    @Query("""
            select m from Matricula m
            join fetch m.atividade atividade
            where m.participante.id = :participanteId
            order by m.status asc, atividade.nome asc
            """)
    List<Matricula> listarPorParticipante(@Param("participanteId") Long participanteId);

    @Query("""
            select m from Matricula m
            join fetch m.participante participante
            where m.atividade.id = :atividadeId
            order by m.status asc, participante.nome asc
            """)
    List<Matricula> listarPorTurma(@Param("atividadeId") Long atividadeId);

    @Query("""
            select m from Matricula m
            join fetch m.participante participante
            where m.atividade.id = :atividadeId and m.status = :status
            order by participante.nome asc
            """)
    List<Matricula> listarPorTurmaEStatus(@Param("atividadeId") Long atividadeId,
            @Param("status") StatusMatricula status);

    @Query("""
            select m from Matricula m
            join fetch m.participante participante
            join fetch m.atividade atividade
            order by participante.nome asc, atividade.nome asc
            """)
    List<Matricula> listarTodas();
}
