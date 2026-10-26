package com.ibmec.remomeurumo.dto;

import com.ibmec.remomeurumo.model.StatusPresenca;

public record LinhaChamada(
        Long participanteId,
        String nome,
        String iniciais,
        long faltasNoMes,
        boolean emRisco,
        StatusPresenca status) {

    public String getResumoFaltas() {
        if (faltasNoMes == 0) {
            return "Sem faltas no mês";
        }
        String texto = faltasNoMes + (faltasNoMes == 1 ? " falta no mês" : " faltas no mês");
        return emRisco ? texto + " — atenção" : texto;
    }

    public String getCodigoStatus() {
        return status == null ? "" : status.name();
    }
}
