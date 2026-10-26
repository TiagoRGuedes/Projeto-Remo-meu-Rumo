package com.ibmec.remomeurumo.controller.api;

import java.time.LocalDate;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ibmec.remomeurumo.dto.EncerramentoMatriculaInput;
import com.ibmec.remomeurumo.dto.MatriculaInput;
import com.ibmec.remomeurumo.dto.MatriculaOutput;
import com.ibmec.remomeurumo.service.MatriculaService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/matriculas")
public class MatriculaApiController {

    private final MatriculaService matriculaService;

    public MatriculaApiController(MatriculaService matriculaService) {
        this.matriculaService = matriculaService;
    }

    @GetMapping
    public List<MatriculaOutput> listar() {
        return matriculaService.listarTodas().stream().map(MatriculaOutput::de).toList();
    }

    @GetMapping("/{id}")
    public MatriculaOutput obter(@PathVariable Long id) {
        return MatriculaOutput.de(matriculaService.obter(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public MatriculaOutput matricular(@Valid @RequestBody MatriculaInput input) {
        LocalDate inicio = input.dataInicio() == null ? LocalDate.now() : input.dataInicio();
        return MatriculaOutput.de(matriculaService.matricular(input.participanteId(), input.turmaId(), inicio));
    }

    @PostMapping("/{id}/encerrar")
    public MatriculaOutput encerrar(@PathVariable Long id, @Valid @RequestBody EncerramentoMatriculaInput input) {
        LocalDate saida = input.dataSaida() == null ? LocalDate.now() : input.dataSaida();
        return MatriculaOutput.de(matriculaService.encerrar(id, input.motivoSaida(), input.observacaoSaida(), saida));
    }

    @PostMapping("/{id}/reativar")
    public MatriculaOutput reativar(@PathVariable Long id) {
        return MatriculaOutput.de(matriculaService.reativar(id));
    }
}
