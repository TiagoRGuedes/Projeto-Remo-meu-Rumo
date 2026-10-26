package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

public record MovimentoMatricula(LocalDate data, String tipo, String texto) {
}
