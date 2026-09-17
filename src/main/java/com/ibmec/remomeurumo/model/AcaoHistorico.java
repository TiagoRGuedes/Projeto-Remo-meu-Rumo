package com.ibmec.remomeurumo.model;

public enum AcaoHistorico {
    CRIACAO("Criação"),
    EDICAO("Edição"),
    ATIVACAO("Ativação"),
    DESATIVACAO("Desativação"),
    EXCLUSAO("Exclusão"),
    REGISTRO("Registro"),
    CORRECAO("Correção"),
    CANCELAMENTO("Cancelamento");

    private final String descricao;

    AcaoHistorico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
