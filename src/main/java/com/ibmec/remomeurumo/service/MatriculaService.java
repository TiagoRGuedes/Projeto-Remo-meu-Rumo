package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.MovimentoMatricula;
import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Matricula;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.MotivoSaida;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.StatusMatricula;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.MatriculaRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;

@Service
public class MatriculaService {

    private final MatriculaRepository matriculaRepository;
    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;
    private final HistoricoOperacaoService historicoService;

    public MatriculaService(MatriculaRepository matriculaRepository,
            ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository,
            HistoricoOperacaoService historicoService) {
        this.matriculaRepository = matriculaRepository;
        this.participanteRepository = participanteRepository;
        this.atividadeRepository = atividadeRepository;
        this.historicoService = historicoService;
    }

    @Transactional(readOnly = true)
    public Matricula obter(Long id) {
        return matriculaRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Matrícula não encontrada."));
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarPorParticipante(Long participanteId) {
        return matriculaRepository.listarPorParticipante(participanteId);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarPorTurma(Long turmaId) {
        return matriculaRepository.listarPorTurma(turmaId);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarAtivasDaTurma(Long turmaId) {
        return matriculaRepository.listarPorTurmaEStatus(turmaId, StatusMatricula.ATIVA);
    }

    @Transactional(readOnly = true)
    public List<Matricula> listarTodas() {
        return matriculaRepository.listarTodas();
    }

    @Transactional(readOnly = true)
    public List<Atividade> turmasDisponiveis(Long participanteId) {
        List<Long> matriculasAtivas = matriculaRepository.listarPorParticipante(participanteId).stream()
                .filter(Matricula::isAtiva)
                .map(matricula -> matricula.getAtividade().getId())
                .toList();
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(atividade -> !matriculasAtivas.contains(atividade.getId()))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<MovimentoMatricula> movimentos(Long participanteId) {
        List<MovimentoMatricula> movimentos = new ArrayList<>();
        for (Matricula matricula : matriculaRepository.listarPorParticipante(participanteId)) {
            String turma = matricula.getAtividade().getNome();
            movimentos.add(new MovimentoMatricula(matricula.getDataInicio(), "Ingresso",
                    "Ingressou na turma " + turma + "."));
            if (!matricula.isAtiva() && matricula.getDataSaida() != null) {
                if (matricula.getMotivoSaida() == MotivoSaida.TROCA_DE_TURMA) {
                    movimentos.add(new MovimentoMatricula(matricula.getDataSaida(), "Troca de turma",
                            "Saiu de " + turma + (matricula.getObservacaoSaida() == null ? "."
                                    : " " + matricula.getObservacaoSaida() + ".")));
                } else {
                    movimentos.add(new MovimentoMatricula(matricula.getDataSaida(), "Saída",
                            "Saiu da turma " + turma + ". Motivo: " + matricula.getMotivoSaidaDescricao()
                                    + (matricula.getObservacaoSaida() == null ? "." : " — " + matricula.getObservacaoSaida() + ".")));
                }
            }
        }
        movimentos.sort(Comparator.comparing(MovimentoMatricula::data).reversed());
        return movimentos;
    }

    @Transactional(readOnly = true)
    public long contarAtivas() {
        return matriculaRepository.countByStatus(StatusMatricula.ATIVA);
    }

    @Transactional(readOnly = true)
    public long contarEncerradas() {
        return matriculaRepository.countByStatus(StatusMatricula.ENCERRADA);
    }

    @Transactional(readOnly = true)
    public long contarAtivasDaTurma(Long turmaId) {
        return matriculaRepository.countByAtividadeIdAndStatus(turmaId, StatusMatricula.ATIVA);
    }

    @Transactional
    public Matricula matricular(Long participanteId, Long turmaId, LocalDate dataInicio) {
        Participante participante = obterParticipante(participanteId);
        Atividade turma = obterTurma(turmaId);
        validarNovaMatricula(participante, turma);
        Matricula matricula = matriculaRepository.save(new Matricula(participante, turma, dataInicio));
        historicoService.registrar(ModuloHistorico.MATRICULAS, "Matrícula", participante.getId(),
                participante.getNome(), AcaoHistorico.INGRESSO,
                "Ingresso de " + participante.getNome() + " na turma " + turma.getNome() + ".",
                null, descricaoMatricula(matricula));
        return matricula;
    }

    @Transactional
    public Matricula encerrar(Long matriculaId, MotivoSaida motivoSaida, String observacaoSaida, LocalDate dataSaida) {
        if (motivoSaida == null) {
            throw new RegraNegocioException("Informe o motivo da saída.");
        }
        Matricula matricula = obter(matriculaId);
        if (!matricula.isAtiva()) {
            throw new RegraNegocioException("Esta matrícula já está encerrada.");
        }
        String anterior = descricaoMatricula(matricula);
        matricula.encerrar(motivoSaida, textoOuNulo(observacaoSaida), dataSaida);
        Matricula salva = matriculaRepository.save(matricula);
        historicoService.registrar(ModuloHistorico.MATRICULAS, "Matrícula", salva.getParticipante().getId(),
                salva.getParticipante().getNome(), AcaoHistorico.SAIDA,
                "Saída de " + salva.getParticipante().getNome() + " da turma "
                        + salva.getAtividade().getNome() + " registrada. Motivo: "
                        + motivoSaida.getDescricao() + ".",
                anterior, descricaoMatricula(salva));
        return salva;
    }

    @Transactional
    public Matricula reativar(Long matriculaId) {
        Matricula anterior = obter(matriculaId);
        if (anterior.isAtiva()) {
            throw new RegraNegocioException("Esta matrícula já está ativa.");
        }
        Participante participante = anterior.getParticipante();
        Atividade turma = anterior.getAtividade();
        validarNovaMatricula(participante, turma);
        Matricula retorno = matriculaRepository.save(new Matricula(participante, turma, LocalDate.now()));
        historicoService.registrar(ModuloHistorico.MATRICULAS, "Matrícula", participante.getId(),
                participante.getNome(), AcaoHistorico.INGRESSO,
                "Retorno de " + participante.getNome() + " à turma " + turma.getNome()
                        + ". O histórico de presença anterior foi preservado.",
                descricaoMatricula(anterior), descricaoMatricula(retorno));
        return retorno;
    }

    @Transactional
    public Matricula trocarTurma(Long matriculaId, Long novaTurmaId, LocalDate data) {
        Matricula atual = obter(matriculaId);
        if (!atual.isAtiva()) {
            throw new RegraNegocioException("Só é possível trocar a turma de uma matrícula ativa.");
        }
        Atividade novaTurma = obterTurma(novaTurmaId);
        if (novaTurma.getId().equals(atual.getAtividade().getId())) {
            throw new RegraNegocioException("Escolha uma turma diferente da atual.");
        }
        Participante participante = atual.getParticipante();
        validarNovaMatricula(participante, novaTurma);
        LocalDate inicio = data == null ? LocalDate.now() : data;
        String turmaAnterior = atual.getAtividade().getNome();
        String antes = descricaoMatricula(atual);
        LocalDate saida = inicio.minusDays(1).isBefore(atual.getDataInicio()) ? atual.getDataInicio() : inicio.minusDays(1);
        atual.encerrar(MotivoSaida.TROCA_DE_TURMA, "para " + novaTurma.getNome(), saida);
        matriculaRepository.save(atual);
        Matricula nova = matriculaRepository.save(new Matricula(participante, novaTurma, inicio));
        historicoService.registrar(ModuloHistorico.MATRICULAS, "Matrícula", participante.getId(),
                participante.getNome(), AcaoHistorico.TROCA_TURMA,
                "Troca de turma de " + participante.getNome() + ": " + turmaAnterior + " → "
                        + novaTurma.getNome() + ". As presenças anteriores continuam no histórico.",
                antes, descricaoMatricula(nova));
        return nova;
    }

    private void validarNovaMatricula(Participante participante, Atividade turma) {
        if (!participante.isAtivo()) {
            throw new RegraNegocioException(
                    "Este participante está inativo. Ative o cadastro antes de matriculá-lo em uma turma.");
        }
        if (!turma.isAtivo()) {
            throw new RegraNegocioException(
                    "Esta turma está inativa e não recebe novas matrículas. Ative a turma antes de continuar.");
        }
        if (matriculaRepository.existsByParticipanteIdAndAtividadeIdAndStatus(
                participante.getId(), turma.getId(), StatusMatricula.ATIVA)) {
            throw new RegraNegocioException("Já existe uma matrícula ativa de " + participante.getNome()
                    + " na turma " + turma.getNome() + ".");
        }
    }

    private Participante obterParticipante(Long participanteId) {
        return participanteRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
    }

    private Atividade obterTurma(Long turmaId) {
        return atividadeRepository.findById(turmaId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
    }

    private String descricaoMatricula(Matricula matricula) {
        StringBuilder texto = new StringBuilder()
                .append("Turma: ").append(matricula.getAtividade().getNome())
                .append(" | Situação: ").append(matricula.getStatus().getDescricao());
        if (matricula.getMotivoSaida() != null) {
            texto.append(" | Motivo: ").append(matricula.getMotivoSaida().getDescricao());
        }
        if (matricula.getDataSaida() != null) {
            texto.append(" | Saída: ").append(matricula.getDataSaida());
        }
        return texto.toString();
    }

    private String textoOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }
}
