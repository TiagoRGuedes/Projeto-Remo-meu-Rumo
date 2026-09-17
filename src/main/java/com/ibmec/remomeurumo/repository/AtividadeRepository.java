package com.ibmec.remomeurumo.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ibmec.remomeurumo.model.Atividade;

public interface AtividadeRepository extends JpaRepository<Atividade, Long> {

    List<Atividade> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    List<Atividade> findTop8ByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    List<Atividade> findByAtivoTrueOrderByNomeAsc();

    List<Atividade> findByAtivoFalseOrderByNomeAsc();

    long countByAtivoTrue();

    long countByAtivoFalse();
}
