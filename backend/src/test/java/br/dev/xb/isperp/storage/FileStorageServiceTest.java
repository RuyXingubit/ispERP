package br.dev.xb.isperp.storage;

import br.dev.xb.isperp.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @Test
    @DisplayName("Regressão: Deve armazenar, recuperar e deletar arquivos operacionais legítimos (.jpg, .pdf, .xml)")
    void shouldStoreRecoverAndDeleteFile(@TempDir Path tempDir) throws IOException {
        LocalFileStorageService storageService = new LocalFileStorageService(tempDir.toString());

        String content = "Conteúdo de teste para foto de instalação";
        ByteArrayInputStream inputStream = new ByteArrayInputStream(content.getBytes(StandardCharsets.UTF_8));

        String storedFilename = storageService.store("foto_instalacao.jpg", "image/jpeg", inputStream);
        assertThat(storedFilename).endsWith(".jpg");

        Resource recovered = storageService.recover(storedFilename);
        assertThat(recovered.exists()).isTrue();
        assertThat(recovered.getContentAsString(StandardCharsets.UTF_8)).isEqualTo(content);

        String fileUrl = storageService.getFileUrl(storedFilename);
        assertThat(fileUrl).isEqualTo("/api/files/" + storedFilename);

        storageService.delete(storedFilename);
        assertThat(tempDir.resolve(storedFilename).toFile().exists()).isFalse();

        // Verifica também PDF e XML fiscais
        String pdfStored = storageService.store("fatura.pdf", "application/pdf",
                new ByteArrayInputStream("PDF".getBytes(StandardCharsets.UTF_8)));
        assertThat(pdfStored).endsWith(".pdf");
    }

    @Test
    @DisplayName("Segurança: Deve bloquear tentativas de Path Traversal (../) em recover() e delete()")
    void shouldBlockPathTraversalOnRecoverAndDelete(@TempDir Path tempDir) throws IOException {
        Path parentDir = tempDir.getParent();
        Path secretOutsideFile = parentDir.resolve("segredo_fora_storage_" + System.nanoTime() + ".txt");
        Files.writeString(secretOutsideFile, "CHAVE_MESTRA_SECRETA");

        try {
            Path storageSubDir = tempDir.resolve("uploads");
            LocalFileStorageService storageService = new LocalFileStorageService(storageSubDir.toString());

            String traversalAttempt = "../../" + secretOutsideFile.getFileName().toString();

            assertThatThrownBy(() -> storageService.recover(traversalAttempt))
                    .isInstanceOf(BusinessException.class);

            assertThatThrownBy(() -> storageService.delete(traversalAttempt))
                    .isInstanceOf(BusinessException.class);

            // Garante que o arquivo externo NÃO foi deletado
            assertThat(Files.exists(secretOutsideFile)).isTrue();
        } finally {
            Files.deleteIfExists(secretOutsideFile);
        }
    }

    @Test
    @DisplayName("Segurança: Deve bloquear upload de extensões ativas perigosas (.html, .svg, .js, .sh, .exe, .jsp) contra Stored XSS e RCE")
    void shouldBlockDangerousExecutableAndXssExtensions(@TempDir Path tempDir) {
        LocalFileStorageService storageService = new LocalFileStorageService(tempDir.toString());
        byte[] maliciousPayload = "<script>alert(document.cookie)</script>".getBytes(StandardCharsets.UTF_8);

        for (String dangerousName : new String[]{"exploit.html", "xss.svg", "malware.js", "shell.sh", "backdoor.jsp", "trojan.exe"}) {
            assertThatThrownBy(() -> storageService.store(
                    dangerousName,
                    "application/octet-stream",
                    new ByteArrayInputStream(maliciousPayload)
            )).isInstanceOf(BusinessException.class);
        }
    }
}

