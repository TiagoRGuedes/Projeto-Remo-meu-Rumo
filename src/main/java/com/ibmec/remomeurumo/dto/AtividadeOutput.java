package com.ibmec.remomeurumo.dto;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Set;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;

public record AtividadeOutput(
        Long id,
        String nome,
        String descricao,
        Set<DiaSemana> diasSemana,
        LocalTime horario,
        String grade,
        boolean ativo,
        LocalDateTime dataCadastro) {

    public static AtividadeOutput de(Atividade atividade) {
        return new AtividadeOutput(
                atividade.getId(),
                atividade.getNome(),
                atividade.getDescricao(),
                atividade.getDiasSemana(),
                atividade.getHorario(),
                atividade.getGradeResumo(),
                atividade.isAtivo(),
                atividade.getDataCadastro());
    }
}
