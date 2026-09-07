package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.api.contract.FileStorageApi;
import br.dev.xb.isperp.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping({"/files", "/api/files"})
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Storage de Arquivos", description = "Endpoints para upload e recuperação de fotos de instalação, comprovantes e documentos fiscais")
@SuppressWarnings("null")
public class FileStorageController implements FileStorageApi {

    private final FileStorageService fileStorageService;

    @Override
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Realiza o upload de um arquivo para o storage")
    public ResponseEntity<String> uploadFile(@RequestPart("file") MultipartFile file) {
        try {
            String storedName = fileStorageService.store(
                    file.getOriginalFilename() != null ? file.getOriginalFilename() : "file",
                    file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                    file.getInputStream()
            );
            return ResponseEntity.ok(fileStorageService.getFileUrl(storedName));
        } catch (IOException e) {
            log.error("Erro ao armazenar arquivo: {}", e.getMessage());
            throw new RuntimeException("Falha no upload do arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    @GetMapping("/{filename}")
    @Operation(summary = "Recupera um arquivo armazenado")
    public ResponseEntity<Resource> downloadFile(@PathVariable String filename) {
        Resource resource = fileStorageService.recover(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
