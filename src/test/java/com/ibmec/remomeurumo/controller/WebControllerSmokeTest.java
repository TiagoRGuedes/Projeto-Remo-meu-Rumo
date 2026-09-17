package com.ibmec.remomeurumo.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.dados-demonstracao=true")
class WebControllerSmokeTest {

    @LocalServerPort
    private int port;

    @Test
    void deveAbrirRotasPrincipais() throws Exception {
        assertOk("/");
        assertOk("/participantes");
        assertOk("/atividades");
        assertOk("/encontros");
        assertOk("/presencas/registrar");
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
        assertOkComTexto("/atividades/1", "Treino demonstrativo de percurso");
        assertOkComTexto("/atividades/1/excluir", "não pode ser excluída");
        assertOkComTexto("/encontros/3", "Encontro");
        assertOkComTexto("/encontros/3/excluir", "deve ser cancelado");
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
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .GET()
                .build();
        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postForm(String path, String body) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://localhost:" + port + path))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body))
                .build();
        return HttpClient.newHttpClient()
                .send(request, HttpResponse.BodyHandlers.ofString());
    }
}
