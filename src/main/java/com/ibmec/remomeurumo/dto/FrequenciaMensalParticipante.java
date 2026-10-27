package com.ibmec.remomeurumo.dto;

import java.util.List;

public record FrequenciaMensalParticipante(
        FrequenciaMensal consolidado,
        List<FrequenciaMensal> porTurma) {
}
