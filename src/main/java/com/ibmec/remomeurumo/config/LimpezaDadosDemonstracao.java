package com.ibmec.remomeurumo.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.HistoricoOperacao;
import com.ibmec.remomeurumo.model.ModuloHistorico;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

/**
 * Limpeza única do banco publicado no Azure antes do uso real pelo Instituto: remove os participantes
 * fictícios e tudo o que depende deles (matrículas, aulas, presenças, anexos e o histórico desses registros).
 * Mantém as turmas oficiais, os usuários de login e o histórico de turmas e usuários.
 * Só roda no Azure (variável WEBSITE_SITE_NAME) e uma única vez: o registro que ela deixa no histórico
 * impede uma nova execução.
 */
@Component
@Order(Ordered.LOWEST_PRECEDENCE)
public class LimpezaDadosDemonstracao implements ApplicationRunner {

    private static final Logger LOG = LoggerFactory.getLogger(LimpezaDadosDemonstracao.class);

    static final String MARCA = "LimpezaDadosDemonstracao";

    private final String siteAzure;

    @PersistenceContext
    private EntityManager entityManager;

    public LimpezaDadosDemonstracao(@Value("${WEBSITE_SITE_NAME:}") String siteAzure) {
        this.siteAzure = siteAzure;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (siteAzure == null || siteAzure.isBlank()) {
            return;
        }
        executar();
    }

    /** Executa a limpeza se ela ainda não foi feita neste banco. Devolve o número de participantes removidos. */
    @Transactional
    public int executar() {
        Long feitas = entityManager.createQuery(
                "select count(h) from HistoricoOperacao h where h.tipoEntidade = :marca", Long.class)
                .setParameter("marca", MARCA)
                .getSingleResult();
        if (feitas > 0) {
            return 0;
        }
        entityManager.createQuery("delete from Presenca").executeUpdate();
        entityManager.createQuery("delete from Anexo").executeUpdate();
        entityManager.createQuery("delete from Matricula").executeUpdate();
        entityManager.createQuery("delete from Encontro").executeUpdate();
        int participantes = entityManager.createQuery("delete from Participante").executeUpdate();
        entityManager.createQuery("delete from HistoricoOperacao h where h.modulo in :modulos")
                .setParameter("modulos", java.util.List.of(ModuloHistorico.PARTICIPANTES, ModuloHistorico.MATRICULAS,
                        ModuloHistorico.ENCONTROS, ModuloHistorico.PRESENCAS))
                .executeUpdate();

        HistoricoOperacao registro = new HistoricoOperacao(ModuloHistorico.PARTICIPANTES, MARCA, 0L,
                "Dados de demonstração", AcaoHistorico.EXCLUSAO,
                "Participantes fictícios removidos antes do uso real, com suas matrículas, aulas, presenças, "
                        + "anexos e histórico (" + participantes + " participantes). Turmas e usuários foram mantidos.");
        registro.setUsuario("sistema");
        entityManager.persist(registro);
        LOG.warn("Limpeza dos dados de demonstração concluída: {} participantes removidos.", participantes);
        return participantes;
    }
}
