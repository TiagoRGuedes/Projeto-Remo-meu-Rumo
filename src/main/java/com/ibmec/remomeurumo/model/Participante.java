package com.ibmec.remomeurumo.model;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "participantes")
public class Participante {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Informe o nome do participante.")
    @Size(max = 120, message = "O nome deve ter no máximo 120 caracteres.")
    @Column(nullable = false, length = 120)
    private String nome;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Sexo sexo = Sexo.NAO_INFORMADO;

    @Past(message = "A data de nascimento deve ser anterior a hoje.")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dataNascimento;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private TipoEscola tipoEscola = TipoEscola.NAO_INFORMADO;

    private Boolean possuiDeficiencia;

    @Size(max = 120, message = "O nome da deficiência ou condição deve ter no máximo 120 caracteres.")
    @Column(length = 120)
    private String condicao;

    @Column(nullable = false)
    private boolean ativo = true;

    @Size(max = 1000, message = "As observações devem ter no máximo 1000 caracteres.")
    @Column(length = 1000)
    private String observacoes;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataCadastro;

    @Column(nullable = false)
    private LocalDateTime dataAtualizacao;

    public Participante() {
    }

    public Participante(String nome, String observacoes) {
        this.nome = nome;
        this.observacoes = observacoes;
        this.ativo = true;
    }

    public Participante(String nome, Sexo sexo, LocalDate dataNascimento, TipoEscola tipoEscola,
            Boolean possuiDeficiencia, String condicao, String observacoes) {
        this.nome = nome;
        this.sexo = sexo == null ? Sexo.NAO_INFORMADO : sexo;
        this.dataNascimento = dataNascimento;
        this.tipoEscola = tipoEscola == null ? TipoEscola.NAO_INFORMADO : tipoEscola;
        this.possuiDeficiencia = possuiDeficiencia;
        this.condicao = condicao;
        this.observacoes = observacoes;
        this.ativo = true;
    }

    @PrePersist
    public void antesDeCadastrar() {
        LocalDateTime agora = LocalDateTime.now();
        if (this.dataCadastro == null) {
            this.dataCadastro = agora;
        }
        if (this.dataAtualizacao == null) {
            this.dataAtualizacao = this.dataCadastro;
        }
        if (this.sexo == null) {
            this.sexo = Sexo.NAO_INFORMADO;
        }
        if (this.tipoEscola == null) {
            this.tipoEscola = TipoEscola.NAO_INFORMADO;
        }
    }

    @PreUpdate
    public void antesDeAtualizar() {
        this.dataAtualizacao = LocalDateTime.now();
    }

    public void ativar() {
        this.ativo = true;
    }

    public void desativar() {
        this.ativo = false;
    }

    public Integer getIdade() {
        if (dataNascimento == null) {
            return null;
        }
        return Period.between(dataNascimento, LocalDate.now()).getYears();
    }

    public String getCodigoDeficiencia() {
        if (possuiDeficiencia == null) {
            return "—";
        }
        return possuiDeficiencia ? "CD" : "SD";
    }

    public String getDescricaoDeficiencia() {
        if (possuiDeficiencia == null) {
            return "Não informado";
        }
        if (!possuiDeficiencia) {
            return "Sem deficiência ou condição (SD)";
        }
        return condicao == null || condicao.isBlank() ? "Com deficiência ou condição (CD)" : condicao + " (CD)";
    }

    public boolean isCondicaoPendente() {
        return Boolean.TRUE.equals(possuiDeficiencia) && (condicao == null || condicao.isBlank());
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

    public Sexo getSexo() {
        return sexo;
    }

    public void setSexo(Sexo sexo) {
        this.sexo = sexo;
    }

    public LocalDate getDataNascimento() {
        return dataNascimento;
    }

    public void setDataNascimento(LocalDate dataNascimento) {
        this.dataNascimento = dataNascimento;
    }

    public TipoEscola getTipoEscola() {
        return tipoEscola;
    }

    public void setTipoEscola(TipoEscola tipoEscola) {
        this.tipoEscola = tipoEscola;
    }

    public Boolean getPossuiDeficiencia() {
        return possuiDeficiencia;
    }

    public void setPossuiDeficiencia(Boolean possuiDeficiencia) {
        this.possuiDeficiencia = possuiDeficiencia;
    }

    public String getCondicao() {
        return condicao;
    }

    public void setCondicao(String condicao) {
        this.condicao = condicao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    public LocalDateTime getDataAtualizacao() {
        return dataAtualizacao;
    }

    public void setDataAtualizacao(LocalDateTime dataAtualizacao) {
        this.dataAtualizacao = dataAtualizacao;
    }
}
