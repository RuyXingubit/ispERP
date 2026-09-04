import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/attendance_notifier.dart';
import 'steps/step_channel_and_search.dart';
import 'steps/step_customer_360_overview.dart';
import 'steps/step_invoice_payment.dart';
import 'steps/step_receipt_evidence.dart';

/// Modal principal do Hub de Atendimento Guiado.
class AttendanceHubModal extends ConsumerWidget {
  const AttendanceHubModal({super.key});

  static Future<void> show(BuildContext context) {
    return showDialog<void>(
      context: context,
      barrierDismissible: false,
      builder: (context) => const AttendanceHubModal(),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);

    String stepTitle = 'Iniciar Atendimento';
    switch (state.step) {
      case AttendanceStep.channelAndSearch:
        stepTitle = '1. Identificação do Assinante';
        break;
      case AttendanceStep.overview:
        stepTitle = '2. Raio-X & Intenção';
        break;
      case AttendanceStep.payment:
        stepTitle = '3. Recebimento Guiado';
        break;
      case AttendanceStep.receiptEvidence:
        stepTitle = '4. Custódia & Canhoto';
        break;
    }

    return Dialog(
      backgroundColor: AppTheme.darkBg,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppTheme.darkBorder),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(
          maxWidth: 820,
          maxHeight: 680,
        ),
        child: Padding(
          padding: const EdgeInsets.all(20.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Cabeçalho com Título, Stepper e Botão Fechar
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.support_agent_rounded, color: AppTheme.primaryBlue, size: 22),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Hub de Atendimento Guiado',
                          style: TextStyle(fontSize: 12, color: AppTheme.textSecondary, fontWeight: FontWeight.bold),
                        ),
                        Text(
                          stepTitle,
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                      ],
                    ),
                  ),
                  // Indicador de Canal Selecionado
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: AppTheme.darkBorder),
                    ),
                    child: Text(
                      state.channel.label.split(' ').first.toUpperCase(),
                      style: const TextStyle(fontSize: 10, color: AppTheme.primaryBlue, fontWeight: FontWeight.bold),
                    ),
                  ),
                  const SizedBox(width: 10),
                  IconButton(
                    icon: const Icon(Icons.close, size: 20),
                    tooltip: 'Cancelar Atendimento',
                    onPressed: () {
                      notifier.reset();
                      Navigator.of(context).pop();
                    },
                  ),
                ],
              ),
              const SizedBox(height: 12),
              const Divider(height: 1),
              const SizedBox(height: 16),

              // Corpo da Etapa Atual
              Expanded(
                child: _buildCurrentStep(state.step),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCurrentStep(AttendanceStep step) {
    switch (step) {
      case AttendanceStep.channelAndSearch:
        return const StepChannelAndSearch();
      case AttendanceStep.overview:
        return const StepCustomer360Overview();
      case AttendanceStep.payment:
        return const StepInvoicePayment();
      case AttendanceStep.receiptEvidence:
        return const StepReceiptEvidence();
    }
  }
}
