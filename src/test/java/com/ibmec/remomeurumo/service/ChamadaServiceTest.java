package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.LinhaChamada;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import org.springframework.mock.web.MockMultipartFile;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.TipoAnexo;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.EncontroRepository;

@SpringBootTest
@Transactional
class ChamadaServiceTest {

    @Autowired private ChamadaService chamadaService;
    @Autowired private MatriculaService matriculaService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private AnexoService anexoService;
    @Autowired private EncontroService encontroService;
    @Autowired private EncontroRepository encontroRepository;

    @Test
    void salvarCriaAAulaQuandoAindaNaoExiste() {
        LocalDate data = LocalDate.now().minusDays(1);
        Atividade turma = turma("Chamada Nova Aula");
        Participante ana = matricular("Chamada Ana", turma, data.minusDays(30));
        Participante bia = matricular("Chamada Bia", turma, data.minusDays(30));

        var resultado = chamadaService.salvar(turma.getId(), data, marcacoes(
                ana.getId(), StatusPresenca.PRESENTE, bia.getId(), StatusPresenca.JUSTIFICADA));

        assertThat(resultado.presentes()).isEqualTo(1);
        assertThat(resultado.justificadas()).isEqualTo(1);
        var aula = encontroRepository.findByAtividadeIdAndData(turma.getId(), data).orElseThrow();
        assertThat(aula.getStatus()).isEqualTo(StatusEncontro.REALIZADO);
        assertThat(chamadaService.montar(turma.getId(), data).isRegistrada()).isTrue();
    }

    @Test
    void salvarExigeTodosMarcados() {
        LocalDate data = LocalDate.now().minusDays(1);
        Atividade turma = turma("Chamada Incompleta");
        Participante ana = matricular("Chamada Incompleta Ana", turma, data.minusDays(30));
        matricular("Chamada Incompleta Bia", turma, data.minusDays(30));

        assertThatThrownBy(() -> chamadaService.salvar(turma.getId(), data,
                marcacoes(ana.getId(), StatusPresenca.PRESENTE)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("Marque todos");
    }

    @Test
    void naoAceitaPessoaDeOutraTurmaNemDataFutura() {
        LocalDate data = LocalDate.now().minusDays(1);
        Atividade turma = turma("Chamada Fechada");
        Participante ana = matricular("Chamada Fechada Ana", turma, data.minusDays(30));
        Participante intruso = participanteService.cadastrar(new Participante("Chamada Intruso", ""));

        assertThatThrownBy(() -> chamadaService.salvar(turma.getId(), data, marcacoes(
                ana.getId(), StatusPresenca.PRESENTE, intruso.getId(), StatusPresenca.PRESENTE)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("não pertence");
        assertThatThrownBy(() -> chamadaService.montar(turma.getId(), LocalDate.now().plusDays(1)))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("dia da aula");
    }

    @Test
    void aulaPlanejadaPassaARealizadaEFaltasAnterioresAparecem() {
        LocalDate hoje = LocalDate.now();
        Atividade turma = turma("Chamada Risco");
        Participante ana = matricular("Chamada Risco Ana", turma, hoje.withDayOfMonth(1));
        if (hoje.getDayOfMonth() > 1) {
            encontroService.criar(turma.getId(), hoje.withDayOfMonth(1), "", StatusEncontro.REALIZADO);
        }
        encontroService.criar(turma.getId(), hoje, "", StatusEncontro.PLANEJADO);

        var chamada = chamadaService.montar(turma.getId(), hoje);
        LinhaChamada linha = chamada.linhas().get(0);
        assertThat(linha.faltasNoMes()).isEqualTo(hoje.getDayOfMonth() > 1 ? 1 : 0);

        chamadaService.salvar(turma.getId(), hoje, marcacoes(ana.getId(), StatusPresenca.AUSENTE));
        assertThat(encontroRepository.findByAtividadeIdAndData(turma.getId(), hoje).orElseThrow().getStatus())
                .isEqualTo(StatusEncontro.REALIZADO);
    }

    @Test
    void pendentesListaAulasPassadasSemRegistro() {
        LocalDate ontem = LocalDate.now().minusDays(1);
        Atividade turma = turma("Chamada Pendente");
        matricular("Chamada Pendente Ana", turma, ontem.minusDays(30));
        encontroService.criar(turma.getId(), ontem, "", StatusEncontro.REALIZADO);

        assertThat(chamadaService.pendentes())
                .anySatisfy(sessao -> {
                    assertThat(sessao.turmaId()).isEqualTo(turma.getId());
                    assertThat(sessao.quando()).isEqualTo("Ontem");
                });
    }

    @Test
    void sessoesDeHojeMostraTurmaDaGradeSemParticipantes() {
        Atividade turma = turma("Chamada Sem Matriculas");

        assertThat(chamadaService.sessoesDeHoje())
                .anySatisfy(sessao -> {
                    assertThat(sessao.turmaId()).isEqualTo(turma.getId());
                    assertThat(sessao.participantes()).isZero();
                });
    }

    @Test
    void reenviarAtestadoDaMesmaAulaTrocaOAnterior() {
        LocalDate hoje = LocalDate.now();
        Atividade turma = turma("Chamada Atestado");
        Participante ana = matricular("Chamada Atestado Ana", turma, hoje.minusDays(30));
        var aula = encontroService.criar(turma.getId(), hoje, "", StatusEncontro.REALIZADO);
        for (int i = 0; i < 3; i++) {
            anexoService.anexar(ana.getId(), TipoAnexo.ATESTADO,
                    new MockMultipartFile("a", "atestado.pdf", "application/pdf", ("%PDF-" + i).getBytes()), aula.getId());
        }

        assertThat(anexoService.listarPorParticipante(ana.getId())).hasSize(1);
    }

    private Atividade turma(String nome) {
        return atividadeService.cadastrar(new Atividade(nome, "",
                EnumSet.allOf(DiaSemana.class), LocalTime.of(14, 0)));
    }

    private Participante matricular(String nome, Atividade turma, LocalDate inicio) {
        Participante participante = participanteService.cadastrar(new Participante(nome, ""));
        matriculaService.matricular(participante.getId(), turma.getId(), inicio);
        return participante;
    }

    private Map<Long, StatusPresenca> marcacoes(Object... pares) {
        Map<Long, StatusPresenca> mapa = new LinkedHashMap<>();
        for (int indice = 0; indice < pares.length; indice += 2) {
            mapa.put((Long) pares[indice], (StatusPresenca) pares[indice + 1]);
        }
        return mapa;
    }
}
