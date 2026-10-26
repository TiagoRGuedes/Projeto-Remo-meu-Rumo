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
import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.dto.MatriculaOutput;
import com.ibmec.remomeurumo.dto.ResumoMensalTurma;
import com.ibmec.remomeurumo.dto.TurmaInput;
import com.ibmec.remomeurumo.dto.TurmaOutput;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.FrequenciaService;
import com.ibmec.remomeurumo.service.MatriculaService;
import com.ibmec.remomeurumo.support.MesReferencia;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/turmas")
public class TurmaApiController {

    private final AtividadeService atividadeService;
    private final MatriculaService matriculaService;
    private final FrequenciaService frequenciaService;

    public TurmaApiController(AtividadeService atividadeService,
            MatriculaService matriculaService,
            FrequenciaService frequenciaService) {
        this.atividadeService = atividadeService;
        this.matriculaService = matriculaService;
        this.frequenciaService = frequenciaService;
    }

    @GetMapping
    public List<TurmaOutput> listar() {
        return atividadeService.listar(null).stream().map(TurmaOutput::de).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public TurmaOutput cadastrar(@Valid @RequestBody TurmaInput input) {
        return TurmaOutput.de(atividadeService.cadastrar(paraModelo(input)));
    }

    @GetMapping("/{id}")
    public TurmaOutput obter(@PathVariable Long id) {
        return TurmaOutput.de(atividadeService.obter(id));
    }

    @PutMapping("/{id}")
    public TurmaOutput atualizar(@PathVariable Long id, @Valid @RequestBody TurmaInput input) {
        return TurmaOutput.de(atividadeService.atualizar(id, paraModelo(input)));
    }

    @DeleteMapping("/{id}")
    public ApiMensagem desativar(@PathVariable Long id) {
        atividadeService.desativar(id);
        return new ApiMensagem("Turma desativada. Aulas, matrículas e presenças anteriores foram mantidas.");
    }

    @GetMapping("/{id}/matriculas")
    public List<MatriculaOutput> matriculas(@PathVariable Long id) {
        atividadeService.obter(id);
        return matriculaService.listarPorTurma(id).stream().map(MatriculaOutput::de).toList();
    }

    @GetMapping("/{id}/frequencia")
    public ResumoMensalTurma frequenciaMensal(@PathVariable Long id, @RequestParam(required = false) String mes) {
        return frequenciaService.resumoMensalDaTurma(id, MesReferencia.resolver(mes));
    }

    @GetMapping("/{id}/frequencia/participantes")
    public List<FrequenciaMensal> frequenciaPorParticipante(@PathVariable Long id,
            @RequestParam(required = false) String mes) {
        return frequenciaService.linhasMensaisDaTurma(id, MesReferencia.resolver(mes));
    }

    @GetMapping("/{id}/faltosos")
    public List<FrequenciaMensal> faltosos(@PathVariable Long id, @RequestParam(required = false) String mes) {
        return frequenciaService.faltososDaTurma(id, MesReferencia.resolver(mes));
    }

    private Atividade paraModelo(TurmaInput input) {
        return new Atividade(input.nome(), input.descricao(), input.diasSemana(), input.horario());
    }
}
