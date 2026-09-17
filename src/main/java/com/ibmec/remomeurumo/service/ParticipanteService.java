package com.ibmec.remomeurumo.service;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class ParticipanteService {

    private final ParticipanteRepository participanteRepository;
    private final PresencaRepository presencaRepository;
    private final HistoricoOperacaoService historicoService;

    public ParticipanteService(ParticipanteRepository participanteRepository,
            PresencaRepository presencaRepository,
            HistoricoOperacaoService historicoService) {
        this.participanteRepository = participanteRepository;
        this.presencaRepository = presencaRepository;
        this.historicoService = historicoService;
    }

    @Transactional(readOnly = true)
    public List<Participante> listar(String busca) {
        return listar(busca, null);
    }

    @Transactional(readOnly = true)
    public List<Participante> listar(String busca, String situacao) {
        boolean somenteAtivos = "ativos".equalsIgnoreCase(situacao);
        boolean somenteInativos = "inativos".equalsIgnoreCase(situacao);
        if (temTexto(busca)) {
            return participanteRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(busca.trim()).stream()
                    .filter(item -> !somenteAtivos || item.isAtivo())
                    .filter(item -> !somenteInativos || !item.isAtivo())
                    .toList();
        }
        if (somenteAtivos) {
            return participanteRepository.findByAtivoTrueOrderByNomeAsc();
        }
        if (somenteInativos) {
            return participanteRepository.findByAtivoFalseOrderByNomeAsc();
        }
        return participanteRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    @Transactional(readOnly = true)
    public List<Participante> listarAtivos() {
        return participanteRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Participante obter(Long id) {
        return participanteRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
    }

    @Transactional
    public Participante cadastrar(Participante participante) {
        validar(participante);
        participante.setNome(participante.getNome().trim());
        Participante salvo = participanteRepository.save(participante);
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", salvo.getId(), salvo.getNome(),
                AcaoHistorico.CRIACAO, "Participante cadastrado no sistema.");
        return salvo;
    }

    @Transactional
    public Participante atualizar(Long id, Participante dados) {
        validar(dados);
        Participante participante = obter(id);
        String detalhesAnteriores = "Nome: " + participante.getNome()
                + " | Situação: " + (participante.isAtivo() ? "Ativo" : "Inativo");
        participante.setNome(dados.getNome().trim());
        participante.setObservacoes(dados.getObservacoes());
        Participante salvo = participanteRepository.save(participante);
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", salvo.getId(), salvo.getNome(),
                AcaoHistorico.EDICAO, "O cadastro do participante foi atualizado.",
                detalhesAnteriores,
                "Nome: " + salvo.getNome() + " | Situação: " + (salvo.isAtivo() ? "Ativo" : "Inativo"));
        return salvo;
    }

    @Transactional
    public void ativar(Long id) {
        Participante participante = obter(id);
        if (participante.isAtivo()) {
            return;
        }
        participante.ativar();
        participanteRepository.save(participante);
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", participante.getId(),
                participante.getNome(), AcaoHistorico.ATIVACAO, "Participante ativado para novas listas de presença.");
    }

    @Transactional
    public void desativar(Long id) {
        Participante participante = obter(id);
        if (!participante.isAtivo()) {
            return;
        }
        participante.desativar();
        participanteRepository.save(participante);
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", participante.getId(),
                participante.getNome(), AcaoHistorico.DESATIVACAO,
                "Participante desativado. O histórico de presença foi preservado.");
    }

    @Transactional(readOnly = true)
    public boolean podeExcluir(Long id) {
        obter(id);
        return !presencaRepository.existsByParticipanteId(id);
    }

    @Transactional(readOnly = true)
    public long contarPresencas(Long id) {
        obter(id);
        return presencaRepository.countByParticipanteId(id);
    }

    @Transactional
    public void excluir(Long id) {
        Participante participante = obter(id);
        if (presencaRepository.existsByParticipanteId(id)) {
            throw new RegraNegocioException(
                    "Este participante possui histórico de presença e não pode ser excluído. Desative-o para preservar os registros.");
        }
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", participante.getId(),
                participante.getNome(), AcaoHistorico.EXCLUSAO,
                "Participante excluído definitivamente porque não possuía registros de presença.");
        participanteRepository.delete(participante);
    }

    @Transactional(readOnly = true)
    public long contarAtivos() {
        return participanteRepository.countByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public long contarInativos() {
        return participanteRepository.countByAtivoFalse();
    }

    @Transactional(readOnly = true)
    public long contarRecentes() {
        return participanteRepository.countByDataCadastroAfter(LocalDateTime.now().minusDays(30));
    }

    private void validar(Participante participante) {
        if (participante == null || !temTexto(participante.getNome())) {
            throw new RegraNegocioException("Informe o nome do participante.");
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}
