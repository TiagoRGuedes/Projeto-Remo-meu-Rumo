document.addEventListener("DOMContentLoaded", () => {
    document.querySelectorAll("[data-fechar-detalhes]").forEach((botao) => {
        botao.addEventListener("click", () => {
            const detalhes = botao.closest("details");
            if (detalhes) {
                detalhes.open = false;
                const resumo = detalhes.querySelector("summary");
                if (resumo) {
                    resumo.focus();
                }
            }
        });
    });

    const form = document.querySelector("[data-chamada]");
    if (!form) {
        return;
    }
    const chave = "irmr-chamada-" + form.dataset.chamada;
    const VALIDADE = 24 * 60 * 60 * 1000;
    const guardar = (marcas) => {
        try {
            if (marcas === null) {
                localStorage.removeItem(chave);
            } else {
                // o rascunho tem dono e prazo: num celular compartilhado, outra pessoa não vê as marcações de quem saiu
                localStorage.setItem(chave, JSON.stringify({dono: form.dataset.usuario, salvoEm: Date.now(), marcas}));
            }
        } catch (erro) {
            return;
        }
    };
    const lerRascunho = () => {
        try {
            const item = JSON.parse(localStorage.getItem(chave) || "null");
            if (item && item.dono === form.dataset.usuario && Date.now() - item.salvoEm < VALIDADE) {
                return item.marcas;
            }
            localStorage.removeItem(chave);
        } catch (erro) {
            return null;
        }
        return null;
    };

    if (form.dataset.somenteLeitura === "true") {
        // só apaga o rascunho quando a chamada acabou de ser salva; ao abrir uma chamada já salva sem corrigir, ele fica
        if (form.dataset.salva === "true") {
            guardar(null);
        }
        return;
    }

    // a marcação só mexe no celular; sem isso a sessão (30 min) vence no meio da aula e o "Salvar" falha
    setInterval(() => {
        fetch("/chamada", {method: "HEAD", credentials: "same-origin"}).catch(() => undefined);
    }, 10 * 60 * 1000);

    const ciclo = ["PRESENTE", "AUSENTE", "JUSTIFICADA"];
    const linhas = [...form.querySelectorAll(".chamada-linha")];
    const salvar = form.querySelector("[data-salvar]");
    const progresso = form.querySelector("[data-progresso]");
    const marcados = form.querySelector("[data-marcados]");

    const valorDe = (linha) => {
        const marcado = linha.querySelector("input[type=radio]:checked");
        return marcado ? marcado.value : "";
    };
    const marcar = (linha, valor) => {
        const alvo = linha.querySelector(`input[type=radio][value="${valor}"]`);
        if (alvo) {
            alvo.checked = true;
        }
    };

    const atualizar = (gravar) => {
        const contas = {PRESENTE: 0, AUSENTE: 0, JUSTIFICADA: 0};
        const rascunho = {};
        linhas.forEach((linha) => {
            const valor = valorDe(linha);
            linha.dataset.status = valor;
            if (valor) {
                contas[valor] += 1;
                rascunho[linha.dataset.participante] = valor;
            }
        });
        const total = linhas.length;
        const feitos = Object.keys(rascunho).length;
        Object.entries(contas).forEach(([status, valor]) => {
            const alvo = form.querySelector(`[data-conta="${status}"]`);
            if (alvo) {
                alvo.textContent = String(valor);
            }
        });
        if (marcados) {
            marcados.textContent = String(feitos);
        }
        if (progresso) {
            progresso.style.width = (total === 0 ? 0 : feitos / total * 100) + "%";
        }
        if (salvar) {
            const faltam = total - feitos;
            salvar.disabled = faltam > 0;
            salvar.textContent = faltam > 0 ? `Faltam ${faltam} para salvar` : "Salvar chamada";
        }
        if (gravar) {
            guardar(rascunho);
        }
    };

    const rascunho = lerRascunho();
    if (rascunho) {
        linhas.forEach((linha) => {
            const valor = rascunho[linha.dataset.participante];
            if (valor) {
                marcar(linha, valor);
            }
        });
    }

    linhas.forEach((linha) => {
        linha.addEventListener("click", (evento) => {
            if (evento.target.closest("label, input")) {
                return;
            }
            const atual = valorDe(linha);
            const proximo = atual ? ciclo[(ciclo.indexOf(atual) + 1) % ciclo.length] : ciclo[0];
            marcar(linha, proximo);
            atualizar(true);
        });
    });

    form.addEventListener("change", () => atualizar(true));

    const todos = form.querySelector("[data-todos-presentes]");
    if (todos) {
        todos.addEventListener("click", () => {
            linhas.filter((linha) => !valorDe(linha)).forEach((linha) => marcar(linha, "PRESENTE"));
            atualizar(true);
        });
    }

    form.addEventListener("submit", (evento) => {
        if (salvar && salvar.disabled) {
            evento.preventDefault();
            return;
        }
        if (salvar) {
            salvar.setAttribute("aria-busy", "true");
            salvar.textContent = "Salvando...";
        }
    });

    atualizar(false);
});
