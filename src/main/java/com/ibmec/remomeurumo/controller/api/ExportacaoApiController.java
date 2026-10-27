package com.ibmec.remomeurumo.controller.api;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.ibmec.remomeurumo.dto.RegistroExportacao;
import com.ibmec.remomeurumo.service.ExportacaoService;
import com.ibmec.remomeurumo.support.MesReferencia;

@RestController
@RequestMapping("/api/exportacao")
public class ExportacaoApiController {

    private final ExportacaoService exportacaoService;

    public ExportacaoApiController(ExportacaoService exportacaoService) {
        this.exportacaoService = exportacaoService;
    }

    @GetMapping("/frequencia-mensal")
    public List<RegistroExportacao> frequenciaMensal(@RequestParam(required = false) String mes) {
        return exportacaoService.frequenciaMensal(MesReferencia.resolver(mes));
    }
}
