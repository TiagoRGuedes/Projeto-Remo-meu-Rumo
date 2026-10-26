package com.ibmec.remomeurumo.config;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.IntStream;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Matricula;
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
        for (Atividade turma : turmas) {
            List<Encontro> aulas = criarAulas(turma, inicioDados, hoje);
            criarPresencas(aulas, matriculados.getOrDefault(turma.getId(), List.of()));
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
        atividadeService.desativar(turmas.get(5).getId());
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
                    boolean deficiencia = indice % 9 == 4;
                    Participante participante = new Participante(
                            NOMES_FICTICIOS.get(indice),
                            indice % 2 == 0 ? Sexo.FEMININO : Sexo.MASCULINO,
                            hoje.minusYears(9L + indice % 10).minusDays(indice * 11L),
                            escolas[indice % escolas.length],
                            deficiencia,
                            deficiencia && indice % 2 == 0 ? "PASTA-" + (100 + indice) : null,
                            OBSERVACOES_FICTICIAS.get(indice % OBSERVACOES_FICTICIAS.size()));
                    participante.setDataCadastro(agora.minusDays(70L + indice * 2L));
                    participante.setDataAtualizacao(participante.getDataCadastro());
                    return participanteService.cadastrar(participante);
                })
                .toList();
    }

    private List<Atividade> criarTurmas() {
        return List.of(
                turma("Remo iniciação · manhã", "Turma fictícia de iniciação ao remo.",
                        EnumSet.of(DiaSemana.TERCA, DiaSemana.QUINTA), LocalTime.of(9, 0)),
                turma("Remo iniciação · tarde", "Turma fictícia de iniciação ao remo no contraturno.",
                        EnumSet.of(DiaSemana.TERCA, DiaSemana.QUINTA), LocalTime.of(14, 0)),
                turma("Técnica de remada", "Turma demonstrativa sem vínculo com a programação oficial.",
                        EnumSet.of(DiaSemana.SEGUNDA, DiaSemana.QUARTA), LocalTime.of(15, 30)),
                turma("Condicionamento", "Cenário fictício para testar frequência e relatórios.",
                        EnumSet.of(DiaSemana.SEXTA), LocalTime.of(10, 0)),
                turma("Remo adaptado", "Turma fictícia de remo adaptado.",
                        EnumSet.of(DiaSemana.QUARTA), LocalTime.of(9, 30)),
                turma("Turma encerrada de exemplo", "Turma fictícia desativada para demonstrar preservação histórica.",
                        EnumSet.of(DiaSemana.SABADO), LocalTime.of(8, 0)));
    }

    private Atividade turma(String nome, String descricao, Set<DiaSemana> dias, LocalTime horario) {
        return atividadeService.cadastrar(new Atividade(nome, descricao, dias, horario));
    }

    private Map<Long, List<Participante>> criarMatriculas(List<Atividade> turmas, List<Participante> participantes,
            LocalDate inicio) {
        Map<Long, List<Participante>> porTurma = new LinkedHashMap<>();
        for (int indice = 0; indice < participantes.size(); indice++) {
            Participante participante = participantes.get(indice);
            List<Atividade> escolhidas = new ArrayList<>();
            escolhidas.add(turmas.get(indice % 5));
            if (indice % 4 == 0) {
                escolhidas.add(turmas.get(3));
            }
            if (indice % 7 == 0) {
                escolhidas.add(turmas.get(5));
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

    private void criarPresencas(List<Encontro> aulas, List<Participante> turma) {
        LocalDate hoje = LocalDate.now();
        int indiceAula = 0;
        for (Encontro aula : aulas) {
            indiceAula++;
            if (aula.getStatus() != StatusEncontro.REALIZADO || aula.getData().equals(hoje)) {
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
