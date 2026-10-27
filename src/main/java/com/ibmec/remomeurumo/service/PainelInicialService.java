package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.dto.PainelInicial;
import com.ibmec.remomeurumo.dto.ResumoMensalTurma;

@Service
public class PainelInicialService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("d 'de' MMMM", PT_BR);

    private final FrequenciaService frequenciaService;
    private final ChamadaService chamadaService;

    public PainelInicialService(FrequenciaService frequenciaService, ChamadaService chamadaService) {
        this.frequenciaService = frequenciaService;
        this.chamadaService = chamadaService;
    }

    @Transactional(readOnly = true)
    public PainelInicial carregar(YearMonth mes) {
        LocalDate hoje = LocalDate.now();
        YearMonth atual = YearMonth.from(hoje);
        boolean mesAtual = mes.equals(atual);

        List<ResumoMensalTurma> turmas = frequenciaService.resumoMensalDasTurmas(mes).stream()
                .sorted(Comparator.comparingDouble(ResumoMensalTurma::percentual).reversed())
                .toList();
        long esperadas = turmas.stream().mapToLong(ResumoMensalTurma::presencasEsperadas).sum();
        double frequencia = percentual(turmas);
        Double variacao = null;
        List<ResumoMensalTurma> anteriores = frequenciaService.resumoMensalDasTurmas(mes.minusMonths(1));
        if (esperadas > 0 && anteriores.stream().mapToLong(ResumoMensalTurma::presencasEsperadas).sum() > 0) {
            variacao = Math.round((frequencia - percentual(anteriores)) * 10.0) / 10.0;
        }

        List<FrequenciaMensal> contato = turmas.stream()
                .flatMap(turma -> turma.faltosos().stream())
                .sorted(Comparator.comparingLong(FrequenciaMensal::faltas).reversed()
                        .thenComparing(FrequenciaMensal::participante))
                .toList();

        YearMonth seguinte = mes.plusMonths(1);
        return new PainelInicial(
                mes.toString(),
                capitalizar(mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR)) + " de " + mes.getYear(),
                capitalizar(hoje.getDayOfWeek().getDisplayName(TextStyle.FULL, PT_BR).replace("-feira", ""))
                        + ", " + hoje.format(DIA_MES),
                mes.minusMonths(1).toString(),
                abreviado(mes.minusMonths(1)),
                seguinte.isAfter(atual) ? null : seguinte.toString(),
                abreviado(seguinte),
                mesAtual,
                frequencia,
                esperadas > 0,
                variacao,
                FrequenciaService.META_FREQUENCIA_MENSAL,
                FrequenciaService.LIMITE_FALTAS_MES,
                contato,
                mesAtual ? chamadaService.sessoesDeHoje() : List.of(),
                mesAtual ? chamadaService.pendentes() : List.of(),
                turmas);
    }

    private double percentual(List<ResumoMensalTurma> turmas) {
        long esperadas = turmas.stream().mapToLong(ResumoMensalTurma::presencasEsperadas).sum();
        long registradas = turmas.stream().mapToLong(ResumoMensalTurma::presencasRegistradas).sum();
        return esperadas == 0 ? 0 : Math.round(registradas * 1000.0 / esperadas) / 10.0;
    }

    private String abreviado(YearMonth mes) {
        return capitalizar(mes.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", ""));
    }

    private String capitalizar(String texto) {
        return texto.isEmpty() ? texto : Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }
}
