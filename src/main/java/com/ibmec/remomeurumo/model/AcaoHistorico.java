package com.ibmec.remomeurumo.model;

public enum AcaoHistorico {
    CRIACAO("Criação"),
    EDICAO("Edição"),
    ATIVACAO("Ativação"),
    DESATIVACAO("Desativação"),
    EXCLUSAO("Exclusão"),
    REGISTRO("Registro"),
    CORRECAO("Correção"),
    CANCELAMENTO("Cancelamento"),
    INGRESSO("Ingresso"),
    SAIDA("Saída"),
    TROCA_TURMA("Troca de turma"),
    ARQUIVO("Arquivo anexado");

    private final String descricao;

    AcaoHistorico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
