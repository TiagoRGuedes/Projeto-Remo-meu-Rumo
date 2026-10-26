package com.ibmec.remomeurumo.dto;

import java.util.List;

public record PainelInicial(
        String mes,
        String tituloMes,
        String hojeTexto,
        String mesAnterior,
        String rotuloMesAnterior,
        String mesSeguinte,
        String rotuloMesSeguinte,
        boolean mesAtual,
        double frequenciaMes,
        boolean temAulasNoMes,
        Double variacao,
        double meta,
        int limiteFaltas,
        List<FrequenciaMensal> precisamContato,
        List<SessaoChamada> chamadasDeHoje,
        List<SessaoChamada> chamadasPendentes,
        List<ResumoMensalTurma> turmas) {

    public boolean isAbaixoDaMeta() {
        return temAulasNoMes && frequenciaMes < meta;
    }

    public int getTotalContato() {
        return precisamContato.size();
    }

    public int getTotalPendentes() {
        return chamadasPendentes.size()
                + (int) chamadasDeHoje.stream().filter(SessaoChamada::isPendente).count();
    }
}
