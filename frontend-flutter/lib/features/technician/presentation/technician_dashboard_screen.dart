import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';

/// Painel Operacional do Técnico de Campo.
/// Gestão de ordens de serviço, rotas e checklist de ativação FTTH.
class TechnicianDashboardScreen extends ConsumerStatefulWidget {
  const TechnicianDashboardScreen({super.key});

  @override
  ConsumerState<TechnicianDashboardScreen> createState() => _TechnicianDashboardScreenState();
}

class _TechnicianDashboardScreenState extends ConsumerState<TechnicianDashboardScreen> {
  int _selectedFilter = 0;

  @override
  Widget build(BuildContext context) {
    final authState = ref.watch(authProvider);

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Header
            Row(
              children: [
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Painel do Técnico de Campo — ${authState.name ?? "Técnico"}',
                        style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'Ordens de Serviço atribuídas, checklist de drop óptico e ativação de ONU',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 20),

            // Chips de Filtro
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildFilterChip(0, 'Todas as O.S.'),
                _buildFilterChip(1, 'Instalação FTTH'),
                _buildFilterChip(2, 'Manutenção / Sinal'),
                _buildFilterChip(3, 'Troca de Equipamento'),
              ],
            ),
            const SizedBox(height: 24),

            // Card de Status do Técnico
            Card(
              child: Padding(
                padding: const EdgeInsets.all(20.0),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      children: [
                        Container(
                          padding: const EdgeInsets.all(10),
                          decoration: BoxDecoration(
                            color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(8),
                          ),
                          child: const Icon(Icons.build_circle_outlined, color: AppTheme.primaryBlue, size: 22),
                        ),
                        const SizedBox(width: 14),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              const Text(
                                'Fila Operacional de Campo',
                                style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
                              ),
                              const SizedBox(height: 2),
                              Text(
                                'Técnico conectado: ${authState.email ?? ""} • Servidor: ${authState.serverUrl ?? "Local"}',
                                style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                              ),
                            ],
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 16),
                    const Divider(),
                    const SizedBox(height: 16),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        const Expanded(
                          child: Text(
                            'Nenhuma O.S. pendente atribuída a este usuário no momento.',
                            style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                          ),
                        ),
                        OutlinedButton.icon(
                          onPressed: () => _openChecklistDialog(context, 'OS-TESTE-PROVEDOR', 'Assinante em Ativação'),
                          icon: const Icon(Icons.checklist, size: 16),
                          label: const Text('Ver Checklist de Campo', style: TextStyle(fontSize: 12)),
                        ),
                      ],
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

  Widget _buildFilterChip(int index, String label) {
    final isSelected = _selectedFilter == index;
    return ChoiceChip(
      label: Text(label, style: TextStyle(fontSize: 12, color: isSelected ? const Color(0xFF0A0F1D) : AppTheme.textSecondary)),
      selected: isSelected,
      selectedColor: AppTheme.primaryBlue,
      backgroundColor: AppTheme.darkSurface,
      side: BorderSide(color: isSelected ? AppTheme.primaryBlue : AppTheme.darkCard),
      onSelected: (val) {
        if (val) setState(() => _selectedFilter = index);
      },
    );
  }

  void _openChecklistDialog(BuildContext context, String osNumber, String client) {
    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: AppTheme.darkSurface,
      shape: const RoundedRectangleBorder(borderRadius: BorderRadius.vertical(top: Radius.circular(16))),
      builder: (ctx) {
        return Padding(
          padding: EdgeInsets.only(
            left: 20,
            right: 20,
            top: 20,
            bottom: MediaQuery.of(ctx).viewInsets.bottom + 24,
          ),
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              Text('Execução da $osNumber', style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
              Text('Cliente: $client', style: const TextStyle(fontSize: 13, color: AppTheme.textSecondary)),
              const SizedBox(height: 16),
              const ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: Icon(Icons.check_box_outlined, color: AppTheme.accentGreen),
                title: Text('1. Fixação do Drop Óptico & Conectorização'),
              ),
              const ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: Icon(Icons.check_box_outlined, color: AppTheme.accentGreen),
                title: Text('2. Medição com Power Meter: -18.5 dBm (Aprovado)'),
              ),
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.qr_code_scanner, color: AppTheme.primaryBlue),
                title: const Text('3. Leitura MAC/SN da ONU (Câmera)'),
                trailing: TextButton(onPressed: () {}, child: const Text('Escanear')),
              ),
              ListTile(
                dense: true,
                contentPadding: EdgeInsets.zero,
                leading: const Icon(Icons.camera_alt_outlined, color: AppTheme.primaryBlue),
                title: const Text('4. Fotos da Instalação & Termo Assinado'),
                trailing: TextButton(onPressed: () {}, child: const Text('Anexar')),
              ),
              const SizedBox(height: 20),
              ElevatedButton.icon(
                onPressed: () => Navigator.pop(ctx),
                icon: const Icon(Icons.check, size: 18),
                label: const Text('Concluir O.S. e Provisionar na OLT'),
              ),
            ],
          ),
        );
      },
    );
  }
}
