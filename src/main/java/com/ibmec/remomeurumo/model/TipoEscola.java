package com.ibmec.remomeurumo.model;

public enum TipoEscola {
    PUBLICA("Escola pública"),
    PARTICULAR("Escola particular"),
    NAO_ESTUDA("Não estuda"),
    NAO_INFORMADO("Não informado");

    private final String descricao;

    TipoEscola(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
