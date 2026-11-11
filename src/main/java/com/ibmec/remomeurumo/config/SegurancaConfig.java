package com.ibmec.remomeurumo.config;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.session.SessionRegistryImpl;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.session.HttpSessionEventPublisher;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.access.AccessDeniedHandlerImpl;
import org.springframework.security.web.access.DelegatingAccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Login com dois perfis. A chave mestra (COORDENACAO) vem de ADMIN_USUARIO e ADMIN_SENHA; os demais usuários ficam no banco.
 * Regra geral: tudo exige COORDENACAO; o PROFESSOR só entra na lista explícita abaixo.
 */
@Configuration
public class SegurancaConfig {

    @Bean
    PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    UserDetailsService usuarios(@Value("${admin.usuario:}") String usuario,
                                @Value("${admin.senha:}") String senha,
                                PasswordEncoder encoder, UsuarioRepository usuarios) {
        if (usuario.isBlank() || senha.isBlank()) {
            throw new IllegalStateException("Defina as variáveis ADMIN_USUARIO e ADMIN_SENHA para iniciar o sistema.");
        }
        return new ContasUserDetailsService(usuario.strip(), encoder.encode(senha), usuarios);
    }

    /** Lista as sessões abertas por usuário, para derrubá-las ao desativar a conta ou trocar a senha. */
    @Bean
    SessionRegistry sessionRegistry() {
        return new SessionRegistryImpl();
    }

    @Bean
    HttpSessionEventPublisher httpSessionEventPublisher() {
        return new HttpSessionEventPublisher();
    }

    @Bean
    SecurityFilterChain seguranca(HttpSecurity http, SessionRegistry registro) throws Exception {
        http
                .sessionManagement(s -> s.sessionConcurrency(c -> c
                        .maximumSessions(-1).sessionRegistry(registro).expiredUrl("/login?expirou")))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/login", "/error", "/css/**", "/js/**", "/img/**",
                                "/manifest.json", "/sw.js").permitAll()
                        .requestMatchers("/chamada/**").hasAnyRole(Perfil.COORDENACAO.name(), Perfil.PROFESSOR.name())
                        .anyRequest().hasRole(Perfil.COORDENACAO.name()))
                .formLogin(f -> f
                        .loginPage("/login")
                        .failureUrl("/login?erro")
                        .successHandler((req, res, auth) -> res.sendRedirect(req.getContextPath()
                                + (auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDENACAO"))
                                        ? "/" : "/chamada"))))
                // CSRF vencido (tela de login aberta por muito tempo ou servidor reiniciado): volta ao login em vez do 403.
                .exceptionHandling(e -> e.accessDeniedHandler(new DelegatingAccessDeniedHandler(
                        new LinkedHashMap<>(Map.of(CsrfException.class,
                                (AccessDeniedHandler) (req, res, ex) -> res.sendRedirect(req.getContextPath() + "/login?expirou"))),
                        new AccessDeniedHandlerImpl())))
                .logout(l -> l.logoutSuccessUrl("/login?saiu"))
                .addFilterBefore(new LimiteTentativasLogin(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    /**
     * Bloqueia por 15 minutos após 5 senhas erradas, contando por IP e também por usuário
     * (o IP pode vir de X-Forwarded-For e ser trocado a cada tentativa).
     * shortcut: contador em memória, zera ao reiniciar e não vale entre instâncias; trocar por cache compartilhado se escalar.
     */
    static class LimiteTentativasLogin extends OncePerRequestFilter {
        private static final int MAXIMO = 5;
        private static final Duration JANELA = Duration.ofMinutes(15);
        private record Falhas(int total, Instant desde) { }
        private final Map<String, Falhas> contadores = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            if (!"POST".equals(req.getMethod()) || !"/login".equals(req.getServletPath())) {
                chain.doFilter(req, res);
                return;
            }
            String usuario = req.getParameter("username");
            List<String> chaves = List.of("ip:" + req.getRemoteAddr(),
                    "u:" + (usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT)));
            Instant agora = Instant.now();
            if (contadores.size() > 1000) {
                contadores.values().removeIf(f -> f.desde().plus(JANELA).isBefore(agora));
            }
            for (String chave : chaves) {
                Falhas f = contadores.get(chave);
                if (f != null && f.desde().plus(JANELA).isBefore(agora)) {
                    contadores.remove(chave);
                } else if (f != null && f.total() >= MAXIMO) {
                    res.sendRedirect(req.getContextPath() + "/login?bloqueado");
                    return;
                }
            }
            chain.doFilter(req, res);
            String destino = res.getHeader("Location");
            if (destino != null && destino.contains("/login?erro")) {
                chaves.forEach(c -> contadores.merge(c, new Falhas(1, agora),
                        (antiga, nova) -> new Falhas(antiga.total() + 1, antiga.desde())));
            } else {
                chaves.forEach(contadores::remove);
            }
        }
    }
}
