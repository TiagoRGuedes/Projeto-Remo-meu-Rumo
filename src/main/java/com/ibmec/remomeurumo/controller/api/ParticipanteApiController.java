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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.ibmec.remomeurumo.dto.ApiMensagem;
import com.ibmec.remomeurumo.dto.ParticipanteInput;
import com.ibmec.remomeurumo.dto.ParticipanteOutput;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.service.ParticipanteService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/participantes")
public class ParticipanteApiController {

    private final ParticipanteService participanteService;

    public ParticipanteApiController(ParticipanteService participanteService) {
        this.participanteService = participanteService;
    }

    @GetMapping
    public List<ParticipanteOutput> listar() {
        return participanteService.listar(null).stream().map(ParticipanteOutput::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ParticipanteOutput cadastrar(@Valid @RequestBody ParticipanteInput input) {
        return ParticipanteOutput.de(participanteService.cadastrar(new Participante(input.nome(), input.observacoes())));
    }

    @GetMapping("/{id}")
    public ParticipanteOutput obter(@PathVariable Long id) {
        return ParticipanteOutput.de(participanteService.obter(id));
    }

    @PutMapping("/{id}")
    public ParticipanteOutput atualizar(@PathVariable Long id, @Valid @RequestBody ParticipanteInput input) {
        return ParticipanteOutput.de(participanteService.atualizar(id, new Participante(input.nome(), input.observacoes())));
    }

    @DeleteMapping("/{id}")
    public ApiMensagem desativar(@PathVariable Long id) {
        participanteService.desativar(id);
        return new ApiMensagem("Participante desativado. O histórico foi mantido.");
    }
}
