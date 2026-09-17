package com.ibmec.remomeurumo.dto;

import java.io.Serializable;

public record AlteracaoConfirmacao(
        String campo,
        String antes,
        String depois,
        boolean alterado) implements Serializable {
}
