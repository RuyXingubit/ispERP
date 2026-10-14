import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/payment_gateway_notifier.dart';
import '../data/system_settings_repository.dart';
import 'payment_gateway_config_screen.dart';

/// Central Unificada de Configurações Administrativas do Provedor (`/admin/settings`).
/// Reúne em 5 abas: Empresas & Filiais, Site Vitrine, Storage S3/Local,
/// Canais de Notificação WhatsApp/SMS e Gateway de Pagamento (Xingubit Pay).
class SystemSettingsScreen extends ConsumerStatefulWidget {
  final int initialTabIndex;

  const SystemSettingsScreen({super.key, this.initialTabIndex = 0});

  @override
  ConsumerState<SystemSettingsScreen> createState() =>
      _SystemSettingsScreenState();
}

class _SystemSettingsScreenState extends ConsumerState<SystemSettingsScreen>
    with SingleTickerProviderStateMixin {
  late TabController _tabController;

  // Controllers para Aba 2: Site Vitrine
  final _siteFormKey = GlobalKey<FormState>();
  final _siteTitleCtrl = TextEditingController();
  final _siteDescCtrl = TextEditingController();
  final _primaryColorCtrl = TextEditingController();
  final _secondaryColorCtrl = TextEditingController();
  bool _sitePopulated = false;
  bool _isSavingSite = false;

  // Controllers para Aba 3: Storage S3 / Local
  final _storageFormKey = GlobalKey<FormState>();
  String _storageType = 'S3';
  String _storageProvider = 'SEAWEEDFS_LOCAL';
  final _endpointCtrl = TextEditingController();
  final _bucketCtrl = TextEditingController(text: 'isperp-files');
  final _regionCtrl = TextEditingController(text: 'us-east-1');
  final _accessKeyCtrl = TextEditingController();
  final _secretKeyCtrl = TextEditingController();
  bool _pathStyleAccess = true;
  bool _storageActive = true;
  bool _storagePopulated = false;
  bool _isSavingStorage = false;
  bool _isTestingStorage = false;
  StorageConnectionTestResult? _storageTestResult;

  @override
  void initState() {
    super.initState();
    _tabController = TabController(
      length: 5,
      vsync: this,
      initialIndex: widget.initialTabIndex.clamp(0, 4),
    );
  }

  @override
  void dispose() {
    _tabController.dispose();
    _siteTitleCtrl.dispose();
    _siteDescCtrl.dispose();
    _primaryColorCtrl.dispose();
    _secondaryColorCtrl.dispose();
    _endpointCtrl.dispose();
    _bucketCtrl.dispose();
    _regionCtrl.dispose();
    _accessKeyCtrl.dispose();
    _secretKeyCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Padding(
            padding: const EdgeInsets.fromLTRB(24, 24, 24, 12),
            child: Wrap(
              spacing: 16,
              runSpacing: 12,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Configurações Unificadas do Sistema',
                      style: TextStyle(
                        fontSize: 22,
                        fontWeight: FontWeight.bold,
                      ),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Governança de filiais, identidade institucional, objetos S3, mensageria WhatsApp e gateway de pagamento',
                      style: TextStyle(
                        color: AppTheme.textSecondary,
                        fontSize: 13,
                      ),
                    ),
                  ],
                ),
                OutlinedButton.icon(
                  onPressed: () {
                    _sitePopulated = false;
                    _storagePopulated = false;
                    ref.invalidate(companiesListProvider);
                    ref.invalidate(siteSettingsProvider);
                    ref.invalidate(storageConfigProvider);
                    ref.invalidate(notificationConfigsListProvider);
                    ref.read(paymentGatewaysProvider.notifier).loadConfigs();
                  },
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('Recarregar Tudo'),
                ),
              ],
            ),
          ),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 24),
            decoration: const BoxDecoration(
              border: Border(bottom: BorderSide(color: AppTheme.darkCard)),
            ),
            child: TabBar(
              controller: _tabController,
              isScrollable: true,
              tabAlignment: TabAlignment.start,
              indicatorColor: AppTheme.primaryBlue,
              labelColor: AppTheme.primaryBlue,
              unselectedLabelColor: AppTheme.textSecondary,
              tabs: const [
                Tab(
                  icon: Icon(Icons.business_outlined, size: 18),
                  text: 'Empresas & Filiais',
                ),
                Tab(
                  icon: Icon(Icons.public_outlined, size: 18),
                  text: 'Site Vitrine',
                ),
                Tab(
                  icon: Icon(Icons.cloud_queue_outlined, size: 18),
                  text: 'Storage S3 / Local',
                ),
                Tab(
                  icon: Icon(Icons.chat_bubble_outline, size: 18),
                  text: 'Notificações WhatsApp/SMS',
                ),
                Tab(
                  icon: Icon(Icons.payments_outlined, size: 18),
                  text: 'Gateway (Xingubit Pay)',
                ),
              ],
            ),
          ),
          Expanded(
            child: TabBarView(
              controller: _tabController,
              children: [
                _buildCompaniesTab(),
                _buildSiteSettingsTab(),
                _buildStorageConfigTab(),
                _buildNotificationsTab(),
                const PaymentGatewayConfigScreen(),
              ],
            ),
          ),
        ],
      ),
    );
  }

  // ===========================================================================
  // ABA 1: EMPRESAS & FILIAIS (/companies)
  // ===========================================================================

  Widget _buildCompaniesTab() {
    final companiesAsync = ref.watch(companiesListProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Expanded(
                child: Text(
                  'Empresas e Unidades Operacionais (/companies)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryBlue,
                  foregroundColor: Colors.white,
                ),
                onPressed: () => _showCompanyDialog(),
                icon: const Icon(Icons.add_business_outlined, size: 18),
                label: const Text('Nova Empresa / Filial'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          companiesAsync.when(
            loading: () => const Center(
              child: Padding(
                padding: EdgeInsets.all(40.0),
                child: CircularProgressIndicator(),
              ),
            ),
            error: (err, _) => _buildErrorCard(
              'Erro ao carregar empresas: $err',
              () => ref.invalidate(companiesListProvider),
            ),
            data: (companies) {
              if (companies.isEmpty) {
                return _buildEmptyCard(
                  icon: Icons.business_outlined,
                  title: 'Nenhuma empresa ou filial cadastrada',
                  subtitle:
                      'Cadastre a razão social matriz do provedor para emissão de contratos e faturamento.',
                );
              }
              return Wrap(
                spacing: 16,
                runSpacing: 16,
                children: companies.map(_buildCompanyCard).toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildCompanyCard(CompanyModel comp) {
    return Container(
      width: 360,
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
              const Icon(
                Icons.business_outlined,
                color: AppTheme.primaryBlue,
                size: 20,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  comp.name,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: (comp.active
                          ? AppTheme.accentGreen
                          : AppTheme.textMuted)
                      .withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  comp.active ? 'ATIVA' : 'INATIVA',
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: comp.active
                        ? AppTheme.accentGreen
                        : AppTheme.textMuted,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            'CNPJ: ${comp.document ?? "Não informado"}',
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          if (comp.email != null && comp.email!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              'E-mail: ${comp.email}',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
              overflow: TextOverflow.ellipsis,
            ),
          ],
          if (comp.phone != null && comp.phone!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              'Telefone: ${comp.phone}',
              style: const TextStyle(
                fontSize: 12,
                color: AppTheme.textSecondary,
              ),
            ),
          ],
          if (comp.address != null && comp.address!.isNotEmpty) ...[
            const SizedBox(height: 4),
            Text(
              comp.address!,
              style: const TextStyle(fontSize: 11, color: AppTheme.textMuted),
              maxLines: 2,
              overflow: TextOverflow.ellipsis,
            ),
          ],
          const Divider(height: 22),
          Row(
            mainAxisAlignment: MainAxisAlignment.end,
            children: [
              TextButton.icon(
                onPressed: () => _showCompanyDialog(existing: comp),
                icon: const Icon(Icons.edit_outlined, size: 16),
                label: const Text('Editar'),
              ),
              const SizedBox(width: 8),
              TextButton.icon(
                style: TextButton.styleFrom(
                  foregroundColor: AppTheme.accentError,
                ),
                onPressed: () async {
                  try {
                    await ref
                        .read(systemSettingsRepositoryProvider)
                        .deleteCompany(comp.id);
                    ref.invalidate(companiesListProvider);
                  } catch (e) {
                    if (mounted) {
                      ScaffoldMessenger.of(context).showSnackBar(
                        SnackBar(
                          content: Text('Erro ao remover empresa: $e'),
                          backgroundColor: AppTheme.accentError,
                        ),
                      );
                    }
                  }
                },
                icon: const Icon(Icons.delete_outline, size: 16),
                label: const Text('Remover'),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Future<void> _showCompanyDialog({CompanyModel? existing}) async {
    final formKey = GlobalKey<FormState>();
    final nameCtrl = TextEditingController(text: existing?.name ?? '');
    final docCtrl = TextEditingController(text: existing?.document ?? '');
    final addressCtrl = TextEditingController(text: existing?.address ?? '');
    final phoneCtrl = TextEditingController(text: existing?.phone ?? '');
    final emailCtrl = TextEditingController(text: existing?.email ?? '');
    final webCtrl = TextEditingController(text: existing?.website ?? '');
    bool active = existing?.active ?? true;
    bool isSaving = false;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: Text(
                existing == null
                    ? 'Cadastrar Empresa / Filial'
                    : 'Editar Empresa: ${existing.name}',
              ),
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
                            labelText: 'Razão Social / Nome Fantasia *',
                          ),
                          validator: (v) =>
                              (v == null || v.trim().isEmpty)
                              ? 'Informe o nome da empresa'
                              : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: docCtrl,
                          decoration: const InputDecoration(
                            labelText: 'CNPJ',
                            hintText: '00.000.000/0001-00',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: phoneCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Telefone Comercial',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: emailCtrl,
                          decoration: const InputDecoration(
                            labelText: 'E-mail Institucional',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: webCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Website (URL)',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: addressCtrl,
                          maxLines: 2,
                          decoration: const InputDecoration(
                            labelText: 'Endereço Completo da Sede / Filial',
                          ),
                        ),
                        const SizedBox(height: 8),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Unidade Ativa'),
                          value: active,
                          onChanged: (v) => setModalState(() => active = v),
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
                          setModalState(() => isSaving = true);
                          try {
                            final payload = CompanySavePayload(
                              name: nameCtrl.text,
                              document: docCtrl.text,
                              address: addressCtrl.text,
                              phone: phoneCtrl.text,
                              email: emailCtrl.text,
                              website: webCtrl.text,
                              active: active,
                            );
                            final repo = ref.read(
                              systemSettingsRepositoryProvider,
                            );
                            if (existing == null) {
                              await repo.createCompany(payload);
                            } else {
                              await repo.updateCompany(existing.id, payload);
                            }
                            ref.invalidate(companiesListProvider);
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                          } catch (e) {
                            setModalState(() => isSaving = false);
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text('Erro ao salvar empresa: $e'),
                                  backgroundColor: AppTheme.accentError,
                                ),
                              );
                            }
                          }
                        },
                  child: Text(isSaving ? 'Salvando...' : 'Salvar'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  // ===========================================================================
  // ABA 2: SITE VITRINE & IDENTIDADE (/site-settings)
  // ===========================================================================

  Widget _buildSiteSettingsTab() {
    final siteAsync = ref.watch(siteSettingsProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: siteAsync.when(
        loading: () => const Center(
          child: Padding(
            padding: EdgeInsets.all(40.0),
            child: CircularProgressIndicator(),
          ),
        ),
        error: (err, _) => _buildErrorCard(
          'Erro ao carregar configurações do site vitrine: $err',
          () => ref.invalidate(siteSettingsProvider),
        ),
        data: (settings) {
          if (!_sitePopulated && settings != null) {
            _sitePopulated = true;
            _siteTitleCtrl.text = settings.siteTitle;
            _siteDescCtrl.text = settings.siteDescription ?? '';
            _primaryColorCtrl.text = settings.primaryColor ?? '#0EA5E9';
            _secondaryColorCtrl.text = settings.secondaryColor ?? '#1E293B';
          }

          return ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 720),
            child: Card(
              color: AppTheme.darkSurface,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: const BorderSide(color: AppTheme.darkCard),
              ),
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Form(
                  key: _siteFormKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Identidade Visual & Site Vitrine (/site-settings)',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Parâmetros públicos consumidos pelo Site Institucional e Central do Assinante',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 20),
                      TextFormField(
                        controller: _siteTitleCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Título Oficial da Vitrine *',
                          hintText: 'Ex: Nexus Fibra — Internet Ultrarrápida',
                        ),
                        validator: (v) => (v == null || v.trim().isEmpty)
                            ? 'Informe o título da vitrine'
                            : null,
                      ),
                      const SizedBox(height: 16),
                      TextFormField(
                        controller: _siteDescCtrl,
                        maxLines: 3,
                        decoration: const InputDecoration(
                          labelText: 'Subtítulo / Descrição Institucional',
                        ),
                      ),
                      const SizedBox(height: 16),
                      Row(
                        children: [
                          Expanded(
                            child: TextFormField(
                              controller: _primaryColorCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Cor Primária (Hex)',
                                hintText: '#0EA5E9',
                              ),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: TextFormField(
                              controller: _secondaryColorCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Cor Secundária (Hex)',
                                hintText: '#1E293B',
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 24),
                      SizedBox(
                        width: double.infinity,
                        child: ElevatedButton.icon(
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.primaryBlue,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(vertical: 14),
                          ),
                          onPressed: _isSavingSite
                              ? null
                              : () async {
                                  if (!_siteFormKey.currentState!.validate()) {
                                    return;
                                  }
                                  setState(() => _isSavingSite = true);
                                  try {
                                    await ref
                                        .read(systemSettingsRepositoryProvider)
                                        .updateSiteSettings(
                                          SiteSettingsSavePayload(
                                            siteTitle: _siteTitleCtrl.text,
                                            siteDescription: _siteDescCtrl.text,
                                            primaryColor:
                                                _primaryColorCtrl.text,
                                            secondaryColor:
                                                _secondaryColorCtrl.text,
                                          ),
                                        );
                                    ref.invalidate(siteSettingsProvider);
                                    if (mounted) {
                                      ScaffoldMessenger.of(
                                        context,
                                      ).showSnackBar(
                                        const SnackBar(
                                          content: Text(
                                            'Configurações do site vitrine salvas com sucesso!',
                                          ),
                                          backgroundColor: AppTheme.accentGreen,
                                        ),
                                      );
                                    }
                                  } catch (e) {
                                    if (mounted) {
                                      ScaffoldMessenger.of(
                                        context,
                                      ).showSnackBar(
                                        SnackBar(
                                          content: Text(
                                            'Falha ao atualizar site vitrine: $e',
                                          ),
                                          backgroundColor: AppTheme.accentError,
                                        ),
                                      );
                                    }
                                  } finally {
                                    if (mounted) {
                                      setState(() => _isSavingSite = false);
                                    }
                                  }
                                },
                          icon: const Icon(Icons.save_outlined, size: 18),
                          label: Text(
                            _isSavingSite
                                ? 'Salvando...'
                                : 'Salvar Identidade do Site Vitrine',
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  // ===========================================================================
  // ABA 3: STORAGE S3 / SEAWEEDFS (/storage/config)
  // ===========================================================================

  Widget _buildStorageConfigTab() {
    final storageAsync = ref.watch(storageConfigProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: storageAsync.when(
        loading: () => const Center(
          child: Padding(
            padding: EdgeInsets.all(40.0),
            child: CircularProgressIndicator(),
          ),
        ),
        error: (err, _) => _buildErrorCard(
          'Erro ao carregar configuração de Storage: $err',
          () => ref.invalidate(storageConfigProvider),
        ),
        data: (config) {
          if (!_storagePopulated && config != null) {
            _storagePopulated = true;
            _storageType = config.storageType;
            _storageProvider = config.provider;
            _endpointCtrl.text = config.endpointUrl ?? '';
            _bucketCtrl.text = config.bucketName;
            _regionCtrl.text = config.region;
            _accessKeyCtrl.text = config.accessKey ?? '';
            _pathStyleAccess = config.pathStyleAccess;
            _storageActive = config.isActive;
          }

          return ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 780),
            child: Card(
              color: AppTheme.darkSurface,
              shape: RoundedRectangleBorder(
                borderRadius: BorderRadius.circular(14),
                side: const BorderSide(color: AppTheme.darkCard),
              ),
              child: Padding(
                padding: const EdgeInsets.all(24.0),
                child: Form(
                  key: _storageFormKey,
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      const Text(
                        'Armazenamento de Objetos S3 / SeaweedFS (/storage/config)',
                        style: TextStyle(
                          fontSize: 17,
                          fontWeight: FontWeight.bold,
                        ),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Repositório para fotos de instalações FTTH, comprovantes de depósito e documentos assinados',
                        style: TextStyle(
                          fontSize: 12,
                          color: AppTheme.textSecondary,
                        ),
                      ),
                      const SizedBox(height: 20),
                      Row(
                        children: [
                          Expanded(
                            child: DropdownButtonFormField<String>(
                              initialValue: _storageType,
                              decoration: const InputDecoration(
                                labelText: 'Tipo de Storage *',
                              ),
                              items: const [
                                DropdownMenuItem(
                                  value: 'S3',
                                  child: Text('Object Storage S3'),
                                ),
                                DropdownMenuItem(
                                  value: 'LOCAL',
                                  child: Text('Disco Local do Servidor'),
                                ),
                              ],
                              onChanged: (v) {
                                if (v != null) setState(() => _storageType = v);
                              },
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: DropdownButtonFormField<String>(
                              initialValue: _storageProvider,
                              decoration: const InputDecoration(
                                labelText: 'Provedor / Engine *',
                              ),
                              items: const [
                                DropdownMenuItem(
                                  value: 'SEAWEEDFS_LOCAL',
                                  child: Text('SeaweedFS (Local/Docker)'),
                                ),
                                DropdownMenuItem(
                                  value: 'AWS_S3',
                                  child: Text('Amazon S3'),
                                ),
                                DropdownMenuItem(
                                  value: 'CLOUDFLARE_R2',
                                  child: Text('Cloudflare R2'),
                                ),
                                DropdownMenuItem(
                                  value: 'CUSTOM_S3',
                                  child: Text('MinIO / Wasabi / S3 Custom'),
                                ),
                                DropdownMenuItem(
                                  value: 'LOCAL_DISK',
                                  child: Text('Filesystem Local'),
                                ),
                              ],
                              onChanged: (v) {
                                if (v != null) {
                                  setState(() => _storageProvider = v);
                                }
                              },
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      TextFormField(
                        controller: _endpointCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Endpoint URL (S3 / SeaweedFS)',
                          hintText: 'http://seaweedfs:8333',
                        ),
                      ),
                      const SizedBox(height: 14),
                      Row(
                        children: [
                          Expanded(
                            child: TextFormField(
                              controller: _bucketCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Nome do Bucket *',
                              ),
                              validator: (v) => (v == null || v.trim().isEmpty)
                                  ? 'Obrigatório'
                                  : null,
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: TextFormField(
                              controller: _regionCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Região *',
                              ),
                              validator: (v) => (v == null || v.trim().isEmpty)
                                  ? 'Obrigatório'
                                  : null,
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 14),
                      Row(
                        children: [
                          Expanded(
                            child: TextFormField(
                              controller: _accessKeyCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Access Key',
                              ),
                            ),
                          ),
                          const SizedBox(width: 16),
                          Expanded(
                            child: TextFormField(
                              controller: _secretKeyCtrl,
                              obscureText: true,
                              decoration: InputDecoration(
                                labelText: 'Secret Key',
                                hintText: config?.maskedSecretKey ?? '',
                              ),
                            ),
                          ),
                        ],
                      ),
                      const SizedBox(height: 12),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text(
                          'Path-Style Access (Recomendado para SeaweedFS / MinIO)',
                          style: TextStyle(fontSize: 13),
                        ),
                        value: _pathStyleAccess,
                        onChanged: (v) => setState(() => _pathStyleAccess = v),
                      ),
                      SwitchListTile(
                        contentPadding: EdgeInsets.zero,
                        title: const Text(
                          'Configuração Ativa',
                          style: TextStyle(fontSize: 13),
                        ),
                        value: _storageActive,
                        onChanged: (v) => setState(() => _storageActive = v),
                      ),
                      if (_storageTestResult != null) ...[
                        const SizedBox(height: 12),
                        Container(
                          padding: const EdgeInsets.all(12),
                          decoration: BoxDecoration(
                            color: (_storageTestResult!.success
                                    ? AppTheme.accentGreen
                                    : AppTheme.accentError)
                                .withValues(alpha: 0.12),
                            borderRadius: BorderRadius.circular(10),
                          ),
                          child: Row(
                            children: [
                              Icon(
                                _storageTestResult!.success
                                    ? Icons.check_circle_outline
                                    : Icons.error_outline,
                                color: _storageTestResult!.success
                                    ? AppTheme.accentGreen
                                    : AppTheme.accentError,
                              ),
                              const SizedBox(width: 10),
                              Expanded(
                                child: Text(
                                  '${_storageTestResult!.message} (${_storageTestResult!.latencyMs} ms)',
                                  style: TextStyle(
                                    fontSize: 12,
                                    color: _storageTestResult!.success
                                        ? AppTheme.accentGreen
                                        : AppTheme.accentError,
                                    fontWeight: FontWeight.w600,
                                  ),
                                ),
                              ),
                            ],
                          ),
                        ),
                      ],
                      const SizedBox(height: 20),
                      Wrap(
                        spacing: 12,
                        runSpacing: 12,
                        children: [
                          OutlinedButton.icon(
                            onPressed: _isTestingStorage
                                ? null
                                : () async {
                                    if (!_storageFormKey.currentState!
                                        .validate()) {
                                      return;
                                    }
                                    setState(() => _isTestingStorage = true);
                                    try {
                                      final res = await ref
                                          .read(
                                            systemSettingsRepositoryProvider,
                                          )
                                          .testStorageConnection(
                                            _buildStoragePayload(),
                                          );
                                      setState(() {
                                        _storageTestResult = res;
                                        _isTestingStorage = false;
                                      });
                                    } catch (e) {
                                      setState(() {
                                        _storageTestResult =
                                            StorageConnectionTestResult(
                                              success: false,
                                              message:
                                                  'Falha na conexão: ${e.toString()}',
                                              latencyMs: 0,
                                            );
                                        _isTestingStorage = false;
                                      });
                                    }
                                  },
                            icon: const Icon(Icons.network_check, size: 18),
                            label: Text(
                              _isTestingStorage
                                  ? 'Testando Bucket...'
                                  : 'Testar Conectividade (/test)',
                            ),
                          ),
                          ElevatedButton.icon(
                            style: ElevatedButton.styleFrom(
                              backgroundColor: AppTheme.primaryBlue,
                              foregroundColor: Colors.white,
                            ),
                            onPressed: _isSavingStorage
                                ? null
                                : () async {
                                    if (!_storageFormKey.currentState!
                                        .validate()) {
                                      return;
                                    }
                                    setState(() => _isSavingStorage = true);
                                    try {
                                      await ref
                                          .read(
                                            systemSettingsRepositoryProvider,
                                          )
                                          .saveStorageConfig(
                                            _buildStoragePayload(),
                                          );
                                      ref.invalidate(storageConfigProvider);
                                      if (mounted) {
                                        ScaffoldMessenger.of(
                                          context,
                                        ).showSnackBar(
                                          const SnackBar(
                                            content: Text(
                                              'Configuração de armazenamento salva com sucesso!',
                                            ),
                                            backgroundColor:
                                                AppTheme.accentGreen,
                                          ),
                                        );
                                      }
                                    } catch (e) {
                                      if (mounted) {
                                        ScaffoldMessenger.of(
                                          context,
                                        ).showSnackBar(
                                          SnackBar(
                                            content: Text(
                                              'Erro ao salvar Storage: $e',
                                            ),
                                            backgroundColor:
                                                AppTheme.accentError,
                                          ),
                                        );
                                      }
                                    } finally {
                                      if (mounted) {
                                        setState(
                                          () => _isSavingStorage = false,
                                        );
                                      }
                                    }
                                  },
                            icon: const Icon(Icons.save_outlined, size: 18),
                            label: Text(
                              _isSavingStorage
                                  ? 'Salvando...'
                                  : 'Salvar Configuração S3/Local',
                            ),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              ),
            ),
          );
        },
      ),
    );
  }

  StorageConfigSavePayload _buildStoragePayload() {
    return StorageConfigSavePayload(
      storageType: _storageType,
      provider: _storageProvider,
      endpointUrl: _endpointCtrl.text,
      bucketName: _bucketCtrl.text,
      region: _regionCtrl.text,
      accessKey: _accessKeyCtrl.text,
      secretKey: _secretKeyCtrl.text,
      pathStyleAccess: _pathStyleAccess,
      isActive: _storageActive,
    );
  }

  // ===========================================================================
  // ABA 4: NOTIFICAÇÕES WHATSAPP / SMS (/notifications/configs)
  // ===========================================================================

  Widget _buildNotificationsTab() {
    final notifAsync = ref.watch(notificationConfigsListProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(24),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Expanded(
                child: Text(
                  'Provedores de Notificação WhatsApp & SMS (/notifications/configs)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              ElevatedButton.icon(
                style: ElevatedButton.styleFrom(
                  backgroundColor: AppTheme.primaryBlue,
                  foregroundColor: Colors.white,
                ),
                onPressed: () => _showNotificationConfigDialog(),
                icon: const Icon(Icons.add_comment_outlined, size: 18),
                label: const Text('Novo Canal WhatsApp/SMS'),
              ),
            ],
          ),
          const SizedBox(height: 16),
          notifAsync.when(
            loading: () => const Center(
              child: Padding(
                padding: EdgeInsets.all(40.0),
                child: CircularProgressIndicator(),
              ),
            ),
            error: (err, _) => _buildErrorCard(
              'Erro ao carregar canais de notificação: $err',
              () => ref.invalidate(notificationConfigsListProvider),
            ),
            data: (configs) {
              if (configs.isEmpty) {
                return _buildEmptyCard(
                  icon: Icons.chat_bubble_outline,
                  title: 'Nenhum canal de notificação configurado',
                  subtitle:
                      'Configure Evolution API, Z-API ou Twilio para envio automático de faturas Pix, PIN do assinante e avisos de O.S.',
                );
              }
              return Wrap(
                spacing: 16,
                runSpacing: 16,
                children: configs.map(_buildNotificationCard).toList(),
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildNotificationCard(NotificationConfigModel cfg) {
    return Container(
      width: 350,
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
              const Icon(
                Icons.chat_outlined,
                color: AppTheme.accentGreen,
                size: 20,
              ),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  cfg.name,
                  style: const TextStyle(
                    fontWeight: FontWeight.bold,
                    fontSize: 15,
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                decoration: BoxDecoration(
                  color: (cfg.active
                          ? AppTheme.accentGreen
                          : AppTheme.textMuted)
                      .withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(8),
                ),
                child: Text(
                  cfg.providerType,
                  style: TextStyle(
                    fontSize: 10,
                    fontWeight: FontWeight.bold,
                    color: cfg.active
                        ? AppTheme.accentGreen
                        : AppTheme.textMuted,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            'URL: ${cfg.apiUrl ?? "Padrão do provedor"}',
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 4),
          Text(
            'Remetente: ${cfg.fromPhoneNumber ?? "Não informado"}',
            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const Divider(height: 22),
          Align(
            alignment: Alignment.centerRight,
            child: TextButton.icon(
              onPressed: () => _showNotificationConfigDialog(existing: cfg),
              icon: const Icon(Icons.edit_outlined, size: 16),
              label: const Text('Editar Credenciais'),
            ),
          ),
        ],
      ),
    );
  }

  Future<void> _showNotificationConfigDialog({
    NotificationConfigModel? existing,
  }) async {
    final formKey = GlobalKey<FormState>();
    String providerType = existing?.providerType ?? 'EVOLUTION_API';
    final nameCtrl = TextEditingController(text: existing?.name ?? '');
    final urlCtrl = TextEditingController(text: existing?.apiUrl ?? '');
    final tokenCtrl = TextEditingController(text: existing?.apiToken ?? '');
    final sidCtrl = TextEditingController(text: existing?.accountSid ?? '');
    final authTokenCtrl = TextEditingController(
      text: existing?.authToken ?? '',
    );
    final phoneCtrl = TextEditingController(
      text: existing?.fromPhoneNumber ?? '',
    );
    bool active = existing?.active ?? true;
    bool isSaving = false;

    await showDialog<void>(
      context: context,
      builder: (dialogContext) {
        return StatefulBuilder(
          builder: (context, setModalState) {
            return AlertDialog(
              backgroundColor: AppTheme.darkSurface,
              title: Text(
                existing == null
                    ? 'Novo Canal de Notificação'
                    : 'Editar Canal: ${existing.name}',
              ),
              content: SizedBox(
                width: 480,
                child: SingleChildScrollView(
                  child: Form(
                    key: formKey,
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        DropdownButtonFormField<String>(
                          initialValue: providerType,
                          decoration: const InputDecoration(
                            labelText: 'Gateway WhatsApp / SMS *',
                          ),
                          items: const [
                            DropdownMenuItem(
                              value: 'EVOLUTION_API',
                              child: Text('Evolution API (WhatsApp)'),
                            ),
                            DropdownMenuItem(
                              value: 'Z_API',
                              child: Text('Z-API (WhatsApp)'),
                            ),
                            DropdownMenuItem(
                              value: 'TWILIO',
                              child: Text('Twilio (WhatsApp / SMS)'),
                            ),
                            DropdownMenuItem(
                              value: 'MOCK',
                              child: Text('Log Interno / Homologação (MOCK)'),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) {
                              setModalState(() => providerType = v);
                            }
                          },
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: nameCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Nome de Identificação *',
                            hintText: 'Ex: WhatsApp Cobrança & SAC',
                          ),
                          validator: (v) => (v == null || v.trim().isEmpty)
                              ? 'Obrigatório'
                              : null,
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: urlCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Endpoint API URL',
                          ),
                        ),
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: tokenCtrl,
                          obscureText: true,
                          decoration: const InputDecoration(
                            labelText: 'API Token / Instance Key',
                          ),
                        ),
                        if (providerType == 'TWILIO') ...[
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: sidCtrl,
                            decoration: const InputDecoration(
                              labelText: 'Twilio Account SID',
                            ),
                          ),
                          const SizedBox(height: 12),
                          TextFormField(
                            controller: authTokenCtrl,
                            obscureText: true,
                            decoration: const InputDecoration(
                              labelText: 'Twilio Auth Token',
                            ),
                          ),
                        ],
                        const SizedBox(height: 12),
                        TextFormField(
                          controller: phoneCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Número Remetente (DDI+DDD+Número)',
                            hintText: '+5511999998888',
                          ),
                        ),
                        const SizedBox(height: 8),
                        SwitchListTile(
                          contentPadding: EdgeInsets.zero,
                          title: const Text('Canal Ativo'),
                          value: active,
                          onChanged: (v) => setModalState(() => active = v),
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
                          setModalState(() => isSaving = true);
                          try {
                            final payload = NotificationConfigSavePayload(
                              providerType: providerType,
                              name: nameCtrl.text,
                              apiUrl: urlCtrl.text,
                              apiToken: tokenCtrl.text,
                              accountSid: sidCtrl.text,
                              authToken: authTokenCtrl.text,
                              fromPhoneNumber: phoneCtrl.text,
                              active: active,
                            );
                            final repo = ref.read(
                              systemSettingsRepositoryProvider,
                            );
                            if (existing == null) {
                              await repo.createNotificationConfig(payload);
                            } else {
                              await repo.updateNotificationConfig(
                                existing.id,
                                payload,
                              );
                            }
                            ref.invalidate(notificationConfigsListProvider);
                            if (dialogContext.mounted) {
                              Navigator.of(dialogContext).pop();
                            }
                          } catch (e) {
                            setModalState(() => isSaving = false);
                            if (mounted) {
                              ScaffoldMessenger.of(this.context).showSnackBar(
                                SnackBar(
                                  content: Text(
                                    'Erro ao salvar canal de notificação: $e',
                                  ),
                                  backgroundColor: AppTheme.accentError,
                                ),
                              );
                            }
                          }
                        },
                  child: Text(isSaving ? 'Salvando...' : 'Salvar Canal'),
                ),
              ],
            );
          },
        );
      },
    );
  }

  Widget _buildEmptyCard({
    required IconData icon,
    required String title,
    required String subtitle,
  }) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(28),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        children: [
          Icon(icon, size: 36, color: AppTheme.textMuted),
          const SizedBox(height: 12),
          Text(
            title,
            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
            textAlign: TextAlign.center,
          ),
          const SizedBox(height: 4),
          Text(
            subtitle,
            style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
            textAlign: TextAlign.center,
          ),
        ],
      ),
    );
  }

  Widget _buildErrorCard(String message, VoidCallback onRetry) {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: AppTheme.accentError.withValues(alpha: 0.1),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Row(
        children: [
          const Icon(Icons.error_outline, color: AppTheme.accentError),
          const SizedBox(width: 12),
          Expanded(
            child: Text(
              message,
              style: const TextStyle(color: AppTheme.accentError, fontSize: 13),
            ),
          ),
          TextButton(onPressed: onRetry, child: const Text('Tentar Novamente')),
        ],
      ),
    );
  }
}
