package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
@Transactional
class HistoricoOperacaoServiceTest {

    @Autowired
    private ParticipanteService participanteService;

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private EncontroService encontroService;

    @Autowired
    private PresencaService presencaService;

    @Autowired
    private HistoricoOperacaoService historicoService;

    @Test
    void deveRegistrarCicloAdministrativoDoParticipante() {
        Participante participante = participanteService.cadastrar(new Participante("Histórico Teste", ""));
        participanteService.atualizar(participante.getId(), new Participante("Histórico Atualizado", ""));
        participanteService.desativar(participante.getId());
        participanteService.ativar(participante.getId());
        participanteService.excluir(participante.getId());

        var acoes = historicoService.listar(null, null, ModuloHistorico.PARTICIPANTES, null, "Histórico")
                .stream().map(registro -> registro.getAcao()).toList();

        assertThat(acoes).contains(
                AcaoHistorico.CRIACAO,
                AcaoHistorico.EDICAO,
                AcaoHistorico.DESATIVACAO,
                AcaoHistorico.ATIVACAO,
                AcaoHistorico.EXCLUSAO);
    }

    @Test
    void deveRegistrarCicloAdministrativoDaAtividade() {
        Atividade atividade = atividadeService.cadastrar(new Atividade("Atividade Histórico", ""));
        atividadeService.atualizar(atividade.getId(), new Atividade("Atividade Histórico Atualizada", ""));
        atividadeService.desativar(atividade.getId());
        atividadeService.ativar(atividade.getId());
        atividadeService.excluir(atividade.getId());

        var acoes = historicoService.listar(null, null, ModuloHistorico.ATIVIDADES, null, "Atividade Histórico")
                .stream().map(registro -> registro.getAcao()).toList();

        assertThat(acoes).contains(
                AcaoHistorico.CRIACAO,
                AcaoHistorico.EDICAO,
                AcaoHistorico.DESATIVACAO,
                AcaoHistorico.ATIVACAO,
                AcaoHistorico.EXCLUSAO);
    }

    @Test
    void deveRegistrarEdicaoCancelamentoEExclusaoDeEncontro() {
        Atividade atividade = atividadeService.cadastrar(new Atividade("Atividade Encontro Histórico", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.of(2035, 5, 10), "");

        encontroService.atualizar(
                encontro.getId(), atividade.getId(), encontro.getData(), "Planejamento atualizado", StatusEncontro.PLANEJADO);
        encontroService.cancelar(encontro.getId());
        encontroService.excluir(encontro.getId());

        var acoes = historicoService.listar(null, null, ModuloHistorico.ENCONTROS, null, "Atividade Encontro Histórico")
                .stream().map(registro -> registro.getAcao()).toList();

        assertThat(acoes).contains(
                AcaoHistorico.CRIACAO,
                AcaoHistorico.EDICAO,
                AcaoHistorico.CANCELAMENTO,
                AcaoHistorico.EXCLUSAO);
    }

    @Test
    void deveRegistrarPresencaECorrecao() {
        Participante participante = participanteService.cadastrar(new Participante("Pessoa Auditoria", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Atividade Auditoria", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.of(2035, 4, 10), "");

        presencaService.registrarLote(encontro.getId(),
                Map.of(participante.getId(), StatusPresenca.PRESENTE), Map.of());
        presencaService.registrarLote(encontro.getId(),
                Map.of(participante.getId(), StatusPresenca.AUSENTE), Map.of());

        var acoes = historicoService.listar(null, null, ModuloHistorico.PRESENCAS, null, "Atividade Auditoria")
                .stream().map(registro -> registro.getAcao()).toList();

        assertThat(acoes).contains(AcaoHistorico.REGISTRO, AcaoHistorico.CORRECAO);
    }

    @Test
    void deveRegistrarExclusaoDaPresencaComSituacaoAnterior() {
        Participante participante = participanteService.cadastrar(new Participante("Pessoa Exclusão Auditoria", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Atividade Exclusão Auditoria", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.of(2035, 6, 10), "");
        var presenca = presencaService.registrarUnico(
                participante.getId(), encontro.getId(), StatusPresenca.PRESENTE, "");

        presencaService.excluir(presenca.getId());

        var exclusao = historicoService.listar(
                null, null, ModuloHistorico.PRESENCAS, AcaoHistorico.EXCLUSAO, "Atividade Exclusão Auditoria");
        assertThat(exclusao).singleElement().satisfies(registro -> {
            assertThat(registro.getDescricao()).contains("Pessoa Exclusão Auditoria", "Presente");
            assertThat(registro.getEntidadeId()).isEqualTo(encontro.getId());
        });
    }
}
