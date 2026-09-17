package com.ibmec.remomeurumo.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;

@Entity
@Table(name = "historico_operacoes")
public class HistoricoOperacao {

    private static final DateTimeFormatter DATA_FORMATADA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter HORA_FORMATADA = DateTimeFormatter.ofPattern("HH:mm");

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ModuloHistorico modulo;

    @Column(nullable = false, length = 60)
    private String tipoEntidade;

    @Column(nullable = false)
    private Long entidadeId;

    @Column(nullable = false, length = 180)
    private String nomeEntidade;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private AcaoHistorico acao;

    @Column(nullable = false, length = 600)
    private String descricao;

    @Column(length = 500)
    private String detalhesAnteriores;

    @Column(length = 500)
    private String detalhesNovos;

    @Column(nullable = false, updatable = false)
    private LocalDateTime dataHora;

    protected HistoricoOperacao() {
    }

    public HistoricoOperacao(ModuloHistorico modulo, String tipoEntidade, Long entidadeId,
            String nomeEntidade, AcaoHistorico acao, String descricao) {
        this(modulo, tipoEntidade, entidadeId, nomeEntidade, acao, descricao, null, null);
    }

    public HistoricoOperacao(ModuloHistorico modulo, String tipoEntidade, Long entidadeId,
            String nomeEntidade, AcaoHistorico acao, String descricao,
            String detalhesAnteriores, String detalhesNovos) {
        this.modulo = modulo;
        this.tipoEntidade = tipoEntidade;
        this.entidadeId = entidadeId;
        this.nomeEntidade = nomeEntidade;
        this.acao = acao;
        this.descricao = descricao;
        this.detalhesAnteriores = detalhesAnteriores;
        this.detalhesNovos = detalhesNovos;
    }

    @PrePersist
    public void antesDeCadastrar() {
        this.dataHora = LocalDateTime.now();
    }

    public Long getId() {
        return id;
    }

    public ModuloHistorico getModulo() {
        return modulo;
    }

    public String getTipoEntidade() {
        return tipoEntidade;
    }

    public Long getEntidadeId() {
        return entidadeId;
    }

    public String getNomeEntidade() {
        return nomeEntidade;
    }

    public AcaoHistorico getAcao() {
        return acao;
    }

    public String getDescricao() {
        return descricao;
    }

    public String getDetalhesAnteriores() {
        return detalhesAnteriores;
    }

    public String getDetalhesNovos() {
        return detalhesNovos;
    }

    public boolean isTemComparacao() {
        return detalhesAnteriores != null && !detalhesAnteriores.isBlank()
                && detalhesNovos != null && !detalhesNovos.isBlank();
    }

    public String getUrlRelacionada() {
        if (acao == AcaoHistorico.EXCLUSAO || entidadeId == null) {
            return null;
        }
        return switch (modulo) {
            case PARTICIPANTES -> "/participantes/" + entidadeId;
            case ATIVIDADES -> "/atividades/" + entidadeId;
            case ENCONTROS, PRESENCAS -> "/encontros/" + entidadeId;
        };
    }

    public LocalDateTime getDataHora() {
        return dataHora;
    }

    public String getDataFormatada() {
        return dataHora == null ? "" : dataHora.format(DATA_FORMATADA);
    }

    public String getHoraFormatada() {
        return dataHora == null ? "" : dataHora.format(HORA_FORMATADA);
    }
}
