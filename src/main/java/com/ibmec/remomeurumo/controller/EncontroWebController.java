package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.dto.EncontroInput;
import com.ibmec.remomeurumo.dto.ConfirmacaoOperacao;
import com.ibmec.remomeurumo.dto.TipoOperacaoConfirmacao;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;
import com.ibmec.remomeurumo.service.PresencaService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/encontros")
public class EncontroWebController {

    private final EncontroService encontroService;
    private final AtividadeService atividadeService;
    private final PresencaService presencaService;
    private final HistoricoOperacaoService historicoService;
    private final ConfirmacaoOperacaoService confirmacaoService;

    public EncontroWebController(EncontroService encontroService,
            AtividadeService atividadeService,
            PresencaService presencaService,
            HistoricoOperacaoService historicoService,
            ConfirmacaoOperacaoService confirmacaoService) {
        this.encontroService = encontroService;
        this.atividadeService = atividadeService;
        this.presencaService = presencaService;
        this.historicoService = historicoService;
        this.confirmacaoService = confirmacaoService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) Long atividadeId,
            @RequestParam(required = false) StatusEncontro status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate inicio,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fim,
            @RequestParam(required = false) String busca,
            Model model) {
        var encontros = encontroService.listar(atividadeId, status, inicio, fim, busca);
        model.addAttribute("encontros", encontros);
        model.addAttribute("atividades", atividadeService.listarAtivas());
        model.addAttribute("atividadeId", atividadeId);
        model.addAttribute("status", status);
        model.addAttribute("statusEncontro", StatusEncontro.values());
        model.addAttribute("inicio", inicio);
        model.addAttribute("fim", fim);
        model.addAttribute("busca", busca);
        model.addAttribute("totalResultados", encontros.size());
        model.addAttribute("encontrosNoMes", encontroService.contarNoMes());
        model.addAttribute("encontrosRealizados", encontroService.contarPorStatus(StatusEncontro.REALIZADO));
        model.addAttribute("encontrosPlanejados", encontroService.contarPorStatus(StatusEncontro.PLANEJADO));
        model.addAttribute("encontrosSemPresenca", encontroService.contarSemPresenca());
        LocalDate hoje = LocalDate.now();
        model.addAttribute("inicioMes", hoje.withDayOfMonth(1));
        model.addAttribute("fimMes", hoje.withDayOfMonth(hoje.lengthOfMonth()));
        return "encontros/lista";
    }

    @GetMapping("/novo")
    public String novo(@RequestParam(required = false) Long atividadeId, Model model) {
        model.addAttribute("encontroForm",
                new EncontroInput(atividadeId, LocalDate.now(), "", StatusEncontro.REALIZADO));
        model.addAttribute("atividades", atividadeService.listarAtivas());
        model.addAttribute("statusEncontro", StatusEncontro.values());
        model.addAttribute("modoEdicao", false);
        return "encontros/form";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("encontroForm") EncontroInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            preencherFormulario(model, false);
            return "encontros/form";
        }
        try {
            ConfirmacaoOperacao confirmacao = prepararCriacao(form, "/encontros/novo/corrigir",
                    "/encontros/novo", "/encontros/{id}", session);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            bindingResult.reject("erroNegocio", ex.getMessage());
            preencherFormulario(model, false);
            return "encontros/form";
        }
    }

    @GetMapping("/{id}")
    public String visualizar(@PathVariable Long id, Model model) {
        model.addAttribute("encontro", encontroService.obter(id));
        model.addAttribute("presencas", presencaService.listarPorEncontro(id));
        model.addAttribute("resumo", presencaService.resumirEncontro(id));
        model.addAttribute("historicoAdministrativo", historicoService.porEncontro(id));
        model.addAttribute("podeExcluir", encontroService.podeExcluir(id));
        return "encontros/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        Encontro encontro = encontroService.obter(id);
        model.addAttribute("encontro", encontro);
        model.addAttribute("encontroForm", new EncontroInput(
                encontro.getAtividade().getId(),
                encontro.getData(),
                encontro.getObservacoes(),
                encontro.getStatus()));
        preencherFormulario(model, true);
        return "encontros/form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id,
            @Valid @ModelAttribute("encontroForm") EncontroInput form,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            preencherFormulario(model, true);
            return "encontros/form";
        }
        try {
            Encontro atual = encontroService.obter(id);
            String novaAtividade = atividadeService.obter(form.atividadeId()).getNome();
            StatusEncontro novoStatus = form.status() == null ? StatusEncontro.REALIZADO : form.status();
            EncontroInput formNormalizado = new EncontroInput(
                    form.atividadeId(), form.data(), form.observacoes(), novoStatus);
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                    TipoOperacaoConfirmacao.EDITAR_ENCONTRO, id, "Revisar encontro",
                    "Atualizar este encontro?", "Confira atividade, data e status antes da gravação.",
                    novaAtividade + " · " + form.data(),
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Atividade", atual.getAtividade().getNome(), novaAtividade),
                            confirmacaoService.alteracao("Data", atual.getData().toString(), form.data().toString()),
                            confirmacaoService.alteracao("Status", atual.getStatus().getDescricao(),
                                    novoStatus.getDescricao()),
                            confirmacaoService.alteracao("Observações", atual.getObservacoes(), form.observacoes())),
                    List.of("O encontro será atualizado nas páginas da atividade e do dashboard.",
                            "Registros de presença existentes permanecerão vinculados.",
                            "A operação gerará um único evento no Histórico."),
                    dadosEncontro(formNormalizado), "/encontros/" + id + "/editar/corrigir",
                    "/encontros/" + id + "/editar", "/encontros/{id}",
                    "Encontro atualizado em todas as páginas relacionadas.", "Confirmar alteração", false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            bindingResult.reject("erroNegocio", ex.getMessage());
            preencherFormulario(model, true);
            return "encontros/form";
        }
    }

    @PostMapping("/rapido")
    public String criarRapido(@RequestParam Long atividadeId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate data,
            @RequestParam(required = false) String observacoes,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        EncontroInput form = new EncontroInput(atividadeId, data, observacoes, StatusEncontro.REALIZADO);
        ConfirmacaoOperacao confirmacao = prepararCriacao(form, "/encontros/novo/corrigir",
                "/encontros", "/presencas/registrar?atividadeId=" + atividadeId + "&encontroId={id}", session);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model, HttpSession session) {
        Encontro encontro = encontroService.obter(id);
        boolean podeExcluir = encontroService.podeExcluir(id);
        if (podeExcluir) {
            ConfirmacaoOperacao confirmacao = prepararExclusao(encontro, session);
            return "redirect:/confirmacoes/" + confirmacao.token();
        }
        model.addAttribute("encontro", encontro);
        model.addAttribute("podeExcluir", false);
        model.addAttribute("totalPresencas", encontroService.contarPresencas(id));
        return "encontros/confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, HttpSession session) {
        ConfirmacaoOperacao confirmacao = prepararExclusao(encontroService.obter(id), session);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/cancelar")
    public String cancelar(@PathVariable Long id, HttpSession session) {
        Encontro encontro = encontroService.obter(id);
        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.CANCELAR_ENCONTRO, id, "Revisar impacto",
                "Cancelar este encontro?", "O cancelamento interrompe novas presenças sem apagar registros.",
                encontro.getAtividade().getNome() + " · " + encontro.getData(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Status", encontro.getStatus().getDescricao(), "Cancelado")),
                List.of("O encontro não aceitará novos registros de presença.",
                        "Presenças e relatórios existentes serão preservados.",
                        "O cancelamento ficará registrado no Histórico."),
                Map.of(), null, "/encontros/" + id, "/encontros/{id}",
                "Encontro cancelado. Presenças e relatórios anteriores foram preservados.",
                "Confirmar cancelamento", true);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/novo/corrigir")
    public String corrigirNovo(@ModelAttribute("encontroForm") EncontroInput form,
            @RequestParam String tokenConfirmacao, HttpSession session, Model model) {
        confirmacaoService.descartar(session, tokenConfirmacao);
        preencherFormulario(model, false);
        return "encontros/form";
    }

    @PostMapping("/{id}/editar/corrigir")
    public String corrigirEdicao(@PathVariable Long id,
            @ModelAttribute("encontroForm") EncontroInput form,
            @RequestParam String tokenConfirmacao, HttpSession session, Model model) {
        confirmacaoService.descartar(session, tokenConfirmacao);
        model.addAttribute("encontro", encontroService.obter(id));
        preencherFormulario(model, true);
        return "encontros/form";
    }

    private void preencherFormulario(Model model, boolean modoEdicao) {
        model.addAttribute("atividades", atividadeService.listarAtivas());
        model.addAttribute("statusEncontro", StatusEncontro.values());
        model.addAttribute("modoEdicao", modoEdicao);
    }

    private ConfirmacaoOperacao prepararCriacao(EncontroInput form, String urlCorrecao,
            String urlCancelar, String urlSucesso, HttpSession session) {
        String atividade = atividadeService.obter(form.atividadeId()).getNome();
        StatusEncontro status = form.status() == null ? StatusEncontro.REALIZADO : form.status();
        return confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.CRIAR_ENCONTRO, null, "Novo encontro", "Criar encontro?",
                "Confira a operação antes de disponibilizar a nova data.", atividade + " · " + form.data(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Atividade", null, atividade),
                        confirmacaoService.alteracao("Data", null, form.data().toString()),
                        confirmacaoService.alteracao("Status inicial", null, status.getDescricao()),
                        confirmacaoService.alteracao("Observações", null, form.observacoes())),
                List.of("O encontro aparecerá no dashboard e na atividade relacionada.",
                        "Ficará disponível no fluxo de presença quando permitido pelo status.",
                        "A criação será registrada no Histórico."),
                dadosEncontro(new EncontroInput(form.atividadeId(), form.data(), form.observacoes(), status)),
                urlCorrecao, urlCancelar, urlSucesso,
                "Encontro criado e conectado ao dashboard, à atividade e ao Histórico.",
                "Confirmar criação", false);
    }

    private ConfirmacaoOperacao prepararExclusao(Encontro encontro, HttpSession session) {
        if (!encontroService.podeExcluir(encontro.getId())) {
            throw new RegraNegocioException(
                    "Este encontro possui registros de presença e deve ser cancelado para preservar o histórico.");
        }
        return confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.EXCLUIR_ENCONTRO, encontro.getId(), "Exclusão definitiva",
                "Excluir este encontro?", "A exclusão é permitida porque não existem presenças vinculadas.",
                encontro.getAtividade().getNome() + " · " + encontro.getData(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Encontro", encontro.getData().toString(), "Será removido")),
                List.of("O encontro será removido definitivamente.",
                        "A exclusão ficará registrada no Histórico administrativo."),
                Map.of(), null, "/encontros/" + encontro.getId(), "/encontros",
                "Encontro excluído definitivamente. A operação ficou registrada no Histórico.",
                "Excluir encontro", true);
    }

    private Map<String, String> dadosEncontro(EncontroInput form) {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("atividadeId", Objects.toString(form.atividadeId(), ""));
        dados.put("data", Objects.toString(form.data(), ""));
        dados.put("status", Objects.toString(form.status(), StatusEncontro.REALIZADO.name()));
        dados.put("observacoes", Objects.toString(form.observacoes(), ""));
        return dados;
    }
}
