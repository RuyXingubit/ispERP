import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';

/// Painel do Técnico de Campo (Mobile-First / O.S. e Instalações).
class TechnicianDashboardScreen extends StatefulWidget {
  const TechnicianDashboardScreen({super.key});

  @override
  State<TechnicianDashboardScreen> createState() => _TechnicianDashboardScreenState();
}

class _TechnicianDashboardScreenState extends State<TechnicianDashboardScreen> {
  int _selectedFilter = 0;

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(16.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header com Status do Técnico
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text(
                    'Ordens de Serviço de Hoje',
                    style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 2),
                  Text(
                    'Equipe Alpha • 5 O.S. atribuídas',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                  ),
                ],
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                decoration: BoxDecoration(
                  color: AppTheme.accentGreen.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(20),
                  border: Border.all(color: AppTheme.accentGreen),
                ),
                child: Row(
                  mainAxisSize: MainAxisSize.min,
                  children: const [
                    Icon(Icons.circle, color: AppTheme.accentGreen, size: 8),
                    SizedBox(width: 6),
                    Text('Em Campo', style: TextStyle(color: AppTheme.accentGreen, fontSize: 11, fontWeight: FontWeight.bold)),
                  ],
                ),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // Filtros de Status (Pendentes, Em Andamento, Concluídas)
          Row(
            children: [
              _buildFilterChip(0, 'Pendentes (3)'),
              const SizedBox(width: 8),
              _buildFilterChip(1, 'Em Execução (1)'),
              const SizedBox(width: 8),
              _buildFilterChip(2, 'Finalizadas (1)'),
            ],
          ),
          const SizedBox(height: 16),

          // Lista de O.S. do Dia
          _buildWorkOrderCard(
            osNumber: 'OS-2026-891',
            type: 'Nova Instalação FTTH',
            client: 'Lucas Mendonça de Souza',
            address: 'Rua das Palmeiras, 342 - Jd. América',
            cto: 'CTO-04 Port 6 (-18.2 dBm)',
            time: '09:00 - 11:00',
            status: 'EM ANDAMENTO',
            statusColor: AppTheme.primaryBlue,
            isCurrent: true,
          ),
          const SizedBox(height: 12),
          _buildWorkOrderCard(
            osNumber: 'OS-2026-894',
            type: 'Manutenção / Sinal Baixo',
            client: 'Mercado Bom Preço Ltda',
            address: 'Av. Brasil, 1205 - Centro',
            cto: 'CTO-01 Port 3 (-27.8 dBm)',
            time: '11:30 - 13:00',
            status: 'PENDENTE',
            statusColor: AppTheme.accentWarning,
            isCurrent: false,
          ),
          const SizedBox(height: 12),
          _buildWorkOrderCard(
            osNumber: 'OS-2026-896',
            type: 'Troca de Roteador Wi-Fi 6',
            client: 'Fernanda Lima Castro',
            address: 'Rua Paraná, 88 - Apto 402',
            cto: 'CTO-08 Port 2 (-19.0 dBm)',
            time: '14:00 - 15:30',
            status: 'PENDENTE',
            statusColor: AppTheme.accentWarning,
            isCurrent: false,
          ),
        ],
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

  Widget _buildWorkOrderCard({
    required String osNumber,
    required String type,
    required String client,
    required String address,
    required String cto,
    required String time,
    required String status,
    required Color statusColor,
    required bool isCurrent,
  }) {
    return Card(
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(12),
        side: BorderSide(color: isCurrent ? AppTheme.primaryBlue : AppTheme.darkCard, width: isCurrent ? 1.5 : 1),
      ),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Linha do Topo da O.S.
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Text(osNumber, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.primaryBlue)),
                    const SizedBox(width: 8),
                    Container(
                      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                      decoration: BoxDecoration(
                        color: statusColor.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(4),
                      ),
                      child: Text(status, style: TextStyle(color: statusColor, fontSize: 10, fontWeight: FontWeight.bold)),
                    ),
                  ],
                ),
                Text(time, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              ],
            ),
            const SizedBox(height: 8),

            // Tipo e Cliente
            Text(type, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15)),
            const SizedBox(height: 2),
            Text(client, style: const TextStyle(fontSize: 13, color: AppTheme.textPrimary)),
            const SizedBox(height: 8),

            // Endereço e CTO
            Row(
              children: [
                const Icon(Icons.location_on_outlined, size: 16, color: AppTheme.textSecondary),
                const SizedBox(width: 6),
                Expanded(child: Text(address, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary))),
              ],
            ),
            const SizedBox(height: 4),
            Row(
              children: [
                const Icon(Icons.cable, size: 16, color: AppTheme.primaryBlue),
                const SizedBox(width: 6),
                Text(cto, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
              ],
            ),
            const SizedBox(height: 14),

            // Botões de Ação do Técnico
            Row(
              children: [
                Expanded(
                  child: OutlinedButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.directions_outlined, size: 16),
                    label: const Text('Navegar GPS', style: TextStyle(fontSize: 12)),
                  ),
                ),
                const SizedBox(width: 8),
                Expanded(
                  child: ElevatedButton.icon(
                    onPressed: () => _openChecklistDialog(context, osNumber, client),
                    icon: const Icon(Icons.qr_code_scanner, size: 16),
                    label: const Text('Executar O.S.', style: TextStyle(fontSize: 12)),
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
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
                label: const Text('Finalizar e Provisionar ONU na OLT'),
              ),
            ],
          ),
        );
      },
    );
  }
}
