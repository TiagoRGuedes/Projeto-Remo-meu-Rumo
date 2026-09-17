package com.ibmec.remomeurumo.dto;

import java.time.LocalDateTime;

import com.ibmec.remomeurumo.model.Atividade;

public record AtividadeOutput(
        Long id,
        String nome,
        String descricao,
        boolean ativo,
        LocalDateTime dataCadastro) {

    public static AtividadeOutput de(Atividade atividade) {
        return new AtividadeOutput(
                atividade.getId(),
                atividade.getNome(),
                atividade.getDescricao(),
                atividade.isAtivo(),
                atividade.getDataCadastro());
    }
}
