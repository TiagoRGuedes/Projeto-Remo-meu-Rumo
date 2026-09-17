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
import com.ibmec.remomeurumo.repository.HistoricoOperacaoRepository;

@SpringBootTest
@Transactional
class ConectividadeModulosIntegrationTest {

    @Autowired
    private ParticipanteService participanteService;

    @Autowired
    private AtividadeService atividadeService;

    @Autowired
    private EncontroService encontroService;

    @Autowired
    private PresencaService presencaService;

    @Autowired
    private FrequenciaService frequenciaService;

    @Autowired
    private DashboardService dashboardService;

    @Autowired
    private BuscaGlobalService buscaGlobalService;

    @Autowired
    private HistoricoOperacaoService historicoService;

    @Autowired
    private HistoricoOperacaoRepository historicoRepository;

    @Test
    void devePropagarOperacaoEntreBuscaFrequenciaDashboardEHistorico() {
        String referencia = "Conexao" + System.nanoTime();
        long historicoInicial = historicoRepository.count();
        long presencasIniciais = dashboardService.carregarIndicadores().totalRegistrosPresenca();

        Participante primeiro = participanteService.cadastrar(
                new Participante(referencia + " Pessoa A", "Registro de integração"));
        Participante segundo = participanteService.cadastrar(
                new Participante(referencia + " Pessoa B", "Registro de integração"));
        Atividade atividade = atividadeService.cadastrar(
                new Atividade(referencia + " Atividade", "Atividade conectada"));
        var encontro = encontroService.criar(
                atividade.getId(), LocalDate.now().minusDays(1), "Encontro conectado", StatusEncontro.REALIZADO);

        assertThat(historicoRepository.count()).isEqualTo(historicoInicial + 4);

        presencaService.registrarLote(
                encontro.getId(), Map.of(primeiro.getId(), StatusPresenca.PRESENTE), Map.of());
        long antesDaOperacaoMista = historicoRepository.count();

        var resultado = presencaService.registrarLote(
                encontro.getId(),
                Map.of(
                        primeiro.getId(), StatusPresenca.AUSENTE,
                        segundo.getId(), StatusPresenca.PRESENTE),
                Map.of());

        assertThat(resultado.novosRegistros()).isEqualTo(1);
        assertThat(resultado.correcoes()).isEqualTo(1);
        assertThat(historicoRepository.count()).isEqualTo(antesDaOperacaoMista + 1);

        var busca = buscaGlobalService.buscar(referencia);
        assertThat(busca.participantes()).extracting(Participante::getId)
                .contains(primeiro.getId(), segundo.getId());
        assertThat(busca.atividades()).extracting(Atividade::getId).contains(atividade.getId());
        assertThat(busca.encontros()).extracting(item -> item.getId()).contains(encontro.getId());

        var historicoPrimeiro = presencaService.listarHistorico(primeiro.getId(), null, null, null, null);
        var frequenciaPrimeiro = frequenciaService.resumir(primeiro.getNome(), historicoPrimeiro);
        assertThat(frequenciaPrimeiro.totalEncontros()).isEqualTo(1);
        assertThat(frequenciaPrimeiro.ausentes()).isEqualTo(1);

        var indicadores = dashboardService.carregarIndicadores();
        assertThat(indicadores.totalRegistrosPresenca()).isEqualTo(presencasIniciais + 2);
        assertThat(dashboardService.carregarUltimoEncontro().id()).isEqualTo(encontro.getId());

        long antesDaEdicao = historicoRepository.count();
        encontroService.atualizar(encontro.getId(), atividade.getId(), encontro.getData(),
                "Encontro conectado revisado", StatusEncontro.REALIZADO);
        assertThat(historicoRepository.count()).isEqualTo(antesDaEdicao + 1);
        assertThat(historicoService.listar(null, null, ModuloHistorico.ENCONTROS, AcaoHistorico.EDICAO, referencia))
                .singleElement()
                .satisfies(evento -> {
                    assertThat(evento.isTemComparacao()).isTrue();
                    assertThat(evento.getUrlRelacionada()).isEqualTo("/encontros/" + encontro.getId());
                });

        participanteService.desativar(primeiro.getId());

        assertThat(presencaService.prepararItens(encontro.getId(), primeiro.getNome(), false)).isEmpty();
        assertThat(presencaService.listarHistorico(primeiro.getId(), null, null, null, null)).hasSize(1);
        assertThat(buscaGlobalService.buscar(referencia).participantes())
                .filteredOn(item -> item.getId().equals(primeiro.getId()))
                .singleElement()
                .satisfies(item -> assertThat(item.isAtivo()).isFalse());
    }
}
