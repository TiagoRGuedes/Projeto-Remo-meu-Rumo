package com.ibmec.remomeurumo.support;

public final class Csv {

    private Csv() {
    }

    /** Texto entre aspas; quem começa com = + - @ (ou tab/CR) ganha um ' na frente para o Excel não abrir como fórmula. */
    public static String celula(String texto) {
        String t = texto == null ? "" : texto;
        if (!t.isEmpty() && "=+-@\t\r".indexOf(t.charAt(0)) >= 0) {
            t = "'" + t;
        }
        return "\"" + t.replace("\"", "\"\"") + "\"";
    }
}
