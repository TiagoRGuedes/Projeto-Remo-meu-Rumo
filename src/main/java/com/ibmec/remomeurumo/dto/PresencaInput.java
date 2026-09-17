package com.ibmec.remomeurumo.dto;

import com.ibmec.remomeurumo.model.StatusPresenca;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PresencaInput(
        @NotNull(message = "Informe o participante.")
        Long participanteId,
        @NotNull(message = "Informe o encontro.")
        Long encontroId,
        @NotNull(message = "Informe a presença.")
        StatusPresenca status,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes) {
}
