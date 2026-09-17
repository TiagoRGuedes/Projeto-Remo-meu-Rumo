package com.ibmec.remomeurumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.service.AcessibilidadeService;
import com.ibmec.remomeurumo.service.AcessibilidadeService.PreferenciasAcessibilidade;

import jakarta.servlet.http.HttpSession;

@Controller
public class AcessibilidadeWebController {

    private final AcessibilidadeService acessibilidadeService;

    public AcessibilidadeWebController(AcessibilidadeService acessibilidadeService) {
        this.acessibilidadeService = acessibilidadeService;
    }

    @GetMapping("/acessibilidade")
    public String acessibilidade() {
        return "acessibilidade/index";
    }

    @PostMapping("/acessibilidade")
    public String salvar(@RequestParam(defaultValue = "normal") String tamanhoFonte,
            @RequestParam(defaultValue = "false") boolean altoContraste,
            @RequestParam(defaultValue = "false") boolean modoEscuro,
            @RequestParam(defaultValue = "false") boolean reduzirAnimacoes,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        acessibilidadeService.salvar(session, new PreferenciasAcessibilidade(
                normalizarFonte(tamanhoFonte),
                altoContraste,
                modoEscuro,
                reduzirAnimacoes));
        redirectAttributes.addFlashAttribute("sucesso", "Preferências de acessibilidade salvas.");
        return "redirect:/acessibilidade";
    }

    private String normalizarFonte(String tamanhoFonte) {
        return switch (tamanhoFonte) {
            case "grande", "muito-grande" -> tamanhoFonte;
            default -> "normal";
        };
    }
}
