package com.ibmec.remomeurumo.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import com.ibmec.remomeurumo.model.TipoAnexo;

public record AnexoResumo(
        Long id,
        TipoAnexo tipo,
        String nomeArquivo,
        long tamanho,
        LocalDateTime enviadoEm,
        LocalDate dataAula) {

    private static final DateTimeFormatter DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DIA_MES = DateTimeFormatter.ofPattern("dd/MM");

    public String getDescricao() {
        String texto = tipo.getDescricao() + " · " + enviadoEm.format(DATA) + " · " + tamanhoFormatado();
        return dataAula == null ? texto : texto + " · aula de " + dataAula.format(DIA_MES);
    }

    private String tamanhoFormatado() {
        if (tamanho < 1024) {
            return tamanho + " B";
        }
        if (tamanho < 1024 * 1024) {
            return Math.round(tamanho / 1024.0) + " KB";
        }
        return String.format(Locale.forLanguageTag("pt-BR"), "%.1f MB", tamanho / (1024.0 * 1024.0));
    }
}
