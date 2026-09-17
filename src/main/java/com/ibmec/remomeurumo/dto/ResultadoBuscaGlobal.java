package com.ibmec.remomeurumo.dto;

import java.util.List;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Participante;

public record ResultadoBuscaGlobal(
        String termo,
        List<Participante> participantes,
        List<Atividade> atividades,
        List<Encontro> encontros) {

    public int total() {
        return participantes.size() + atividades.size() + encontros.size();
    }
}
