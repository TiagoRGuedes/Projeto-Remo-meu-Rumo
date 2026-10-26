package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

import com.ibmec.remomeurumo.model.MotivoSaida;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record EncerramentoMatriculaInput(
        @NotNull(message = "Informe o motivo da saída.")
        MotivoSaida motivoSaida,
        @Size(max = 300, message = "A observação deve ter no máximo 300 caracteres.")
        String observacaoSaida,
        LocalDate dataSaida) {
}
