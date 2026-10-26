package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import com.ibmec.remomeurumo.model.StatusPresenca;

public record Chamada(
        Long turmaId,
        String turma,
        String grade,
        LocalDate data,
        String quando,
        Long encontroId,
        LocalDateTime registradaEm,
        String aulaNaoRealizada,
        List<LinhaChamada> linhas) {

    public boolean isNaoRealizada() {
        return aulaNaoRealizada != null;
    }

    public int getTotal() {
        return linhas.size();
    }

    public long getMarcados() {
        return linhas.stream().filter(linha -> linha.status() != null).count();
    }

    public long getPresentes() {
        return contar(StatusPresenca.PRESENTE);
    }

    public long getFaltas() {
        return contar(StatusPresenca.AUSENTE);
    }

    public long getAvisadas() {
        return contar(StatusPresenca.JUSTIFICADA);
    }

    private long contar(StatusPresenca status) {
        return linhas.stream().filter(linha -> linha.status() == status).count();
    }

    public boolean isRegistrada() {
        return registradaEm != null;
    }
}
