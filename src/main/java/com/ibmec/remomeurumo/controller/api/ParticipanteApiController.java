package com.ibmec.remomeurumo.controller.api;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ibmec.remomeurumo.dto.ApiMensagem;
import com.ibmec.remomeurumo.dto.FrequenciaMensalParticipante;
import com.ibmec.remomeurumo.dto.MatriculaOutput;
import com.ibmec.remomeurumo.dto.ParticipanteInput;
import com.ibmec.remomeurumo.dto.ParticipanteOutput;
import com.ibmec.remomeurumo.service.FrequenciaService;
import com.ibmec.remomeurumo.service.MatriculaService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.support.MesReferencia;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/participantes")
public class ParticipanteApiController {

    private final ParticipanteService participanteService;
    private final MatriculaService matriculaService;
    private final FrequenciaService frequenciaService;

    public ParticipanteApiController(ParticipanteService participanteService,
            MatriculaService matriculaService,
            FrequenciaService frequenciaService) {
        this.participanteService = participanteService;
        this.matriculaService = matriculaService;
        this.frequenciaService = frequenciaService;
    }

    @GetMapping
    public List<ParticipanteOutput> listar() {
        return participanteService.listar(null).stream().map(ParticipanteOutput::resumo).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipanteOutput cadastrar(@Valid @RequestBody ParticipanteInput input) {
        return ParticipanteOutput.de(participanteService.cadastrar(input.paraModelo()));
    }

    @GetMapping("/{id}")
    public ParticipanteOutput obter(@PathVariable Long id) {
        return ParticipanteOutput.de(participanteService.obter(id));
    }

    @PutMapping("/{id}")
    public ParticipanteOutput atualizar(@PathVariable Long id, @Valid @RequestBody ParticipanteInput input) {
        return ParticipanteOutput.de(participanteService.atualizar(id, input.paraModelo()));
    }

    @GetMapping("/{id}/frequencia")
    public FrequenciaMensalParticipante frequenciaMensal(@PathVariable Long id,
            @RequestParam(required = false) String mes) {
        return frequenciaService.frequenciaMensalDoParticipante(id, MesReferencia.resolver(mes));
    }

    @GetMapping("/{id}/matriculas")
    public List<MatriculaOutput> matriculas(@PathVariable Long id) {
        participanteService.obter(id);
        return matriculaService.listarPorParticipante(id).stream().map(MatriculaOutput::de).toList();
    }

    @DeleteMapping("/{id}")
    public ApiMensagem desativar(@PathVariable Long id) {
        participanteService.desativar(id);
        return new ApiMensagem("Participante desativado. O histórico foi mantido.");
    }
}
