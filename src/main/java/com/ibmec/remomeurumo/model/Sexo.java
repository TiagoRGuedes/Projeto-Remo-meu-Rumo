package com.ibmec.remomeurumo.model;

public enum Sexo {
    FEMININO("F", "Feminino"),
    MASCULINO("M", "Masculino"),
    NAO_INFORMADO("-", "Não informado");

    private final String codigo;
    private final String descricao;

    Sexo(String codigo, String descricao) {
        this.codigo = codigo;
        this.descricao = descricao;
    }

    public String getCodigo() {
        return codigo;
    }

    public String getDescricao() {
        return descricao;
    }
}
