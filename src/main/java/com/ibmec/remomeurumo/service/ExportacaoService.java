package com.ibmec.remomeurumo.service;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaMensal;
import com.ibmec.remomeurumo.dto.RegistroExportacao;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Matricula;

@Service
public class ExportacaoService {

    private final MatriculaService matriculaService;
    private final FrequenciaService frequenciaService;

    public ExportacaoService(MatriculaService matriculaService, FrequenciaService frequenciaService) {
        this.matriculaService = matriculaService;
        this.frequenciaService = frequenciaService;
    }

    @Transactional(readOnly = true)
    public List<RegistroExportacao> frequenciaMensal(YearMonth mes) {
        Map<Long, Map<Long, FrequenciaMensal>> frequenciaPorTurma = new LinkedHashMap<>();
        Map<Long, Map<Long, FrequenciaMensal>> anualPorTurma = new LinkedHashMap<>();
        List<RegistroExportacao> registros = new ArrayList<>();
        for (Matricula matricula : matriculaService.listarTodas()) {
            Atividade turma = matricula.getAtividade();
            Map<Long, FrequenciaMensal> linhas = frequenciaPorTurma.computeIfAbsent(
                    turma.getId(), turmaId -> indexarPorParticipante(turmaId, mes));
            FrequenciaMensal linha = linhas.get(matricula.getParticipante().getId());
            if (linha == null) {
                continue;
            }
            FrequenciaMensal anual = anualPorTurma.computeIfAbsent(turma.getId(),
                    turmaId -> indexar(frequenciaService.linhasAnuaisDaTurma(turmaId, mes.getYear())))
                    .get(matricula.getParticipante().getId());
            registros.add(new RegistroExportacao(
                    mes + "-" + matricula.getParticipante().getId() + "-" + turma.getId(),
                    matricula.getParticipante().getId(),
                    matricula.getParticipante().getNome(),
                    turma.getId(),
                    turma.getNome(),
                    turma.getGradeResumo(),
                    mes.toString(),
                    matricula.getStatus().name(),
                    matricula.getMotivoSaida() == null ? null : matricula.getMotivoSaida().name(),
                    linha.aulasPrevistas(),
                    linha.presencas(),
                    linha.faltas(),
                    linha.faltasJustificadas(),
                    linha.percentual(),
                    anual == null ? 0 : anual.presencas(),
                    anual == null ? 0 : anual.aulasPrevistas(),
                    linha.contatoNecessario(),
                    linha.abaixoDaMeta()));
        }
        return registros;
    }

    private Map<Long, FrequenciaMensal> indexarPorParticipante(Long turmaId, YearMonth mes) {
        return indexar(frequenciaService.linhasMensaisDaTurma(turmaId, mes));
    }

    private Map<Long, FrequenciaMensal> indexar(List<FrequenciaMensal> linhasDaTurma) {
        Map<Long, FrequenciaMensal> indice = new LinkedHashMap<>();
        for (FrequenciaMensal linha : linhasDaTurma) {
            indice.put(linha.participanteId(), linha);
        }
        return indice;
    }
}
