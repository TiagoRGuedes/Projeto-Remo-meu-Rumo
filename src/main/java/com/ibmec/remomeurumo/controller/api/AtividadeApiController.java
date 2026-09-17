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
import com.ibmec.remomeurumo.dto.AtividadeInput;
import com.ibmec.remomeurumo.dto.AtividadeOutput;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.service.AtividadeService;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/atividades")
public class AtividadeApiController {

    private final AtividadeService atividadeService;

    public AtividadeApiController(AtividadeService atividadeService) {
        this.atividadeService = atividadeService;
    }

    @GetMapping
    public List<AtividadeOutput> listar() {
        return atividadeService.listar(null).stream().map(AtividadeOutput::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public AtividadeOutput cadastrar(@Valid @RequestBody AtividadeInput input) {
        return AtividadeOutput.de(atividadeService.cadastrar(new Atividade(input.nome(), input.descricao())));
    }

    @GetMapping("/{id}")
    public AtividadeOutput obter(@PathVariable Long id) {
        return AtividadeOutput.de(atividadeService.obter(id));
    }

    @PutMapping("/{id}")
    public AtividadeOutput atualizar(@PathVariable Long id, @Valid @RequestBody AtividadeInput input) {
        return AtividadeOutput.de(atividadeService.atualizar(id, new Atividade(input.nome(), input.descricao())));
    }

    @DeleteMapping("/{id}")
    public ApiMensagem desativar(@PathVariable Long id) {
        atividadeService.desativar(id);
        return new ApiMensagem("Atividade desativada. Os encontros antigos continuam disponíveis.");
    }
}
