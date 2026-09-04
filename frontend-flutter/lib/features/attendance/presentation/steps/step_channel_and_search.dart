import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../data/attendance_models.dart';
import '../../data/attendance_notifier.dart';

/// Passo 1 do Hub de Atendimento: Seleção do canal de entrada e busca do cliente.
class StepChannelAndSearch extends ConsumerStatefulWidget {
  const StepChannelAndSearch({super.key});

  @override
  ConsumerState<StepChannelAndSearch> createState() => _StepChannelAndSearchState();
}

class _StepChannelAndSearchState extends ConsumerState<StepChannelAndSearch> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 1. Seletor de Canal de Entrada Omnichannel
        const Text(
          'Canal de Atendimento:',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
        ),
        const SizedBox(height: 8),
        Wrap(
          spacing: 8,
          runSpacing: 8,
          children: AttendanceChannel.values.map((channel) {
            final isSelected = state.channel == channel;
            IconData icon;
            switch (channel) {
              case AttendanceChannel.presential:
                icon = Icons.storefront_rounded;
                break;
              case AttendanceChannel.phone:
                icon = Icons.phone_in_talk_rounded;
                break;
              case AttendanceChannel.whatsapp:
                icon = Icons.chat_rounded;
                break;
              case AttendanceChannel.email:
                icon = Icons.mail_outline_rounded;
                break;
            }

            return ChoiceChip(
              avatar: Icon(icon, size: 16, color: isSelected ? Colors.white : AppTheme.textSecondary),
              label: Text(channel.label),
              selected: isSelected,
              selectedColor: AppTheme.primaryBlue,
              backgroundColor: AppTheme.darkSurface,
              labelStyle: TextStyle(
                color: isSelected ? Colors.white : AppTheme.textPrimary,
                fontWeight: isSelected ? FontWeight.bold : FontWeight.normal,
                fontSize: 12,
              ),
              onSelected: (val) {
                if (val) notifier.setChannel(channel);
              },
            );
          }).toList(),
        ),
        const SizedBox(height: 16),

        // 2. Campo de Busca Unificada
        const Text(
          'Identificar Cliente:',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13, color: AppTheme.textSecondary),
        ),
        const SizedBox(height: 8),
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
                        'Verifique os dígitos do CPF ou o nome digitado.',
                        style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
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
