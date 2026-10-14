import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/features/admin/data/backup_dr_repository.dart';
import 'package:isperp_app/features/admin/data/catalog_repository.dart';
import 'package:isperp_app/features/admin/data/payment_gateway_model.dart';
import 'package:isperp_app/features/admin/data/payment_gateway_repository.dart';
import 'package:isperp_app/features/admin/data/system_settings_repository.dart';
import 'package:isperp_app/features/admin/presentation/backup_dr_screen.dart';
import 'package:isperp_app/features/admin/presentation/catalog_and_templates_screen.dart';
import 'package:isperp_app/features/admin/presentation/system_settings_screen.dart';
import 'package:shared_preferences/shared_preferences.dart';

class _FakePaymentGatewayRepository extends PaymentGatewayRepository {
  _FakePaymentGatewayRepository(super.apiClient);

  @override
  Future<List<PaymentGatewayModel>> getConfigs() async {
    return [
      PaymentGatewayModel(
        id: '0195f000-0000-7000-8000-000000000777',
        gatewayType: PaymentGatewayType.xingubitPay,
        name: 'Xingubit Pay Principal',
        pixKey: 'pix@nexusfibra.com.br',
        sandbox: false,
        active: true,
      ),
    ];
  }
}

void main() {
  group('Onda A — Governança, Catálogo, Configurações & Backup DR', () {
    test(
      'AdminPlanModel, PlanSavePayload, ContractTemplateModel e ContractTemplateVariableModel serializam/deserializam conforme OpenAPI',
      () {
        final planJson = {
          'id': '0195f000-0000-7000-8000-000000000001',
          'name': 'Fibra Gamer 1 Giga',
          'downloadSpeed': 1000,
          'uploadSpeed': 500,
          'price': 149.90,
          'description': 'Plano Wi-Fi 6 com rota otimizada',
          'svaIncluded': 'Paramount+, ExitLag',
          'suspensionDays': 7,
          'alwaysIssueNfcom': true,
          'active': true,
          'createdAt': '2026-10-14T10:00:00Z',
        };
        final plan = AdminPlanModel.fromJson(planJson);
        expect(plan.id, '0195f000-0000-7000-8000-000000000001');
        expect(plan.name, 'Fibra Gamer 1 Giga');
        expect(plan.downloadSpeed, 1000);
        expect(plan.uploadSpeed, 500);
        expect(plan.price, 149.90);
        expect(plan.suspensionDays, 7);
        expect(plan.alwaysIssueNfcom, isTrue);
        expect(plan.active, isTrue);

        const savePlan = PlanSavePayload(
          name: 'Fibra Start 300 Mega',
          downloadSpeed: 300,
          uploadSpeed: 150,
          price: 79.90,
          svaIncluded: 'E-books',
          suspensionDays: 5,
          alwaysIssueNfcom: false,
          active: true,
        );
        expect(savePlan.toJson()['downloadSpeed'], 300);
        expect(savePlan.toJson()['price'], 79.90);

        final tpl = ContractTemplateModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000002',
          'name': 'Contrato Padrão SCM 2026',
          'documentType': 'SERVICE_AGREEMENT',
          'version': 2,
          'isActive': true,
          'contentMarkdown': '# Contrato {{customer.name}}',
          'consentClause': 'Concordo com os termos.',
          'createdAt': '2026-10-14T10:00:00Z',
        });
        expect(tpl.documentTypeLabel, 'Contrato de Prestação SCM/SVA');
        expect(tpl.version, 2);

        final variable = ContractTemplateVariableModel.fromJson({
          'tag': '{{customer.name}}',
          'label': 'Nome do Assinante',
          'category': 'CLIENTE',
          'example': 'João da Silva',
        });
        expect(variable.tag, '{{customer.name}}');
        expect(variable.category, 'CLIENTE');
      },
    );

    test(
      'SystemSettings models (CompanyModel, SiteSettingsModel, StorageConfigModel, NotificationConfigModel) deserializam conforme OpenAPI',
      () {
        final company = CompanyModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000010',
          'name': 'Nexus Fibra Telecom LTDA',
          'document': '12.345.678/0001-90',
          'email': 'contato@nexusfibra.com.br',
          'active': true,
        });
        expect(company.name, 'Nexus Fibra Telecom LTDA');
        expect(company.document, '12.345.678/0001-90');

        final site = SiteSettingsModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000011',
          'siteTitle': 'Nexus Fibra Oficial',
          'primaryColor': '#0EA5E9',
          'secondaryColor': '#1E293B',
        });
        expect(site.siteTitle, 'Nexus Fibra Oficial');
        expect(site.primaryColor, '#0EA5E9');

        final storage = StorageConfigModel.fromJson({
          'storageType': 'S3',
          'provider': 'SEAWEEDFS_LOCAL',
          'endpointUrl': 'http://seaweedfs:8333',
          'bucketName': 'isperp-files',
          'region': 'us-east-1',
          'pathStyleAccess': true,
          'isActive': true,
        });
        expect(storage.provider, 'SEAWEEDFS_LOCAL');
        expect(storage.bucketName, 'isperp-files');

        final testRes = StorageConnectionTestResult.fromJson({
          'success': true,
          'message': 'Conexão S3 OK',
          'latencyMs': 18,
        });
        expect(testRes.success, isTrue);
        expect(testRes.latencyMs, 18);

        final notif = NotificationConfigModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000012',
          'providerType': 'EVOLUTION_API',
          'name': 'WhatsApp Cobrança',
          'fromPhoneNumber': '+5593991000000',
          'active': true,
        });
        expect(notif.providerType, 'EVOLUTION_API');
        expect(notif.name, 'WhatsApp Cobrança');
      },
    );

    test(
      'BackupDr models (Overview, PolicyResult, Destination, ExecutionLog) deserializam conforme OpenAPI',
      () {
        final overview = BackupOverviewModel.fromJson({
          'securityMode': 'ZERO_KNOWLEDGE',
          'hasActivePolicy': true,
          'cronExpression': '0 0 3 * * *',
          'retentionDays': 45,
          'activeDestinationsCount': 2,
          'totalBackupsCount': 14,
          'lastBackupStatus': 'VERIFIED_OK',
          'lastBackupSizeBytes': 10485760,
          'isDryRunVerified': true,
          'rescueKitDownloaded': true,
        });
        expect(overview.securityMode, 'ZERO_KNOWLEDGE');
        expect(overview.retentionDays, 45);
        expect(overview.isDryRunVerified, isTrue);
        expect(overview.rescueKitDownloaded, isTrue);

        final policyRes = BackupPolicyResultModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000020',
          'securityMode': 'MANAGED_RESCUE',
          'masterKeyHash': 'sha256:abc123',
          'generatedPlainMasterKey': 'MK-SECRET-2026-KEY-9988',
          'cronExpression': '0 0 3 * * *',
          'retentionDays': 30,
          'compressionAlgorithm': 'ZSTD',
          'autoDryRunEnabled': true,
          'isActive': true,
        });
        expect(policyRes.generatedPlainMasterKey, 'MK-SECRET-2026-KEY-9988');

        final log = BackupExecutionLogModel.fromJson({
          'id': '0195f000-0000-7000-8000-000000000021',
          'triggerType': 'MANUAL',
          'status': 'SUCCESS',
          'fileName': 'isperp-20261014.sql.zst.enc',
          'compressedSizeBytes': 5242880,
          'sha256Hash': 'e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855',
          'durationSeconds': 12,
          'isDryRunVerified': true,
        });
        expect(log.fileName, 'isperp-20261014.sql.zst.enc');
        expect(log.durationSeconds, 12);
      },
    );

    testWidgets(
      'CatalogAndTemplatesScreen renderiza abas de Planos e Templates Contratuais sem overflow',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sharedPreferencesProvider.overrideWithValue(prefs),
              adminPlansListProvider.overrideWith(
                (ref) async => const [
                  AdminPlanModel(
                    id: 'plan-01',
                    name: 'Fibra 600 Mega',
                    downloadSpeed: 600,
                    uploadSpeed: 300,
                    price: 109.90,
                    svaIncluded: 'Paramount+',
                    active: true,
                  ),
                ],
              ),
              contractTemplatesListProvider.overrideWith(
                (ref) async => const [
                  ContractTemplateModel(
                    id: 'tpl-01',
                    name: 'Minuta SCM Padrão',
                    documentType: 'SERVICE_AGREEMENT',
                    version: 1,
                    isActive: true,
                    contentMarkdown: '# Minuta SCM',
                    consentClause: 'Aceito os termos.',
                  ),
                ],
              ),
            ],
            child: const MaterialApp(home: CatalogAndTemplatesScreen()),
          ),
        );

        await tester.pumpAndSettle();
        expect(find.text('Fibra 600 Mega'), findsOneWidget);
        expect(find.text('600 Mbps Down'), findsOneWidget);

        await tester.tap(
          find.text('Templates de Contrato (/contracts/templates)'),
        );
        await tester.pumpAndSettle();
        expect(find.textContaining('Minuta SCM Padrão'), findsOneWidget);
      },
    );

    testWidgets(
      'SystemSettingsScreen renderiza 5 abas unificadas incluindo Empresas, Site, Storage, Notificações e Gateway',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sharedPreferencesProvider.overrideWithValue(prefs),
              companiesListProvider.overrideWith(
                (ref) async => const [
                  CompanyModel(
                    id: 'comp-01',
                    name: 'Nexus Fibra Matriz',
                    document: '12.345.678/0001-90',
                    active: true,
                  ),
                ],
              ),
              siteSettingsProvider.overrideWith(
                (ref) async => const SiteSettingsModel(
                  id: 'site-01',
                  siteTitle: 'Nexus Fibra Vitrine',
                ),
              ),
              storageConfigProvider.overrideWith(
                (ref) async => const StorageConfigModel(
                  storageType: 'S3',
                  provider: 'SEAWEEDFS_LOCAL',
                  bucketName: 'isperp-files',
                  region: 'us-east-1',
                ),
              ),
              notificationConfigsListProvider.overrideWith(
                (ref) async => const [
                  NotificationConfigModel(
                    id: 'notif-01',
                    providerType: 'EVOLUTION_API',
                    name: 'WhatsApp Oficial',
                    active: true,
                  ),
                ],
              ),
              paymentGatewayRepositoryProvider.overrideWith((ref) {
                return _FakePaymentGatewayRepository(
                  ref.watch(apiClientProvider),
                );
              }),
            ],
            child: const MaterialApp(home: SystemSettingsScreen()),
          ),
        );

        await tester.pumpAndSettle();
        expect(find.text('Nexus Fibra Matriz'), findsOneWidget);
        expect(find.text('Gateway (Xingubit Pay)'), findsOneWidget);
      },
    );

    testWidgets(
      'BackupDrScreen renderiza visão geral, destinos e histórico SHA-256 sem overflow',
      (tester) async {
        SharedPreferences.setMockInitialValues({});
        final prefs = await SharedPreferences.getInstance();

        await tester.pumpWidget(
          ProviderScope(
            overrides: [
              sharedPreferencesProvider.overrideWithValue(prefs),
              backupOverviewProvider.overrideWith(
                (ref) async => const BackupOverviewModel(
                  securityMode: 'MANAGED_RESCUE',
                  hasActivePolicy: true,
                  cronExpression: '0 0 3 * * *',
                  retentionDays: 30,
                  activeDestinationsCount: 1,
                  totalBackupsCount: 5,
                  lastBackupStatus: 'VERIFIED_OK',
                  lastBackupSizeBytes: 2097152,
                  isDryRunVerified: true,
                  rescueKitDownloaded: true,
                ),
              ),
              backupDestinationsProvider.overrideWith(
                (ref) async => const [
                  BackupDestinationModel(
                    id: 'dest-01',
                    name: 'Wasabi S3 Cofre',
                    storageType: 'S3_COMPATIBLE',
                    bucketName: 'isperp-dr-vault',
                    isPrimary: true,
                  ),
                ],
              ),
              backupHistoryProvider.overrideWith(
                (ref) async => const [
                  BackupExecutionLogModel(
                    id: 'log-01',
                    triggerType: 'SCHEDULED',
                    status: 'VERIFIED_OK',
                    fileName: 'dump-2026-10-14.zst.enc',
                    compressedSizeBytes: 2097152,
                    durationSeconds: 8,
                    sha256Hash: 'abc123def456',
                  ),
                ],
              ),
            ],
            child: const MaterialApp(home: BackupDrScreen()),
          ),
        );

        await tester.pumpAndSettle();
        expect(find.text('Managed Rescue'), findsOneWidget);
        expect(find.textContaining('Wasabi S3 Cofre'), findsOneWidget);
        expect(find.text('dump-2026-10-14.zst.enc'), findsOneWidget);
      },
    );
  });
}
