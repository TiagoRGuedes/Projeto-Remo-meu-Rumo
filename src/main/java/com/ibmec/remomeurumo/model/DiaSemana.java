package com.ibmec.remomeurumo.model;

import java.time.DayOfWeek;

public enum DiaSemana {
    SEGUNDA("SEG", "Segunda-feira", DayOfWeek.MONDAY),
    TERCA("TER", "Terça-feira", DayOfWeek.TUESDAY),
    QUARTA("QUA", "Quarta-feira", DayOfWeek.WEDNESDAY),
    QUINTA("QUI", "Quinta-feira", DayOfWeek.THURSDAY),
    SEXTA("SEX", "Sexta-feira", DayOfWeek.FRIDAY),
    SABADO("SÁB", "Sábado", DayOfWeek.SATURDAY),
    DOMINGO("DOM", "Domingo", DayOfWeek.SUNDAY);

    private final String sigla;
    private final String descricao;
    private final DayOfWeek diaDaSemana;

    DiaSemana(String sigla, String descricao, DayOfWeek diaDaSemana) {
        this.sigla = sigla;
        this.descricao = descricao;
        this.diaDaSemana = diaDaSemana;
    }

    public static DiaSemana de(DayOfWeek diaDaSemana) {
        for (DiaSemana item : values()) {
            if (item.diaDaSemana == diaDaSemana) {
                return item;
            }
        }
        throw new IllegalArgumentException("Dia da semana desconhecido: " + diaDaSemana);
    }

    public String getSigla() {
        return sigla;
    }

    public String getDescricao() {
        return descricao;
    }

    public DayOfWeek getDiaDaSemana() {
        return diaDaSemana;
    }
}
