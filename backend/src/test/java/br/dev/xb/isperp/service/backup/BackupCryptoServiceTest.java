package br.dev.xb.isperp.service.backup;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;

class BackupCryptoServiceTest {

    private final BackupCryptoService cryptoService = new BackupCryptoService("test-system-secret-key-12345");

    @Test
    @DisplayName("Deve gerar Chave Mestra forte de 256 bits e calcular SHA-256")
    void shouldGenerateMasterKeyAndCalculateSha256() {
        String key = cryptoService.generateMasterKey();
        assertThat(key).isNotBlank();
        assertThat(key.length()).isGreaterThanOrEqualTo(40);

        String hash = cryptoService.calculateSha256(key);
        assertThat(hash).hasSize(64);
    }

    @Test
    @DisplayName("Deve criptografar e descriptografar stream com AES-256 com fidelidade de bits 100%")
    void shouldEncryptAndDecryptStreamWithAes256() throws Exception {
        String masterKey = cryptoService.generateMasterKey();
        String originalText = "POSTGRESQL 17 BACKUP DUMP - DADOS CONFIDENCIAIS DO CLIENTE ISP - CPF 123.456.789-00";
        byte[] originalBytes = originalText.getBytes(StandardCharsets.UTF_8);

        // 1. Criptografia em Stream
        ByteArrayOutputStream encryptedOut = new ByteArrayOutputStream();
        try (OutputStream cipherOut = cryptoService.createCipherOutputStream(encryptedOut, masterKey)) {
            cipherOut.write(originalBytes);
            cipherOut.flush();
        }

        byte[] cipherBytes = encryptedOut.toByteArray();
        // O stream criptografado deve ser maior que os dados originais (Salt 16B + IV 16B + padding)
        assertThat(cipherBytes.length).isGreaterThan(originalBytes.length);
        assertThat(new String(cipherBytes, StandardCharsets.UTF_8)).isNotEqualTo(originalText);

        // 2. Descriptografia em Stream
        ByteArrayInputStream cipherIn = new ByteArrayInputStream(cipherBytes);
        ByteArrayOutputStream decryptedOut = new ByteArrayOutputStream();
        try (InputStream decryptIn = cryptoService.createCipherInputStream(cipherIn, masterKey)) {
            byte[] buffer = new byte[1024];
            int read;
            while ((read = decryptIn.read(buffer)) != -1) {
                decryptedOut.write(buffer, 0, read);
            }
        }

        String decryptedText = decryptedOut.toString(StandardCharsets.UTF_8);
        assertThat(decryptedText).isEqualTo(originalText);
    }

    @Test
    @DisplayName("Deve cifrar e decifrar segredos internos do sistema")
    void shouldEncryptAndDecryptSystemSecrets() {
        String secret = "minha-chave-s3-super-secreta-aws-secret-key-xyz";
        String encrypted = cryptoService.encryptSystemSecret(secret);

        assertThat(encrypted).isNotEqualTo(secret);

        String decrypted = cryptoService.decryptSystemSecret(encrypted);
        assertThat(decrypted).isEqualTo(secret);
    }
}
