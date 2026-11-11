package com.ibmec.remomeurumo.service;

import java.io.IOException;
import java.util.List;

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
        Encontro encontro = encontroId == null ? null : encontroRepository.findById(encontroId)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Aula não encontrada."));
        byte[] conteudo;
        try {
            conteudo = arquivo.getBytes();
        } catch (IOException ex) {
            throw new RegraNegocioException("Não foi possível ler o arquivo. Tente enviar novamente.");
        }
        // o tipo vem do conteúdo (primeiros bytes), não do que o navegador diz
        String contentType = tipoReal(conteudo);
        if (contentType == null) {
            throw new RegraNegocioException("Envie um PDF ou uma foto (JPG, PNG, WEBP ou HEIC).");
        }
        TipoAnexo tipoFinal = tipo == null ? TipoAnexo.OUTRO : tipo;
        if (encontro != null) { // reenviar o atestado da mesma aula troca o anterior, em vez de acumular arquivos
            anexoRepository.deleteByParticipanteIdAndEncontroIdAndTipo(participanteId, encontroId, tipoFinal);
        }
        Anexo anexo = anexoRepository.save(new Anexo(participante, encontro, tipoFinal,
                comExtensao(nomeSeguro(arquivo.getOriginalFilename()), contentType), contentType, conteudo));
        historicoService.registrar(ModuloHistorico.PARTICIPANTES, "Participante", participante.getId(),
                participante.getNome(), AcaoHistorico.ARQUIVO,
                tipoFinal.getDescricao() + " anexado ao cadastro de " + participante.getNome()
                        + (encontro == null ? "." : " para a aula de " + encontro.getAtividade().getNome()
                                + " em " + encontro.getData() + "."));
        return anexo;
    }

    private static boolean comecaCom(byte[] b, int desde, String texto) {
        if (b.length < desde + texto.length()) {
            return false;
        }
        for (int i = 0; i < texto.length(); i++) {
            if (b[desde + i] != (byte) texto.charAt(i)) {
                return false;
            }
        }
        return true;
    }

    /** Reconhece PDF, JPG, PNG, WEBP e HEIC pelos primeiros bytes; qualquer outra coisa devolve null. */
    static String tipoReal(byte[] b) {
        if (comecaCom(b, 0, "%PDF")) {
            return "application/pdf";
        }
        if (b.length > 3 && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF) {
            return "image/jpeg";
        }
        if (b.length > 7 && b[0] == (byte) 0x89 && comecaCom(b, 1, "PNG")) {
            return "image/png";
        }
        if (comecaCom(b, 0, "RIFF") && comecaCom(b, 8, "WEBP")) {
            return "image/webp";
        }
        if (comecaCom(b, 4, "ftyp") && (comecaCom(b, 8, "heic") || comecaCom(b, 8, "heix")
                || comecaCom(b, 8, "mif1") || comecaCom(b, 8, "heif") || comecaCom(b, 8, "hevc"))) {
            return "image/heic";
        }
        return null;
    }

    private String comExtensao(String nome, String tipo) {
        String extensao = switch (tipo) {
            case "application/pdf" -> ".pdf";
            case "image/jpeg" -> ".jpg";
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".heic";
        };
        String semExtensao = nome.replaceFirst("\\.[A-Za-z0-9]{1,5}$", "");
        return semExtensao + extensao;
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
