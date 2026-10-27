package com.ibmec.remomeurumo.dto;

import java.util.List;

public record LinhaFrequenciaParticipante(
        Long participanteId,
        String participante,
        String turmas,
        List<String> listaTurmas,
        long presencas,
        long previstas,
        long faltas,
        long faltasAvisadas,
        double percentual,
        long presencasAno,
        long previstasAno,
        boolean contatoNecessario,
        boolean abaixoDaMeta,
        String observacao) {

    public boolean semAulasPrevistas() {
        return previstas == 0;
    }
}
