package com.ibmec.remomeurumo.dto;

import java.util.List;

public record ResumoMensalTurma(
        Long turmaId,
        String turma,
        String grade,
        String mes,
        long aulasPrevistas,
        long matriculasAtivas,
        long presencasEsperadas,
        long presencasRegistradas,
        double percentual,
        boolean abaixoDaMeta,
        List<FrequenciaMensal> faltosos) {

    public int totalFaltosos() {
        return faltosos == null ? 0 : faltosos.size();
    }
}
