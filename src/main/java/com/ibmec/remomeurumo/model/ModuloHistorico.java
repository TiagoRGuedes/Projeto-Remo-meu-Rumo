package com.ibmec.remomeurumo.model;

public enum ModuloHistorico {
    PARTICIPANTES("Participantes"),
    ATIVIDADES("Turmas"),
    MATRICULAS("Matrículas"),
    ENCONTROS("Aulas"),
    PRESENCAS("Presenças");

    private final String descricao;

    ModuloHistorico(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
