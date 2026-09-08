import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/nas_model.dart';
import '../data/nas_notifier.dart';

/// Tela de Gestão de Concentradores BNG / NAS (MikroTik CHR & Huawei).
class NasManagementScreen extends ConsumerWidget {
  const NasManagementScreen({super.key});

  void _openNasModal(BuildContext context, WidgetRef ref, [NasModel? nas]) {
    showDialog(
      context: context,
      barrierDismissible: false,
      builder: (ctx) => _NasFormDialog(nas: nas),
    );
  }

  void _confirmDelete(BuildContext context, WidgetRef ref, NasModel nas) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(14)),
        title: Row(
          children: const [
            Icon(Icons.warning_amber_rounded, color: AppTheme.accentError),
            SizedBox(width: 10),
            Text('Excluir Concentrador'),
          ],
        ),
        content: Text(
          'Tem certeza que deseja remover o concentrador "${nas.shortname}" (${nas.nasname})?\n'
          'As autenticações PPPoE provenientes deste IP passarão a ser rejeitadas pelo FreeRADIUS.',
          style: const TextStyle(fontSize: 13, height: 1.4),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(ctx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentError,
              foregroundColor: Colors.white,
            ),
            onPressed: () async {
              Navigator.pop(ctx);
              try {
                await ref.read(nasListProvider.notifier).deleteNas(nas.id!);
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    const SnackBar(
                      content: Text('Concentrador removido com sucesso!'),
                      backgroundColor: AppTheme.accentGreen,
                    ),
                  );
                }
              } catch (e) {
                if (context.mounted) {
                  ScaffoldMessenger.of(context).showSnackBar(
                    SnackBar(
                      content: Text('Erro ao remover concentrador: $e'),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            child: const Text('Excluir'),
          ),
        ],
      ),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final nasListAsync = ref.watch(nasListProvider);

    return Scaffold(
      backgroundColor: AppTheme.darkBg,
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Cabeçalho
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: const [
                      Text(
                        'Concentradores de Acesso (BNG / NAS)',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Roteadores de borda para autenticação PPPoE/IPoE no FreeRADIUS com suporte a MikroTik e Huawei',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                const SizedBox(width: 12),
                OutlinedButton.icon(
                  onPressed: () => ref.read(nasListProvider.notifier).loadNasList(),
                  icon: const Icon(Icons.refresh, size: 18),
                  label: const Text('Recarregar'),
                ),
                const SizedBox(width: 8),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                  ),
                  onPressed: () => _openNasModal(context, ref),
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Novo Concentrador'),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Banner explicativo com suporte MikroTik & Huawei
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppTheme.darkSurface,
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.darkCard),
              ),
              child: Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(10),
                    ),
                    child: const Icon(Icons.router_outlined, color: AppTheme.primaryBlue, size: 24),
                  ),
                  const SizedBox(width: 16),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: const [
                        Text(
                          'Provisionamento Dinâmico RADIUS Ativo',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                        ),
                        SizedBox(height: 4),
                        Text(
                          'MikroTik: Rate-Limit e Address-List dinâmicos via CoA porta 3799. '
                          'Huawei: Huawei-Input/Output-Average-Rate e redirecionamento de corte por captive portal.',
                          style: TextStyle(fontSize: 12, color: AppTheme.textSecondary, height: 1.4),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
            ),
            const SizedBox(height: 24),

            // Lista de Concentradores
            nasListAsync.when(
              loading: () => const Center(
                child: Padding(
                  padding: EdgeInsets.all(48.0),
                  child: CircularProgressIndicator(),
                ),
              ),
              error: (err, _) => Card(
                color: AppTheme.accentError.withValues(alpha: 0.1),
                child: Padding(
                  padding: const EdgeInsets.all(20.0),
                  child: Row(
                    children: [
                      const Icon(Icons.error_outline, color: AppTheme.accentError),
                      const SizedBox(width: 12),
                      Expanded(
                        child: Text(
                          'Erro ao carregar lista de concentradores: $err',
                          style: const TextStyle(color: AppTheme.accentError),
                        ),
                      ),
                    ],
                  ),
                ),
              ),
              data: (list) {
                if (list.isEmpty) {
                  return _buildEmptyState(context, ref);
                }

                return ListView.separated(
                  shrinkWrap: true,
                  physics: const NeverScrollableScrollPhysics(),
                  itemCount: list.length,
                  separatorBuilder: (_, _) => const SizedBox(height: 12),
                  itemBuilder: (ctx, index) {
                    final nas = list[index];
                    return _buildNasCard(context, ref, nas);
                  },
                );
              },
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmptyState(BuildContext context, WidgetRef ref) {
    return Card(
      color: AppTheme.darkSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: AppTheme.darkCard),
      ),
      child: Padding(
        padding: const EdgeInsets.all(48.0),
        child: Center(
          child: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 420),
            child: Column(
              mainAxisSize: MainAxisSize.min,
              children: [
                Container(
                  padding: const EdgeInsets.all(16),
                  decoration: BoxDecoration(
                    color: AppTheme.primaryBlue.withValues(alpha: 0.12),
                    shape: BoxShape.circle,
                  ),
                  child: const Icon(Icons.dns_outlined, color: AppTheme.primaryBlue, size: 36),
                ),
                const SizedBox(height: 20),
                const Text(
                  'Nenhum Concentrador Cadastrado',
                  style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 8),
                const Text(
                  'Cadastre o IP e o secret do seu MikroTik (CHR / RouterOS) ou Huawei para que os clientes possam autenticar via PPPoE/IPoE.',
                  style: TextStyle(fontSize: 13, color: AppTheme.textSecondary, height: 1.4),
                  textAlign: TextAlign.center,
                ),
                const SizedBox(height: 24),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                  ),
                  onPressed: () => _openNasModal(context, ref),
                  icon: const Icon(Icons.add, size: 18),
                  label: const Text('Cadastrar Primeiro Concentrador'),
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildNasCard(BuildContext context, WidgetRef ref, NasModel nas) {
    final isMikrotik = nas.vendorType == NasVendorType.mikrotik;
    final isHuawei = nas.vendorType == NasVendorType.huawei;

    final Color badgeColor = isMikrotik
        ? AppTheme.primaryBlue
        : (isHuawei ? Colors.deepOrangeAccent : AppTheme.accentGreen);

    return Card(
      color: AppTheme.darkSurface,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: const BorderSide(color: AppTheme.darkCard),
      ),
      child: Padding(
        padding: const EdgeInsets.all(18.0),
        child: Row(
          children: [
            // Ícone de Concentrador
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: badgeColor.withValues(alpha: 0.15),
                borderRadius: BorderRadius.circular(10),
              ),
              child: Icon(Icons.router, color: badgeColor, size: 24),
            ),
            const SizedBox(width: 16),

            // Informações Principais
            Expanded(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Flexible(
                        child: Text(
                          nas.shortname,
                          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                      const SizedBox(width: 10),
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                        decoration: BoxDecoration(
                          color: badgeColor.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(12),
                          border: Border.all(color: badgeColor.withValues(alpha: 0.3)),
                        ),
                        child: Text(
                          nas.vendorType.label,
                          style: TextStyle(
                            color: badgeColor,
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Wrap(
                    spacing: 16,
                    runSpacing: 4,
                    children: [
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.lan_outlined, size: 14, color: AppTheme.textMuted),
                          const SizedBox(width: 4),
                          Text(
                            'IP / Host: ${nas.nasname}',
                            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary, fontFamily: 'monospace'),
                          ),
                        ],
                      ),
                      Row(
                        mainAxisSize: MainAxisSize.min,
                        children: [
                          const Icon(Icons.numbers, size: 14, color: AppTheme.textMuted),
                          const SizedBox(width: 4),
                          Text(
                            'Portas: ${nas.ports} (CoA: 3799)',
                            style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                      if (nas.description != null && nas.description!.isNotEmpty)
                        Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.notes, size: 14, color: AppTheme.textMuted),
                            const SizedBox(width: 4),
                            Text(
                              nas.description!,
                              style: const TextStyle(fontSize: 12, color: AppTheme.textMuted),
                            ),
                          ],
                        ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(width: 12),

            // Botões de Ação
            IconButton(
              icon: const Icon(Icons.key, size: 18, color: AppTheme.textSecondary),
              tooltip: 'Copiar Secret RADIUS',
              onPressed: () {
                Clipboard.setData(ClipboardData(text: nas.secret));
                ScaffoldMessenger.of(context).showSnackBar(
                  const SnackBar(
                    content: Text('Secret do FreeRADIUS copiado!'),
                    duration: Duration(seconds: 2),
                  ),
                );
              },
            ),
            IconButton(
              icon: const Icon(Icons.edit_outlined, size: 18, color: AppTheme.primaryBlue),
              tooltip: 'Editar Concentrador',
              onPressed: () => _openNasModal(context, ref, nas),
            ),
            IconButton(
              icon: const Icon(Icons.delete_outline, size: 18, color: AppTheme.accentError),
              tooltip: 'Excluir',
              onPressed: () => _confirmDelete(context, ref, nas),
            ),
          ],
        ),
      ),
    );
  }
}

/// Dialog para Cadastro e Edição de Concentrador (MikroTik / Huawei).
class _NasFormDialog extends ConsumerStatefulWidget {
  final NasModel? nas;

  const _NasFormDialog({this.nas});

  @override
  ConsumerState<_NasFormDialog> createState() => _NasFormDialogState();
}

class _NasFormDialogState extends ConsumerState<_NasFormDialog> {
  final _formKey = GlobalKey<FormState>();

  late NasVendorType _selectedVendor;
  final _shortnameController = TextEditingController();
  final _nasnameController = TextEditingController();
  final _secretController = TextEditingController();
  final _portsController = TextEditingController(text: '1812');
  final _descriptionController = TextEditingController();

  bool _obscureSecret = true;
  bool _isSaving = false;

  @override
  void initState() {
    super.initState();
    if (widget.nas != null) {
      final n = widget.nas!;
      _selectedVendor = n.vendorType;
      _shortnameController.text = n.shortname;
      _nasnameController.text = n.nasname;
      _secretController.text = n.secret;
      _portsController.text = n.ports.toString();
      _descriptionController.text = n.description ?? '';
    } else {
      _selectedVendor = NasVendorType.mikrotik;
    }
  }

  @override
  void dispose() {
    _shortnameController.dispose();
    _nasnameController.dispose();
    _secretController.dispose();
    _portsController.dispose();
    _descriptionController.dispose();
    super.dispose();
  }

  Future<void> _submit() async {
    if (!_formKey.currentState!.validate()) return;

    setState(() => _isSaving = true);

    try {
      final ports = int.tryParse(_portsController.text.trim()) ?? 1812;
      final model = NasModel(
        id: widget.nas?.id,
        companyId: widget.nas?.companyId,
        shortname: _shortnameController.text.trim(),
        nasname: _nasnameController.text.trim(),
        secret: _secretController.text.trim(),
        ports: ports,
        vendorType: _selectedVendor,
        description: _descriptionController.text.trim(),
      );

      if (widget.nas == null) {
        await ref.read(nasListProvider.notifier).createNas(model);
      } else {
        await ref.read(nasListProvider.notifier).updateNas(widget.nas!.id!, model);
      }

      if (!mounted) return;
      Navigator.pop(context);

      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(widget.nas == null ? 'Concentrador cadastrado com sucesso!' : 'Concentrador atualizado com sucesso!'),
          backgroundColor: AppTheme.accentGreen,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Falha ao salvar concentrador: ${e.toString()}'),
          backgroundColor: AppTheme.accentError,
        ),
      );
    } finally {
      if (mounted) setState(() => _isSaving = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final isEdit = widget.nas != null;

    return Dialog(
      backgroundColor: AppTheme.darkSurface,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 540),
        child: SingleChildScrollView(
          padding: const EdgeInsets.all(28.0),
          child: Form(
            key: _formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(10),
                      ),
                      child: const Icon(Icons.router, color: AppTheme.primaryBlue, size: 22),
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            isEdit ? 'Editar Concentrador' : 'Novo Concentrador (BNG / NAS)',
                            style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
                          ),
                          const SizedBox(height: 2),
                          const Text(
                            'Configuração de BNG para FreeRADIUS',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close, size: 20),
                      onPressed: () => Navigator.pop(context),
                    ),
                  ],
                ),
                const SizedBox(height: 20),
                const Divider(),
                const SizedBox(height: 16),

                // Fabricante / Vendor Type
                DropdownButtonFormField<NasVendorType>(
                  initialValue: _selectedVendor,
                  decoration: const InputDecoration(
                    labelText: 'Fabricante / Modelo de Equipamento *',
                    prefixIcon: Icon(Icons.precision_manufacturing_outlined, size: 20),
                  ),
                  items: NasVendorType.values.map((v) {
                    return DropdownMenuItem(
                      value: v,
                      child: Text(v.label, style: const TextStyle(fontSize: 13)),
                    );
                  }).toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedVendor = val);
                  },
                ),
                const SizedBox(height: 14),

                // Identificador Amigável (shortname)
                TextFormField(
                  controller: _shortnameController,
                  decoration: const InputDecoration(
                    labelText: 'Identificador Amigável (Nome Curto) *',
                    hintText: 'Ex: bng-mikrotik-core-01 ou huawei-bras-pop01',
                    prefixIcon: Icon(Icons.label_outline, size: 20),
                  ),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe um nome identificador' : null,
                ),
                const SizedBox(height: 14),

                // IP / Hostname (nasname)
                TextFormField(
                  controller: _nasnameController,
                  decoration: const InputDecoration(
                    labelText: 'IP do Concentrador ou Hostname *',
                    hintText: 'Ex: 192.168.88.1 ou 10.0.0.1',
                    prefixIcon: Icon(Icons.dns_outlined, size: 20),
                  ),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe o IP do concentrador' : null,
                ),
                const SizedBox(height: 14),

                // RADIUS Secret
                TextFormField(
                  controller: _secretController,
                  obscureText: _obscureSecret,
                  decoration: InputDecoration(
                    labelText: 'Secret Compartilhado do FreeRADIUS *',
                    hintText: 'Senha configurada no /radius secret do router',
                    prefixIcon: const Icon(Icons.key_outlined, size: 20),
                    suffixIcon: IconButton(
                      icon: Icon(_obscureSecret ? Icons.visibility_outlined : Icons.visibility_off_outlined, size: 20),
                      onPressed: () => setState(() => _obscureSecret = !_obscureSecret),
                    ),
                  ),
                  validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe a senha secreta' : null,
                ),
                const SizedBox(height: 14),

                // Portas
                TextFormField(
                  controller: _portsController,
                  decoration: const InputDecoration(
                    labelText: 'Porta de Autenticação RADIUS',
                    hintText: '1812',
                    prefixIcon: Icon(Icons.numbers_outlined, size: 20),
                  ),
                  keyboardType: TextInputType.number,
                ),
                const SizedBox(height: 14),

                // Descrição
                TextFormField(
                  controller: _descriptionController,
                  decoration: const InputDecoration(
                    labelText: 'Descrição / Localização do POP (Opcional)',
                    hintText: 'Ex: Concentrador Central - POP Centro',
                    prefixIcon: Icon(Icons.notes_outlined, size: 20),
                  ),
                ),
                const SizedBox(height: 24),

                // Botões
                Row(
                  mainAxisAlignment: MainAxisAlignment.end,
                  children: [
                    TextButton(
                      onPressed: () => Navigator.pop(context),
                      child: const Text('Cancelar'),
                    ),
                    const SizedBox(width: 12),
                    ElevatedButton.icon(
                      style: ElevatedButton.styleFrom(
                        backgroundColor: AppTheme.primaryBlue,
                        foregroundColor: Colors.white,
                        padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                      ),
                      onPressed: _isSaving ? null : _submit,
                      icon: _isSaving
                          ? const SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                            )
                          : const Icon(Icons.save_outlined, size: 18),
                      label: Text(_isSaving ? 'Salvando...' : 'Salvar Concentrador'),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ),
      ),
    );
  }
}
