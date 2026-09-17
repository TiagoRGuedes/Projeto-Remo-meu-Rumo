package com.ibmec.remomeurumo.controller;

import java.time.LocalTime;
import java.time.LocalDate;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.ibmec.remomeurumo.service.DashboardService;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;

@Controller
public class HomeController {

    private final DashboardService dashboardService;
    private final HistoricoOperacaoService historicoService;

    public HomeController(DashboardService dashboardService, HistoricoOperacaoService historicoService) {
        this.dashboardService = dashboardService;
        this.historicoService = historicoService;
    }

    @GetMapping("/")
    public String home(Model model) {
        int hora = LocalTime.now().getHour();
        String saudacao = hora < 12 ? "Bom dia" : hora < 18 ? "Boa tarde" : "Boa noite";
        model.addAttribute("saudacao", saudacao);
        LocalDate hoje = LocalDate.now();
        model.addAttribute("inicioMes", hoje.withDayOfMonth(1));
        model.addAttribute("fimMes", hoje.withDayOfMonth(hoje.lengthOfMonth()));
        model.addAttribute("indicadores", dashboardService.carregarIndicadores());
        model.addAttribute("ultimoEncontro", dashboardService.carregarUltimoEncontro());
        model.addAttribute("encontrosRecentes", dashboardService.carregarEncontrosRecentes());
        model.addAttribute("encontrosSemPresenca", dashboardService.contarEncontrosSemPresenca());
        model.addAttribute("ultimasAlteracoes", historicoService.ultimasAlteracoes());
        return "home";
    }
}
