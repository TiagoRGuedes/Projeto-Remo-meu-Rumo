package com.ibmec.remomeurumo.config;

import java.io.IOException;
import java.net.URI;
import java.time.Duration;
import java.time.LocalDate;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.core.Authentication;
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
import org.springframework.security.web.savedrequest.HttpSessionRequestCache;
import org.springframework.security.web.savedrequest.SavedRequest;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.filter.OncePerRequestFilter;

import com.ibmec.remomeurumo.model.Perfil;
import com.ibmec.remomeurumo.repository.UsuarioRepository;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;

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
                        .successHandler(SegurancaConfig::aposLogin))
                // CSRF vencido (tela de login aberta por muito tempo ou servidor reiniciado): volta ao login em vez do 403.
                .exceptionHandling(e -> e.accessDeniedHandler(new DelegatingAccessDeniedHandler(
                        new LinkedHashMap<>(Map.of(CsrfException.class,
                                (AccessDeniedHandler) SegurancaConfig::csrfVencido)),
                        new AccessDeniedHandlerImpl())))
                .logout(l -> l.logoutSuccessUrl("/login?saiu"))
                .addFilterBefore(new LimiteTentativasLogin(), UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }

    private static final String VOLTAR_PARA = "voltarParaChamada";
    private static final Pattern TURMA = Pattern.compile("/chamada/(\\d+)");

    /** Sessão vencida ao salvar a chamada: guarda a turma e a data para voltar a ela depois do login (as marcações ficam no aparelho). */
    static void csrfVencido(HttpServletRequest req, HttpServletResponse res, AccessDeniedException ex) throws IOException {
        String voltar = null;
        Matcher turma = TURMA.matcher(req.getServletPath());
        if (turma.matches()) {
            try {
                voltar = "/chamada/" + turma.group(1) + "?data=" + LocalDate.parse(req.getParameter("data")) + "&corrigir=1";
                req.getSession().setAttribute(VOLTAR_PARA, voltar);
            } catch (RuntimeException dataInvalida) {
                voltar = null;
            }
        }
        res.sendRedirect(req.getContextPath() + "/login?expirou" + (voltar == null ? "" : "&chamada"));
    }

    /** Volta para a turma que estava aberta (só caminhos /chamada/..., nunca um endereço vindo de fora); senão vai para o início do perfil. */
    static void aposLogin(HttpServletRequest req, HttpServletResponse res, Authentication auth) throws IOException {
        String destino = null;
        HttpSession sessao = req.getSession(false);
        if (sessao != null && sessao.getAttribute(VOLTAR_PARA) instanceof String voltar && voltar.startsWith("/chamada/")) {
            destino = req.getContextPath() + voltar;
            sessao.removeAttribute(VOLTAR_PARA);
        }
        HttpSessionRequestCache cache = new HttpSessionRequestCache();
        SavedRequest salvo = cache.getRequest(req, res);
        if (salvo != null) {
            cache.removeRequest(req, res);
            if (destino == null && URI.create(salvo.getRedirectUrl()).getPath().startsWith(req.getContextPath() + "/chamada/")) {
                destino = salvo.getRedirectUrl();
            }
        }
        boolean coordenacao = auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_COORDENACAO"));
        res.sendRedirect(destino != null ? destino : req.getContextPath() + (coordenacao ? "/" : "/chamada"));
    }

    /**
     * Limite de tentativas de login. Por usuário: a partir de 5 erros bloqueia por 1, 5 e depois 15 minutos (curto, para
     * trancar uma conta alheia custar caro e atrapalhar pouco). Por IP: limite bem maior (30 erros em 15 minutos), para
     * não trancar uma equipe inteira atrás do mesmo IP. Só o login certo zera o contador do usuário, nunca o do IP.
     * O IP vem de getRemoteAddr: o X-Forwarded-For só vale quando o Tomcat o recebe de um proxy interno
     * (server.tomcat.remoteip.internal-proxies), senão qualquer um trocaria o IP a cada tentativa.
     * shortcut: contadores em memória, zeram ao reiniciar e não valem entre instâncias; trocar por cache compartilhado se escalar.
     */
    static class LimiteTentativasLogin extends OncePerRequestFilter {
        private static final int MAXIMO_USUARIO = 5;
        private static final int MAXIMO_IP = 30;
        private static final Duration JANELA = Duration.ofMinutes(15);
        private record Falhas(int total, Instant ultima, Instant bloqueadoAte) { }
        private final Map<String, Falhas> contadores = new ConcurrentHashMap<>();

        @Override
        protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
                throws ServletException, IOException {
            if (!"POST".equals(req.getMethod()) || !"/login".equals(req.getServletPath())) {
                chain.doFilter(req, res);
                return;
            }
            String usuario = req.getParameter("username");
            // alguns proxies (Azure) mandam "ip:porta"; sem tirar a porta cada conexão pareceria um IP novo
            String chaveIp = "ip:" + req.getRemoteAddr().replaceFirst("^(\\d+\\.\\d+\\.\\d+\\.\\d+):\\d+$", "$1");
            String chaveUsuario = "u:" + (usuario == null ? "" : usuario.strip().toLowerCase(Locale.ROOT));
            Instant agora = Instant.now();
            if (contadores.size() > 1000) {
                contadores.values().removeIf(f -> f.ultima().plus(JANELA).isBefore(agora));
            }
            Falhas doIp = vigente(chaveIp, agora);
            Falhas doUsuario = vigente(chaveUsuario, agora);
            if ((doIp != null && doIp.total() >= MAXIMO_IP)
                    || (doUsuario != null && doUsuario.bloqueadoAte().isAfter(agora))) {
                res.sendRedirect(req.getContextPath() + "/login?bloqueado");
                return;
            }
            chain.doFilter(req, res);
            String destino = res.getHeader("Location");
            if (destino != null && destino.contains("/login?erro")) {
                contadores.merge(chaveIp, new Falhas(1, agora, agora),
                        (antiga, nova) -> new Falhas(antiga.total() + 1, agora, agora));
                contadores.merge(chaveUsuario, new Falhas(1, agora, agora), (antiga, nova) -> {
                    int total = antiga.total() + 1;
                    Duration espera = total < MAXIMO_USUARIO ? Duration.ZERO
                            : total == MAXIMO_USUARIO ? Duration.ofMinutes(1)
                            : total < MAXIMO_USUARIO + 3 ? Duration.ofMinutes(5) : Duration.ofMinutes(15);
                    return new Falhas(total, agora, agora.plus(espera));
                });
            } else {
                contadores.remove(chaveUsuario);
            }
        }

        private Falhas vigente(String chave, Instant agora) {
            Falhas f = contadores.get(chave);
            if (f != null && f.ultima().plus(JANELA).isBefore(agora)) {
                contadores.remove(chave);
                return null;
            }
            return f;
        }
    }
}
