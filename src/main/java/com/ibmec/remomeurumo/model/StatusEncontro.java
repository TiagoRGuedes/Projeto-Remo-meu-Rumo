package com.ibmec.remomeurumo.model;

public enum StatusEncontro {
    PLANEJADO("Planejado"),
    REALIZADO("Realizado"),
    CANCELADO("Cancelado");

    private final String descricao;

    StatusEncontro(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
