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
    }

    @Test
    void outrosErrosNaoGeramNovaTentativa() {
        RuntimeException erro = new IllegalStateException("Falha ao subir", new SQLException("Tabela inexistente", "42S02", 42102));

        assertThat(RemoMeuRumoApplication.bancoEmUso(erro)).isFalse();
        assertThat(RemoMeuRumoApplication.bancoEmUso(new IllegalArgumentException())).isFalse();
    }
}
