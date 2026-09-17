package com.ibmec.remomeurumo.controller;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.dto.FrequenciaResumo;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.FrequenciaService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

@Controller
public class RelatorioWebController {

    private final FrequenciaService frequenciaService;
    private final PresencaService presencaService;
    private final ParticipanteService participanteService;
    private final AtividadeService atividadeService;
    private final EncontroService encontroService;

    public RelatorioWebController(FrequenciaService frequenciaService,
            PresencaService presencaService,
            ParticipanteService participanteService,
            AtividadeService atividadeService,
            EncontroService encontroService) {
        this.frequenciaService = frequenciaService;
        this.presencaService = presencaService;
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
    }

    @GetMapping("/relatorios")
    public String relatorios(@RequestParam(required = false) Long atividadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            Model model) {
        model.addAttribute("participantes", participanteService.listar(null));
        model.addAttribute("atividades", atividadeService.listar(null));
        model.addAttribute("encontros", encontroService.listar());
        model.addAttribute("atividadeId", atividadeId);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("frequenciaParticipantes", frequenciaService.porParticipante(atividadeId, inicio, fim));
        model.addAttribute("frequenciaAtividades", frequenciaService.porAtividade(inicio, fim));
        return "relatorios/index";
    }

    @GetMapping("/relatorios/frequencia-participantes.csv")
    public ResponseEntity<String> exportarFrequenciaParticipantes(
            @RequestParam(required = false) Long atividadeId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim) {
        List<FrequenciaResumo> dados = frequenciaService.porParticipante(atividadeId, inicio, fim);
        StringBuilder csv = new StringBuilder("nome,total_encontros,presentes,ausentes,justificadas,percentual\n");
        dados.forEach(item -> csv.append(valor(item.nome())).append(',')
                .append(item.totalEncontros()).append(',')
                .append(item.presentes()).append(',')
                .append(item.ausentes()).append(',')
                .append(item.justificadas()).append(',')
                .append(item.percentual()).append('\n'));
        return csv("frequencia-participantes.csv", csv.toString());
    }

    @GetMapping("/relatorios/encontro/{id}.csv")
    public ResponseEntity<String> exportarEncontro(@PathVariable Long id) {
        List<Presenca> presencas = presencaService.listarPorEncontro(id);
        StringBuilder csv = new StringBuilder("participante,atividade,data,status,observacoes\n");
        presencas.forEach(p -> csv.append(valor(p.getParticipante().getNome())).append(',')
                .append(valor(p.getEncontro().getAtividade().getNome())).append(',')
                .append(p.getEncontro().getData()).append(',')
                .append(p.getStatus()).append(',')
                .append(valor(p.getObservacoes())).append('\n'));
        return csv("presenca-encontro-" + id + ".csv", csv.toString());
    }

    private ResponseEntity<String> csv(String nomeArquivo, String conteudo) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(new MediaType("text", "csv", StandardCharsets.UTF_8));
        headers.setContentDisposition(ContentDisposition.attachment().filename(nomeArquivo).build());
        return ResponseEntity.ok().headers(headers).body(conteudo);
    }

    private String valor(String texto) {
        String seguro = texto == null ? "" : texto.replace("\"", "\"\"");
        return "\"" + seguro + "\"";
    }
}
