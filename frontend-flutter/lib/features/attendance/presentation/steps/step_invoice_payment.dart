import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/attendance_notifier.dart';

/// Passo 3 do Hub de Atendimento: Escolha da Fatura e Recebimento Guiado.
class StepInvoicePayment extends ConsumerWidget {
  const StepInvoicePayment({super.key});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);

    final invoices = state.invoices;
    final selectedInvoice = state.selectedInvoice;

    if (invoices.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.check_circle_outline_rounded, color: AppTheme.accentGreen, size: 54),
            const SizedBox(height: 12),
            const Text(
              'Nenhuma fatura em aberto!',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
            ),
            const SizedBox(height: 6),
            const Text(
              'O assinante está com todas as mensalidades quitadas.',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
            ),
            const SizedBox(height: 16),
            ElevatedButton(
              onPressed: () => notifier.goBack(),
              child: const Text('Voltar ao Raio-X'),
            ),
          ],
        ),
      );
    }

    final amountToPay = selectedInvoice?.amount ?? 0.0;

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // 1. Lista de Faturas em Aberto para Escolha
          const Text(
            'Selecione a fatura para baixa:',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 8),
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: invoices.length,
            separatorBuilder: (_, index) => const SizedBox(height: 6),
            itemBuilder: (context, index) {
              final inv = invoices[index];
              final isSelected = selectedInvoice?.id == inv.id;

              return InkWell(
                onTap: () => notifier.selectInvoice(inv),
                borderRadius: BorderRadius.circular(8),
                child: Container(
                  padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  decoration: BoxDecoration(
                    color: isSelected ? AppTheme.primaryBlue.withValues(alpha: 0.1) : AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: isSelected ? AppTheme.primaryBlue : AppTheme.darkBorder,
                      width: isSelected ? 1.5 : 1.0,
                    ),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        isSelected ? Icons.radio_button_checked : Icons.radio_button_off,
                        color: isSelected ? AppTheme.primaryBlue : AppTheme.textSecondary,
                        size: 18,
                      ),
                      const SizedBox(width: 10),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Vencimento: ${inv.dueDate}',
                              style: TextStyle(
                                fontWeight: FontWeight.bold,
                                fontSize: 13,
                                color: inv.isOverdue ? AppTheme.accentError : AppTheme.textPrimary,
                              ),
                            ),
                            Text(
                              inv.isOverdue ? 'Em atraso' : 'A vencer',
                              style: TextStyle(
                                fontSize: 11,
                                color: inv.isOverdue ? AppTheme.accentError : AppTheme.textSecondary,
                              ),
                            ),
                          ],
                        ),
                      ),
                      Text(
                        'R\$ ${inv.amount.toStringAsFixed(2)}',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: 16),

          // 2. Seleção da Forma de Pagamento
          const Text(
            'Forma de Pagamento:',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            runSpacing: 8,
            children: [
              _buildPaymentChip(
                method: 'DEBIT_CARD',
                label: 'Cartão de Débito',
                icon: Icons.credit_card_rounded,
                selectedMethod: state.paymentMethod,
                onSelected: () => notifier.setPaymentMethod('DEBIT_CARD'),
              ),
              _buildPaymentChip(
                method: 'CREDIT_CARD',
                label: 'Cartão de Crédito',
                icon: Icons.credit_score_rounded,
                selectedMethod: state.paymentMethod,
                onSelected: () => notifier.setPaymentMethod('CREDIT_CARD'),
              ),
              _buildPaymentChip(
                method: 'PIX',
                label: 'PIX Presencial',
                icon: Icons.qr_code_2_rounded,
                selectedMethod: state.paymentMethod,
                onSelected: () => notifier.setPaymentMethod('PIX'),
              ),
              _buildPaymentChip(
                method: 'CASH',
                label: 'Dinheiro em Espécie',
                icon: Icons.attach_money_rounded,
                selectedMethod: state.paymentMethod,
                onSelected: () => notifier.setPaymentMethod('CASH'),
              ),
            ],
          ),
          const SizedBox(height: 16),

          // 3. Orientação ao Atendente (Instrução da Maquininha / Caixa)
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(8),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Row(
              children: [
                const Icon(Icons.point_of_sale_rounded, color: AppTheme.primaryBlue, size: 24),
                const SizedBox(width: 10),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'Cobrar exatamente R\$ ${amountToPay.toStringAsFixed(2)} na maquininha oficial',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                      ),
                      const SizedBox(height: 2),
                      const Text(
                        'O comprovante emitido pela maquininha deverá ser fotografado para liberação da custódia de caixa.',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 18),

          // 4. Mensagem de Erro se houver
          if (state.errorMessage != null)
            Padding(
              padding: const EdgeInsets.only(bottom: 12),
              child: Text(
                state.errorMessage!,
                style: const TextStyle(color: AppTheme.accentError, fontSize: 12, fontWeight: FontWeight.bold),
              ),
            ),

          // 5. Botões de Ação
          Row(
            children: [
              OutlinedButton(
                onPressed: () => notifier.goBack(),
                child: const Text('Voltar'),
              ),
              const SizedBox(width: 10),
              Expanded(
                child: ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.accentGreen,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(vertical: 14),
                  ),
                  icon: state.isProcessingPayment
                      ? const SizedBox(width: 18, height: 18, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2))
                      : const Icon(Icons.check_circle_rounded),
                  label: Text(
                    state.isProcessingPayment
                        ? 'Registrando no Servidor...'
                        : 'Confirmar Recebimento (R\$ ${amountToPay.toStringAsFixed(2)})',
                    style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
                  ),
                  onPressed: state.isProcessingPayment ? null : () => notifier.processPayment(),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }

  Widget _buildPaymentChip({
    required String method,
    required String label,
    required IconData icon,
    required String selectedMethod,
    required VoidCallback onSelected,
  }) {
    final isSelected = method == selectedMethod;

    return ChoiceChip(
      avatar: Icon(icon, size: 16, color: isSelected ? Colors.white : AppTheme.textSecondary),
      label: Text(label),
      selected: isSelected,
      selectedColor: AppTheme.primaryBlue,
      backgroundColor: AppTheme.darkSurface,
      labelStyle: TextStyle(
        color: isSelected ? Colors.white : AppTheme.textPrimary,
        fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
        fontSize: 12,
      ),
      onSelected: (_) => onSelected(),
    );
  }
}
