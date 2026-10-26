package com.ibmec.remomeurumo.service;

import java.io.IOException;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import com.ibmec.remomeurumo.dto.AnexoResumo;
import com.ibmec.remomeurumo.exception.RecursoNaoEncontradoException;
import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.AcaoHistorico;
import com.ibmec.remomeurumo.model.Anexo;
import com.ibmec.remomeurumo.model.Encontro;
import com.ibmec.remomeurumo.model.ModuloHistorico;
import com.ibmec.remomeurumo.model.Participante;
import com.ibmec.remomeurumo.model.TipoAnexo;
import com.ibmec.remomeurumo.repository.AnexoRepository;
import com.ibmec.remomeurumo.repository.EncontroRepository;
import com.ibmec.remomeurumo.repository.ParticipanteRepository;

@Service
public class AnexoService {

    public static final long TAMANHO_MAXIMO = 5L * 1024 * 1024;

    private static final Set<String> TIPOS_PERMITIDOS = Set.of(
            "application/pdf", "image/jpeg", "image/png", "image/webp", "image/heic");

    private final AnexoRepository anexoRepository;
    private final ParticipanteRepository participanteRepository;
    private final EncontroRepository encontroRepository;
    private final HistoricoOperacaoService historicoService;

    public AnexoService(AnexoRepository anexoRepository,
            ParticipanteRepository participanteRepository,
            EncontroRepository encontroRepository,
            HistoricoOperacaoService historicoService) {
        this.anexoRepository = anexoRepository;
        this.participanteRepository = participanteRepository;
        this.encontroRepository = encontroRepository;
        this.historicoService = historicoService;
    }

    @Transactional(readOnly = true)
    public List<AnexoResumo> listarPorParticipante(Long participanteId) {
        return anexoRepository.listarPorParticipante(participanteId);
    }

    @Transactional(readOnly = true)
    public List<Long> participantesComAnexoNaAula(Long encontroId) {
        return encontroId == null ? List.of() : anexoRepository.participantesComAnexoNaAula(encontroId);
    }

    @Transactional(readOnly = true)
    public Anexo obter(Long id) {
        Anexo anexo = anexoRepository.findById(id)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Arquivo não encontrado."));
        anexo.getConteudo();
        anexo.getParticipante().getNome();
        return anexo;
    }

    @Transactional
    public Anexo anexar(Long participanteId, TipoAnexo tipo, MultipartFile arquivo, Long encontroId) {
        Participante participante = participanteRepository.findById(participanteId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Participante não encontrado."));
        if (arquivo == null || arquivo.isEmpty()) {
            throw new RegraNegocioException("Escolha um arquivo para anexar.");
        }
        if (arquivo.getSize() > TAMANHO_MAXIMO) {
            throw new RegraNegocioException("O arquivo deve ter no máximo 5 MB.");
        }
        String contentType = arquivo.getContentType() == null ? "" : arquivo.getContentType().toLowerCase(Locale.ROOT);
        if (!TIPOS_PERMITIDOS.contains(contentType)) {
            throw new RegraNegocioException("Envie um PDF ou uma foto (JPG, PNG, WEBP ou HEIC).");
        }
        Encontro encontro = encontroId == null ? null : encontroRepository.findById(encontroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula não encontrada."));
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException ex) {
            throw new RegraNegocioException("Não foi possível ler o arquivo. Tente enviar novamente.");
        }
        TipoAnexo tipoFinal = tipo == null ? TipoAnexo.OUTRO : tipo;
        Anexo anexo = anexoRepository.save(new Anexo(participante, encontro, tipoFinal,
                nomeSeguro(arquivo.getOriginalFilename()), contentType, conteudo));
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", participante.getId(),
                participante.getNome(), AcaoHistorico.ARQUIVO,
                tipoFinal.getDescricao() + " anexado ao cadastro de " + participante.getNome()
                        + (encontro == null ? "." : " para a aula de " + encontro.getAtividade().getNome()
                                + " em " + encontro.getData() + "."));
        return anexo;
    }

    private String nomeSeguro(String nome) {
        String base = nome == null || nome.isBlank() ? "arquivo" : nome.replace('\\', '/');
        base = base.substring(base.lastIndexOf('/') + 1).replaceAll("[\\r\\n\"]", "_").trim();
        if (base.isEmpty()) {
            base = "arquivo";
        }
        return base.length() > 200 ? base.substring(base.length() - 200) : base;
    }
}
