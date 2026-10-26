package com.ibmec.remomeurumo.controller;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

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
import com.ibmec.remomeurumo.model.DiaSemana;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.MotivoSaida;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.Presenca;
import com.ibmec.remomeurumo.model.Sexo;
import com.ibmec.remomeurumo.model.StatusEncontro;
import com.ibmec.remomeurumo.model.TipoEscola;
import com.ibmec.remomeurumo.service.AtividadeService;
import com.ibmec.remomeurumo.service.ConfirmacaoOperacaoService;
import com.ibmec.remomeurumo.service.EncontroService;
import com.ibmec.remomeurumo.service.MatriculaService;
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
    private final MatriculaService matriculaService;

    public ConfirmacaoWebController(ConfirmacaoOperacaoService confirmacaoService,
            ParticipanteService participanteService,
            AtividadeService atividadeService,
            EncontroService encontroService,
            PresencaService presencaService,
            MatriculaService matriculaService) {
        this.confirmacaoService = confirmacaoService;
        this.participanteService = participanteService;
        this.atividadeService = atividadeService;
        this.encontroService = encontroService;
        this.presencaService = presencaService;
        this.matriculaService = matriculaService;
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
            case CRIAR_PARTICIPANTE -> {
                Long participanteId = participanteService.cadastrar(participanteDe(dados)).getId();
                String turmaIds = dados.getOrDefault("turmaIds", "");
                for (String turmaId : turmaIds.split(",")) {
                    if (!turmaId.isBlank()) {
                        matriculaService.matricular(participanteId, Long.valueOf(turmaId.trim()), LocalDate.now());
                    }
                }
                yield participanteId;
            }
            case EDITAR_PARTICIPANTE -> participanteService.atualizar(confirmacao.entidadeId(),
                    participanteDe(dados)).getId();
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
            case CRIAR_ATIVIDADE -> atividadeService.cadastrar(atividadeDe(dados)).getId();
            case EDITAR_ATIVIDADE -> atividadeService.atualizar(confirmacao.entidadeId(),
                    atividadeDe(dados)).getId();
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
            case MATRICULAR_PARTICIPANTE -> {
                matriculaService.matricular(
                        Long.valueOf(dados.get("participanteId")),
                        Long.valueOf(dados.get("turmaId")),
                        data(dados.get("dataInicio")));
                yield Long.valueOf(dados.get("participanteId"));
            }
            case ENCERRAR_MATRICULA -> matriculaService.encerrar(
                    confirmacao.entidadeId(),
                    MotivoSaida.valueOf(dados.get("motivoSaida")),
                    dados.get("observacaoSaida"),
                    data(dados.get("dataSaida"))).getParticipante().getId();
            case REATIVAR_MATRICULA -> matriculaService.reativar(
                    confirmacao.entidadeId()).getParticipante().getId();
            case REMOVER_TURMA -> matriculaService.removerTurma(
                    confirmacao.entidadeId(),
                    data(dados.get("dataRemocao"))).getParticipante().getId();
            case TROCAR_TURMA -> matriculaService.trocarTurma(
                    confirmacao.entidadeId(),
                    Long.valueOf(dados.get("novaTurmaId")),
                    data(dados.get("dataTroca"))).getParticipante().getId();
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

    private Participante participanteDe(Map<String, String> dados) {
        return new Participante(
                dados.get("nome"),
                enumOuPadrao(dados.get("sexo"), Sexo.class, Sexo.NAO_INFORMADO),
                data(dados.get("dataNascimento")),
                enumOuPadrao(dados.get("tipoEscola"), TipoEscola.class, TipoEscola.NAO_INFORMADO),
                booleanOuNulo(dados.get("possuiDeficiencia")),
                dados.get("condicao"),
                dados.get("observacoes"));
    }

    private Atividade atividadeDe(Map<String, String> dados) {
        return new Atividade(
                dados.get("nome"),
                dados.get("descricao"),
                diasSemana(dados.get("diasSemana")),
                hora(dados.get("horario")));
    }

    private Set<DiaSemana> diasSemana(String valor) {
        if (valor == null || valor.isBlank()) {
            return Set.of();
        }
        Set<DiaSemana> dias = new LinkedHashSet<>();
        for (String item : valor.split(",")) {
            String nome = item.trim();
            if (!nome.isEmpty()) {
                dias.add(DiaSemana.valueOf(nome));
            }
        }
        return dias;
    }

    private LocalDate data(String valor) {
        return valor == null || valor.isBlank() ? null : LocalDate.parse(valor);
    }

    private LocalTime hora(String valor) {
        return valor == null || valor.isBlank() ? null : LocalTime.parse(valor);
    }

    private Boolean booleanOuNulo(String valor) {
        return valor == null || valor.isBlank() ? null : Boolean.valueOf(valor);
    }

    private <T extends Enum<T>> T enumOuPadrao(String valor, Class<T> tipo, T padrao) {
        return valor == null || valor.isBlank() ? padrao : Enum.valueOf(tipo, valor);
    }

    private String destino(String url, Long id) {
        if (url == null || url.isBlank()) {
            return "/";
        }
        return id == null ? url : url.replace("{id}", id.toString());
    }
}
