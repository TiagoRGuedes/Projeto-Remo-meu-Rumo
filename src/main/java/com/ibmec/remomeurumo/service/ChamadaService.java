package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.AbaChamada;
import com.ibmec.remomeurumo.dto.Chamada;
import com.ibmec.remomeurumo.dto.LinhaChamada;
import com.ibmec.remomeurumo.dto.ResultadoRegistroPresenca;
import com.ibmec.remomeurumo.dto.SessaoChamada;
import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.MotivoCancelamento;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.AnexoRepository;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.MatriculaRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class ChamadaService {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("HH'h'mm");
    private static final int DIAS_PENDENCIA = 14;

    private final AtividadeRepository atividadeRepository;
    private final EncontroRepository encontroRepository;
    private final MatriculaRepository matriculaRepository;
    private final PresencaRepository presencaRepository;
    private final EncontroService encontroService;
    private final PresencaService presencaService;
    private final FrequenciaService frequenciaService;
    private final AnexoRepository anexoRepository;

    public ChamadaService(AtividadeRepository atividadeRepository,
            EncontroRepository encontroRepository,
            MatriculaRepository matriculaRepository,
            PresencaRepository presencaRepository,
            EncontroService encontroService,
            PresencaService presencaService,
            FrequenciaService frequenciaService,
            AnexoRepository anexoRepository) {
        this.anexoRepository = anexoRepository;
        this.atividadeRepository = atividadeRepository;
        this.encontroRepository = encontroRepository;
        this.matriculaRepository = matriculaRepository;
        this.presencaRepository = presencaRepository;
        this.encontroService = encontroService;
        this.presencaService = presencaService;
        this.frequenciaService = frequenciaService;
    }

    @Transactional(readOnly = true)
    public List<SessaoChamada> sessoesDeHoje() {
        LocalDate hoje = LocalDate.now();
        LocalTime agora = LocalTime.now();
        Map<Long, Encontro> aulasDeHoje = encontroRepository.findByDataOrderByAtividadeNomeAsc(hoje).stream()
                .collect(Collectors.toMap(aula -> aula.getAtividade().getId(), Function.identity(), (a, b) -> a));
        List<SessaoChamada> sessoes = new ArrayList<>();
        for (Atividade turma : atividadeRepository.findByAtivoTrueOrderByNomeAsc()) {
            Encontro aula = aulasDeHoje.get(turma.getId());
            boolean temAulaNaGrade = turma.getDiasSemana().contains(DiaSemana.de(hoje.getDayOfWeek()));
            if (aula == null && !temAulaNaGrade) {
                continue;
            }
            if (aula != null && aula.getStatus() == StatusEncontro.CANCELADO) {
                sessoes.add(new SessaoChamada(turma.getId(), turma.getNome(), turma.getGradeResumo(), hoje, "Hoje",
                        hora(turma), aula.getId(), contarMatriculados(turma.getId(), hoje), 0, "Não aconteceu"));
                continue;
            }
            long participantes = contarMatriculados(turma.getId(), hoje);
            long registrados = aula == null ? 0 : presencaRepository.countByEncontroId(aula.getId());
            sessoes.add(new SessaoChamada(turma.getId(), turma.getNome(), turma.getGradeResumo(), hoje, "Hoje",
                    hora(turma), aula == null ? null : aula.getId(), participantes, registrados,
                    situacaoDeHoje(turma.getHorario(), agora, registrados)));
        }
        sessoes.sort(Comparator.comparing(SessaoChamada::hora));
        return sessoes;
    }

    @Transactional(readOnly = true)
    public List<SessaoChamada> pendentes() {
        LocalDate hoje = LocalDate.now();
        List<SessaoChamada> sessoes = new ArrayList<>();
        for (Encontro aula : encontroRepository.findByStatusNotAndDataBetweenOrderByDataAscAtividadeNomeAsc(
                StatusEncontro.CANCELADO, hoje.minusDays(DIAS_PENDENCIA), hoje.minusDays(1))) {
            Atividade turma = aula.getAtividade();
            if (!turma.isAtivo() || presencaRepository.countByEncontroId(aula.getId()) > 0) {
                continue;
            }
            long participantes = contarMatriculados(turma.getId(), aula.getData());
            if (participantes == 0) {
                continue;
            }
            sessoes.add(new SessaoChamada(turma.getId(), turma.getNome(), turma.getGradeResumo(), aula.getData(),
                    quando(aula.getData()), hora(turma), aula.getId(), participantes, 0, "Pendente"));
        }
        sessoes.sort(Comparator.comparing(SessaoChamada::data).reversed());
        return sessoes;
    }

    @Transactional(readOnly = true)
    public List<AbaChamada> abas() {
        LocalDate hoje = LocalDate.now();
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .sorted(Atividade.ordemDaGrade())
                .map(turma -> new AbaChamada(turma.getId(), turma.getNome(), ultimaAulaDaGrade(turma, hoje)))
                .toList();
    }

    private LocalDate ultimaAulaDaGrade(Atividade turma, LocalDate hoje) {
        if (turma.getDiasSemana() == null || turma.getDiasSemana().isEmpty()) {
            return hoje;
        }
        LocalDate data = hoje;
        while (!turma.getDiasSemana().contains(DiaSemana.de(data.getDayOfWeek()))) {
            data = data.minusDays(1);
        }
        return data;
    }

    @Transactional(readOnly = true)
    public Optional<SessaoChamada> sessaoAtual() {
        return sessoesDeHoje().stream().filter(SessaoChamada::isAgora).findFirst();
    }

    @Transactional(readOnly = true)
    public Chamada montar(Long turmaId, LocalDate data) {
        Atividade turma = obterTurma(turmaId);
        validarData(data);
        Optional<Encontro> aula = encontroRepository.findByAtividadeIdAndData(turmaId, data);
        String naoRealizada = aula.filter(encontro -> encontro.getStatus() == StatusEncontro.CANCELADO)
                .map(encontro -> encontro.getMotivoCancelamentoTexto() == null ? "Aula cancelada"
                        : encontro.getMotivoCancelamentoTexto())
                .orElse(null);
        List<Presenca> presencas = aula
                .map(encontro -> presencaRepository.findByEncontroIdOrderByParticipanteNomeAsc(encontro.getId()))
                .orElse(List.of());
        Map<Long, Presenca> porParticipante = presencas.stream()
                .collect(Collectors.toMap(presenca -> presenca.getParticipante().getId(), Function.identity()));

        Map<Long, Participante> pessoas = new LinkedHashMap<>();
        for (Matricula matricula : matriculaRepository.listarPorTurma(turmaId)) {
            if (matricula.vigenteEm(data) && matricula.getParticipante().isAtivo()) {
                pessoas.put(matricula.getParticipante().getId(), matricula.getParticipante());
            }
        }
        presencas.forEach(presenca -> pessoas.putIfAbsent(presenca.getParticipante().getId(), presenca.getParticipante()));

        Map<Long, Long> faltas = frequenciaService.faltasAntesDe(turmaId, data);
        List<Long> comAtestado = aula.map(encontro -> anexoRepository.participantesComAnexoNaAula(encontro.getId()))
                .orElse(List.of());
        List<LinhaChamada> linhas = pessoas.values().stream()
                .sorted(Comparator.comparing(Participante::getNome))
                .map(pessoa -> {
                    long faltasNoMes = faltas.getOrDefault(pessoa.getId(), 0L);
                    Presenca presenca = porParticipante.get(pessoa.getId());
                    return new LinhaChamada(pessoa.getId(), pessoa.getNome(), iniciais(pessoa.getNome()),
                            faltasNoMes, faltasNoMes >= FrequenciaService.LIMITE_FALTAS_MES,
                            presenca == null ? null : presenca.getStatus(), comAtestado.contains(pessoa.getId()));
                })
                .toList();

        LocalDateTime registradaEm = presencas.stream()
                .map(presenca -> presenca.getAtualizadoEm() != null ? presenca.getAtualizadoEm() : presenca.getRegistradoEm())
                .filter(Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);
        return new Chamada(turma.getId(), turma.getNome(), turma.getGradeResumo(), data, quandoCompleto(data),
                aula.map(Encontro::getId).orElse(null), registradaEm, naoRealizada, linhas);
    }

    @Transactional
    public ResultadoRegistroPresenca salvar(Long turmaId, LocalDate data, Map<Long, StatusPresenca> marcacoes) {
        Chamada chamada = montar(turmaId, data);
        if (chamada.isNaoRealizada()) {
            throw new RegraNegocioException("Esta aula foi registrada como não realizada. Desfaça esse registro para fazer a chamada.");
        }
        if (chamada.linhas().isEmpty()) {
            throw new RegraNegocioException("Esta turma não tem participantes matriculados nesta data.");
        }
        Map<Long, StatusPresenca> situacoes = new LinkedHashMap<>();
        for (LinhaChamada linha : chamada.linhas()) {
            StatusPresenca status = marcacoes.get(linha.participanteId());
            if (status == null) {
                throw new RegraNegocioException("Marque todos os participantes antes de salvar. Falta marcar "
                        + linha.nome() + ".");
            }
            situacoes.put(linha.participanteId(), status);
        }
        if (marcacoes.keySet().stream().anyMatch(id -> !situacoes.containsKey(id))) {
            throw new RegraNegocioException("A chamada tem uma pessoa que não pertence a esta turma. Recarregue a página.");
        }

        Long encontroId = chamada.encontroId();
        if (encontroId == null) {
            encontroId = encontroService.criar(turmaId, data, "Aula registrada pela chamada.",
                    StatusEncontro.REALIZADO).getId();
        } else {
            Encontro aula = encontroService.obter(encontroId);
            if (aula.getStatus() == StatusEncontro.PLANEJADO) {
                encontroService.atualizar(encontroId, turmaId, data, aula.getObservacoes(), StatusEncontro.REALIZADO);
            }
        }
        return presencaService.registrarLote(encontroId, situacoes, Map.of());
    }

    @Transactional
    public void registrarAulaNaoRealizada(Long turmaId, LocalDate data, MotivoCancelamento motivo, String justificativa) {
        obterTurma(turmaId);
        validarData(data);
        encontroService.registrarAulaNaoRealizada(turmaId, data, motivo, justificativa);
    }

    @Transactional
    public void desfazerAulaNaoRealizada(Long turmaId, LocalDate data) {
        Encontro aula = encontroRepository.findByAtividadeIdAndData(turmaId, data)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula não encontrada."));
        encontroService.desfazerAulaNaoRealizada(aula.getId());
    }

    private long contarMatriculados(Long turmaId, LocalDate data) {
        return matriculaRepository.listarPorTurma(turmaId).stream()
                .filter(matricula -> matricula.vigenteEm(data) && matricula.getParticipante().isAtivo())
                .map(matricula -> matricula.getParticipante().getId())
                .distinct()
                .count();
    }

    private String situacaoDeHoje(LocalTime horario, LocalTime agora, long registrados) {
        if (registrados > 0) {
            return "Registrada";
        }
        if (horario == null) {
            return "Hoje";
        }
        if (agora.isBefore(horario.minusMinutes(30))) {
            return "Mais tarde";
        }
        if (agora.isBefore(horario.plusHours(2))) {
            return "Agora";
        }
        return "Pendente";
    }

    private void validarData(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Informe a data da aula.");
        }
        if (data.isAfter(LocalDate.now())) {
            throw new RegraNegocioException("A chamada só pode ser feita no dia da aula ou depois.");
        }
    }

    private Atividade obterTurma(Long turmaId) {
        Atividade turma = atividadeRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        if (!turma.isAtivo()) {
            throw new RegraNegocioException("Esta turma está inativa e não recebe chamada.");
        }
        return turma;
    }

    private String hora(Atividade turma) {
        return turma.getHorario() == null ? "--" : turma.getHorario().format(HORA);
    }

    private String quando(LocalDate data) {
        LocalDate hoje = LocalDate.now();
        if (data.equals(hoje)) {
            return "Hoje";
        }
        if (data.equals(hoje.minusDays(1))) {
            return "Ontem";
        }
        String dia = data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        return Character.toUpperCase(dia.charAt(0)) + dia.substring(1) + ", " + data.format(DIA_MES);
    }

    private String quandoCompleto(LocalDate data) {
        String dia = data.getDayOfWeek().getDisplayName(TextStyle.SHORT, PT_BR).replace(".", "");
        String rotulo = Character.toUpperCase(dia.charAt(0)) + dia.substring(1) + ", " + data.format(DIA_MES);
        String relativo = quando(data);
        return relativo.equals("Hoje") || relativo.equals("Ontem") ? relativo + " · " + rotulo : rotulo;
    }

    private String iniciais(String nome) {
        String[] partes = nome.trim().split("\\s+");
        StringBuilder texto = new StringBuilder();
        for (int indice = 0; indice < partes.length && texto.length() < 2; indice++) {
            if (!partes[indice].isEmpty()) {
                texto.append(Character.toUpperCase(partes[indice].charAt(0)));
            }
        }
        return texto.toString();
    }
}
