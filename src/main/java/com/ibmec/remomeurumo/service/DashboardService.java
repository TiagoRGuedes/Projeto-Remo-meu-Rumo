package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.EncontroResumoDashboard;
import com.ibmec.remomeurumo.dto.IndicadoresDashboard;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class DashboardService {

    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;
    private final EncontroRepository encontroRepository;
    private final PresencaRepository presencaRepository;
    private final FrequenciaService frequenciaService;

    public DashboardService(ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository,
            EncontroRepository encontroRepository,
            PresencaRepository presencaRepository,
            FrequenciaService frequenciaService) {
        this.participanteRepository = participanteRepository;
        this.atividadeRepository = atividadeRepository;
        this.encontroRepository = encontroRepository;
        this.presencaRepository = presencaRepository;
        this.frequenciaService = frequenciaService;
    }

    @Transactional(readOnly = true)
    public IndicadoresDashboard carregarIndicadores() {
        LocalDate hoje = LocalDate.now();
        LocalDate inicioMes = hoje.withDayOfMonth(1);
        LocalDate fimMes = hoje.withDayOfMonth(hoje.lengthOfMonth());
        long participantesAtivos = participanteRepository.countByAtivoTrue();
        long participantesInativos = participanteRepository.countByAtivoFalse();
        long atividadesAtivas = atividadeRepository.countByAtivoTrue();
        long atividadesInativas = atividadeRepository.countByAtivoFalse();
        long encontrosNoMes = encontroRepository.countByDataBetween(inicioMes, fimMes);
        long presencasNoMes = presencaRepository.countByStatusAndEncontroDataBetween(
                StatusPresenca.PRESENTE, inicioMes, fimMes);
        long ausenciasNoMes = presencaRepository.countByStatusAndEncontroDataBetween(
                StatusPresenca.AUSENTE, inicioMes, fimMes);
        long justificadasNoMes = presencaRepository.countByStatusAndEncontroDataBetween(
                StatusPresenca.JUSTIFICADA, inicioMes, fimMes);

        return new IndicadoresDashboard(
                participantesAtivos,
                participantesInativos,
                atividadesAtivas,
                atividadesInativas,
                encontrosNoMes,
                presencasNoMes,
                ausenciasNoMes,
                justificadasNoMes,
                frequenciaService.frequenciaMediaGeral(),
                presencaRepository.count());
    }

    @Transactional(readOnly = true)
    public EncontroResumoDashboard carregarUltimoEncontro() {
        return encontroRepository.findFirstByStatusAndDataLessThanEqualOrderByDataDescIdDesc(
                        StatusEncontro.REALIZADO, LocalDate.now())
                .map(this::resumir)
                .orElse(null);
    }

    @Transactional(readOnly = true)
    public List<EncontroResumoDashboard> carregarEncontrosRecentes() {
        return encontroRepository.findTop5ByOrderByDataDesc().stream()
                .map(this::resumir)
                .toList();
    }

    @Transactional(readOnly = true)
    public long contarEncontrosSemPresenca() {
        return encontroRepository.contarSemPresencaRegistrada();
    }

    private EncontroResumoDashboard resumir(Encontro encontro) {
        long presentes = presencaRepository.countByEncontroIdAndStatus(encontro.getId(), StatusPresenca.PRESENTE);
        long ausentes = presencaRepository.countByEncontroIdAndStatus(encontro.getId(), StatusPresenca.AUSENTE);
        long justificadas = presencaRepository.countByEncontroIdAndStatus(encontro.getId(), StatusPresenca.JUSTIFICADA);
        long totalRegistros = presentes + ausentes + justificadas;
        long totalEsperado = Math.max(totalRegistros, participanteRepository.countByAtivoTrue());
        double percentual = totalRegistros == 0 ? 0 : Math.round(presentes * 1000.0 / totalRegistros) / 10.0;
        return new EncontroResumoDashboard(
                encontro.getId(),
                encontro.getAtividade().getId(),
                encontro.getAtividade().getNome(),
                encontro.getData(),
                encontro.getStatus(),
                totalEsperado,
                totalRegistros,
                presentes,
                ausentes,
                justificadas,
                percentual);
    }
}
