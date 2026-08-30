package br.dev.xb.isperp.storage;

import br.dev.xb.isperp.exception.BusinessException;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Service
@Slf4j
public class LocalFileStorageService implements FileStorageService {

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.storage.local.directory:./uploads}") String directory) {
        this.rootLocation = Paths.get(directory);
        try {
            Files.createDirectories(this.rootLocation);
        } catch (Exception e) {
            log.error("Não foi possível inicializar o diretório de upload: {}", e.getMessage(), e);
        }
    }

    @Override
    public String store(String originalFilename, String contentType, InputStream inputStream) {
        try {
            String extension = "";
            int dotIndex = originalFilename.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = originalFilename.substring(dotIndex);
            }
            String uniqueName = UuidCreatorUtils.generateUuidV7().toString() + extension;
            Path destinationFile = this.rootLocation.resolve(uniqueName).normalize().toAbsolutePath();

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("📁 [Storage] Arquivo salvo com sucesso: {}", uniqueName);
            return uniqueName;
        } catch (Exception e) {
            log.error("Erro ao armazenar arquivo: {}", e.getMessage(), e);
            throw new BusinessException("Não foi possível armazenar o arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource recover(String storedFilename) {
        try {
            Path file = this.rootLocation.resolve(storedFilename).normalize();
            Resource resource = new FileSystemResource(file);
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            throw new BusinessException("Arquivo não encontrado ou inacessível: " + storedFilename);
        } catch (Exception e) {
            throw new BusinessException("Erro ao recuperar arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storedFilename) {
        try {
            Path file = this.rootLocation.resolve(storedFilename).normalize();
            Files.deleteIfExists(file);
            log.info("🗑️ [Storage] Arquivo removido com sucesso: {}", storedFilename);
        } catch (Exception e) {
            log.warn("Falha ao remover arquivo {}: {}", storedFilename, e.getMessage());
        }
    }

    @Override
    public String getFileUrl(String storedFilename) {
        return "/api/files/" + storedFilename;
    }
}
