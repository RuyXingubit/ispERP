import 'package:flutter/material.dart';
import '../../../core/theme/app_theme.dart';

/// Painel da Diretoria & Administração Geral do Provedor.
class AdminDashboardScreen extends StatelessWidget {
  const AdminDashboardScreen({super.key});

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
                    'Painel Executivo 360º',
                    style: TextStyle(fontSize: 24, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Visão integrada de infraestrutura, finanças, OLTs e conformidade regulatória',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                  ),
                ],
              ),
              ElevatedButton.icon(
                onPressed: () {},
                icon: const Icon(Icons.refresh, size: 18),
                label: const Text('Atualizar Métricas'),
              ),
            ],
          ),
          const SizedBox(height: 24),

          // Cards de Métricas Principais
          Wrap(
            spacing: 16,
            runSpacing: 16,
            children: [
              _buildMetricCard(
                title: 'Assinantes Ativos',
                value: '4.820',
                delta: '+52 este mês',
                icon: Icons.people_alt_outlined,
                color: AppTheme.primaryBlue,
              ),
              _buildMetricCard(
                title: 'Faturamento Previsto',
                value: 'R\$ 498.600',
                delta: '98,4% Adimplência',
                icon: Icons.monetization_on_outlined,
                color: AppTheme.accentGreen,
              ),
              _buildMetricCard(
                title: 'OLTs Operacionais',
                value: '12 / 12 UP',
                delta: 'Latência média 4ms',
                icon: Icons.router_outlined,
                color: AppTheme.primaryIndigo,
              ),
              _buildMetricCard(
                title: 'O.S. em Aberto Hoje',
                value: '14 Pendentes',
                delta: '6 em andamento',
                icon: Icons.assignment_outlined,
                color: AppTheme.accentWarning,
              ),
            ],
          ),
          const SizedBox(height: 28),

          // Grid com Status de OLTs e Logs de Auditoria
          LayoutBuilder(
            builder: (context, constraints) {
              final isWide = constraints.maxWidth > 800;
              return Flex(
                direction: isWide ? Axis.horizontal : Axis.vertical,
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  // Painel de OLTs e Infraestrutura
                  Expanded(
                    flex: isWide ? 6 : 0,
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(20.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: const [
                                Text(
                                  'Topologia & OLTs da Rede',
                                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                                ),
                                Icon(Icons.settings_input_composite, color: AppTheme.primaryBlue, size: 20),
                              ],
                            ),
                            const SizedBox(height: 16),
                            _buildOltStatusTile('OLT-01 Central Huawei MA5800', '1.240 ONUs', 'Online (PON 1-16 OK)', true),
                            _buildOltStatusTile('OLT-02 Filial Sul ZTE C320', '840 ONUs', 'Online (PON 1-8 OK)', true),
                            _buildOltStatusTile('OLT-03 Bairro Novo Fiberhome', '510 ONUs', 'Online (Alerta de Potência PON 3)', false),
                          ],
                        ),
                      ),
                    ),
                  ),
                  if (isWide) const SizedBox(width: 16) else const SizedBox(height: 16),

                  // Registro de Auditoria e Conformidade
                  Expanded(
                    flex: isWide ? 4 : 0,
                    child: Card(
                      child: Padding(
                        padding: const EdgeInsets.all(20.0),
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Row(
                              mainAxisAlignment: MainAxisAlignment.spaceBetween,
                              children: const [
                                Text(
                                  'Auditoria & Segurança',
                                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                                ),
                                Icon(Icons.verified_user_outlined, color: AppTheme.accentGreen, size: 20),
                              ],
                            ),
                            const SizedBox(height: 16),
                            _buildAuditTile('Backup Automático Concluído', 'Hoje 03:00 - S3 Criptografado AES-256'),
                            _buildAuditTile('Exportação Lote NFCom 62', 'Hoje 08:30 - 4.820 notas assinadas'),
                            _buildAuditTile('Sincronização Radius AAA', 'Há 12 min - 0 falhas registradas'),
                          ],
                        ),
                      ),
                    ),
                  ),
                ],
              );
            },
          ),
        ],
      ),
    );
  }

  Widget _buildMetricCard({
    required String title,
    required String value,
    required String delta,
    required IconData icon,
    required Color color,
  }) {
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
              Icon(icon, color: color, size: 20),
            ],
          ),
          const SizedBox(height: 10),
          Text(
            value,
            style: const TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 6),
          Text(
            delta,
            style: TextStyle(fontSize: 12, color: color, fontWeight: FontWeight.w500),
          ),
        ],
      ),
    );
  }

  Widget _buildOltStatusTile(String name, String onus, String status, bool ok) {
    return ListTile(
      dense: true,
      contentPadding: EdgeInsets.zero,
      leading: Icon(
        ok ? Icons.check_circle : Icons.warning_amber_rounded,
        color: ok ? AppTheme.accentGreen : AppTheme.accentWarning,
        size: 20,
      ),
      title: Text(name, style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600)),
      subtitle: Text('$onus • $status', style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
    );
  }

  Widget _buildAuditTile(String title, String subtitle) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          Text(title, style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600)),
          const SizedBox(height: 2),
          Text(subtitle, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
        ],
      ),
    );
  }
}
