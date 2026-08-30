package br.dev.xb.isperp.storage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class FileStorageServiceTest {

    @Test
    @DisplayName("Deve armazenar, recuperar e deletar arquivo localmente com sucesso")
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
    }
}
