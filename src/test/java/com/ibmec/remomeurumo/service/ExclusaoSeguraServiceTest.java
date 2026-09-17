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
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;

@SpringBootTest
@Transactional
class ExclusaoSeguraServiceTest {

    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private EncontroService encontroService;
    @Autowired private PresencaService presencaService;
    @Autowired private ParticipanteRepository participanteRepository;
    @Autowired private AtividadeRepository atividadeRepository;
    @Autowired private EncontroRepository encontroRepository;

    @Test
    void participanteSemDependenciasPodeSerExcluido() {
        Participante participante = participanteService.cadastrar(new Participante("Sem Presença", ""));

        participanteService.excluir(participante.getId());

        assertThat(participanteRepository.findById(participante.getId())).isEmpty();
    }

    @Test
    void participanteComPresencaNaoPodeSerApagado() {
        Participante participante = participanteService.cadastrar(new Participante("Com Presença", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Dependência Participante", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.of(2036, 1, 10), "");
        presencaService.registrarUnico(participante.getId(), encontro.getId(), StatusPresenca.PRESENTE, "");

        assertThatThrownBy(() -> participanteService.excluir(participante.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("histórico de presença");
        assertThat(participanteRepository.findById(participante.getId())).isPresent();
    }

    @Test
    void atividadeSemEncontrosPodeSerExcluida() {
        Atividade atividade = atividadeService.cadastrar(new Atividade("Sem Encontros", ""));

        atividadeService.excluir(atividade.getId());

        assertThat(atividadeRepository.findById(atividade.getId())).isEmpty();
    }

    @Test
    void atividadeComEncontroNaoPerdeHistorico() {
        Atividade atividade = atividadeService.cadastrar(new Atividade("Com Encontro", ""));
        encontroService.criar(atividade.getId(), LocalDate.of(2036, 2, 10), "");

        assertThatThrownBy(() -> atividadeService.excluir(atividade.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("possui encontros");
        assertThat(atividadeRepository.findById(atividade.getId())).isPresent();
    }

    @Test
    void encontroComPresencaDeveSerCanceladoEmVezDeRemovido() {
        Participante participante = participanteService.cadastrar(new Participante("Pessoa Encontro", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Encontro Protegido", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.of(2036, 3, 10), "");
        presencaService.registrarUnico(participante.getId(), encontro.getId(), StatusPresenca.PRESENTE, "");

        assertThatThrownBy(() -> encontroService.excluir(encontro.getId()))
                .isInstanceOf(RegraNegocioException.class)
                .hasMessageContaining("possui registros de presença");

        encontroService.cancelar(encontro.getId());
        assertThat(encontroRepository.findById(encontro.getId())).get()
                .extracting(item -> item.getStatus()).isEqualTo(StatusEncontro.CANCELADO);
    }

    @Test
    void encontroSemPresencaPodeSerExcluido() {
        Atividade atividade = atividadeService.cadastrar(new Atividade("Encontro Excluível", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.of(2036, 4, 10), "");

        encontroService.excluir(encontro.getId());

        assertThat(encontroRepository.findById(encontro.getId())).isEmpty();
    }
}
