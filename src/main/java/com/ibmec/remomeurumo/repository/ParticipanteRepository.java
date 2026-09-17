package com.ibmec.remomeurumo.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.ibmec.remomeurumo.model.Participante;

public interface ParticipanteRepository extends JpaRepository<Participante, Long> {

    List<Participante> findByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    List<Participante> findTop8ByNomeContainingIgnoreCaseOrderByNomeAsc(String nome);

    List<Participante> findByAtivoTrueOrderByNomeAsc();

    List<Participante> findByAtivoFalseOrderByNomeAsc();

    long countByAtivoTrue();

    long countByAtivoFalse();

    long countByDataCadastroAfter(LocalDateTime data);
}
