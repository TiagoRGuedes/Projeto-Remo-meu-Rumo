package com.ibmec.remomeurumo.service;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.dto.ResultadoBuscaGlobal;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;

@Service
public class BuscaGlobalService {

    private static final int LIMITE_POR_GRUPO = 8;

    private final ParticipanteRepository participanteRepository;
    private final AtividadeRepository atividadeRepository;
    private final EncontroRepository encontroRepository;

    public BuscaGlobalService(ParticipanteRepository participanteRepository,
            AtividadeRepository atividadeRepository,
            EncontroRepository encontroRepository) {
        this.participanteRepository = participanteRepository;
        this.atividadeRepository = atividadeRepository;
        this.encontroRepository = encontroRepository;
    }

    @Transactional(readOnly = true)
    public ResultadoBuscaGlobal buscar(String consulta) {
        String termo = consulta == null ? "" : consulta.trim();
        if (termo.length() < 2) {
            return new ResultadoBuscaGlobal(termo, List.of(), List.of(), List.of());
        }

        var participantes = participanteRepository.findTop8ByNomeContainingIgnoreCaseOrderByNomeAsc(termo);
        var atividades = atividadeRepository.findTop8ByNomeContainingIgnoreCaseOrderByNomeAsc(termo);
        var encontros = buscarEncontros(termo);
        return new ResultadoBuscaGlobal(termo, participantes, atividades, encontros);
    }

    private List<Encontro> buscarEncontros(String termo) {
        List<Encontro> porAtividade = encontroRepository
                .findTop8ByAtividadeNomeContainingIgnoreCaseOrderByDataDesc(termo);
        try {
            LocalDate data = LocalDate.parse(termo);
            return encontroRepository.findByDataOrderByAtividadeNomeAsc(data).stream()
                    .limit(LIMITE_POR_GRUPO)
                    .toList();
        } catch (DateTimeParseException ex) {
            return porAtividade;
        }
    }
}
