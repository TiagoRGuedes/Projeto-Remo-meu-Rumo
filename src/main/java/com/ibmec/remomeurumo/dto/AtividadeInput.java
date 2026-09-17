package com.ibmec.remomeurumo.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AtividadeInput(
        @NotBlank(message = "Informe o nome da atividade.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao) {
}
