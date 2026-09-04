import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/sales_onboarding_notifier.dart';

/// Passo 2 do Onboarding de Venda Expressa: Catálogo de Planos Comerciais & Vencimento.
class StepSalesPlanPicker extends ConsumerWidget {
  const StepSalesPlanPicker({super.key});

  String _formatCurrency(double val) {
    final fmt = NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$');
    return fmt.format(val);
  }

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final state = ref.watch(salesOnboardingProvider);
    final notifier = ref.read(salesOnboardingProvider.notifier);

    final plans = state.plans;
    final selectedPlan = state.selectedPlan;

    if (state.isLoadingPlans) {
      return const Center(
        child: Padding(
          padding: EdgeInsets.all(48.0),
          child: CircularProgressIndicator(),
        ),
      );
    }

    if (plans.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.inventory_2_outlined, color: AppTheme.textSecondary, size: 48),
            const SizedBox(height: 12),
            const Text(
              'Nenhum plano ativo encontrado no catálogo.',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
            ),
            const SizedBox(height: 6),
            const Text(
              'Cadastre planos comerciais no módulo de Gestão de Planos para habilitar vendas.',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            OutlinedButton.icon(
              onPressed: () => notifier.loadActivePlans(),
              icon: const Icon(Icons.refresh, size: 16),
              label: const Text('Tentar novamente'),
            ),
          ],
        ),
      );
    }

    final dueDays = [5, 10, 15, 20, 25];

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            '2. Seleção de Plano & Vencimento',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          const Text(
            'Escolha o pacote de velocidade e a data preferencial de vencimento da mensalidade.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 16),

          // Lista de Planos Ativos
          ListView.separated(
            shrinkWrap: true,
            physics: const NeverScrollableScrollPhysics(),
            itemCount: plans.length,
            separatorBuilder: (_, index) => const SizedBox(height: 8),
            itemBuilder: (context, index) {
              final plan = plans[index];
              final isSelected = selectedPlan?.id == plan.id;

              return InkWell(
                onTap: () => notifier.selectPlan(plan),
                borderRadius: BorderRadius.circular(10),
                child: Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: isSelected
                        ? AppTheme.primaryBlue.withValues(alpha: 0.12)
                        : AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(
                      color: isSelected ? AppTheme.primaryBlue : AppTheme.darkBorder,
                      width: isSelected ? 1.5 : 1.0,
                    ),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        isSelected ? Icons.radio_button_checked : Icons.radio_button_unchecked,
                        color: isSelected ? AppTheme.primaryBlue : AppTheme.textSecondary,
                        size: 22,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              plan.name,
                              style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              '${plan.downloadSpeed} Mbps Download / ${plan.uploadSpeed} Mbps Upload',
                              style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                            ),
                            if (plan.description != null && plan.description!.isNotEmpty) ...[
                              const SizedBox(height: 4),
                              Text(
                                plan.description!,
                                style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                                maxLines: 1,
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ],
                        ),
                      ),
                      const SizedBox(width: 12),
                      Column(
                        crossAxisAlignment: CrossAxisAlignment.end,
                        children: [
                          Text(
                            _formatCurrency(plan.price),
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                              color: AppTheme.accentGreen,
                            ),
                          ),
                          const Text(
                            '/mês',
                            style: TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
              );
            },
          ),
          const SizedBox(height: 20),

          // Escolha do Dia de Vencimento
          const Text(
            'Dia preferencial de vencimento:',
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 8),
          Wrap(
            spacing: 8,
            children: dueDays.map((day) {
              final isSelected = state.preferredDueDate == day;
              return ChoiceChip(
                label: Text('Dia $day'),
                selected: isSelected,
                selectedColor: AppTheme.primaryBlue,
                labelStyle: TextStyle(
                  color: isSelected ? Colors.white : AppTheme.textPrimary,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
                onSelected: (_) => notifier.setPreferredDueDate(day),
              );
            }).toList(),
          ),
        ],
      ),
    );
  }
}
