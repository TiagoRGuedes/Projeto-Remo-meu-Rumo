package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.MotivoSaida;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusMatricula;

@SpringBootTest
@Transactional
class MatriculaServiceTest {

    @Autowired private MatriculaService matriculaService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;

    @Test
    void participantePodeTerMatriculaEmMaisDeUmaTurma() {
        Participante participante = participanteService.cadastrar(new Participante("Duas Turmas", ""));
        Atividade manha = turma("Manhã Matrícula");
        Atividade tarde = turma("Tarde Matrícula");

        matriculaService.matricular(participante.getId(), manha.getId(), LocalDate.now());
        matriculaService.matricular(participante.getId(), tarde.getId(), LocalDate.now());

        assertThat(matriculaService.listarPorParticipante(participante.getId())).hasSize(2);
        assertThat(matriculaService.turmasDisponiveis(participante.getId()))
                .extracting(Atividade::getId)
                .doesNotContain(manha.getId(), tarde.getId());
    }

    @Test
    void naoPermiteMatriculaAtivaDuplicada() {
        Participante participante = participanteService.cadastrar(new Participante("Duplicada", ""));
        Atividade turma = turma("Turma Duplicada");
        matriculaService.matricular(participante.getId(), turma.getId(), LocalDate.now());

        assertThatThrownBy(() -> matriculaService.matricular(participante.getId(), turma.getId(), LocalDate.now()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Já existe uma matrícula ativa");
    }

    @Test
    void encerrarExigeMotivoEReativarPreservaAMesmaMatricula() {
        Participante participante = participanteService.cadastrar(new Participante("Saída e Retorno", ""));
        Atividade turma = turma("Turma Saída");
        Matricula matricula = matriculaService.matricular(participante.getId(), turma.getId(), LocalDate.now());

        assertThatThrownBy(() -> matriculaService.encerrar(matricula.getId(), null, null, LocalDate.now()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("motivo");

        Matricula encerrada = matriculaService.encerrar(matricula.getId(),
                MotivoSaida.DEDICACAO_OUTRA_ATIVIDADE, "Passou a jogar futebol", LocalDate.now());
        assertThat(encerrada.getStatus()).isEqualTo(StatusMatricula.ENCERRADA);
        assertThat(encerrada.getMotivoSaida()).isEqualTo(MotivoSaida.DEDICACAO_OUTRA_ATIVIDADE);

        Matricula retorno = matriculaService.matricular(participante.getId(), turma.getId(), LocalDate.now());
        assertThat(retorno.getId()).isEqualTo(matricula.getId());
        assertThat(retorno.isAtiva()).isTrue();
        assertThat(retorno.getMotivoSaida()).isNull();
    }

    @Test
    void naoMatriculaParticipanteOuTurmaInativos() {
        Participante inativo = participanteService.cadastrar(new Participante("Inativo Matrícula", ""));
        participanteService.desativar(inativo.getId());
        Atividade turma = turma("Turma Ativa");

        assertThatThrownBy(() -> matriculaService.matricular(inativo.getId(), turma.getId(), LocalDate.now()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativo");

        Participante ativo = participanteService.cadastrar(new Participante("Ativo Matrícula", ""));
        Atividade fechada = turma("Turma Fechada");
        atividadeService.desativar(fechada.getId());

        assertThatThrownBy(() -> matriculaService.matricular(ativo.getId(), fechada.getId(), LocalDate.now()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("inativa");
    }

    @Test
    void participanteComMatriculaNaoPodeSerExcluido() {
        Participante participante = participanteService.cadastrar(new Participante("Com Matrícula", ""));
        Atividade turma = turma("Turma Exclusão");
        matriculaService.matricular(participante.getId(), turma.getId(), LocalDate.now());

        assertThat(participanteService.podeExcluir(participante.getId())).isFalse();
        assertThat(atividadeService.podeExcluir(turma.getId())).isFalse();
    }

    private Atividade turma(String nome) {
        return atividadeService.cadastrar(new Atividade(nome, "",
                EnumSet.of(DiaSemana.TERCA, DiaSemana.QUINTA), LocalTime.of(14, 0)));
    }
}
