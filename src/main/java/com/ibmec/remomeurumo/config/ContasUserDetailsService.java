package com.ibmec.remomeurumo.config;

import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Locale;

import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.repository.UsuarioRepository;

/** Junta a chave mestra (ADMIN_USUARIO/ADMIN_SENHA, sempre COORDENACAO, fora do banco) e os usuários ativos do banco. */
class ContasUserDetailsService implements UserDetailsService {

    private final String loginMestre;
    private final String senhaMestre;
    private final UsuarioRepository usuarios;

    ContasUserDetailsService(String loginMestre, String senhaMestreCodificada, UsuarioRepository usuarios) {
        this.loginMestre = loginMestre;
        this.senhaMestre = senhaMestreCodificada;
        this.usuarios = usuarios;
    }

    @Override
    public UserDetails loadUserByUsername(String login) {
        String chave = login == null ? "" : login.strip().toLowerCase(Locale.ROOT);
        if (chave.equals(loginMestre.toLowerCase(Locale.ROOT))) {
            // objeto novo a cada chamada: o Spring apaga a senha do UserDetails após o login
            return User.withUsername(loginMestre).password(senhaMestre).roles(Perfil.COORDENACAO.name()).build();
        }
        return usuarios.findByLoginAndAtivoTrue(chave)
                .map(u -> User.withUsername(u.getLogin()).password(u.getSenhaHash())
                        .roles(u.getPerfil().name()).build())
                .orElseThrow(() -> new UsernameNotFoundException("Usuário não encontrado."));
    }
}
