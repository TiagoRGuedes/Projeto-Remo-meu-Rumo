package com.ibmec.remomeurumo.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import jakarta.persistence.Basic;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "anexos")
public class Anexo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "participante_id", nullable = false)
    private Participante participante;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "encontro_id")
    private Encontro encontro;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TipoAnexo tipo;

    @Column(nullable = false, length = 255)
    private String nomeArquivo;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long tamanho;

    @Basic(fetch = FetchType.LAZY)
    @JdbcTypeCode(SqlTypes.VARBINARY)
    @Column(nullable = false, columnDefinition = "varbinary(6000000)")
    private byte[] conteudo;

    @Column(nullable = false, updatable = false)
    private LocalDateTime enviadoEm;

    public Anexo() {
    }

    public Anexo(Participante participante, Encontro encontro, TipoAnexo tipo, String nomeArquivo,
            String contentType, byte[] conteudo) {
        this.participante = participante;
        this.encontro = encontro;
        this.tipo = tipo;
        this.nomeArquivo = nomeArquivo;
        this.contentType = contentType;
        this.conteudo = conteudo;
        this.tamanho = conteudo.length;
    }

    @PrePersist
    public void antesDeCadastrar() {
        if (enviadoEm == null) {
            enviadoEm = LocalDateTime.now();
        }
    }

    public Long getId() {
        return id;
    }

    public Participante getParticipante() {
        return participante;
    }

    public Encontro getEncontro() {
        return encontro;
    }

    public void setEncontro(Encontro encontro) {
        this.encontro = encontro;
    }

    public TipoAnexo getTipo() {
        return tipo;
    }

    public String getNomeArquivo() {
        return nomeArquivo;
    }

    public String getContentType() {
        return contentType;
    }

    public long getTamanho() {
        return tamanho;
    }

    public byte[] getConteudo() {
        return conteudo;
    }

    public LocalDateTime getEnviadoEm() {
        return enviadoEm;
    }
}
