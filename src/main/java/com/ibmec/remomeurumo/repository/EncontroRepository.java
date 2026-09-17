package com.ibmec.remomeurumo.repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.StatusEncontro;

public interface EncontroRepository extends JpaRepository<Encontro, Long> {

    List<Encontro> findByAtividadeIdOrderByDataDesc(Long atividadeId);

    List<Encontro> findTop8ByAtividadeNomeContainingIgnoreCaseOrderByDataDesc(String nomeAtividade);

    List<Encontro> findByDataOrderByAtividadeNomeAsc(LocalDate data);

    Optional<Encontro> findByAtividadeIdAndData(Long atividadeId, LocalDate data);

    long countByDataBetween(LocalDate inicio, LocalDate fim);

    List<Encontro> findTop8ByOrderByDataDesc();

    List<Encontro> findTop5ByOrderByDataDesc();

    Optional<Encontro> findFirstByStatusAndDataLessThanEqualOrderByDataDescIdDesc(
            StatusEncontro status, LocalDate data);

    boolean existsByAtividadeId(Long atividadeId);

    long countByAtividadeId(Long atividadeId);

    long countByStatus(StatusEncontro status);

    @Query("""
            select e from Encontro e
            join fetch e.atividade atividade
            where (:atividadeId is null or atividade.id = :atividadeId)
              and (:status is null or e.status = :status)
              and (:inicio is null or e.data >= :inicio)
              and (:fim is null or e.data <= :fim)
              and (:texto is null or lower(atividade.nome) like lower(concat('%', :texto, '%')))
            order by e.data desc, atividade.nome asc
            """)
    List<Encontro> filtrar(
            @Param("atividadeId") Long atividadeId,
            @Param("status") StatusEncontro status,
            @Param("inicio") LocalDate inicio,
            @Param("fim") LocalDate fim,
            @Param("texto") String texto);

    @org.springframework.data.jpa.repository.Query("""
            select count(e) from Encontro e
            where e.status <> com.ibmec.remomeurumo.model.StatusEncontro.CANCELADO
              and not exists (select p.id from Presenca p where p.encontro = e)
            """)
    long contarSemPresencaRegistrada();
}
