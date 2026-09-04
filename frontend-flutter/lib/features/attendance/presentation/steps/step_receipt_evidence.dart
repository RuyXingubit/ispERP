import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/attendance_notifier.dart';

/// Passo 4 do Hub de Atendimento: Confirmação de Baixa e Comprovação de Custódia de Caixa.
class StepReceiptEvidence extends ConsumerWidget {
  const StepReceiptEvidence({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);

    final amount = state.selectedInvoice?.amount ?? 0.0;
    final isCard = state.paymentReceiptPending;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 1. Banner de Sucesso
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: AppTheme.accentGreen.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(12),
              border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.4)),
            ),
            child: Column(
              children: [
                const Icon(Icons.check_circle_rounded, color: AppTheme.accentGreen, size: 52),
                const SizedBox(height: 10),
                const Text(
                  'Fatura Liquidada com Sucesso!',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16, color: AppTheme.accentGreen),
                ),
                const SizedBox(height: 4),
                Text(
                  'Recebimento de R\$ ${amount.toStringAsFixed(2)} registrado no backend.',
                  style: const TextStyle(fontSize: 13, color: AppTheme.textPrimary),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // 2. Alerta de Custódia e Conciliação Antifraude
          if (isCard)
            Container(
              padding: const EdgeInsets.all(14),
              decoration: BoxDecoration(
                color: Colors.orangeAccent.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: Colors.orangeAccent.withValues(alpha: 0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Row(
                    children: [
                      Icon(Icons.shield_outlined, color: Colors.orangeAccent, size: 20),
                      SizedBox(width: 8),
                      Text(
                        'Custódia de Caixa Temporária Ativa',
                        style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: Colors.orangeAccent),
                      ),
                    ],
                  ),
                  const SizedBox(height: 6),
                  Text(
                    'O valor de R\$ ${amount.toStringAsFixed(2)} está sob sua responsabilidade temporária até a anexação do comprovante (canhoto da maquininha).',
                    style: const TextStyle(fontSize: 12, color: AppTheme.textPrimary),
                  ),
                  const SizedBox(height: 10),
                  const Text(
                    'Você pode fotografar agora ou pegar o celular para bater a foto pelo app móvel no menu "Meu Caixa".',
                    style: TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                  ),
                ],
              ),
            ),
          const SizedBox(height: 20),

          // 3. Botões de Ação de Comprovação
          Card(
            color: AppTheme.darkSurface,
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
            child: Padding(
              padding: const EdgeInsets.all(14),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  const Text(
                    'Comprovação do Pagamento:',
                    style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                  ),
                  const SizedBox(height: 10),
                  ElevatedButton.icon(
                    style: ElevatedButton.styleFrom(
                      backgroundColor: AppTheme.primaryBlue,
                      foregroundColor: Colors.white,
                      padding: const EdgeInsets.symmetric(vertical: 12),
                    ),
                    icon: const Icon(Icons.camera_alt_rounded, size: 18),
                    label: const Text('Fotografar Canhoto da Maquininha'),
                    onPressed: () {
                      ScaffoldMessenger.of(context).showSnackBar(
                        const SnackBar(
                          content: Text('Comprovante anexado com sucesso e enviado para conferência cega!'),
                          backgroundColor: AppTheme.accentGreen,
                        ),
                      );
                    },
                  ),
                  const SizedBox(height: 8),
                  OutlinedButton.icon(
                    icon: const Icon(Icons.phone_android_rounded, size: 18),
                    label: const Text('Fotografar Depois pelo Celular (Meu Caixa)'),
                    onPressed: () {
                      Navigator.of(context).pop();
                      notifier.reset();
                    },
                  ),
                ],
              ),
            ),
          ),
          const SizedBox(height: 20),

          // 4. Botão Final de Conclusão do Atendimento
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentGreen,
              foregroundColor: Colors.white,
              padding: const EdgeInsets.symmetric(vertical: 14),
            ),
            child: const Text('Finalizar Atendimento & Liberar Assinante', style: TextStyle(fontWeight: FontWeight.bold)),
            onPressed: () {
              Navigator.of(context).pop();
              notifier.reset();
            },
          ),
        ],
      ),
    );
  }
}
