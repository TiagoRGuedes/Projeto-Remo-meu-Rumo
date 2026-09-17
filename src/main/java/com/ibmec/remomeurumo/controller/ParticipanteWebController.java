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
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.FrequenciaService;
import com.ibmec.remomeurumo.service.HistoricoOperacaoService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

import jakarta.validation.Valid;
import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/participantes")
public class ParticipanteWebController {

    private final ParticipanteService participanteService;
    private final PresencaService presencaService;
    private final HistoricoOperacaoService historicoService;
    private final ConfirmacaoOperacaoService confirmacaoService;
    private final FrequenciaService frequenciaService;

    public ParticipanteWebController(ParticipanteService participanteService,
            PresencaService presencaService,
            HistoricoOperacaoService historicoService,
            ConfirmacaoOperacaoService confirmacaoService,
            FrequenciaService frequenciaService) {
        this.participanteService = participanteService;
        this.presencaService = presencaService;
        this.historicoService = historicoService;
        this.confirmacaoService = confirmacaoService;
        this.frequenciaService = frequenciaService;
    }

    @GetMapping
    public String listar(@RequestParam(required = false) String busca,
            @RequestParam(required = false) String situacao, Model model) {
        var participantes = participanteService.listar(busca, situacao);
        model.addAttribute("participantes", participantes);
        model.addAttribute("busca", busca);
        model.addAttribute("situacao", situacao);
        model.addAttribute("totalResultados", participantes.size());
        model.addAttribute("totalParticipantes", participanteService.contarAtivos() + participanteService.contarInativos());
        model.addAttribute("participantesAtivos", participanteService.contarAtivos());
        model.addAttribute("participantesInativos", participanteService.contarInativos());
        model.addAttribute("participantesRecentes", participanteService.contarRecentes());
        return "participantes/lista";
    }

    @GetMapping("/novo")
    public String novo(Model model) {
        model.addAttribute("participante", new Participante());
        model.addAttribute("modoEdicao", false);
        return "participantes/form";
    }

    @PostMapping("/novo")
    public String cadastrar(@Valid @ModelAttribute("participante") Participante participante,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicao", false);
            return "participantes/form";
        }
        try {
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(
                    session,
                    TipoOperacaoConfirmacao.CRIAR_PARTICIPANTE,
                    null,
                    "Novo participante",
                    "Criar participante?",
                    "Confira o cadastro antes de adicioná-lo às rotinas do Instituto.",
                    participante.getNome(),
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Nome", null, participante.getNome()),
                            confirmacaoService.alteracao("Situação inicial", null, "Ativo"),
                            confirmacaoService.alteracao("Observações", null, participante.getObservacoes())),
                    List.of(
                            "A pessoa será adicionada à lista de participantes.",
                            "Ela ficará disponível para novas chamadas.",
                            "A criação será registrada no Histórico do sistema."),
                    dadosParticipante(participante),
                    "/participantes/novo/corrigir",
                    "/participantes/novo",
                    "/participantes/{id}",
                    "Participante cadastrado. Listas, indicadores e Histórico foram atualizados.",
                    "Confirmar cadastro",
                    false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            bindingResult.reject("erroNegocio", ex.getMessage());
            model.addAttribute("modoEdicao", false);
            return "participantes/form";
        }
    }

    @GetMapping("/{id}")
    public String visualizar(@PathVariable Long id, Model model) {
        Participante participante = participanteService.obter(id);
        var historico = presencaService.listarHistorico(id, null, null, null, null);
        model.addAttribute("participante", participante);
        model.addAttribute("historico", historico);
        model.addAttribute("frequencia", frequenciaService.resumir(participante.getNome(), historico));
        model.addAttribute("historicoAdministrativo",
                historicoService.porEntidade(ModuloHistorico.PARTICIPANTES, id));
        model.addAttribute("podeExcluir", participanteService.podeExcluir(id));
        model.addAttribute("totalPresencas", participanteService.contarPresencas(id));
        return "participantes/detalhe";
    }

    @GetMapping("/{id}/editar")
    public String editar(@PathVariable Long id, Model model) {
        model.addAttribute("participante", participanteService.obter(id));
        model.addAttribute("modoEdicao", true);
        return "participantes/form";
    }

    @PostMapping("/{id}/editar")
    public String atualizar(@PathVariable Long id,
            @Valid @ModelAttribute("participante") Participante participante,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes,
            HttpSession session) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("modoEdicao", true);
            return "participantes/form";
        }
        try {
            Participante atual = participanteService.obter(id);
            ConfirmacaoOperacao confirmacao = confirmacaoService.criar(
                    session,
                    TipoOperacaoConfirmacao.EDITAR_PARTICIPANTE,
                    id,
                    "Revisar alteração",
                    "Atualizar este participante?",
                    "Compare os dados atuais com o que será salvo.",
                    participante.getNome(),
                    confirmacaoService.alteracoes(
                            confirmacaoService.alteracao("Nome", atual.getNome(), participante.getNome()),
                            confirmacaoService.alteracao("Observações", atual.getObservacoes(), participante.getObservacoes()),
                            confirmacaoService.alteracao("Situação", atual.isAtivo() ? "Ativo" : "Inativo",
                                    atual.isAtivo() ? "Ativo" : "Inativo")),
                    List.of(
                            "O novo nome aparecerá nas listas e páginas relacionadas.",
                            "Presenças e frequências anteriores continuarão vinculadas ao cadastro.",
                            "A edição será registrada no Histórico do sistema."),
                    dadosParticipante(participante),
                    "/participantes/" + id + "/editar/corrigir",
                    "/participantes/" + id + "/editar",
                    "/participantes/{id}",
                    "Participante atualizado. As páginas relacionadas e o Histórico já refletem a alteração.",
                    "Confirmar alteração",
                    false);
            return "redirect:/confirmacoes/" + confirmacao.token();
        } catch (RegraNegocioException ex) {
            bindingResult.reject("erroNegocio", ex.getMessage());
            model.addAttribute("modoEdicao", true);
            return "participantes/form";
        }
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, HttpSession session) {
        Participante participante = participanteService.obter(id);
        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.ATIVAR_PARTICIPANTE, id, "Alterar situação",
                "Ativar " + participante.getNome() + "?",
                "A pessoa voltará a participar das novas operações.", participante.getNome(),
                confirmacaoService.alteracoes(confirmacaoService.alteracao("Situação", "Inativo", "Ativo")),
                List.of("Voltará a aparecer em novas chamadas.", "O histórico anterior será mantido.",
                        "A ativação ficará registrada no Histórico."),
                Map.of(), null, "/participantes/" + id, "/participantes/{id}",
                "Participante ativado e disponível para novas chamadas.", "Confirmar ativação", false);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, HttpSession session) {
        Participante participante = participanteService.obter(id);
        ConfirmacaoOperacao confirmacao = confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.DESATIVAR_PARTICIPANTE, id, "Revisar impacto",
                "Desativar " + participante.getNome() + "?",
                "A desativação interrompe novas operações sem apagar a trajetória existente.", participante.getNome(),
                confirmacaoService.alteracoes(confirmacaoService.alteracao("Situação", "Ativo", "Inativo")),
                List.of("Não aparecerá em novas chamadas.", "Presenças e frequência históricas serão preservadas.",
                        "A desativação ficará registrada no Histórico."),
                Map.of(), null, "/participantes/" + id, "/participantes/{id}",
                "Participante desativado. Todo o histórico anterior foi preservado.", "Confirmar desativação", true);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @GetMapping("/{id}/excluir")
    public String confirmarExclusao(@PathVariable Long id, Model model, HttpSession session) {
        Participante participante = participanteService.obter(id);
        boolean podeExcluir = participanteService.podeExcluir(id);
        if (podeExcluir) {
            ConfirmacaoOperacao confirmacao = prepararExclusao(participante, session);
            return "redirect:/confirmacoes/" + confirmacao.token();
        }
        model.addAttribute("participante", participante);
        model.addAttribute("podeExcluir", false);
        model.addAttribute("totalPresencas", participanteService.contarPresencas(id));
        return "participantes/confirmar-exclusao";
    }

    @PostMapping("/{id}/excluir")
    public String excluir(@PathVariable Long id, HttpSession session) {
        ConfirmacaoOperacao confirmacao = prepararExclusao(participanteService.obter(id), session);
        return "redirect:/confirmacoes/" + confirmacao.token();
    }

    @PostMapping("/novo/corrigir")
    public String corrigirNovo(@ModelAttribute("participante") Participante participante,
            @RequestParam String tokenConfirmacao, HttpSession session, Model model) {
        confirmacaoService.descartar(session, tokenConfirmacao);
        model.addAttribute("modoEdicao", false);
        return "participantes/form";
    }

    @PostMapping("/{id}/editar/corrigir")
    public String corrigirEdicao(@PathVariable Long id,
            @ModelAttribute("participante") Participante participante,
            @RequestParam String tokenConfirmacao, HttpSession session, Model model) {
        confirmacaoService.descartar(session, tokenConfirmacao);
        participante.setId(id);
        model.addAttribute("modoEdicao", true);
        return "participantes/form";
    }

    private ConfirmacaoOperacao prepararExclusao(Participante participante, HttpSession session) {
        if (!participanteService.podeExcluir(participante.getId())) {
            throw new RegraNegocioException(
                    "Este participante possui histórico de presença e não pode ser excluído. Desative-o para preservar os registros.");
        }
        return confirmacaoService.criar(session,
                TipoOperacaoConfirmacao.EXCLUIR_PARTICIPANTE, participante.getId(), "Exclusão definitiva",
                "Excluir " + participante.getNome() + "?",
                "Esta operação é permitida porque o cadastro nunca apareceu em uma presença.", participante.getNome(),
                confirmacaoService.alteracoes(
                        confirmacaoService.alteracao("Cadastro", participante.getNome(), "Será removido")),
                List.of("O cadastro será removido definitivamente.",
                        "A exclusão ficará registrada no Histórico administrativo."),
                Map.of(), null, "/participantes/" + participante.getId(), "/participantes",
                "Participante excluído definitivamente. A operação ficou registrada no Histórico.",
                "Excluir participante", true);
    }

    private Map<String, String> dadosParticipante(Participante participante) {
        Map<String, String> dados = new LinkedHashMap<>();
        dados.put("nome", Objects.toString(participante.getNome(), ""));
        dados.put("observacoes", Objects.toString(participante.getObservacoes(), ""));
        return dados;
    }
}
