package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.config.TurmasOficiais;
import com.ibmec.remomeurumo.dto.LinhaChamada;
import com.ibmec.remomeurumo.dto.LinhaFrequenciaParticipante;
import com.ibmec.remomeurumo.dto.MesDisponivel;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.MotivoCancelamento;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
@Transactional
class PedidosCoordenacaoTest {

    private static final YearMonth MES = YearMonth.of(2025, 9);

    @Autowired private MatriculaService matriculaService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private EncontroService encontroService;
    @Autowired private PresencaService presencaService;
    @Autowired private FrequenciaService frequenciaService;
    @Autowired private ChamadaService chamadaService;
    @Autowired private HistoricoOperacaoService historicoService;

    @Test
    void listaDeTurmasTemDuasTurmasDeSabadoESemSabadoOnzeHoras() {
        List<String> nomes = TurmasOficiais.LISTA.stream().map(TurmasOficiais.Turma::nome).toList();

        assertThat(nomes).contains("SEG · 08h", "TER/QUI · 08h", "SÁB · 09h", "SÁB · 10h30");
        assertThat(nomes).doesNotContain("SÁB · 11h");
        assertThat(nomes).doesNotHaveDuplicates();
    }

    @Test
    void alunoEmDuasTurmasApareceNasDuasChamadasESomaAsAulasPrevistas() {
        Atividade segunda = turma("Teste SEG 08h", EnumSet.of(DiaSemana.SEGUNDA));
        Atividade terQui = turma("Teste TER/QUI 08h", EnumSet.of(DiaSemana.TERCA, DiaSemana.QUINTA));
        Participante pessoa = pessoa("Aluno Duas Turmas");
        matriculaService.matricular(pessoa.getId(), segunda.getId(), MES.atDay(1));
        matriculaService.matricular(pessoa.getId(), terQui.getId(), MES.atDay(9));
        for (int dia : new int[] {1, 8, 15, 22, 29}) {
            encontroService.criar(segunda.getId(), MES.atDay(dia), "", StatusEncontro.REALIZADO);
        }
        for (int dia : new int[] {2, 4, 9, 11, 16, 18}) {
            encontroService.criar(terQui.getId(), MES.atDay(dia), "", StatusEncontro.REALIZADO);
        }

        assertThat(chamadaService.montar(segunda.getId(), MES.atDay(15)).linhas())
                .extracting(LinhaChamada::participanteId).contains(pessoa.getId());
        assertThat(chamadaService.montar(terQui.getId(), MES.atDay(16)).linhas())
                .extracting(LinhaChamada::participanteId).contains(pessoa.getId());

        var consolidado = frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), MES).consolidado();
        assertThat(consolidado.aulasPrevistas()).isEqualTo(5 + 4);

        LinhaFrequenciaParticipante linha = linhaDe(frequenciaService.painelPorParticipante(false, MES, null), pessoa);
        assertThat(linha.previstas()).isEqualTo(9);
        assertThat(linha.turmas()).isEqualTo("Teste SEG 08h · Teste TER/QUI 08h");
        assertThat(linhaDe(frequenciaService.painelPorParticipante(false, MES, terQui.getId()), pessoa)).isNotNull();
    }

    @Test
    void removerTurmaRegistraMovimentoEHistoricoMasNaoRemoveAUltima() {
        Atividade primeira = turma("Remover A", EnumSet.of(DiaSemana.SEGUNDA));
        Atividade segunda = turma("Remover B", EnumSet.of(DiaSemana.QUARTA));
        Participante pessoa = pessoa("Pessoa Remover");
        var matriculaA = matriculaService.matricular(pessoa.getId(), primeira.getId(), MES.atDay(1));
        var matriculaB = matriculaService.matricular(pessoa.getId(), segunda.getId(), MES.atDay(3));

        matriculaService.removerTurma(matriculaB.getId(), MES.atDay(20));

        assertThatThrownBy(() -> matriculaService.removerTurma(matriculaA.getId(), MES.atDay(21)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Registrar saída");
        assertThat(matriculaService.movimentos(pessoa.getId())).extracting(m -> m.tipo())
                .contains("Ingresso", "Turma adicionada", "Turma removida");
        assertThat(historicoService.listar(null, null, null, AcaoHistorico.TURMA_ADICIONADA, "Pessoa Remover")).hasSize(1);
        assertThat(historicoService.listar(null, null, null, AcaoHistorico.TURMA_REMOVIDA, "Pessoa Remover")).hasSize(1);
    }

    @Test
    void aulaQueNaoAconteceuPorCondicoesClimaticasNaoMudaAFrequenciaDeNinguem() {
        Atividade turma = turma("Clima", EnumSet.of(DiaSemana.SEGUNDA));
        Participante presente = pessoa("Clima Presente");
        Participante faltoso = pessoa("Clima Faltoso");
        matriculaService.matricular(presente.getId(), turma.getId(), MES.atDay(1));
        matriculaService.matricular(faltoso.getId(), turma.getId(), MES.atDay(1));
        for (int dia : new int[] {1, 8}) {
            var aula = encontroService.criar(turma.getId(), MES.atDay(dia), "", StatusEncontro.REALIZADO);
            presencaService.registrarLote(aula.getId(), Map.of(presente.getId(), StatusPresenca.PRESENTE,
                    faltoso.getId(), StatusPresenca.AUSENTE), Map.of());
        }
        double antesPresente = percentual(presente);
        double antesFaltoso = percentual(faltoso);

        chamadaService.registrarAulaNaoRealizada(turma.getId(), MES.atDay(15), MotivoCancelamento.CONDICOES_CLIMATICAS,
                "Alerta da Defesa Civil");
        encontroService.criar(turma.getId(), MES.atDay(22), "", StatusEncontro.PLANEJADO);
        chamadaService.registrarAulaNaoRealizada(turma.getId(), MES.atDay(22), MotivoCancelamento.CONDICOES_CLIMATICAS, null);

        assertThat(percentual(presente)).isEqualTo(antesPresente).isEqualTo(100.0);
        assertThat(percentual(faltoso)).isEqualTo(antesFaltoso).isEqualTo(0.0);
        assertThat(chamadaService.montar(turma.getId(), MES.atDay(15)).aulaNaoRealizada())
                .startsWith("Condições climáticas");

        chamadaService.desfazerAulaNaoRealizada(turma.getId(), MES.atDay(22));
        assertThat(frequenciaService.frequenciaMensalDoParticipante(presente.getId(), MES).consolidado().aulasPrevistas())
                .isEqualTo(3);
    }

    @Test
    void marcarAAulaDeHojeComoNaoAconteceuNaoMudaAFrequencia() {
        LocalDate hoje = LocalDate.now();
        Atividade turma = turma("Clima Hoje", EnumSet.allOf(DiaSemana.class));
        Participante pessoa = pessoa("Clima Hoje Pessoa");
        matriculaService.matricular(pessoa.getId(), turma.getId(), hoje.minusDays(20));
        for (LocalDate dia = hoje.minusDays(3); dia.isBefore(hoje); dia = dia.plusDays(1)) {
            var aula = encontroService.criar(turma.getId(), dia, "", StatusEncontro.REALIZADO);
            presencaService.registrarLote(aula.getId(), Map.of(pessoa.getId(),
                    dia.equals(hoje.minusDays(2)) ? StatusPresenca.AUSENTE : StatusPresenca.PRESENTE), Map.of());
        }
        encontroService.criar(turma.getId(), hoje, "", StatusEncontro.REALIZADO);
        YearMonth mes = YearMonth.now();
        double antes = frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), mes).consolidado().percentual();
        long anoAntes = frequenciaService.frequenciaAnualDoParticipante(pessoa.getId(), hoje.getYear()).consolidado().aulasPrevistas();

        chamadaService.registrarAulaNaoRealizada(turma.getId(), hoje, MotivoCancelamento.CONDICOES_CLIMATICAS, null);

        assertThat(frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), mes).consolidado().percentual())
                .isEqualTo(antes);
        assertThat(frequenciaService.frequenciaAnualDoParticipante(pessoa.getId(), hoje.getYear()).consolidado().aulasPrevistas())
                .isEqualTo(anoAntes).isEqualTo(3);
    }

    @Test
    void visaoAnualBateComASomaDosMeses() {
        Atividade turma = turma("Anual", EnumSet.of(DiaSemana.SEGUNDA));
        Participante pessoa = pessoa("Pessoa Anual");
        matriculaService.matricular(pessoa.getId(), turma.getId(), LocalDate.of(2025, 3, 10));
        LocalDate dia = LocalDate.of(2025, 3, 3);
        int indice = 0;
        while (dia.getYear() == 2025) {
            var aula = encontroService.criar(turma.getId(), dia, "", StatusEncontro.REALIZADO);
            if (indice % 5 == 3) {
                encontroService.cancelar(aula.getId());
            } else {
                presencaService.registrarLote(aula.getId(), Map.of(pessoa.getId(),
                        indice % 3 == 0 ? StatusPresenca.AUSENTE : StatusPresenca.PRESENTE), Map.of());
            }
            dia = dia.plusWeeks(1);
            indice++;
        }

        long presencas = 0;
        long previstas = 0;
        for (int mes = 1; mes <= 12; mes++) {
            var mensal = frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), YearMonth.of(2025, mes)).consolidado();
            presencas += mensal.presencas();
            previstas += mensal.aulasPrevistas();
        }
        var anual = frequenciaService.frequenciaAnualDoParticipante(pessoa.getId(), 2025).consolidado();

        assertThat(anual.presencas()).isEqualTo(presencas);
        assertThat(anual.aulasPrevistas()).isEqualTo(previstas);
        assertThat(anual.faltas()).isEqualTo(previstas - presencas);
        assertThat(previstas).isGreaterThan(30);
        assertThat(linhaDe(frequenciaService.painelPorParticipante(false, YearMonth.of(2025, 6), null), pessoa).presencasAno())
                .isEqualTo(presencas);
    }

    @Test
    void mesesDoFiltroVaoAteOMesCorrenteSemCadastroManual() {
        Atividade turma = turma("Meses", EnumSet.of(DiaSemana.SEGUNDA));
        Participante pessoa = pessoa("Pessoa Meses");
        LocalDate inicio = LocalDate.now().withDayOfYear(1);
        matriculaService.matricular(pessoa.getId(), turma.getId(), inicio);

        List<MesDisponivel> meses = frequenciaService.mesesDisponiveis(YearMonth.now());

        assertThat(meses).isNotEmpty();
        assertThat(meses.get(meses.size() - 1).valor()).isEqualTo(YearMonth.now().toString());
        assertThat(meses).extracting(MesDisponivel::valor).doesNotHaveDuplicates();
        assertThat(meses.get(0).valor()).isEqualTo(YearMonth.of(LocalDate.now().getYear(), 2).isAfter(YearMonth.now())
                ? YearMonth.now().toString() : YearMonth.of(LocalDate.now().getYear(), 2).toString());
    }

    private double percentual(Participante pessoa) {
        return frequenciaService.frequenciaMensalDoParticipante(pessoa.getId(), MES).consolidado().percentual();
    }

    private LinhaFrequenciaParticipante linhaDe(List<LinhaFrequenciaParticipante> linhas, Participante pessoa) {
        return linhas.stream().filter(linha -> linha.participanteId().equals(pessoa.getId())).findFirst().orElse(null);
    }

    private Atividade turma(String nome, Set<DiaSemana> dias) {
        return atividadeService.cadastrar(new Atividade(nome, "", dias, LocalTime.of(8, 0)));
    }

    private Participante pessoa(String nome) {
        return participanteService.cadastrar(new Participante(nome, ""));
    }
}
