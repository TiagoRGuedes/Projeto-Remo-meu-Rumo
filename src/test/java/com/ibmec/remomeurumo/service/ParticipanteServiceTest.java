package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Participante;

@SpringBootTest
@Transactional
class ParticipanteServiceTest {

    @Autowired
    private ParticipanteService participanteService;

    @Test
    void deveCadastrarParticipanteValido() {
        Participante participante = participanteService.cadastrar(new Participante("Maria Teste", "Cadastro de teste"));

        assertThat(participante.getId()).isNotNull();
        assertThat(participante.isAtivo()).isTrue();
        assertThat(participante.getNome()).isEqualTo("Maria Teste");
    }

    @Test
    void deveValidarNomeObrigatorio() {
        assertThatThrownBy(() -> participanteService.cadastrar(new Participante(" ", "")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Informe o nome");
    }
}
