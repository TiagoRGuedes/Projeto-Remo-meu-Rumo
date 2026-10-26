package com.ibmec.remomeurumo.model;

public enum TipoAnexo {
    ATESTADO("Atestado"),
    LAUDO("Laudo"),
    OUTRO("Outro documento");

    private final String descricao;

    TipoAnexo(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
