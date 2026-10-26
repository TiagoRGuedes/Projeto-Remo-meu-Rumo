package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;

import com.ibmec.remomeurumo.model.Matricula;

public record MatriculaOutput(
        Long id,
        Long participanteId,
        String participante,
        Long turmaId,
        String turma,
        String grade,
        String status,
        LocalDate dataInicio,
        LocalDate dataSaida,
        String motivoSaida,
        String observacaoSaida) {

    public static MatriculaOutput de(Matricula matricula) {
        return new MatriculaOutput(
                matricula.getId(),
                matricula.getParticipante().getId(),
                matricula.getParticipante().getNome(),
                matricula.getAtividade().getId(),
                matricula.getAtividade().getNome(),
                matricula.getAtividade().getGradeResumo(),
                matricula.getStatus().name(),
                matricula.getDataInicio(),
                matricula.getDataSaida(),
                matricula.getMotivoSaida() == null ? null : matricula.getMotivoSaida().name(),
                matricula.getObservacaoSaida());
    }
}
