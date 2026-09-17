document.addEventListener("DOMContentLoaded", () => {
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
