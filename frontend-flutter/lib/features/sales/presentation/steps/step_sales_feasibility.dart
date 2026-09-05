import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/sales_models.dart';
import '../../data/sales_onboarding_notifier.dart';

/// Passo 1 do Onboarding de Venda Expressa: Endereço & Viabilidade FTTH com GeoCEP Inteligente.
class StepSalesFeasibility extends ConsumerStatefulWidget {
  const StepSalesFeasibility({super.key});

  @override
  ConsumerState<StepSalesFeasibility> createState() => _StepSalesFeasibilityState();
}

class _StepSalesFeasibilityState extends ConsumerState<StepSalesFeasibility> {
  late TextEditingController _searchCtrl;
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
    _searchCtrl = TextEditingController(text: state.searchQuery.isNotEmpty ? state.searchQuery : state.cep);
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
    _searchCtrl.dispose();
    _cepCtrl.dispose();
    _streetCtrl.dispose();
    _numberCtrl.dispose();
    _complementCtrl.dispose();
    _neighborhoodCtrl.dispose();
    _cityCtrl.dispose();
    _stateCtrl.dispose();
    super.dispose();
  }

  void _triggerSearch() {
    final query = _searchCtrl.text.trim();
    if (query.isNotEmpty) {
      ref.read(salesOnboardingProvider.notifier).performSmartSearch(query);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(salesOnboardingProvider);
    final notifier = ref.read(salesOnboardingProvider.notifier);

    // Sincroniza controllers se preenchido via busca inteligente
    if (_cepCtrl.text != state.cep && state.cep.isNotEmpty) {
      _cepCtrl.text = CpfUtils.formatCep(state.cep);
    }
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
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              const Text(
                '1. Localização & Viabilidade GeoCEP',
                style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
              ),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                decoration: BoxDecoration(
                  color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                  borderRadius: BorderRadius.circular(6),
                ),
                child: const Text(
                  'CEP • Rua • Coordenadas GPS',
                  style: TextStyle(color: AppTheme.primaryBlue, fontSize: 10, fontWeight: FontWeight.bold),
                ),
              ),
            ],
          ),
          const SizedBox(height: 4),
          const Text(
            'Busque por CEP (68371-000), nome da rua (ex: Djalma Dutra) ou coordenadas (-3.2107, -52.2371).',
            style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 14),

          // Barra de Busca Inteligente GeoCEP + Botão GPS Físico
          Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Expanded(
                child: TextField(
                  controller: _searchCtrl,
                  decoration: InputDecoration(
                    hintText: 'Digite o CEP, Nome da Rua ou Coordenadas...',
                    prefixIcon: const Icon(Icons.explore_outlined, color: AppTheme.primaryBlue, size: 20),
                    suffixIcon: state.isSearchingAddress
                        ? const Padding(
                            padding: EdgeInsets.all(12.0),
                            child: SizedBox(
                              width: 16,
                              height: 16,
                              child: CircularProgressIndicator(strokeWidth: 2),
                            ),
                          )
                        : (_searchCtrl.text.isNotEmpty
                            ? IconButton(
                                icon: const Icon(Icons.clear, size: 18),
                                onPressed: () {
                                  _searchCtrl.clear();
                                  notifier.clearSuggestions();
                                },
                              )
                            : null),
                  ),
                  onSubmitted: (_) => _triggerSearch(),
                ),
              ),
              const SizedBox(width: 8),
              ElevatedButton.icon(
                onPressed: state.isSearchingAddress ? null : _triggerSearch,
                icon: const Icon(Icons.search, size: 18),
                label: const Text('Buscar'),
                style: ElevatedButton.styleFrom(
                  padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 14),
                ),
              ),
              const SizedBox(width: 8),
              OutlinedButton.icon(
                onPressed: (state.isSearchingAddress || state.isAcquiringGps)
                    ? null
                    : () async {
                        await notifier.acquireDeviceLocation();
                        final s = ref.read(salesOnboardingProvider);
                        if (s.street.isNotEmpty) {
                          _searchCtrl.text = s.street;
                          _streetCtrl.text = s.street;
                          _cityCtrl.text = s.city;
                          _stateCtrl.text = s.state;
                          _neighborhoodCtrl.text = s.neighborhood;
                          _cepCtrl.text = CpfUtils.formatCep(s.cep);
                        }
                      },
                icon: state.isAcquiringGps
                    ? const SizedBox(
                        width: 16,
                        height: 16,
                        child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.accentGreen),
                      )
                    : const Icon(Icons.my_location_rounded, size: 18, color: AppTheme.accentGreen),
                label: Text(
                  state.isAcquiringGps ? 'Buscando GPS...' : 'Usar GPS',
                  style: const TextStyle(color: AppTheme.accentGreen, fontWeight: FontWeight.bold),
                ),
                style: OutlinedButton.styleFrom(
                  side: BorderSide(color: AppTheme.accentGreen.withValues(alpha: 0.6)),
                  padding: const EdgeInsets.symmetric(vertical: 16, horizontal: 12),
                ),
              ),
            ],
          ),

          if (state.isAcquiringGps) ...[
            const SizedBox(height: 8),
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: AppTheme.accentGreen.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(6),
                border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.3)),
              ),
              child: const Row(
                children: [
                  SizedBox(
                    width: 14,
                    height: 14,
                    child: CircularProgressIndicator(strokeWidth: 2, color: AppTheme.accentGreen),
                  ),
                  SizedBox(width: 10),
                  Expanded(
                    child: Text(
                      'Sintonizando satélites GPS do dispositivo com alta precisão...',
                      style: TextStyle(fontSize: 12, color: AppTheme.accentGreen),
                    ),
                  ),
                ],
              ),
            ),
          ],

          // Lista de Sugestões de Ruas Encontradas no GeoCEP
          if (state.searchSuggestions.isNotEmpty) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.darkSurface,
                borderRadius: BorderRadius.circular(10),
                border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    mainAxisAlignment: MainAxisAlignment.spaceBetween,
                    children: [
                      Text(
                        'Selecione o endereço encontrado (${state.searchSuggestions.length}):',
                        style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12, color: AppTheme.primaryBlue),
                      ),
                      IconButton(
                        icon: const Icon(Icons.close, size: 16),
                        padding: EdgeInsets.zero,
                        constraints: const BoxConstraints(),
                        onPressed: () => notifier.clearSuggestions(),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  ConstrainedBox(
                    constraints: const BoxConstraints(maxHeight: 180),
                    child: ListView.separated(
                      shrinkWrap: true,
                      itemCount: state.searchSuggestions.length,
                      separatorBuilder: (_, index) => const Divider(height: 1),
                      itemBuilder: (context, index) {
                        final item = state.searchSuggestions[index];
                        return ListTile(
                          dense: true,
                          contentPadding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                          leading: const Icon(Icons.location_city_outlined, size: 18, color: AppTheme.primaryBlue),
                          title: Text(
                            item.street,
                            style: const TextStyle(fontSize: 13, fontWeight: FontWeight.w600),
                          ),
                          subtitle: Text(
                            '${item.neighborhood.isNotEmpty ? "${item.neighborhood}, " : ""}${item.city} - ${item.state} (CEP ${CpfUtils.formatCep(item.cep)})',
                            style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          ),
                          trailing: const Icon(Icons.arrow_forward_ios, size: 12, color: AppTheme.textSecondary),
                          onTap: () {
                            notifier.selectSuggestion(item);
                            _searchCtrl.text = item.street;
                            _streetCtrl.text = item.street;
                            _neighborhoodCtrl.text = item.neighborhood;
                            _cityCtrl.text = item.city;
                            _stateCtrl.text = item.state;
                            _cepCtrl.text = CpfUtils.formatCep(item.cep);
                          },
                        );
                      },
                    ),
                  ),
                ],
              ),
            ),
          ],

          const SizedBox(height: 14),

          // Badge de Viabilidade Técnica FTTH Real
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

          // Campos de Endereço Preenchidos / Editáveis
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

          if (state.latitude != null && state.longitude != null) ...[
            const SizedBox(height: 12),
            Container(
              padding: const EdgeInsets.all(12),
              decoration: BoxDecoration(
                color: AppTheme.darkSurface,
                borderRadius: BorderRadius.circular(8),
                border: Border.all(color: AppTheme.darkBorder),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Row(
                    children: [
                      const Icon(Icons.gps_fixed, size: 16, color: AppTheme.primaryBlue),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Coordenadas GPS: ${state.latitude!.toStringAsFixed(6)}, ${state.longitude!.toStringAsFixed(6)}'
                          '${state.gpsAccuracy != null ? " (Precisão: ±${state.gpsAccuracy!.toStringAsFixed(1)}m)" : ""}',
                          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.textPrimary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 10),
                  const Divider(height: 1),
                  const SizedBox(height: 10),

                  // Área de Contribuição Colaborativa GeoCEP (Crowdsourcing)
                  if (state.hasContributedCoordinate)
                    Container(
                      padding: const EdgeInsets.all(10),
                      decoration: BoxDecoration(
                        color: AppTheme.accentGreen.withValues(alpha: 0.12),
                        borderRadius: BorderRadius.circular(6),
                        border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.4)),
                      ),
                      child: Row(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          const Icon(Icons.verified_rounded, color: AppTheme.accentGreen, size: 18),
                          const SizedBox(width: 8),
                          Expanded(
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                const Text(
                                  'Coordenada Registrada no GeoCEP!',
                                  style: TextStyle(
                                    fontSize: 12,
                                    fontWeight: FontWeight.bold,
                                    color: AppTheme.accentGreen,
                                  ),
                                ),
                                if (state.coordinateContributionMessage != null && state.coordinateContributionMessage!.isNotEmpty) ...[
                                  const SizedBox(height: 2),
                                  Text(
                                    state.coordinateContributionMessage!,
                                    style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                                  ),
                                ],
                              ],
                            ),
                          ),
                        ],
                      ),
                    )
                  else
                    Wrap(
                      alignment: WrapAlignment.spaceBetween,
                      crossAxisAlignment: WrapCrossAlignment.center,
                      spacing: 12,
                      runSpacing: 8,
                      children: [
                        ConstrainedBox(
                          constraints: const BoxConstraints(maxWidth: 420),
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: const [
                              Text(
                                'Atualizar Coordenada Precisa no GeoCEP',
                                style: TextStyle(fontSize: 12, fontWeight: FontWeight.w600, color: AppTheme.textPrimary),
                              ),
                              SizedBox(height: 2),
                              Text(
                                'Contribua com o posicionamento real deste imóvel para atualizar a base cartográfica colaborativa do GeoCEP.',
                                style: TextStyle(fontSize: 10, color: AppTheme.textSecondary),
                              ),
                            ],
                          ),
                        ),
                        ElevatedButton.icon(
                          onPressed: state.isContributingCoordinate
                              ? null
                              : () => notifier.contributeCoordinateToGeoCep(),
                          icon: state.isContributingCoordinate
                              ? const SizedBox(
                                  width: 14,
                                  height: 14,
                                  child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white),
                                )
                              : const Icon(Icons.cloud_upload_outlined, size: 16),
                          label: Text(
                            state.isContributingCoordinate ? 'Atualizando...' : 'Atualizar no GeoCEP',
                            style: const TextStyle(fontSize: 12),
                          ),
                          style: ElevatedButton.styleFrom(
                            backgroundColor: AppTheme.primaryBlue,
                            foregroundColor: Colors.white,
                            padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                          ),
                        ),
                      ],
                    ),
                ],
              ),
            ),
          ] else if (state.street.trim().isNotEmpty) ...[
            const SizedBox(height: 12),
            SizedBox(
              width: double.infinity,
              child: OutlinedButton.icon(
                onPressed: state.isSearchingAddress
                    ? null
                    : () {
                        final numPart = state.number.trim().isNotEmpty ? ' ${state.number.trim()}' : '';
                        final cityPart = state.city.trim().isNotEmpty ? ' ${state.city.trim()}' : '';
                        final q = '${state.street}$numPart$cityPart';
                        _searchCtrl.text = q;
                        notifier.performSmartSearch(q);
                      },
                icon: const Icon(Icons.location_searching, size: 16, color: AppTheme.primaryBlue),
                label: const Text('Localizar Coordenadas e Checar Viabilidade de Fibra'),
                style: OutlinedButton.styleFrom(
                  side: BorderSide(color: AppTheme.primaryBlue.withValues(alpha: 0.5)),
                  padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 16),
                ),
              ),
            ),
          ],
        ],
      ),
    );
  }
}
