package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

public record SessaoChamada(
        Long turmaId,
        String turma,
        String grade,
        LocalDate data,
        String quando,
        String hora,
        Long encontroId,
        long participantes,
        long registrados,
        String situacao) {

    public boolean isRegistrada() {
        return "Registrada".equals(situacao);
    }

    public boolean isAgora() {
        return "Agora".equals(situacao);
    }

    public boolean isPendente() {
        return "Pendente".equals(situacao);
    }
}
