package com.ibmec.remomeurumo.dto;

public record FrequenciaMensal(
        Long participanteId,
        String participante,
        Long turmaId,
        String turma,
        String mes,
        long aulasPrevistas,
        long presencas,
        long faltas,
        long faltasJustificadas,
        double percentual,
        boolean contatoNecessario,
        boolean abaixoDaMeta) {

    public long faltasNaoJustificadas() {
        return Math.max(0, faltas - faltasJustificadas);
    }

    public boolean semAulasPrevistas() {
        return aulasPrevistas == 0;
    }
}
