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
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "matriculas")
public class Matricula {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull(message = "Informe o participante.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    @NotNull(message = "Informe a turma.")
    @ManyToOne(optional = false)
    @JoinColumn(name = "atividade_id", nullable = false)
    private Atividade atividade;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusMatricula status = StatusMatricula.ATIVA;

    @Column(nullable = false)
    private LocalDate dataInicio;

    private LocalDate dataSaida;

    @Enumerated(EnumType.STRING)
    @Column(length = 40)
    private MotivoSaida motivoSaida;

    @Size(max = 300, message = "A observação de saída deve ter no máximo 300 caracteres.")
    @Column(length = 300)
    private String observacaoSaida;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Matricula() {
    }

    public Matricula(Participante participante, Atividade atividade, LocalDate dataInicio) {
        this.participante = participante;
        this.atividade = atividade;
        this.dataInicio = dataInicio == null ? LocalDate.now() : dataInicio;
        this.status = StatusMatricula.ATIVA;
    }

    @PrePersist
    public void antesDeCadastrar() {
        LocalDateTime agora = LocalDateTime.now();
        if (this.dataCadastro == null) {
            this.dataCadastro = agora;
        }
        if (this.dataInicio == null) {
            this.dataInicio = LocalDate.now();
        }
        this.dataAtualizacao = agora;
    }

    @PreUpdate
    public void antesDeAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void encerrar(MotivoSaida motivoSaida, String observacaoSaida, LocalDate dataSaida) {
        this.status = StatusMatricula.ENCERRADA;
        this.motivoSaida = motivoSaida;
        this.observacaoSaida = observacaoSaida;
        this.dataSaida = dataSaida == null ? LocalDate.now() : dataSaida;
    }

    public void reativar() {
        this.status = StatusMatricula.ATIVA;
        this.motivoSaida = null;
        this.observacaoSaida = null;
        this.dataSaida = null;
    }

    public boolean isAtiva() {
        return status == StatusMatricula.ATIVA;
    }

    public boolean vigenteAPartirDe(LocalDate data) {
        if (isAtiva()) {
            return true;
        }
        return dataSaida == null || !dataSaida.isBefore(data);
    }

    public boolean vigenteEm(LocalDate data) {
        if (dataInicio != null && data.isBefore(dataInicio)) {
            return false;
        }
        return isAtiva() || dataSaida == null || !data.isAfter(dataSaida);
    }

    public LocalDate inicioNoPeriodo(LocalDate inicio) {
        return dataInicio != null && dataInicio.isAfter(inicio) ? dataInicio : inicio;
    }

    public LocalDate fimNoPeriodo(LocalDate fim) {
        return !isAtiva() && dataSaida != null && dataSaida.isBefore(fim) ? dataSaida : fim;
    }

    public boolean vigenteNoPeriodo(LocalDate inicio, LocalDate fim) {
        return !inicioNoPeriodo(inicio).isAfter(fimNoPeriodo(fim));
    }

    public String getMotivoSaidaDescricao() {
        return motivoSaida == null ? "Não informado" : motivoSaida.getDescricao();
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Participante getParticipante() {
        return participante;
    }

    public void setParticipante(Participante participante) {
        this.participante = participante;
    }

    public Atividade getAtividade() {
        return atividade;
    }

    public void setAtividade(Atividade atividade) {
        this.atividade = atividade;
    }

    public StatusMatricula getStatus() {
        return status;
    }

    public void setStatus(StatusMatricula status) {
        this.status = status;
    }

    public LocalDate getDataInicio() {
        return dataInicio;
    }

    public void setDataInicio(LocalDate dataInicio) {
        this.dataInicio = dataInicio;
    }

    public LocalDate getDataSaida() {
        return dataSaida;
    }

    public void setDataSaida(LocalDate dataSaida) {
        this.dataSaida = dataSaida;
    }

    public MotivoSaida getMotivoSaida() {
        return motivoSaida;
    }

    public void setMotivoSaida(MotivoSaida motivoSaida) {
        this.motivoSaida = motivoSaida;
    }

    public String getObservacaoSaida() {
        return observacaoSaida;
    }

    public void setObservacaoSaida(String observacaoSaida) {
        this.observacaoSaida = observacaoSaida;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }
}
