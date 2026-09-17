package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
@Transactional
class PresencaServiceTest {

    @Autowired
    private ParticipanteService participanteService;

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private EncontroService encontroService;

    @Autowired
    private PresencaService presencaService;

    @Test
    void deveRegistrarPresencaUnica() {
        Participante participante = participanteService.cadastrar(new Participante("Pessoa Única", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Atividade Única", ""));
        Encontro encontro = encontroService.criar(atividade.getId(), LocalDate.of(2026, 1, 11), "");

        Presenca presenca = presencaService.registrarUnico(
                participante.getId(), encontro.getId(), StatusPresenca.PRESENTE, "");

        assertThat(presenca.getId()).isNotNull();
        assertThat(presenca.getStatus()).isEqualTo(StatusPresenca.PRESENTE);
    }

    @Test
    void deveImpedirPresencaDuplicadaNaApiIndividual() {
        Participante participante = participanteService.cadastrar(new Participante("Pessoa Duplicada", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Atividade Duplicada", ""));
        Encontro encontro = encontroService.criar(atividade.getId(), LocalDate.of(2026, 1, 12), "");

        presencaService.registrarUnico(participante.getId(), encontro.getId(), StatusPresenca.PRESENTE, "");

        assertThatThrownBy(() -> presencaService.registrarUnico(
                participante.getId(), encontro.getId(), StatusPresenca.AUSENTE, ""))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe uma presença");
    }
}
