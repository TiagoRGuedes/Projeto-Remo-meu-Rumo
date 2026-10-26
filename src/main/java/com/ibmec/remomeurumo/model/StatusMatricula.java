package com.ibmec.remomeurumo.model;

public enum StatusMatricula {
    ATIVA("Ativa"),
    ENCERRADA("Encerrada");

    private final String descricao;

    StatusMatricula(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
