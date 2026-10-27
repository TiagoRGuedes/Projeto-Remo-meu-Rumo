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
import com.ibmec.remomeurumo.model.MotivoCancelamento;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.repository.AnexoRepository;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.PresencaRepository;

@Service
public class EncontroService {

    private final EncontroRepository encontroRepository;
    private final AtividadeRepository atividadeRepository;
    private final PresencaRepository presencaRepository;
    private final AnexoRepository anexoRepository;
    private final HistoricoOperacaoService historicoService;

    public EncontroService(EncontroRepository encontroRepository,
            AtividadeRepository atividadeRepository,
            PresencaRepository presencaRepository,
            AnexoRepository anexoRepository,
            HistoricoOperacaoService historicoService) {
        this.anexoRepository = anexoRepository;
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
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula não encontrada."));
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
            throw new RegraNegocioException("Já existe uma aula dessa turma nessa data.");
        });
        Encontro novo = new Encontro(atividade, data, observacoes);
        novo.setStatus(status == null ? StatusEncontro.REALIZADO : status);
        Encontro salvo = encontroRepository.save(novo);
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Aula", salvo.getId(), nome(salvo),
                AcaoHistorico.CRIACAO, "Aula criada com status "
                        + salvo.getStatus().getDescricao().toLowerCase() + ".");
        return salvo;
    }

    @Transactional
    public Encontro atualizar(Long id, Long atividadeId, LocalDate data, String observacoes, StatusEncontro status) {
        Encontro encontro = obter(id);
        Atividade atividade = obterAtividadeAtiva(atividadeId);
        validarData(data);
        StatusEncontro statusAnterior = encontro.getStatus();
        String detalhesAnteriores = "Turma: " + encontro.getAtividade().getNome()
                + " | Data: " + encontro.getData() + " | Status: " + encontro.getStatus().getDescricao();

        encontroRepository.findByAtividadeIdAndData(atividadeId, data)
                .filter(outro -> !outro.getId().equals(id))
                .ifPresent(outro -> {
                    throw new RegraNegocioException("Já existe uma aula dessa turma nessa data.");
                });

        encontro.setAtividade(atividade);
        encontro.setData(data);
        encontro.setObservacoes(observacoes);
        encontro.setStatus(status == null ? StatusEncontro.REALIZADO : status);
        if (encontro.getStatus() != StatusEncontro.CANCELADO) {
            encontro.setMotivoCancelamento(null);
            encontro.setJustificativaCancelamento(null);
        }
        Encontro salvo = encontroRepository.save(encontro);
        AcaoHistorico acao = statusAnterior != salvo.getStatus() && salvo.getStatus() == StatusEncontro.CANCELADO
                ? AcaoHistorico.CANCELAMENTO : AcaoHistorico.EDICAO;
        String descricao = statusAnterior == salvo.getStatus()
                ? "Os dados da aula foram atualizados."
                : "Aula atualizada e status alterado de " + statusAnterior.getDescricao()
                        + " para " + salvo.getStatus().getDescricao() + ".";
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Aula", salvo.getId(), nome(salvo),
                acao, descricao, detalhesAnteriores,
                "Turma: " + salvo.getAtividade().getNome() + " | Data: " + salvo.getData()
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
                    "Esta aula possui registros de presença e não pode ser excluída. Cancele-a para preservar o histórico.");
        }
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Aula", encontro.getId(), nome(encontro),
                AcaoHistorico.EXCLUSAO, "Aula excluída definitivamente porque não possuía registros de presença.");
        anexoRepository.desvincularAula(id);
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
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Aula", encontro.getId(), nome(encontro),
                AcaoHistorico.CANCELAMENTO, "Aula cancelada. Os registros anteriores foram preservados.");
    }

    @Transactional
    public Encontro registrarAulaNaoRealizada(Long atividadeId, LocalDate data, MotivoCancelamento motivo,
            String justificativa) {
        if (motivo == null) {
            throw new RegraNegocioException("Escolha por que a aula não aconteceu.");
        }
        String texto = justificativa == null || justificativa.isBlank() ? null : justificativa.trim();
        if (texto != null && texto.length() > 300) {
            throw new RegraNegocioException("A justificativa deve ter no máximo 300 caracteres.");
        }
        Encontro encontro = encontroRepository.findByAtividadeIdAndData(atividadeId, data)
                .orElseGet(() -> {
                    Atividade atividade = obterAtividadeAtiva(atividadeId);
                    validarData(data);
                    return new Encontro(atividade, data, null);
                });
        StatusEncontro anterior = encontro.getId() == null ? null : encontro.getStatus();
        encontro.setStatus(StatusEncontro.CANCELADO);
        encontro.setMotivoCancelamento(motivo);
        encontro.setJustificativaCancelamento(texto);
        Encontro salvo = encontroRepository.save(encontro);
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Aula", salvo.getId(), nome(salvo),
                AcaoHistorico.CANCELAMENTO, "Aula não aconteceu: " + salvo.getMotivoCancelamentoTexto()
                        + ". Ela não conta na frequência de ninguém.",
                anterior == null ? null : "Status: " + anterior.getDescricao(),
                "Status: " + salvo.getStatus().getDescricao() + " | Motivo: " + salvo.getMotivoCancelamentoTexto());
        return salvo;
    }

    @Transactional
    public Encontro desfazerAulaNaoRealizada(Long id) {
        Encontro encontro = obter(id);
        if (encontro.getStatus() != StatusEncontro.CANCELADO) {
            return encontro;
        }
        String motivoAnterior = encontro.getMotivoCancelamentoTexto();
        encontro.setStatus(presencaRepository.existsByEncontroId(id) ? StatusEncontro.REALIZADO : StatusEncontro.PLANEJADO);
        encontro.setMotivoCancelamento(null);
        encontro.setJustificativaCancelamento(null);
        Encontro salvo = encontroRepository.save(encontro);
        historicoService.registrar(ModuloHistorico.ENCONTROS, "Aula", salvo.getId(), nome(salvo),
                AcaoHistorico.EDICAO, "Registro de aula não realizada desfeito. A aula volta a contar na frequência.",
                "Status: Cancelada | Motivo: " + motivoAnterior, "Status: " + salvo.getStatus().getDescricao());
        return salvo;
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
            throw new RegraNegocioException("Informe a turma.");
        }
        Atividade atividade = atividadeRepository.findById(atividadeId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Turma não encontrada."));
        if (!atividade.isAtivo()) {
            throw new RegraNegocioException("Escolha uma turma ativa para criar aula.");
        }
        return atividade;
    }

    private void validarData(LocalDate data) {
        if (data == null) {
            throw new RegraNegocioException("Informe a data da aula.");
        }
    }

    private String nome(Encontro encontro) {
        return encontro.getAtividade().getNome() + " - " + encontro.getData();
    }
}
