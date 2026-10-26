package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.dto.ConfirmacaoOperacao;
import com.ibmec.remomeurumo.dto.TipoOperacaoConfirmacao;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.MotivoSaida;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.MatriculaService;
import com.ibmec.remomeurumo.service.ParticipanteService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/matriculas")
public class MatriculaWebController {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final MatriculaService matriculaService;
    private final ParticipanteService participanteService;
    private final AtividadeService atividadeService;
    private final ConfirmacaoOperacaoService confirmacaoService;

    public MatriculaWebController(MatriculaService matriculaService,
            ParticipanteService participanteService,
            AtividadeService atividadeService,
            ConfirmacaoOperacaoService confirmacaoService) {
        this.matriculaService = matriculaService;
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.confirmacaoService = confirmacaoService;
    }

    @PostMapping("/nova")
    public String matricular(@RequestParam Long participanteId,
            @RequestParam Long turmaId,
            @RequestParam(required = false) String dataInicio,
            HttpSession session) {
        Participante participante = participanteService.obter(participanteId);
        Atividade turma = atividadeService.obter(turmaId);
        LocalDate inicio = dataInicio == null || dataInicio.isBlank()
                ? LocalDate.now() : LocalDate.parse(dataInicio);

        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("participanteId", participanteId.toString());
        dados.put("turmaId", turmaId.toString());
        dados.put("dataInicio", inicio.toString());

        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.MATRICULAR_PARTICIPANTE, participanteId, "Nova matrícula",
                "Matricular " + participante.getNome() + "?",
                "A matrícula define em qual turma a presença desta pessoa passa a ser esperada.",
                participante.getNome(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Turma", null, turma.getNome()),
                        confirmacaoService.alteracao("Grade", null, turma.getGradeResumo()),
                        confirmacaoService.alteracao("Início", null, inicio.toString())),
                List.of("A pessoa passará a contar nas aulas previstas desta turma.",
                        "A frequência mensal dela será calculada sobre essas aulas.",
                        "A matrícula ficará registrada no Histórico."),
                dados, null, "/participantes/" + participanteId, "/participantes/{id}",
                "Matrícula criada. A frequência desta turma já considera o participante.",
                "Confirmar matrícula", false);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/encerrar")
    public String encerrar(@PathVariable Long id,
            @RequestParam MotivoSaida motivoSaida,
            @RequestParam(required = false) String observacaoSaida,
            @RequestParam(required = false) String dataSaida,
            HttpSession session) {
        Matricula matricula = matriculaService.obter(id);
        LocalDate saida = dataSaida == null || dataSaida.isBlank()
                ? LocalDate.now() : LocalDate.parse(dataSaida);

        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("motivoSaida", motivoSaida.name());
        dados.put("observacaoSaida", Objects.toString(observacaoSaida, ""));
        dados.put("dataSaida", saida.toString());

        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.ENCERRAR_MATRICULA, id, "Registrar saída",
                "Registrar a saída de " + matricula.getParticipante().getNome() + "?",
                "O histórico de presença permanece; a pessoa deixa de contar nas próximas aulas da turma.",
                matricula.getParticipante().getNome(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Turma", matricula.getAtividade().getNome(),
                                matricula.getAtividade().getNome()),
                        confirmacaoService.alteracao("Situação", "Ativa", "Encerrada"),
                        confirmacaoService.alteracao("Motivo", null, motivoSaida.getDescricao()),
                        confirmacaoService.alteracao("Data da saída", null, saida.toString())),
                List.of("O histórico de presença anterior será preservado.",
                        "A pessoa sai das próximas listas de presença desta turma.",
                        "Se ela voltar, a mesma matrícula pode ser reativada.",
                        "A saída ficará registrada no Histórico."),
                dados, null, "/participantes/" + matricula.getParticipante().getId(),
                "/participantes/{id}",
                "Saída registrada. O histórico de presença foi preservado.",
                "Confirmar saída", true);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/trocar")
    public String trocar(@PathVariable Long id, @RequestParam Long novaTurmaId, HttpSession session) {
        Matricula matricula = matriculaService.obter(id);
        Atividade novaTurma = atividadeService.obter(novaTurmaId);
        LocalDate hoje = LocalDate.now();

        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("novaTurmaId", novaTurmaId.toString());
        dados.put("dataTroca", hoje.toString());

        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.TROCAR_TURMA, id, "Trocar turma",
                "Trocar a turma de " + matricula.getParticipante().getNome() + "?",
                "A pessoa sai da turma atual e entra na nova a partir de hoje.",
                matricula.getParticipante().getNome(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Turma", matricula.getAtividade().getNome(), novaTurma.getNome()),
                        confirmacaoService.alteracao("Grade", matricula.getAtividade().getGradeResumo(),
                                novaTurma.getGradeResumo()),
                        confirmacaoService.alteracao("A partir de", null, hoje.format(DATA))),
                List.of("As presenças já registradas continuam no histórico.",
                        "A partir de hoje a frequência conta as aulas da nova turma.",
                        "A troca ficará registrada no Histórico."),
                dados, null, "/participantes/" + matricula.getParticipante().getId(),
                "/participantes/{id}",
                "Turma trocada. A frequência deste mês conta cada turma só no período em que a pessoa esteve nela.",
                "Confirmar troca", false);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/reativar")
    public String reativar(@PathVariable Long id, HttpSession session) {
        Matricula matricula = matriculaService.obter(id);
        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.REATIVAR_MATRICULA, id, "Retorno à turma",
                "Reativar a matrícula de " + matricula.getParticipante().getNome() + "?",
                "O retorno reabre a matrícula existente, sem criar um cadastro novo.",
                matricula.getParticipante().getNome(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Turma", matricula.getAtividade().getNome(),
                                matricula.getAtividade().getNome()),
                        confirmacaoService.alteracao("Situação", "Encerrada", "Ativa"),
                        confirmacaoService.alteracao("Motivo da saída",
                                matricula.getMotivoSaidaDescricao(), "Removido")),
                List.of("Todo o histórico de presença anterior continua vinculado a esta matrícula.",
                        "A pessoa volta a aparecer nas listas de presença da turma.",
                        "O retorno ficará registrado no Histórico."),
                Map.of(), null, "/participantes/" + matricula.getParticipante().getId(),
                "/participantes/{id}",
                "Matrícula reativada. O histórico de presença anterior foi mantido.",
                "Confirmar retorno", false);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }
}
