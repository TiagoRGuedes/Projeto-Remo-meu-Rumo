package com.ibmec.remomeurumo;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.time.Duration;
import java.util.Arrays;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.sun.net.httpserver.HttpServer;

@SpringBootApplication
public class RemoMeuRumoApplication {

    private static final Logger LOG = LoggerFactory.getLogger(RemoMeuRumoApplication.class);

    /** Código de erro do H2 para "Database may be already in use". */
    static final int H2_BANCO_EM_USO = 90020;

    static final int MAX_TENTATIVAS = 4;

    static final Duration ESPERA_ENTRE_TENTATIVAS = Duration.ofSeconds(10);

    static final Path BANCO = Path.of("dados", "remo-meu-rumo.mv.db");

    static final Path BANCO_RECUPERADO = Path.of("dados", "remo-meu-rumo-recuperado.mv.db");

    static final String URL_RECUPERADO = "jdbc:h2:file:./dados/remo-meu-rumo-recuperado;AUTO_SERVER=TRUE;DB_CLOSE_DELAY=-1";

    private static volatile boolean iniciado;

    private static volatile String etapa = "iniciando";

    private static HttpServer paginaDiagnostico;

    public static void main(String[] args) throws InterruptedException {
        Thread principal = Thread.currentThread();
        int porta = porta(args);
        Thread vigia = new Thread(() -> vigiarInicio(principal, porta), "vigia-inicializacao");
        vigia.setDaemon(true);
        vigia.start();
        try {
            iniciar(args, porta);
        } finally {
            etapa = iniciado ? "iniciado" : etapa;
        }
    }

    private static void iniciar(String[] args, int porta) throws InterruptedException {
        boolean bancoPrincipal = usaBancoPrincipal(args);
        if (bancoPrincipal && Files.exists(BANCO_RECUPERADO)) {
            LOG.warn("Usando o banco recuperado {}.", BANCO_RECUPERADO);
            System.setProperty("spring.datasource.url", URL_RECUPERADO);
        }

        // Num reinício do Azure, a versão nova pode subir enquanto a antiga ainda segura o arquivo do banco H2.
        RuntimeException falha = null;
        for (int tentativa = 1; tentativa <= MAX_TENTATIVAS; tentativa++) {
            etapa = "tentativa " + tentativa + " de abrir o banco e subir o Spring";
            try {
                SpringApplication.run(RemoMeuRumoApplication.class, args);
                iniciado = true;
                return;
            } catch (RuntimeException erro) {
                falha = erro;
                if (!bancoEmUso(erro)) {
                    break;
                }
                if (tentativa < MAX_TENTATIVAS) {
                    LOG.warn("Banco de dados em uso por outro processo (tentativa {} de {}). Nova tentativa em {} s.",
                            tentativa, MAX_TENTATIVAS, ESPERA_ENTRE_TENTATIVAS.toSeconds());
                    Thread.sleep(ESPERA_ENTRE_TENTATIVAS);
                }
            }
        }

        // A trava não saiu: abre uma cópia do arquivo, que não carrega a trava. O arquivo original fica intacto.
        if (bancoPrincipal && bancoEmUso(falha) && !Files.exists(BANCO_RECUPERADO)) {
            LOG.error("O banco continua travado. Abrindo uma cópia em {}.", BANCO_RECUPERADO);
            etapa = "copiando o banco travado";
            copiarBanco();
            System.setProperty("spring.datasource.url", URL_RECUPERADO);
            etapa = "subindo o Spring com a cópia do banco";
            try {
                SpringApplication.run(RemoMeuRumoApplication.class, args);
                iniciado = true;
                return;
            } catch (RuntimeException erro) {
                falha = erro;
            }
        }

        LOG.error("O sistema não conseguiu iniciar.", falha);
        servirPaginaDeFalha(porta, resumoDaFalha(falha));
    }

    /** Se o início demorar demais, mostra onde ele está parado, em vez de deixar o Azure esperando. */
    private static void vigiarInicio(Thread principal, int porta) {
        try {
            Thread.sleep(Duration.ofSeconds(170));
        } catch (InterruptedException erro) {
            Thread.currentThread().interrupt();
            return;
        }
        if (iniciado || !principal.isAlive()) {
            return;
        }
        StringBuilder texto = new StringBuilder("O sistema ainda não terminou de iniciar após 170 s.\n")
                .append("Etapa: ").append(etapa).append("\n\nOnde a inicialização está parada:\n");
        StackTraceElement[] pilha = principal.getStackTrace();
        for (int i = 0; i < Math.min(pilha.length, 60); i++) {
            texto.append("  at ").append(pilha[i]).append('\n');
        }
        LOG.error(texto.toString());
        servirPaginaDeFalha(porta, texto.toString());
    }

    static boolean bancoEmUso(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof SQLException sql && sql.getErrorCode() == H2_BANCO_EM_USO) {
                return true;
            }
            String mensagem = atual.getMessage();
            if (mensagem != null && (mensagem.contains("Database may be already in use")
                    || mensagem.contains("The file is locked"))) {
                return true;
            }
            if (atual.getCause() == atual) {
                break;
            }
        }
        return false;
    }

    static boolean usaBancoPrincipal(String[] args) {
        String perfis = System.getenv().getOrDefault("SPRING_PROFILES_ACTIVE", "")
                + "," + System.getProperty("spring.profiles.active", "");
        boolean argumentoDemo = Arrays.stream(args)
                .anyMatch(a -> a.startsWith("--spring.profiles.active=") && a.contains("demo"));
        boolean urlPropria = System.getProperty("spring.datasource.url") != null
                || System.getenv("SPRING_DATASOURCE_URL") != null
                || Arrays.stream(args).anyMatch(a -> a.startsWith("--spring.datasource.url="));
        return !perfis.contains("demo") && !argumentoDemo && !urlPropria;
    }

    static String resumoDaFalha(Throwable falha) {
        StringBuilder texto = new StringBuilder("O sistema não conseguiu iniciar.\n\nCausas, da mais externa à mais interna:\n");
        int nivel = 0;
        for (Throwable atual = falha; atual != null && nivel < 15; atual = atual.getCause(), nivel++) {
            String mensagem = String.valueOf(atual.getMessage());
            if (mensagem.length() > 400) {
                mensagem = mensagem.substring(0, 400) + "...";
            }
            texto.append("- ").append(atual.getClass().getName()).append(": ").append(mensagem).append('\n');
            if (atual.getCause() == atual) {
                break;
            }
        }
        return texto.toString();
    }

    private static void copiarBanco() {
        Path temporario = BANCO_RECUPERADO.resolveSibling("remo-meu-rumo-recuperado.tmp");
        try {
            Files.copy(BANCO, temporario, StandardCopyOption.REPLACE_EXISTING);
            Files.move(temporario, BANCO_RECUPERADO, StandardCopyOption.REPLACE_EXISTING);
            LOG.warn("Cópia do banco criada em {}.", BANCO_RECUPERADO);
        } catch (IOException erro) {
            LOG.error("Não foi possível copiar o banco; o sistema vai iniciar com um banco novo.", erro);
            try {
                Files.deleteIfExists(temporario);
            } catch (IOException ignorado) {
                LOG.debug("Arquivo temporário não removido", ignorado);
            }
        }
    }

    private static int porta(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--server.port=")) {
                return Integer.parseInt(arg.substring("--server.port=".length()));
            }
        }
        String porta = System.getenv().getOrDefault("SERVER_PORT", System.getenv().getOrDefault("PORT", "8080"));
        return Integer.parseInt(porta.trim());
    }

    private static volatile byte[] textoDiagnostico = new byte[0];

    private static synchronized void servirPaginaDeFalha(int porta, String texto) {
        textoDiagnostico = texto.getBytes(StandardCharsets.UTF_8);
        if (paginaDiagnostico != null) {
            return;
        }
        try {
            HttpServer servidor = HttpServer.create(new InetSocketAddress(porta), 0);
            servidor.createContext("/", troca -> {
                byte[] corpo = textoDiagnostico;
                troca.getResponseHeaders().set("Content-Type", "text/plain; charset=utf-8");
                troca.sendResponseHeaders(503, corpo.length);
                try (OutputStream saida = troca.getResponseBody()) {
                    saida.write(corpo);
                }
            });
            servidor.start();
            paginaDiagnostico = servidor;
            LOG.error("Página de diagnóstico no ar na porta {}.", porta);
        } catch (IOException erro) {
            LOG.error("Não foi possível abrir a página de diagnóstico.", erro);
        }
    }
}
