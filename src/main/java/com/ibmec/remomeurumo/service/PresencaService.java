package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.RegistroPresencaItem;
import com.ibmec.remomeurumo.dto.RegistroPresencaResumo;
import com.ibmec.remomeurumo.dto.ResultadoRegistroPresenca;
import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class PresencaService {

    private final PresencaRepository presencaRepository;
    private final ParticipanteRepository participanteRepository;
    private final EncontroRepository encontroRepository;
    private final HistoricoOperacaoService historicoService;

    public PresencaService(PresencaRepository presencaRepository,
            ParticipanteRepository participanteRepository,
            EncontroRepository encontroRepository,
            HistoricoOperacaoService historicoService) {
        this.presencaRepository = presencaRepository;
        this.participanteRepository = participanteRepository;
        this.encontroRepository = encontroRepository;
        this.historicoService = historicoService;
    }

    @Transactional(readOnly = true)
    public List<RegistroPresencaItem> prepararItens(Long encontroId, String busca, boolean marcarTodos) {
        Encontro encontro = obterEncontro(encontroId);
        Map<Long, Presenca> presencas = presencaRepository.findByEncontroIdOrderByParticipanteNomeAsc(encontro.getId())
                .stream()
                .collect(Collectors.toMap(p -> p.getParticipante().getId(), p -> p));

        String filtro = busca == null ? "" : busca.trim().toLowerCase(Locale.ROOT);
        return participanteRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(participante -> filtro.isBlank()
                        || participante.getNome().toLowerCase(Locale.ROOT).contains(filtro))
                .map(participante -> montarItem(participante, presencas.get(participante.getId()), marcarTodos))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<RegistroPresencaItem> prepararRascunho(Map<Long, StatusPresenca> statusPorParticipante,
            Map<Long, String> observacoesPorParticipante) {
        validarSelecao(statusPorParticipante);
        Map<Long, String> observacoes = observacoesPorParticipante == null ? Map.of() : observacoesPorParticipante;
        return statusPorParticipante.entrySet().stream()
                .map(entrada -> {
                    Participante participante = participanteRepository.findById(entrada.getKey())
                            .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
                    return new RegistroPresencaItem(
                            participante.getId(),
                            participante.getNome(),
                            entrada.getValue(),
                            Objects.toString(observacoes.get(participante.getId()), ""));
                })
                .sorted(Comparator.comparing(RegistroPresencaItem::nomeParticipante))
                .toList();
    }

    public RegistroPresencaResumo resumirSelecao(Map<Long, StatusPresenca> statusPorParticipante) {
        validarSelecao(statusPorParticipante);
        long presentes = statusPorParticipante.values().stream()
                .filter(status -> status == StatusPresenca.PRESENTE).count();
        long ausentes = statusPorParticipante.values().stream()
                .filter(status -> status == StatusPresenca.AUSENTE).count();
        long justificadas = statusPorParticipante.values().stream()
                .filter(status -> status == StatusPresenca.JUSTIFICADA).count();
        return new RegistroPresencaResumo(presentes + ausentes + justificadas, presentes, ausentes, justificadas);
    }

    @Transactional
    public ResultadoRegistroPresenca registrarLote(Long encontroId, Map<Long, StatusPresenca> statusPorParticipante,
            Map<Long, String> observacoesPorParticipante) {
        Encontro encontro = obterEncontroParaRegistro(encontroId);
        validarSelecao(statusPorParticipante);
        RegistroPresencaResumo resumoAnterior = resumirEncontro(encontroId);
        Map<Long, String> observacoes = observacoesPorParticipante == null ? Map.of() : observacoesPorParticipante;

        int novosRegistros = 0;
        int correcoes = 0;
        for (Map.Entry<Long, StatusPresenca> entrada : statusPorParticipante.entrySet()) {
            Long participanteId = entrada.getKey();
            StatusPresenca status = entrada.getValue();
            if (participanteId == null || status == null) {
                continue;
            }

            Participante participante = participanteRepository.findById(participanteId)
                    .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
            String observacao = Objects.toString(observacoes.get(participanteId), "").trim();
            Optional<Presenca> existente = presencaRepository.findByParticipanteIdAndEncontroId(
                    participanteId, encontro.getId());

            if (!participante.isAtivo() && existente.isEmpty()) {
                throw new RegraNegocioException(
                        "O participante " + participante.getNome() + " está inativo e não pode receber uma nova presença.");
            }

            if (existente.isEmpty()) {
                presencaRepository.save(new Presenca(participante, encontro, status, observacao));
                novosRegistros++;
            } else {
                Presenca presenca = existente.get();
                boolean mudou = presenca.getStatus() != status
                        || !Objects.equals(Objects.toString(presenca.getObservacoes(), "").trim(), observacao);
                if (mudou) {
                    presenca.atualizarStatus(status, observacao);
                    presencaRepository.save(presenca);
                    correcoes++;
                }
            }
        }

        RegistroPresencaResumo resumo = resumirEncontro(encontroId);
        String referencia = nomeEncontro(encontro);
        if (novosRegistros > 0 || correcoes > 0) {
            AcaoHistorico acao = novosRegistros > 0 ? AcaoHistorico.REGISTRO : AcaoHistorico.CORRECAO;
            String descricao = novosRegistros + (novosRegistros == 1 ? " nova presença" : " novas presenças")
                    + " e " + correcoes + (correcoes == 1 ? " correção foram confirmadas." : " correções foram confirmadas.");
            historicoService.registrar(ModuloHistorico.PRESENCAS, "Encontro", encontro.getId(), referencia,
                    acao, descricao, resumoHistorico(resumoAnterior), resumoHistorico(resumo));
        }
        return new ResultadoRegistroPresenca(
                resumo.total(), resumo.presentes(), resumo.ausentes(), resumo.justificadas(), novosRegistros, correcoes);
    }

    @Transactional
    public Presenca registrarUnico(Long participanteId, Long encontroId, StatusPresenca status, String observacoes) {
        if (status == null) {
            throw new RegraNegocioException("Informe a presença.");
        }
        Participante participante = participanteRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
        if (!participante.isAtivo()) {
            throw new RegraNegocioException("Participante inativo não pode receber uma nova presença.");
        }
        Encontro encontro = obterEncontroParaRegistro(encontroId);
        if (presencaRepository.existsByParticipanteIdAndEncontroId(participanteId, encontroId)) {
            throw new RegraNegocioException("Já existe uma presença registrada para este participante nesse encontro.");
        }
        Presenca salva = presencaRepository.save(new Presenca(participante, encontro, status, observacoes));
        historicoService.registrar(ModuloHistorico.PRESENCAS, "Encontro", encontro.getId(), nomeEncontro(encontro),
                AcaoHistorico.REGISTRO, "Presença de " + participante.getNome() + " registrada como "
                        + status.getDescricao().toLowerCase(Locale.ROOT) + ".");
        return salva;
    }

    @Transactional(readOnly = true)
    public Presenca obter(Long id) {
        return presencaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Registro de presença não encontrado."));
    }

    @Transactional
    public void excluir(Long id) {
        Presenca presenca = obter(id);
        Encontro encontro = presenca.getEncontro();
        String descricao = "Registro de " + presenca.getParticipante().getNome() + " excluído. Situação anterior: "
                + presenca.getStatus().getDescricao() + ".";
        historicoService.registrar(ModuloHistorico.PRESENCAS, "Encontro", encontro.getId(), nomeEncontro(encontro),
                AcaoHistorico.EXCLUSAO, descricao,
                "Participante: " + presenca.getParticipante().getNome()
                        + " | Situação: " + presenca.getStatus().getDescricao(),
                "Registro removido");
        presencaRepository.delete(presenca);
    }

    @Transactional(readOnly = true)
    public RegistroPresencaResumo resumirEncontro(Long encontroId) {
        long presentes = presencaRepository.countByEncontroIdAndStatus(encontroId, StatusPresenca.PRESENTE);
        long ausentes = presencaRepository.countByEncontroIdAndStatus(encontroId, StatusPresenca.AUSENTE);
        long justificadas = presencaRepository.countByEncontroIdAndStatus(encontroId, StatusPresenca.JUSTIFICADA);
        return new RegistroPresencaResumo(presentes + ausentes + justificadas, presentes, ausentes, justificadas);
    }

    @Transactional(readOnly = true)
    public List<Presenca> listarHistorico(Long participanteId, Long atividadeId, LocalDate inicio,
            LocalDate fim, StatusPresenca status) {
        return presencaRepository.filtrarHistorico(participanteId, atividadeId, inicio, fim, status);
    }

    @Transactional(readOnly = true)
    public List<Presenca> listarHistoricoCompleto() {
        return presencaRepository.listarHistorico();
    }

    @Transactional(readOnly = true)
    public List<Presenca> listarPorEncontro(Long encontroId) {
        return presencaRepository.findByEncontroIdOrderByParticipanteNomeAsc(encontroId);
    }

    @Transactional(readOnly = true)
    public long contarParticipantesPorAtividade(Long atividadeId) {
        return presencaRepository.contarParticipantesPorAtividade(atividadeId);
    }

    public Map<Long, StatusPresenca> extrairStatus(Map<String, String> parametros) {
        Map<Long, StatusPresenca> resultado = new LinkedHashMap<>();
        try {
            parametros.forEach((chave, valor) -> {
                if (chave.startsWith("status_") && valor != null && !valor.isBlank()) {
                    Long participanteId = Long.valueOf(chave.substring("status_".length()));
                    resultado.put(participanteId, StatusPresenca.valueOf(valor));
                }
            });
        } catch (IllegalArgumentException ex) {
            throw new RegraNegocioException("Uma marcação de presença é inválida. Revise a lista e tente novamente.");
        }
        return resultado;
    }

    public Map<Long, String> extrairObservacoes(Map<String, String> parametros) {
        Map<Long, String> resultado = new LinkedHashMap<>();
        try {
            parametros.forEach((chave, valor) -> {
                if (chave.startsWith("observacoes_")) {
                    Long participanteId = Long.valueOf(chave.substring("observacoes_".length()));
                    resultado.put(participanteId, valor);
                }
            });
        } catch (NumberFormatException ex) {
            throw new RegraNegocioException("Não foi possível identificar um participante. Reabra a lista e tente novamente.");
        }
        return resultado;
    }

    private RegistroPresencaItem montarItem(Participante participante, Presenca presenca, boolean marcarTodos) {
        StatusPresenca status = marcarTodos ? StatusPresenca.PRESENTE : StatusPresenca.AUSENTE;
        String observacoes = "";
        if (!marcarTodos && presenca != null) {
            status = presenca.getStatus();
            observacoes = Objects.toString(presenca.getObservacoes(), "");
        }
        return new RegistroPresencaItem(participante.getId(), participante.getNome(), status, observacoes);
    }

    private Encontro obterEncontro(Long encontroId) {
        if (encontroId == null) {
            throw new RegraNegocioException("Escolha um encontro.");
        }
        return encontroRepository.findById(encontroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encontro não encontrado."));
    }

    private Encontro obterEncontroParaRegistro(Long encontroId) {
        Encontro encontro = obterEncontro(encontroId);
        if (encontro.getStatus() == StatusEncontro.CANCELADO) {
            throw new RegraNegocioException(
                    "Este encontro está cancelado e não pode receber novos registros. Escolha outro encontro.");
        }
        return encontro;
    }

    private void validarSelecao(Map<Long, StatusPresenca> statusPorParticipante) {
        if (statusPorParticipante == null || statusPorParticipante.isEmpty()) {
            throw new RegraNegocioException("Marque a presença de pelo menos um participante antes de continuar.");
        }
    }

    private String nomeEncontro(Encontro encontro) {
        return encontro.getAtividade().getNome() + " - " + encontro.getData();
    }

    private String resumoHistorico(RegistroPresencaResumo resumo) {
        return "Presentes: " + resumo.presentes()
                + " | Ausentes: " + resumo.ausentes()
                + " | Justificadas: " + resumo.justificadas();
    }
}
