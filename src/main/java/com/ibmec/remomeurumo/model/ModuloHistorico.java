package com.ibmec.remomeurumo.model;

public enum ModuloHistorico {
    PARTICIPANTES("Participantes"),
    ATIVIDADES("Atividades"),
    ENCONTROS("Encontros"),
    PRESENCAS("Presenças");

    private final String descricao;

    ModuloHistorico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
