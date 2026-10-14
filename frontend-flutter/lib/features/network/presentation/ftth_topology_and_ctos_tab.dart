import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/network_ftth_repository.dart';

/// Aba 3: Topologia FTTH (POPs, Postes, Cabos, CTOs, Viabilidade GPS & Rastreamento Óptico LightPath).
class FtthTopologyAndCtosTab extends ConsumerStatefulWidget {
  const FtthTopologyAndCtosTab({super.key});

  @override
  ConsumerState<FtthTopologyAndCtosTab> createState() =>
      _FtthTopologyAndCtosTabState();
}

class _FtthTopologyAndCtosTabState
    extends ConsumerState<FtthTopologyAndCtosTab> {
  final _latCtrl = TextEditingController(text: '-23.550520');
  final _lonCtrl = TextEditingController(text: '-46.633308');
  final _maxDistCtrl = TextEditingController(text: '250');
  FtthFeasibilityResultModel? _feasibilityResult;
  bool _checkingFeasibility = false;

  @override
  void dispose() {
    _latCtrl.dispose();
    _lonCtrl.dispose();
    _maxDistCtrl.dispose();
    super.dispose();
  }

  Future<void> _handleCheckFeasibility() async {
    final lat = double.tryParse(_latCtrl.text.trim());
    final lon = double.tryParse(_lonCtrl.text.trim());
    final dist = double.tryParse(_maxDistCtrl.text.trim()) ?? 200.0;
    if (lat == null || lon == null) return;

    setState(() => _checkingFeasibility = true);
    try {
      final res =
          await ref.read(networkFtthRepositoryProvider).checkFeasibility(
                latitude: lat,
                longitude: lon,
                maxDistanceMeters: dist,
              );
      if (mounted) {
        setState(() => _feasibilityResult = res);
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
      if (mounted) setState(() => _checkingFeasibility = false);
    }
  }

  void _showCreatePopDialog() {
    final nameCtrl = TextEditingController();
    final addressCtrl = TextEditingController();
    final latCtrl = TextEditingController(text: '-23.550520');
    final lonCtrl = TextEditingController(text: '-46.633308');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Cadastrar POP / Central Óptica'),
        content: SizedBox(
          width: 440,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Nome do POP *',
                  hintText: 'Ex: POP Central Matriz',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: addressCtrl,
                decoration: const InputDecoration(labelText: 'Endereço Físico'),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: latCtrl,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: 'Latitude'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: lonCtrl,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration: const InputDecoration(labelText: 'Longitude'),
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
                await ref.read(networkFtthRepositoryProvider).createPop(
                      name: name,
                      address: addressCtrl.text.trim(),
                      latitude: double.tryParse(latCtrl.text.trim()),
                      longitude: double.tryParse(lonCtrl.text.trim()),
                    );
                ref.invalidate(ftthPopsProvider);
                if (ctx.mounted) Navigator.of(ctx).pop();
              } catch (e) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(e.toString().replaceFirst('Exception: ', '')),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            child: const Text('Salvar POP'),
          ),
        ],
      ),
    );
  }

  void _showCreatePoleDialog() {
    final codeCtrl = TextEditingController();
    final latCtrl = TextEditingController(text: '-23.551200');
    final lonCtrl = TextEditingController(text: '-46.634100');
    final reserveCtrl = TextEditingController(text: '15');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Cadastrar Poste Georreferenciado'),
        content: SizedBox(
          width: 440,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: codeCtrl,
                decoration: const InputDecoration(
                  labelText: 'Código de Identificação / Plaqueta *',
                  hintText: 'Ex: POSTE-CPFL-9021',
                ),
              ),
              const SizedBox(height: 12),
              Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: latCtrl,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration:
                          const InputDecoration(labelText: 'Latitude *'),
                    ),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: TextField(
                      controller: lonCtrl,
                      keyboardType:
                          const TextInputType.numberWithOptions(decimal: true),
                      decoration:
                          const InputDecoration(labelText: 'Longitude *'),
                    ),
                  ),
                ],
              ),
              const SizedBox(height: 12),
              TextField(
                controller: reserveCtrl,
                keyboardType: TextInputType.number,
                decoration: const InputDecoration(
                  labelText: 'Reserva Técnica Óptica no Poste (Metros)',
                ),
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
              final code = codeCtrl.text.trim();
              final lat = double.tryParse(latCtrl.text.trim());
              final lon = double.tryParse(lonCtrl.text.trim());
              if (code.isEmpty || lat == null || lon == null) return;
              try {
                await ref.read(networkFtthRepositoryProvider).createPole(
                      code: code,
                      latitude: lat,
                      longitude: lon,
                      reservationMeters:
                          int.tryParse(reserveCtrl.text.trim()) ?? 0,
                    );
                ref.invalidate(ftthPolesProvider);
                if (ctx.mounted) Navigator.of(ctx).pop();
              } catch (e) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(e.toString().replaceFirst('Exception: ', '')),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            child: const Text('Salvar Poste'),
          ),
        ],
      ),
    );
  }

  void _showCreateCableDialog() {
    final nameCtrl = TextEditingController();
    final fibersCtrl = TextEditingController(text: '24');
    final tubesCtrl = TextEditingController(text: '2');
    final lenCtrl = TextEditingController(text: '450');
    String cableType = 'ALIMENTADOR';
    String colorStandard = 'ABNT_NBR_14106';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar Cabo Óptico FTTH'),
          content: SizedBox(
            width: 480,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nome do Cabo *',
                    hintText: 'Ex: CABO-TRONCAL-01',
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        initialValue: cableType,
                        decoration:
                            const InputDecoration(labelText: 'Tipo de Cabo'),
                        items: const [
                          DropdownMenuItem(
                              value: 'ALIMENTADOR', child: Text('ALIMENTADOR')),
                          DropdownMenuItem(
                              value: 'DISTRIBUICAO',
                              child: Text('DISTRIBUICAO')),
                          DropdownMenuItem(value: 'DROP', child: Text('DROP')),
                        ],
                        onChanged: (v) {
                          if (v != null) setModalState(() => cableType = v);
                        },
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        initialValue: colorStandard,
                        decoration: const InputDecoration(
                            labelText: 'Padrão de Cores'),
                        items: const [
                          DropdownMenuItem(
                            value: 'ABNT_NBR_14106',
                            child: Text('ABNT NBR 14106 (Verde #1)'),
                          ),
                          DropdownMenuItem(
                            value: 'TIA_EIA_598',
                            child: Text('TIA/EIA-598 (Azul #1)'),
                          ),
                        ],
                        onChanged: (v) {
                          if (v != null) {
                            setModalState(() => colorStandard = v);
                          }
                        },
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: fibersCtrl,
                        keyboardType: TextInputType.number,
                        decoration:
                            const InputDecoration(labelText: 'Qtd. Fibras (FO)'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextField(
                        controller: tubesCtrl,
                        keyboardType: TextInputType.number,
                        decoration:
                            const InputDecoration(labelText: 'Tubos Loose'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextField(
                        controller: lenCtrl,
                        keyboardType: const TextInputType.numberWithOptions(
                            decimal: true),
                        decoration:
                            const InputDecoration(labelText: 'Metragem (m)'),
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
                  await ref.read(networkFtthRepositoryProvider).createCable(
                        name: name,
                        cableType: cableType,
                        fiberCount: int.tryParse(fibersCtrl.text.trim()) ?? 12,
                        tubeCount: int.tryParse(tubesCtrl.text.trim()) ?? 1,
                        colorStandard: colorStandard,
                        lengthMeters:
                            double.tryParse(lenCtrl.text.trim()) ?? 100.0,
                      );
                  ref.invalidate(ftthCablesProvider);
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
              child: const Text('Salvar Cabo'),
            ),
          ],
        ),
      ),
    );
  }

  void _showCreateCtoDialog() {
    final nameCtrl = TextEditingController();
    final latCtrl = TextEditingController(text: '-23.550520');
    final lonCtrl = TextEditingController(text: '-46.633308');
    final portsCtrl = TextEditingController(text: '16');
    String splitterType = 'BALANCED_1_16';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar Caixa de Atendimento (CTO)'),
          content: SizedBox(
            width: 460,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Identificação da CTO *',
                    hintText: 'Ex: CTO-CENTRO-01',
                  ),
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: latCtrl,
                        keyboardType: const TextInputType.numberWithOptions(
                            decimal: true),
                        decoration:
                            const InputDecoration(labelText: 'Latitude *'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: TextField(
                        controller: lonCtrl,
                        keyboardType: const TextInputType.numberWithOptions(
                            decimal: true),
                        decoration:
                            const InputDecoration(labelText: 'Longitude *'),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Row(
                  children: [
                    Expanded(
                      child: TextField(
                        controller: portsCtrl,
                        keyboardType: TextInputType.number,
                        decoration:
                            const InputDecoration(labelText: 'Total de Portas'),
                      ),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: DropdownButtonFormField<String>(
                        initialValue: splitterType,
                        decoration: const InputDecoration(
                            labelText: 'Splitter de Atendimento'),
                        items: const [
                          DropdownMenuItem(
                              value: 'BALANCED_1_8',
                              child: Text('PLC Balanceado 1:8')),
                          DropdownMenuItem(
                              value: 'BALANCED_1_16',
                              child: Text('PLC Balanceado 1:16')),
                        ],
                        onChanged: (v) {
                          if (v != null) setModalState(() => splitterType = v);
                        },
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
                final lat = double.tryParse(latCtrl.text.trim());
                final lon = double.tryParse(lonCtrl.text.trim());
                if (name.isEmpty || lat == null || lon == null) return;
                try {
                  await ref.read(networkFtthRepositoryProvider).createCto(
                        name: name,
                        latitude: lat,
                        longitude: lon,
                        totalPorts: int.tryParse(portsCtrl.text.trim()) ?? 16,
                        splitterType: splitterType,
                      );
                  ref.invalidate(ftthCtosProvider);
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
              child: const Text('Salvar CTO'),
            ),
          ],
        ),
      ),
    );
  }

  Future<void> _showCtoPortsAndLightPathModal(FtthCtoModel ctoSummary) async {
    try {
      final detailed =
          await ref.read(networkFtthRepositoryProvider).getCtoById(ctoSummary.id);
      if (!mounted) return;

      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Text(
              'Portas & Rastreamento Óptico — ${detailed.name} (${detailed.occupiedPortsCount}/${detailed.totalPorts} ocupadas)'),
          content: SizedBox(
            width: 620,
            child: detailed.ports.isEmpty
                ? const Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Nenhuma porta individual detalhada para esta CTO.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  )
                : SingleChildScrollView(
                    child: Column(
                      mainAxisSize: MainAxisSize.min,
                      children: detailed.ports.map((port) {
                        final isOccupied = port.status == 'OCUPADA';
                        return ListTile(
                          dense: true,
                          leading: CircleAvatar(
                            radius: 14,
                            backgroundColor: isOccupied
                                ? AppTheme.primaryBlue.withValues(alpha: 0.2)
                                : AppTheme.accentGreen.withValues(alpha: 0.2),
                            child: Text(
                              '${port.portNumber}',
                              style: TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                color: isOccupied
                                    ? AppTheme.primaryBlue
                                    : AppTheme.accentGreen,
                              ),
                            ),
                          ),
                          title: Text(
                            isOccupied
                                ? '${port.customerName ?? "Assinante"} • SN: ${port.onuSerial ?? "-"}'
                                : 'Porta ${port.status}',
                            style: const TextStyle(
                                fontSize: 13, fontWeight: FontWeight.w600),
                          ),
                          subtitle: Text(
                            port.pppoeUser != null
                                ? 'PPPoE: ${port.pppoeUser}'
                                : 'Sem circuito ativo',
                            style: const TextStyle(
                                fontSize: 11, color: AppTheme.textSecondary),
                          ),
                          trailing: OutlinedButton.icon(
                            onPressed: () => _handleTraceLightPath(port.id),
                            icon: const Icon(Icons.timeline, size: 15),
                            label: const Text('LightPath'),
                          ),
                        );
                      }).toList(),
                    ),
                  ),
          ),
          actions: [
            ElevatedButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Fechar'),
            ),
          ],
        ),
      );
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

  Future<void> _handleTraceLightPath(String ctoPortId) async {
    try {
      final trace = await ref
          .read(networkFtthRepositoryProvider)
          .traceLightPath(ctoPortId);
      if (!mounted) return;
      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Caminho Óptico Unifilar (LightPath Trace)'),
          content: SizedBox(
            width: 520,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 16,
                    runSpacing: 8,
                    children: [
                      Chip(
                        label: Text(
                          trace.reachedSource
                              ? 'Continuidade até POP: ${trace.sourcePopName ?? "OK"}'
                              : 'Caminho Incompleto / Sem Fusão',
                        ),
                      ),
                      Chip(
                        label: Text(
                            'Atenuação Acumulada: ${trace.totalAttenuationDb.toStringAsFixed(2)} dB'),
                      ),
                      Chip(
                        label: Text(
                            'Potência RX Estimada: ${trace.estimatedRxPowerDbm.toStringAsFixed(2)} dBm'),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  ...trace.nodes.map(
                    (node) => ListTile(
                      dense: true,
                      leading: const Icon(Icons.fiber_manual_record,
                          size: 14, color: AppTheme.primaryBlue),
                      title: Text(
                        '${node.elementType}: ${node.name}',
                        style: const TextStyle(
                            fontSize: 13, fontWeight: FontWeight.bold),
                      ),
                      subtitle: Text(
                        '${node.details ?? ""} • +${node.addedAttenuationDb.toStringAsFixed(2)} dB (Acum: ${node.cumulativeAttenuationDb.toStringAsFixed(2)} dB)',
                        style: const TextStyle(
                            fontSize: 11, color: AppTheme.textSecondary),
                      ),
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            ElevatedButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Fechar'),
            ),
          ],
        ),
      );
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
    final popsAsync = ref.watch(ftthPopsProvider);
    final polesAsync = ref.watch(ftthPolesProvider);
    final cablesAsync = ref.watch(ftthCablesProvider);
    final ctosAsync = ref.watch(ftthCtosProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Barra de Ações Rápidas da Topologia
          Wrap(
            spacing: 12,
            runSpacing: 12,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Inventário Georreferenciado FTTH & Viabilidade Óptica',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Gestão de POPs, Postes, Cabos ABNT NBR 14106, CTOs, consulta de viabilidade GPS e rastreamento LightPath',
                    style:
                        TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
              Wrap(
                spacing: 8,
                runSpacing: 8,
                children: [
                  OutlinedButton.icon(
                    onPressed: _showCreatePopDialog,
                    icon: const Icon(Icons.domain_add, size: 16),
                    label: const Text('Novo POP'),
                  ),
                  OutlinedButton.icon(
                    onPressed: _showCreatePoleDialog,
                    icon: const Icon(Icons.location_on_outlined, size: 16),
                    label: const Text('Novo Poste'),
                  ),
                  OutlinedButton.icon(
                    onPressed: _showCreateCableDialog,
                    icon: const Icon(Icons.cable, size: 16),
                    label: const Text('Novo Cabo'),
                  ),
                  ElevatedButton.icon(
                    onPressed: _showCreateCtoDialog,
                    icon: const Icon(Icons.settings_input_hdmi_outlined,
                        size: 16),
                    label: const Text('Nova CTO'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 20),

          // Simulador de Viabilidade Técnica por Coordenadas GPS
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Consulta de Viabilidade Técnica FTTH por Coordenadas GPS',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 12,
                    runSpacing: 12,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      SizedBox(
                        width: 180,
                        child: TextField(
                          controller: _latCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Latitude',
                            isDense: true,
                          ),
                        ),
                      ),
                      SizedBox(
                        width: 180,
                        child: TextField(
                          controller: _lonCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Longitude',
                            isDense: true,
                          ),
                        ),
                      ),
                      SizedBox(
                        width: 150,
                        child: TextField(
                          controller: _maxDistCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Raio Máx. (m)',
                            isDense: true,
                          ),
                        ),
                      ),
                      ElevatedButton.icon(
                        onPressed: _checkingFeasibility
                            ? null
                            : _handleCheckFeasibility,
                        icon: const Icon(Icons.my_location, size: 16),
                        label: Text(_checkingFeasibility
                            ? 'Consultando...'
                            : 'Verificar Viabilidade'),
                      ),
                    ],
                  ),
                  if (_feasibilityResult != null) ...[
                    const SizedBox(height: 12),
                    Container(
                      padding: const EdgeInsets.all(12),
                      decoration: BoxDecoration(
                        color: (_feasibilityResult!.viable
                                ? AppTheme.accentGreen
                                : AppTheme.accentWarning)
                            .withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            _feasibilityResult!.viable
                                ? 'VIÁVEL: ${_feasibilityResult!.viableCtosCount} CTO(s) com porta livre dentro do raio.'
                                : 'INVIAVEL NO RAIO: Nenhuma CTO com porta disponível localizada.',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              color: _feasibilityResult!.viable
                                  ? AppTheme.accentGreen
                                  : AppTheme.accentWarning,
                            ),
                          ),
                          if (_feasibilityResult!.nearbyCtos.isNotEmpty) ...[
                            const SizedBox(height: 6),
                            Wrap(
                              spacing: 12,
                              runSpacing: 6,
                              children: _feasibilityResult!.nearbyCtos
                                  .map((item) => Chip(
                                        label: Text(
                                          '${item.cto.name} • ${item.distanceMeters.toStringAsFixed(1)}m (${item.freePorts} livres)',
                                          style: const TextStyle(fontSize: 12),
                                        ),
                                      ))
                                  .toList(),
                            ),
                          ],
                        ],
                      ),
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Seção CTOs
          const Text(
            'Caixas de Atendimento ao Cliente (CTOs)',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          ctosAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (ctos) {
              if (ctos.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Nenhuma CTO cadastrada.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Wrap(
                spacing: 12,
                runSpacing: 12,
                children: ctos.map((cto) {
                  return Container(
                    width: 290,
                    padding: const EdgeInsets.all(16),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppTheme.darkCard),
                    ),
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Row(
                          mainAxisAlignment: MainAxisAlignment.spaceBetween,
                          children: [
                            Expanded(
                              child: Text(
                                cto.name,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold, fontSize: 14),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Text(
                              '${cto.occupancyPercentage.toStringAsFixed(0)}% Ocup.',
                              style: const TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryBlue,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 6),
                        Text(
                          'Portas: ${cto.occupiedPortsCount}/${cto.totalPorts} (${cto.freePortsCount} livres)',
                          style: const TextStyle(fontSize: 12),
                        ),
                        Text(
                          'Splitter: ${cto.splitterType} • GPS: ${cto.latitude.toStringAsFixed(4)}, ${cto.longitude.toStringAsFixed(4)}',
                          style: const TextStyle(
                              fontSize: 11, color: AppTheme.textSecondary),
                          overflow: TextOverflow.ellipsis,
                        ),
                        const SizedBox(height: 10),
                        Align(
                          alignment: Alignment.centerRight,
                          child: TextButton.icon(
                            onPressed: () =>
                                _showCtoPortsAndLightPathModal(cto),
                            icon: const Icon(Icons.visibility_outlined,
                                size: 16),
                            label: const Text('Portas & LightPath'),
                          ),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          ),
          const SizedBox(height: 24),

          // Seção Cabos Ópticos, POPs e Postes
          const Text(
            'Cabos Ópticos, POPs & Postes Georreferenciados',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          cablesAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (cables) {
              final pops = popsAsync.valueOrNull ?? [];
              final poles = polesAsync.valueOrNull ?? [];
              return Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 12,
                    runSpacing: 8,
                    children: [
                      Chip(label: Text('POPs Cadastrados: ${pops.length}')),
                      Chip(label: Text('Postes Mapeados: ${poles.length}')),
                      Chip(label: Text('Cabos Ópticos: ${cables.length}')),
                    ],
                  ),
                  const SizedBox(height: 10),
                  if (cables.isEmpty)
                    const Card(
                      child: Padding(
                        padding: EdgeInsets.all(16),
                        child: Text(
                          'Nenhum cabo óptico cadastrado.',
                          style: TextStyle(color: AppTheme.textSecondary),
                        ),
                      ),
                    )
                  else
                    Wrap(
                      spacing: 12,
                      runSpacing: 12,
                      children: cables.map((cable) {
                        return Container(
                          width: 280,
                          padding: const EdgeInsets.all(14),
                          decoration: BoxDecoration(
                            color: AppTheme.darkSurface,
                            borderRadius: BorderRadius.circular(12),
                            border: Border.all(color: AppTheme.darkCard),
                          ),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                cable.name,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold, fontSize: 14),
                                overflow: TextOverflow.ellipsis,
                              ),
                              const SizedBox(height: 4),
                              Text(
                                '${cable.cableType} • ${cable.fiberCount} FO (${cable.tubeCount} tubos)',
                                style: const TextStyle(
                                    fontSize: 12, color: AppTheme.primaryBlue),
                              ),
                              Text(
                                '${cable.lengthMeters.toStringAsFixed(0)}m • ${cable.colorStandard} • ${cable.attenuationDbPerKm} dB/km',
                                style: const TextStyle(
                                    fontSize: 11,
                                    color: AppTheme.textSecondary),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        );
                      }).toList(),
                    ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }
}
