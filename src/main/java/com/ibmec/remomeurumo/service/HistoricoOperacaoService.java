package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.IndicadoresHistorico;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.HistoricoOperacao;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.repository.HistoricoOperacaoRepository;

@Service
public class HistoricoOperacaoService {

    private final HistoricoOperacaoRepository historicoRepository;

    public HistoricoOperacaoService(HistoricoOperacaoRepository historicoRepository) {
        this.historicoRepository = historicoRepository;
    }

    @Transactional
    public HistoricoOperacao registrar(ModuloHistorico modulo, String tipoEntidade, Long entidadeId,
            String nomeEntidade, AcaoHistorico acao, String descricao) {
        return registrar(modulo, tipoEntidade, entidadeId, nomeEntidade, acao, descricao, null, null);
    }

    @Transactional
    public HistoricoOperacao registrar(ModuloHistorico modulo, String tipoEntidade, Long entidadeId,
            String nomeEntidade, AcaoHistorico acao, String descricao,
            String detalhesAnteriores, String detalhesNovos) {
        HistoricoOperacao registro = new HistoricoOperacao(
                modulo,
                limitar(tipoEntidade, 60),
                entidadeId,
                limitar(nomeEntidade, 180),
                acao,
                limitar(descricao, 600),
                limitarOpcional(detalhesAnteriores, 500),
                limitarOpcional(detalhesNovos, 500));
        return historicoRepository.save(registro);
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacao> listar(LocalDate inicio, LocalDate fim, ModuloHistorico modulo,
            AcaoHistorico acao, String texto) {
        LocalDateTime inicioDataHora = inicio == null ? null : inicio.atStartOfDay();
        LocalDateTime fimExclusivo = fim == null ? null : fim.plusDays(1).atStartOfDay();
        String termo = temTexto(texto) ? texto.trim() : null;
        return historicoRepository.filtrar(inicioDataHora, fimExclusivo, modulo, acao, termo);
    }

    @Transactional(readOnly = true)
    public Map<LocalDate, List<HistoricoOperacao>> agruparPorData(List<HistoricoOperacao> registros) {
        Map<LocalDate, List<HistoricoOperacao>> grupos = new LinkedHashMap<>();
        registros.forEach(registro -> grupos.computeIfAbsent(
                registro.getDataHora().toLocalDate(), chave -> new java.util.ArrayList<>()).add(registro));
        return grupos;
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacao> ultimasAlteracoes() {
        return historicoRepository.findTop8ByOrderByDataHoraDescIdDesc();
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacao> porEntidade(ModuloHistorico modulo, Long entidadeId) {
        return historicoRepository.findTop8ByEntidadeIdAndModuloInOrderByDataHoraDescIdDesc(
                entidadeId, List.of(modulo));
    }

    @Transactional(readOnly = true)
    public List<HistoricoOperacao> porEncontro(Long encontroId) {
        return historicoRepository.findTop8ByEntidadeIdAndModuloInOrderByDataHoraDescIdDesc(
                encontroId, List.of(ModuloHistorico.ENCONTROS, ModuloHistorico.PRESENCAS));
    }

    @Transactional(readOnly = true)
    public IndicadoresHistorico carregarIndicadores() {
        LocalDateTime agora = LocalDateTime.now();
        long hoje = historicoRepository.countByDataHoraBetween(LocalDate.now().atStartOfDay(), agora.plusSeconds(1));
        long seteDias = historicoRepository.countByDataHoraBetween(LocalDate.now().minusDays(6).atStartOfDay(),
                agora.plusSeconds(1));
        long cadastros = historicoRepository.countByAcao(AcaoHistorico.CRIACAO);
        long alteracoes = historicoRepository.countByAcaoIn(EnumSet.of(
                AcaoHistorico.EDICAO,
                AcaoHistorico.ATIVACAO,
                AcaoHistorico.DESATIVACAO,
                AcaoHistorico.CORRECAO,
                AcaoHistorico.CANCELAMENTO));
        return new IndicadoresHistorico(hoje, seteDias, cadastros, alteracoes);
    }

    private String limitar(String valor, int limite) {
        String texto = valor == null ? "" : valor.trim();
        return texto.length() <= limite ? texto : texto.substring(0, limite);
    }

    private String limitarOpcional(String valor, int limite) {
        if (!temTexto(valor)) {
            return null;
        }
        return limitar(valor, limite);
    }

    private boolean temTexto(String valor) {
        return valor != null && !valor.trim().isEmpty();
    }
}
