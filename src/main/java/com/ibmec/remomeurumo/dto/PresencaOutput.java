package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusPresenca;

public record PresencaOutput(
        Long id,
        Long participanteId,
        String participanteNome,
        Long encontroId,
        Long atividadeId,
        String atividadeNome,
        LocalDate data,
        StatusPresenca status,
        String observacoes,
        LocalDateTime registradoEm,
        LocalDateTime atualizadoEm) {

    public static PresencaOutput de(Presenca presenca) {
        return new PresencaOutput(
                presenca.getId(),
                presenca.getParticipante().getId(),
                presenca.getParticipante().getNome(),
                presenca.getEncontro().getId(),
                presenca.getEncontro().getAtividade().getId(),
                presenca.getEncontro().getAtividade().getNome(),
                presenca.getEncontro().getData(),
                presenca.getStatus(),
                presenca.getObservacoes(),
                presenca.getRegistradoEm(),
                presenca.getAtualizadoEm());
    }
}
