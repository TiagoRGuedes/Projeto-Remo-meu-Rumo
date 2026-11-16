package com.ibmec.remomeurumo;

import java.sql.SQLException;
import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class RemoMeuRumoApplication {

    private static final Logger LOG = LoggerFactory.getLogger(RemoMeuRumoApplication.class);

    /** Código de erro do H2 para "Database may be already in use". */
    static final int H2_BANCO_EM_USO = 90020;

    static final int MAX_TENTATIVAS = 18;

    static final Duration ESPERA_ENTRE_TENTATIVAS = Duration.ofSeconds(10);

    public static void main(String[] args) throws InterruptedException {
        // Num reinício do Azure, a versão nova pode subir enquanto a antiga ainda segura o arquivo do banco H2.
        // Em vez de cair na hora, espera a antiga liberar o arquivo (até cerca de 3 minutos).
        for (int tentativa = 1; ; tentativa++) {
            try {
                SpringApplication.run(RemoMeuRumoApplication.class, args);
                return;
            } catch (RuntimeException erro) {
                if (!bancoEmUso(erro) || tentativa >= MAX_TENTATIVAS) {
                    throw erro;
                }
                LOG.warn("Banco de dados ainda em uso por outro processo (tentativa {} de {}). Nova tentativa em {} s.",
                        tentativa, MAX_TENTATIVAS, ESPERA_ENTRE_TENTATIVAS.toSeconds());
                Thread.sleep(ESPERA_ENTRE_TENTATIVAS);
            }
        }
    }

    static boolean bancoEmUso(Throwable erro) {
        for (Throwable atual = erro; atual != null; atual = atual.getCause()) {
            if (atual instanceof SQLException sql && sql.getErrorCode() == H2_BANCO_EM_USO) {
                return true;
            }
            String mensagem = atual.getMessage();
            if (mensagem != null && mensagem.contains("Database may be already in use")) {
                return true;
            }
            if (atual.getCause() == atual) {
                break;
            }
        }
        return false;
    }
}
