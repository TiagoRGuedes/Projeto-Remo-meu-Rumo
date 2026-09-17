package com.ibmec.remomeurumo.dto;

import java.time.LocalDateTime;

import com.ibmec.remomeurumo.model.Participante;

public record ParticipanteOutput(
        Long id,
        String nome,
        boolean ativo,
        String observacoes,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao) {

    public static ParticipanteOutput de(Participante participante) {
        return new ParticipanteOutput(
                participante.getId(),
                participante.getNome(),
                participante.isAtivo(),
                participante.getObservacoes(),
                participante.getDataCadastro(),
                participante.getDataAtualizacao());
    }
}
