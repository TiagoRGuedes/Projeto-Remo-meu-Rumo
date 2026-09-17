package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.HistoricoOperacao;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;

@Controller
public class HistoricoWebController {

    private final HistoricoOperacaoService historicoService;

    public HistoricoWebController(HistoricoOperacaoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/historico")
    public String historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) ModuloHistorico modulo,
            @RequestParam(required = false) AcaoHistorico acao,
            @RequestParam(required = false) String busca,
            Model model) {
        List<HistoricoOperacao> registros = historicoService.listar(inicio, fim, modulo, acao, busca);
        model.addAttribute("indicadores", historicoService.carregarIndicadores());
        model.addAttribute("historicoPorData", historicoService.agruparPorData(registros));
        model.addAttribute("totalResultados", registros.size());
        model.addAttribute("modulos", ModuloHistorico.values());
        model.addAttribute("acoes", AcaoHistorico.values());
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("modulo", modulo);
        model.addAttribute("acao", acao);
        model.addAttribute("busca", busca);
        return "historico/index";
    }
}
