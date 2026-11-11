package com.ibmec.remomeurumo.service;

import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.model.Usuario;
import com.ibmec.remomeurumo.repository.UsuarioRepository;

@Service
public class UsuarioService {

    private static final Pattern LOGIN = Pattern.compile("[a-z0-9._-]{3,60}");
    private static final int SENHA_MINIMA = 8;
    private static final int SENHA_MAXIMA = 72; // limite do BCrypt

    private final UsuarioRepository usuarioRepository;
    private final PasswordEncoder encoder;
    private final HistoricoOperacaoService historicoService;
    private final String loginMestre;

    public UsuarioService(UsuarioRepository usuarioRepository, PasswordEncoder encoder,
            HistoricoOperacaoService historicoService, @Value("${admin.usuario:}") String loginMestre) {
        this.usuarioRepository = usuarioRepository;
        this.encoder = encoder;
        this.historicoService = historicoService;
        this.loginMestre = loginMestre.strip().toLowerCase(Locale.ROOT);
    }

    @Transactional(readOnly = true)
    public List<Usuario> listar() {
        return usuarioRepository.findAllByOrderByNomeAsc();
    }

    @Transactional
    public Usuario cadastrar(String nome, String login, String senha, Perfil perfil) {
        String nomeLimpo = nome == null ? "" : nome.strip();
        String loginLimpo = login == null ? "" : login.strip().toLowerCase(Locale.ROOT);
        if (nomeLimpo.isEmpty() || nomeLimpo.length() > 120) {
            throw new RegraNegocioException("Informe o nome (até 120 caracteres).");
        }
        if (!LOGIN.matcher(loginLimpo).matches()) {
            throw new RegraNegocioException(
                    "O login deve ter de 3 a 60 caracteres: letras sem acento, números, ponto, hífen ou sublinhado.");
        }
        if (perfil == null) {
            throw new RegraNegocioException("Escolha o perfil.");
        }
        validarSenha(senha);
        if (loginLimpo.equals(loginMestre) || usuarioRepository.existsByLogin(loginLimpo)) {
            throw new RegraNegocioException("Já existe um usuário com este login.");
        }
        Usuario usuario = usuarioRepository.save(new Usuario(nomeLimpo, loginLimpo, encoder.encode(senha), perfil));
        registrar(usuario, AcaoHistorico.CRIACAO, "Usuário cadastrado com perfil " + perfil.getDescricao() + ".");
        return usuario;
    }

    @Transactional
    public void definirAtivo(Long id, boolean ativo, String loginLogado) {
        Usuario usuario = obter(id);
        if (!ativo && usuario.getLogin().equalsIgnoreCase(loginLogado)) {
            throw new RegraNegocioException("Você não pode desativar o seu próprio usuário.");
        }
        usuario.setAtivo(ativo);
        registrar(usuario, ativo ? AcaoHistorico.ATIVACAO : AcaoHistorico.DESATIVACAO,
                ativo ? "Usuário ativado." : "Usuário desativado.");
    }

    @Transactional
    public void redefinirSenha(Long id, String senha) {
        validarSenha(senha);
        Usuario usuario = obter(id);
        usuario.setSenhaHash(encoder.encode(senha));
        registrar(usuario, AcaoHistorico.EDICAO, "Senha redefinida.");
    }

    private Usuario obter(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Usuário não encontrado."));
    }

    private void validarSenha(String senha) {
        if (senha == null || senha.length() < SENHA_MINIMA) {
            throw new RegraNegocioException("A senha deve ter no mínimo " + SENHA_MINIMA + " caracteres.");
        }
        if (senha.getBytes(java.nio.charset.StandardCharsets.UTF_8).length > SENHA_MAXIMA) {
            throw new RegraNegocioException("A senha deve ter no máximo " + SENHA_MAXIMA + " bytes.");
        }
    }

    private void registrar(Usuario usuario, AcaoHistorico acao, String descricao) {
        historicoService.registrar(ModuloHistorico.USUARIOS, "Usuário", usuario.getId(),
                usuario.getNome() + " (" + usuario.getLogin() + ")", acao, descricao);
    }
}
