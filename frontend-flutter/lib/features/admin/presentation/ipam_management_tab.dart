import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/radius_ipam_marcocivil_repository.dart';

/// Aba 4 de `/admin/nas`: Gestão de Endereçamento IP (IPAM), ASNs, VRFs, Sub-redes IPv4/IPv6 e Split VLSM.
class IpamManagementTab extends ConsumerStatefulWidget {
  const IpamManagementTab({super.key});

  @override
  ConsumerState<IpamManagementTab> createState() => _IpamManagementTabState();
}

class _IpamManagementTabState extends ConsumerState<IpamManagementTab> {
  final _calcCidrCtrl = TextEditingController(text: '200.150.10.0/24');
  SubnetCalculationResultModel? _calcResult;
  bool _calculating = false;

  @override
  void dispose() {
    _calcCidrCtrl.dispose();
    super.dispose();
  }

  Future<void> _handleCalculateCidr() async {
    final cidr = _calcCidrCtrl.text.trim();
    if (cidr.isEmpty) return;

    setState(() => _calculating = true);
    try {
      final res = await ref
          .read(radiusIpamMarcoCivilRepositoryProvider)
          .calculateCidr(cidr);
      if (mounted) {
        setState(() => _calcResult = res);
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
      if (mounted) setState(() => _calculating = false);
    }
  }

  void _showCreateAsnDialog() {
    final asnCtrl = TextEditingController(text: '265000');
    final nameCtrl = TextEditingController();
    String rir = 'REGISTRO_BR';

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar ASN (Sistema Autônomo BGP)'),
          content: SizedBox(
            width: 420,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: asnCtrl,
                  keyboardType: TextInputType.number,
                  decoration:
                      const InputDecoration(labelText: 'Número do ASN *'),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: nameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Razão Social / Nome do Sistema Autônomo *',
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: rir,
                  decoration:
                      const InputDecoration(labelText: 'RIR Registrador'),
                  items: const [
                    DropdownMenuItem(
                        value: 'REGISTRO_BR', child: Text('REGISTRO.BR (NIC.br)')),
                    DropdownMenuItem(value: 'LACNIC', child: Text('LACNIC')),
                    DropdownMenuItem(value: 'ARIN', child: Text('ARIN')),
                    DropdownMenuItem(value: 'RIPE', child: Text('RIPE NCC')),
                  ],
                  onChanged: (v) {
                    if (v != null) setModalState(() => rir = v);
                  },
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
                final asnNum = int.tryParse(asnCtrl.text.trim());
                final name = nameCtrl.text.trim();
                if (asnNum == null || name.isEmpty) return;
                try {
                  await ref
                      .read(radiusIpamMarcoCivilRepositoryProvider)
                      .createAsn(asn: asnNum, name: name, rir: rir);
                  ref.invalidate(ipamAsnsProvider);
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
              child: const Text('Salvar ASN'),
            ),
          ],
        ),
      ),
    );
  }

  void _showCreateVrfDialog() {
    final nameCtrl = TextEditingController();
    final rdCtrl = TextEditingController(text: '265000:100');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Nova VRF (Virtual Routing and Forwarding)'),
        content: SizedBox(
          width: 420,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: nameCtrl,
                decoration: const InputDecoration(
                  labelText: 'Nome da VRF *',
                  hintText: 'Ex: VRF-CLIENTES-B2C',
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: rdCtrl,
                decoration: const InputDecoration(
                  labelText: 'Route Distinguisher (RD)',
                  hintText: 'Ex: 265000:100',
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
              final name = nameCtrl.text.trim();
              if (name.isEmpty) return;
              try {
                await ref
                    .read(radiusIpamMarcoCivilRepositoryProvider)
                    .createVrf(name: name, rd: rdCtrl.text.trim());
                ref.invalidate(ipamVrfsProvider);
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
            child: const Text('Salvar VRF'),
          ),
        ],
      ),
    );
  }

  void _showCreateSubnetDialog() {
    final cidrCtrl = TextEditingController();
    final poolCtrl = TextEditingController();
    final descCtrl = TextEditingController();
    String category = 'CUSTOMER_ACCESS';
    bool isPool = true;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Cadastrar Sub-rede IPv4 ou IPv6 no IPAM'),
          content: SizedBox(
            width: 460,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                TextField(
                  controller: cidrCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Bloco CIDR (IPv4 ou IPv6) *',
                    hintText: 'Ex: 100.64.0.0/22 ou 2804:1234::/48',
                  ),
                ),
                const SizedBox(height: 12),
                DropdownButtonFormField<String>(
                  initialValue: category,
                  decoration:
                      const InputDecoration(labelText: 'Categoria Operacional'),
                  items: const [
                    DropdownMenuItem(
                        value: 'CUSTOMER_ACCESS',
                        child: Text('CUSTOMER_ACCESS (Clientes PPPoE)')),
                    DropdownMenuItem(
                        value: 'CGNAT', child: Text('CGNAT (RFC 6598)')),
                    DropdownMenuItem(
                        value: 'INFRASTRUCTURE',
                        child: Text('INFRASTRUCTURE (Backbone)')),
                    DropdownMenuItem(
                        value: 'MANAGEMENT',
                        child: Text('MANAGEMENT (Gerência OLTs/BNGs)')),
                    DropdownMenuItem(
                        value: 'LOOPBACK', child: Text('LOOPBACK')),
                  ],
                  onChanged: (v) {
                    if (v != null) setModalState(() => category = v);
                  },
                ),
                const SizedBox(height: 8),
                SwitchListTile(
                  contentPadding: EdgeInsets.zero,
                  title: const Text('Habilitar como Pool RADIUS / DHCP'),
                  value: isPool,
                  onChanged: (v) => setModalState(() => isPool = v),
                ),
                if (isPool)
                  TextField(
                    controller: poolCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Nome do Pool no BNG',
                      hintText: 'Ex: pool-cgnat-01',
                    ),
                  ),
                const SizedBox(height: 12),
                TextField(
                  controller: descCtrl,
                  decoration: const InputDecoration(labelText: 'Descrição'),
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
                final cidr = cidrCtrl.text.trim();
                if (cidr.isEmpty) return;
                try {
                  await ref
                      .read(radiusIpamMarcoCivilRepositoryProvider)
                      .createSubnet(
                        cidr: cidr,
                        category: category,
                        isPool: isPool,
                        poolName:
                            poolCtrl.text.trim().isEmpty ? null : poolCtrl.text.trim(),
                        description:
                            descCtrl.text.trim().isEmpty ? null : descCtrl.text.trim(),
                      );
                  ref.invalidate(ipamSubnetsProvider);
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
              child: const Text('Salvar Sub-rede'),
            ),
          ],
        ),
      ),
    );
  }

  void _showSplitSubnetDialog(IpamSubnetModel subnet) {
    final targetPrefixCtrl =
        TextEditingController(text: '${subnet.prefixLength + 2}');

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: Text('Dividir Sub-rede (Split VLSM) — ${subnet.cidr}'),
        content: SizedBox(
          width: 420,
          child: TextField(
            controller: targetPrefixCtrl,
            keyboardType: TextInputType.number,
            decoration: const InputDecoration(
              labelText: 'Novo Prefixo Alvo (ex: /26, /28, /56) *',
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
              final target = int.tryParse(targetPrefixCtrl.text.trim());
              if (target == null) return;
              try {
                final res = await ref
                    .read(radiusIpamMarcoCivilRepositoryProvider)
                    .splitSubnet(
                      subnetId: subnet.id,
                      targetPrefixLength: target,
                      createSubnets: true,
                    );
                ref.invalidate(ipamSubnetsProvider);
                if (ctx.mounted) {
                  Navigator.of(ctx).pop();
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(
                          '${res.totalSubnetsGenerated} sub-redes /${res.targetPrefixLength} geradas com sucesso.'),
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
            child: const Text('Dividir & Persistir'),
          ),
        ],
      ),
    );
  }

  Future<void> _showSubnetIpsModal(IpamSubnetModel subnet) async {
    try {
      final repo = ref.read(radiusIpamMarcoCivilRepositoryProvider);
      final ips = await repo.getIpsBySubnet(subnet.id);
      final nextIp = await repo.getNextAvailableIp(subnet.id);
      if (!mounted) return;

      final ipCtrl = TextEditingController(text: nextIp ?? '');
      final dnsCtrl = TextEditingController();

      showDialog(
        context: context,
        builder: (ctx) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Text('Inventário de IPs — ${subnet.cidr}'),
          content: SizedBox(
            width: 560,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Próximo IP Livre Sugerido: ${nextIp ?? "Sub-rede esgotada"}',
                    style: const TextStyle(
                        fontSize: 13,
                        fontWeight: FontWeight.bold,
                        color: AppTheme.accentGreen),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: ipCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Endereço IP para Alocar',
                            isDense: true,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: TextField(
                          controller: dnsCtrl,
                          decoration: const InputDecoration(
                            labelText: 'DNS Reverso / Hostname',
                            isDense: true,
                          ),
                        ),
                      ),
                      const SizedBox(width: 10),
                      ElevatedButton(
                        onPressed: () async {
                          final ipAddr = ipCtrl.text.trim();
                          if (ipAddr.isEmpty) return;
                          await repo.registerIp(
                            subnetId: subnet.id,
                            ipAddress: ipAddr,
                            dnsName: dnsCtrl.text.trim().isEmpty
                                ? null
                                : dnsCtrl.text.trim(),
                          );
                          ref.invalidate(ipamSubnetsProvider);
                          if (ctx.mounted) Navigator.of(ctx).pop();
                        },
                        child: const Text('Alocar IP'),
                      ),
                    ],
                  ),
                  const Divider(height: 24),
                  if (ips.isEmpty)
                    const Text(
                      'Nenhum IP individual registrado nesta sub-rede.',
                      style:
                          TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    )
                  else
                    ...ips.map((ip) => ListTile(
                          dense: true,
                          contentPadding: EdgeInsets.zero,
                          title: Text(
                            '${ip.ipAddress} (${ip.status})',
                            style: const TextStyle(
                                fontSize: 13, fontWeight: FontWeight.bold),
                          ),
                          subtitle: Text(
                            '${ip.dnsName ?? "Sem DNS"} • ${ip.assignedToLabel ?? ip.description ?? ""}',
                            style: const TextStyle(
                                fontSize: 11, color: AppTheme.textSecondary),
                          ),
                        )),
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
    final asnsAsync = ref.watch(ipamAsnsProvider);
    final vrfsAsync = ref.watch(ipamVrfsProvider);
    final subnetsAsync = ref.watch(ipamSubnetsProvider);

    return SingleChildScrollView(
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
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'IPAM — Endereçamento IPv4/IPv6, Sub-redes, ASNs & VRFs',
                    style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Gestão de blocos Registro.br/LACNIC, divisão VLSM, calculadora CIDR e inventário de IPs alocados',
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
                    onPressed: _showCreateAsnDialog,
                    icon: const Icon(Icons.public, size: 16),
                    label: const Text('Novo ASN'),
                  ),
                  OutlinedButton.icon(
                    onPressed: _showCreateVrfDialog,
                    icon: const Icon(Icons.alt_route, size: 16),
                    label: const Text('Nova VRF'),
                  ),
                  ElevatedButton.icon(
                    onPressed: _showCreateSubnetDialog,
                    icon: const Icon(Icons.add, size: 18),
                    label: const Text('Nova Sub-rede CIDR'),
                  ),
                ],
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Calculadora CIDR
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text(
                    'Calculadora Matemática CIDR (IPv4 / IPv6)',
                    style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                  ),
                  const SizedBox(height: 10),
                  Wrap(
                    spacing: 12,
                    runSpacing: 10,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      SizedBox(
                        width: 260,
                        child: TextField(
                          controller: _calcCidrCtrl,
                          decoration: const InputDecoration(
                            labelText: 'Bloco CIDR',
                            isDense: true,
                          ),
                        ),
                      ),
                      ElevatedButton.icon(
                        onPressed: _calculating ? null : _handleCalculateCidr,
                        icon: const Icon(Icons.calculate_outlined, size: 18),
                        label: Text(
                            _calculating ? 'Calculando...' : 'Calcular Bloco'),
                      ),
                    ],
                  ),
                  if (_calcResult != null) ...[
                    const SizedBox(height: 10),
                    Wrap(
                      spacing: 14,
                      runSpacing: 6,
                      children: [
                        Chip(
                            label: Text(
                                '${_calcResult!.ipVersion} • Rede: ${_calcResult!.networkAddress}')),
                        Chip(label: Text('Máscara: ${_calcResult!.netmask}')),
                        Chip(
                            label: Text(
                                'Range Útil: ${_calcResult!.firstUsableIp} a ${_calcResult!.lastUsableIp}')),
                        Chip(
                            label: Text(
                                'Hosts Úteis: ${_calcResult!.usableHosts} / ${_calcResult!.totalHosts}')),
                      ],
                    ),
                  ],
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),

          // ASNs & VRFs
          Wrap(
            spacing: 12,
            runSpacing: 8,
            children: [
              ...(asnsAsync.valueOrNull ?? []).map((a) => Chip(
                    avatar: const Icon(Icons.public,
                        size: 16, color: AppTheme.primaryBlue),
                    label: Text('AS${a.asn} — ${a.name} (${a.rir})'),
                  )),
              ...(vrfsAsync.valueOrNull ?? []).map((v) => Chip(
                    avatar: const Icon(Icons.alt_route,
                        size: 16, color: AppTheme.accentGreen),
                    label: Text('VRF: ${v.name} (RD ${v.rd ?? "-"})'),
                  )),
            ],
          ),
          const SizedBox(height: 16),

          // Lista de Sub-redes
          const Text(
            'Sub-redes Cadastradas & Utilização de Endereços',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          subnetsAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (subnets) {
              if (subnets.isEmpty) {
                return const Card(
                  child: Padding(
                    padding: EdgeInsets.all(16),
                    child: Text(
                      'Nenhuma sub-rede cadastrada no IPAM.',
                      style: TextStyle(color: AppTheme.textSecondary),
                    ),
                  ),
                );
              }
              return Wrap(
                spacing: 12,
                runSpacing: 12,
                children: subnets.map((sub) {
                  return Container(
                    width: 320,
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
                                sub.cidr,
                                style: const TextStyle(
                                    fontWeight: FontWeight.bold, fontSize: 15),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ),
                            Text(
                              '${sub.utilizationPercentage.toStringAsFixed(1)}% Uso',
                              style: const TextStyle(
                                fontSize: 11,
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryBlue,
                              ),
                            ),
                          ],
                        ),
                        const SizedBox(height: 4),
                        Text(
                          '${sub.ipVersion} • ${sub.category} • ${sub.allocatedHosts}/${sub.totalHosts} IPs',
                          style: const TextStyle(
                              fontSize: 12, color: AppTheme.textSecondary),
                        ),
                        if (sub.poolName != null)
                          Text(
                            'Pool: ${sub.poolName}',
                            style: const TextStyle(
                                fontSize: 11, color: AppTheme.accentGreen),
                          ),
                        const SizedBox(height: 10),
                        SizedBox(
                          width: double.infinity,
                          child: Wrap(
                            alignment: WrapAlignment.end,
                            spacing: 8,
                            runSpacing: 8,
                            children: [
                              TextButton(
                                onPressed: () => _showSplitSubnetDialog(sub),
                                child: const Text('Split VLSM'),
                              ),
                              ElevatedButton(
                                onPressed: () => _showSubnetIpsModal(sub),
                                child: const Text('Inventário IPs'),
                              ),
                            ],
                          ),
                        ),
                      ],
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
