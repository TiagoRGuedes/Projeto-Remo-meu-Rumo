package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
        List<Long> jaMatriculado = matriculaRepository.listarPorParticipante(participanteId).stream()
                .map(matricula -> matricula.getAtividade().getId())
                .toList();
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc().stream()
                .filter(atividade -> !jaMatriculado.contains(atividade.getId()))
                .toList();
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
        if (!participante.isAtivo()) {
            throw new RegraNegocioException(
                    "Este participante está inativo. Ative o cadastro antes de matriculá-lo em uma turma.");
        }
        if (!turma.isAtivo()) {
            throw new RegraNegocioException(
                    "Esta turma está inativa e não recebe novas matrículas. Ative a turma antes de continuar.");
        }

        Matricula existente = matriculaRepository
                .findByParticipanteIdAndAtividadeId(participanteId, turmaId)
                .orElse(null);
        if (existente != null) {
            if (existente.isAtiva()) {
                throw new RegraNegocioException("Já existe uma matrícula ativa de " + participante.getNome()
                        + " na turma " + turma.getNome() + ".");
            }
            return reativar(existente.getId());
        }

        Matricula matricula = matriculaRepository.save(new Matricula(participante, turma, dataInicio));
        historicoService.registrar(ModuloHistorico.MATRICULAS, "Matrícula", participante.getId(),
                participante.getNome(), AcaoHistorico.CRIACAO,
                "Matrícula de " + participante.getNome() + " criada na turma " + turma.getNome() + ".",
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
                salva.getParticipante().getNome(), AcaoHistorico.DESATIVACAO,
                "Saída de " + salva.getParticipante().getNome() + " da turma "
                        + salva.getAtividade().getNome() + " registrada. Motivo: "
                        + motivoSaida.getDescricao() + ".",
                anterior, descricaoMatricula(salva));
        return salva;
    }

    @Transactional
    public Matricula reativar(Long matriculaId) {
        Matricula matricula = obter(matriculaId);
        if (matricula.isAtiva()) {
            throw new RegraNegocioException("Esta matrícula já está ativa.");
        }
        if (!matricula.getParticipante().isAtivo()) {
            throw new RegraNegocioException(
                    "Este participante está inativo. Ative o cadastro antes de reativar a matrícula.");
        }
        String anterior = descricaoMatricula(matricula);
        matricula.reativar();
        Matricula salva = matriculaRepository.save(matricula);
        historicoService.registrar(ModuloHistorico.MATRICULAS, "Matrícula", salva.getParticipante().getId(),
                salva.getParticipante().getNome(), AcaoHistorico.ATIVACAO,
                "Retorno de " + salva.getParticipante().getNome() + " à turma "
                        + salva.getAtividade().getNome()
                        + ". O histórico de presença anterior foi preservado.",
                anterior, descricaoMatricula(salva));
        return salva;
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
