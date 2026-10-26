package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;

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
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
@Transactional
class PainelInicialServiceTest {

    private static final YearMonth MES = YearMonth.of(2025, 5);

    @Autowired private PainelInicialService painelInicialService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private EncontroService encontroService;
    @Autowired private PresencaService presencaService;

    @Test
    void painelMostraFrequenciaContraAMetaEQuemPrecisaDeContato() {
        Atividade turma = atividadeService.cadastrar(new Atividade("Painel Turma", "",
                EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(8, 0)));
        Participante faltosa = participanteService.cadastrar(new Participante("Painel Faltosa", ""));
        Participante presente = participanteService.cadastrar(new Participante("Painel Presente", ""));
        matriculaService.matricular(faltosa.getId(), turma.getId(), MES.atDay(1));
        matriculaService.matricular(presente.getId(), turma.getId(), MES.atDay(1));
        for (int dia : new int[] {5, 12, 19, 26}) {
            var aula = encontroService.criar(turma.getId(), MES.atDay(dia), "", StatusEncontro.REALIZADO);
            presencaService.registrarLote(aula.getId(), Map.of(
                    faltosa.getId(), StatusPresenca.AUSENTE,
                    presente.getId(), StatusPresenca.PRESENTE), Map.of());
        }

        var painel = painelInicialService.carregar(MES);

        assertThat(painel.tituloMes()).isEqualTo("Maio de 2025");
        assertThat(painel.meta()).isEqualTo(60.0);
        assertThat(painel.precisamContato()).extracting(FrequenciaMensal::participanteId).contains(faltosa.getId());
        assertThat(painel.precisamContato()).extracting(FrequenciaMensal::participanteId).doesNotContain(presente.getId());
        assertThat(painel.turmas()).anySatisfy(resumo -> {
            assertThat(resumo.turmaId()).isEqualTo(turma.getId());
            assertThat(resumo.percentual()).isEqualTo(50.0);
            assertThat(resumo.abaixoDaMeta()).isTrue();
        });
        assertThat(painel.mesAtual()).isFalse();
        assertThat(painel.chamadasDeHoje()).isEmpty();
        assertThat(painel.mesSeguinte()).isEqualTo("2025-06");
    }

    @Test
    void mesAtualNaoOfereceMesFuturo() {
        var painel = painelInicialService.carregar(YearMonth.now());

        assertThat(painel.mesAtual()).isTrue();
        assertThat(painel.mesSeguinte()).isNull();
    }
}
