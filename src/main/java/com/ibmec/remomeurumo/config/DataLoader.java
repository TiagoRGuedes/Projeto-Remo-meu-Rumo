package com.ibmec.remomeurumo.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.MotivoCancelamento;
import com.ibmec.remomeurumo.model.MotivoSaida;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Sexo;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.model.TipoEscola;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.HistoricoOperacaoRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.MatriculaService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

@Component
@Order(10)
public class DataLoader implements CommandLineRunner {

    private static final List<String> NOMES_FICTICIOS = List.of(
            "Ana Souza", "Bruno Lima", "Camila Rocha", "Diego Martins", "Fernanda Alves", "Gabriel Costa",
            "Helena Ramos", "João Pereira", "Larissa Monteiro", "Marcos Vieira", "Natália Azevedo",
            "Otávio Nunes", "Patrícia Cardoso", "Rafael Mendes", "Sabrina Freitas", "Thiago Batista",
            "Aline Moreira", "Caio Teixeira", "Daniela Rezende", "Eduardo Paiva", "Flávia Moraes",
            "Gustavo Barros", "Isabela Campos", "Leandro Farias", "Marina Duarte", "Nelson Ribeiro",
            "Priscila Neves", "Renan Carvalho", "Sofia Tavares", "Vinícius Andrade", "Yasmin Correia",
            "André Pires", "Beatriz Fonseca", "César Lopes", "Débora Sales", "Felipe Araújo");

    private static final List<String> OBSERVACOES_FICTICIAS = List.of(
            "Cadastro fictício para simulação do sistema.",
            "Participante demonstrativo sem dados pessoais reais.",
            "Registro criado exclusivamente para apresentação acadêmica.",
            "Perfil fictício usado para testar frequência e relatórios.");

    private final boolean dadosDemonstracao;
    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;
    private final EncontroRepository encontroRepository;
    private final PresencaRepository presencaRepository;
    private final HistoricoOperacaoRepository historicoRepository;
    private final ParticipanteService participanteService;
    private final AtividadeService atividadeService;
    private final EncontroService encontroService;
    private final PresencaService presencaService;
    private final MatriculaService matriculaService;

    private static final int SEMANAS_ANTERIORES = 9;

    private static final List<String> CONDICOES = List.of(
            "TEA (autismo)", "Síndrome de Down", "Deficiência intelectual", "Paralisia cerebral", "TDAH",
            "Deficiência auditiva", "Deficiência visual");

    public DataLoader(@Value("${app.dados-demonstracao:false}") boolean dadosDemonstracao,
            ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository,
            EncontroRepository encontroRepository,
            PresencaRepository presencaRepository,
            HistoricoOperacaoRepository historicoRepository,
            ParticipanteService participanteService,
            AtividadeService atividadeService,
            EncontroService encontroService,
            PresencaService presencaService,
            MatriculaService matriculaService) {
        this.dadosDemonstracao = dadosDemonstracao;
        this.participanteRepository = participanteRepository;
        this.atividadeRepository = atividadeRepository;
        this.encontroRepository = encontroRepository;
        this.presencaRepository = presencaRepository;
        this.historicoRepository = historicoRepository;
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
        this.presencaService = presencaService;
        this.matriculaService = matriculaService;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!dadosDemonstracao || !bancoVazio()) {
            return;
        }

        LocalDate hoje = LocalDate.now();
        LocalDate inicioDados = hoje.minusWeeks(SEMANAS_ANTERIORES);
        List<Participante> participantes = criarParticipantes();
        List<Atividade> turmas = criarTurmas();
        Map<Long, List<Participante>> matriculados = criarMatriculas(turmas, participantes, inicioDados);
        for (int indice = 0; indice < turmas.size(); indice++) {
            Atividade turma = turmas.get(indice);
            List<Encontro> aulas = criarAulas(turma, inicioDados, hoje);
            if (indice == 4) {
                registrarAulaNaoRealizada(aulas, hoje, false, MotivoCancelamento.CONDICOES_CLIMATICAS,
                        "Alerta de chuva forte da Defesa Civil.");
            }
            if (indice == 0) {
                registrarAulaNaoRealizada(aulas, hoje, true, MotivoCancelamento.FERIADO, "Feriado municipal.");
            }
            criarPresencas(aulas, matriculados.getOrDefault(turma.getId(), List.of()), indice == 1 || indice == 3);
        }

        Matricula saida = matriculaService.listarPorParticipante(participantes.get(5).getId()).get(0);
        matriculaService.encerrar(saida.getId(), MotivoSaida.MUDANCA_ROTINA_FAMILIAR,
                "Família informou mudança de horário escolar.", hoje.minusWeeks(2));
        for (int indice : List.of(34, 35)) {
            Long participanteId = participantes.get(indice).getId();
            matriculaService.listarPorParticipante(participanteId).stream()
                    .filter(Matricula::isAtiva)
                    .forEach(matricula -> matriculaService.encerrar(matricula.getId(),
                            MotivoSaida.DEDICACAO_OUTRA_ATIVIDADE, null, hoje.minusWeeks(3)));
            participanteService.desativar(participanteId);
        }
        Matricula troca = matriculaService.listarPorParticipante(participantes.get(4).getId()).stream()
                .filter(Matricula::isAtiva)
                .findFirst()
                .orElseThrow();
        Atividade novaTurma = turmas.get(6).getId().equals(troca.getAtividade().getId()) ? turmas.get(7) : turmas.get(6);
        matriculaService.trocarTurma(troca.getId(), novaTurma.getId(), hoje.minusDays(8));
    }

    private void registrarAulaNaoRealizada(List<Encontro> aulas, LocalDate hoje, boolean futura,
            MotivoCancelamento motivo, String justificativa) {
        aulas.stream()
                .filter(aula -> futura ? aula.getData().isAfter(hoje) : aula.getData().isBefore(hoje.minusDays(3)))
                .reduce((primeira, segunda) -> futura ? primeira : segunda)
                .ifPresent(aula -> encontroService.registrarAulaNaoRealizada(aula.getAtividade().getId(),
                        aula.getData(), motivo, justificativa));
    }

    private boolean bancoVazio() {
        return participanteRepository.count() == 0
                && atividadeRepository.count() == 0
                && encontroRepository.count() == 0
                && presencaRepository.count() == 0
                && historicoRepository.count() == 0;
    }

    private List<Participante> criarParticipantes() {
        LocalDateTime agora = LocalDateTime.now();
        LocalDate hoje = LocalDate.now();
        TipoEscola[] escolas = {TipoEscola.PUBLICA, TipoEscola.PUBLICA, TipoEscola.PARTICULAR, TipoEscola.NAO_ESTUDA};
        return IntStream.range(0, NOMES_FICTICIOS.size())
                .mapToObj(indice -> {
                    boolean deficiencia = indice % 5 == 4;
                    Participante participante = new Participante(
                            NOMES_FICTICIOS.get(indice),
                            indice % 2 == 0 ? Sexo.FEMININO : Sexo.MASCULINO,
                            hoje.minusYears(9L + indice % 10).minusDays(indice * 11L),
                            escolas[indice % escolas.length],
                            deficiencia,
                            deficiencia && indice != 9 ? CONDICOES.get(indice % CONDICOES.size()) : null,
                            OBSERVACOES_FICTICIAS.get(indice % OBSERVACOES_FICTICIAS.size()));
                    participante.setDataCadastro(agora.minusDays(70L + indice * 2L));
                    participante.setDataAtualizacao(participante.getDataCadastro());
                    return participanteService.cadastrar(participante);
                })
                .toList();
    }

    private List<Atividade> criarTurmas() {
        return TurmasOficiais.LISTA.stream()
                .map(turma -> atividadeService.cadastrar(turma.novaAtividade()))
                .toList();
    }

    private Map<Long, List<Participante>> criarMatriculas(List<Atividade> turmas, List<Participante> participantes,
            LocalDate inicio) {
        Map<Long, List<Participante>> porTurma = new LinkedHashMap<>();
        for (int indice = 0; indice < participantes.size(); indice++) {
            Participante participante = participantes.get(indice);
            List<Atividade> escolhidas = new ArrayList<>();
            escolhidas.add(turmas.get(indice % turmas.size()));
            if (indice % 4 == 0) {
                escolhidas.add(turmas.get((indice + 1) % turmas.size()));
            }
            if (indice % 9 == 0 && indice > 0) {
                escolhidas.add(turmas.get((indice + 4) % turmas.size()));
            }
            for (Atividade turma : escolhidas.stream().distinct().toList()) {
                matriculaService.matricular(participante.getId(), turma.getId(), inicio);
                porTurma.computeIfAbsent(turma.getId(), id -> new ArrayList<>()).add(participante);
            }
        }
        return porTurma;
    }

    private List<Encontro> criarAulas(Atividade turma, LocalDate inicio, LocalDate hoje) {
        List<Encontro> aulas = new ArrayList<>();
        LocalDate limite = hoje.plusDays(7);
        int numero = 0;
        for (LocalDate data = inicio; !data.isAfter(limite); data = data.plusDays(1)) {
            if (!turma.getDiasSemana().contains(DiaSemana.de(data.getDayOfWeek()))) {
                continue;
            }
            numero++;
            StatusEncontro status;
            if (data.isAfter(hoje)) {
                status = StatusEncontro.PLANEJADO;
            } else if (numero == 6) {
                status = StatusEncontro.CANCELADO;
            } else {
                status = StatusEncontro.REALIZADO;
            }
            String observacao = status == StatusEncontro.CANCELADO
                    ? "Aula fictícia cancelada por condição do tempo."
                    : "Aula fictícia para simulação do sistema.";
            aulas.add(encontroService.criar(turma.getId(), data, observacao, status));
        }
        return aulas;
    }

    private void criarPresencas(List<Encontro> aulas, List<Participante> turma, boolean deixarUltimaPendente) {
        LocalDate hoje = LocalDate.now();
        Encontro ultimaRealizada = aulas.stream()
                .filter(aula -> aula.getStatus() == StatusEncontro.REALIZADO && aula.getData().isBefore(hoje))
                .reduce((primeira, segunda) -> segunda)
                .orElse(null);
        int indiceAula = 0;
        for (Encontro aula : aulas) {
            indiceAula++;
            if (aula.getStatus() != StatusEncontro.REALIZADO || aula.getData().equals(hoje)
                    || deixarUltimaPendente && aula == ultimaRealizada) {
                continue;
            }
            Map<Long, StatusPresenca> situacoes = new LinkedHashMap<>();
            Map<Long, String> observacoes = new LinkedHashMap<>();
            for (int posicao = 0; posicao < turma.size(); posicao++) {
                Participante participante = turma.get(posicao);
                StatusPresenca status = statusFicticio(posicao, indiceAula);
                situacoes.put(participante.getId(), status);
                observacoes.put(participante.getId(), status == StatusPresenca.JUSTIFICADA
                        ? "Família avisou com antecedência." : "");
            }
            if (!situacoes.isEmpty()) {
                presencaService.registrarLote(aula.getId(), situacoes, observacoes);
            }
        }
    }

    private StatusPresenca statusFicticio(int posicao, int aula) {
        if (posicao == 1) {
            return StatusPresenca.AUSENTE;
        }
        if (posicao == 2) {
            return aula % 2 == 0 ? StatusPresenca.AUSENTE : StatusPresenca.PRESENTE;
        }
        int valor = posicao * 3 + aula * 5;
        if (valor % 11 == 0) {
            return StatusPresenca.AUSENTE;
        }
        if (valor % 13 == 0) {
            return StatusPresenca.JUSTIFICADA;
        }
        return StatusPresenca.PRESENTE;
    }
}
