package com.ibmec.remomeurumo.dto;

public record RegistroExportacao(
        String record_id,
        Long participante_id,
        String participante,
        Long turma_id,
        String turma,
        String turma_grade,
        String mes,
        String matricula_status,
        String motivo_saida,
        long aulas_previstas,
        long presencas,
        long faltas,
        long faltas_justificadas,
        double percentual_frequencia,
        long presencas_ano,
        long previstas_ano,
        boolean acima_limite_faltas,
        boolean abaixo_meta_mensal) {
}
