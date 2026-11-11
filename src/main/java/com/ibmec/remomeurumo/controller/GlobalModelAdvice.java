package com.ibmec.remomeurumo.controller;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
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

    @ModelAttribute("usuarioLogado")
    public String usuarioLogado() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth == null ? "" : auth.getName();
    }

    /** Só esconde itens de menu; quem barra o acesso é o SecurityFilterChain. */
    @ModelAttribute("ehProfessor")
    public boolean ehProfessor() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_PROFESSOR"));
    }
}
