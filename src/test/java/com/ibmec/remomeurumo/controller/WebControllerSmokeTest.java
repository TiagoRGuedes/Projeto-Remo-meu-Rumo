package com.ibmec.remomeurumo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.dados-demonstracao=true")
class WebControllerSmokeTest {

    @LocalServerPort
    private int port;

    private ClienteLogado cliente;

    @BeforeEach
    void entrar() throws Exception {
        cliente = ClienteLogado.entrar(port);
    }

    @Test
    void deveAbrirRotasPrincipais() throws Exception {
        assertOk("/");
        assertOk("/participantes");
        assertOk("/atividades");
        assertOk("/encontros");
        assertRedirect("/presencas/registrar", "/chamada");
        assertOk("/presencas/registrar?completa=1");
        assertOk("/chamada?lista=1");
        assertOk("/frequencia");
        assertOk("/relatorios");
        assertOk("/historico");
        assertOk("/busca?q=Ana");
        assertOk("/ajuda");
        assertOk("/acessibilidade");
    }

    @Test
    void deveAbrirDetalhesEConfirmacoesSeguras() throws Exception {
        assertOkComTexto("/participantes/1", "Ana Souza");
        assertOkComTexto("/participantes/1/excluir", "não pode ser excluído");
        assertOkComTexto("/atividades/1", "SEG · 9h");
        assertOkComTexto("/atividades/1/excluir", "não pode ser excluída");
        assertOkComTexto("/encontros/3", "Aula");
        assertOkComTexto("/encontros/3/excluir", "deve ser cancelada");
        assertRedirect("/presencas/1/excluir", "/confirmacoes/");
    }

    @Test
    void deveConferirPresencaSemPersistirAlteracoes() throws Exception {
        HttpResponse<String> response = postForm(
                "/presencas/conferir",
                "encontroId=1&status_1=PRESENTE&observacoes_1=");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Revise a chamada", "Nada foi salvo", "Ana Souza", "Presente");
    }

    @Test
    void deveRetornarApiParticipantes() throws Exception {
        assertOk("/api/participantes");
    }

    @Test
    void deveExporTurmasMatriculasEFrequenciaMensalNaApi() throws Exception {
        assertOkComTexto("/api/turmas", "\"grade\"");
        assertOkComTexto("/api/turmas/1/matriculas", "\"participante\"");
        assertOkComTexto("/api/turmas/1/frequencia", "\"aulasPrevistas\"");
        assertOkComTexto("/api/turmas/1/faltosos", "[");
        assertOkComTexto("/api/matriculas", "\"status\"");
        assertOkComTexto("/api/participantes/1/frequencia", "\"consolidado\"");
        assertOkComTexto("/api/exportacao/frequencia-mensal", "\"record_id\"");
    }

    @Test
    void naoExpoeDadosSensiveisNaListagemNemNaExportacao() throws Exception {
        assertThat(get("/api/participantes").body()).doesNotContain("possuiDeficiencia", "condicao");
        assertThat(get("/api/exportacao/frequencia-mensal").body())
                .doesNotContain("deficiencia", "condicao", "nascimento");
    }

    @Test
    void deveRecusarMesInvalido() throws Exception {
        HttpResponse<String> response = get("/api/turmas/1/frequencia?mes=marco");

        assertThat(response.statusCode()).isEqualTo(400);
        assertThat(response.body()).contains("AAAA-MM");
    }

    @Test
    void deveMostrarMatriculasEFrequenciaMensalNasTelas() throws Exception {
        assertOkComTexto("/participantes/1", "Adicionar turma");
        assertOkComTexto("/participantes/1", "Frequência do mês");
        assertOkComTexto("/atividades/1", "Matrículas da turma");
        assertOkComTexto("/frequencia", "Presenças no ano");
        assertOkComTexto("/frequencia?turmaId=1", "Resumo por turma");
        assertOkComTexto("/frequencia?periodo=ano", "Ano " + java.time.LocalDate.now().getYear());
        assertOkComTexto("/frequencia/planilha.csv", "presencas_no_ano;previstas_no_ano");
        assertOkComTexto("/api/exportacao/frequencia-mensal", "\"presencas_ano\"");
        assertOkComTexto("/ajuda/instalar", "Adicionar à Tela de Início");
        assertOkComTexto("/manifest.json", "\"standalone\"");
        assertOk("/sw.js");
    }

    @Test
    void inicioMostraPrioridadesDoMes() throws Exception {
        HttpResponse<String> response = get("/");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains("Frequência do mês", "Precisam de contato", "Chamadas pendentes",
                "Frequência por turma", "Fazer chamada");
        assertThat(response.body()).doesNotContain("Centro de operações", "Pulso da operação");
    }

    @Test
    void chamadaDeUmaTurmaAbreComMatriculados() throws Exception {
        HttpResponse<String> response = get("/chamada/1?data=" + java.time.LocalDate.now().minusDays(1));

        assertThat(response.statusCode()).isIn(200, 302);
        HttpResponse<String> lista = get("/chamada?lista=1");
        assertThat(lista.body()).contains("Aulas anteriores sem chamada", "Outra data");
    }

    @Test
    void deveRenderizarPaginaNaoEncontrada() throws Exception {
        HttpResponse<String> response = get("/pagina-inexistente");

        assertThat(response.statusCode()).isEqualTo(404);
        assertThat(response.body()).contains("Página não encontrada");
    }

    private void assertOk(String path) throws Exception {
        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(200);
    }

    private void assertOkComTexto(String path, String texto) throws Exception {
        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).contains(texto);
    }

    private void assertRedirect(String path, String destino) throws Exception {
        HttpResponse<String> response = get(path);

        assertThat(response.statusCode()).isEqualTo(302);
        assertThat(response.headers().firstValue("location").orElseThrow()).contains(destino);
    }

    private HttpResponse<String> get(String path) throws Exception {
        return cliente.get(path);
    }

    private HttpResponse<String> postForm(String path, String body) throws Exception {
        return cliente.postForm(path, body);
    }

    @Test
    void rotaProtegidaExigeLogin() throws Exception {
        HttpResponse<String> semLogin = HttpClient.newHttpClient().send(
                HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/participantes")).GET().build(),
                HttpResponse.BodyHandlers.ofString());

        assertThat(semLogin.statusCode()).isEqualTo(302);
        assertThat(semLogin.headers().firstValue("location").orElseThrow()).endsWith("/login");
        assertThat(get("/participantes").statusCode()).isEqualTo(200);
    }
}
