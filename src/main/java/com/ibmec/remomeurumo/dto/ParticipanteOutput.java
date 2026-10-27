package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonInclude;

import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Sexo;
import com.ibmec.remomeurumo.model.TipoEscola;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ParticipanteOutput(
        Long id,
        String nome,
        Sexo sexo,
        LocalDate dataNascimento,
        Integer idade,
        TipoEscola tipoEscola,
        Boolean possuiDeficiencia,
        String condicao,
        boolean ativo,
        String observacoes,
        LocalDateTime dataCadastro,
        LocalDateTime dataAtualizacao) {

    public static ParticipanteOutput de(Participante participante) {
        return new ParticipanteOutput(
                participante.getId(),
                participante.getNome(),
                participante.getSexo(),
                participante.getDataNascimento(),
                participante.getIdade(),
                participante.getTipoEscola(),
                participante.getPossuiDeficiencia(),
                participante.getCondicao(),
                participante.isAtivo(),
                participante.getObservacoes(),
                participante.getDataCadastro(),
                participante.getDataAtualizacao());
    }

    public static ParticipanteOutput resumo(Participante participante) {
        return new ParticipanteOutput(
                participante.getId(),
                participante.getNome(),
                participante.getSexo(),
                participante.getDataNascimento(),
                participante.getIdade(),
                participante.getTipoEscola(),
                null,
                null,
                participante.isAtivo(),
                participante.getObservacoes(),
                participante.getDataCadastro(),
                participante.getDataAtualizacao());
    }
}
