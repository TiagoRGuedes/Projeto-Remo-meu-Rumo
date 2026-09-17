package com.ibmec.remomeurumo.controller;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.ibmec.remomeurumo.config.DataLoader;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.HistoricoOperacaoRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.dados-demonstracao=true")
class ConfirmacaoOperacaoWebIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ParticipanteRepository participanteRepository;

    @Autowired
    private HistoricoOperacaoRepository historicoRepository;

    @Autowired
    private AtividadeRepository atividadeRepository;

    @Autowired
    private EncontroRepository encontroRepository;

    @Autowired
    private PresencaRepository presencaRepository;

    @Autowired
    private DataLoader dataLoader;

    private HttpClient client;

    @BeforeEach
    void configurarClienteComSessao() {
        CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);
        client = HttpClient.newBuilder()
                .cookieHandler(cookies)
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    @Test
    void deveRevisarSemPersistirEConfirmarUmaUnicaVez() throws Exception {
        String nome = "Fluxo Confirmacao " + System.nanoTime();
        long participantesAntes = participanteRepository.count();
        long historicoAntes = historicoRepository.count();

        HttpResponse<String> preparo = postForm(
                "/participantes/novo",
                "nome=" + codificar(nome) + "&observacoes=Teste+de+confirmacao");

        assertThat(preparo.statusCode()).isEqualTo(302);
        String paginaConfirmacao = caminhoRedirecionamento(preparo);
        assertThat(paginaConfirmacao).startsWith("/confirmacoes/");
        assertThat(participanteRepository.count()).isEqualTo(participantesAntes);
        assertThat(historicoRepository.count()).isEqualTo(historicoAntes);

        HttpResponse<String> revisao = get(paginaConfirmacao);

        assertThat(revisao.statusCode()).isEqualTo(200);
        assertThat(revisao.body()).contains("Criar participante?", nome, "A revisão não alterou");
        assertThat(participanteRepository.count()).isEqualTo(participantesAntes);
        assertThat(historicoRepository.count()).isEqualTo(historicoAntes);

        HttpResponse<String> confirmacao = postForm(paginaConfirmacao, "");

        assertThat(confirmacao.statusCode()).isEqualTo(302);
        assertThat(caminhoRedirecionamento(confirmacao)).startsWith("/participantes/");
        assertThat(participanteRepository.count()).isEqualTo(participantesAntes + 1);
        assertThat(historicoRepository.count()).isEqualTo(historicoAntes + 1);
        assertThat(participanteRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome)).singleElement();

        HttpResponse<String> busca = get("/busca?q=" + codificar(nome));
        assertThat(busca.statusCode()).isEqualTo(200);
        assertThat(busca.body()).contains(nome, "Participantes");

        HttpResponse<String> repeticao = postForm(paginaConfirmacao, "");

        assertThat(repeticao.statusCode()).isEqualTo(302);
        assertThat(participanteRepository.count()).isEqualTo(participantesAntes + 1);
        assertThat(historicoRepository.count()).isEqualTo(historicoAntes + 1);
    }

    @Test
    void deveCancelarRevisaoSemAlterarDadosOuHistorico() throws Exception {
        long participantesAntes = participanteRepository.count();
        long historicoAntes = historicoRepository.count();
        String nome = "Operacao Cancelada " + System.nanoTime();

        HttpResponse<String> preparo = postForm(
                "/participantes/novo",
                "nome=" + codificar(nome) + "&observacoes=Nao+persistir");
        String paginaConfirmacao = caminhoRedirecionamento(preparo);

        HttpResponse<String> cancelamento = postForm(paginaConfirmacao + "/cancelar", "");

        assertThat(cancelamento.statusCode()).isEqualTo(302);
        assertThat(participanteRepository.count()).isEqualTo(participantesAntes);
        assertThat(historicoRepository.count()).isEqualTo(historicoAntes);
        assertThat(participanteRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(nome)).isEmpty();
    }

    @Test
    void deveCarregarCenarioDemonstrativoUmaUnicaVez() throws Exception {
        long participantes = participanteRepository.count();
        long atividades = atividadeRepository.count();
        long encontros = encontroRepository.count();
        long presencas = presencaRepository.count();
        long historico = historicoRepository.count();

        assertThat(participantes).isGreaterThanOrEqualTo(36);
        assertThat(atividades).isGreaterThanOrEqualTo(6);
        assertThat(encontros).isGreaterThanOrEqualTo(24);
        assertThat(presencas).isGreaterThan(0);

        dataLoader.run();

        assertThat(participanteRepository.count()).isEqualTo(participantes);
        assertThat(atividadeRepository.count()).isEqualTo(atividades);
        assertThat(encontroRepository.count()).isEqualTo(encontros);
        assertThat(presencaRepository.count()).isEqualTo(presencas);
        assertThat(historicoRepository.count()).isEqualTo(historico);
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path)).GET().build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        URI valor = URI.create(path);
        return valor.isAbsolute() ? valor : URI.create("http://localhost:" + port + path);
    }

    private String codificar(String valor) {
        return URLEncoder.encode(valor, UTF_8);
    }

    private String caminhoRedirecionamento(HttpResponse<?> response) {
        URI destino = URI.create(response.headers().firstValue("location").orElseThrow());
        String caminho = destino.getRawPath();
        int inicioSessao = caminho.indexOf(";jsessionid=");
        return inicioSessao < 0 ? caminho : caminho.substring(0, inicioSessao);
    }
}
