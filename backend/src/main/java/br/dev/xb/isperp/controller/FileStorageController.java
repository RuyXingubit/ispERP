package br.dev.xb.isperp.controller;

import br.dev.xb.isperp.storage.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/files")
@RequiredArgsConstructor
@Tag(name = "Storage de Arquivos", description = "Endpoints para upload e recuperação de fotos de instalação, comprovantes e documentos fiscais")
@SuppressWarnings("null")
public class FileStorageController {

    private final FileStorageService fileStorageService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Realiza o upload de um arquivo para o storage")
    public ResponseEntity<String> upload(@RequestParam("file") MultipartFile file) throws IOException {
        String storedName = fileStorageService.store(
                file.getOriginalFilename() != null ? file.getOriginalFilename() : "file",
                file.getContentType() != null ? file.getContentType() : MediaType.APPLICATION_OCTET_STREAM_VALUE,
                file.getInputStream()
        );
        return ResponseEntity.ok(fileStorageService.getFileUrl(storedName));
    }

    @GetMapping("/{filename}")
    @Operation(summary = "Recupera um arquivo armazenado")
    public ResponseEntity<Resource> download(@PathVariable String filename) {
        Resource resource = fileStorageService.recover(filename);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + filename + "\"")
                .body(resource);
    }
}
