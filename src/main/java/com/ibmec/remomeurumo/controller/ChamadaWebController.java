package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.dto.ResultadoRegistroPresenca;
import com.ibmec.remomeurumo.dto.SessaoChamada;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.MotivoCancelamento;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.model.TipoAnexo;
import com.ibmec.remomeurumo.service.AnexoService;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ChamadaService;

import jakarta.servlet.http.HttpServletRequest;

@Controller
@RequestMapping("/chamada")
public class ChamadaWebController {

    private final ChamadaService chamadaService;
    private final AtividadeService atividadeService;
    private final AnexoService anexoService;

    public ChamadaWebController(ChamadaService chamadaService, AtividadeService atividadeService,
            AnexoService anexoService) {
        this.chamadaService = chamadaService;
        this.atividadeService = atividadeService;
        this.anexoService = anexoService;
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
        model.addAttribute("abas", chamadaService.abas());
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
            model.addAttribute("modoCorrecao", !chamada.isNaoRealizada() && (corrigir != null || !chamada.isRegistrada()));
            model.addAttribute("motivosCancelamento", MotivoCancelamento.values());
            model.addAttribute("abas", chamadaService.abas());
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
            HttpServletRequest requisicao,
            RedirectAttributes redirectAttributes) {
        try {
            Map<Long, StatusPresenca> marcacoes = marcacoes(parametros);
            ResultadoRegistroPresenca resultado = chamadaService.salvar(turmaId, data, marcacoes);
            Long encontroId = chamadaService.montar(turmaId, data).encontroId();
            if (requisicao instanceof MultipartHttpServletRequest multipart) {
                int anexados = 0;
                List<String> problemas = new ArrayList<>();
                for (Map.Entry<String, MultipartFile> arquivo : multipart.getFileMap().entrySet()) {
                    if (!arquivo.getKey().startsWith("atestado_") || arquivo.getValue().isEmpty()) {
                        continue;
                    }
                    Long participanteId = Long.valueOf(arquivo.getKey().substring("atestado_".length()));
                    if (marcacoes.get(participanteId) != StatusPresenca.JUSTIFICADA) {
                        continue;
                    }
                    try {
                        anexoService.anexar(participanteId, TipoAnexo.ATESTADO, arquivo.getValue(), encontroId);
                        anexados++;
                    } catch (RegraNegocioException ex) {
                        problemas.add(arquivo.getValue().getOriginalFilename() + ": " + ex.getMessage());
                    }
                }
                if (anexados > 0 || !problemas.isEmpty()) {
                    String texto = anexados == 0 ? "" : anexados == 1 ? "1 atestado anexado. " : anexados + " atestados anexados. ";
                    if (!problemas.isEmpty()) {
                        texto += "Não foi possível anexar: " + String.join("; ", problemas);
                    }
                    redirectAttributes.addFlashAttribute("aviso", texto.trim());
                }
            }
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

    @PostMapping("/{turmaId}/nao-aconteceu")
    public String aulaNaoAconteceu(@PathVariable Long turmaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) MotivoCancelamento motivo,
            @RequestParam(required = false) String justificativa,
            RedirectAttributes redirectAttributes) {
        try {
            chamadaService.registrarAulaNaoRealizada(turmaId, data, motivo, justificativa);
            redirectAttributes.addFlashAttribute("sucesso",
                    "Aula registrada como não realizada. Ela não conta na frequência de ninguém.");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/chamada/" + turmaId + "?data=" + data;
    }

    @PostMapping("/{turmaId}/desfazer-nao-aconteceu")
    public String desfazerAulaNaoAconteceu(@PathVariable Long turmaId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            RedirectAttributes redirectAttributes) {
        chamadaService.desfazerAulaNaoRealizada(turmaId, data);
        redirectAttributes.addFlashAttribute("sucesso", "Registro desfeito. A aula volta a contar na frequência.");
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
