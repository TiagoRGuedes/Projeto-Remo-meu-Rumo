package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.support.MesReferencia;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.FrequenciaService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

@Controller
public class FrequenciaWebController {

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
            Model model) {
        var referencia = MesReferencia.resolver(mes);
        model.addAttribute("mesReferencia", referencia.toString());
        model.addAttribute("resumosMensais", frequenciaService.resumoMensalDasTurmas(referencia));
        model.addAttribute("linhasMensais", atividadeId == null ? List.of()
                : frequenciaService.linhasMensaisDaTurma(atividadeId, referencia));
        model.addAttribute("limiteFaltas", FrequenciaService.LIMITE_FALTAS_MES);
        model.addAttribute("metaFrequencia", FrequenciaService.META_FREQUENCIA_MENSAL);
        model.addAttribute("participantes", participanteService.listar(null));
        model.addAttribute("atividades", atividadeService.listar(null));
        model.addAttribute("statusPresenca", StatusPresenca.values());
        var historico = presencaService.listarHistorico(participanteId, atividadeId, inicio, fim, status);
        model.addAttribute("historico", historico);
        model.addAttribute("frequencias", frequenciaService.porParticipante(atividadeId, inicio, fim));
        model.addAttribute("resumoFrequencia", frequenciaService.resumirDashboard(historico));
        model.addAttribute("participanteId", participanteId);
        model.addAttribute("atividadeId", atividadeId);
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("status", status);
        return "frequencia/index";
    }
}
