package com.ibmec.remomeurumo.config;

import java.time.LocalTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;

public final class TurmasOficiais {

    public static final List<Turma> LISTA = List.of(
            new Turma(EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(9, 0)),
            new Turma(EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(10, 0)),
            new Turma(EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(14, 0)),
            new Turma(EnumSet.of(DiaSemana.SEGUNDA), LocalTime.of(16, 0)),
            new Turma(EnumSet.of(DiaSemana.TERCA), LocalTime.of(9, 0)),
            new Turma(EnumSet.of(DiaSemana.TERCA), LocalTime.of(10, 0)),
            new Turma(EnumSet.of(DiaSemana.TERCA), LocalTime.of(14, 0)),
            new Turma(EnumSet.of(DiaSemana.TERCA), LocalTime.of(16, 0)),
            new Turma(EnumSet.of(DiaSemana.QUARTA), LocalTime.of(9, 0)),
            new Turma(EnumSet.of(DiaSemana.QUARTA), LocalTime.of(10, 0)),
            new Turma(EnumSet.of(DiaSemana.QUARTA), LocalTime.of(14, 0)),
            new Turma(EnumSet.of(DiaSemana.QUARTA), LocalTime.of(16, 0)),
            new Turma(EnumSet.of(DiaSemana.QUINTA), LocalTime.of(9, 0)),
            new Turma(EnumSet.of(DiaSemana.QUINTA), LocalTime.of(10, 0)),
            new Turma(EnumSet.of(DiaSemana.QUINTA), LocalTime.of(14, 0)),
            new Turma(EnumSet.of(DiaSemana.QUINTA), LocalTime.of(16, 0)),
            new Turma(EnumSet.of(DiaSemana.SABADO), LocalTime.of(9, 0)),
            new Turma(EnumSet.of(DiaSemana.SABADO), LocalTime.of(10, 30)));

    private TurmasOficiais() {
    }

    public record Turma(Set<DiaSemana> dias, LocalTime horario) {

        public String nome() {
            return new Atividade(null, null, dias, horario).getGradeResumo();
        }

        public Atividade novaAtividade() {
            String descricao = "Turma de remo · " + dias.stream()
                    .map(dia -> dia.getDescricao().toLowerCase())
                    .reduce((primeiro, segundo) -> primeiro + " e " + segundo)
                    .orElse("") + " às " + new Atividade(null, null, dias, horario).getHorarioCurto() + ".";
            return new Atividade(nome(), descricao, dias, horario);
        }
    }
}
