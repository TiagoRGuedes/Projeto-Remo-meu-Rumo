package com.ibmec.remomeurumo.model;

public enum Perfil {
    COORDENACAO("Coordenação"),
    PROFESSOR("Professor");

    private final String descricao;

    Perfil(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
