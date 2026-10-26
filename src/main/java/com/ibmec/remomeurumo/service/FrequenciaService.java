package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.dto.FrequenciaMensalParticipante;
import com.ibmec.remomeurumo.dto.FrequenciaResumo;
import com.ibmec.remomeurumo.dto.ResumoFrequenciaDashboard;
import com.ibmec.remomeurumo.dto.ResumoMensalTurma;
import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.MatriculaRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class FrequenciaService {

    public static final int LIMITE_FALTAS_MES = 3;

    public static final double META_FREQUENCIA_MENSAL = 60.0;

    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    private final PresencaRepository presencaRepository;
    private final EncontroRepository encontroRepository;
    private final MatriculaRepository matriculaRepository;
    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;

    public FrequenciaService(PresencaRepository presencaRepository,
            EncontroRepository encontroRepository,
            MatriculaRepository matriculaRepository,
            ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository) {
        this.presencaRepository = presencaRepository;
        this.encontroRepository = encontroRepository;
        this.matriculaRepository = matriculaRepository;
        this.participanteRepository = participanteRepository;
        this.atividadeRepository = atividadeRepository;
    }

    @Transactional(readOnly = true)
    public FrequenciaMensalParticipante frequenciaMensalDoParticipante(Long participanteId, YearMonth mes) {
        var participante = participanteRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
        LocalDate inicio = mes.atDay(1);
        LocalDate fim = limiteDoMes(mes);

        Map<Long, Periodos> porTurmaId = new LinkedHashMap<>();
        for (Matricula matricula : matriculaRepository.listarPorParticipante(participanteId)) {
            if (!matricula.vigenteNoPeriodo(inicio, fim)) {
                continue;
            }
            FrequenciaMensal parcial = calcular(matricula, mes, inicio, fim);
            porTurmaId.computeIfAbsent(matricula.getAtividade().getId(), id -> new Periodos(matricula.getAtividade()))
                    .somar(matricula, parcial.aulasPrevistas(), parcial.presencas(), parcial.faltasJustificadas(), inicio, fim);
        }
        List<FrequenciaMensal> porTurma = new ArrayList<>();
        long totalPrevistas = 0;
        long totalPresencas = 0;
        long totalJustificadas = 0;
        for (Periodos periodos : porTurmaId.values()) {
            FrequenciaMensal linha = periodos.montar(participanteId, participante.getNome(), mes);
            porTurma.add(linha);
            totalPrevistas += linha.aulasPrevistas();
            totalPresencas += linha.presencas();
            totalJustificadas += linha.faltasJustificadas();
        }

        FrequenciaMensal consolidado = montar(participanteId, participante.getNome(), null, null,
                mes, totalPrevistas, totalPresencas, totalJustificadas);
        return new FrequenciaMensalParticipante(consolidado, porTurma);
    }

    @Transactional(readOnly = true)
    public ResumoMensalTurma resumoMensalDaTurma(Long turmaId, YearMonth mes) {
        Atividade turma = obterTurma(turmaId);
        List<FrequenciaMensal> linhas = linhasMensaisDaTurma(turma, mes);

        long aulasPrevistas = encontroRepository.countByAtividadeIdAndStatusNotAndDataBetween(
                turmaId, StatusEncontro.CANCELADO, mes.atDay(1), limiteDoMes(mes));
        long presencasRegistradas = linhas.stream().mapToLong(FrequenciaMensal::presencas).sum();
        long presencasEsperadas = linhas.stream().mapToLong(FrequenciaMensal::aulasPrevistas).sum();
        double percentual = presencasEsperadas == 0 ? 0
                : arredondar(presencasRegistradas * 100.0 / presencasEsperadas);
        List<FrequenciaMensal> faltosos = linhas.stream()
                .filter(FrequenciaMensal::contatoNecessario)
                .sorted(Comparator.comparingLong(FrequenciaMensal::faltas).reversed()
                        .thenComparing(FrequenciaMensal::participante))
                .toList();

        return new ResumoMensalTurma(turma.getId(), turma.getNome(), turma.getGradeResumo(), mes.toString(),
                aulasPrevistas, linhas.size(), presencasEsperadas, presencasRegistradas, percentual,
                presencasEsperadas > 0 && percentual < META_FREQUENCIA_MENSAL, faltosos);
    }

    @Transactional(readOnly = true)
    public List<FrequenciaMensal> linhasMensaisDaTurma(Long turmaId, YearMonth mes) {
        return linhasMensaisDaTurma(obterTurma(turmaId), mes);
    }

    @Transactional(readOnly = true)
    public List<FrequenciaMensal> faltososDaTurma(Long turmaId, YearMonth mes) {
        return resumoMensalDaTurma(turmaId, mes).faltosos();
    }

    @Transactional(readOnly = true)
    public List<ResumoMensalTurma> resumoMensalDasTurmas(YearMonth mes) {
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .map(turma -> resumoMensalDaTurma(turma.getId(), mes))
                .toList();
    }

    public LocalDate limiteDoMes(YearMonth mes) {
        LocalDate fim = mes.atEndOfMonth();
        LocalDate hoje = LocalDate.now();
        return fim.isAfter(hoje) ? hoje : fim;
    }

    private Atividade obterTurma(Long turmaId) {
        return atividadeRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> faltasAntesDe(Long turmaId, LocalDate data) {
        YearMonth mes = YearMonth.from(data);
        Map<Long, Long> faltas = new LinkedHashMap<>();
        if (data.getDayOfMonth() == 1) {
            return faltas;
        }
        for (FrequenciaMensal linha : linhasDoPeriodo(obterTurma(turmaId), mes, mes.atDay(1), data.minusDays(1))) {
            faltas.put(linha.participanteId(), linha.faltas());
        }
        return faltas;
    }

    private List<FrequenciaMensal> linhasMensaisDaTurma(Atividade turma, YearMonth mes) {
        return linhasDoPeriodo(turma, mes, mes.atDay(1), limiteDoMes(mes));
    }

    private List<FrequenciaMensal> linhasDoPeriodo(Atividade turma, YearMonth mes, LocalDate inicio, LocalDate fim) {
        List<Encontro> aulas = encontroRepository.findByAtividadeIdAndStatusNotAndDataBetweenOrderByDataAsc(
                turma.getId(), StatusEncontro.CANCELADO, inicio, fim);
        Map<Long, List<Presenca>> presencasPorParticipante = presencaRepository
                .listarPorTurmaNoPeriodo(turma.getId(), inicio, fim).stream()
                .collect(Collectors.groupingBy(presenca -> presenca.getParticipante().getId()));

        Map<Long, Periodos> porParticipante = new LinkedHashMap<>();
        for (Matricula matricula : matriculaRepository.listarPorTurma(turma.getId())) {
            if (!matricula.vigenteNoPeriodo(inicio, fim)) {
                continue;
            }
            LocalDate de = matricula.inicioNoPeriodo(inicio);
            LocalDate ate = matricula.fimNoPeriodo(fim);
            long aulasPrevistas = aulas.stream().filter(aula -> dentro(aula.getData(), de, ate)).count();
            List<Presenca> presencas = presencasPorParticipante
                    .getOrDefault(matricula.getParticipante().getId(), List.of()).stream()
                    .filter(presenca -> dentro(presenca.getEncontro().getData(), de, ate))
                    .toList();
            porParticipante.computeIfAbsent(matricula.getParticipante().getId(), id -> new Periodos(turma))
                    .somar(matricula, aulasPrevistas, contar(presencas, StatusPresenca.PRESENTE),
                            contar(presencas, StatusPresenca.JUSTIFICADA), inicio, fim);
        }
        List<FrequenciaMensal> linhas = new ArrayList<>();
        for (Map.Entry<Long, Periodos> entrada : porParticipante.entrySet()) {
            linhas.add(entrada.getValue().montar(entrada.getKey(), entrada.getValue().nomeParticipante, mes));
        }
        linhas.sort(Comparator.comparing(FrequenciaMensal::participante));
        return linhas;
    }

    private FrequenciaMensal calcular(Matricula matricula, YearMonth mes, LocalDate inicio, LocalDate fim) {
        Atividade turma = matricula.getAtividade();
        Long participanteId = matricula.getParticipante().getId();
        LocalDate de = matricula.inicioNoPeriodo(inicio);
        LocalDate ate = matricula.fimNoPeriodo(fim);
        long aulasPrevistas = encontroRepository.countByAtividadeIdAndStatusNotAndDataBetween(
                turma.getId(), StatusEncontro.CANCELADO, de, ate);
        long presencas = presencaRepository.contarPorParticipanteTurmaEStatus(
                participanteId, turma.getId(), StatusPresenca.PRESENTE, de, ate);
        long justificadas = presencaRepository.contarPorParticipanteTurmaEStatus(
                participanteId, turma.getId(), StatusPresenca.JUSTIFICADA, de, ate);
        return montar(participanteId, matricula.getParticipante().getNome(), turma.getId(), turma.getNome(),
                mes, aulasPrevistas, presencas, justificadas);
    }

    private boolean dentro(LocalDate data, LocalDate de, LocalDate ate) {
        return !data.isBefore(de) && !data.isAfter(ate);
    }

    private FrequenciaMensal montar(Long participanteId, String nomeParticipante, Long turmaId, String nomeTurma,
            YearMonth mes, long aulasPrevistas, long presencas, long justificadas) {
        return montar(participanteId, nomeParticipante, turmaId, nomeTurma, mes, aulasPrevistas, presencas,
                justificadas, null);
    }

    private FrequenciaMensal montar(Long participanteId, String nomeParticipante, Long turmaId, String nomeTurma,
            YearMonth mes, long aulasPrevistas, long presencas, long justificadas, String observacao) {
        long presencasConsideradas = Math.min(presencas, aulasPrevistas);
        long faltas = Math.max(0, aulasPrevistas - presencasConsideradas);
        long justificadasConsideradas = Math.min(justificadas, faltas);
        double percentual = aulasPrevistas == 0 ? 0
                : arredondar(presencasConsideradas * 100.0 / aulasPrevistas);
        return new FrequenciaMensal(participanteId, nomeParticipante, turmaId, nomeTurma, mes.toString(),
                aulasPrevistas, presencasConsideradas, faltas, justificadasConsideradas, percentual,
                faltas > LIMITE_FALTAS_MES,
                aulasPrevistas > 0 && percentual < META_FREQUENCIA_MENSAL, observacao);
    }

    @Transactional(readOnly = true)
    public List<FrequenciaResumo> porParticipante(Long atividadeId, LocalDate inicio, LocalDate fim) {
        List<Presenca> presencas = presencaRepository.filtrarHistorico(null, atividadeId, inicio, fim, null);
        Map<String, List<Presenca>> porParticipante = presencas.stream()
                .collect(Collectors.groupingBy(p -> p.getParticipante().getNome()));
        return porParticipante.entrySet().stream()
                .map(entry -> resumir(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FrequenciaResumo::nome))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FrequenciaResumo> porAtividade(LocalDate inicio, LocalDate fim) {
        List<Presenca> presencas = presencaRepository.filtrarHistorico(null, null, inicio, fim, null);
        Map<String, List<Presenca>> porAtividade = presencas.stream()
                .collect(Collectors.groupingBy(p -> p.getEncontro().getAtividade().getNome()));
        return porAtividade.entrySet().stream()
                .map(entry -> resumir(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FrequenciaResumo::nome))
                .toList();
    }

    @Transactional(readOnly = true)
    public double frequenciaMediaGeral() {
        long total = presencaRepository.count();
        if (total == 0) {
            return 0;
        }
        long presentes = presencaRepository.countByStatus(StatusPresenca.PRESENTE);
        return arredondar(presentes * 100.0 / total);
    }

    @Transactional(readOnly = true)
    public ResumoFrequenciaDashboard resumirDashboard(Long atividadeId, LocalDate inicio, LocalDate fim) {
        List<Presenca> presencas = presencaRepository.filtrarHistorico(null, atividadeId, inicio, fim, null);
        return resumirDashboard(presencas);
    }

    public ResumoFrequenciaDashboard resumirDashboard(List<Presenca> presencas) {
        long presentes = contar(presencas, StatusPresenca.PRESENTE);
        long ausentes = contar(presencas, StatusPresenca.AUSENTE);
        long justificadas = contar(presencas, StatusPresenca.JUSTIFICADA);
        long total = presentes + ausentes + justificadas;
        double media = total == 0 ? 0 : arredondar(presentes * 100.0 / total);
        long participantes = presencas.stream().map(p -> p.getParticipante().getId()).distinct().count();
        return new ResumoFrequenciaDashboard(media, participantes, presentes, ausentes, justificadas);
    }

    public FrequenciaResumo resumir(String nome, List<Presenca> presencas) {
        long presentes = contar(presencas, StatusPresenca.PRESENTE);
        long ausentes = contar(presencas, StatusPresenca.AUSENTE);
        long justificadas = contar(presencas, StatusPresenca.JUSTIFICADA);
        long total = presentes + ausentes + justificadas;
        double percentual = total == 0 ? 0 : arredondar(presentes * 100.0 / total);
        return new FrequenciaResumo(nome, total, presentes, ausentes, justificadas, percentual);
    }

    private long contar(List<Presenca> presencas, StatusPresenca status) {
        return presencas.stream().filter(p -> p.getStatus() == status).count();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }

    private final class Periodos {

        private final Atividade turma;
        private String nomeParticipante;
        private long aulasPrevistas;
        private long presencas;
        private long justificadas;
        private LocalDate primeiroDia;
        private LocalDate ultimoDia;
        private boolean saiu = true;

        private Periodos(Atividade turma) {
            this.turma = turma;
        }

        private void somar(Matricula matricula, long aulas, long presentes, long avisadas,
                LocalDate inicio, LocalDate fim) {
            nomeParticipante = matricula.getParticipante().getNome();
            aulasPrevistas += aulas;
            presencas += presentes;
            justificadas += avisadas;
            LocalDate de = matricula.inicioNoPeriodo(inicio);
            LocalDate ate = matricula.fimNoPeriodo(fim);
            primeiroDia = primeiroDia == null || de.isBefore(primeiroDia) ? de : primeiroDia;
            ultimoDia = ultimoDia == null || ate.isAfter(ultimoDia) ? ate : ultimoDia;
            if (matricula.isAtiva()) {
                saiu = false;
            }
        }

        private FrequenciaMensal montar(Long participanteId, String nome, YearMonth mes) {
            List<String> partes = new ArrayList<>();
            if (primeiroDia.isAfter(mes.atDay(1))) {
                partes.add("desde o ingresso em " + primeiroDia.format(DIA_MES));
            }
            if (saiu && ultimoDia.isBefore(mes.atEndOfMonth())) {
                partes.add("até a saída em " + ultimoDia.format(DIA_MES));
            }
            String observacao = partes.isEmpty() ? null : "Conta só as aulas " + String.join(" e ", partes) + ".";
            return FrequenciaService.this.montar(participanteId, nome, turma.getId(), turma.getNome(), mes,
                    aulasPrevistas, presencas, justificadas, observacao);
        }
    }
}
