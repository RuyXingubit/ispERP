import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../sales/data/sales_models.dart';
import '../../../sales/data/sales_onboarding_notifier.dart';
import '../../../sales/presentation/sales_onboarding_modal.dart';
import '../../data/attendance_models.dart';
import '../../data/attendance_notifier.dart';

/// Passo 1 do Hub de Atendimento: Seleção do canal de entrada e busca do cliente.
class StepChannelAndSearch extends ConsumerStatefulWidget {
  const StepChannelAndSearch({super.key});

  @override
  ConsumerState<StepChannelAndSearch> createState() => _StepChannelAndSearchState();
}

class _StepChannelAndSearchState extends ConsumerState<StepChannelAndSearch> {
  late TextEditingController _searchController;

  @override
  void initState() {
    super.initState();
    _searchController = TextEditingController();
  }

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  void _openSalesOnboarding(String? initialQuery) {
    if (initialQuery != null && initialQuery.isNotEmpty) {
      final salesNotifier = ref.read(salesOnboardingProvider.notifier);
      final clean = CpfUtils.clean(initialQuery);
      if (clean.length == 11) {
        salesNotifier.setCustomerCpf(clean);
      } else if (clean.length != 8) {
        salesNotifier.setCustomerName(initialQuery.trim());
      }
    }
    SalesOnboardingModal.show(context);
  }

  Widget _buildChannelOption({
    required AttendanceChannel channel,
    required AttendanceChannel? current,
    required IconData icon,
    required String label,
    required VoidCallback onTap,
  }) {
    final isSelected = current == channel;
    return Expanded(
      child: GestureDetector(
        onTap: onTap,
        child: Container(
          padding: const EdgeInsets.symmetric(vertical: 8),
          decoration: BoxDecoration(
            color: isSelected ? AppTheme.primaryBlue : AppTheme.darkSurface,
            borderRadius: BorderRadius.circular(8),
            border: Border.all(
              color: isSelected ? AppTheme.primaryBlue : AppTheme.darkBorder,
            ),
          ),
          child: Column(
            children: [
              Icon(icon, size: 20, color: isSelected ? Colors.white : AppTheme.textSecondary),
              const SizedBox(height: 4),
              Text(
                label,
                style: TextStyle(
                  fontSize: 10,
                  color: isSelected ? Colors.white : AppTheme.textSecondary,
                  fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 1. Origem do Atendimento (Omnichannel)
        const Text(
          'Canal de Entrada:',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
        ),
        const SizedBox(height: 8),
        Row(
          children: [
            _buildChannelOption(
              channel: AttendanceChannel.presential,
              current: state.channel,
              icon: Icons.storefront_rounded,
              label: 'Presencial',
              onTap: () => notifier.setChannel(AttendanceChannel.presential),
            ),
            const SizedBox(width: 8),
            _buildChannelOption(
              channel: AttendanceChannel.phone,
              current: state.channel,
              icon: Icons.phone_in_talk_rounded,
              label: 'Telefone',
              onTap: () => notifier.setChannel(AttendanceChannel.phone),
            ),
            const SizedBox(width: 8),
            _buildChannelOption(
              channel: AttendanceChannel.whatsapp,
              current: state.channel,
              icon: Icons.chat_rounded,
              label: 'WhatsApp',
              onTap: () => notifier.setChannel(AttendanceChannel.whatsapp),
            ),
            const SizedBox(width: 8),
            _buildChannelOption(
              channel: AttendanceChannel.email,
              current: state.channel,
              icon: Icons.mail_outline_rounded,
              label: 'E-mail',
              onTap: () => notifier.setChannel(AttendanceChannel.email),
            ),
          ],
        ),
        const SizedBox(height: 16),

        // 2. Campo de Busca Unificada & Botão de Nova Venda
        Row(
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            const Text(
              'Identificar Cliente:',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
            ),
            TextButton.icon(
              onPressed: () => _openSalesOnboarding(_searchController.text),
              icon: const Icon(Icons.person_add_alt_1_outlined, size: 16, color: AppTheme.accentGreen),
              label: const Text(
                '+ Novo Assinante / Venda',
                style: TextStyle(color: AppTheme.accentGreen, fontSize: 12, fontWeight: FontWeight.bold),
              ),
            ),
          ],
        ),
        const SizedBox(height: 6),
        TextField(
          controller: _searchController,
          autofocus: true,
          decoration: InputDecoration(
            hintText: 'Digite o CPF (apenas números) ou Nome do assinante...',
            prefixIcon: const Icon(Icons.search, color: AppTheme.primaryBlue),
            suffixIcon: _searchController.text.isNotEmpty
                ? IconButton(
                    icon: const Icon(Icons.clear, size: 18),
                    onPressed: () {
                      _searchController.clear();
                      notifier.searchCustomers('');
                    },
                  )
                : null,
            filled: true,
            fillColor: AppTheme.darkSurface,
            border: OutlineInputBorder(
              borderRadius: BorderRadius.circular(10),
              borderSide: const BorderSide(color: AppTheme.darkBorder),
            ),
          ),
          onChanged: (val) => notifier.searchCustomers(val),
        ),
        const SizedBox(height: 12),

        // 3. Resultados da Busca em Tempo Real
        Expanded(
          child: Builder(
            builder: (context) {
              if (state.isSearching) {
                return const Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      CircularProgressIndicator(),
                      SizedBox(height: 12),
                      Text('Consultando base de assinantes...', style: TextStyle(color: AppTheme.textSecondary, fontSize: 13)),
                    ],
                  ),
                );
              }

              if (state.searchQuery.isNotEmpty && state.searchResults.isEmpty) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.person_off_outlined, size: 48, color: AppTheme.textSecondary.withValues(alpha: 0.5)),
                      const SizedBox(height: 12),
                      Text(
                        'Nenhum assinante encontrado para "${state.searchQuery}".',
                        style: const TextStyle(color: AppTheme.textSecondary, fontSize: 14),
                      ),
                      const SizedBox(height: 6),
                      const Text(
                        'Deseja cadastrar uma nova proposta comercial para este lead?',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                      ),
                      const SizedBox(height: 16),
                      ElevatedButton.icon(
                        onPressed: () => _openSalesOnboarding(state.searchQuery),
                        icon: const Icon(Icons.flash_on_rounded, size: 16),
                        label: const Text('Iniciar Venda Expressa'),
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.accentGreen,
                          foregroundColor: Colors.white,
                        ),
                      ),
                    ],
                  ),
                );
              }

              if (state.searchResults.isEmpty) {
                return Center(
                  child: Column(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Icon(Icons.support_agent_rounded, size: 48, color: AppTheme.primaryBlue.withValues(alpha: 0.3)),
                      const SizedBox(height: 12),
                      const Text(
                        'Digite para localizar o cadastro do assinante',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 14),
                      ),
                      const SizedBox(height: 4),
                      const Text(
                        'O sistema trará contratos, faturas abertas e O.S. ativas automaticamente',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                      ),
                    ],
                  ),
                );
              }

              return ListView.separated(
                itemCount: state.searchResults.length,
                separatorBuilder: (_, index) => const Divider(height: 1),
                itemBuilder: (context, index) {
                  final customer = state.searchResults[index];
                  return ListTile(
                    tileColor: AppTheme.darkSurface,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
                    leading: CircleAvatar(
                      backgroundColor: AppTheme.primaryBlue.withValues(alpha: 0.2),
                      child: Text(
                        customer.name.isNotEmpty ? customer.name[0].toUpperCase() : '?',
                        style: const TextStyle(color: AppTheme.primaryBlue, fontWeight: FontWeight.bold),
                      ),
                    ),
                    title: Text(customer.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14)),
                    subtitle: Text(
                      'CPF: ${customer.formattedCpf} • ${customer.city ?? "Altamira"}/${customer.state ?? "PA"}',
                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                    trailing: Row(
                      mainAxisSize: MainAxisSize.min,
                      children: [
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                          decoration: BoxDecoration(
                            color: customer.active
                                ? AppTheme.accentGreen.withValues(alpha: 0.15)
                                : AppTheme.accentError.withValues(alpha: 0.15),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            customer.active ? 'ATIVO' : 'INATIVO',
                            style: TextStyle(
                              color: customer.active ? AppTheme.accentGreen : AppTheme.accentError,
                              fontSize: 10,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        const SizedBox(width: 8),
                        const Icon(Icons.arrow_forward_ios_rounded, size: 14, color: AppTheme.primaryBlue),
                      ],
                    ),
                    onTap: () => notifier.selectCustomer(customer),
                  );
                },
              );
            },
          ),
        ),
      ],
    );
  }
}
