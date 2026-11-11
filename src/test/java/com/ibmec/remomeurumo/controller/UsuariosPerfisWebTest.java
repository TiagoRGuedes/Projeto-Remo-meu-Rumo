package com.ibmec.remomeurumo.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.model.Usuario;
import com.ibmec.remomeurumo.repository.UsuarioRepository;
import com.ibmec.remomeurumo.service.UsuarioService;

@SpringBootTest(
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
        properties = "app.dados-demonstracao=true")
class UsuariosPerfisWebTest {

    @LocalServerPort
    private int port;
    @Autowired
    private UsuarioService usuarioService;
    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void loginComCsrfVencidoVoltaParaOLoginComAviso() throws Exception {
        var resposta = java.net.http.HttpClient.newBuilder().followRedirects(java.net.http.HttpClient.Redirect.NEVER).build()
                .send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/login"))
                        .header("Content-Type", "application/x-www-form-urlencoded")
                        .POST(java.net.http.HttpRequest.BodyPublishers.ofString("username=a&password=b&_csrf=token-velho"))
                        .build(), java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(resposta.statusCode()).isEqualTo(302);
        assertThat(resposta.headers().firstValue("location").orElse("")).endsWith("/login?expirou");
    }

    @Test
    void desativarOuTrocarSenhaDerrubaSessaoAberta() throws Exception {
        Usuario u = usuarioService.cadastrar("Prof Sessao", "prof.sessao", "senha-longa-123", Perfil.PROFESSOR);
        ClienteLogado a = ClienteLogado.entrar(port, "prof.sessao", "senha-longa-123");
        ClienteLogado b = ClienteLogado.entrar(port, "prof.sessao", "senha-longa-123");
        assertThat(a.get("/chamada?lista=1").statusCode()).isEqualTo(200);

        usuarioService.redefinirSenha(u.getId(), "outra-senha-456");
        assertThat(a.get("/chamada?lista=1").statusCode()).isEqualTo(302);

        usuarioService.definirAtivo(u.getId(), false, "admin-teste");
        assertThat(b.get("/chamada?lista=1").statusCode()).isEqualTo(302);
    }

    @Test
    void limiteDeLoginBloqueiaOUsuarioNaoOIpNemObedeceXForwardedFor() throws Exception {
        var http = java.net.http.HttpClient.newBuilder().followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .cookieHandler(new java.net.CookieManager()).build();
        for (int i = 1; i <= 6; i++) { // cada tentativa diz vir de um IP diferente: não pode adiantar
            assertThat(tentarLogin(http, "alvo.bloqueio", "errada", "9.9.9." + i)).contains("/login?");
        }
        assertThat(tentarLogin(http, "alvo.bloqueio", "errada", "9.9.9.99")).endsWith("/login?bloqueado");
        usuarioService.cadastrar("Outro Prof", "outro.prof", "Rumo-teste-91", Perfil.PROFESSOR);
        assertThat(tentarLogin(http, "outro.prof", "Rumo-teste-91", null)).doesNotContain("bloqueado").doesNotContain("erro");
    }

    @Test
    void sessaoVencidaAoSalvarChamadaVoltaParaATurmaDepoisDoLogin() throws Exception {
        var http = java.net.http.HttpClient.newBuilder().followRedirects(java.net.http.HttpClient.Redirect.NEVER)
                .cookieHandler(new java.net.CookieManager()).build();
        var salvar = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/chamada/7"))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(java.net.http.HttpRequest.BodyPublishers.ofString("data=2026-11-10&_csrf=velho")).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());
        assertThat(salvar.headers().firstValue("location").orElse("")).endsWith("/login?expirou&chamada");

        assertThat(tentarLogin(http, "admin-teste", "senha-teste", null)).endsWith("/chamada/7?data=2026-11-10&corrigir=1");
    }

    private String tentarLogin(java.net.http.HttpClient http, String usuario, String senha, String xff) throws Exception {
        String base = "http://localhost:" + port;
        String html = http.send(java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/login")).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString()).body();
        var m = java.util.regex.Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"").matcher(html);
        m.find();
        var b = java.net.http.HttpRequest.newBuilder(java.net.URI.create(base + "/login"))
                .header("Content-Type", "application/x-www-form-urlencoded");
        if (xff != null) {
            b.header("X-Forwarded-For", xff);
        }
        return http.send(b.POST(java.net.http.HttpRequest.BodyPublishers.ofString(
                "username=" + usuario + "&password=" + senha + "&_csrf=" + m.group(1))).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString()).headers().firstValue("location").orElse("");
    }

    @Autowired
    private com.ibmec.remomeurumo.service.HistoricoOperacaoService historicoService;

    @Test
    void historicoGravaQuemFezAAlteracao() throws Exception {
        ClienteLogado.entrar(port).postForm("/usuarios", "nome=Quem+Fez&login=quem.fez&senha=Rumo-teste-91&perfil=PROFESSOR");

        assertThat(historicoService.listar(null, null, com.ibmec.remomeurumo.model.ModuloHistorico.USUARIOS, null, "quem.fez"))
                .isNotEmpty().allSatisfy(h -> assertThat(h.getUsuario()).isEqualTo("admin-teste"));
    }

    @Test
    void respostaTemCspEReferrerPolicy() throws Exception {
        var r = java.net.http.HttpClient.newHttpClient().send(
                java.net.http.HttpRequest.newBuilder(java.net.URI.create("http://localhost:" + port + "/login")).build(),
                java.net.http.HttpResponse.BodyHandlers.ofString());

        assertThat(r.headers().firstValue("Content-Security-Policy").orElse(""))
                .contains("script-src 'self'", "object-src 'none'", "frame-ancestors 'none'");
        assertThat(r.headers().firstValue("Referrer-Policy")).contains("same-origin");
        assertThat(r.body()).doesNotContain("<script>");
    }

    @Test
    void idInvalidoViraNaoEncontradoEmVezDeErro500() throws Exception {
        assertThat(ClienteLogado.entrar(port).getPagina("/participantes/abc").statusCode()).isEqualTo(404);
    }

    @Test
    void professorAcessaSoAChamada() throws Exception {
        usuarioService.cadastrar("Prof Teste", "prof.chamada", "Rumo-teste-91", Perfil.PROFESSOR);
        ClienteLogado professor = ClienteLogado.entrar(port, "prof.chamada", "Rumo-teste-91");

        assertThat(professor.get("/chamada?lista=1").statusCode()).isEqualTo(200);
        assertThat(professor.get("/chamada?lista=1").body()).doesNotContain("href=\"/participantes\"", "/usuarios");
        for (String rota : new String[] {"/", "/participantes", "/atividades", "/frequencia", "/relatorios",
                "/historico", "/usuarios", "/api/participantes", "/presencas/registrar?completa=1", "/busca?q=Ana"}) {
            HttpResponse<String> r = professor.get(rota);
            assertThat(r.statusCode()).as(rota).isEqualTo(403);
        }
        assertThat(professor.getPagina("/usuarios").body()).contains("não tem acesso");
        assertThat(professor.postForm("/usuarios", "nome=X&login=xxx&senha=Rumo-teste-91&perfil=COORDENACAO")
                .statusCode()).isEqualTo(403);
    }

    @Test
    void coordenacaoCadastraUsuarioQueLogaEDesativadoNaoLoga() throws Exception {
        ClienteLogado coordenacao = ClienteLogado.entrar(port);
        assertThat(coordenacao.get("/usuarios").statusCode()).isEqualTo(200);

        HttpResponse<String> criado = coordenacao.postForm("/usuarios",
                "nome=Maria+Silva&login=Maria.Prof&senha=Rumo-teste-91&perfil=PROFESSOR");
        assertThat(criado.statusCode()).isEqualTo(302);

        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue("maria.prof").orElseThrow();
        assertThat(usuario.getSenhaHash()).startsWith("{bcrypt}").doesNotContain("Rumo-teste-91");
        ClienteLogado.entrar(port, "maria.prof", "Rumo-teste-91");
        assertThat(coordenacao.get("/usuarios").body()).contains("Maria Silva");

        coordenacao.postForm("/usuarios/" + usuario.getId() + "/desativar", "");
        assertThatThrownBy(() -> ClienteLogado.entrar(port, "maria.prof", "Rumo-teste-91"))
                .isInstanceOf(IllegalStateException.class);

        coordenacao.postForm("/usuarios/" + usuario.getId() + "/ativar", "");
        coordenacao.postForm("/usuarios/" + usuario.getId() + "/senha", "senha=outrasenha99");
        ClienteLogado.entrar(port, "maria.prof", "outrasenha99");
    }

    @Test
    void validaCadastroEProtegeContaPropriaEChaveMestra() {
        assertThatThrownBy(() -> usuarioService.cadastrar("A", "curta", "123456789", Perfil.PROFESSOR))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("mínimo 10");
        for (String fraca : new String[] {"1234567890", "aaaaaaaaaaaa", "Senha12345", "RemoMeuRumo"}) {
            assertThatThrownBy(() -> usuarioService.cadastrar("A", "fraca", fraca, Perfil.PROFESSOR))
                    .as(fraca).isInstanceOf(RegraNegocioException.class).hasMessageContaining("fraca");
        }
        assertThatThrownBy(() -> usuarioService.cadastrar("A", "admin-teste", "Rumo-teste-91", Perfil.PROFESSOR))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
        Usuario u = usuarioService.cadastrar("Coord Dois", "coord.dois", "Rumo-teste-91", Perfil.COORDENACAO);
        assertThatThrownBy(() -> usuarioService.cadastrar("Outro", "COORD.DOIS", "Rumo-teste-91", Perfil.PROFESSOR))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
        assertThatThrownBy(() -> usuarioService.definirAtivo(u.getId(), false, "coord.dois"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("próprio");
    }
}
