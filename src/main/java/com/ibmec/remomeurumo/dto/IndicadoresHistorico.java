package com.ibmec.remomeurumo.dto;

public record IndicadoresHistorico(
        long total,
        long acoesHoje,
        long acoesUltimosSeteDias,
        long cadastros,
        long alteracoes) {
}
