package com.ibmec.remomeurumo.controller;

import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.ibmec.remomeurumo.service.AcessibilidadeService;
import com.ibmec.remomeurumo.service.AcessibilidadeService.PreferenciasAcessibilidade;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;

@ControllerAdvice
public class GlobalModelAdvice {

    private final AcessibilidadeService acessibilidadeService;

    public GlobalModelAdvice(AcessibilidadeService acessibilidadeService) {
        this.acessibilidadeService = acessibilidadeService;
    }

    @ModelAttribute("preferenciasAcessibilidade")
    public PreferenciasAcessibilidade preferenciasAcessibilidade(HttpSession session) {
        return acessibilidadeService.obter(session);
    }

    @ModelAttribute("temaClasses")
    public String temaClasses(HttpSession session) {
        return acessibilidadeService.obter(session).classesCss();
    }

    @ModelAttribute("rotaAtual")
    public String rotaAtual(HttpServletRequest request) {
        return request.getRequestURI();
    }
}
