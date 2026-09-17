package com.ibmec.remomeurumo.model;

public enum StatusPresenca {
    PRESENTE("Presente"),
    AUSENTE("Ausente"),
    JUSTIFICADA("Justificada");

    private final String descricao;

    StatusPresenca(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
