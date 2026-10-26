package com.ibmec.remomeurumo.model;

public enum MotivoCancelamento {
    FERIADO("Feriado"),
    GREVE("Greve"),
    CONDICOES_CLIMATICAS("Condições climáticas"),
    OUTRO("Outro");

    private final String descricao;

    MotivoCancelamento(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
