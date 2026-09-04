import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/sales_models.dart';
import '../../data/sales_onboarding_notifier.dart';

/// Passo 1 do Onboarding de Venda Expressa: Endereço & Viabilidade FTTH.
class StepSalesFeasibility extends ConsumerStatefulWidget {
  const StepSalesFeasibility({super.key});

  @override
  ConsumerState<StepSalesFeasibility> createState() => _StepSalesFeasibilityState();
}

class _StepSalesFeasibilityState extends ConsumerState<StepSalesFeasibility> {
  late TextEditingController _cepCtrl;
  late TextEditingController _streetCtrl;
  late TextEditingController _numberCtrl;
  late TextEditingController _complementCtrl;
  late TextEditingController _neighborhoodCtrl;
  late TextEditingController _cityCtrl;
  late TextEditingController _stateCtrl;

  @override
  void initState() {
    super.initState();
    final state = ref.read(salesOnboardingProvider);
    _cepCtrl = TextEditingController(text: CpfUtils.formatCep(state.cep));
    _streetCtrl = TextEditingController(text: state.street);
    _numberCtrl = TextEditingController(text: state.number);
    _complementCtrl = TextEditingController(text: state.complement);
    _neighborhoodCtrl = TextEditingController(text: state.neighborhood);
    _cityCtrl = TextEditingController(text: state.city);
    _stateCtrl = TextEditingController(text: state.state);
  }

  @override
  void dispose() {
    _cepCtrl.dispose;
    _streetCtrl.dispose();
    _numberCtrl.dispose();
    _complementCtrl.dispose();
    _neighborhoodCtrl.dispose();
    _cityCtrl.dispose();
    _stateCtrl.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(salesOnboardingProvider);
    final notifier = ref.read(salesOnboardingProvider.notifier);

    // Sincroniza controllers se preenchido via GeoCep
    if (_streetCtrl.text != state.street && state.street.isNotEmpty) {
      _streetCtrl.text = state.street;
    }
    if (_neighborhoodCtrl.text != state.neighborhood && state.neighborhood.isNotEmpty) {
      _neighborhoodCtrl.text = state.neighborhood;
    }
    if (_cityCtrl.text != state.city && state.city.isNotEmpty) {
      _cityCtrl.text = state.city;
    }
    if (_stateCtrl.text != state.state && state.state.isNotEmpty) {
      _stateCtrl.text = state.state;
    }

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          const Text(
            '1. Consulta de Viabilidade & Endereço',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 4),
          const Text(
            'Digite o CEP para localizar a rota de fibra e verificar portas livres na CTO mais próxima.',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 16),

          // Campo CEP com botão de busca
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                flex: 3,
                child: TextField(
                  controller: _cepCtrl,
                  keyboardType: TextInputType.number,
                  decoration: InputDecoration(
                    labelText: 'CEP *',
                    hintText: '00000-000',
                    prefixIcon: const Icon(Icons.location_on_outlined, size: 20),
                    suffixIcon: state.isSearchingCep
                        ? const Padding(
                            padding: EdgeInsets.all(12.0),
                            child: SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          )
                        : null,
                  ),
                  onChanged: (val) => notifier.setCep(val),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                flex: 2,
                child: ElevatedButton.icon(
                  onPressed: state.isSearchingCep
                      ? null
                      : () => notifier.searchCep(CpfUtils.clean(_cepCtrl.text)),
                  icon: const Icon(Icons.search, size: 18),
                  label: const Text('Buscar'),
                  style: ElevatedButton.styleFrom(
                    padding: const EdgeInsets.symmetric(vertical: 16),
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 14),

          // Badge de Viabilidade Técnica FTTH (Real)
          if (state.isCheckingFeasibility)
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.primaryBlue.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.3)),
              ),
              child: Row(
                children: const [
                  SizedBox(
                    width: 16,
                    height: 16,
                    child: CircularProgressIndicator(strokeWidth: 2),
                  ),
                  SizedBox(width: 12),
                  Expanded(
                    child: Text(
                      'Calculando viabilidade contra CTOs e portas livres...',
                      style: TextStyle(fontSize: 12, color: AppTheme.primaryBlue),
                    ),
                  ),
                ],
              ),
            )
          else if (state.feasibility != null)
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: state.feasibility!.viable
                    ? AppTheme.accentGreen.withValues(alpha: 0.1)
                    : AppTheme.accentWarning.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: state.feasibility!.viable
                      ? AppTheme.accentGreen.withValues(alpha: 0.4)
                      : AppTheme.accentWarning.withValues(alpha: 0.4),
                ),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      Icon(
                        state.feasibility!.viable
                            ? Icons.check_circle_rounded
                            : Icons.warning_amber_rounded,
                        color: state.feasibility!.viable
                            ? AppTheme.accentGreen
                            : AppTheme.accentWarning,
                        size: 20,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          state.feasibility!.viable
                              ? 'Viabilidade de Fibra Confirmada!'
                              : 'Sem portas livres no raio imediato (Demanda de Expansão)',
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                            color: state.feasibility!.viable
                                ? AppTheme.accentGreen
                                : AppTheme.accentWarning,
                          ),
                        ),
                      ),
                    ],
                  ),
                  if (state.feasibility!.nearbyCtos.isNotEmpty) ...[
                    const SizedBox(height: 6),
                    Text(
                      'CTO mais próxima: ${state.feasibility!.nearbyCtos.first.ctoName} '
                      '(${state.feasibility!.nearbyCtos.first.distanceMeters.toStringAsFixed(1)}m de distância) '
                      '- ${state.feasibility!.nearbyCtos.first.freePorts} portas livres',
                      style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                    ),
                  ],
                ],
              ),
            ),

          const SizedBox(height: 16),

          // Campos de Endereço Completo
          Row(
            children: [
              Expanded(
                flex: 4,
                child: TextField(
                  controller: _streetCtrl,
                  decoration: const InputDecoration(labelText: 'Logradouro / Rua *'),
                  onChanged: (val) => notifier.setStreet(val),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                flex: 2,
                child: TextField(
                  controller: _numberCtrl,
                  decoration: const InputDecoration(labelText: 'Número *'),
                  onChanged: (val) => notifier.setNumber(val),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          Row(
            children: [
              Expanded(
                flex: 3,
                child: TextField(
                  controller: _complementCtrl,
                  decoration: const InputDecoration(labelText: 'Complemento (Apto, Bloco)'),
                  onChanged: (val) => notifier.setComplement(val),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                flex: 3,
                child: TextField(
                  controller: _neighborhoodCtrl,
                  decoration: const InputDecoration(labelText: 'Bairro *'),
                  onChanged: (val) => notifier.setNeighborhood(val),
                ),
              ),
            ],
          ),
          const SizedBox(height: 12),

          Row(
            children: [
              Expanded(
                flex: 4,
                child: TextField(
                  controller: _cityCtrl,
                  decoration: const InputDecoration(labelText: 'Cidade *'),
                  onChanged: (val) => notifier.setCity(val),
                ),
              ),
              const SizedBox(width: 8),
              Expanded(
                flex: 2,
                child: TextField(
                  controller: _stateCtrl,
                  decoration: const InputDecoration(labelText: 'UF *'),
                  onChanged: (val) => notifier.setStateUf(val),
                ),
              ),
            ],
          ),
        ],
      ),
    );
  }
}
