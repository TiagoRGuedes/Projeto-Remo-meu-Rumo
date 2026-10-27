if ("serviceWorker" in navigator) {
    window.addEventListener("load", () => {
        navigator.serviceWorker.register("/sw.js").catch(() => undefined);
    });
}

let pedidoInstalacao = null;

window.addEventListener("beforeinstallprompt", (evento) => {
    evento.preventDefault();
    pedidoInstalacao = evento;
    document.querySelectorAll("button[data-instalar-agora]").forEach((botao) => {
        botao.hidden = false;
    });
});

const prepararInstalacao = () => {
    const chave = "irmr-banner-instalar-fechado";
    const lerFechado = () => {
        try {
            return localStorage.getItem(chave) === "1";
        } catch (erro) {
            return false;
        }
    };
    const instalado = window.matchMedia("(display-mode: standalone)").matches || window.navigator.standalone === true;
    const banner = document.querySelector("[data-banner-instalar]");
    if (banner && !instalado && !lerFechado() && window.matchMedia("(max-width: 760px)").matches
            && !window.location.pathname.startsWith("/ajuda/instalar")) {
        banner.hidden = false;
    }
    const fechar = document.querySelector("[data-fechar-banner]");
    if (fechar && banner) {
        fechar.addEventListener("click", () => {
            banner.hidden = true;
            try {
                localStorage.setItem(chave, "1");
            } catch (erro) {
                return;
            }
        });
    }
    document.querySelectorAll("[data-instalar-agora]").forEach((alvo) => {
        alvo.addEventListener("click", (evento) => {
            if (!pedidoInstalacao) {
                return;
            }
            evento.preventDefault();
            pedidoInstalacao.prompt();
            pedidoInstalacao.userChoice.finally(() => {
                pedidoInstalacao = null;
                if (banner) {
                    banner.hidden = true;
                }
            });
        });
    });
};

document.addEventListener("DOMContentLoaded", () => {
    prepararInstalacao();
    document.querySelectorAll("form").forEach((form) => {
        form.addEventListener("submit", () => {
            const button = form.querySelector("[data-confirm-once]");
            if (!button || button.disabled) {
                return;
            }
            button.disabled = true;
            button.setAttribute("aria-busy", "true");
            const label = button.querySelector("span:last-child");
            if (label) {
                label.textContent = "Confirmando...";
            }
        });
    });

    const attendanceForm = document.querySelector("[data-attendance-form]");
    if (!attendanceForm) {
        return;
    }

    const updateSummary = () => {
        const selected = [...attendanceForm.querySelectorAll('input[type="radio"]:checked')];
        const counts = {PRESENTE: 0, AUSENTE: 0, JUSTIFICADA: 0};
        selected.forEach((input) => {
            if (Object.hasOwn(counts, input.value)) {
                counts[input.value] += 1;
            }
        });
        const values = {
            total: selected.length,
            presente: counts.PRESENTE,
            ausente: counts.AUSENTE,
            justificada: counts.JUSTIFICADA
        };
        Object.entries(values).forEach(([name, value]) => {
            const target = attendanceForm.querySelector(`[data-attendance-count="${name}"]`);
            if (target) {
                target.textContent = String(value);
            }
        });
    };

    attendanceForm.addEventListener("change", (event) => {
        if (event.target.matches('input[type="radio"]')) {
            updateSummary();
        }
    });
    updateSummary();
});
