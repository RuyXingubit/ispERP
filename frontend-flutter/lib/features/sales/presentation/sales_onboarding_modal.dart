import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/sales_onboarding_notifier.dart';
import 'steps/step_sales_customer_form.dart';
import 'steps/step_sales_feasibility.dart';
import 'steps/step_sales_plan_picker.dart';
import 'steps/step_sales_summary_confirmation.dart';

/// Modal container da esteira guiada de Venda Expressa (Onboarding em 4 passos).
class SalesOnboardingModal extends ConsumerWidget {
  const SalesOnboardingModal({super.key});

  /// Abre o modal responsivo.
  static Future<void> show(BuildContext context) {
    return showDialog(
      context: context,
      barrierDismissible: false,
      builder: (_) => const SalesOnboardingModal(),
    );
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(salesOnboardingProvider);
    final notifier = ref.read(salesOnboardingProvider.notifier);

    final isFinalSuccess = state.saleResult != null;

    return Dialog(
      backgroundColor: AppTheme.darkBg,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppTheme.darkBorder),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 24),
      child: ConstrainedBox(
        constraints: const BoxConstraints(
          maxWidth: 680,
          maxHeight: 740,
        ),
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 1. Cabeçalho com indicador de progresso
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(Icons.person_add_alt_1_rounded, color: AppTheme.primaryBlue, size: 20),
                  ),
                  const SizedBox(width: 12),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        const Text(
                          'Onboarding de Nova Venda Expressa',
                          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
                        ),
                        Text(
                          _getStepSubtitle(state.step),
                          style: const TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    icon: const Icon(Icons.close, size: 20),
                    onPressed: () {
                      notifier.reset();
                      Navigator.of(context).pop();
                    },
                  ),
                ],
              ),
              const SizedBox(height: 14),

              // Barra de progresso dos 4 passos
              LinearProgressIndicator(
                value: _getStepProgress(state.step),
                backgroundColor: AppTheme.darkSurface,
                valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.primaryBlue),
                minHeight: 4,
                borderRadius: BorderRadius.circular(2),
              ),
              const SizedBox(height: 16),

              // Mensagem de Erro (se houver)
              if (state.errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
                  decoration: BoxDecoration(
                    color: AppTheme.accentError.withValues(alpha: 0.15),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.accentError.withValues(alpha: 0.4)),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.error_outline, color: AppTheme.accentError, size: 18),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          state.errorMessage!,
                          style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 12),
              ],

              // 2. Corpo do Passo Ativo
              Expanded(
                child: _buildCurrentStepWidget(state.step),
              ),
              const SizedBox(height: 16),

              // 3. Rodapé de Ação (Avançar / Voltar)
              if (!isFinalSuccess)
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    if (state.step != SalesOnboardingStep.feasibility)
                      OutlinedButton.icon(
                        onPressed: () => notifier.previousStep(),
                        icon: const Icon(Icons.arrow_back, size: 16),
                        label: const Text('Voltar'),
                      )
                    else
                      const SizedBox.shrink(),

                    if (state.step != SalesOnboardingStep.confirmation)
                      ElevatedButton.icon(
                        onPressed: () => notifier.nextStep(),
                        icon: const Icon(Icons.arrow_forward, size: 16),
                        label: const Text('Avançar'),
                        style: ElevatedButton.styleFrom(
                          padding: const EdgeInsets.symmetric(horizontal: 20, vertical: 12),
                        ),
                      ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildCurrentStepWidget(SalesOnboardingStep step) {
    switch (step) {
      case SalesOnboardingStep.feasibility:
        return const StepSalesFeasibility();
      case SalesOnboardingStep.planSelection:
        return const StepSalesPlanPicker();
      case SalesOnboardingStep.customerData:
        return const StepSalesCustomerForm();
      case SalesOnboardingStep.confirmation:
        return const StepSalesSummaryConfirmation();
    }
  }

  double _getStepProgress(SalesOnboardingStep step) {
    switch (step) {
      case SalesOnboardingStep.feasibility:
        return 0.25;
      case SalesOnboardingStep.planSelection:
        return 0.50;
      case SalesOnboardingStep.customerData:
        return 0.75;
      case SalesOnboardingStep.confirmation:
        return 1.0;
    }
  }

  String _getStepSubtitle(SalesOnboardingStep step) {
    switch (step) {
      case SalesOnboardingStep.feasibility:
        return 'Passo 1 de 4: Endereço & Viabilidade de Fibra';
      case SalesOnboardingStep.planSelection:
        return 'Passo 2 de 4: Plano Comercial & Vencimento';
      case SalesOnboardingStep.customerData:
        return 'Passo 3 de 4: Dados Pessoais do Assinante';
      case SalesOnboardingStep.confirmation:
        return 'Passo 4 de 4: Resumo & Emissão de Contrato';
    }
  }
}
