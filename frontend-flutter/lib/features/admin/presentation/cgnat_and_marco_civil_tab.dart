import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/radius_ipam_marcocivil_repository.dart';

/// Aba 5 de `/admin/nas`: Mapeamentos CGNAT, Importador Multi-Vendor e Investigação Forense Marco Civil (Lei 12.965/2014).
class CgnatAndMarcoCivilTab extends ConsumerStatefulWidget {
  const CgnatAndMarcoCivilTab({super.key});

  @override
  ConsumerState<CgnatAndMarcoCivilTab> createState() =>
      _CgnatAndMarcoCivilTabState();
}

class _CgnatAndMarcoCivilTabState extends ConsumerState<CgnatAndMarcoCivilTab> {
  final _ipCtrl = TextEditingController();
  final _portCtrl = TextEditingController();
  final _timestampCtrl =
      TextEditingController(text: '2026-10-14T14:30:00Z');
  final _courtOrderCtrl = TextEditingController();
  final _authorityCtrl = TextEditingController();
  final _validateTokenCtrl = TextEditingController();

  MarcoCivilSearchResultModel? _searchResult;
  MarcoCivilReportModel? _generatedReport;
  PublicValidationResultModel? _validationResult;
  bool _searching = false;
  bool _emittingReport = false;
  bool _validatingToken = false;

  @override
  void dispose() {
    _ipCtrl.dispose();
    _portCtrl.dispose();
    _timestampCtrl.dispose();
    _courtOrderCtrl.dispose();
    _authorityCtrl.dispose();
    _validateTokenCtrl.dispose();
    super.dispose();
  }

  Future<void> _handleForensicSearch() async {
    final ip = _ipCtrl.text.trim();
    final ts = _timestampCtrl.text.trim();
    if (ip.isEmpty || ts.isEmpty) return;

    setState(() {
      _searching = true;
      _generatedReport = null;
    });
    try {
      final res = await ref
          .read(radiusIpamMarcoCivilRepositoryProvider)
          .searchSubscriber(
            ip: ip,
            port: int.tryParse(_portCtrl.text.trim()),
            timestamp: ts,
          );
      if (mounted) {
        setState(() => _searchResult = res);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _searching = false);
    }
  }

  Future<void> _handleEmitReport() async {
    final res = _searchResult;
    if (res == null) return;

    setState(() => _emittingReport = true);
    try {
      final report =
          await ref.read(radiusIpamMarcoCivilRepositoryProvider).generateReport(
                courtOrderNumber: _courtOrderCtrl.text.trim().isEmpty
                    ? null
                    : _courtOrderCtrl.text.trim(),
                requesterAuthority: _authorityCtrl.text.trim().isEmpty
                    ? null
                    : _authorityCtrl.text.trim(),
                queriedIp: res.queriedIp,
                queriedPort: res.queriedPort,
                queriedTimestamp: res.queriedTimestamp,
                matchedContractId: res.contractId,
              );
      if (mounted) {
        setState(() {
          _generatedReport = report;
          _validateTokenCtrl.text = report.validationToken;
        });
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _emittingReport = false);
    }
  }

  Future<void> _handleValidateToken() async {
    final token = _validateTokenCtrl.text.trim();
    if (token.isEmpty) return;

    setState(() => _validatingToken = true);
    try {
      final val = await ref
          .read(radiusIpamMarcoCivilRepositoryProvider)
          .validateReportToken(token);
      if (mounted) {
        setState(() => _validationResult = val);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _validatingToken = false);
    }
  }

  void _showCreateCgnatMappingDialog() {
    final pubIpCtrl = TextEditingController(text: '200.20.10.1');
    final portStartCtrl = TextEditingController(text: '1024');
    final portEndCtrl = TextEditingController(text: '2047');
    final privStartCtrl = TextEditingController(text: '100.64.0.2');
    final privEndCtrl = TextEditingController(text: '100.64.0.33');
    String vendorType = 'MIKROTIK';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar Mapeamento CGNAT Determinístico (PBA)'),
          content: SizedBox(
            width: 460,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  DropdownButtonFormField<String>(
                    initialValue: vendorType,
                    decoration:
                        const InputDecoration(labelText: 'Fabricante / BNG'),
                    items: const [
                      DropdownMenuItem(
                          value: 'MIKROTIK', child: Text('MIKROTIK')),
                      DropdownMenuItem(value: 'HUAWEI', child: Text('HUAWEI')),
                      DropdownMenuItem(value: 'A10', child: Text('A10')),
                      DropdownMenuItem(value: 'CISCO', child: Text('CISCO')),
                    ],
                    onChanged: (v) {
                      if (v != null) setModalState(() => vendorType = v);
                    },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: pubIpCtrl,
                    decoration:
                        const InputDecoration(labelText: 'IP Público de Saída *'),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: portStartCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'Porta Inicial *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: portEndCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'Porta Final *'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: privStartCtrl,
                          decoration: const InputDecoration(
                              labelText: 'IP Privado Inicial *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: privEndCtrl,
                          decoration: const InputDecoration(
                              labelText: 'IP Privado Final *'),
                        ),
                      ),
                    ],
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                try {
                  await ref
                      .read(radiusIpamMarcoCivilRepositoryProvider)
                      .createCgnatMapping(
                        vendorType: vendorType,
                        publicIp: pubIpCtrl.text.trim(),
                        portStart:
                            int.tryParse(portStartCtrl.text.trim()) ?? 1024,
                        portEnd: int.tryParse(portEndCtrl.text.trim()) ?? 2047,
                        privateIpStart: privStartCtrl.text.trim(),
                        privateIpEnd: privEndCtrl.text.trim(),
                      );
                  ref.invalidate(cgnatMappingsProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content:
                            Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Salvar Regra CGNAT'),
            ),
          ],
        ),
      ),
    );
  }

  void _showImportScriptDialog() {
    final scriptCtrl = TextEditingController();
    String vendorType = 'MIKROTIK';
    bool replaceExisting = false;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text(
              'Importar Script de Firewall CGNAT (MikroTik / Huawei / A10 / CSV)'),
          content: SizedBox(
            width: 520,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                DropdownButtonFormField<String>(
                  initialValue: vendorType,
                  decoration: const InputDecoration(labelText: 'Sintaxe do Script'),
                  items: const [
                    DropdownMenuItem(
                        value: 'MIKROTIK',
                        child: Text('MikroTik (/ip firewall nat)')),
                    DropdownMenuItem(
                        value: 'HUAWEI', child: Text('Huawei VRP CGNAT')),
                    DropdownMenuItem(value: 'A10', child: Text('A10 Thunder')),
                    DropdownMenuItem(
                        value: 'CISCO', child: Text('Cisco IOS-XR / CSV')),
                  ],
                  onChanged: (v) {
                    if (v != null) setModalState(() => vendorType = v);
                  },
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: scriptCtrl,
                  maxLines: 5,
                  decoration: const InputDecoration(
                    labelText: 'Conteúdo do Script ou Regras NAT *',
                    hintText:
                        'add action=src-nat chain=srcnat src-address=100.64.0.2 to-addresses=200.20.10.1 to-ports=1024-2047',
                  ),
                ),
                const SizedBox(height: 8),
                CheckboxListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Substituir regras existentes deste NAS'),
                  value: replaceExisting,
                  onChanged: (v) =>
                      setModalState(() => replaceExisting = v ?? false),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                final content = scriptCtrl.text.trim();
                if (content.isEmpty) return;
                try {
                  final res = await ref
                      .read(radiusIpamMarcoCivilRepositoryProvider)
                      .importCgnatScript(
                        vendorType: vendorType,
                        scriptContent: content,
                        replaceExisting: replaceExisting,
                      );
                  ref.invalidate(cgnatMappingsProvider);
                  if (ctx.mounted) {
                    Navigator.of(ctx).pop();
                    ScaffoldMessenger.of(context).showSnackBar(
                      SnackBar(
                        content: Text(
                            'Script processado: ${res.totalSaved}/${res.totalParsed} regras CGNAT importadas.'),
                        backgroundColor: AppTheme.accentGreen,
                      ),
                    );
                  }
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content:
                            Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Importar Regras'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final cgnatAsync = ref.watch(cgnatMappingsProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 1. Investigação Forense Marco Civil da Internet (Lei 12.965/2014)
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Investigação Forense Reversa — Marco Civil da Internet (Lei 12.965/2014)',
                    style: TextStyle(fontSize: 17, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 4),
                  const Text(
                    'Correlação determinística entre blocos de portas CGNAT e sessões RADIUS Accounting com emissão de laudo pericial SHA-256',
                    style:
                        TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                  const SizedBox(height: 14),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      SizedBox(
                        width: 200,
                        child: TextField(
                          controller: _ipCtrl,
                          decoration: const InputDecoration(
                            labelText: 'IP Público Investigado *',
                            hintText: 'Ex: 200.20.10.1',
                            isDense: true,
                          ),
                        ),
                      ),
                      SizedBox(
                        width: 140,
                        child: TextField(
                          controller: _portCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                            labelText: 'Porta Lógica',
                            hintText: 'Ex: 1055',
                            isDense: true,
                          ),
                        ),
                      ),
                      SizedBox(
                        width: 240,
                        child: TextField(
                          controller: _timestampCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Data/Hora Exata (ISO-8601) *',
                            isDense: true,
                          ),
                        ),
                      ),
                      ElevatedButton.icon(
                        onPressed: _searching ? null : _handleForensicSearch,
                        icon: const Icon(Icons.policy, size: 18),
                        label: Text(
                            _searching ? 'Correlacionando...' : 'Rastrear Assinante'),
                      ),
                    ],
                  ),
                  if (_searchResult != null) ...[
                    const Divider(height: 28),
                    Container(
                      padding: const EdgeInsets.all(14),
                      decoration: BoxDecoration(
                        color: (_searchResult!.matched
                                ? AppTheme.accentGreen
                                : AppTheme.accentWarning)
                            .withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _searchResult!.matched
                                ? 'ASSINANTE IDENTIFICADO: ${_searchResult!.customerName ?? _searchResult!.username ?? "-"} (CPF/CNPJ: ${_searchResult!.customerCpfCnpj ?? "-"})'
                                : 'NENHUMA SESSÃO LOCALIZADA PARA OS PARÂMETROS INFORMADOS',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: _searchResult!.matched
                                  ? AppTheme.accentGreen
                                  : AppTheme.accentWarning,
                            ),
                          ),
                          if (_searchResult!.matched) ...[
                            const SizedBox(height: 6),
                            Text(
                              'IP Privado CGNAT: ${_searchResult!.resolvedPrivateIp ?? "IP Direto"} • Regra: ${_searchResult!.cgnatRuleSummary ?? "-"}',
                              style: const TextStyle(fontSize: 12),
                            ),
                            Text(
                              'PPPoE: ${_searchResult!.username ?? "-"} • MAC: ${_searchResult!.callingStationId ?? "-"} • Contrato: ${_searchResult!.contractNumber ?? "-"}',
                              style: const TextStyle(
                                  fontSize: 12, color: AppTheme.textSecondary),
                            ),
                            const SizedBox(height: 12),
                            Wrap(
                              spacing: 12,
                              runSpacing: 8,
                              crossAxisAlignment: WrapCrossAlignment.center,
                              children: [
                                SizedBox(
                                  width: 220,
                                  child: TextField(
                                    controller: _courtOrderCtrl,
                                    decoration: const InputDecoration(
                                      labelText: 'Nº Ofício Judicial / Inquérito',
                                      isDense: true,
                                    ),
                                  ),
                                ),
                                SizedBox(
                                  width: 220,
                                  child: TextField(
                                    controller: _authorityCtrl,
                                    decoration: const InputDecoration(
                                      labelText: 'Autoridade Requisitante',
                                      isDense: true,
                                    ),
                                  ),
                                ),
                                ElevatedButton.icon(
                                  onPressed: _emittingReport
                                      ? null
                                      : _handleEmitReport,
                                  icon: const Icon(Icons.verified_user_outlined,
                                      size: 16),
                                  label: Text(_emittingReport
                                      ? 'Assinando...'
                                      : 'Emitir Laudo Pericial SHA-256'),
                                ),
                              ],
                            ),
                          ],
                        ],
                      ),
                    ),
                  ],
                  if (_generatedReport != null) ...[
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryBlue.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            'Laudo Emitido — Token Público: ${_generatedReport!.validationToken}',
                            style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryBlue),
                          ),
                          Text(
                            'SHA-256: ${_generatedReport!.sha256Hash}',
                            style: const TextStyle(
                                fontSize: 11, fontFamily: 'monospace'),
                          ),
                          Text(
                            'URL de Validação: ${_generatedReport!.publicValidationUrl}',
                            style: const TextStyle(
                                fontSize: 11, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),

          // 2. Validação Pública de Laudo Pericial por Token
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Verificador Público de Autenticidade de Laudo Pericial (/public/marco-civil/validate)',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 12,
                    runSpacing: 10,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      SizedBox(
                        width: 280,
                        child: TextField(
                          controller: _validateTokenCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Token Público de Autenticação',
                            isDense: true,
                          ),
                        ),
                      ),
                      OutlinedButton.icon(
                        onPressed:
                            _validatingToken ? null : _handleValidateToken,
                        icon: const Icon(Icons.qr_code_scanner, size: 18),
                        label: Text(
                            _validatingToken ? 'Validando...' : 'Validar Token'),
                      ),
                    ],
                  ),
                  if (_validationResult != null) ...[
                    const SizedBox(height: 10),
                    Text(
                      '${_validationResult!.valid ? "AUTÊNTICO" : "INVÁLIDO"}: ${_validationResult!.statusMessage} • SHA-256: ${_validationResult!.sha256Hash}',
                      style: TextStyle(
                        fontSize: 12,
                        fontWeight: FontWeight.bold,
                        color: _validationResult!.valid
                            ? AppTheme.accentGreen
                            : AppTheme.accentError,
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // 3. Mapeamentos CGNAT e Importador de Scripts
          Wrap(
            spacing: 12,
            runSpacing: 12,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              const Text(
                'Blocos e Mapeamentos CGNAT Determinísticos (Port Block Allocation)',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              Wrap(
                spacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: _showImportScriptDialog,
                    icon: const Icon(Icons.upload_file_outlined, size: 16),
                    label: const Text('Importar Script NAT'),
                  ),
                  ElevatedButton.icon(
                    onPressed: _showCreateCgnatMappingDialog,
                    icon: const Icon(Icons.add, size: 18),
                    label: const Text('Nova Regra CGNAT'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 12),
          cgnatAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (mappings) {
              if (mappings.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Nenhum mapeamento CGNAT cadastrado.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Column(
                children: mappings.map((m) {
                  return Card(
                    margin: const EdgeInsets.only(bottom: 8),
                    child: ListTile(
                      dense: true,
                      leading: const Icon(Icons.swap_horiz,
                          color: AppTheme.primaryBlue),
                      title: Text(
                        'IP Público: ${m.publicIp} [Portas ${m.portStart}..${m.portEnd}]  ➔  Privado: ${m.privateIpStart}..${m.privateIpEnd}',
                        style: const TextStyle(
                            fontSize: 13, fontWeight: FontWeight.bold),
                      ),
                      subtitle: Text(
                        'Vendor: ${m.vendorType} • Protocolo: ${m.protocol} ${m.nasName != null ? "• BNG: ${m.nasName}" : ""}',
                        style: const TextStyle(
                            fontSize: 11, color: AppTheme.textSecondary),
                      ),
                      trailing: IconButton(
                        icon: const Icon(Icons.delete_outline,
                            size: 18, color: AppTheme.accentError),
                        onPressed: () async {
                          await ref
                              .read(radiusIpamMarcoCivilRepositoryProvider)
                              .deleteCgnatMapping(m.id);
                          ref.invalidate(cgnatMappingsProvider);
                        },
                      ),
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ],
      ),
    );
  }
}
