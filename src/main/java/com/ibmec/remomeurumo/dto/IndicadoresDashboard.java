package com.ibmec.remomeurumo.dto;

public record IndicadoresDashboard(
        long participantesAtivos,
        long participantesInativos,
        long atividadesAtivas,
        long atividadesInativas,
        long encontrosNoMes,
        long presencasNoMes,
        long ausenciasNoMes,
        long justificadasNoMes,
        double frequenciaMedia,
        long totalRegistrosPresenca) {
}
