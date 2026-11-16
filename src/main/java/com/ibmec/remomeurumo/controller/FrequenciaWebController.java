package com.ibmec.remomeurumo.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.dto.LinhaFrequenciaParticipante;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.support.MesReferencia;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.FrequenciaService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

@Controller
public class FrequenciaWebController {

    private static final Locale PT_BR = Locale.forLanguageTag("pt-BR");

    private static final List<String> TOPICOS = List.of("participantes", "contatar", "turmas", "registros");

    private final ParticipanteService participanteService;
    private final AtividadeService atividadeService;
    private final PresencaService presencaService;
    private final FrequenciaService frequenciaService;

    public FrequenciaWebController(ParticipanteService participanteService,
            AtividadeService atividadeService,
            PresencaService presencaService,
            FrequenciaService frequenciaService) {
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.presencaService = presencaService;
        this.frequenciaService = frequenciaService;
    }

    @GetMapping("/frequencia")
    public String frequencia(@RequestParam(required = false) Long participanteId,
            @RequestParam(required = false) Long atividadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) StatusPresenca status,
            @RequestParam(required = false) String mes,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Long turmaId,
            @RequestParam(required = false) String topico,
            Model model) {
        String topicoAtual = topico != null && TOPICOS.contains(topico) ? topico
                : participanteId != null || atividadeId != null || inicio != null || fim != null || status != null
                        ? "registros" : "participantes";
        model.addAttribute("topico", topicoAtual);
        var referencia = MesReferencia.resolver(mes);
        boolean anual = "ano".equals(periodo);
        int anoAtual = LocalDate.now().getYear();
        model.addAttribute("mesReferencia", referencia.toString());
        model.addAttribute("anual", anual);
        model.addAttribute("anoAtual", anoAtual);
        model.addAttribute("turmaId", turmaId);
        model.addAttribute("mesesDisponiveis", frequenciaService.mesesDisponiveis(referencia));
        model.addAttribute("turmasAtivas", atividadeService.listarAtivas());
        model.addAttribute("rotuloPeriodo", anual ? "Ano " + anoAtual : rotuloMes(referencia));
        var resumos = anual ? frequenciaService.resumoAnualDasTurmas(anoAtual)
                : frequenciaService.resumoMensalDasTurmas(referencia);
        var linhasPainel = frequenciaService.painelPorParticipante(anual, referencia, turmaId);
        var linhasContatar = linhasPainel.stream()
                .filter(linha -> linha.contatoNecessario() || linha.abaixoDaMeta())
                .toList();
        model.addAttribute("resumosMensais", resumos);
        model.addAttribute("linhasExibidas", "contatar".equals(topicoAtual) ? linhasContatar : linhasPainel);
        model.addAttribute("totalPainel", linhasPainel.size());
        model.addAttribute("totalContatar", linhasContatar.size());
        model.addAttribute("totalTurmas", resumos.size());
        model.addAttribute("limiteFaltas", FrequenciaService.LIMITE_FALTAS_MES);
        model.addAttribute("metaFrequencia", FrequenciaService.META_FREQUENCIA_MENSAL);
        model.addAttribute("participantes", participanteService.listar(null));
        model.addAttribute("atividades", atividadeService.listar(null));
        model.addAttribute("statusPresenca", StatusPresenca.values());
        var historico = presencaService.listarHistorico(participanteId, atividadeId, inicio, fim, status);
        model.addAttribute("historico", historico);
        model.addAttribute("totalRegistros", historico.size());
        model.addAttribute("resumoFrequencia", frequenciaService.resumirDashboard(historico));
        model.addAttribute("participanteId", participanteId);
        model.addAttribute("atividadeId", atividadeId);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("status", status);
        return "frequencia/index";
    }

    @GetMapping("/frequencia/planilha.csv")
    public ResponseEntity<String> planilha(@RequestParam(required = false) String mes,
            @RequestParam(required = false) String periodo,
            @RequestParam(required = false) Long turmaId) {
        var referencia = MesReferencia.resolver(mes);
        boolean anual = "ano".equals(periodo);
        String rotulo = anual ? String.valueOf(LocalDate.now().getYear()) : referencia.toString();
        StringBuilder csv = new StringBuilder("\uFEFFparticipante;turmas;periodo;presencas;previstas;faltas;"
                + "faltas_avisadas;frequencia_percentual;presencas_no_ano;previstas_no_ano;situacao\n");
        for (LinhaFrequenciaParticipante linha : frequenciaService.painelPorParticipante(anual, referencia, turmaId)) {
            csv.append(celula(linha.participante())).append(';')
                    .append(celula(linha.turmas())).append(';')
                    .append(rotulo).append(';')
                    .append(linha.presencas()).append(';')
                    .append(linha.previstas()).append(';')
                    .append(linha.faltas()).append(';')
                    .append(linha.faltasAvisadas()).append(';')
                    .append(String.valueOf(linha.percentual()).replace('.', ',')).append(';')
                    .append(linha.presencasAno()).append(';')
                    .append(linha.previstasAno()).append(';')
                    .append(celula(situacao(linha))).append('\n');
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment()
                .filename("frequencia-" + rotulo + ".csv").build());
        return ResponseEntity.ok().headers(headers).body(csv.toString());
    }

    private String situacao(LinhaFrequenciaParticipante linha) {
        if (linha.semAulasPrevistas()) {
            return "Sem aulas";
        }
        if (linha.contatoNecessario()) {
            return "Contatar família";
        }
        return linha.abaixoDaMeta() ? "Abaixo da meta" : "Em dia";
    }

    private String celula(String texto) {
        return com.ibmec.remomeurumo.support.Csv.celula(texto);
    }

    private String rotuloMes(YearMonth mes) {
        String nome = mes.getMonth().getDisplayName(TextStyle.FULL, PT_BR);
        return Character.toUpperCase(nome.charAt(0)) + nome.substring(1) + " de " + mes.getYear();
    }
}
