package com.ibmec.remomeurumo.config;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.ibmec.remomeurumo.model.Atividade;
import com.ibmec.remomeurumo.repository.AtividadeRepository;
import com.ibmec.remomeurumo.service.AtividadeService;

@Component
@Order(20)
public class SincronizacaoTurmas implements ApplicationRunner {

    private final boolean ativa;
    private final AtividadeRepository atividadeRepository;
    private final AtividadeService atividadeService;

    public SincronizacaoTurmas(@Value("${app.turmas-oficiais.criar:true}") boolean ativa,
            AtividadeRepository atividadeRepository,
            AtividadeService atividadeService) {
        this.ativa = ativa;
        this.atividadeRepository = atividadeRepository;
        this.atividadeService = atividadeService;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!ativa) {
            return;
        }
        Set<String> existentes = atividadeRepository.findAll().stream()
                .map(Atividade::getNome)
                .collect(Collectors.toSet());
        for (TurmasOficiais.Turma turma : TurmasOficiais.LISTA) {
            if (!existentes.contains(turma.nome())) {
                atividadeService.cadastrar(turma.novaAtividade());
            }
        }
    }
}
