package com.ibmec.remomeurumo.service;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.ibmec.remomeurumo.dto.AlteracaoConfirmacao;
import com.ibmec.remomeurumo.dto.ConfirmacaoOperacao;
import com.ibmec.remomeurumo.dto.TipoOperacaoConfirmacao;

import jakarta.servlet.http.HttpSession;

@Service
public class ConfirmacaoOperacaoService {

    private static final String CHAVE_SESSAO = "confirmacoesOperacao";
    private static final Duration VALIDADE = Duration.ofMinutes(30);
    private static final int LIMITE_POR_SESSAO = 12;

    public ConfirmacaoOperacao criar(HttpSession session,
            TipoOperacaoConfirmacao tipo,
            Long entidadeId,
            String etiqueta,
            String titulo,
            String resumo,
            String nomeEntidade,
            List<AlteracaoConfirmacao> alteracoes,
            List<String> consequencias,
            Map<String, String> dados,
            String urlCorrecao,
            String urlCancelar,
            String urlSucesso,
            String mensagemSucesso,
            String textoConfirmacao,
            boolean destrutiva) {
        String token = UUID.randomUUID().toString();
        ConfirmacaoOperacao confirmacao = new ConfirmacaoOperacao(
                token, tipo, entidadeId, etiqueta, titulo, resumo, nomeEntidade,
                alteracoes, consequencias, dados, urlCorrecao, urlCancelar,
                urlSucesso, mensagemSucesso, textoConfirmacao, destrutiva, Instant.now());
        synchronized (session) {
            Map<String, ConfirmacaoOperacao> confirmacoes = confirmacoes(session);
            limparExpiradas(confirmacoes);
            while (confirmacoes.size() >= LIMITE_POR_SESSAO) {
                confirmacoes.remove(confirmacoes.keySet().iterator().next());
            }
            confirmacoes.put(token, confirmacao);
        }
        return confirmacao;
    }

    public Optional<ConfirmacaoOperacao> obter(HttpSession session, String token) {
        synchronized (session) {
            Map<String, ConfirmacaoOperacao> confirmacoes = confirmacoes(session);
            limparExpiradas(confirmacoes);
            return Optional.ofNullable(confirmacoes.get(token));
        }
    }

    public Optional<ConfirmacaoOperacao> consumir(HttpSession session, String token) {
        synchronized (session) {
            Map<String, ConfirmacaoOperacao> confirmacoes = confirmacoes(session);
            limparExpiradas(confirmacoes);
            return Optional.ofNullable(confirmacoes.remove(token));
        }
    }

    public void descartar(HttpSession session, String token) {
        synchronized (session) {
            confirmacoes(session).remove(token);
        }
    }

    public List<AlteracaoConfirmacao> alteracoes(AlteracaoConfirmacao... itens) {
        List<AlteracaoConfirmacao> resultado = new ArrayList<>();
        if (itens != null) {
            for (AlteracaoConfirmacao item : itens) {
                if (item != null) {
                    resultado.add(item);
                }
            }
        }
        return resultado;
    }

    public AlteracaoConfirmacao alteracao(String campo, String antes, String depois) {
        String valorAntes = texto(antes);
        String valorDepois = texto(depois);
        return new AlteracaoConfirmacao(campo, valorAntes, valorDepois, !valorAntes.equals(valorDepois));
    }

    @SuppressWarnings("unchecked")
    private Map<String, ConfirmacaoOperacao> confirmacoes(HttpSession session) {
        Object existente = session.getAttribute(CHAVE_SESSAO);
        if (existente instanceof Map<?, ?> mapa) {
            return (Map<String, ConfirmacaoOperacao>) mapa;
        }
        Map<String, ConfirmacaoOperacao> novo = new LinkedHashMap<>();
        session.setAttribute(CHAVE_SESSAO, novo);
        return novo;
    }

    private void limparExpiradas(Map<String, ConfirmacaoOperacao> confirmacoes) {
        Instant limite = Instant.now().minus(VALIDADE);
        confirmacoes.values().removeIf(item -> item.criadaEm().isBefore(limite));
    }

    private String texto(String valor) {
        return valor == null || valor.isBlank() ? "Não informado" : valor.trim();
    }
}
