package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.FrequenciaResumo;
import com.ibmec.remomeurumo.dto.ResumoFrequenciaDashboard;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class FrequenciaService {

    private final PresencaRepository presencaRepository;

    public FrequenciaService(PresencaRepository presencaRepository) {
        this.presencaRepository = presencaRepository;
    }

    @Transactional(readOnly = true)
    public List<FrequenciaResumo> porParticipante(Long atividadeId, LocalDate inicio, LocalDate fim) {
        List<Presenca> presencas = presencaRepository.filtrarHistorico(null, atividadeId, inicio, fim, null);
        Map<String, List<Presenca>> porParticipante = presencas.stream()
                .collect(Collectors.groupingBy(p -> p.getParticipante().getNome()));
        return porParticipante.entrySet().stream()
                .map(entry -> resumir(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FrequenciaResumo::nome))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FrequenciaResumo> porAtividade(LocalDate inicio, LocalDate fim) {
        List<Presenca> presencas = presencaRepository.filtrarHistorico(null, null, inicio, fim, null);
        Map<String, List<Presenca>> porAtividade = presencas.stream()
                .collect(Collectors.groupingBy(p -> p.getEncontro().getAtividade().getNome()));
        return porAtividade.entrySet().stream()
                .map(entry -> resumir(entry.getKey(), entry.getValue()))
                .sorted(Comparator.comparing(FrequenciaResumo::nome))
                .toList();
    }

    @Transactional(readOnly = true)
    public double frequenciaMediaGeral() {
        long total = presencaRepository.count();
        if (total == 0) {
            return 0;
        }
        long presentes = presencaRepository.countByStatus(StatusPresenca.PRESENTE);
        return arredondar(presentes * 100.0 / total);
    }

    @Transactional(readOnly = true)
    public ResumoFrequenciaDashboard resumirDashboard(Long atividadeId, LocalDate inicio, LocalDate fim) {
        List<Presenca> presencas = presencaRepository.filtrarHistorico(null, atividadeId, inicio, fim, null);
        return resumirDashboard(presencas);
    }

    public ResumoFrequenciaDashboard resumirDashboard(List<Presenca> presencas) {
        long presentes = contar(presencas, StatusPresenca.PRESENTE);
        long ausentes = contar(presencas, StatusPresenca.AUSENTE);
        long justificadas = contar(presencas, StatusPresenca.JUSTIFICADA);
        long total = presentes + ausentes + justificadas;
        double media = total == 0 ? 0 : arredondar(presentes * 100.0 / total);
        long participantes = presencas.stream().map(p -> p.getParticipante().getId()).distinct().count();
        return new ResumoFrequenciaDashboard(media, participantes, presentes, ausentes, justificadas);
    }

    public FrequenciaResumo resumir(String nome, List<Presenca> presencas) {
        long presentes = contar(presencas, StatusPresenca.PRESENTE);
        long ausentes = contar(presencas, StatusPresenca.AUSENTE);
        long justificadas = contar(presencas, StatusPresenca.JUSTIFICADA);
        long total = presentes + ausentes + justificadas;
        double percentual = total == 0 ? 0 : arredondar(presentes * 100.0 / total);
        return new FrequenciaResumo(nome, total, presentes, ausentes, justificadas, percentual);
    }

    private long contar(List<Presenca> presencas, StatusPresenca status) {
        return presencas.stream().filter(p -> p.getStatus() == status).count();
    }

    private double arredondar(double valor) {
        return Math.round(valor * 10.0) / 10.0;
    }
}
