package com.ibmec.remomeurumo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ParticipanteInput(
        @NotBlank(message = "Informe o nome do participante.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes) {
}
