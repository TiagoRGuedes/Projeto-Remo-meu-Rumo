package com.ibmec.remomeurumo.support;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class CsvTest {

    @Test
    void textoComecandoComCaractereDeFormulaGanhaApostrofo() {
        assertThat(Csv.celula("=1+1")).isEqualTo("\"'=1+1\"");
        assertThat(Csv.celula("+55")).isEqualTo("\"'+55\"");
        assertThat(Csv.celula("-2")).isEqualTo("\"'-2\"");
        assertThat(Csv.celula("@soma")).isEqualTo("\"'@soma\"");
        assertThat(Csv.celula("Ana \"Lu\"")).isEqualTo("\"Ana \"\"Lu\"\"\"");
        assertThat(Csv.celula(null)).isEqualTo("\"\"");
    }
}
