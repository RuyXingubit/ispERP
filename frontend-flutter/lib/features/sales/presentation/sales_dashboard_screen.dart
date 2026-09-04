import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';

/// Painel Comercial & Vendas do Provedor.
class SalesDashboardScreen extends StatelessWidget {
  const SalesDashboardScreen({super.key});

  @override
  Widget build(BuildContext context) {
    return SingleChildScrollView(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Header
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text(
                    'Gestão Comercial & Captação',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Funil de vendas, catálogo de planos ativos, consulta de viabilidade e conversão de campanhas',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                  ),
                ],
              ),
              ElevatedButton.icon(
                onPressed: () {},
                icon: const Icon(Icons.person_add_alt_1_outlined, size: 18),
                label: const Text('Nova Proposta'),
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Métricas do Funil
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              _buildFunnelCard('Leads do Site Vitrine', '28 novos', 'Via Google Ads & Instagram', AppTheme.primaryBlue, Icons.public),
              _buildFunnelCard('Propostas em Análise', '14 ativas', 'Aguardando aceite do cliente', AppTheme.accentWarning, Icons.edit_document),
              _buildFunnelCard('Contratos Fechados (Mês)', '62 vendas', 'Meta: 80 contratos (77,5%)', AppTheme.accentGreen, Icons.verified_outlined),
              _buildFunnelCard('Ticket Médio dos Planos', 'R\$ 109,90', '+6,2% vs mês anterior', AppTheme.primaryIndigo, Icons.payments_outlined),
            ],
          ),
          const SizedBox(height: 28),

          // Catálogo de Planos Ativos
          Card(
            child: Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('Planos Vigentes (Sincronizados com a API)', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 16),
                  Wrap(
                    spacing: 16,
                    runSpacing: 16,
                    children: [
                      _buildPlanCard('Fibra 400 Mega', '400 Mbps Down / 200 Mbps Up', 'R\$ 89,90/mês', 'Wi-Fi 5 incluso', AppTheme.primaryBlue),
                      _buildPlanCard('Fibra 600 Mega (Mais Vendido)', '600 Mbps Down / 300 Mbps Up', 'R\$ 109,90/mês', 'Wi-Fi 6 Mesh + Paramount+', AppTheme.accentGreen),
                      _buildPlanCard('Fibra 1 Giga Gamer', '1000 Mbps Down / 500 Mbps Up', 'R\$ 149,90/mês', 'Wi-Fi 6 Roteador Gamer + IP Fixo', AppTheme.primaryIndigo),
                    ],
                  ),
                ],
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFunnelCard(String title, String val, String sub, Color col, IconData ic) {
    return Container(
      width: 240,
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
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Text(title, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
              Icon(ic, color: col, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(val, style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
          const SizedBox(height: 4),
          Text(sub, style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
        ],
      ),
    );
  }

  Widget _buildPlanCard(String name, String speed, String price, String perks, Color col) {
    return Container(
      width: 280,
      padding: const EdgeInsets.all(18),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(10),
        border: Border.all(color: col.withValues(alpha: 0.5)),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(name, style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: col)),
          const SizedBox(height: 6),
          Text(speed, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
          const SizedBox(height: 12),
          Text(price, style: const TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 6),
          Text('• $perks', style: const TextStyle(fontSize: 11, color: AppTheme.textMuted)),
        ],
      ),
    );
  }
}
