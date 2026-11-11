package com.ibmec.remomeurumo.controller;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Cliente HTTP de teste que entra pelo formulário de login e leva o token CSRF nos POST. */
final class ClienteLogado {

    private static final Pattern CSRF = Pattern.compile("name=\"_csrf\"[^>]*value=\"([^\"]+)\"");

    final HttpClient client = HttpClient.newBuilder()
            .cookieHandler(new CookieManager(null, CookiePolicy.ACCEPT_ALL))
            .followRedirects(HttpClient.Redirect.NEVER)
            .build();
    private final String base;
    private String csrf;

    private ClienteLogado(int port) {
        this.base = "http://localhost:" + port;
    }

    static ClienteLogado entrar(int port) throws Exception {
        return entrar(port, "admin-teste", "senha-teste");
    }

    static ClienteLogado entrar(int port, String usuario, String senha) throws Exception {
        ClienteLogado c = new ClienteLogado(port);
        c.csrf = c.extrairCsrf(c.get("/login").body());
        HttpResponse<String> resposta = c.enviarForm("/login",
                "username=" + URLEncoder.encode(usuario, UTF_8) + "&password=" + URLEncoder.encode(senha, UTF_8));
        if (resposta.statusCode() != 302 || resposta.headers().firstValue("location").orElse("").contains("erro")) {
            throw new IllegalStateException("Login de teste falhou: " + resposta.statusCode() + " " + resposta.headers().firstValue("location").orElse(""));
        }
        c.csrf = c.extrairCsrf(c.get("/chamada?lista=1").body()); // a home é só da coordenação
        return c;
    }

    HttpResponse<String> get(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    /** Como o navegador: pede HTML, então o erro vem na página do site e não em JSON. */
    HttpResponse<String> getPagina(String path) throws Exception {
        return client.send(HttpRequest.newBuilder(URI.create(base + path)).header("Accept", "text/html").GET().build(),
                HttpResponse.BodyHandlers.ofString());
    }

    HttpResponse<String> postForm(String path, String body) throws Exception {
        return enviarForm(path, body);
    }

    private HttpResponse<String> enviarForm(String path, String body) throws Exception {
        URI uri = URI.create(path).isAbsolute() ? URI.create(path) : URI.create(base + path);
        HttpRequest request = HttpRequest.newBuilder(uri)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(body + "&_csrf=" + URLEncoder.encode(csrf, UTF_8)))
                .build();
        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private String extrairCsrf(String html) {
        Matcher m = CSRF.matcher(html);
        if (!m.find()) {
            throw new IllegalStateException("Token CSRF não encontrado na página");
        }
        return m.group(1);
    }
}
