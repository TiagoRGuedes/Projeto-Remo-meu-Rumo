/* Só interface, não é segurança: quem barra o acesso é o SegurancaConfig.
   Celular (PWA instalado ou tela de toque estreita) vê só a chamada. */
(function () {
    var m = function (q) { return window.matchMedia(q).matches; };
    if (!m("(pointer: coarse)") || !(m("(display-mode: standalone)") || m("(max-width: 768px)"))) { return; }
    document.documentElement.classList.add("modo-celular");
    if (!/^\/(chamada|login|error|ajuda\/instalar)(\/|$)/.test(location.pathname)) { location.replace("/chamada"); }
})();
