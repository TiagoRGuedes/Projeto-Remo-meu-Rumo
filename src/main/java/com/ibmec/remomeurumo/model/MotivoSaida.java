package com.ibmec.remomeurumo.model;

public enum MotivoSaida {
    DEDICACAO_OUTRA_ATIVIDADE("Dedicação a outra atividade"),
    MUDANCA_ROTINA_FAMILIAR("Mudança na rotina familiar"),
    AFASTAMENTO_SAUDE("Afastamento por saúde"),
    OUTRO("Outro motivo");

    private final String descricao;

    MotivoSaida(String descricao) {
        this.descricao = descricao;
    }

    public String getDescricao() {
        return descricao;
    }
}
