package br.dev.xb.isperp.service.backup;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.security.spec.KeySpec;
import java.util.Base64;

@Service
@Slf4j
public class BackupCryptoService {

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS5Padding";
    private static final String KEY_DERIVATION_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATION_COUNT = 65536;
    private static final int KEY_LENGTH_BITS = 256;
    private static final int SALT_LENGTH_BYTES = 16;
    private static final int IV_LENGTH_BYTES = 16;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String systemInternalSecret;

    public BackupCryptoService(@Value("${app.backup.system-secret:isperp-default-vault-secure-key-2026}") String systemSecret) {
        this.systemInternalSecret = systemSecret;
    }

    /**
     * Gera uma nova Chave Mestra forte de 256 bits em Base64 formatado.
     */
    public String generateMasterKey() {
        byte[] keyBytes = new byte[32];
        secureRandom.nextBytes(keyBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(keyBytes);
    }

    /**
     * Calcula hash SHA-256 de uma chave ou dado em String.
     */
    public String calculateSha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(hash);
        } catch (Exception e) {
            throw new IllegalStateException("Erro ao calcular SHA-256", e);
        }
    }

    /**
     * Envolve uma OutputStream de destino com criptografia AES-256 em pipeline contínuo.
     * Grava Salt (16B) e IV (16B) nos primeiros 32 bytes do stream.
     */
    public CipherOutputStream createCipherOutputStream(OutputStream targetStream, String masterKey) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            secureRandom.nextBytes(salt);
            targetStream.write(salt);

            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);
            targetStream.write(iv);

            SecretKey secretKey = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            return new CipherOutputStream(targetStream, cipher);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar CipherOutputStream para backup", e);
        }
    }

    /**
     * Envolve uma InputStream criptografada para leitura e descriptografia em streaming.
     * Lê Salt (16B) e IV (16B) do cabeçalho do stream.
     */
    public CipherInputStream createCipherInputStream(InputStream sourceStream, String masterKey) {
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            int saltRead = sourceStream.readNBytes(salt, 0, SALT_LENGTH_BYTES);
            if (saltRead != SALT_LENGTH_BYTES) {
                throw new IllegalArgumentException("Arquivo de backup inválido: cabeçalho Salt incompleto.");
            }

            byte[] iv = new byte[IV_LENGTH_BYTES];
            int ivRead = sourceStream.readNBytes(iv, 0, IV_LENGTH_BYTES);
            if (ivRead != IV_LENGTH_BYTES) {
                throw new IllegalArgumentException("Arquivo de backup inválido: cabeçalho IV incompleto.");
            }

            SecretKey secretKey = deriveKey(masterKey, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            return new CipherInputStream(sourceStream, cipher);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao inicializar CipherInputStream para descriptografia", e);
        }
    }

    /**
     * Criptografa strings sensíveis (ex: senhas/chaves de storage) para repouso no banco.
     */
    public String encryptSystemSecret(String plainText) {
        if (plainText == null || plainText.isBlank()) return plainText;
        try {
            byte[] salt = new byte[SALT_LENGTH_BYTES];
            secureRandom.nextBytes(salt);
            byte[] iv = new byte[IV_LENGTH_BYTES];
            secureRandom.nextBytes(iv);

            SecretKey secretKey = deriveKey(systemInternalSecret, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.ENCRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] cipherBytes = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            byte[] result = new byte[salt.length + iv.length + cipherBytes.length];
            System.arraycopy(salt, 0, result, 0, salt.length);
            System.arraycopy(iv, 0, result, salt.length, iv.length);
            System.arraycopy(cipherBytes, 0, result, salt.length + iv.length, cipherBytes.length);

            return Base64.getEncoder().encodeToString(result);
        } catch (Exception e) {
            throw new IllegalStateException("Falha ao cifrar credencial", e);
        }
    }

    /**
     * Descriptografa strings sensíveis gravadas no banco de dados.
     */
    public String decryptSystemSecret(String cipherTextBase64) {
        if (cipherTextBase64 == null || cipherTextBase64.isBlank()) return cipherTextBase64;
        try {
            byte[] data = Base64.getDecoder().decode(cipherTextBase64);
            if (data.length < SALT_LENGTH_BYTES + IV_LENGTH_BYTES) {
                return cipherTextBase64;
            }

            byte[] salt = new byte[SALT_LENGTH_BYTES];
            byte[] iv = new byte[IV_LENGTH_BYTES];
            System.arraycopy(data, 0, salt, 0, SALT_LENGTH_BYTES);
            System.arraycopy(data, SALT_LENGTH_BYTES, iv, 0, IV_LENGTH_BYTES);

            int cipherLength = data.length - SALT_LENGTH_BYTES - IV_LENGTH_BYTES;
            byte[] cipherBytes = new byte[cipherLength];
            System.arraycopy(data, SALT_LENGTH_BYTES + IV_LENGTH_BYTES, cipherBytes, 0, cipherLength);

            SecretKey secretKey = deriveKey(systemInternalSecret, salt);
            Cipher cipher = Cipher.getInstance(CIPHER_ALGORITHM);
            cipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            byte[] plainBytes = cipher.doFinal(cipherBytes);
            return new String(plainBytes, StandardCharsets.UTF_8);
        } catch (Exception e) {
            log.error("Erro ao decifrar credencial, retornando original: {}", e.getMessage());
            return cipherTextBase64;
        }
    }

    private SecretKey deriveKey(String password, byte[] salt) throws Exception {
        KeySpec spec = new PBEKeySpec(password.toCharArray(), salt, ITERATION_COUNT, KEY_LENGTH_BITS);
        SecretKeyFactory factory = SecretKeyFactory.getInstance(KEY_DERIVATION_ALGORITHM);
        byte[] keyBytes = factory.generateSecret(spec).getEncoded();
        return new SecretKeySpec(keyBytes, "AES");
    }

    public static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }
}
