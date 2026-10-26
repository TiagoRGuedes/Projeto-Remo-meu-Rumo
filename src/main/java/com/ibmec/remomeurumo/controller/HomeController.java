package com.ibmec.remomeurumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.service.PainelInicialService;
import com.ibmec.remomeurumo.support.MesReferencia;

@Controller
public class HomeController {

    private final PainelInicialService painelInicialService;

    public HomeController(PainelInicialService painelInicialService) {
        this.painelInicialService = painelInicialService;
    }

    @GetMapping("/")
    public String home(@RequestParam(required = false) String mes, Model model) {
        model.addAttribute("painel", painelInicialService.carregar(MesReferencia.resolver(mes)));
        return "home";
    }
}
