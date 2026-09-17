package com.ibmec.remomeurumo.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ibmec.remomeurumo.dto.PresencaInput;
import com.ibmec.remomeurumo.dto.PresencaOutput;
import com.ibmec.remomeurumo.service.PresencaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/presencas")
public class PresencaApiController {

    private final PresencaService presencaService;

    public PresencaApiController(PresencaService presencaService) {
        this.presencaService = presencaService;
    }

    @GetMapping
    public List<PresencaOutput> listar() {
        return presencaService.listarHistoricoCompleto().stream().map(PresencaOutput::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public PresencaOutput registrar(@Valid @RequestBody PresencaInput input) {
        return PresencaOutput.de(presencaService.registrarUnico(
                input.participanteId(),
                input.encontroId(),
                input.status(),
                input.observacoes()));
    }
}
