package com.ibmec.remomeurumo.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusPresenca;

@SpringBootTest
@Transactional
class DashboardServiceTest {

    @Autowired private DashboardService dashboardService;
    @Autowired private ParticipanteService participanteService;
    @Autowired private AtividadeService atividadeService;
    @Autowired private EncontroService encontroService;
    @Autowired private PresencaService presencaService;

    @Test
    void deveAtualizarIndicadoresComNovosRegistros() {
        var antes = dashboardService.carregarIndicadores();
        Participante participante = participanteService.cadastrar(new Participante("Indicador Pessoa", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Indicador Atividade", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.now(), "");
        presencaService.registrarUnico(participante.getId(), encontro.getId(), StatusPresenca.PRESENTE, "");

        var depois = dashboardService.carregarIndicadores();

        assertThat(depois.participantesAtivos()).isEqualTo(antes.participantesAtivos() + 1);
        assertThat(depois.atividadesAtivas()).isEqualTo(antes.atividadesAtivas() + 1);
        assertThat(depois.encontrosNoMes()).isEqualTo(antes.encontrosNoMes() + 1);
        assertThat(depois.presencasNoMes()).isEqualTo(antes.presencasNoMes() + 1);
        assertThat(depois.totalRegistrosPresenca()).isEqualTo(antes.totalRegistrosPresenca() + 1);
        assertThat(dashboardService.carregarEncontrosRecentes())
                .extracting(item -> item.id()).contains(encontro.getId());
    }

    @Test
    void deveCalcularTodosOsIndicadoresPrincipais() {
        var antes = dashboardService.carregarIndicadores();
        Participante presente = participanteService.cadastrar(new Participante("Indicador Presente", ""));
        Participante ausente = participanteService.cadastrar(new Participante("Indicador Ausente", ""));
        Participante justificado = participanteService.cadastrar(new Participante("Indicador Justificado", ""));
        Atividade atividade = atividadeService.cadastrar(new Atividade("Indicadores Completos", ""));
        var encontro = encontroService.criar(atividade.getId(), LocalDate.now(), "");

        presencaService.registrarLote(encontro.getId(), Map.of(
                presente.getId(), StatusPresenca.PRESENTE,
                ausente.getId(), StatusPresenca.AUSENTE,
                justificado.getId(), StatusPresenca.JUSTIFICADA), Map.of());
        participanteService.desativar(justificado.getId());

        var depois = dashboardService.carregarIndicadores();
        assertThat(depois.participantesAtivos()).isEqualTo(antes.participantesAtivos() + 2);
        assertThat(depois.participantesInativos()).isEqualTo(antes.participantesInativos() + 1);
        assertThat(depois.atividadesAtivas()).isEqualTo(antes.atividadesAtivas() + 1);
        assertThat(depois.encontrosNoMes()).isEqualTo(antes.encontrosNoMes() + 1);
        assertThat(depois.presencasNoMes()).isEqualTo(antes.presencasNoMes() + 1);
        assertThat(depois.ausenciasNoMes()).isEqualTo(antes.ausenciasNoMes() + 1);
        assertThat(depois.justificadasNoMes()).isEqualTo(antes.justificadasNoMes() + 1);
        assertThat(depois.totalRegistrosPresenca()).isEqualTo(antes.totalRegistrosPresenca() + 3);
    }
}
