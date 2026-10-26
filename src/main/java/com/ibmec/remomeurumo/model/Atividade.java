package com.ibmec.remomeurumo.model;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.EnumSet;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "atividades")
public class Atividade {

    private static final DateTimeFormatter HORARIO = DateTimeFormatter.ofPattern("HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o nome da turma.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    @Column(nullable = false, length = 120)
    private String nome;

    @Size(max = 1000, message = "A descrição deve ter no máximo 1000 caracteres.")
    @Column(length = 1000)
    private String descricao;

    @ElementCollection(fetch = FetchType.EAGER, targetClass = DiaSemana.class)
    @CollectionTable(name = "atividade_dias_semana",
            joinColumns = @JoinColumn(name = "atividade_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "dia_semana", length = 20)
    private Set<DiaSemana> diasSemana = new LinkedHashSet<>();

    @DateTimeFormat(pattern = "HH:mm")
    private LocalTime horario;

    @Column(nullable = false)
    private boolean ativo = true;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    public Atividade() {
    }

    public Atividade(String nome, String descricao) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = true;
    }

    public Atividade(String nome, String descricao, Set<DiaSemana> diasSemana, LocalTime horario) {
        this.nome = nome;
        this.descricao = descricao;
        this.ativo = true;
        setDiasSemana(diasSemana);
        this.horario = horario;
    }

    @PrePersist
    public void antesDeCadastrar() {
        if (this.dataCadastro == null) {
            this.dataCadastro = LocalDateTime.now();
        }
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public String getDiasSemanaResumo() {
        if (diasSemana == null || diasSemana.isEmpty()) {
            return "";
        }
        return EnumSet.copyOf(diasSemana).stream().map(DiaSemana::getSigla).collect(Collectors.joining("/"));
    }

    public String getHorarioFormatado() {
        return horario == null ? "" : horario.format(HORARIO);
    }

    public String getGradeResumo() {
        String dias = getDiasSemanaResumo();
        String hora = getHorarioFormatado();
        if (dias.isBlank() && hora.isBlank()) {
            return "Grade não informada";
        }
        if (dias.isBlank()) {
            return hora;
        }
        if (hora.isBlank()) {
            return dias;
        }
        return dias + " · " + hora;
    }

    public boolean isGradeDefinida() {
        return diasSemana != null && !diasSemana.isEmpty() && horario != null;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public Set<DiaSemana> getDiasSemana() {
        return diasSemana;
    }

    public void setDiasSemana(Set<DiaSemana> diasSemana) {
        this.diasSemana = diasSemana == null ? new LinkedHashSet<>() : new LinkedHashSet<>(diasSemana);
    }

    public LocalTime getHorario() {
        return horario;
    }

    public void setHorario(LocalTime horario) {
        this.horario = horario;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
