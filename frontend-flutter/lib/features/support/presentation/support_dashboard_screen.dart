import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';

/// Painel de Atendimento ao Cliente & Suporte Técnico.
class SupportDashboardScreen extends StatefulWidget {
  const SupportDashboardScreen({super.key});

  @override
  State<SupportDashboardScreen> createState() => _SupportDashboardScreenState();
}

class _SupportDashboardScreenState extends State<SupportDashboardScreen> {
  final _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          const Text(
            'Central de Atendimento & Helpdesk',
            style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          const Text(
            'Localização rápida de assinantes, desbloqueio em confiança, diagnóstico de sinal e chamados ANATEL',
            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
          ),
          const SizedBox(height: 24),

          // Barra de Busca Rápida
          Card(
            child: Padding(
              padding: const EdgeInsets.all(16.0),
              child: Row(
                children: [
                  Expanded(
                    child: TextField(
                      controller: _searchController,
                      decoration: const InputDecoration(
                        hintText: 'Buscar assinante por CPF/CNPJ, Nome, MAC da ONU ou Contrato...',
                        prefixIcon: Icon(Icons.search, size: 20),
                      ),
                    ),
                  ),
                  const SizedBox(width: 12),
                  ElevatedButton.icon(
                    onPressed: () {},
                    icon: const Icon(Icons.person_search, size: 18),
                    label: const Text('Localizar'),
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 24),

          // Cards de SLA e Chamados
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              _buildTicketMetric('Fila de Espera', '4 atendimentos', AppTheme.accentWarning, Icons.phone_in_talk),
              _buildTicketMetric('SLA Médio (ANATEL)', '14 min', AppTheme.accentGreen, Icons.timer_outlined),
              _buildTicketMetric('Desbloqueios Hoje', '18 efetuados', AppTheme.primaryBlue, Icons.lock_open),
              _buildTicketMetric('Chamados de Fibra', '2 abertos', AppTheme.primaryIndigo, Icons.cable),
            ],
          ),
          const SizedBox(height: 28),

          // Fila de Chamados Ativos
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: const [
                      Text('Chamados em Andamento', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                      Icon(Icons.headset_mic_outlined, color: AppTheme.primaryBlue, size: 20),
                    ],
                  ),
                  const SizedBox(height: 16),
                  _buildCallRow('#1042', 'Carlos Eduardo Silva', 'Lentidão no Wi-Fi 5GHz', 'Há 8 min', 'Em Atendimento', AppTheme.primaryBlue),
                  const Divider(),
                  _buildCallRow('#1041', 'Maria das Graças Ferreira', 'Solicitação de 2ª via Pix', 'Há 15 min', 'Resolvido', AppTheme.accentGreen),
                  const Divider(),
                  _buildCallRow('#1040', 'Padaria & Confeitaria Central', 'Sem conexão (LOS Vermelho)', 'Há 22 min', 'O.S. Agendada', AppTheme.accentWarning),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTicketMetric(String title, String val, Color col, IconData ic) {
    return Container(
      width: 240,
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
              color: col.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(ic, color: col, size: 22),
          ),
          const SizedBox(width: 14),
          Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(title, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              const SizedBox(height: 2),
              Text(val, style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildCallRow(String id, String client, String motivo, String tempo, String status, Color col) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Row(
        children: [
          Text(id, style: const TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primaryBlue, fontSize: 13)),
          const SizedBox(width: 16),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(client, style: const TextStyle(fontWeight: FontWeight.w600, fontSize: 13)),
                Text(motivo, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              ],
            ),
          ),
          Text(tempo, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
          const SizedBox(width: 16),
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
            decoration: BoxDecoration(
              color: col.withValues(alpha: 0.15),
              borderRadius: BorderRadius.circular(6),
              border: Border.all(color: col),
            ),
            child: Text(status, style: TextStyle(color: col, fontSize: 11, fontWeight: FontWeight.bold)),
          ),
        ],
      ),
    );
  }
}
