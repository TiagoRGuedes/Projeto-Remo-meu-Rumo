package com.ibmec.remomeurumo.repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.HistoricoOperacao;
import com.ibmec.remomeurumo.model.ModuloHistorico;

public interface HistoricoOperacaoRepository extends JpaRepository<HistoricoOperacao, Long> {

    List<HistoricoOperacao> findTop8ByOrderByDataHoraDescIdDesc();

    List<HistoricoOperacao> findTop8ByEntidadeIdAndModuloInOrderByDataHoraDescIdDesc(
            Long entidadeId, Collection<ModuloHistorico> modulos);

    long countByDataHoraBetween(LocalDateTime inicio, LocalDateTime fim);

    long countByAcao(AcaoHistorico acao);

    long countByAcaoIn(Collection<AcaoHistorico> acoes);

    @Query("""
            select h from HistoricoOperacao h
            where (:inicio is null or h.dataHora >= :inicio)
              and (:fim is null or h.dataHora < :fim)
              and (:modulo is null or h.modulo = :modulo)
              and (:acao is null or h.acao = :acao)
              and (:texto is null
                   or lower(h.nomeEntidade) like lower(concat('%', :texto, '%'))
                   or lower(h.tipoEntidade) like lower(concat('%', :texto, '%'))
                   or lower(h.descricao) like lower(concat('%', :texto, '%')))
            order by h.dataHora desc, h.id desc
            """)
    List<HistoricoOperacao> filtrar(
            @Param("inicio") LocalDateTime inicio,
            @Param("fim") LocalDateTime fim,
            @Param("modulo") ModuloHistorico modulo,
            @Param("acao") AcaoHistorico acao,
            @Param("texto") String texto);
}
