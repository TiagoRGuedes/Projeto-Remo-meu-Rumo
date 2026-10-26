package com.ibmec.remomeurumo.controller;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.HistoricoOperacao;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;

@Controller
public class HistoricoWebController {

    private static final Pattern DATA_BUSCA = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{4}))?");

    private static final List<TipoRapido> TIPOS_RAPIDOS = List.of(
            new TipoRapido("Ingresso", null, AcaoHistorico.INGRESSO),
            new TipoRapido("Saída", null, AcaoHistorico.SAIDA),
            new TipoRapido("Troca de turma", null, AcaoHistorico.TROCA_TURMA),
            new TipoRapido("Chamada", ModuloHistorico.PRESENCAS, null),
            new TipoRapido("Aula não aconteceu", ModuloHistorico.ENCONTROS, AcaoHistorico.CANCELAMENTO),
            new TipoRapido("Arquivo", null, AcaoHistorico.ARQUIVO));

    private final HistoricoOperacaoService historicoService;

    public HistoricoWebController(HistoricoOperacaoService historicoService) {
        this.historicoService = historicoService;
    }

    @GetMapping("/historico")
    public String historico(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) ModuloHistorico modulo,
            @RequestParam(required = false) AcaoHistorico acao,
            @RequestParam(required = false) String busca,
            Model model) {
        String termo = busca;
        LocalDate dataBuscada = dataDaBusca(busca);
        if (dataBuscada != null && inicio == null && fim == null) {
            inicio = dataBuscada;
            fim = dataBuscada;
            termo = null;
        }
        List<HistoricoOperacao> registros = historicoService.listar(inicio, fim, modulo, acao, termo);
        model.addAttribute("indicadores", historicoService.carregarIndicadores());
        model.addAttribute("historicoPorData", historicoService.agruparPorData(registros));
        model.addAttribute("totalResultados", registros.size());
        model.addAttribute("modulos", ModuloHistorico.values());
        model.addAttribute("acoes", AcaoHistorico.values());
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("modulo", modulo);
        model.addAttribute("acao", acao);
        model.addAttribute("busca", busca);
        model.addAttribute("tiposRapidos", TIPOS_RAPIDOS);
        return "historico/index";
    }

    private LocalDate dataDaBusca(String busca) {
        if (busca == null) {
            return null;
        }
        Matcher matcher = DATA_BUSCA.matcher(busca.trim());
        if (!matcher.matches()) {
            return null;
        }
        int ano = matcher.group(3) == null ? LocalDate.now().getYear() : Integer.parseInt(matcher.group(3));
        try {
            return LocalDate.of(ano, Integer.parseInt(matcher.group(2)), Integer.parseInt(matcher.group(1)));
        } catch (DateTimeException ex) {
            return null;
        }
    }

    public record TipoRapido(String rotulo, ModuloHistorico modulo, AcaoHistorico acao) {
    }
}
