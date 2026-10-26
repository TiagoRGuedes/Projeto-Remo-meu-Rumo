package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.MotivoCancelamento;
import com.ibmec.remomeurumo.model.MotivoSaida;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.model.TipoAnexo;
import com.ibmec.remomeurumo.model.TipoEscola;
import com.ibmec.remomeurumo.model.Sexo;

@SpringBootTest
@Transactional
class MelhoriasInstitutoTest {

    private static final YearMonth MES = YearMonth.of(2025, 9);

    @Autowired private MatriculaService matriculaService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private EncontroService encontroService;
    @Autowired private PresencaService presencaService;
    @Autowired private FrequenciaService frequenciaService;
    @Autowired private ChamadaService chamadaService;
    @Autowired private AnexoService anexoService;

    @Test
    void trocaDeTurmaDivideAFrequenciaDoMesEntreAsDuasTurmas() {
        Atividade antiga = turma("Troca Antiga");
        Atividade nova = turma("Troca Nova");
        Participante pessoa = participanteService.cadastrar(new Participante("Troca Pessoa", ""));
        var matricula = matriculaService.matricular(pessoa.getId(), antiga.getId(), MES.atDay(1));
        for (int dia : new int[] {1, 8, 15, 22, 29}) {
            encontroService.criar(antiga.getId(), MES.atDay(dia), "", StatusEncontro.REALIZADO);
            encontroService.criar(nova.getId(), MES.atDay(dia), "", StatusEncontro.REALIZADO);
        }

        matriculaService.trocarTurma(matricula.getId(), nova.getId(), MES.atDay(15));

        var porTurma = frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), MES).porTurma();
        assertThat(porTurma).hasSize(2);
        FrequenciaMensal linhaAntiga = porTurma.stream().filter(l -> l.turmaId().equals(antiga.getId())).findFirst().orElseThrow();
        FrequenciaMensal linhaNova = porTurma.stream().filter(l -> l.turmaId().equals(nova.getId())).findFirst().orElseThrow();
        assertThat(linhaAntiga.aulasPrevistas()).isEqualTo(2);
        assertThat(linhaAntiga.observacao()).contains("até a saída em 14/09");
        assertThat(linhaNova.aulasPrevistas()).isEqualTo(3);
        assertThat(linhaNova.observacao()).contains("desde o ingresso em 15/09");
        assertThat(matriculaService.movimentos(pessoa.getId())).extracting(m -> m.tipo()).contains("Troca de turma");
    }

    @Test
    void trocarParaAMesmaTurmaNaoEPermitido() {
        Atividade turma = turma("Troca Mesma");
        Participante pessoa = participanteService.cadastrar(new Participante("Troca Mesma Pessoa", ""));
        var matricula = matriculaService.matricular(pessoa.getId(), turma.getId(), MES.atDay(1));

        assertThatThrownBy(() -> matriculaService.trocarTurma(matricula.getId(), turma.getId(), MES.atDay(10)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("diferente");
    }

    @Test
    void aulaQueNaoAconteceuNaoGeraFaltaEPodeSerDesfeita() {
        Atividade turma = turma("Feriado Turma");
        Participante pessoa = participanteService.cadastrar(new Participante("Feriado Pessoa", ""));
        matriculaService.matricular(pessoa.getId(), turma.getId(), MES.atDay(1));
        Encontro aula = encontroService.criar(turma.getId(), MES.atDay(8), "", StatusEncontro.REALIZADO);
        presencaService.registrarUnico(pessoa.getId(), aula.getId(), StatusPresenca.PRESENTE, "");

        chamadaService.registrarAulaNaoRealizada(turma.getId(), MES.atDay(1), MotivoCancelamento.FERIADO, "Feriado municipal");

        var chamada = chamadaService.montar(turma.getId(), MES.atDay(1));
        assertThat(chamada.isNaoRealizada()).isTrue();
        assertThat(chamada.aulaNaoRealizada()).isEqualTo("Feriado — Feriado municipal");
        assertThat(frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), MES).consolidado().faltas()).isZero();
        assertThatThrownBy(() -> chamadaService.salvar(turma.getId(), MES.atDay(1),
                Map.of(pessoa.getId(), StatusPresenca.PRESENTE)))
                .isInstanceOf(RegraNegocioException.class);

        chamadaService.desfazerAulaNaoRealizada(turma.getId(), MES.atDay(1));
        assertThat(frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), MES).consolidado().faltas()).isEqualTo(1);
    }

    @Test
    void anexoAceitaPdfEFotoERecusaOutrosFormatos() {
        Participante pessoa = participanteService.cadastrar(new Participante("Anexo Pessoa", ""));
        var pdf = new MockMultipartFile("arquivo", "atestado.pdf", "application/pdf", "%PDF-1.4 teste".getBytes());

        var anexo = anexoService.anexar(pessoa.getId(), TipoAnexo.ATESTADO, pdf, null);

        assertThat(anexoService.obter(anexo.getId()).getConteudo()).isEqualTo("%PDF-1.4 teste".getBytes());
        assertThat(anexoService.listarPorParticipante(pessoa.getId())).singleElement()
                .satisfies(resumo -> assertThat(resumo.getDescricao()).startsWith("Atestado"));
        assertThat(participanteService.podeExcluir(pessoa.getId())).isFalse();
        var exe = new MockMultipartFile("arquivo", "virus.exe", "application/octet-stream", new byte[] {1, 2});
        assertThatThrownBy(() -> anexoService.anexar(pessoa.getId(), TipoAnexo.OUTRO, exe, null))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("PDF");
    }

    @Test
    void condicaoSoValeParaQuemTemDeficienciaECompoeADescricao() {
        Participante comCondicao = participanteService.cadastrar(new Participante("Condição Pessoa", Sexo.FEMININO,
                LocalDate.of(2015, 3, 2), TipoEscola.PUBLICA, true, "TEA (autismo)", ""));
        assertThat(comCondicao.getDescricaoDeficiencia()).isEqualTo("TEA (autismo) (CD)");

        assertThatThrownBy(() -> participanteService.cadastrar(new Participante("Condição Inválida", Sexo.MASCULINO,
                LocalDate.of(2015, 3, 2), TipoEscola.PUBLICA, false, "TDAH", "")))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("deficiência ou condição");
    }

    @Test
    void motivoTrocaDeTurmaNaoApareceComoSaidaComum() {
        assertThat(MotivoSaida.paraSaida()).doesNotContain(MotivoSaida.TROCA_DE_TURMA);
    }

    private Atividade turma(String nome) {
        return atividadeService.cadastrar(new Atividade(nome, "", EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(9, 0)));
    }
}
