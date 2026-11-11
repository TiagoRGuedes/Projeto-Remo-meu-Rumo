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
    void professorAcessaSoAChamada() throws Exception {
        usuarioService.cadastrar("Prof Teste", "prof.chamada", "senha12345", Perfil.PROFESSOR);
        ClienteLogado professor = ClienteLogado.entrar(port, "prof.chamada", "senha12345");

        assertThat(professor.get("/chamada?lista=1").statusCode()).isEqualTo(200);
        assertThat(professor.get("/chamada?lista=1").body()).doesNotContain("href=\"/participantes\"", "/usuarios");
        for (String rota : new String[] {"/", "/participantes", "/atividades", "/frequencia", "/relatorios",
                "/historico", "/usuarios", "/api/participantes", "/presencas/registrar?completa=1", "/busca?q=Ana"}) {
            HttpResponse<String> r = professor.get(rota);
            assertThat(r.statusCode()).as(rota).isEqualTo(403);
        }
        assertThat(professor.getPagina("/usuarios").body()).contains("não tem acesso");
        assertThat(professor.postForm("/usuarios", "nome=X&login=xxx&senha=senha12345&perfil=COORDENACAO")
                .statusCode()).isEqualTo(403);
    }

    @Test
    void coordenacaoCadastraUsuarioQueLogaEDesativadoNaoLoga() throws Exception {
        ClienteLogado coordenacao = ClienteLogado.entrar(port);
        assertThat(coordenacao.get("/usuarios").statusCode()).isEqualTo(200);

        HttpResponse<String> criado = coordenacao.postForm("/usuarios",
                "nome=Maria+Silva&login=Maria.Prof&senha=senha12345&perfil=PROFESSOR");
        assertThat(criado.statusCode()).isEqualTo(302);

        Usuario usuario = usuarioRepository.findByLoginAndAtivoTrue("maria.prof").orElseThrow();
        assertThat(usuario.getSenhaHash()).startsWith("{bcrypt}").doesNotContain("senha12345");
        ClienteLogado.entrar(port, "maria.prof", "senha12345");
        assertThat(coordenacao.get("/usuarios").body()).contains("Maria Silva");

        coordenacao.postForm("/usuarios/" + usuario.getId() + "/desativar", "");
        assertThatThrownBy(() -> ClienteLogado.entrar(port, "maria.prof", "senha12345"))
                .isInstanceOf(IllegalStateException.class);

        coordenacao.postForm("/usuarios/" + usuario.getId() + "/ativar", "");
        coordenacao.postForm("/usuarios/" + usuario.getId() + "/senha", "senha=outrasenha99");
        ClienteLogado.entrar(port, "maria.prof", "outrasenha99");
    }

    @Test
    void validaCadastroEProtegeContaPropriaEChaveMestra() {
        assertThatThrownBy(() -> usuarioService.cadastrar("A", "curta", "1234567", Perfil.PROFESSOR))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("mínimo 8");
        assertThatThrownBy(() -> usuarioService.cadastrar("A", "admin-teste", "senha12345", Perfil.PROFESSOR))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
        Usuario u = usuarioService.cadastrar("Coord Dois", "coord.dois", "senha12345", Perfil.COORDENACAO);
        assertThatThrownBy(() -> usuarioService.cadastrar("Outro", "COORD.DOIS", "senha12345", Perfil.PROFESSOR))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("Já existe");
        assertThatThrownBy(() -> usuarioService.definirAtivo(u.getId(), false, "coord.dois"))
                .isInstanceOf(RegraNegocioException.class).hasMessageContaining("próprio");
    }
}
