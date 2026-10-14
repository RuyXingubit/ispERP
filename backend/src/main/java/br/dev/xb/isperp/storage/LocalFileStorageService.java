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

    private static final java.util.Set<String> BLOCKED_EXTENSIONS = java.util.Set.of(
            ".html", ".htm", ".xhtml", ".svg", ".js", ".mjs", ".cjs",
            ".sh", ".bash", ".zsh", ".exe", ".bat", ".cmd", ".ps1",
            ".jsp", ".jspx", ".php", ".py", ".rb", ".pl", ".jar", ".war", ".class"
    );

    private final Path rootLocation;

    public LocalFileStorageService(@Value("${app.storage.local.directory:./uploads}") String directory) {
        this.rootLocation = Paths.get(directory).toAbsolutePath().normalize();
        try {
            Files.createDirectories(this.rootLocation);
        } catch (Exception e) {
            log.error("Não foi possível inicializar o diretório de upload: {}", e.getMessage(), e);
        }
    }

    private Path resolveSafePath(String filename) {
        if (filename == null || filename.isBlank()
                || filename.contains("..")
                || filename.contains("/")
                || filename.contains("\\")) {
            throw new BusinessException("Tentativa de Path Traversal ou nome de arquivo inválido bloqueada: " + filename);
        }
        Path rootAbs = this.rootLocation.toAbsolutePath().normalize();
        Path resolved = rootAbs.resolve(filename).normalize();
        if (!resolved.startsWith(rootAbs)) {
            throw new BusinessException("Acesso fora do diretório de armazenamento bloqueado: " + filename);
        }
        return resolved;
    }

    @Override
    public String store(String originalFilename, String contentType, InputStream inputStream) {
        try {
            String safeOriginal = originalFilename != null ? originalFilename : "file";
            if (safeOriginal.contains("..")) {
                throw new BusinessException("Nome de arquivo inválido: " + safeOriginal);
            }
            String extension = "";
            int dotIndex = safeOriginal.lastIndexOf('.');
            if (dotIndex >= 0) {
                extension = safeOriginal.substring(dotIndex).toLowerCase();
            }
            if (BLOCKED_EXTENSIONS.contains(extension)) {
                throw new BusinessException("Tipo de arquivo bloqueado por política de segurança: " + extension);
            }
            String uniqueName = UuidCreatorUtils.generateUuidV7().toString() + extension;
            Path destinationFile = resolveSafePath(uniqueName);

            Files.copy(inputStream, destinationFile, StandardCopyOption.REPLACE_EXISTING);
            log.info("📁 [Storage] Arquivo salvo com sucesso: {}", uniqueName);
            return uniqueName;
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("Erro ao armazenar arquivo: {}", e.getMessage(), e);
            throw new BusinessException("Não foi possível armazenar o arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    public Resource recover(String storedFilename) {
        try {
            Path file = resolveSafePath(storedFilename);
            Resource resource = new FileSystemResource(file);
            if (resource.exists() || resource.isReadable()) {
                return resource;
            }
            throw new BusinessException("Arquivo não encontrado ou inacessível: " + storedFilename);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Erro ao recuperar arquivo: " + e.getMessage(), e);
        }
    }

    @Override
    public void delete(String storedFilename) {
        Path file = resolveSafePath(storedFilename);
        try {
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

