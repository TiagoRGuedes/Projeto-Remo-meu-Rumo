package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
@Transactional
class FrequenciaMensalServiceTest {

    private static final YearMonth MES = YearMonth.of(2035, 3);

    @Autowired private FrequenciaService frequenciaService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private EncontroService encontroService;
    @Autowired private PresencaService presencaService;

    @Test
    void aulaSemRegistroContaComoFaltaEAulaCanceladaNaoConta() {
        Atividade turma = turma("Mensal Base");
        Participante participante = matricular("Mensal Pessoa", turma, MES.atDay(1));
        Encontro primeira = aula(turma, 5, StatusEncontro.REALIZADO);
        Encontro segunda = aula(turma, 12, StatusEncontro.REALIZADO);
        aula(turma, 19, StatusEncontro.REALIZADO);
        aula(turma, 26, StatusEncontro.CANCELADO);
        presencaService.registrarUnico(participante.getId(), primeira.getId(), StatusPresenca.PRESENTE, "");
        presencaService.registrarUnico(participante.getId(), segunda.getId(), StatusPresenca.JUSTIFICADA, "");

        FrequenciaMensal linha = frequenciaService.linhasMensaisDaTurma(turma.getId(), MES).get(0);

        assertThat(linha.aulasPrevistas()).isEqualTo(3);
        assertThat(linha.presencas()).isEqualTo(1);
        assertThat(linha.faltas()).isEqualTo(2);
        assertThat(linha.faltasJustificadas()).isEqualTo(1);
        assertThat(linha.percentual()).isEqualTo(33.3);
        assertThat(linha.abaixoDaMeta()).isTrue();
        assertThat(linha.contatoNecessario()).isFalse();
    }

    @Test
    void maisDeTresFaltasNoMesPedemContato() {
        Atividade turma = turma("Mensal Faltas");
        Participante faltoso = matricular("Mensal Faltoso", turma, MES.atDay(1));
        Participante assiduo = matricular("Mensal Assíduo", turma, MES.atDay(1));
        for (int dia = 3; dia <= 24; dia += 7) {
            Encontro aula = aula(turma, dia, StatusEncontro.REALIZADO);
            presencaService.registrarLote(aula.getId(), Map.of(
                    faltoso.getId(), StatusPresenca.AUSENTE,
                    assiduo.getId(), StatusPresenca.PRESENTE), Map.of());
        }

        var resumo = frequenciaService.resumoMensalDaTurma(turma.getId(), MES);

        assertThat(resumo.aulasPrevistas()).isEqualTo(4);
        assertThat(resumo.percentual()).isEqualTo(50.0);
        assertThat(resumo.faltosos()).extracting(FrequenciaMensal::participanteId)
                .containsExactly(faltoso.getId());
    }

    @Test
    void matriculaNoMeioDoMesSoContaAulasAPartirDoInicio() {
        Atividade turma = turma("Mensal Meio");
        aula(turma, 2, StatusEncontro.REALIZADO);
        aula(turma, 9, StatusEncontro.REALIZADO);
        Encontro depois = aula(turma, 16, StatusEncontro.REALIZADO);
        Participante novato = matricular("Mensal Novato", turma, MES.atDay(15));
        presencaService.registrarUnico(novato.getId(), depois.getId(), StatusPresenca.PRESENTE, "");

        var frequencia = frequenciaService.frequenciaMensalDoParticipante(novato.getId(), MES);

        assertThat(frequencia.consolidado().aulasPrevistas()).isEqualTo(1);
        assertThat(frequencia.consolidado().percentual()).isEqualTo(100.0);
    }

    @Test
    void chamadaListaSomenteMatriculadosDaTurma() {
        Atividade turma = turma("Mensal Chamada");
        Participante matriculado = matricular("Mensal Matriculado", turma, MES.atDay(1));
        participanteService.cadastrar(new Participante("Mensal Fora da Turma", ""));
        Encontro aula = aula(turma, 6, StatusEncontro.REALIZADO);

        assertThat(presencaService.prepararItens(aula.getId(), "Mensal", false))
                .extracting(item -> item.participanteId())
                .containsExactly(matriculado.getId());
    }

    private Atividade turma(String nome) {
        return atividadeService.cadastrar(new Atividade(nome, "",
                EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(9, 0)));
    }

    private Participante matricular(String nome, Atividade turma, LocalDate inicio) {
        Participante participante = participanteService.cadastrar(new Participante(nome, ""));
        matriculaService.matricular(participante.getId(), turma.getId(), inicio);
        return participante;
    }

    private Encontro aula(Atividade turma, int dia, StatusEncontro status) {
        return encontroService.criar(turma.getId(), MES.atDay(dia), "", status);
    }
}
