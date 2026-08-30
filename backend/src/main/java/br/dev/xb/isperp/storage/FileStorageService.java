package br.dev.xb.isperp.storage;

import org.springframework.core.io.Resource;

import java.io.InputStream;

public interface FileStorageService {

    /**
     * Armazena um arquivo e retorna o nome final gerado com UUID/hash.
     */
    String store(String originalFilename, String contentType, InputStream inputStream);

    /**
     * Recupera um arquivo armazenado como Resource do Spring.
     */
    Resource recover(String storedFilename);

    /**
     * Remove um arquivo armazenado.
     */
    void delete(String storedFilename);

    /**
     * Retorna a URL pública ou caminho relativo do arquivo.
     */
    String getFileUrl(String storedFilename);
}
