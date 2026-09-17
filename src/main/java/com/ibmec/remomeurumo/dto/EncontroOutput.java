package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.StatusEncontro;

public record EncontroOutput(
        Long id,
        Long atividadeId,
        String atividadeNome,
        LocalDate data,
        String observacoes,
        StatusEncontro status,
        LocalDateTime dataCadastro) {

    public static EncontroOutput de(Encontro encontro) {
        return new EncontroOutput(
                encontro.getId(),
                encontro.getAtividade().getId(),
                encontro.getAtividade().getNome(),
                encontro.getData(),
                encontro.getObservacoes(),
                encontro.getStatus(),
                encontro.getDataCadastro());
    }
}
