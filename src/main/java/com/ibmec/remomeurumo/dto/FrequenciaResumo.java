package com.ibmec.remomeurumo.dto;

public record FrequenciaResumo(
        String nome,
        long totalEncontros,
        long presentes,
        long ausentes,
        long justificadas,
        double percentual) {
}
