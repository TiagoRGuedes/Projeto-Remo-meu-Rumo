package com.ibmec.remomeurumo.service;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.MatriculaRepository;

@Service
public class AtividadeService {

    private final AtividadeRepository atividadeRepository;
    private final EncontroRepository encontroRepository;
    private final MatriculaRepository matriculaRepository;
    private final HistoricoOperacaoService historicoService;

    public AtividadeService(AtividadeRepository atividadeRepository,
            EncontroRepository encontroRepository,
            MatriculaRepository matriculaRepository,
            HistoricoOperacaoService historicoService) {
        this.atividadeRepository = atividadeRepository;
        this.encontroRepository = encontroRepository;
        this.matriculaRepository = matriculaRepository;
        this.historicoService = historicoService;
    }

    private String resumoAtividade(Atividade atividade) {
        return "Nome: " + atividade.getNome()
                + " | Grade: " + atividade.getGradeResumo()
                + " | Situação: " + (atividade.isAtivo() ? "Ativa" : "Inativa");
    }

    @Transactional(readOnly = true)
    public List<Atividade> listar(String busca) {
        return listar(busca, null);
    }

    @Transactional(readOnly = true)
    public List<Atividade> listar(String busca, String situacao) {
        boolean somenteAtivas = "ativas".equalsIgnoreCase(situacao);
        boolean somenteInativas = "inativas".equalsIgnoreCase(situacao);
        if (temTexto(busca)) {
            return atividadeRepository.findByNomeContainingIgnoreCaseOrderByNomeAsc(busca.trim()).stream()
                    .filter(item -> !somenteAtivas || item.isAtivo())
                    .filter(item -> !somenteInativas || !item.isAtivo())
                    .toList();
        }
        if (somenteAtivas) {
            return atividadeRepository.findByAtivoTrueOrderByNomeAsc();
        }
        if (somenteInativas) {
            return atividadeRepository.findByAtivoFalseOrderByNomeAsc();
        }
        return atividadeRepository.findAll(Sort.by(Sort.Direction.ASC, "nome"));
    }

    @Transactional(readOnly = true)
    public List<Atividade> listarAtivas() {
        return atividadeRepository.findByAtivoTrueOrderByNomeAsc();
    }

    @Transactional(readOnly = true)
    public Atividade obter(Long id) {
        return atividadeRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
    }

    @Transactional
    public Atividade cadastrar(Atividade atividade) {
        validar(atividade);
        atividade.setNome(atividade.getNome().trim());
        Atividade salva = atividadeRepository.save(atividade);
        historicoService.registrar(ModuloHistorico.ATIVIDADES, "Turma", salva.getId(), salva.getNome(),
                AcaoHistorico.CRIACAO, "Turma cadastrada no sistema. Grade: " + salva.getGradeResumo() + ".");
        return salva;
    }

    @Transactional
    public Atividade atualizar(Long id, Atividade dados) {
        validar(dados);
        Atividade atividade = obter(id);
        String detalhesAnteriores = resumoAtividade(atividade);
        atividade.setNome(dados.getNome().trim());
        atividade.setDescricao(dados.getDescricao());
        atividade.setDiasSemana(dados.getDiasSemana());
        atividade.setHorario(dados.getHorario());
        Atividade salva = atividadeRepository.save(atividade);
        historicoService.registrar(ModuloHistorico.ATIVIDADES, "Turma", salva.getId(), salva.getNome(),
                AcaoHistorico.EDICAO, "Os dados da turma foram atualizados.",
                detalhesAnteriores, resumoAtividade(salva));
        return salva;
    }

    @Transactional
    public void ativar(Long id) {
        Atividade atividade = obter(id);
        if (atividade.isAtivo()) {
            return;
        }
        atividade.ativar();
        atividadeRepository.save(atividade);
        historicoService.registrar(ModuloHistorico.ATIVIDADES, "Turma", atividade.getId(), atividade.getNome(),
                AcaoHistorico.ATIVACAO, "Turma ativada para novas aulas.");
    }

    @Transactional
    public void desativar(Long id) {
        Atividade atividade = obter(id);
        if (!atividade.isAtivo()) {
            return;
        }
        atividade.desativar();
        atividadeRepository.save(atividade);
        historicoService.registrar(ModuloHistorico.ATIVIDADES, "Turma", atividade.getId(), atividade.getNome(),
                AcaoHistorico.DESATIVACAO, "Turma desativada. As aulas anteriores foram preservadas.");
    }

    @Transactional(readOnly = true)
    public boolean podeExcluir(Long id) {
        obter(id);
        return !encontroRepository.existsByAtividadeId(id) && !matriculaRepository.existsByAtividadeId(id);
    }

    @Transactional(readOnly = true)
    public long contarEncontros(Long id) {
        obter(id);
        return encontroRepository.countByAtividadeId(id);
    }

    @Transactional
    public void excluir(Long id) {
        Atividade atividade = obter(id);
        if (encontroRepository.existsByAtividadeId(id)) {
            throw new RegraNegocioException(
                    "Esta turma possui aulas e não pode ser excluída. Desative-a para preservar o histórico.");
        }
        if (matriculaRepository.existsByAtividadeId(id)) {
            throw new RegraNegocioException(
                    "Esta turma possui matrículas e não pode ser excluída. Desative-a para preservar o histórico.");
        }
        historicoService.registrar(ModuloHistorico.ATIVIDADES, "Turma", atividade.getId(), atividade.getNome(),
                AcaoHistorico.EXCLUSAO, "Turma excluída definitivamente porque não possuía aulas.");
        atividadeRepository.delete(atividade);
    }

    @Transactional(readOnly = true)
    public long contarAtivas() {
        return atividadeRepository.countByAtivoTrue();
    }

    @Transactional(readOnly = true)
    public long contarInativas() {
        return atividadeRepository.countByAtivoFalse();
    }

    private void validar(Atividade atividade) {
        if (atividade == null || !temTexto(atividade.getNome())) {
            throw new RegraNegocioException("Informe o nome da turma.");
        }
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}
