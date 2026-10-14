import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../../core/theme/app_theme.dart';
import '../../../support/data/helpdesk_models.dart';
import '../../../support/data/helpdesk_repository.dart';
import '../../../support/presentation/ticket_detail_modal.dart';
import '../../data/attendance_models.dart';
import '../../data/attendance_notifier.dart';

/// Passo integrado do Hub de Atendimento Guiado para abertura de Chamado ANATEL (N1/N2)
/// e consulta de protocolos anteriores do assinante selecionado.
class StepSupportTicket extends ConsumerStatefulWidget {
  const StepSupportTicket({super.key});

  @override
  ConsumerState<StepSupportTicket> createState() => _StepSupportTicketState();
}

class _StepSupportTicketState extends ConsumerState<StepSupportTicket> {
  String _category = 'CONNECTION_OUTAGE';
  String _priority = 'NORMAL';
  final _subjectCtrl = TextEditingController();
  final _descriptionCtrl = TextEditingController();
  final _attendantCtrl = TextEditingController(text: 'Atendente N1');
  bool _isCreating = false;
  HelpdeskTicket? _createdTicket;

  @override
  void initState() {
    super.initState();
    final intent = ref.read(attendanceProvider).selectedIntent;
    if (intent == AttendanceIntent.general) {
      _category = 'FINANCIAL';
    }
  }

  @override
  void dispose() {
    _subjectCtrl.dispose();
    _descriptionCtrl.dispose();
    _attendantCtrl.dispose();
    super.dispose();
  }

  String _mapChannel(AttendanceChannel ch) {
    switch (ch) {
      case AttendanceChannel.presential:
        return 'IN_PERSON';
      case AttendanceChannel.phone:
        return 'PHONE';
      case AttendanceChannel.whatsapp:
        return 'WHATSAPP_BOT';
      case AttendanceChannel.email:
        return 'EMAIL';
    }
  }

  Future<void> _submitTicket(String customerId, String? contractId, AttendanceChannel channel) async {
    final subject = _subjectCtrl.text.trim();
    final description = _descriptionCtrl.text.trim();
    if (subject.isEmpty || description.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Preencha o assunto e a descrição detalhada do atendimento.'),
          backgroundColor: AppTheme.accentWarning,
        ),
      );
      return;
    }

    setState(() => _isCreating = true);
    try {
      final repo = ref.read(helpdeskRepositoryProvider);
      final ticket = await repo.createTicket(
        customerId: customerId,
        contractId: contractId,
        category: _category,
        priority: _priority,
        channel: _mapChannel(channel),
        subject: subject,
        description: description,
        attendantName: _attendantCtrl.text.trim().isEmpty
            ? 'Atendente N1'
            : _attendantCtrl.text.trim(),
      );
      ref.invalidate(customerTicketsProvider(customerId));
      ref.invalidate(helpdeskTicketsProvider);
      if (mounted) {
        setState(() => _createdTicket = ticket);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erro ao gerar Protocolo Anatel: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreating = false);
    }
  }

  Future<void> _quickEscalateN2(HelpdeskTicket ticket) async {
    setState(() => _isCreating = true);
    try {
      final repo = ref.read(helpdeskRepositoryProvider);
      final updated = await repo.escalateToN2(
        ticketId: ticket.id,
        attendantName: _attendantCtrl.text.trim().isEmpty
            ? 'Atendente N1'
            : _attendantCtrl.text.trim(),
        reason: 'Escalonado imediatamente pelo N1 durante o atendimento guiado: ${ticket.description}',
      );
      ref.invalidate(customerTicketsProvider(ticket.customerId));
      ref.invalidate(helpdeskTicketsProvider);
      if (mounted) {
        setState(() => _createdTicket = updated);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Protocolo ${updated.protocol} encaminhado para a Fila N2 / NOC!'),
            backgroundColor: AppTheme.primaryBlue,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Falha ao escalonar para N2: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreating = false);
    }
  }

  Future<void> _quickCloseFirstContact(HelpdeskTicket ticket) async {
    setState(() => _isCreating = true);
    try {
      final repo = ref.read(helpdeskRepositoryProvider);
      final updated = await repo.closeTicket(
        ticketId: ticket.id,
        satisfactionRating: 5,
        closureNotes: 'Resolvido no 1º contato (N1) durante o atendimento ao assinante.',
      );
      ref.invalidate(customerTicketsProvider(ticket.customerId));
      ref.invalidate(helpdeskTicketsProvider);
      if (mounted) {
        setState(() => _createdTicket = updated);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Protocolo ${updated.protocol} concluído no 1º contato (N1)!'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Falha ao concluir protocolo: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isCreating = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(attendanceProvider);
    final notifier = ref.read(attendanceProvider.notifier);
    final customer = state.selectedCustomer;

    if (customer == null) {
      return const Center(child: Text('Nenhum assinante selecionado.'));
    }

    final historyAsync = ref.watch(customerTicketsProvider(customer.id));

    return SingleChildScrollView(
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Barra superior de retorno ao Raio-X
          Row(
            children: [
              TextButton.icon(
                onPressed: () => notifier.goBack(),
                icon: const Icon(Icons.arrow_back, size: 16),
                label: const Text('Voltar ao Raio-X 360'),
              ),
              const Spacer(),
              Text(
                'Assinante: ${customer.name}',
                style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary, fontWeight: FontWeight.bold),
              ),
            ],
          ),
          const SizedBox(height: 8),

          // Se um chamado acabou de ser criado nesta sessão, exibe o Banner de Protocolo Anatel Gerado
          if (_createdTicket != null) ...[
            Container(
              padding: const EdgeInsets.all(16),
              decoration: BoxDecoration(
                color: AppTheme.accentGreen.withValues(alpha: 0.1),
                borderRadius: BorderRadius.circular(12),
                border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.4)),
              ),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Wrap(
                    spacing: 10,
                    runSpacing: 8,
                    crossAxisAlignment: WrapCrossAlignment.center,
                    children: [
                      const Icon(Icons.verified_rounded, color: AppTheme.accentGreen, size: 22),
                      SelectableText(
                        'PROTOCOLO ANATEL GERADO: ${_createdTicket!.protocol}',
                        style: const TextStyle(
                          fontSize: 15,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.accentGreen,
                        ),
                      ),
                      IconButton(
                        onPressed: () {
                          Clipboard.setData(ClipboardData(text: _createdTicket!.protocol));
                          ScaffoldMessenger.of(context).showSnackBar(
                            SnackBar(content: Text('Protocolo ${_createdTicket!.protocol} copiado!')),
                          );
                        },
                        icon: const Icon(Icons.copy, size: 16, color: AppTheme.accentGreen),
                        tooltip: 'Copiar Protocolo',
                      ),
                    ],
                  ),
                  const SizedBox(height: 4),
                  Text(
                    'Status: ${_createdTicket!.statusLabel} • Prazo SLA Anatel: ${_createdTicket!.formattedSlaDeadline}',
                    style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                  const SizedBox(height: 12),
                  Wrap(
                    spacing: 10,
                    runSpacing: 8,
                    children: [
                      if (_createdTicket!.status.toUpperCase() == 'OPEN')
                        ElevatedButton.icon(
                          onPressed: _isCreating ? null : () => _quickEscalateN2(_createdTicket!),
                          icon: const Icon(Icons.trending_up, size: 16),
                          label: const Text('Encaminhar p/ Fila N2 / NOC'),
                        ),
                      if (_createdTicket!.isActive)
                        OutlinedButton.icon(
                          onPressed: _isCreating ? null : () => _quickCloseFirstContact(_createdTicket!),
                          icon: const Icon(Icons.check_circle_outline, size: 16, color: AppTheme.accentGreen),
                          label: const Text('Resolver no 1º Contato (N1)'),
                        ),
                      OutlinedButton.icon(
                        onPressed: () => TicketDetailModal.show(context, _createdTicket!),
                        icon: const Icon(Icons.open_in_new, size: 16),
                        label: const Text('Abrir Linha do Tempo Completa'),
                      ),
                    ],
                  ),
                ],
              ),
            ),
            const SizedBox(height: 16),
          ],

          // Formulário de Abertura de Novo Chamado Anatel
          Container(
            padding: const EdgeInsets.all(16),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Abrir Novo Protocolo de Atendimento (Padrão Anatel)',
                  style: TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 12),
                Wrap(
                  spacing: 12,
                  runSpacing: 12,
                  children: [
                    SizedBox(
                      width: 290,
                      child: DropdownButtonFormField<String>(
                        initialValue: _category,
                        isExpanded: true,
                        decoration: const InputDecoration(
                          labelText: 'Categoria Anatel & SLA',
                          isDense: true,
                        ),
                        items: const [
                          DropdownMenuItem(value: 'CONNECTION_OUTAGE', child: Text('Queda de Conexão (SLA 24h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'SLOW_SPEED', child: Text('Lentidão / Oscilação (SLA 48h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'ROUTER_CONFIG', child: Text('Roteador / Wi-Fi (SLA 48h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'FINANCIAL', child: Text('Financeiro / 2ª Via (SLA 24h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'ADDRESS_CHANGE', child: Text('Mudança de Endereço (SLA 72h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'ROOM_TRANSFER', child: Text('Mudança de Ponto (SLA 72h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'CANCELLATION_REQUEST', child: Text('Cancelamento (SLA 24h)', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'OTHER', child: Text('Outros Atendimentos (SLA 48h)', overflow: TextOverflow.ellipsis)),
                        ],
                        onChanged: (v) {
                          if (v != null) setState(() => _category = v);
                        },
                      ),
                    ),
                    SizedBox(
                      width: 180,
                      child: DropdownButtonFormField<String>(
                        initialValue: _priority,
                        isExpanded: true,
                        decoration: const InputDecoration(
                          labelText: 'Prioridade',
                          isDense: true,
                        ),
                        items: const [
                          DropdownMenuItem(value: 'LOW', child: Text('Baixa', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'NORMAL', child: Text('Normal', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'HIGH', child: Text('Alta', overflow: TextOverflow.ellipsis)),
                          DropdownMenuItem(value: 'URGENT', child: Text('Urgente', overflow: TextOverflow.ellipsis)),
                        ],
                        onChanged: (v) {
                          if (v != null) setState(() => _priority = v);
                        },
                      ),
                    ),
                    SizedBox(
                      width: 220,
                      child: TextField(
                        controller: _attendantCtrl,
                        decoration: const InputDecoration(
                          labelText: 'Atendente N1',
                          isDense: true,
                        ),
                      ),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: _subjectCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Assunto Resumido',
                    hintText: 'Ex: Sem conexão — LED LOS piscando vermelho na ONU',
                    isDense: true,
                  ),
                ),
                const SizedBox(height: 10),
                TextField(
                  controller: _descriptionCtrl,
                  maxLines: 2,
                  decoration: const InputDecoration(
                    labelText: 'Relato do Assinante / Triagem N1',
                    hintText: 'Descreva o relato do cliente e os testes iniciais realizados no N1...',
                    isDense: true,
                  ),
                ),
                const SizedBox(height: 12),
                Align(
                  alignment: Alignment.centerRight,
                  child: ElevatedButton.icon(
                    onPressed: _isCreating
                        ? null
                        : () => _submitTicket(
                              customer.id,
                              state.selectedContract?.id,
                              state.channel,
                            ),
                    icon: const Icon(Icons.confirmation_number_outlined, size: 18),
                    label: Text(_isCreating ? 'Gerando Protocolo...' : 'Gerar Protocolo Anatel'),
                  ),
                ),
              ],
            ),
          ),
          const SizedBox(height: 16),

          // Histórico de Protocolos Anteriores do Assinante
          const Text(
            'Histórico de Protocolos Anatel deste Assinante',
            style: TextStyle(fontSize: 13, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
          ),
          const SizedBox(height: 8),
          historyAsync.when(
            loading: () => const Padding(
              padding: EdgeInsets.all(16),
              child: Center(child: CircularProgressIndicator(strokeWidth: 2)),
            ),
            error: (err, _) => Text(
              'Erro ao carregar histórico de protocolos: $err',
              style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
            ),
            data: (tickets) {
              if (tickets.isEmpty) {
                return Container(
                  padding: const EdgeInsets.all(14),
                  decoration: BoxDecoration(
                    color: AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.darkBorder),
                  ),
                  child: const Text(
                    'Nenhum protocolo Anatel anterior registrado para este assinante.',
                    style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                );
              }

              return Column(
                children: tickets.take(5).map((t) {
                  return Container(
                    margin: const EdgeInsets.only(bottom: 8),
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: AppTheme.darkSurface,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppTheme.darkBorder),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.receipt_long_outlined, size: 18, color: AppTheme.primaryBlue),
                        const SizedBox(width: 10),
                        Expanded(
                          child: Column(
                            crossAxisAlignment: CrossAxisAlignment.start,
                            children: [
                              Text(
                                'Protocolo ${t.protocol} — ${t.subject}',
                                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                                overflow: TextOverflow.ellipsis,
                              ),
                              Text(
                                '${t.categoryLabel} • ${t.statusLabel} • SLA: ${t.formattedSlaDeadline}',
                                style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                                overflow: TextOverflow.ellipsis,
                              ),
                            ],
                          ),
                        ),
                        TextButton.icon(
                          onPressed: () => TicketDetailModal.show(context, t),
                          icon: const Icon(Icons.visibility_outlined, size: 15),
                          label: const Text('Tratar', style: TextStyle(fontSize: 12)),
                        ),
                      ],
                    ),
                  );
                }).toList(),
              );
            },
          ),
        ],
      ),
    );
  }
}
