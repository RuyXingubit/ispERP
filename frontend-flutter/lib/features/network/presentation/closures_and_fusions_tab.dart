import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/network_ftth_repository.dart';

/// Aba 4: Caixas de Emenda Óptica (CEO), Splitters PLC/FBT & Diagrama Unifilar de Fusões.
class ClosuresAndFusionsTab extends ConsumerStatefulWidget {
  const ClosuresAndFusionsTab({super.key});

  @override
  ConsumerState<ClosuresAndFusionsTab> createState() =>
      _ClosuresAndFusionsTabState();
}

class _ClosuresAndFusionsTabState extends ConsumerState<ClosuresAndFusionsTab> {
  String? _selectedClosureId;
  FtthClosureDiagramModel? _diagram;
  bool _loadingDiagram = false;

  Future<void> _loadDiagram(String closureId) async {
    setState(() {
      _selectedClosureId = closureId;
      _loadingDiagram = true;
    });
    try {
      final diag = await ref
          .read(networkFtthRepositoryProvider)
          .getClosureDiagram(closureId);
      if (mounted) {
        setState(() => _diagram = diag);
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
      if (mounted) setState(() => _loadingDiagram = false);
    }
  }

  void _showCreateClosureDialog() {
    final nameCtrl = TextEditingController();
    final traysCtrl = TextEditingController(text: '4');
    final capCtrl = TextEditingController(text: '48');
    String closureType = 'DOMO';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar Caixa de Emenda Óptica (CEO)'),
          content: SizedBox(
            width: 440,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Identificação da CEO *',
                    hintText: 'Ex: CEO-01-CENTRO',
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: closureType,
                  decoration: const InputDecoration(labelText: 'Formato Físico'),
                  items: const [
                    DropdownMenuItem(value: 'DOMO', child: Text('DOMO')),
                    DropdownMenuItem(
                        value: 'RETANGULAR', child: Text('RETANGULAR')),
                    DropdownMenuItem(
                        value: 'SUBTERRANEA', child: Text('SUBTERRÂNEA')),
                  ],
                  onChanged: (v) {
                    if (v != null) setModalState(() => closureType = v);
                  },
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: traysCtrl,
                        keyboardType: TextInputType.number,
                        decoration:
                            const InputDecoration(labelText: 'Qtd. Bandejas'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextField(
                        controller: capCtrl,
                        keyboardType: TextInputType.number,
                        decoration: const InputDecoration(
                            labelText: 'Capacidade de Fusões'),
                      ),
                    ),
                  ],
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
                final name = nameCtrl.text.trim();
                if (name.isEmpty) return;
                try {
                  final created = await ref
                      .read(networkFtthRepositoryProvider)
                      .createClosure(
                        name: name,
                        closureType: closureType,
                        trayCount: int.tryParse(traysCtrl.text.trim()) ?? 4,
                        capacityFusions:
                            int.tryParse(capCtrl.text.trim()) ?? 48,
                      );
                  ref.invalidate(ftthClosuresProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  await _loadDiagram(created.id);
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
              child: const Text('Salvar CEO'),
            ),
          ],
        ),
      ),
    );
  }

  void _showCreateSplitterDialog(String closureId, List<FtthCableModel> cables) {
    final nameCtrl = TextEditingController(text: 'SPLITTER-1X8-01');
    final fiberCtrl = TextEditingController(text: '1');
    String splitterType = 'BALANCED_1_8';
    String? inputCableId = cables.isNotEmpty ? cables.first.id : null;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Instalar Splitter Óptico na CEO'),
          content: SizedBox(
            width: 460,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Identificação do Splitter *',
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: splitterType,
                  decoration: const InputDecoration(
                    labelText: 'Tipo de Divisor (PLC Balanceado / FBT Desbalanceado)',
                  ),
                  items: const [
                    DropdownMenuItem(
                        value: 'BALANCED_1_2', child: Text('PLC Balanceado 1:2')),
                    DropdownMenuItem(
                        value: 'BALANCED_1_4', child: Text('PLC Balanceado 1:4')),
                    DropdownMenuItem(
                        value: 'BALANCED_1_8', child: Text('PLC Balanceado 1:8')),
                    DropdownMenuItem(
                        value: 'BALANCED_1_16', child: Text('PLC Balanceado 1:16')),
                    DropdownMenuItem(
                        value: 'UNBALANCED_90_10',
                        child: Text('FBT Desbalanceado 90/10')),
                    DropdownMenuItem(
                        value: 'UNBALANCED_80_20',
                        child: Text('FBT Desbalanceado 80/20')),
                    DropdownMenuItem(
                        value: 'UNBALANCED_70_30',
                        child: Text('FBT Desbalanceado 70/30')),
                  ],
                  onChanged: (v) {
                    if (v != null) setModalState(() => splitterType = v);
                  },
                ),
                const SizedBox(height: 12),
                if (cables.isNotEmpty)
                  DropdownButtonFormField<String?>(
                    initialValue: inputCableId,
                    decoration:
                        const InputDecoration(labelText: 'Cabo de Entrada'),
                    items: cables
                        .map((c) => DropdownMenuItem<String?>(
                              value: c.id,
                              child: Text('${c.name} (${c.fiberCount} FO)'),
                            ))
                        .toList(),
                    onChanged: (v) => setModalState(() => inputCableId = v),
                  ),
                const SizedBox(height: 12),
                TextField(
                  controller: fiberCtrl,
                  keyboardType: TextInputType.number,
                  decoration: const InputDecoration(
                      labelText: 'Fibra de Entrada (FO #)'),
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
                final name = nameCtrl.text.trim();
                if (name.isEmpty) return;
                try {
                  await ref.read(networkFtthRepositoryProvider).createSplitter(
                        closureId: closureId,
                        name: name,
                        splitterType: splitterType,
                        inputCableId: inputCableId,
                        inputFiberNumber: int.tryParse(fiberCtrl.text.trim()),
                      );
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  await _loadDiagram(closureId);
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
              child: const Text('Instalar Splitter'),
            ),
          ],
        ),
      ),
    );
  }

  void _showCreateFusionDialog(
    String closureId,
    List<FtthCableModel> cables,
    List<FtthSplitterModel> splitters,
  ) {
    if (cables.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text(
              'Cadastre cabos ópticos na aba "Topologia FTTH" antes de registrar fusões.'),
          backgroundColor: AppTheme.accentWarning,
        ),
      );
      return;
    }

    String sourceCableId = cables.first.id;
    String? targetCableId = cables.length > 1 ? cables[1].id : cables.first.id;
    String? targetSplitterId;
    final trayCtrl = TextEditingController(text: '1');
    final srcFiberCtrl = TextEditingController(text: '1');
    final tgtFiberCtrl = TextEditingController(text: '1');
    final lossCtrl = TextEditingController(text: '0.05');
    final descCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Registrar Fusão Óptica na Bandeja'),
          content: SizedBox(
            width: 480,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: trayCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Bandeja Nº *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: lossCtrl,
                          keyboardType: const TextInputType.numberWithOptions(
                              decimal: true),
                          decoration: const InputDecoration(
                              labelText: 'Perda na Fusão (dB)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        flex: 2,
                        child: DropdownButtonFormField<String>(
                          initialValue: sourceCableId,
                          decoration: const InputDecoration(
                              labelText: 'Cabo de Origem *'),
                          items: cables
                              .map((c) => DropdownMenuItem(
                                    value: c.id,
                                    child: Text(c.name),
                                  ))
                              .toList(),
                          onChanged: (v) {
                            if (v != null) {
                              setModalState(() => sourceCableId = v);
                            }
                          },
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: srcFiberCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'FO Origem # *'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        flex: 2,
                        child: DropdownButtonFormField<String?>(
                          initialValue: targetCableId,
                          decoration: const InputDecoration(
                              labelText: 'Cabo de Destino'),
                          items: [
                            const DropdownMenuItem<String?>(
                              value: null,
                              child: Text('Nenhum (Fusão em Splitter)'),
                            ),
                            ...cables.map((c) => DropdownMenuItem<String?>(
                                  value: c.id,
                                  child: Text(c.name),
                                )),
                          ],
                          onChanged: (v) =>
                              setModalState(() => targetCableId = v),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: tgtFiberCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'FO Destino #'),
                        ),
                      ),
                    ],
                  ),
                  if (splitters.isNotEmpty) ...[
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String?>(
                      initialValue: targetSplitterId,
                      decoration: const InputDecoration(
                          labelText: 'Ou Splitter de Destino na CEO'),
                      items: [
                        const DropdownMenuItem<String?>(
                          value: null,
                          child: Text('Nenhum splitter'),
                        ),
                        ...splitters.map((s) => DropdownMenuItem<String?>(
                              value: s.id,
                              child: Text('${s.name} (${s.splitterType})'),
                            )),
                      ],
                      onChanged: (v) =>
                          setModalState(() => targetSplitterId = v),
                    ),
                  ],
                  const SizedBox(height: 12),
                  TextField(
                    controller: descCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Observação da Fusão',
                      hintText: 'Ex: Alimentação primária CTO-01',
                    ),
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
                final srcFiber = int.tryParse(srcFiberCtrl.text.trim()) ?? 1;
                try {
                  await ref.read(networkFtthRepositoryProvider).createFusion(
                        closureId: closureId,
                        trayNumber: int.tryParse(trayCtrl.text.trim()) ?? 1,
                        sourceCableId: sourceCableId,
                        sourceFiberNumber: srcFiber,
                        targetCableId: targetCableId,
                        targetFiberNumber:
                            int.tryParse(tgtFiberCtrl.text.trim()),
                        targetSplitterId: targetSplitterId,
                        lossDb: double.tryParse(lossCtrl.text.trim()) ?? 0.05,
                        description: descCtrl.text.trim(),
                      );
                  ref.invalidate(ftthClosuresProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                  await _loadDiagram(closureId);
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
              child: const Text('Registrar Fusão'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _handleDeleteFusion(String fusionId, String closureId) async {
    try {
      await ref.read(networkFtthRepositoryProvider).deleteFusion(fusionId);
      ref.invalidate(ftthClosuresProvider);
      await _loadDiagram(closureId);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final closuresAsync = ref.watch(ftthClosuresProvider);
    final cablesAsync = ref.watch(ftthCablesProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Row(
            children: [
              const Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      'Caixas de Emenda Óptica (CEO), Splitters & Diagrama de Fusões',
                      style:
                          TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Bandejas de fusão, divisores PLC balanceados e FBT desbalanceados em barramento (90/10, 80/20)',
                      style:
                          TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
              ),
              ElevatedButton.icon(
                onPressed: _showCreateClosureDialog,
                icon: const Icon(Icons.add, size: 18),
                label: const Text('Nova Caixa de Emenda (CEO)'),
              ),
            ],
          ),
          const SizedBox(height: 16),

          closuresAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (closures) {
              if (closures.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(20),
                    child: Text(
                      'Nenhuma Caixa de Emenda Óptica (CEO) cadastrada. Clique em "Nova Caixa de Emenda (CEO)" para criar.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Wrap(
                spacing: 12,
                runSpacing: 12,
                children: closures.map((ceo) {
                  final isSelected = _selectedClosureId == ceo.id;
                  return InkWell(
                    onTap: () => _loadDiagram(ceo.id),
                    borderRadius: BorderRadius.circular(12),
                    child: Container(
                      width: 280,
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: AppTheme.darkSurface,
                        borderRadius: BorderRadius.circular(12),
                        border: Border.all(
                          color: isSelected
                              ? AppTheme.primaryBlue
                              : AppTheme.darkCard,
                          width: isSelected ? 2 : 1,
                        ),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Row(
                            mainAxisAlignment: MainAxisAlignment.spaceBetween,
                            children: [
                              Expanded(
                                child: Text(
                                  ceo.name,
                                  style: const TextStyle(
                                      fontWeight: FontWeight.bold,
                                      fontSize: 14),
                                  overflow: TextOverflow.ellipsis,
                                ),
                              ),
                              Text(
                                ceo.closureType,
                                style: const TextStyle(
                                    fontSize: 11, color: AppTheme.primaryBlue),
                              ),
                            ],
                          ),
                          const SizedBox(height: 6),
                          Text(
                            'Bandejas: ${ceo.trayCount} • Fusões: ${ceo.usedFusionsCount}/${ceo.capacityFusions}',
                            style: const TextStyle(fontSize: 12),
                          ),
                          const SizedBox(height: 8),
                          Align(
                            alignment: Alignment.centerRight,
                            child: Text(
                              isSelected
                                  ? 'Diagrama Aberto ▼'
                                  : 'Abrir Diagrama Unifilar →',
                              style: const TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryBlue,
                              ),
                            ),
                          ),
                        ],
                      ),
                    ),
                  );
                }).toList(),
              );
            },
          ),
          const SizedBox(height: 24),

          // Diagrama Unifilar da CEO selecionada
          if (_loadingDiagram)
            const Center(
              child: Padding(
                padding: EdgeInsets.all(24),
                child: CircularProgressIndicator(),
              ),
            )
          else if (_diagram != null) ...[
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Diagrama Interno — ${_diagram!.closure.name} (${_diagram!.closure.closureType})',
                              style: const TextStyle(
                                  fontSize: 16, fontWeight: FontWeight.bold),
                            ),
                            Text(
                              '${_diagram!.splitters.length} Splitter(s) • ${_diagram!.fusions.length} Fusão(ões) • ${_diagram!.connectedCtos.length} CTO(s) derivada(s)',
                              style: const TextStyle(
                                  fontSize: 12, color: AppTheme.textSecondary),
                            ),
                          ],
                        ),
                        Wrap(
                          spacing: 10,
                          runSpacing: 8,
                          children: [
                            OutlinedButton.icon(
                              onPressed: () {
                                final allCables =
                                    cablesAsync.valueOrNull ?? _diagram!.cables;
                                _showCreateSplitterDialog(
                                    _diagram!.closure.id, allCables);
                              },
                              icon: const Icon(Icons.call_split, size: 16),
                              label: const Text('Instalar Splitter'),
                            ),
                            ElevatedButton.icon(
                              onPressed: () {
                                final allCables =
                                    cablesAsync.valueOrNull ?? _diagram!.cables;
                                _showCreateFusionDialog(
                                  _diagram!.closure.id,
                                  allCables,
                                  _diagram!.splitters,
                                );
                              },
                              icon: const Icon(Icons.linear_scale, size: 16),
                              label: const Text('Registrar Fusão'),
                            ),
                          ],
                        ),
                      ],
                    ),
                    const Divider(height: 28),

                    // Splitters na CEO
                    const Text(
                      'Splitters Instalados na CEO',
                      style:
                          TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    if (_diagram!.splitters.isEmpty)
                      const Text(
                        'Nenhum splitter instalado nesta caixa de emenda.',
                        style: TextStyle(
                            fontSize: 12, color: AppTheme.textSecondary),
                      )
                    else
                      Wrap(
                        spacing: 10,
                        runSpacing: 8,
                        children: _diagram!.splitters
                            .map((s) => Chip(
                                  avatar: const Icon(Icons.call_split,
                                      size: 16, color: AppTheme.primaryBlue),
                                  label: Text(
                                    '${s.name} • ${s.splitterType} (${s.outputPorts} saídas, -${s.attenuationDb} dB)',
                                    style: const TextStyle(fontSize: 12),
                                  ),
                                ))
                            .toList(),
                      ),
                    const SizedBox(height: 20),

                    // Fusões nas Bandejas
                    const Text(
                      'Mapa de Fusões por Bandeja',
                      style:
                          TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 8),
                    if (_diagram!.fusions.isEmpty)
                      const Text(
                        'Nenhuma fusão óptica registrada nas bandejas desta CEO.',
                        style: TextStyle(
                            fontSize: 12, color: AppTheme.textSecondary),
                      )
                    else
                      Column(
                        children: _diagram!.fusions.map((f) {
                          final srcColor =
                              f.sourceFiberColor?.fiberColorName ?? '';
                          final tgtLabel = f.targetSplitterName != null
                              ? 'Splitter ${f.targetSplitterName}'
                              : '${f.targetCableName ?? "Cabo"} FO #${f.targetFiberNumber ?? "-"} (${f.targetFiberColor?.fiberColorName ?? ""})';
                          return ListTile(
                            dense: true,
                            contentPadding: EdgeInsets.zero,
                            leading: CircleAvatar(
                              radius: 14,
                              backgroundColor:
                                  AppTheme.primaryIndigo.withValues(alpha: 0.2),
                              child: Text(
                                'B${f.trayNumber}',
                                style: const TextStyle(
                                    fontSize: 11, fontWeight: FontWeight.bold),
                              ),
                            ),
                            title: Text(
                              '${f.sourceCableName} FO #${f.sourceFiberNumber} ($srcColor)  ➔  $tgtLabel',
                              style: const TextStyle(
                                  fontSize: 13, fontWeight: FontWeight.w600),
                            ),
                            subtitle: Text(
                              'Perda: ${f.lossDb.toStringAsFixed(2)} dB ${f.description != null ? "• ${f.description}" : ""}',
                              style: const TextStyle(
                                  fontSize: 11, color: AppTheme.textSecondary),
                            ),
                            trailing: IconButton(
                              tooltip: 'Desfazer Fusão',
                              icon: const Icon(Icons.delete_outline,
                                  size: 18, color: AppTheme.accentError),
                              onPressed: () => _handleDeleteFusion(
                                  f.id, _diagram!.closure.id),
                            ),
                          );
                        }).toList(),
                      ),
                  ],
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
