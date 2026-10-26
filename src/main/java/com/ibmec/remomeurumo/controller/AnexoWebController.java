package com.ibmec.remomeurumo.controller;

import java.nio.charset.StandardCharsets;

import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.ibmec.remomeurumo.exception.RegraNegocioException;
import com.ibmec.remomeurumo.model.Anexo;
import com.ibmec.remomeurumo.model.TipoAnexo;
import com.ibmec.remomeurumo.service.AnexoService;

@Controller
public class AnexoWebController {

    private final AnexoService anexoService;

    public AnexoWebController(AnexoService anexoService) {
        this.anexoService = anexoService;
    }

    @PostMapping("/participantes/{id}/anexos")
    public String anexar(@PathVariable Long id,
            @RequestParam(required = false) TipoAnexo tipo,
            @RequestParam(required = false) MultipartFile arquivo,
            RedirectAttributes redirectAttributes) {
        try {
            Anexo anexo = anexoService.anexar(id, tipo, arquivo, null);
            redirectAttributes.addFlashAttribute("sucesso", anexo.getTipo().getDescricao() + " anexado: "
                    + anexo.getNomeArquivo() + ".");
        } catch (RegraNegocioException ex) {
            redirectAttributes.addFlashAttribute("erro", ex.getMessage());
        }
        return "redirect:/participantes/" + id + "#arquivos-titulo";
    }

    @GetMapping("/anexos/{id}")
    public ResponseEntity<byte[]> abrir(@PathVariable Long id) {
        Anexo anexo = anexoService.obter(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(anexo.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(anexo.getNomeArquivo(), StandardCharsets.UTF_8).build().toString())
                .header(HttpHeaders.CACHE_CONTROL, "no-store")
                .header("X-Content-Type-Options", "nosniff")
                .body(anexo.getConteudo());
    }
}
