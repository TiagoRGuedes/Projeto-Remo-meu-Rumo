package com.ibmec.remomeurumo.dto;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;

public record ConfirmacaoOperacao(
        String token,
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
        boolean destrutiva,
        Instant criadaEm) implements Serializable {

    public ConfirmacaoOperacao {
        alteracoes = alteracoes == null ? List.of() : List.copyOf(alteracoes);
        consequencias = consequencias == null ? List.of() : List.copyOf(consequencias);
        dados = dados == null ? Map.of() : Map.copyOf(dados);
        criadaEm = criadaEm == null ? Instant.now() : criadaEm;
    }
}
