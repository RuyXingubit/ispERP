package br.dev.xb.isperp.service.backup;

import br.dev.xb.isperp.backup.BackupStatus;
import br.dev.xb.isperp.backup.BackupTriggerType;
import br.dev.xb.isperp.backup.CompressionAlgorithm;
import br.dev.xb.isperp.backup.SecurityMode;
import br.dev.xb.isperp.entity.backup.BackupExecutionLog;
import br.dev.xb.isperp.entity.backup.BackupPolicy;
import br.dev.xb.isperp.repository.backup.BackupDestinationRepository;
import br.dev.xb.isperp.repository.backup.BackupExecutionLogRepository;
import br.dev.xb.isperp.repository.backup.BackupPolicyRepository;
import com.github.f4b6a3.uuid.UuidCreator;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.Collections;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("null")
class BackupStreamingPipelineServiceTest {

    @Mock
    private BackupPolicyRepository policyRepository;

    @Mock
    private BackupDestinationRepository destinationRepository;

    @Mock
    private BackupExecutionLogRepository logRepository;

    @Mock
    private StorageProviderService storageProviderService;

    @Mock
    private DataSource dataSource;

    @Mock
    private Connection connection;

    @Mock
    private DatabaseMetaData metaData;

    @Mock
    private ResultSet resultSet;

    private final BackupCryptoService cryptoService = new BackupCryptoService("test-system-secret-key-12345");

    @Test
    @DisplayName("Deve acionar fallback JDBC e gerar dump com originalSizeBytes > 0 quando pg_dump não estiver disponível ou falhar")
    void shouldFallbackToJdbcDumpWhenPgDumpIsUnavailableOrReturnsZeroBytes() throws Exception {
        BackupStreamingPipelineService pipelineService = new BackupStreamingPipelineService(
                policyRepository,
                destinationRepository,
                logRepository,
                cryptoService,
                storageProviderService,
                dataSource
        );

        String masterKey = cryptoService.generateMasterKey();
        BackupPolicy policy = BackupPolicy.builder()
                .id(UuidCreator.getTimeOrderedEpoch())
                .securityMode(SecurityMode.MANAGED_RESCUE)
                .masterKeyHash(cryptoService.calculateSha256(masterKey))
                .encryptedMasterKey(cryptoService.encryptSystemSecret(masterKey))
                .compressionAlgorithm(CompressionAlgorithm.ZSTD)
                .retentionDays(30)
                .isActive(true)
                .build();

        when(policyRepository.findFirstByIsActiveTrueOrderByCreatedAtDesc()).thenReturn(Optional.of(policy));
        when(destinationRepository.findByIsActiveTrue()).thenReturn(Collections.emptyList());
        when(logRepository.save(any(BackupExecutionLog.class))).thenAnswer(inv -> inv.getArgument(0));

        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getTables(any(), any(), any(), any())).thenReturn(resultSet);
        when(resultSet.next()).thenReturn(true, false);
        when(resultSet.getString("TABLE_NAME")).thenReturn("users");

        BackupExecutionLog logEntry = pipelineService.executeBackup(BackupTriggerType.MANUAL, null);

        assertThat(logEntry.getStatus()).isEqualTo(BackupStatus.SUCCESS);
        assertThat(logEntry.getOriginalSizeBytes()).isGreaterThan(0L);
        assertThat(logEntry.getCompressedSizeBytes()).isGreaterThan(0L);
        assertThat(logEntry.getSha256Hash()).hasSize(64);
    }
}
