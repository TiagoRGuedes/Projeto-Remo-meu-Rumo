package com.ibmec.remomeurumo.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ibmec.remomeurumo.dto.EncontroInput;
import com.ibmec.remomeurumo.dto.EncontroOutput;
import com.ibmec.remomeurumo.service.EncontroService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/encontros")
public class EncontroApiController {

    private final EncontroService encontroService;

    public EncontroApiController(EncontroService encontroService) {
        this.encontroService = encontroService;
    }

    @GetMapping
    public List<EncontroOutput> listar() {
        return encontroService.listar().stream().map(EncontroOutput::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public EncontroOutput criar(@Valid @RequestBody EncontroInput input) {
        return EncontroOutput.de(encontroService.criar(input.atividadeId(), input.data(), input.observacoes()));
    }

    @GetMapping("/{id}")
    public EncontroOutput obter(@PathVariable Long id) {
        return EncontroOutput.de(encontroService.obter(id));
    }

    @PutMapping("/{id}")
    public EncontroOutput atualizar(@PathVariable Long id, @Valid @RequestBody EncontroInput input) {
        return EncontroOutput.de(encontroService.atualizar(
                id,
                input.atividadeId(),
                input.data(),
                input.observacoes(),
                input.status()));
    }
}
