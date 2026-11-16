package com.ibmec.remomeurumo;

import static org.assertj.core.api.Assertions.assertThat;

import java.sql.SQLException;

import org.junit.jupiter.api.Test;

class RemoMeuRumoApplicationTest {

    @Test
    void reconheceBancoEmUsoNaCadeiaDeCausas() {
        SQLException h2 = new SQLException("Database may be already in use: \"dados/remo-meu-rumo.mv.db\"", "90020",
                RemoMeuRumoApplication.H2_BANCO_EM_USO);
        RuntimeException erro = new IllegalStateException("Falha ao subir", new RuntimeException("JPA", h2));

        assertThat(RemoMeuRumoApplication.bancoEmUso(erro)).isTrue();
        assertThat(RemoMeuRumoApplication.bancoEmUso(new RuntimeException("The file is locked: dados/x.mv.db"))).isTrue();
    }

    @Test
    void outrosErrosNaoGeramNovaTentativa() {
        RuntimeException erro = new IllegalStateException("Falha ao subir", new SQLException("Tabela inexistente", "42S02", 42102));

        assertThat(RemoMeuRumoApplication.bancoEmUso(erro)).isFalse();
        assertThat(RemoMeuRumoApplication.bancoEmUso(new IllegalArgumentException())).isFalse();
        assertThat(RemoMeuRumoApplication.bancoEmUso(null)).isFalse();
    }

    @Test
    void perfilDemoEUrlPropriaNaoUsamBancoPrincipal() {
        assertThat(RemoMeuRumoApplication.usaBancoPrincipal(new String[] {"--spring.profiles.active=demo"})).isFalse();
        assertThat(RemoMeuRumoApplication.usaBancoPrincipal(new String[] {"--spring.datasource.url=jdbc:h2:mem:x"})).isFalse();
    }

    @Test
    void resumoDaFalhaListaAsCausas() {
        RuntimeException erro = new IllegalStateException("externa", new IllegalArgumentException("interna"));

        assertThat(RemoMeuRumoApplication.resumoDaFalha(erro))
                .contains("java.lang.IllegalStateException: externa")
                .contains("java.lang.IllegalArgumentException: interna");
    }
}
