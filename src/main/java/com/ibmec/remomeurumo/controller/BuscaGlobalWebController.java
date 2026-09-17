package com.ibmec.remomeurumo.controller;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.service.BuscaGlobalService;

@Controller
public class BuscaGlobalWebController {

    private final BuscaGlobalService buscaGlobalService;

    public BuscaGlobalWebController(BuscaGlobalService buscaGlobalService) {
        this.buscaGlobalService = buscaGlobalService;
    }

    @GetMapping("/busca")
    public String buscar(@RequestParam(name = "q", required = false) String consulta, Model model) {
        model.addAttribute("resultado", buscaGlobalService.buscar(consulta));
        return "busca/index";
    }
}
