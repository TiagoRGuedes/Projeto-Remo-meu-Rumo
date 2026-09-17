package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.ibmec.remomeurumo.dto.FrequenciaResumo;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
class FrequenciaServiceTest {

    @Autowired
    private FrequenciaService frequenciaService;

    @Test
    void deveCalcularFrequenciaComPresentesSobreTotal() {
        Presenca p1 = new Presenca();
        p1.setStatus(StatusPresenca.PRESENTE);
        Presenca p2 = new Presenca();
        p2.setStatus(StatusPresenca.AUSENTE);
        Presenca p3 = new Presenca();
        p3.setStatus(StatusPresenca.JUSTIFICADA);

        FrequenciaResumo resumo = frequenciaService.resumir("Participante", List.of(p1, p2, p3));

        assertThat(resumo.totalEncontros()).isEqualTo(3);
        assertThat(resumo.presentes()).isEqualTo(1);
        assertThat(resumo.ausentes()).isEqualTo(1);
        assertThat(resumo.justificadas()).isEqualTo(1);
        assertThat(resumo.percentual()).isEqualTo(33.3);
    }
}
