package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Sexo;
import com.ibmec.remomeurumo.model.TipoEscola;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record ParticipanteInput(
        @NotBlank(message = "Informe o nome do participante.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,
        Sexo sexo,
        @Past(message = "A data de nascimento deve ser anterior a hoje.")
        LocalDate dataNascimento,
        TipoEscola tipoEscola,
        Boolean possuiDeficiencia,
        @Size(max = 80, message = "A referência do laudo deve ter no máximo 80 caracteres.")
        String laudoReferencia,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes) {

    public Participante paraModelo() {
        return new Participante(nome, sexo, dataNascimento, tipoEscola, possuiDeficiencia, laudoReferencia, observacoes);
    }
}
