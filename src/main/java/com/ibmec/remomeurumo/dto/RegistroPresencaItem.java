package com.ibmec.remomeurumo.dto;

import com.ibmec.remomeurumo.model.StatusPresenca;

public record RegistroPresencaItem(
        Long participanteId,
        String nomeParticipante,
        StatusPresenca statusAtual,
        String observacoes) {
}
