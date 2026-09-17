package com.ibmec.remomeurumo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "encontros", uniqueConstraints = {
        @UniqueConstraint(name = "uk_encontro_atividade_data", columnNames = {"atividade_id", "data"})
})
public class Encontro {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe a atividade.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @NotNull(message = "Informe a data do encontro.")
    @Column(nullable = false)
    private LocalDate data;

    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
    @Column(length = 1000)
    private String observacoes;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusEncontro status = StatusEncontro.REALIZADO;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    public Encontro() {
    }

    public Encontro(Atividade atividade, LocalDate data, String observacoes) {
        this.atividade = atividade;
        this.data = data;
        this.observacoes = observacoes;
        this.status = StatusEncontro.REALIZADO;
    }

    @PrePersist
    public void antesDeCadastrar() {
        if (this.dataCadastro == null) {
            this.dataCadastro = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Atividade getAtividade() {
        return atividade;
    }

    public void setAtividade(Atividade atividade) {
        this.atividade = atividade;
    }

    public LocalDate getData() {
        return data;
    }

    public void setData(LocalDate data) {
        this.data = data;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public StatusEncontro getStatus() {
        return status;
    }

    public void setStatus(StatusEncontro status) {
        this.status = status;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }
}
