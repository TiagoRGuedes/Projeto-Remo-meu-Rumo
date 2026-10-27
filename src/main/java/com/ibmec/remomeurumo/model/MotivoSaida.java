package com.ibmec.remomeurumo.model;

import java.util.Arrays;
import java.util.List;

public enum MotivoSaida {
    DEDICACAO_OUTRA_ATIVIDADE("Dedicação a outra atividade"),
    MUDANCA_ROTINA_FAMILIAR("Mudança na rotina familiar"),
    AFASTAMENTO_SAUDE("Afastamento por saúde"),
    OUTRO("Outro motivo"),
    TROCA_DE_TURMA("Troca de turma"),
    TURMA_REMOVIDA("Turma removida");

    private final String descricao;

    MotivoSaida(String descricao) {
        this.descricao = descricao;
    }

    public static List<MotivoSaida> paraSaida() {
        return Arrays.stream(values()).filter(motivo -> motivo != TROCA_DE_TURMA && motivo != TURMA_REMOVIDA).toList();
    }

    public String getDescricao() {
        return descricao;
    }
}
