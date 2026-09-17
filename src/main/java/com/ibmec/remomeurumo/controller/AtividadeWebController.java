package com.ibmec.remomeurumo.controller;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

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

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.dto.ConfirmacaoOperacao;
import com.ibmec.remomeurumo.dto.TipoOperacaoConfirmacao;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;
import com.ibmec.remomeurumo.service.PresencaService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/atividades")
public class AtividadeWebController {

    private final AtividadeService atividadeService;
    private final EncontroService encontroService;
    private final HistoricoOperacaoService historicoService;
    private final ConfirmacaoOperacaoService confirmacaoService;
    private final PresencaService presencaService;

    public AtividadeWebController(AtividadeService atividadeService,
            EncontroService encontroService,
            HistoricoOperacaoService historicoService,
            ConfirmacaoOperacaoService confirmacaoService,
            PresencaService presencaService) {
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
        this.historicoService = historicoService;
        this.confirmacaoService = confirmacaoService;
        this.presencaService = presencaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) String situacao, Model model) {
        var atividades = atividadeService.listar(busca, situacao);
        model.addAttribute("atividades", atividades);
        model.addAttribute("busca", busca);
        model.addAttribute("situacao", situacao);
        model.addAttribute("totalResultados", atividades.size());
        model.addAttribute("totalAtividades", atividadeService.contarAtivas() + atividadeService.contarInativas());
        model.addAttribute("atividadesAtivas", atividadeService.contarAtivas());
        model.addAttribute("atividadesInativas", atividadeService.contarInativas());
        model.addAttribute("totalEncontros", encontroService.contarTodos());
        return "atividades/lista";
    }

    @GetMapping("/nova")
    public String nova(Model model) {
        model.addAttribute("atividade", new Atividade());
        model.addAttribute("modoEdicao", false);
        return "atividades/form";
    }

    @PostMapping("/nova")
    public String cadastrar(@Valid @ModelAttribute("atividade") Atividade atividade,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicao", false);
            return "atividades/form";
        }
        try {
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                    TipoOperacaoConfirmacao.CRIAR_ATIVIDADE, null, "Nova atividade",
                    "Criar atividade?", "Confira os dados antes de disponibilizar a atividade para novos encontros.",
                    atividade.getNome(),
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Nome", null, atividade.getNome()),
                            confirmacaoService.alteracao("Descrição", null, atividade.getDescricao()),
                            confirmacaoService.alteracao("Situação inicial", null, "Ativa")),
                    List.of("A atividade aparecerá na gestão e na busca global.",
                            "Ficará disponível para a criação de encontros.",
                            "A criação será registrada no Histórico do sistema."),
                    dadosAtividade(atividade), "/atividades/nova/corrigir", "/atividades/nova",
                    "/atividades/{id}",
                    "Atividade cadastrada. Gestão, busca e Histórico foram atualizados.",
                    "Confirmar cadastro", false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            bindingResult.reject("erroNegocio", ex.getMessage());
            model.addAttribute("modoEdicao", false);
            return "atividades/form";
        }
    }

    @GetMapping("/{id}")
    public String visualizar(@PathVariable Long id, Model model) {
        model.addAttribute("atividade", atividadeService.obter(id));
        model.addAttribute("encontros", encontroService.listarPorAtividade(id));
        model.addAttribute("historicoAdministrativo", historicoService.porEntidade(ModuloHistorico.ATIVIDADES, id));
        model.addAttribute("podeExcluir", atividadeService.podeExcluir(id));
        model.addAttribute("totalEncontros", atividadeService.contarEncontros(id));
        model.addAttribute("participantesRelacionados", presencaService.contarParticipantesPorAtividade(id));
        return "atividades/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("atividade", atividadeService.obter(id));
        model.addAttribute("modoEdicao", true);
        return "atividades/form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id,
            @Valid @ModelAttribute("atividade") Atividade atividade,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicao", true);
            return "atividades/form";
        }
        try {
            Atividade atual = atividadeService.obter(id);
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                    TipoOperacaoConfirmacao.EDITAR_ATIVIDADE, id, "Revisar alteração",
                    "Atualizar esta atividade?", "Compare os dados atuais com a nova versão.",
                    atividade.getNome(),
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Nome", atual.getNome(), atividade.getNome()),
                            confirmacaoService.alteracao("Descrição", atual.getDescricao(), atividade.getDescricao()),
                            confirmacaoService.alteracao("Situação", atual.isAtivo() ? "Ativa" : "Inativa",
                                    atual.isAtivo() ? "Ativa" : "Inativa")),
                    List.of("O novo nome aparecerá nos encontros e relatórios relacionados.",
                            "Os registros históricos continuarão vinculados à atividade.",
                            "A edição será registrada no Histórico do sistema."),
                    dadosAtividade(atividade), "/atividades/" + id + "/editar/corrigir",
                    "/atividades/" + id + "/editar", "/atividades/{id}",
                    "Atividade atualizada em todas as páginas relacionadas.", "Confirmar alteração", false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            bindingResult.reject("erroNegocio", ex.getMessage());
            model.addAttribute("modoEdicao", true);
            return "atividades/form";
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, HttpSession session) {
        Atividade atividade = atividadeService.obter(id);
        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.ATIVAR_ATIVIDADE, id, "Alterar situação",
                "Ativar " + atividade.getNome() + "?", "A atividade voltará às novas operações.",
                atividade.getNome(),
                confirmacaoService.alteracoes(confirmacaoService.alteracao("Situação", "Inativa", "Ativa")),
                List.of("Voltará a ser oferecida para novos encontros.", "Os encontros anteriores permanecerão disponíveis.",
                        "A ativação ficará registrada no Histórico."),
                Map.of(), null, "/atividades/" + id, "/atividades/{id}",
                "Atividade ativada e disponível para novos encontros.", "Confirmar ativação", false);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, HttpSession session) {
        Atividade atividade = atividadeService.obter(id);
        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.DESATIVAR_ATIVIDADE, id, "Revisar impacto",
                "Desativar " + atividade.getNome() + "?",
                "A atividade deixará as novas operações, sem perder sua trajetória.", atividade.getNome(),
                confirmacaoService.alteracoes(confirmacaoService.alteracao("Situação", "Ativa", "Inativa")),
                List.of("Não será oferecida para novos encontros.", "Encontros e relatórios anteriores serão preservados.",
                        "A desativação ficará registrada no Histórico."),
                Map.of(), null, "/atividades/" + id, "/atividades/{id}",
                "Atividade desativada. Encontros e relatórios anteriores foram preservados.",
                "Confirmar desativação", true);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model, HttpSession session) {
        Atividade atividade = atividadeService.obter(id);
        boolean podeExcluir = atividadeService.podeExcluir(id);
        if (podeExcluir) {
            ConfirmacaoOperacao confirmacao = prepararExclusao(atividade, session);
            return "redirect:/confirmacoes/" + confirmacao.token();
        }
        model.addAttribute("atividade", atividade);
        model.addAttribute("podeExcluir", false);
        model.addAttribute("totalEncontros", atividadeService.contarEncontros(id));
        return "atividades/confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, HttpSession session) {
        ConfirmacaoOperacao confirmacao = prepararExclusao(atividadeService.obter(id), session);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/nova/corrigir")
    public String corrigirNova(@ModelAttribute("atividade") Atividade atividade,
            @RequestParam String tokenConfirmacao, HttpSession session, Model model) {
        confirmacaoService.descartar(session, tokenConfirmacao);
        model.addAttribute("modoEdicao", false);
        return "atividades/form";
    }

    @PostMapping("/{id}/editar/corrigir")
    public String corrigirEdicao(@PathVariable Long id, @ModelAttribute("atividade") Atividade atividade,
            @RequestParam String tokenConfirmacao, HttpSession session, Model model) {
        confirmacaoService.descartar(session, tokenConfirmacao);
        atividade.setId(id);
        model.addAttribute("modoEdicao", true);
        return "atividades/form";
    }

    private ConfirmacaoOperacao prepararExclusao(Atividade atividade, HttpSession session) {
        if (!atividadeService.podeExcluir(atividade.getId())) {
            throw new RegraNegocioException(
                    "Esta atividade possui encontros e não pode ser excluída. Desative-a para preservar o histórico.");
        }
        return confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.EXCLUIR_ATIVIDADE, atividade.getId(), "Exclusão definitiva",
                "Excluir " + atividade.getNome() + "?",
                "A exclusão é permitida porque não existem encontros vinculados.", atividade.getNome(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Atividade", atividade.getNome(), "Será removida")),
                List.of("A atividade será removida definitivamente.",
                        "A exclusão ficará registrada no Histórico administrativo."),
                Map.of(), null, "/atividades/" + atividade.getId(), "/atividades",
                "Atividade excluída definitivamente. A operação ficou registrada no Histórico.",
                "Excluir atividade", true);
    }

    private Map<String, String> dadosAtividade(Atividade atividade) {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("nome", Objects.toString(atividade.getNome(), ""));
        dados.put("descricao", Objects.toString(atividade.getDescricao(), ""));
        return dados;
    }
}
