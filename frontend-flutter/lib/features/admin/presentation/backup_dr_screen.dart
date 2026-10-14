import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../data/backup_dr_repository.dart';

/// Tela de Governança de Backup Criptografado AES-256-GCM & Disaster Recovery (`/admin/backup`).
class BackupDrScreen extends ConsumerStatefulWidget {
  const BackupDrScreen({super.key});

  @override
  ConsumerState<BackupDrScreen> createState() => _BackupDrScreenState();
}

class _BackupDrScreenState extends ConsumerState<BackupDrScreen> {
  bool _isExecutingBackup = false;
  bool _isLoadingKit = false;
  final Map<String, BackupDestinationTestResult> _testResults = {};
  final Set<String> _testingDestinationIds = {};

  String _formatBytes(int? bytes) {
    if (bytes == null || bytes <= 0) return '0 B';
    if (bytes < 1024) return '$bytes B';
    if (bytes < 1024 * 1024) {
      return '${(bytes / 1024).toStringAsFixed(1)} KB';
    }
    return '${(bytes / (1024 * 1024)).toStringAsFixed(2)} MB';
  }

  String _formatDate(DateTime? dt) {
    if (dt == null) return 'Nunca executado';
    return DateFormat('dd/MM/yyyy HH:mm', 'pt_BR').format(dt.toLocal());
  }

  @override
  Widget build(BuildContext context) {
    final overviewAsync = ref.watch(backupOverviewProvider);
    final destinationsAsync = ref.watch(backupDestinationsProvider);
    final historyAsync = ref.watch(backupHistoryProvider);

    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Wrap(
              spacing: 16,
              runSpacing: 12,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Backup Criptografado & Disaster Recovery',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Esteira AES-256-GCM com ZSTD/GZIP, destinos multi-cloud S3/SFTP, verificação Dry-Run e Kit de Resgate',
                      style: TextStyle(
                        color: AppTheme.textSecondary,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
                Wrap(
                  spacing: 10,
                  runSpacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () {
                        ref.invalidate(backupOverviewProvider);
                        ref.invalidate(backupDestinationsProvider);
                        ref.invalidate(backupHistoryProvider);
                      },
                      icon: const Icon(Icons.refresh, size: 18),
                      label: const Text('Atualizar'),
                    ),
                    OutlinedButton.icon(
                      onPressed: _isLoadingKit ? null : _openEmergencyKitModal,
                      icon: const Icon(
                        Icons.medical_services_outlined,
                        size: 18,
                      ),
                      label: Text(
                        _isLoadingKit ? 'Gerando Kit...' : 'Kit de Resgate',
                      ),
                    ),
                    OutlinedButton.icon(
                      onPressed: () => _showConfigurePolicyDialog(
                        overviewAsync.asData?.value,
                      ),
                      icon: const Icon(Icons.enhanced_encryption, size: 18),
                      label: const Text('Política AES-256'),
                    ),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryBlue,
                        foregroundColor: Colors.white,
                      ),
                      onPressed: _isExecutingBackup
                          ? null
                          : () => _triggerManualBackup(
                              overviewAsync.asData?.value,
                            ),
                      icon: _isExecutingBackup
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(
                                strokeWidth: 2,
                                color: Colors.white,
                              ),
                            )
                          : const Icon(Icons.cloud_upload_outlined, size: 18),
                      label: Text(
                        _isExecutingBackup
                            ? 'Executando...'
                            : 'Executar Backup Agora',
                      ),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 24),

            // 1. Visão Geral (GET /financial/backup/overview)
            overviewAsync.when(
              loading: () => const Center(
                child: Padding(
                  padding: EdgeInsets.all(24.0),
                  child: CircularProgressIndicator(),
                ),
              ),
              error: (err, _) => _buildErrorBanner(
                'Erro ao carregar status de Disaster Recovery: $err',
                () => ref.invalidate(backupOverviewProvider),
              ),
              data: _buildOverviewCards,
            ),
            const SizedBox(height: 28),

            // 2. Destinos de Armazenamento (/financial/backup/destinations)
            Card(
              color: AppTheme.darkSurface,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: const BorderSide(color: AppTheme.darkCard),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 12,
                      runSpacing: 8,
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: const [
                            Text(
                              'Destinos de Armazenamento Remoto & Local (/destinations)',
                              style: TextStyle(
                                fontSize: 16,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                            SizedBox(height: 2),
                            Text(
                              'Cofres S3, SFTP ou Volumes Locais para replicação criptografada dos dumps',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppTheme.textSecondary,
                              ),
                            ),
                          ],
                        ),
                        ElevatedButton.icon(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.primaryIndigo,
                            foregroundColor: Colors.white,
                          ),
                          onPressed: _showAddDestinationDialog,
                          icon: const Icon(Icons.add_to_drive, size: 18),
                          label: const Text('Adicionar Destino'),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    destinationsAsync.when(
                      loading: () => const Center(
                        child: Padding(
                          padding: EdgeInsets.all(24.0),
                          child: CircularProgressIndicator(),
                        ),
                      ),
                      error: (err, _) => _buildErrorBanner(
                        'Erro ao listar destinos: $err',
                        () => ref.invalidate(backupDestinationsProvider),
                      ),
                      data: (destinations) {
                        if (destinations.isEmpty) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 16.0),
                            child: Text(
                              'Nenhum destino de backup cadastrado. Adicione um bucket S3, servidor SFTP ou volume para iniciar.',
                              style: TextStyle(
                                color: AppTheme.textSecondary,
                                fontSize: 13,
                              ),
                            ),
                          );
                        }
                        return Column(
                          children: destinations
                              .map(_buildDestinationTile)
                              .toList(),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
            const SizedBox(height: 28),

            // 3. Histórico de Execuções com SHA-256 (/financial/backup/history)
            Card(
              color: AppTheme.darkSurface,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: const BorderSide(color: AppTheme.darkCard),
              ),
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Histórico de Execuções & Auditoria SHA-256 (/history)',
                      style: TextStyle(
                        fontSize: 16,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    const SizedBox(height: 4),
                    const Text(
                      'Registro imutável de dumps criptografados, taxa de compressão e validação de integridade',
                      style: TextStyle(
                        fontSize: 12,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 16),
                    historyAsync.when(
                      loading: () => const Center(
                        child: Padding(
                          padding: EdgeInsets.all(24.0),
                          child: CircularProgressIndicator(),
                        ),
                      ),
                      error: (err, _) => _buildErrorBanner(
                        'Erro ao carregar histórico de backups: $err',
                        () => ref.invalidate(backupHistoryProvider),
                      ),
                      data: (logs) {
                        if (logs.isEmpty) {
                          return const Padding(
                            padding: EdgeInsets.symmetric(vertical: 16.0),
                            child: Text(
                              'Nenhuma execução de backup registrada até o momento.',
                              style: TextStyle(
                                color: AppTheme.textSecondary,
                                fontSize: 13,
                              ),
                            ),
                          );
                        }
                        return Column(
                          children: logs.map(_buildHistoryRow).toList(),
                        );
                      },
                    ),
                  ],
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildOverviewCards(BackupOverviewModel ov) {
    final statusOk =
        ov.lastBackupStatus == 'SUCCESS' ||
        ov.lastBackupStatus == 'VERIFIED_OK';

    return Wrap(
      spacing: 16,
      runSpacing: 16,
      children: [
        _buildStatCard(
          title: 'Modo de Criptografia',
          value: ov.securityMode == 'ZERO_KNOWLEDGE'
              ? 'Zero-Knowledge'
              : 'Managed Rescue',
          subtitle: ov.hasActivePolicy
              ? 'Política ativa • Cron: ${ov.cronExpression}'
              : 'Política pendente de ativação',
          icon: Icons.enhanced_encryption_outlined,
          color: ov.hasActivePolicy
              ? AppTheme.primaryBlue
              : AppTheme.accentWarning,
        ),
        _buildStatCard(
          title: 'Último Backup',
          value: ov.lastBackupStatus ?? 'SEM EXECUÇÃO',
          subtitle:
              '${_formatDate(ov.lastBackupAt)} • ${_formatBytes(ov.lastBackupSizeBytes)}',
          icon: Icons.backup_outlined,
          color: statusOk ? AppTheme.accentGreen : AppTheme.textMuted,
        ),
        _buildStatCard(
          title: 'Retenção & Cofres',
          value: '${ov.activeDestinationsCount} Destino(s)',
          subtitle:
              'Retenção: ${ov.retentionDays} dias • Total: ${ov.totalBackupsCount} dumps',
          icon: Icons.storage_outlined,
          color: AppTheme.primaryIndigo,
        ),
        _buildStatCard(
          title: 'Prontidão de Resgate',
          value: ov.rescueKitDownloaded ? 'Kit Baixado' : 'Kit Pendente',
          subtitle: ov.isDryRunVerified
              ? 'Dry-Run verificado com sucesso'
              : 'Aguardando verificação Dry-Run',
          icon: Icons.verified_user_outlined,
          color: ov.rescueKitDownloaded
              ? AppTheme.accentGreen
              : AppTheme.accentWarning,
        ),
      ],
    );
  }

  Widget _buildStatCard({
    required String title,
    required String value,
    required String subtitle,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      width: 260,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              Expanded(
                child: Text(
                  title,
                  style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.textSecondary,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Icon(icon, color: color, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 6),
          Text(
            subtitle,
            style: TextStyle(
              fontSize: 11,
              color: color,
              fontWeight: FontWeight.w500,
            ),
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
        ],
      ),
    );
  }

  Widget _buildDestinationTile(BackupDestinationModel dest) {
    final testRes = _testResults[dest.id];
    final isTesting = _testingDestinationIds.contains(dest.id);

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 12,
            runSpacing: 8,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    dest.isPrimary ? Icons.star_rounded : Icons.cloud_outlined,
                    color: dest.isPrimary
                        ? AppTheme.accentWarning
                        : AppTheme.primaryBlue,
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      '${dest.name} (${dest.storageType})',
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 14,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Wrap(
                spacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: isTesting
                        ? null
                        : () => _testDestination(dest.id),
                    icon: const Icon(Icons.network_check, size: 16),
                    label: Text(
                      isTesting ? 'Testando...' : 'Testar Destino',
                    ),
                  ),
                  IconButton(
                    tooltip: 'Remover Destino',
                    icon: const Icon(
                      Icons.delete_outline,
                      color: AppTheme.accentError,
                      size: 20,
                    ),
                    onPressed: () async {
                      try {
                        await ref
                            .read(backupDrRepositoryProvider)
                            .deleteDestination(dest.id);
                        ref.invalidate(backupDestinationsProvider);
                        ref.invalidate(backupOverviewProvider);
                      } catch (e) {
                        if (mounted) {
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(
                              content: Text('Erro ao remover destino: $e'),
                              backgroundColor: AppTheme.accentError,
                            ),
                          );
                        }
                      }
                    },
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'Bucket/Path: ${dest.bucketName ?? dest.pathPrefix ?? "Padrão"} • Endpoint: ${dest.endpointUrl ?? "Local"}',
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
          if (testRes != null) ...[
            const SizedBox(height: 8),
            Text(
              '${testRes.success ? "OK" : "FALHA"}: ${testRes.message} (${testRes.latencyMs} ms)',
              style: TextStyle(
                fontSize: 12,
                fontWeight: FontWeight.w600,
                color: testRes.success
                    ? AppTheme.accentGreen
                    : AppTheme.accentError,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildHistoryRow(BackupExecutionLogModel log) {
    final isOk = log.status == 'SUCCESS' || log.status == 'VERIFIED_OK';

    return Container(
      margin: const EdgeInsets.only(bottom: 10),
      padding: const EdgeInsets.all(14),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Wrap(
            spacing: 12,
            runSpacing: 6,
            alignment: WrapAlignment.spaceBetween,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    isOk ? Icons.check_circle_outline : Icons.warning_amber,
                    size: 18,
                    color: isOk ? AppTheme.accentGreen : AppTheme.accentWarning,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Text(
                      log.fileName.isNotEmpty ? log.fileName : log.id,
                      style: const TextStyle(
                        fontWeight: FontWeight.bold,
                        fontSize: 13,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                ],
              ),
              Text(
                '${log.status} • ${log.triggerType} • ${_formatDate(log.startedAt)}',
                style: const TextStyle(
                  fontSize: 11,
                  color: AppTheme.textSecondary,
                ),
              ),
            ],
          ),
          const SizedBox(height: 6),
          Text(
            'Tamanho comprimido: ${_formatBytes(log.compressedSizeBytes)} • Duração: ${log.durationSeconds ?? 0}s • SHA-256: ${log.sha256Hash ?? "N/A"}',
            style: const TextStyle(
              fontSize: 11,
              fontFamily: 'monospace',
              color: AppTheme.textMuted,
            ),
            overflow: TextOverflow.ellipsis,
          ),
          if (log.errorMessage != null && log.errorMessage!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              'Erro: ${log.errorMessage}',
              style: const TextStyle(
                fontSize: 11,
                color: AppTheme.accentError,
              ),
            ),
          ],
        ],
      ),
    );
  }

  Future<void> _testDestination(String id) async {
    setState(() => _testingDestinationIds.add(id));
    try {
      final res = await ref
          .read(backupDrRepositoryProvider)
          .testDestination(id);
      if (mounted) {
        setState(() {
          _testResults[id] = res;
          _testingDestinationIds.remove(id);
        });
      }
    } catch (e) {
      if (mounted) {
        setState(() {
          _testResults[id] = BackupDestinationTestResult(
            success: false,
            message: 'Falha ao testar: $e',
            latencyMs: 0,
          );
          _testingDestinationIds.remove(id);
        });
      }
    }
  }

  Future<void> _showConfigurePolicyDialog(BackupOverviewModel? current) async {
    final formKey = GlobalKey<FormState>();
    String securityMode = current?.securityMode ?? 'MANAGED_RESCUE';
    String compression = 'ZSTD';
    final cronCtrl = TextEditingController(
      text: current?.cronExpression ?? '0 0 3 * * *',
    );
    final retentionCtrl = TextEditingController(
      text: '${current?.retentionDays ?? 30}',
    );
    final customKeyCtrl = TextEditingController();
    bool autoDryRun = true;
    bool zeroKnowledgeConfirmed = false;
    bool isSaving = false;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: const Text(
                'Configurar Política de Backup AES-256-GCM (/policies)',
              ),
              content: SizedBox(
                width: 520,
                child: SingleChildScrollView(
                  child: Form(
                    key: formKey,
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        DropdownButtonFormField<String>(
                          initialValue: securityMode,
                          decoration: const InputDecoration(
                            labelText: 'Modo de Custódia da Master Key *',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'MANAGED_RESCUE',
                              child: Text(
                                'MANAGED_RESCUE (Chave gerada com Kit de Resgate)',
                              ),
                            ),
                            DropdownMenuItem(
                              value: 'ZERO_KNOWLEDGE',
                              child: Text(
                                'ZERO_KNOWLEDGE (Chave exclusiva do Administrador)',
                              ),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) {
                              setModalState(() => securityMode = v);
                            }
                          },
                        ),
                        const SizedBox(height: 12),
                        if (securityMode == 'ZERO_KNOWLEDGE') ...[
                          TextFormField(
                            controller: customKeyCtrl,
                            obscureText: true,
                            decoration: const InputDecoration(
                              labelText:
                                  'Master Key Personalizada (Mínimo 16 caracteres) *',
                            ),
                            validator: (v) => (v == null || v.trim().length < 16)
                                ? 'Informe uma chave forte (mín. 16 caracteres)'
                                : null,
                          ),
                          const SizedBox(height: 8),
                          CheckboxListTile(
                            contentPadding: EdgeInsets.zero,
                            value: zeroKnowledgeConfirmed,
                            onChanged: (v) => setModalState(
                              () => zeroKnowledgeConfirmed = v == true,
                            ),
                            title: const Text(
                              'Estou ciente de que no modo ZERO_KNOWLEDGE a chave não é salva em texto claro no servidor e sua perda impossibilita a descriptografia.',
                              style: TextStyle(
                                fontSize: 12,
                                color: AppTheme.accentWarning,
                              ),
                            ),
                          ),
                        ],
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: cronCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Expressão Cron *',
                                  hintText: '0 0 3 * * *',
                                ),
                                validator: (v) =>
                                    (v == null || v.trim().isEmpty)
                                    ? 'Obrigatório'
                                    : null,
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: retentionCtrl,
                                keyboardType: TextInputType.number,
                                decoration: const InputDecoration(
                                  labelText: 'Retenção (Dias) *',
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          initialValue: compression,
                          decoration: const InputDecoration(
                            labelText: 'Algoritmo de Compressão *',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'ZSTD',
                              child: Text('Zstandard (ZSTD - Alta Performance)'),
                            ),
                            DropdownMenuItem(
                              value: 'GZIP',
                              child: Text('GZIP (Compatibilidade Universal)'),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) setModalState(() => compression = v);
                          },
                        ),
                        const SizedBox(height: 8),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text(
                            'Verificação Automática de Integridade (Dry-Run)',
                            style: TextStyle(fontSize: 13),
                          ),
                          value: autoDryRun,
                          onChanged: (v) => setModalState(() => autoDryRun = v),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: isSaving
                      ? null
                      : () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancelar'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: isSaving
                      ? null
                      : () async {
                          if (!formKey.currentState!.validate()) return;
                          if (securityMode == 'ZERO_KNOWLEDGE' &&
                              !zeroKnowledgeConfirmed) {
                            ScaffoldMessenger.of(this.context).showSnackBar(
                              const SnackBar(
                                content: Text(
                                  'Confirme o termo de responsabilidade do modo ZERO_KNOWLEDGE.',
                                ),
                                backgroundColor: AppTheme.accentWarning,
                              ),
                            );
                            return;
                          }
                          setModalState(() => isSaving = true);
                          try {
                            final result = await ref
                                .read(backupDrRepositoryProvider)
                                .configurePolicy(
                                  BackupPolicySavePayload(
                                    securityMode: securityMode,
                                    customMasterKey:
                                        securityMode == 'ZERO_KNOWLEDGE'
                                        ? customKeyCtrl.text
                                        : null,
                                    cronExpression: cronCtrl.text,
                                    retentionDays:
                                        int.tryParse(
                                          retentionCtrl.text.trim(),
                                        ) ??
                                        30,
                                    compressionAlgorithm: compression,
                                    autoDryRunEnabled: autoDryRun,
                                  ),
                                );
                            ref.invalidate(backupOverviewProvider);
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                            if (result.generatedPlainMasterKey != null &&
                                result.generatedPlainMasterKey!.isNotEmpty &&
                                mounted) {
                              await _showOneTimeMasterKeyModal(
                                result.generatedPlainMasterKey!,
                              );
                            }
                          } catch (e) {
                            setModalState(() => isSaving = false);
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    'Erro ao configurar política: $e',
                                  ),
                                  backgroundColor: AppTheme.accentError,
                                ),
                              );
                            }
                          }
                        },
                  child: Text(isSaving ? 'Salvando...' : 'Salvar Política'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  /// Modal de exibição única da Master Key gerada pelo backend com cópia obrigatória.
  Future<void> _showOneTimeMasterKeyModal(String plainMasterKey) async {
    bool copied = false;
    await showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (ctx) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: Row(
                children: const [
                  Icon(Icons.key_outlined, color: AppTheme.accentWarning),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Chave Mestra AES-256 (Exibição Única)',
                      style: TextStyle(fontSize: 17),
                    ),
                  ),
                ],
              ),
              content: SizedBox(
                width: 480,
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    const Text(
                      'Copie e guarde esta Master Key em um cofre de senhas seguro. Por segurança, ela não será exibida novamente em texto aberto:',
                      style: TextStyle(
                        fontSize: 13,
                        color: AppTheme.textSecondary,
                      ),
                    ),
                    const SizedBox(height: 14),
                    Container(
                      width: double.infinity,
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: AppTheme.darkBg,
                        borderRadius: BorderRadius.circular(8),
                        border: Border.all(color: AppTheme.accentWarning),
                      ),
                      child: SelectableText(
                        plainMasterKey,
                        style: const TextStyle(
                          fontFamily: 'monospace',
                          fontSize: 14,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.accentWarning,
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              actions: [
                OutlinedButton.icon(
                  onPressed: () {
                    Clipboard.setData(ClipboardData(text: plainMasterKey));
                    setModalState(() => copied = true);
                  },
                  icon: const Icon(Icons.copy, size: 16),
                  label: Text(copied ? 'Chave Copiada!' : 'Copiar Master Key'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.accentGreen,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: copied ? () => Navigator.of(ctx).pop() : null,
                  child: const Text('Já Copiei em Local Seguro'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<void> _showAddDestinationDialog() async {
    final formKey = GlobalKey<FormState>();
    String storageType = 'S3_COMPATIBLE';
    final nameCtrl = TextEditingController();
    final endpointCtrl = TextEditingController();
    final bucketCtrl = TextEditingController();
    final regionCtrl = TextEditingController(text: 'us-east-1');
    final accessCtrl = TextEditingController();
    final secretCtrl = TextEditingController();
    final prefixCtrl = TextEditingController(text: 'backups/isperp');
    bool isPrimary = true;
    bool isSaving = false;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: const Text('Novo Destino de Backup (/destinations)'),
              content: SizedBox(
                width: 480,
                child: SingleChildScrollView(
                  child: Form(
                    key: formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        TextFormField(
                          controller: nameCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Nome do Cofre / Destino *',
                            hintText: 'Ex: Wasabi Hot Cloud Storage DR',
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Obrigatório'
                              : null,
                        ),
                        const SizedBox(height: 12),
                        DropdownButtonFormField<String>(
                          initialValue: storageType,
                          decoration: const InputDecoration(
                            labelText: 'Tipo de Armazenamento *',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'S3_COMPATIBLE',
                              child: Text('S3 Compatível (AWS / Wasabi / R2)'),
                            ),
                            DropdownMenuItem(
                              value: 'SFTP',
                              child: Text('Servidor SFTP Remoto'),
                            ),
                            DropdownMenuItem(
                              value: 'LOCAL_VOLUME',
                              child: Text('Volume Local / NAS Montado'),
                            ),
                            DropdownMenuItem(
                              value: 'ISPERP_CLOUD',
                              child: Text('ispERP Cloud Vault'),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) {
                              setModalState(() => storageType = v);
                            }
                          },
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: endpointCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Endpoint URL / Host',
                          ),
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: bucketCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Bucket / Diretório',
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: regionCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Região',
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        Row(
                          children: [
                            Expanded(
                              child: TextFormField(
                                controller: accessCtrl,
                                decoration: const InputDecoration(
                                  labelText: 'Access Key / Usuário',
                                ),
                              ),
                            ),
                            const SizedBox(width: 12),
                            Expanded(
                              child: TextFormField(
                                controller: secretCtrl,
                                obscureText: true,
                                decoration: const InputDecoration(
                                  labelText: 'Secret Key / Senha',
                                ),
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: prefixCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Prefixo do Caminho (Path Prefix)',
                          ),
                        ),
                        const SizedBox(height: 8),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Definir como Destino Primário'),
                          value: isPrimary,
                          onChanged: (v) => setModalState(() => isPrimary = v),
                        ),
                      ],
                    ),
                  ),
                ),
              ),
              actions: [
                TextButton(
                  onPressed: isSaving
                      ? null
                      : () => Navigator.of(dialogContext).pop(),
                  child: const Text('Cancelar'),
                ),
                ElevatedButton(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryIndigo,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: isSaving
                      ? null
                      : () async {
                          if (!formKey.currentState!.validate()) return;
                          setModalState(() => isSaving = true);
                          try {
                            await ref
                                .read(backupDrRepositoryProvider)
                                .createDestination(
                                  BackupDestinationSavePayload(
                                    name: nameCtrl.text,
                                    storageType: storageType,
                                    endpointUrl: endpointCtrl.text,
                                    bucketName: bucketCtrl.text,
                                    region: regionCtrl.text,
                                    accessKey: accessCtrl.text,
                                    secretKey: secretCtrl.text,
                                    pathPrefix: prefixCtrl.text,
                                    isPrimary: isPrimary,
                                  ),
                                );
                            ref.invalidate(backupDestinationsProvider);
                            ref.invalidate(backupOverviewProvider);
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                          } catch (e) {
                            setModalState(() => isSaving = false);
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    'Erro ao cadastrar destino: $e',
                                  ),
                                  backgroundColor: AppTheme.accentError,
                                ),
                              );
                            }
                          }
                        },
                  child: Text(isSaving ? 'Salvando...' : 'Adicionar Destino'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Future<void> _triggerManualBackup(BackupOverviewModel? overview) async {
    String? customMasterKey;
    if (overview?.securityMode == 'ZERO_KNOWLEDGE') {
      final keyCtrl = TextEditingController();
      final confirmed = await showDialog<bool>(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Informar Master Key (Zero-Knowledge)'),
          content: TextField(
            controller: keyCtrl,
            obscureText: true,
            decoration: const InputDecoration(
              labelText: 'Master Key para criptografar este dump',
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.of(ctx).pop(true),
              child: const Text('Disparar Backup'),
            ),
          ],
        ),
      );
      if (confirmed != true) return;
      customMasterKey = keyCtrl.text;
    }

    setState(() => _isExecutingBackup = true);
    try {
      final log = await ref
          .read(backupDrRepositoryProvider)
          .executeBackup(masterKey: customMasterKey);
      ref.invalidate(backupOverviewProvider);
      ref.invalidate(backupHistoryProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Backup concluído (${log.status}) — Arquivo: ${log.fileName}',
            ),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Falha na execução manual de backup: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isExecutingBackup = false);
    }
  }

  Future<void> _openEmergencyKitModal() async {
    setState(() => _isLoadingKit = true);
    try {
      final markdown = await ref
          .read(backupDrRepositoryProvider)
          .fetchEmergencyKitMarkdown();
      ref.invalidate(backupOverviewProvider);
      if (!mounted) return;
      setState(() => _isLoadingKit = false);

      await showDialog<void>(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Row(
            children: const [
              Icon(
                Icons.medical_services_outlined,
                color: AppTheme.accentGreen,
              ),
              SizedBox(width: 10),
              Expanded(
                child: Text(
                  'KIT_RESGATE_EMERGENCIA_ISPERP.md',
                  style: TextStyle(fontSize: 16),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          content: SizedBox(
            width: 680,
            height: 440,
            child: Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: AppTheme.darkBg,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.darkCard),
              ),
              child: SingleChildScrollView(
                child: SelectableText(
                  markdown,
                  style: const TextStyle(fontFamily: 'monospace', fontSize: 12),
                ),
              ),
            ),
          ),
          actions: [
            OutlinedButton.icon(
              onPressed: () {
                Clipboard.setData(ClipboardData(text: markdown));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text(
                      'Kit de Resgate copiado integralmente para a área de transferência!',
                    ),
                    backgroundColor: AppTheme.accentGreen,
                  ),
                );
              },
              icon: const Icon(Icons.copy, size: 16),
              label: const Text('Copiar Markdown Integral'),
            ),
            ElevatedButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Fechar'),
            ),
          ],
        ),
      );
    } catch (e) {
      if (mounted) {
        setState(() => _isLoadingKit = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erro ao gerar Kit de Resgate: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  Widget _buildErrorBanner(String message, VoidCallback onRetry) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.accentError.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(10),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: AppTheme.accentError),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('Repetir')),
        ],
      ),
    );
  }
}
