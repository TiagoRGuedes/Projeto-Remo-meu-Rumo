package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.util.Map;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.dto.ConfirmacaoOperacao;
import com.ibmec.remomeurumo.dto.ResultadoRegistroPresenca;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.ParticipanteService;
import com.ibmec.remomeurumo.service.PresencaService;

import jakarta.servlet.http.HttpSession;

@Controller
@RequestMapping("/confirmacoes")
public class ConfirmacaoWebController {

    private final ConfirmacaoOperacaoService confirmacaoService;
    private final ParticipanteService participanteService;
    private final AtividadeService atividadeService;
    private final EncontroService encontroService;
    private final PresencaService presencaService;

    public ConfirmacaoWebController(ConfirmacaoOperacaoService confirmacaoService,
            ParticipanteService participanteService,
            AtividadeService atividadeService,
            EncontroService encontroService,
            PresencaService presencaService) {
        this.confirmacaoService = confirmacaoService;
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
        this.presencaService = presencaService;
    }

    @GetMapping("/{token}")
    public String revisar(@PathVariable String token, HttpSession session, Model model,
            RedirectAttributes redirectAttributes) {
        return confirmacaoService.obter(session, token)
                .map(confirmacao -> {
                    model.addAttribute("confirmacao", confirmacao);
                    return "confirmacoes/revisar";
                })
                .orElseGet(() -> {
                    redirectAttributes.addFlashAttribute("aviso",
                            "Esta revisão expirou ou já foi confirmada. Reabra a operação para continuar.");
                    return "redirect:/";
                });
    }

    @PostMapping("/{token}")
    public String confirmar(@PathVariable String token, HttpSession session,
            RedirectAttributes redirectAttributes) {
        ConfirmacaoOperacao confirmacao = confirmacaoService.consumir(session, token).orElse(null);
        if (confirmacao == null) {
            redirectAttributes.addFlashAttribute("aviso",
                    "A operação não foi repetida porque esta confirmação já foi utilizada ou expirou.");
            return "redirect:/";
        }

        try {
            Long idResultado = executar(confirmacao, redirectAttributes);
            redirectAttributes.addFlashAttribute("sucesso", confirmacao.mensagemSucesso());
            return "redirect:" + destino(confirmacao.urlSucesso(), idResultado);
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
            return "redirect:" + confirmacao.urlCancelar();
        }
    }

    @PostMapping("/{token}/cancelar")
    public String cancelar(@PathVariable String token, HttpSession session,
            RedirectAttributes redirectAttributes) {
        ConfirmacaoOperacao confirmacao = confirmacaoService.consumir(session, token).orElse(null);
        if (confirmacao == null) {
            redirectAttributes.addFlashAttribute("aviso", "Esta revisão já foi encerrada.");
            return "redirect:/";
        }
        redirectAttributes.addFlashAttribute("aviso", "Operação cancelada. Nenhuma informação foi alterada.");
        return "redirect:" + confirmacao.urlCancelar();
    }

    private Long executar(ConfirmacaoOperacao confirmacao, RedirectAttributes redirectAttributes) {
        Map<String, String> dados = confirmacao.dados();
        return switch (confirmacao.tipo()) {
            case CRIAR_PARTICIPANTE -> participanteService.cadastrar(
                    new Participante(dados.get("nome"), dados.get("observacoes"))).getId();
            case EDITAR_PARTICIPANTE -> participanteService.atualizar(confirmacao.entidadeId(),
                    new Participante(dados.get("nome"), dados.get("observacoes"))).getId();
            case ATIVAR_PARTICIPANTE -> {
                participanteService.ativar(confirmacao.entidadeId());
                yield confirmacao.entidadeId();
            }
            case DESATIVAR_PARTICIPANTE -> {
                participanteService.desativar(confirmacao.entidadeId());
                yield confirmacao.entidadeId();
            }
            case EXCLUIR_PARTICIPANTE -> {
                participanteService.excluir(confirmacao.entidadeId());
                yield null;
            }
            case CRIAR_ATIVIDADE -> atividadeService.cadastrar(
                    new Atividade(dados.get("nome"), dados.get("descricao"))).getId();
            case EDITAR_ATIVIDADE -> atividadeService.atualizar(confirmacao.entidadeId(),
                    new Atividade(dados.get("nome"), dados.get("descricao"))).getId();
            case ATIVAR_ATIVIDADE -> {
                atividadeService.ativar(confirmacao.entidadeId());
                yield confirmacao.entidadeId();
            }
            case DESATIVAR_ATIVIDADE -> {
                atividadeService.desativar(confirmacao.entidadeId());
                yield confirmacao.entidadeId();
            }
            case EXCLUIR_ATIVIDADE -> {
                atividadeService.excluir(confirmacao.entidadeId());
                yield null;
            }
            case CRIAR_ENCONTRO -> encontroService.criar(
                    Long.valueOf(dados.get("atividadeId")),
                    LocalDate.parse(dados.get("data")),
                    dados.get("observacoes"),
                    StatusEncontro.valueOf(dados.get("status"))).getId();
            case EDITAR_ENCONTRO -> encontroService.atualizar(
                    confirmacao.entidadeId(),
                    Long.valueOf(dados.get("atividadeId")),
                    LocalDate.parse(dados.get("data")),
                    dados.get("observacoes"),
                    StatusEncontro.valueOf(dados.get("status"))).getId();
            case CANCELAR_ENCONTRO -> {
                encontroService.cancelar(confirmacao.entidadeId());
                yield confirmacao.entidadeId();
            }
            case EXCLUIR_ENCONTRO -> {
                encontroService.excluir(confirmacao.entidadeId());
                yield null;
            }
            case REGISTRAR_PRESENCA -> {
                Long encontroId = Long.valueOf(dados.get("encontroId"));
                ResultadoRegistroPresenca resultado = presencaService.registrarLote(
                        encontroId,
                        presencaService.extrairStatus(dados),
                        presencaService.extrairObservacoes(dados));
                redirectAttributes.addFlashAttribute("resultadoPresenca", resultado);
                yield encontroId;
            }
            case EXCLUIR_PRESENCA -> {
                Presenca presenca = presencaService.obter(confirmacao.entidadeId());
                Long encontroId = presenca.getEncontro().getId();
                presencaService.excluir(confirmacao.entidadeId());
                yield encontroId;
            }
        };
    }

    private String destino(String url, Long id) {
        if (url == null || url.isBlank()) {
            return "/";
        }
        return id == null ? url : url.replace("{id}", id.toString());
    }
}
