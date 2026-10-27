package com.ibmec.remomeurumo.dto;

import java.time.LocalTime;
import java.util.Set;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;

public record TurmaOutput(
        Long id,
        String nome,
        String descricao,
        Set<DiaSemana> diasSemana,
        String diasSemanaResumo,
        LocalTime horario,
        String grade,
        boolean ativa) {

    public static TurmaOutput de(Atividade atividade) {
        return new TurmaOutput(
                atividade.getId(),
                atividade.getNome(),
                atividade.getDescricao(),
                atividade.getDiasSemana(),
                atividade.getDiasSemanaResumo(),
                atividade.getHorario(),
                atividade.getGradeResumo(),
                atividade.isAtivo());
    }
}
