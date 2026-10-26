package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.dto.ResultadoRegistroPresenca;
import com.ibmec.remomeurumo.dto.SessaoChamada;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ChamadaService;

@Controller
@RequestMapping("/chamada")
public class ChamadaWebController {

    private final ChamadaService chamadaService;
    private final AtividadeService atividadeService;

    public ChamadaWebController(ChamadaService chamadaService, AtividadeService atividadeService) {
        this.chamadaService = chamadaService;
        this.atividadeService = atividadeService;
    }

    @GetMapping
    public String sessoes(@RequestParam(required = false) String lista, Model model) {
        if (lista == null) {
            var atual = chamadaService.sessaoAtual();
            if (atual.isPresent()) {
                return "redirect:" + endereco(atual.get());
            }
        }
        model.addAttribute("sessoesDeHoje", chamadaService.sessoesDeHoje());
        model.addAttribute("pendentes", chamadaService.pendentes());
        model.addAttribute("turmas", atividadeService.listarAtivas());
        model.addAttribute("hoje", LocalDate.now());
        return "chamada/index";
    }

    @GetMapping("/escolher")
    public String escolher(@RequestParam Long turmaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data) {
        return "redirect:/chamada/" + turmaId + "?data=" + data;
    }

    @GetMapping("/{turmaId}")
    public String chamada(@PathVariable Long turmaId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String corrigir,
            Model model, RedirectAttributes redirectAttributes) {
        LocalDate dia = data == null ? LocalDate.now() : data;
        try {
            var chamada = chamadaService.montar(turmaId, dia);
            model.addAttribute("chamada", chamada);
            model.addAttribute("modoCorrecao", corrigir != null || !chamada.isRegistrada());
            return "chamada/turma";
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/chamada?lista=1";
        }
    }

    @PostMapping("/{turmaId}")
    public String salvar(@PathVariable Long turmaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam Map<String, String> parametros,
            RedirectAttributes redirectAttributes) {
        try {
            ResultadoRegistroPresenca resultado = chamadaService.salvar(turmaId, data, marcacoes(parametros));
            redirectAttributes.addFlashAttribute("sucesso", "Chamada salva: "
                    + resultado.presentes() + (resultado.presentes() == 1 ? " presente, " : " presentes, ")
                    + resultado.ausentes() + (resultado.ausentes() == 1 ? " falta e " : " faltas e ")
                    + resultado.justificadas() + (resultado.justificadas() == 1 ? " falta avisada." : " faltas avisadas."));
            redirectAttributes.addFlashAttribute("chamadaSalva", true);
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/chamada/" + turmaId + "?data=" + data + "&corrigir=1";
        }
        return "redirect:/chamada/" + turmaId + "?data=" + data;
    }

    private Map<Long, StatusPresenca> marcacoes(Map<String, String> parametros) {
        Map<Long, StatusPresenca> marcacoes = new LinkedHashMap<>();
        parametros.forEach((chave, valor) -> {
            if (!chave.startsWith("status_") || valor == null || valor.isBlank()) {
                return;
            }
            try {
                marcacoes.put(Long.valueOf(chave.substring("status_".length())), StatusPresenca.valueOf(valor));
            } catch (IllegalArgumentException ex) {
                throw new RegraNegocioException("Uma marcação da chamada é inválida. Recarregue a página e tente novamente.");
            }
        });
        return marcacoes;
    }

    private String endereco(SessaoChamada sessao) {
        return "/chamada/" + sessao.turmaId() + "?data=" + sessao.data();
    }
}
