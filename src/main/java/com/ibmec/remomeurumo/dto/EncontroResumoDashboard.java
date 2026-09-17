package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

import com.ibmec.remomeurumo.model.StatusEncontro;

public record EncontroResumoDashboard(
        Long id,
        Long atividadeId,
        String atividadeNome,
        LocalDate data,
        StatusEncontro status,
        long totalEsperado,
        long totalRegistros,
        long presentes,
        long ausentes,
        long justificadas,
        double percentualPresenca) {
}
