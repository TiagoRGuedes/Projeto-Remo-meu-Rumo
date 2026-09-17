package com.ibmec.remomeurumo.dto;

public record ResultadoRegistroPresenca(
        long total,
        long presentes,
        long ausentes,
        long justificadas,
        int novosRegistros,
        int correcoes) {
}
