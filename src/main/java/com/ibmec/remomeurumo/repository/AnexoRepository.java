package com.ibmec.remomeurumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ibmec.remomeurumo.dto.AnexoResumo;
import com.ibmec.remomeurumo.model.Anexo;

public interface AnexoRepository extends JpaRepository<Anexo, Long> {

    @Query("""
            select new com.ibmec.remomeurumo.dto.AnexoResumo(a.id, a.tipo, a.nomeArquivo, a.tamanho, a.enviadoEm, e.data)
            from Anexo a
            left join a.encontro e
            where a.participante.id = :participanteId
            order by a.enviadoEm desc, a.id desc
            """)
    List<AnexoResumo> listarPorParticipante(@Param("participanteId") Long participanteId);

    boolean existsByParticipanteId(Long participanteId);

    @Query("""
            select a.participante.id from Anexo a
            where a.encontro.id = :encontroId
            """)
    List<Long> participantesComAnexoNaAula(@Param("encontroId") Long encontroId);

    @Modifying
    @Query("delete from Anexo a where a.participante.id = :participanteId and a.encontro.id = :encontroId and a.tipo = :tipo")
    void deleteByParticipanteIdAndEncontroIdAndTipo(@Param("participanteId") Long participanteId,
            @Param("encontroId") Long encontroId, @Param("tipo") com.ibmec.remomeurumo.model.TipoAnexo tipo);

    @Modifying
    @Query("update Anexo a set a.encontro = null where a.encontro.id = :encontroId")
    void desvincularAula(@Param("encontroId") Long encontroId);
}
