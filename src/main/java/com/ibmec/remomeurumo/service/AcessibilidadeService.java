package com.ibmec.remomeurumo.service;

import org.springframework.stereotype.Service;

import jakarta.servlet.http.HttpSession;

@Service
public class AcessibilidadeService {

    public static final String SESSION_KEY = "preferenciasAcessibilidade";

    public PreferenciasAcessibilidade obter(HttpSession session) {
        Object valor = session.getAttribute(SESSION_KEY);
        if (valor instanceof PreferenciasAcessibilidade preferencias) {
            return preferencias;
        }
        PreferenciasAcessibilidade preferencias = PreferenciasAcessibilidade.padrao();
        session.setAttribute(SESSION_KEY, preferencias);
        return preferencias;
    }

    public void salvar(HttpSession session, PreferenciasAcessibilidade preferencias) {
        session.setAttribute(SESSION_KEY, preferencias);
    }

    public record PreferenciasAcessibilidade(
            String tamanhoFonte,
            boolean altoContraste,
            boolean modoEscuro,
            boolean reduzirAnimacoes) {

        public static PreferenciasAcessibilidade padrao() {
            return new PreferenciasAcessibilidade("normal", false, false, false);
        }

        public String classesCss() {
            StringBuilder classes = new StringBuilder("font-").append(tamanhoFonte == null ? "normal" : tamanhoFonte);
            if (altoContraste) {
                classes.append(" alto-contraste");
            }
            if (modoEscuro) {
                classes.append(" modo-escuro");
            }
            if (reduzirAnimacoes) {
                classes.append(" reduzir-animacoes");
            }
            return classes.toString();
        }
    }
}
