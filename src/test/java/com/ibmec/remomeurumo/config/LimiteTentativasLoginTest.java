package com.ibmec.remomeurumo.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import com.ibmec.remomeurumo.config.SegurancaConfig.LimiteTentativasLogin;

class LimiteTentativasLoginTest {

    private MockHttpServletResponse tentar(LimiteTentativasLogin filtro, String ip, String usuario) throws Exception {
        MockHttpServletRequest req = new MockHttpServletRequest("POST", "/login");
        req.setServletPath("/login");
        req.setRemoteAddr(ip);
        req.setParameter("username", usuario);
        MockHttpServletResponse res = new MockHttpServletResponse();
        filtro.doFilter(req, res, (r, s) -> ((MockHttpServletResponse) s).sendRedirect("/login?erro"));
        return res;
    }

    @Test
    void bloqueiaPorUsuarioMesmoTrocandoDeIp() throws Exception {
        LimiteTentativasLogin filtro = new LimiteTentativasLogin();
        for (int i = 1; i <= 5; i++) {
            assertThat(tentar(filtro, "9.9.9." + i, "Admin").getRedirectedUrl()).isEqualTo("/login?erro");
        }
        assertThat(tentar(filtro, "9.9.9.99", "admin").getRedirectedUrl()).isEqualTo("/login?bloqueado");
    }

    @Test
    void bloqueiaPorIpMesmoTrocandoDeUsuario() throws Exception {
        LimiteTentativasLogin filtro = new LimiteTentativasLogin();
        for (int i = 1; i <= 5; i++) {
            tentar(filtro, "8.8.8.8", "u" + i);
        }
        assertThat(tentar(filtro, "8.8.8.8", "outro").getRedirectedUrl()).isEqualTo("/login?bloqueado");
    }
}
