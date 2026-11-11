package com.ibmec.remomeurumo.controller;

import java.security.Principal;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.service.UsuarioService;

@Controller
@RequestMapping("/usuarios")
public class UsuarioWebController {

    private final UsuarioService usuarioService;

    public UsuarioWebController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping
    public String listar(Model model, Principal principal) {
        model.addAttribute("usuarios", usuarioService.listar());
        model.addAttribute("perfis", Perfil.values());
        model.addAttribute("loginLogado", principal.getName());
        return "usuarios/index";
    }

    @PostMapping
    public String cadastrar(@RequestParam String nome, @RequestParam String login, @RequestParam String senha,
            @RequestParam Perfil perfil, RedirectAttributes redirect) {
        return executar(redirect, "Usuário cadastrado.", () -> usuarioService.cadastrar(nome, login, senha, perfil));
    }

    @PostMapping("/{id}/ativar")
    public String ativar(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return executar(redirect, "Usuário ativado.", () -> usuarioService.definirAtivo(id, true, principal.getName()));
    }

    @PostMapping("/{id}/desativar")
    public String desativar(@PathVariable Long id, Principal principal, RedirectAttributes redirect) {
        return executar(redirect, "Usuário desativado.",
                () -> usuarioService.definirAtivo(id, false, principal.getName()));
    }

    @PostMapping("/{id}/senha")
    public String redefinirSenha(@PathVariable Long id, @RequestParam String senha, RedirectAttributes redirect) {
        return executar(redirect, "Senha redefinida.", () -> usuarioService.redefinirSenha(id, senha));
    }

    private String executar(RedirectAttributes redirect, String sucesso, Runnable acao) {
        try {
            acao.run();
            redirect.addFlashAttribute("sucesso", sucesso);
        } catch (RegraNegocioException ex) {
            redirect.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/usuarios";
    }
}
