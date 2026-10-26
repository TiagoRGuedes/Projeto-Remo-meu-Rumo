package com.ibmec.remomeurumo.model;

public enum StatusEncontro {
    PLANEJADO("Planejada"),
    REALIZADO("Realizada"),
    CANCELADO("Cancelada");

    private final String descricao;

    StatusEncontro(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
