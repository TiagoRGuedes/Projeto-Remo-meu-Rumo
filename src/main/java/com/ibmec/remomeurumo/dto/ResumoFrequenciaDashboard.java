package com.ibmec.remomeurumo.dto;

public record ResumoFrequenciaDashboard(
        double mediaGeral,
        long participantesExibidos,
        long presencas,
        long ausencias,
        long justificadas) {
}
