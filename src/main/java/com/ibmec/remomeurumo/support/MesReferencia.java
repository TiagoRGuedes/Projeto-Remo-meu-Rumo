package com.ibmec.remomeurumo.support;

import java.time.YearMonth;
import java.time.format.DateTimeParseException;

import com.ibmec.remomeurumo.exception.RegraNegocioException;

public final class MesReferencia {

    private MesReferencia() {
    }

    public static YearMonth resolver(String mes) {
        if (mes == null || mes.isBlank()) {
            return YearMonth.now();
        }
        try {
            return YearMonth.parse(mes.trim());
        } catch (DateTimeParseException ex) {
            throw new RegraNegocioException("Informe o mês no formato AAAA-MM, por exemplo 2026-08.");
        }
    }
}
