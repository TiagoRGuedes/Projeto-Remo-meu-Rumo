package com.ibmec.remomeurumo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusPresenca;

public interface PresencaRepository extends JpaRepository<Presenca, Long> {

    Optional<Presenca> findByParticipanteIdAndEncontroId(Long participanteId, Long encontroId);

    boolean existsByParticipanteIdAndEncontroId(Long participanteId, Long encontroId);

    boolean existsByParticipanteId(Long participanteId);

    boolean existsByEncontroId(Long encontroId);

    long countByParticipanteId(Long participanteId);

    long countByEncontroId(Long encontroId);

    List<Presenca> findByEncontroIdOrderByParticipanteNomeAsc(Long encontroId);

    List<Presenca> findByParticipanteIdOrderByEncontroDataDesc(Long participanteId);

    long countByStatus(StatusPresenca status);

    long countByEncontroIdAndStatus(Long encontroId, StatusPresenca status);

    long countByStatusAndEncontroDataBetween(StatusPresenca status, LocalDate inicio, LocalDate fim);

    @Query("""
            select p from Presenca p
            join fetch p.participante participante
            join fetch p.encontro encontro
            join fetch encontro.atividade atividade
            where (:participanteId is null or participante.id = :participanteId)
              and (:atividadeId is null or atividade.id = :atividadeId)
              and (:status is null or p.status = :status)
              and (:inicio is null or encontro.data >= :inicio)
              and (:fim is null or encontro.data <= :fim)
            order by encontro.data desc, participante.nome asc
            """)
    List<Presenca> filtrarHistorico(
            @Param("participanteId") Long participanteId,
            @Param("atividadeId") Long atividadeId,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("status") StatusPresenca status);

    @Query("""
            select p from Presenca p
            join fetch p.participante participante
            join fetch p.encontro encontro
            join fetch encontro.atividade atividade
            order by encontro.data desc, participante.nome asc
            """)
    List<Presenca> listarHistorico();

    @Query("""
            select p from Presenca p
            join fetch p.participante participante
            join fetch p.encontro encontro
            join fetch encontro.atividade atividade
            where encontro.atividade.id = :atividadeId
            order by encontro.data desc, participante.nome asc
            """)
    List<Presenca> findByAtividadeId(@Param("atividadeId") Long atividadeId);

    @Query("""
            select count(distinct p.participante.id) from Presenca p
            where p.encontro.atividade.id = :atividadeId
            """)
    long contarParticipantesPorAtividade(@Param("atividadeId") Long atividadeId);
}
