package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.dto.RegistroPresencaItem;
import com.ibmec.remomeurumo.dto.RegistroPresencaResumo;
import com.ibmec.remomeurumo.dto.ConfirmacaoOperacao;
import com.ibmec.remomeurumo.dto.TipoOperacaoConfirmacao;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusPresenca;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/presencas")
public class PresencaWebController {

    private final AtividadeService atividadeService;
    private final EncontroService encontroService;
    private final ParticipanteService participanteService;
    private final PresencaService presencaService;
    private final ConfirmacaoOperacaoService confirmacaoService;

    public PresencaWebController(AtividadeService atividadeService,
            EncontroService encontroService,
            ParticipanteService participanteService,
            PresencaService presencaService,
            ConfirmacaoOperacaoService confirmacaoService) {
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
        this.participanteService = participanteService;
        this.presencaService = presencaService;
        this.confirmacaoService = confirmacaoService;
    }

    @GetMapping
    public String presencas() {
        return "redirect:/presencas/registrar";
    }

    @GetMapping("/registrar")
    public String registrar(@RequestParam(required = false) Long atividadeId,
            @RequestParam(required = false) Long encontroId,
            @RequestParam(required = false) String busca,
            @RequestParam(required = false) String marcar,
            Model model) {
        preencherPagina(atividadeId, encontroId, busca, marcar, null, model);
        return "presencas/registrar";
    }

    @PostMapping("/encontro-rapido")
    public String criarEncontroRapido(@RequestParam Long atividadeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String observacoes,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            String atividade = atividadeService.obter(atividadeId).getNome();
            Map<String, String> dados = new LinkedHashMap<>();
            dados.put("atividadeId", atividadeId.toString());
            dados.put("data", data.toString());
            dados.put("observacoes", Objects.toString(observacoes, ""));
            dados.put("status", StatusEncontro.REALIZADO.name());
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                    TipoOperacaoConfirmacao.CRIAR_ENCONTRO, null, "Encontro rápido",
                    "Criar encontro e abrir a chamada?", "Confira a data antes de preparar a lista de presença.",
                    atividade + " · " + data,
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Atividade", null, atividade),
                            confirmacaoService.alteracao("Data", null, data.toString()),
                            confirmacaoService.alteracao("Status inicial", null, "Realizado")),
                    List.of("O encontro aparecerá no dashboard e na atividade.",
                            "A chamada será aberta após a confirmação.",
                            "A criação ficará registrada no Histórico."),
                    dados, "/encontros/novo/corrigir", "/presencas/registrar?atividadeId=" + atividadeId,
                    "/presencas/registrar?atividadeId=" + atividadeId + "&encontroId={id}",
                    "Encontro criado. A lista de presença está pronta.", "Criar e abrir chamada", false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/presencas/registrar?atividadeId=" + atividadeId;
        }
    }

    @PostMapping("/conferir")
    public String conferir(@RequestParam Long encontroId,
            @RequestParam Map<String, String> parametros,
            Model model,
            RedirectAttributes redirectAttributes) {
        try {
            Map<Long, StatusPresenca> status = presencaService.extrairStatus(parametros);
            Map<Long, String> observacoes = presencaService.extrairObservacoes(parametros);
            Encontro encontro = encontroService.obter(encontroId);
            model.addAttribute("encontro", encontro);
            model.addAttribute("itens", presencaService.prepararRascunho(status, observacoes));
            model.addAttribute("resumoConferencia", presencaService.resumirSelecao(status));
            return "presencas/conferir";
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/presencas/registrar?encontroId=" + encontroId;
        }
    }

    @PostMapping("/revisar")
    public String revisar(@RequestParam Long encontroId,
            @RequestParam(required = false) String tokenConfirmacao,
            @RequestParam Map<String, String> parametros,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            if (tokenConfirmacao != null) {
                confirmacaoService.descartar(session, tokenConfirmacao);
            }
            Map<Long, StatusPresenca> status = presencaService.extrairStatus(parametros);
            Map<Long, String> observacoes = presencaService.extrairObservacoes(parametros);
            List<RegistroPresencaItem> rascunho = presencaService.prepararRascunho(status, observacoes);
            Encontro encontro = encontroService.obter(encontroId);
            model.addAttribute("aviso", "Revise as marcações e clique em Conferir presença quando estiver pronto.");
            preencherPagina(encontro.getAtividade().getId(), encontroId, null, null, rascunho, model);
            return "presencas/registrar";
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/presencas/registrar?encontroId=" + encontroId;
        }
    }

    @PostMapping("/registrar")
    public String salvar(@RequestParam Long encontroId,
            @RequestParam Map<String, String> parametros,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        try {
            Map<Long, StatusPresenca> status = presencaService.extrairStatus(parametros);
            Map<Long, String> observacoes = presencaService.extrairObservacoes(parametros);
            Encontro encontro = encontroService.obter(encontroId);
            RegistroPresencaResumo resumo = presencaService.resumirSelecao(status);
            Map<String, String> dados = dadosPresenca(encontroId, parametros);
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                    TipoOperacaoConfirmacao.REGISTRAR_PRESENCA, encontroId, "Etapa 5 de 5",
                    "Confirmar presença?", "Esta é a última etapa. Os dados ainda não foram gravados.",
                    encontro.getAtividade().getNome() + " · " + encontro.getData(),
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Participantes", null, Long.toString(resumo.total())),
                            confirmacaoService.alteracao("Presentes", null, Long.toString(resumo.presentes())),
                            confirmacaoService.alteracao("Ausentes", null, Long.toString(resumo.ausentes())),
                            confirmacaoService.alteracao("Justificadas", null, Long.toString(resumo.justificadas()))),
                    List.of("A chamada será vinculada ao encontro e aos participantes.",
                            "Frequência, dashboard e relatórios serão recalculados pela mesma fonte de dados.",
                            "A operação gerará um único evento no Histórico."),
                    dados, "/presencas/revisar",
                    "/presencas/registrar?atividadeId=" + encontro.getAtividade().getId() + "&encontroId=" + encontroId,
                    "/presencas/registrar?atividadeId=" + encontro.getAtividade().getId() + "&encontroId={id}",
                    "Presença confirmada. Encontro, frequência, dashboard, relatórios e Histórico foram atualizados.",
                    "Confirmar presença", false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:/presencas/registrar?encontroId=" + encontroId;
        }
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, HttpSession session) {
        ConfirmacaoOperacao confirmacao = prepararExclusao(presencaService.obter(id), session);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, HttpSession session) {
        ConfirmacaoOperacao confirmacao = prepararExclusao(presencaService.obter(id), session);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    private void preencherPagina(Long atividadeId, Long encontroId, String busca, String marcar,
            List<RegistroPresencaItem> rascunho, Model model) {
        if (encontroId != null && atividadeId == null) {
            atividadeId = encontroService.obter(encontroId).getAtividade().getId();
        }
        boolean marcarTodos = "presentes".equalsIgnoreCase(marcar);

        model.addAttribute("atividades", atividadeService.listarAtivas());
        model.addAttribute("participantesAtivos", participanteService.contarAtivos());
        model.addAttribute("atividadeId", atividadeId);
        model.addAttribute("encontroId", encontroId);
        model.addAttribute("busca", busca);
        model.addAttribute("hoje", LocalDate.now());
        model.addAttribute("statusPresenca", StatusPresenca.values());

        if (atividadeId != null) {
            model.addAttribute("encontros", encontroService.listarPorAtividade(atividadeId));
        }
        if (encontroId != null) {
            model.addAttribute("encontro", encontroService.obter(encontroId));
            model.addAttribute("itens", rascunho == null
                    ? presencaService.prepararItens(encontroId, busca, marcarTodos)
                    : rascunho);
            model.addAttribute("resumo", presencaService.resumirEncontro(encontroId));
        }
    }

    private ConfirmacaoOperacao prepararExclusao(Presenca presenca, HttpSession session) {
        Long encontroId = presenca.getEncontro().getId();
        return confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.EXCLUIR_PRESENCA, presenca.getId(), "Correção administrativa",
                "Excluir este registro de presença?",
                "Use esta opção somente quando o registro não deve existir. Para trocar a situação, volte à chamada.",
                presenca.getParticipante().getNome() + " · " + presenca.getEncontro().getData(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Situação", presenca.getStatus().getDescricao(), "Registro removido")),
                List.of("O registro deixará os cálculos de frequência e relatórios.",
                        "O encontro e o participante continuarão cadastrados.",
                        "A situação anterior ficará registrada no Histórico."),
                Map.of(), null, "/encontros/" + encontroId, "/encontros/{id}",
                "Registro de presença excluído. Frequência, relatórios e Histórico foram atualizados.",
                "Excluir registro", true);
    }

    private Map<String, String> dadosPresenca(Long encontroId, Map<String, String> parametros) {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("encontroId", encontroId.toString());
        parametros.forEach((chave, valor) -> {
            if ((chave.startsWith("status_") || chave.startsWith("observacoes_")) && valor != null) {
                dados.put(chave, valor);
            }
        });
        return dados;
    }
}
