package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class EncontroService {

    private final EncontroRepository encontroRepository;
    private final AtividadeRepository atividadeRepository;
    private final PresencaRepository presencaRepository;
    private final HistoricoOperacaoService historicoService;

    public EncontroService(EncontroRepository encontroRepository,
            AtividadeRepository atividadeRepository,
            PresencaRepository presencaRepository,
            HistoricoOperacaoService historicoService) {
        this.encontroRepository = encontroRepository;
        this.atividadeRepository = atividadeRepository;
        this.presencaRepository = presencaRepository;
        this.historicoService = historicoService;
    }

    @Transactional(readOnly = true)
    public List<Encontro> listar() {
        return encontroRepository.findAll(Sort.by(Sort.Direction.DESC, "data"));
    }

    @Transactional(readOnly = true)
    public List<Encontro> listarPorAtividade(Long atividadeId) {
        if (atividadeId == null) {
            return listar();
        }
        return encontroRepository.findByAtividadeIdOrderByDataDesc(atividadeId);
    }

    @Transactional(readOnly = true)
    public List<Encontro> listar(Long atividadeId, StatusEncontro status,
            LocalDate inicio, LocalDate fim, String busca) {
        String termo = busca == null || busca.isBlank() ? null : busca.trim();
        return encontroRepository.filtrar(atividadeId, status, inicio, fim, termo);
    }

    @Transactional(readOnly = true)
    public Encontro obter(Long id) {
        return encontroRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Encontro não encontrado."));
    }

    @Transactional
    public Encontro criar(Long atividadeId, LocalDate data, String observacoes) {
        return criar(atividadeId, data, observacoes, StatusEncontro.REALIZADO);
    }

    @Transactional
    public Encontro criar(Long atividadeId, LocalDate data, String observacoes, StatusEncontro status) {
        Atividade atividade = obterAtividadeAtiva(atividadeId);
        validarData(data);
        encontroRepository.findByAtividadeIdAndData(atividadeId, data).ifPresent(encontro -> {
            throw new RegraNegocioException("Já existe um encontro dessa atividade nessa data.");
        });
        Encontro novo = new Encontro(atividade, data, observacoes);
        novo.setStatus(status == null ? StatusEncontro.REALIZADO : status);
        Encontro salvo = encontroRepository.save(novo);
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Encontro", salvo.getId(), nome(salvo),
                AcaoHistorico.CRIACAO, "Encontro criado com status "
                        + salvo.getStatus().getDescricao().toLowerCase() + ".");
        return salvo;
    }

    @Transactional
    public Encontro atualizar(Long id, Long atividadeId, LocalDate data, String observacoes, StatusEncontro status) {
        Encontro encontro = obter(id);
        Atividade atividade = obterAtividadeAtiva(atividadeId);
        validarData(data);
        StatusEncontro statusAnterior = encontro.getStatus();
        String detalhesAnteriores = "Atividade: " + encontro.getAtividade().getNome()
                + " | Data: " + encontro.getData() + " | Status: " + encontro.getStatus().getDescricao();

        encontroRepository.findByAtividadeIdAndData(atividadeId, data)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("Já existe um encontro dessa atividade nessa data.");
                });

        encontro.setAtividade(atividade);
        encontro.setData(data);
        encontro.setObservacoes(observacoes);
        encontro.setStatus(status == null ? StatusEncontro.REALIZADO : status);
        Encontro salvo = encontroRepository.save(encontro);
        AcaoHistorico acao = statusAnterior != salvo.getStatus() && salvo.getStatus() == StatusEncontro.CANCELADO
                ? AcaoHistorico.CANCELAMENTO : AcaoHistorico.EDICAO;
        String descricao = statusAnterior == salvo.getStatus()
                ? "Os dados do encontro foram atualizados."
                : "Encontro atualizado e status alterado de " + statusAnterior.getDescricao()
                        + " para " + salvo.getStatus().getDescricao() + ".";
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Encontro", salvo.getId(), nome(salvo),
                acao, descricao, detalhesAnteriores,
                "Atividade: " + salvo.getAtividade().getNome() + " | Data: " + salvo.getData()
                        + " | Status: " + salvo.getStatus().getDescricao());
        return salvo;
    }

    @Transactional(readOnly = true)
    public boolean podeExcluir(Long id) {
        obter(id);
        return !presencaRepository.existsByEncontroId(id);
    }

    @Transactional(readOnly = true)
    public long contarPresencas(Long id) {
        obter(id);
        return presencaRepository.countByEncontroId(id);
    }

    @Transactional
    public void excluir(Long id) {
        Encontro encontro = obter(id);
        if (presencaRepository.existsByEncontroId(id)) {
            throw new RegraNegocioException(
                    "Este encontro possui registros de presença e não pode ser excluído. Cancele-o para preservar o histórico.");
        }
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Encontro", encontro.getId(), nome(encontro),
                AcaoHistorico.EXCLUSAO, "Encontro excluído definitivamente porque não possuía registros de presença.");
        encontroRepository.delete(encontro);
    }

    @Transactional
    public void cancelar(Long id) {
        Encontro encontro = obter(id);
        if (encontro.getStatus() == StatusEncontro.CANCELADO) {
            return;
        }
        encontro.setStatus(StatusEncontro.CANCELADO);
        encontroRepository.save(encontro);
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Encontro", encontro.getId(), nome(encontro),
                AcaoHistorico.CANCELAMENTO, "Encontro cancelado. Os registros anteriores foram preservados.");
    }

    @Transactional(readOnly = true)
    public long contarNoMes() {
        LocalDate hoje = LocalDate.now();
        return encontroRepository.countByDataBetween(hoje.withDayOfMonth(1), hoje.withDayOfMonth(hoje.lengthOfMonth()));
    }

    @Transactional(readOnly = true)
    public long contarTodos() {
        return encontroRepository.count();
    }

    @Transactional(readOnly = true)
    public long contarPorStatus(StatusEncontro status) {
        return encontroRepository.countByStatus(status);
    }

    @Transactional(readOnly = true)
    public long contarSemPresenca() {
        return encontroRepository.contarSemPresencaRegistrada();
    }

    private Atividade obterAtividadeAtiva(Long atividadeId) {
        if (atividadeId == null) {
            throw new RegraNegocioException("Informe a atividade.");
        }
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Atividade não encontrada."));
        if (!atividade.isAtivo()) {
            throw new RegraNegocioException("Escolha uma atividade ativa para criar encontro.");
        }
        return atividade;
    }

    private void validarData(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Informe a data do encontro.");
        }
    }

    private String nome(Encontro encontro) {
        return encontro.getAtividade().getNome() + " - " + encontro.getData();
    }
}
