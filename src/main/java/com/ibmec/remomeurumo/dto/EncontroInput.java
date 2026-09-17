package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import com.ibmec.remomeurumo.model.StatusEncontro;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EncontroInput(
        @NotNull(message = "Informe a atividade.")
        Long atividadeId,
        @NotNull(message = "Informe a data do encontro.")
        @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
        LocalDate data,
        @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
        String observacoes,
        StatusEncontro status) {
}
