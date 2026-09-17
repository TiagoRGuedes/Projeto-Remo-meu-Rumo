package com.ibmec.remomeurumo.controller;

import org.springframework.http.HttpStatus;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;

@ControllerAdvice
public class TratamentoErrosWeb {

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String rotaNaoEncontrada(Model model) {
        model.addAttribute("mensagem", "A página solicitada não existe ou foi movida.");
        return "erros/404";
    }

    @ExceptionHandler(RecursoNaoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String recursoNaoEncontrado(RecursoNaoEncontradoException ex, Model model) {
        model.addAttribute("mensagem", ex.getMessage());
        return "erros/404";
    }

    @ExceptionHandler(RegraNegocioException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public String regraNegocio(RegraNegocioException ex, Model model) {
        model.addAttribute("mensagem", ex.getMessage());
        return "erros/erro";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String erroInesperado(Exception ex, Model model) {
        model.addAttribute("mensagem", "Não foi possível concluir a ação. Tente novamente em alguns instantes.");
        return "erros/500";
    }
}
