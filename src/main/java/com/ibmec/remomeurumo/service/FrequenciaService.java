package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.dto.FrequenciaMensalParticipante;
import com.ibmec.remomeurumo.dto.FrequenciaResumo;
import com.ibmec.remomeurumo.dto.LinhaFrequenciaParticipante;
import com.ibmec.remomeurumo.dto.MesDisponivel;
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

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private final int mesInicioAnoLetivo;

    private final PresencaRepository presencaRepository;
    private final EncontroRepository encontroRepository;
    private final MatriculaRepository matriculaRepository;
    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;

    public FrequenciaService(PresencaRepository presencaRepository,
            EncontroRepository encontroRepository,
            MatriculaRepository matriculaRepository,
            ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository,
            @Value("${app.ano-letivo.mes-inicio:2}") int mesInicioAnoLetivo) {
        this.mesInicioAnoLetivo = mesInicioAnoLetivo;
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
        return consolidar(participanteId, participante.getNome(),
                matriculaRepository.listarPorParticipante(participanteId),
                mes.atDay(1), limiteDoMes(mes), mes.atEndOfMonth(), mes.toString());
    }

    @Transactional(readOnly = true)
    public FrequenciaMensalParticipante frequenciaAnualDoParticipante(Long participanteId, int ano) {
        var participante = participanteRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
        return consolidar(participanteId, participante.getNome(),
                matriculaRepository.listarPorParticipante(participanteId),
                LocalDate.of(ano, 1, 1), limiteDoAno(ano), LocalDate.of(ano, 12, 31), String.valueOf(ano));
    }

    @Transactional(readOnly = true)
    public List<LinhaFrequenciaParticipante> painelPorParticipante(boolean anual, YearMonth mes, Long turmaId) {
        int ano = anual ? LocalDate.now().getYear() : mes.getYear();
        LocalDate inicio = anual ? LocalDate.of(ano, 1, 1) : mes.atDay(1);
        LocalDate fim = anual ? limiteDoAno(ano) : limiteDoMes(mes);
        LocalDate fimNatural = anual ? LocalDate.of(ano, 12, 31) : mes.atEndOfMonth();
        String rotulo = anual ? String.valueOf(ano) : mes.toString();
        LocalDate inicioAno = LocalDate.of(ano, 1, 1);

        Map<Long, List<Matricula>> porParticipante = new LinkedHashMap<>();
        for (Matricula matricula : matriculaRepository.listarTodas()) {
            porParticipante.computeIfAbsent(matricula.getParticipante().getId(), id -> new ArrayList<>()).add(matricula);
        }
        List<LinhaFrequenciaParticipante> linhas = new ArrayList<>();
        for (Map.Entry<Long, List<Matricula>> entrada : porParticipante.entrySet()) {
            List<Matricula> matriculas = entrada.getValue();
            List<Matricula> vigentes = matriculas.stream()
                    .filter(matricula -> matricula.vigenteNoPeriodo(inicio, fim))
                    .toList();
            if (vigentes.isEmpty() || turmaId != null && vigentes.stream()
                    .noneMatch(matricula -> matricula.getAtividade().getId().equals(turmaId))) {
                continue;
            }
            String nome = matriculas.get(0).getParticipante().getNome();
            FrequenciaMensalParticipante periodo = consolidar(entrada.getKey(), nome, matriculas,
                    inicio, fim, fimNatural, rotulo);
            FrequenciaMensalParticipante doAno = anual ? periodo : consolidar(entrada.getKey(), nome, matriculas,
                    inicioAno, limiteDoAno(ano), LocalDate.of(ano, 12, 31), String.valueOf(ano));
            FrequenciaMensal total = periodo.consolidado();
            List<String> listaTurmas = vigentes.stream()
                    .map(Matricula::getAtividade)
                    .distinct()
                    .sorted(Atividade.ordemDaGrade())
                    .map(Atividade::getNome)
                    .toList();
            String observacao = periodo.porTurma().stream()
                    .filter(linha -> linha.observacao() != null)
                    .map(linha -> periodo.porTurma().size() > 1 ? linha.turma() + ": " + linha.observacao() : linha.observacao())
                    .collect(Collectors.joining(" "));
            linhas.add(new LinhaFrequenciaParticipante(entrada.getKey(), nome, String.join(" · ", listaTurmas),
                    listaTurmas,
                    total.presencas(), total.aulasPrevistas(), total.faltas(), total.faltasJustificadas(),
                    total.percentual(), doAno.consolidado().presencas(), doAno.consolidado().aulasPrevistas(),
                    !anual && total.contatoNecessario(), total.abaixoDaMeta(),
                    observacao.isBlank() ? null : observacao));
        }
        linhas.sort(Comparator.comparing(LinhaFrequenciaParticipante::participante));
        return linhas;
    }

    @Transactional(readOnly = true)
    public List<MesDisponivel> mesesDisponiveis(YearMonth selecionado) {
        YearMonth atual = YearMonth.now();
        YearMonth inicioAnoLetivo = YearMonth.of(atual.getYear(), mesInicioAnoLetivo);
        LocalDate primeiroRegistro = primeiraData(encontroRepository.primeiraData(), matriculaRepository.primeiroInicio());
        YearMonth inicio = primeiroRegistro == null ? atual : YearMonth.from(primeiroRegistro);
        if (inicio.isBefore(inicioAnoLetivo)) {
            inicio = inicioAnoLetivo;
        }
        if (inicio.isAfter(atual)) {
            inicio = atual;
        }
        if (selecionado != null && selecionado.isBefore(inicio)) {
            inicio = selecionado;
        }
        boolean variosAnos = inicio.getYear() != atual.getYear();
        List<MesDisponivel> meses = new ArrayList<>();
        for (YearMonth mes = inicio; !mes.isAfter(atual); mes = mes.plusMonths(1)) {
            String nome = mes.getMonth().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
            String rotulo = Character.toUpperCase(nome.charAt(0)) + nome.substring(1)
                    + (variosAnos ? "/" + String.valueOf(mes.getYear()).substring(2) : "");
            meses.add(new MesDisponivel(mes.toString(), rotulo));
        }
        return meses;
    }

    private LocalDate primeiraData(LocalDate primeira, LocalDate segunda) {
        if (primeira == null) {
            return segunda;
        }
        return segunda == null || primeira.isBefore(segunda) ? primeira : segunda;
    }

    public LocalDate limiteDoAno(int ano) {
        LocalDate fim = LocalDate.of(ano, 12, 31);
        LocalDate hoje = LocalDate.now();
        return fim.isAfter(hoje) ? hoje : fim;
    }

    private FrequenciaMensalParticipante consolidar(Long participanteId, String nome, List<Matricula> matriculas,
            LocalDate inicio, LocalDate fim, LocalDate fimNatural, String rotulo) {
        Map<Long, Periodos> porTurmaId = new LinkedHashMap<>();
        for (Matricula matricula : matriculas) {
            if (!matricula.vigenteNoPeriodo(inicio, fim)) {
                continue;
            }
            FrequenciaMensal parcial = calcular(matricula, rotulo, inicio, fim);
            porTurmaId.computeIfAbsent(matricula.getAtividade().getId(), id -> new Periodos(matricula.getAtividade()))
                    .somar(matricula, parcial.aulasPrevistas(), parcial.presencas(), parcial.faltasJustificadas(), inicio, fim);
        }
        List<FrequenciaMensal> porTurma = new ArrayList<>();
        long totalPrevistas = 0;
        long totalPresencas = 0;
        long totalJustificadas = 0;
        for (Periodos periodos : porTurmaId.values()) {
            FrequenciaMensal linha = periodos.montar(participanteId, nome, rotulo, inicio, fimNatural);
            porTurma.add(linha);
            totalPrevistas += linha.aulasPrevistas();
            totalPresencas += linha.presencas();
            totalJustificadas += linha.faltasJustificadas();
        }
        FrequenciaMensal consolidado = montar(participanteId, nome, null, null,
                rotulo, totalPrevistas, totalPresencas, totalJustificadas);
        return new FrequenciaMensalParticipante(consolidado, porTurma);
    }

    @Transactional(readOnly = true)
    public ResumoMensalTurma resumoMensalDaTurma(Long turmaId, YearMonth mes) {
        return resumoDaTurma(obterTurma(turmaId), mes.toString(), mes.atDay(1), limiteDoMes(mes), mes.atEndOfMonth());
    }

    @Transactional(readOnly = true)
    public List<ResumoMensalTurma> resumoAnualDasTurmas(int ano) {
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .sorted(Atividade.ordemDaGrade())
                .map(turma -> resumoDaTurma(turma, String.valueOf(ano), LocalDate.of(ano, 1, 1), limiteDoAno(ano),
                        LocalDate.of(ano, 12, 31)))
                .toList();
    }

    private ResumoMensalTurma resumoDaTurma(Atividade turma, String rotulo, LocalDate inicio, LocalDate fim,
            LocalDate fimNatural) {
        Long turmaId = turma.getId();
        List<FrequenciaMensal> linhas = linhasDoPeriodo(turma, rotulo, inicio, fim, fimNatural);

        long aulasPrevistas = encontroRepository.countByAtividadeIdAndStatusNotAndDataBetween(
                turmaId, StatusEncontro.CANCELADO, inicio, fim) - (aulaDeHojeSemChamada(turmaId, inicio, fim) ? 1 : 0);
        long presencasRegistradas = linhas.stream().mapToLong(FrequenciaMensal::presencas).sum();
        long presencasEsperadas = linhas.stream().mapToLong(FrequenciaMensal::aulasPrevistas).sum();
        double percentual = presencasEsperadas == 0 ? 0
                : arredondar(presencasRegistradas * 100.0 / presencasEsperadas);
        List<FrequenciaMensal> faltosos = linhas.stream()
                .filter(FrequenciaMensal::contatoNecessario)
                .sorted(Comparator.comparingLong(FrequenciaMensal::faltas).reversed()
                        .thenComparing(FrequenciaMensal::participante))
                .toList();

        return new ResumoMensalTurma(turma.getId(), turma.getNome(), turma.getGradeResumo(), rotulo,
                aulasPrevistas, linhas.size(), presencasEsperadas, presencasRegistradas, percentual,
                presencasEsperadas > 0 && percentual < META_FREQUENCIA_MENSAL, faltosos);
    }

    @Transactional(readOnly = true)
    public List<FrequenciaMensal> linhasMensaisDaTurma(Long turmaId, YearMonth mes) {
        return linhasMensaisDaTurma(obterTurma(turmaId), mes);
    }

    @Transactional(readOnly = true)
    public List<FrequenciaMensal> linhasAnuaisDaTurma(Long turmaId, int ano) {
        return linhasDoPeriodo(obterTurma(turmaId), String.valueOf(ano), LocalDate.of(ano, 1, 1), limiteDoAno(ano),
                LocalDate.of(ano, 12, 31));
    }

    @Transactional(readOnly = true)
    public List<FrequenciaMensal> faltososDaTurma(Long turmaId, YearMonth mes) {
        return resumoMensalDaTurma(turmaId, mes).faltosos();
    }

    @Transactional(readOnly = true)
    public List<ResumoMensalTurma> resumoMensalDasTurmas(YearMonth mes) {
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .sorted(Atividade.ordemDaGrade())
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
        for (FrequenciaMensal linha : linhasDoPeriodo(obterTurma(turmaId), mes.toString(), mes.atDay(1),
                data.minusDays(1), mes.atEndOfMonth())) {
            faltas.put(linha.participanteId(), linha.faltas());
        }
        return faltas;
    }

    private List<FrequenciaMensal> linhasMensaisDaTurma(Atividade turma, YearMonth mes) {
        return linhasDoPeriodo(turma, mes.toString(), mes.atDay(1), limiteDoMes(mes), mes.atEndOfMonth());
    }

    private List<FrequenciaMensal> linhasDoPeriodo(Atividade turma, String rotulo, LocalDate inicio, LocalDate fim,
            LocalDate fimNatural) {
        boolean hojeSemChamada = aulaDeHojeSemChamada(turma.getId(), inicio, fim);
        LocalDate hoje = LocalDate.now();
        List<Encontro> aulas = encontroRepository.findByAtividadeIdAndStatusNotAndDataBetweenOrderByDataAsc(
                turma.getId(), StatusEncontro.CANCELADO, inicio, fim).stream()
                .filter(aula -> !(hojeSemChamada && aula.getData().equals(hoje)))
                .toList();
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
            linhas.add(entrada.getValue().montar(entrada.getKey(), entrada.getValue().nomeParticipante, rotulo,
                    inicio, fimNatural));
        }
        linhas.sort(Comparator.comparing(FrequenciaMensal::participante));
        return linhas;
    }

    private FrequenciaMensal calcular(Matricula matricula, String rotulo, LocalDate inicio, LocalDate fim) {
        Atividade turma = matricula.getAtividade();
        Long participanteId = matricula.getParticipante().getId();
        LocalDate de = matricula.inicioNoPeriodo(inicio);
        LocalDate ate = matricula.fimNoPeriodo(fim);
        long aulasPrevistas = encontroRepository.countByAtividadeIdAndStatusNotAndDataBetween(
                turma.getId(), StatusEncontro.CANCELADO, de, ate) - (aulaDeHojeSemChamada(turma.getId(), de, ate) ? 1 : 0);
        long presencas = presencaRepository.contarPorParticipanteTurmaEStatus(
                participanteId, turma.getId(), StatusPresenca.PRESENTE, de, ate);
        long justificadas = presencaRepository.contarPorParticipanteTurmaEStatus(
                participanteId, turma.getId(), StatusPresenca.JUSTIFICADA, de, ate);
        return montar(participanteId, matricula.getParticipante().getNome(), turma.getId(), turma.getNome(),
                rotulo, aulasPrevistas, presencas, justificadas);
    }

    private boolean aulaDeHojeSemChamada(Long turmaId, LocalDate de, LocalDate ate) {
        LocalDate hoje = LocalDate.now();
        if (!dentro(hoje, de, ate)) {
            return false;
        }
        return encontroRepository.findByAtividadeIdAndData(turmaId, hoje)
                .filter(aula -> aula.getStatus() != StatusEncontro.CANCELADO)
                .map(aula -> !presencaRepository.existsByEncontroId(aula.getId()))
                .orElse(false);
    }

    private boolean dentro(LocalDate data, LocalDate de, LocalDate ate) {
        return !data.isBefore(de) && !data.isAfter(ate);
    }

    private FrequenciaMensal montar(Long participanteId, String nomeParticipante, Long turmaId, String nomeTurma,
            String rotulo, long aulasPrevistas, long presencas, long justificadas) {
        return montar(participanteId, nomeParticipante, turmaId, nomeTurma, rotulo, aulasPrevistas, presencas,
                justificadas, null);
    }

    private FrequenciaMensal montar(Long participanteId, String nomeParticipante, Long turmaId, String nomeTurma,
            String rotulo, long aulasPrevistas, long presencas, long justificadas, String observacao) {
        long presencasConsideradas = Math.min(presencas, aulasPrevistas);
        long faltas = Math.max(0, aulasPrevistas - presencasConsideradas);
        long justificadasConsideradas = Math.min(justificadas, faltas);
        double percentual = aulasPrevistas == 0 ? 0
                : arredondar(presencasConsideradas * 100.0 / aulasPrevistas);
        return new FrequenciaMensal(participanteId, nomeParticipante, turmaId, nomeTurma, rotulo,
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

        private FrequenciaMensal montar(Long participanteId, String nome, String rotulo, LocalDate inicio,
                LocalDate fimNatural) {
            List<String> partes = new ArrayList<>();
            if (primeiroDia.isAfter(inicio)) {
                partes.add("desde o ingresso em " + primeiroDia.format(DIA_MES));
            }
            if (saiu && ultimoDia.isBefore(fimNatural)) {
                partes.add("até a saída em " + ultimoDia.format(DIA_MES));
            }
            String observacao = partes.isEmpty() ? null : "Conta só as aulas " + String.join(" e ", partes) + ".";
            return FrequenciaService.this.montar(participanteId, nome, turma.getId(), turma.getNome(), rotulo,
                    aulasPrevistas, presencas, justificadas, observacao);
        }
    }
}
