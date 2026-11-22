package com.ibmec.remomeurumo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RedirecionamentoAposLoginTest {

    @Test
    void remontaOEnderecoDaChamadaAPartirDePartesValidas() {
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/chamada/7?data=2026-11-10&corrigir=1"))
                .isEqualTo("/chamada/7?data=2026-11-10&corrigir=1");
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/chamada/12")).isEqualTo("/chamada/12");
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/chamada/12?data=2026-11-10&outro=https://mal.com"))
                .isEqualTo("/chamada/12?data=2026-11-10");
    }

    @Test
    void recusaEnderecosQueLevariamParaForaDoSistema() {
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("//mal.com/chamada/1")).isNull();
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("https://mal.com/chamada/1")).isNull();
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/chamada/1/../../outra")).isNull();
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/chamada/abc")).isNull();
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/participantes")).isNull();
        assertThat(SegurancaConfig.caminhoSeguroDaChamada(null)).isNull();
        assertThat(SegurancaConfig.caminhoSeguroDaChamada("/chamada/3?data=amanha")).isEqualTo("/chamada/3");
    }
}
