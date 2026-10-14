import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/technician_execution_repository.dart';
import '../data/technician_repository.dart';
import 'technician_field_execution_modal.dart';

class TechnicianDashboardScreen extends ConsumerStatefulWidget {
  const TechnicianDashboardScreen({super.key});

  @override
  ConsumerState<TechnicianDashboardScreen> createState() =>
      _TechnicianDashboardScreenState();
}

class _TechnicianDashboardScreenState
    extends ConsumerState<TechnicianDashboardScreen> {
  bool _isLoading = true;
  List<Map<String, dynamic>> _workOrders = [];
  String _selectedFilter = 'TODAS';

  @override
  void initState() {
    super.initState();
    _loadWorkOrders();
  }

  Future<void> _loadWorkOrders() async {
    setState(() => _isLoading = true);
    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      final list = await repo.listWorkOrders();
      if (!mounted) return;
      setState(() {
        _workOrders = list;
        _isLoading = false;
      });
    } catch (_) {
      if (!mounted) return;
      setState(() => _isLoading = false);
    }
  }

  Future<void> _startExecution(String workOrderId) async {
    try {
      final repo = ref.read(technicianExecutionRepositoryProvider);
      await repo.startWorkOrder(workOrderId);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(
            content: Text('Deslocamento e execução iniciados!'),
            backgroundColor: AppTheme.primaryBlue,
          ),
        );
      }
      await _loadWorkOrders();
      ref.invalidate(technicianWorkOrdersProvider);
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Falha ao iniciar O.S.: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    }
  }

  List<Map<String, dynamic>> _resolveAllOrders(
    List<TechnicianWorkOrderModel> providerOrders,
  ) {
    if (_workOrders.isNotEmpty) return _workOrders;
    return providerOrders
        .map(
          (m) => <String, dynamic>{
            'id': m.id,
            'contractId': m.contractId,
            'customerId': m.customerId,
            'type': m.type,
            'status': m.status,
            'scheduledDate': m.scheduledDate,
            'scheduledPeriod': m.scheduledPeriod,
            'technicianName': m.technicianName,
            'onuMac': m.onuMac,
            'onuSerial': m.onuSerial,
            'fiberSignalDbm': m.fiberSignalDbm,
            'onuRxPowerDbm': m.onuRxPowerDbm,
            'radiusAuthenticated': m.radiusAuthenticated,
            'customerSignatureName': m.customerSignatureName,
            'notes': m.notes,
          },
        )
        .toList();
  }

  List<Map<String, dynamic>> _filterOrders(List<Map<String, dynamic>> source) {
    if (_selectedFilter == 'TODAS') return source;
    if (_selectedFilter == 'RETIRADA') {
      return source
          .where(
            (w) =>
                (w['type']?.toString() ?? '').toUpperCase().contains('RETIRADA'),
          )
          .toList();
    }
    if (_selectedFilter == 'CONCLUIDAS') {
      return source.where((w) {
        final st = (w['status']?.toString() ?? '').toUpperCase();
        return st == 'CONCLUIDA' || st == 'COMPLETED' || st == 'INFRUTIFERA';
      }).toList();
    }
    return source.where((w) {
      final st = (w['status']?.toString() ?? '').toUpperCase();
      return st != 'CONCLUIDA' && st != 'COMPLETED' && st != 'INFRUTIFERA';
    }).toList();
  }

  @override
  Widget build(BuildContext context) {
    final asyncProviderOrders = ref.watch(technicianWorkOrdersProvider);
    final fallbackOrders = asyncProviderOrders.valueOrNull ?? const [];
    final allOrders = _resolveAllOrders(fallbackOrders);
    final orders = _filterOrders(allOrders);
    final showSpinner =
        _isLoading && allOrders.isEmpty && asyncProviderOrders.isLoading;

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(20.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              children: [
                const Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        'App do Técnico em Campo (Mobile / Desktop)',
                        style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      ),
                      SizedBox(height: 4),
                      Text(
                        'Auto-Find OLT, Provisionamento 1-Clique, Diagnóstico FreeRADIUS e Logística Reversa',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      ),
                    ],
                  ),
                ),
                IconButton(
                  onPressed: () {
                    _loadWorkOrders();
                    ref.invalidate(technicianWorkOrdersProvider);
                  },
                  icon: const Icon(Icons.refresh),
                  tooltip: 'Atualizar Fila',
                ),
              ],
            ),
            const SizedBox(height: 14),

            // Filtros rápidos da fila de campo
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                _buildFilterChip('TODAS', 'Todas (${allOrders.length})'),
                _buildFilterChip('EM_CAMPO', 'Pendentes / Em Campo'),
                _buildFilterChip('RETIRADA', 'Retirada de Comodato'),
                _buildFilterChip('CONCLUIDAS', 'Concluídas / Infrutíferas'),
              ],
            ),
            const SizedBox(height: 16),

            if (showSpinner)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(40),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (orders.isEmpty)
              Card(
                child: Padding(
                  padding: const EdgeInsets.all(32.0),
                  child: Center(
                    child: Text(
                      'Nenhuma Ordem de Serviço encontrada para o filtro selecionado.',
                      style: TextStyle(color: Colors.grey.shade400),
                    ),
                  ),
                ),
              )
            else
              ...orders.map((wo) {
                final id = wo['id']?.toString() ?? '';
                final shortId =
                    id.length > 8 ? id.substring(0, 8).toUpperCase() : id;
                final type = wo['type']?.toString() ?? 'INSTALACAO';
                final status = wo['status']?.toString() ?? 'ABERTA';
                final period = wo['scheduledPeriod']?.toString() ?? 'MANHA';
                final customerName =
                    wo['customerName']?.toString() ?? 'Assinante';
                final address = wo['installationAddress']?.toString() ??
                    'Endereço da instalação';
                final onuSerial = wo['onuSerial']?.toString();
                final rxPower = wo['fiberSignalDbm'] ?? wo['onuRxPowerDbm'];
                final radiusAuth = wo['radiusAuthenticated'] == true;
                final isCompleted = status == 'CONCLUIDA' ||
                    status == 'COMPLETED' ||
                    status == 'INFRUTIFERA';
                final isInProgress =
                    status == 'EM_ANDAMENTO' || status == 'IN_PROGRESS';

                return Padding(
                  padding: const EdgeInsets.only(bottom: 12.0),
                  child: Card(
                    child: Padding(
                      padding: const EdgeInsets.all(18.0),
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Wrap(
                            spacing: 8,
                            runSpacing: 6,
                            alignment: WrapAlignment.spaceBetween,
                            crossAxisAlignment: WrapCrossAlignment.center,
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(
                                  horizontal: 10,
                                  vertical: 4,
                                ),
                                decoration: BoxDecoration(
                                  color: (isCompleted
                                          ? AppTheme.accentGreen
                                          : AppTheme.primaryBlue)
                                      .withValues(alpha: 0.15),
                                  borderRadius: BorderRadius.circular(20),
                                ),
                                child: Text(
                                  'O.S. #$shortId • $type • $status',
                                  style: TextStyle(
                                    color: isCompleted
                                        ? AppTheme.accentGreen
                                        : AppTheme.primaryBlue,
                                    fontSize: 11,
                                    fontWeight: FontWeight.bold,
                                  ),
                                ),
                              ),
                              if (radiusAuth)
                                Container(
                                  padding: const EdgeInsets.symmetric(
                                    horizontal: 8,
                                    vertical: 3,
                                  ),
                                  decoration: BoxDecoration(
                                    color: AppTheme.accentGreen.withValues(
                                      alpha: 0.15,
                                    ),
                                    borderRadius: BorderRadius.circular(12),
                                  ),
                                  child: const Text(
                                    'RADIUS ONLINE',
                                    style: TextStyle(
                                      color: AppTheme.accentGreen,
                                      fontSize: 10,
                                      fontWeight: FontWeight.bold,
                                    ),
                                  ),
                                ),
                              Text(
                                'Turno: $period',
                                style: const TextStyle(
                                  color: AppTheme.textSecondary,
                                  fontSize: 12,
                                ),
                              ),
                            ],
                          ),
                          const SizedBox(height: 10),
                          Text(
                            customerName,
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                          const SizedBox(height: 4),
                          Text(
                            address,
                            style: const TextStyle(
                              fontSize: 13,
                              color: AppTheme.textSecondary,
                            ),
                          ),
                          if (onuSerial != null && onuSerial.isNotEmpty) ...[
                            const SizedBox(height: 6),
                            Text(
                              'ONU: $onuSerial ${rxPower != null ? "• Sinal: $rxPower dBm" : ""}',
                              style: const TextStyle(
                                fontSize: 12,
                                color: AppTheme.textSecondary,
                              ),
                            ),
                          ],
                          const SizedBox(height: 14),
                          if (!isCompleted)
                            Wrap(
                              spacing: 10,
                              runSpacing: 8,
                              children: [
                                if (!isInProgress)
                                  OutlinedButton.icon(
                                    onPressed: () => _startExecution(id),
                                    icon: const Icon(
                                      Icons.directions_car_outlined,
                                      size: 16,
                                    ),
                                    label: const Text('Iniciar Deslocamento'),
                                  ),
                                ElevatedButton.icon(
                                  onPressed: () =>
                                      TechnicianFieldExecutionModal.show(
                                    context,
                                    workOrder: wo,
                                    onUpdated: () {
                                      _loadWorkOrders();
                                      ref.invalidate(
                                        technicianWorkOrdersProvider,
                                      );
                                    },
                                  ),
                                  icon: Icon(
                                    type.toUpperCase().contains('RETIRADA')
                                        ? Icons.assignment_return_outlined
                                        : Icons.router_outlined,
                                    size: 16,
                                  ),
                                  label: Text(
                                    type.toUpperCase().contains('RETIRADA')
                                        ? 'Executar Logística Reversa'
                                        : 'Executar O.S.',
                                  ),
                                ),
                              ],
                            ),
                        ],
                      ),
                    ),
                  ),
                );
              }),
          ],
        ),
      ),
    );
  }

  Widget _buildFilterChip(String key, String label) {
    final selected = _selectedFilter == key;
    return ChoiceChip(
      selected: selected,
      label: Text(
        label,
        style: TextStyle(
          fontSize: 12,
          fontWeight: selected ? FontWeight.bold : FontWeight.normal,
        ),
      ),
      selectedColor: AppTheme.primaryBlue.withValues(alpha: 0.25),
      onSelected: (_) => setState(() => _selectedFilter = key),
    );
  }
}
