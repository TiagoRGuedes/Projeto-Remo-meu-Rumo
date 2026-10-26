package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

public record MatriculaInput(
        @NotNull(message = "Informe o participante.")
        Long participanteId,
        @NotNull(message = "Informe a turma.")
        Long turmaId,
        LocalDate dataInicio) {
}
