package com.ibmec.remomeurumo.dto;

import java.time.LocalTime;
import java.util.Set;

import com.ibmec.remomeurumo.model.DiaSemana;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TurmaInput(
        @NotBlank(message = "Informe o nome da turma.")
        @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
        String nome,
        @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
        String descricao,
        Set<DiaSemana> diasSemana,
        LocalTime horario) {
}
