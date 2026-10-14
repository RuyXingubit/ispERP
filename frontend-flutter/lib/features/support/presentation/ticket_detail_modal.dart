import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../../technician/data/technician_repository.dart';
import '../data/helpdesk_models.dart';
import '../data/helpdesk_repository.dart';

/// Modal de Tratamento Completo do Chamado Anatel (Esteira N1 -> N2 -> Campo -> Encerramento CSAT).
class TicketDetailModal extends ConsumerStatefulWidget {
  final HelpdeskTicket initialTicket;

  const TicketDetailModal({
    super.key,
    required this.initialTicket,
  });

  static Future<void> show(BuildContext context, HelpdeskTicket ticket) {
    return showDialog<void>(
      context: context,
      builder: (_) => TicketDetailModal(initialTicket: ticket),
    );
  }

  @override
  ConsumerState<TicketDetailModal> createState() => _TicketDetailModalState();
}

class _TicketDetailModalState extends ConsumerState<TicketDetailModal> {
  late HelpdeskTicket _ticket;
  final _messageCtrl = TextEditingController();
  final _senderNameCtrl = TextEditingController(text: 'Operador ISP');
  String _senderType = 'ATTENDANT';
  bool _isInternalNote = false;
  bool _isSubmitting = false;

  @override
  void initState() {
    super.initState();
    _ticket = widget.initialTicket;
    if (_ticket.status.toUpperCase() == 'IN_PROGRESS') {
      _senderType = 'SUPPORT_N2';
    }
  }

  @override
  void dispose() {
    _messageCtrl.dispose();
    _senderNameCtrl.dispose();
    super.dispose();
  }

  Future<void> _refreshTicket() async {
    try {
      final repo = ref.read(helpdeskRepositoryProvider);
      final updated = await repo.getTicketById(_ticket.id);
      if (updated != null && mounted) {
        setState(() => _ticket = updated);
      }
      ref.invalidate(ticketInteractionsProvider(_ticket.id));
      ref.invalidate(helpdeskTicketsProvider);
      ref.invalidate(customerTicketsProvider(_ticket.customerId));
    } catch (_) {}
  }

  Future<void> _addInteraction() async {
    final msg = _messageCtrl.text.trim();
    final sender = _senderNameCtrl.text.trim();
    if (msg.isEmpty || sender.isEmpty) return;

    setState(() => _isSubmitting = true);
    try {
      final repo = ref.read(helpdeskRepositoryProvider);
      await repo.addInteraction(
        ticketId: _ticket.id,
        senderType: _senderType,
        senderName: sender,
        message: msg,
        isInternalNote: _isInternalNote,
      );
      _messageCtrl.clear();
      await _refreshTicket();
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('Erro ao registrar interação: $e'),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _isSubmitting = false);
    }
  }

  Future<void> _promptEscalateN2() async {
    final attendantCtrl = TextEditingController(text: _senderNameCtrl.text.trim());
    final reasonCtrl = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Escalonar Chamado para N2 / NOC'),
        content: SizedBox(
          width: 440,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: attendantCtrl,
                decoration: const InputDecoration(
                  labelText: 'Atendente N1 Responsável',
                  prefixIcon: Icon(Icons.person_outline, size: 18),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: reasonCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Diagnóstico Inicial N1 / Motivo do Escalonamento',
                  hintText: 'Ex: ONU com LOS vermelho, reinicialização física sem sucesso...',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton.icon(
            onPressed: () => Navigator.of(ctx).pop(true),
            icon: const Icon(Icons.trending_up, size: 16),
            label: const Text('Confirmar Escalonamento N2'),
          ),
        ],
      ),
    );

    if (confirmed == true && reasonCtrl.text.trim().isNotEmpty) {
      setState(() => _isSubmitting = true);
      try {
        final repo = ref.read(helpdeskRepositoryProvider);
        final updated = await repo.escalateToN2(
          ticketId: _ticket.id,
          attendantName: attendantCtrl.text.trim().isEmpty
              ? 'Atendente N1'
              : attendantCtrl.text.trim(),
          reason: reasonCtrl.text.trim(),
        );
        if (mounted) {
          setState(() {
            _ticket = updated;
            _senderType = 'SUPPORT_N2';
          });
        }
        await _refreshTicket();
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
        if (mounted) setState(() => _isSubmitting = false);
      }
    }
  }

  Future<void> _promptResolveN2() async {
    final analystCtrl = TextEditingController(text: 'Especialista NOC N2');
    final notesCtrl = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Resolver Remotamente (N2 / NOC)'),
        content: SizedBox(
          width: 440,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: analystCtrl,
                decoration: const InputDecoration(
                  labelText: 'Especialista N2 / NOC',
                  prefixIcon: Icon(Icons.engineering_outlined, size: 18),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: notesCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Parecer Técnico de Resolução Remota',
                  hintText: 'Ex: Reprovisionado profile VLAN na OLT e sessão PPPoE restabelecida.',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentGreen,
              foregroundColor: Colors.white,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            icon: const Icon(Icons.check_circle_outline, size: 16),
            label: const Text('Marcar como Resolvido'),
          ),
        ],
      ),
    );

    if (confirmed == true && notesCtrl.text.trim().isNotEmpty) {
      setState(() => _isSubmitting = true);
      try {
        final repo = ref.read(helpdeskRepositoryProvider);
        final updated = await repo.resolveByN2(
          ticketId: _ticket.id,
          n2Name: analystCtrl.text.trim().isEmpty
              ? 'Especialista N2'
              : analystCtrl.text.trim(),
          resolutionNotes: notesCtrl.text.trim(),
        );
        if (mounted) setState(() => _ticket = updated);
        await _refreshTicket();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Falha ao resolver chamado no N2: $e'),
              backgroundColor: AppTheme.accentError,
            ),
          );
        }
      } finally {
        if (mounted) setState(() => _isSubmitting = false);
      }
    }
  }

  Future<void> _promptEscalateWorkOrder() async {
    final analystCtrl = TextEditingController(text: 'Especialista NOC N2');
    final reasonCtrl = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Despachar Equipe de Campo (Gerar O.S.)'),
        content: SizedBox(
          width: 440,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            children: [
              TextField(
                controller: analystCtrl,
                decoration: const InputDecoration(
                  labelText: 'Analista N2 Despachante',
                  prefixIcon: Icon(Icons.engineering, size: 18),
                ),
              ),
              const SizedBox(height: 12),
              TextField(
                controller: reasonCtrl,
                maxLines: 3,
                decoration: const InputDecoration(
                  labelText: 'Laudo Técnico p/ Equipe de Campo (O.S. Manutenção)',
                  hintText: 'Ex: Atenuação crítica -31 dBm na CTO-14; verificar drop óptico e conector APC.',
                ),
              ),
            ],
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(false),
            child: const Text('Cancelar'),
          ),
          ElevatedButton.icon(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentWarning,
              foregroundColor: Colors.black,
            ),
            onPressed: () => Navigator.of(ctx).pop(true),
            icon: const Icon(Icons.handyman_outlined, size: 16),
            label: const Text('Gerar O.S. de Manutenção'),
          ),
        ],
      ),
    );

    if (confirmed == true && reasonCtrl.text.trim().isNotEmpty) {
      setState(() => _isSubmitting = true);
      try {
        final repo = ref.read(helpdeskRepositoryProvider);
        await repo.escalateToWorkOrder(
          ticketId: _ticket.id,
          n2Name: analystCtrl.text.trim().isEmpty
              ? 'Especialista N2'
              : analystCtrl.text.trim(),
          technicalReason: reasonCtrl.text.trim(),
        );
        ref.invalidate(technicianWorkOrdersProvider);
        await _refreshTicket();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Falha ao gerar Ordem de Serviço: $e'),
              backgroundColor: AppTheme.accentError,
            ),
          );
        }
      } finally {
        if (mounted) setState(() => _isSubmitting = false);
      }
    }
  }

  Future<void> _promptCloseTicket() async {
    int selectedRating = 5;
    final notesCtrl = TextEditingController();

    final confirmed = await showDialog<bool>(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (ctx, setDialogState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Encerrar Protocolo Anatel & CSAT'),
          content: SizedBox(
            width: 440,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Nota de Satisfação do Assinante (CSAT Anatel 1 a 5):',
                  style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 8),
                Wrap(
                  spacing: 8,
                  children: List.generate(5, (index) {
                    final rating = index + 1;
                    final isSelected = rating <= selectedRating;
                    return IconButton(
                      onPressed: () => setDialogState(() => selectedRating = rating),
                      icon: Icon(
                        isSelected ? Icons.star_rounded : Icons.star_border_rounded,
                        color: isSelected ? AppTheme.accentWarning : AppTheme.textSecondary,
                        size: 28,
                      ),
                    );
                  }),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: notesCtrl,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    labelText: 'Termo de Encerramento / Confirmação com Assinante',
                    hintText: 'Ex: Cliente confirmou normalização da conexão e recebeu o protocolo final.',
                  ),
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(false),
              child: const Text('Cancelar'),
            ),
            ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.accentGreen,
                foregroundColor: Colors.white,
              ),
              onPressed: () => Navigator.of(ctx).pop(true),
              icon: const Icon(Icons.task_alt, size: 16),
              label: const Text('Encerrar Protocolo'),
            ),
          ],
        ),
      ),
    );

    if (confirmed == true) {
      setState(() => _isSubmitting = true);
      try {
        final repo = ref.read(helpdeskRepositoryProvider);
        final updated = await repo.closeTicket(
          ticketId: _ticket.id,
          satisfactionRating: selectedRating,
          closureNotes: notesCtrl.text.trim().isEmpty
              ? 'Encerrado após validação com o assinante.'
              : notesCtrl.text.trim(),
        );
        if (mounted) setState(() => _ticket = updated);
        await _refreshTicket();
      } catch (e) {
        if (mounted) {
          ScaffoldMessenger.of(context).showSnackBar(
            SnackBar(
              content: Text('Falha ao encerrar protocolo: $e'),
              backgroundColor: AppTheme.accentError,
            ),
          );
        }
      } finally {
        if (mounted) setState(() => _isSubmitting = false);
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    final interactionsAsync = ref.watch(ticketInteractionsProvider(_ticket.id));

    return Dialog(
      backgroundColor: AppTheme.darkBg,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(16),
        side: const BorderSide(color: AppTheme.darkBorder),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 880, maxHeight: 760),
        child: Padding(
          padding: const EdgeInsets.all(16),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // 1. Cabeçalho do Protocolo Anatel
              Wrap(
                alignment: WrapAlignment.spaceBetween,
                crossAxisAlignment: WrapCrossAlignment.center,
                spacing: 10,
                runSpacing: 6,
                children: [
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                        decoration: BoxDecoration(
                          color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                          borderRadius: BorderRadius.circular(8),
                          border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.4)),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: [
                            const Icon(Icons.verified_user_outlined, size: 15, color: AppTheme.primaryBlue),
                            const SizedBox(width: 6),
                            SelectableText(
                              'Protocolo ANATEL: ${_ticket.protocol}',
                              style: const TextStyle(
                                fontWeight: FontWeight.bold,
                                color: AppTheme.primaryBlue,
                                fontSize: 13,
                              ),
                            ),
                            const SizedBox(width: 6),
                            InkWell(
                              onTap: () {
                                Clipboard.setData(ClipboardData(text: _ticket.protocol));
                                ScaffoldMessenger.of(context).showSnackBar(
                                  SnackBar(content: Text('Protocolo ${_ticket.protocol} copiado!')),
                                );
                              },
                              child: const Icon(Icons.copy, size: 14, color: AppTheme.primaryBlue),
                            ),
                          ],
                        ),
                      ),
                      const SizedBox(width: 8),
                      _buildStatusBadge(_ticket.status, _ticket.statusLabel),
                    ],
                  ),
                  Row(
                    mainAxisSize: MainAxisSize.min,
                    children: [
                      Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                        decoration: BoxDecoration(
                          color: _ticket.isSlaExpired
                              ? AppTheme.accentError.withValues(alpha: 0.15)
                              : AppTheme.accentGreen.withValues(alpha: 0.12),
                          borderRadius: BorderRadius.circular(8),
                        ),
                        child: Text(
                          'SLA: ${_ticket.formattedSlaDeadline}',
                          style: TextStyle(
                            fontSize: 11,
                            fontWeight: FontWeight.bold,
                            color: _ticket.isSlaExpired ? AppTheme.accentError : AppTheme.accentGreen,
                          ),
                        ),
                      ),
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        onPressed: _refreshTicket,
                        icon: const Icon(Icons.refresh, size: 18, color: AppTheme.primaryBlue),
                        tooltip: 'Atualizar Chamado',
                      ),
                      IconButton(
                        visualDensity: VisualDensity.compact,
                        onPressed: () => Navigator.of(context).pop(),
                        icon: const Icon(Icons.close, size: 18),
                        tooltip: 'Fechar',
                      ),
                    ],
                  ),
                ],
              ),
              const SizedBox(height: 8),

              // 2. Resumo Técnico e Metadados do Chamado
              Container(
                padding: const EdgeInsets.all(10),
                decoration: BoxDecoration(
                  color: AppTheme.darkSurface,
                  borderRadius: BorderRadius.circular(10),
                  border: Border.all(color: AppTheme.darkBorder),
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      _ticket.subject,
                      style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 2),
                    Text(
                      _ticket.description,
                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                    ),
                    const SizedBox(height: 6),
                    Wrap(
                      spacing: 8,
                      runSpacing: 4,
                      children: [
                        _buildMetaChip(Icons.category_outlined, _ticket.categoryLabel),
                        _buildMetaChip(Icons.flag_outlined, 'Prioridade: ${_ticket.priorityLabel}'),
                        _buildMetaChip(Icons.headset_mic_outlined, 'Canal: ${_ticket.channelLabel}'),
                        if (_ticket.workOrderId != null && _ticket.workOrderId!.isNotEmpty)
                          _buildMetaChip(
                            Icons.handyman_outlined,
                            'O.S. Campo #${_ticket.workOrderId!.substring(0, _ticket.workOrderId!.length > 8 ? 8 : _ticket.workOrderId!.length)}',
                          ),
                        if (_ticket.anatelSatisfactionRating != null)
                          _buildMetaChip(
                            Icons.star_rounded,
                            'CSAT Anatel: ${_ticket.anatelSatisfactionRating}/5',
                          ),
                      ],
                    ),
                    if (_ticket.resolutionNotes != null && _ticket.resolutionNotes!.isNotEmpty) ...[
                      const SizedBox(height: 6),
                      Container(
                        padding: const EdgeInsets.all(6),
                        decoration: BoxDecoration(
                          color: AppTheme.accentGreen.withValues(alpha: 0.1),
                          borderRadius: BorderRadius.circular(6),
                          border: Border.all(color: AppTheme.accentGreen.withValues(alpha: 0.3)),
                        ),
                        child: Row(
                          children: [
                            const Icon(Icons.check_circle_outline, size: 14, color: AppTheme.accentGreen),
                            const SizedBox(width: 6),
                            Expanded(
                              child: Text(
                                'Parecer de Resolução: ${_ticket.resolutionNotes}',
                                style: const TextStyle(fontSize: 11, color: AppTheme.accentGreen),
                              ),
                            ),
                          ],
                        ),
                      ),
                    ],
                  ],
                ),
              ),
              const SizedBox(height: 8),

              // 3. Barra de Ações da Esteira N1 -> N2 -> Campo -> Encerramento
              if (_ticket.isActive) ...[
                Wrap(
                  spacing: 8,
                  runSpacing: 6,
                  children: [
                    if (_ticket.status.toUpperCase() == 'OPEN')
                      ElevatedButton.icon(
                        onPressed: _isSubmitting ? null : _promptEscalateN2,
                        icon: const Icon(Icons.trending_up, size: 15),
                        label: const Text('Escalonar p/ N2 (NOC)'),
                      ),
                    if (_ticket.status.toUpperCase() == 'OPEN' ||
                        _ticket.status.toUpperCase() == 'IN_PROGRESS')
                      ElevatedButton.icon(
                        style: ElevatedButton.styleFrom(
                          backgroundColor: AppTheme.accentGreen,
                          foregroundColor: Colors.white,
                        ),
                        onPressed: _isSubmitting ? null : _promptResolveN2,
                        icon: const Icon(Icons.build_circle_outlined, size: 15),
                        label: const Text('Resolver no N2 (Remoto)'),
                      ),
                    if (_ticket.status.toUpperCase() == 'OPEN' ||
                        _ticket.status.toUpperCase() == 'IN_PROGRESS')
                      OutlinedButton.icon(
                        onPressed: _isSubmitting ? null : _promptEscalateWorkOrder,
                        icon: const Icon(Icons.handyman_outlined, size: 15, color: AppTheme.accentWarning),
                        label: const Text('Gerar O.S. de Campo'),
                      ),
                    OutlinedButton.icon(
                      onPressed: _isSubmitting ? null : _promptCloseTicket,
                      icon: const Icon(Icons.task_alt, size: 15, color: AppTheme.accentGreen),
                      label: const Text('Encerrar Protocolo (CSAT)'),
                    ),
                  ],
                ),
                const SizedBox(height: 8),
              ],

              // 4. Linha do Tempo Auditável (Mensagens Públicas + Notas Técnicas Internas)
              const Text(
                'Linha do Tempo do Protocolo (Histórico Auditável Anatel & Notas Internas N1/N2)',
                style: TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 6),
              Expanded(
                child: Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppTheme.darkBorder),
                  ),
                  child: interactionsAsync.when(
                    loading: () => const Center(child: CircularProgressIndicator(strokeWidth: 2)),
                    error: (err, _) => Center(
                      child: Text(
                        'Erro ao carregar linha do tempo: $err',
                        style: const TextStyle(color: AppTheme.accentError, fontSize: 12),
                      ),
                    ),
                    data: (interactions) {
                      if (interactions.isEmpty) {
                        return const Center(
                          child: Text(
                            'Nenhuma interação registrada ainda neste protocolo.',
                            style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
                          ),
                        );
                      }
                      return ListView.separated(
                        itemCount: interactions.length,
                        separatorBuilder: (_, _) => const SizedBox(height: 6),
                        itemBuilder: (context, index) {
                          final item = interactions[index];
                          final isInternal = item.isInternalNote;
                          return Container(
                            padding: const EdgeInsets.all(8),
                            decoration: BoxDecoration(
                              color: isInternal
                                  ? AppTheme.accentWarning.withValues(alpha: 0.08)
                                  : AppTheme.darkBg,
                              borderRadius: BorderRadius.circular(8),
                              border: Border.all(
                                color: isInternal
                                    ? AppTheme.accentWarning.withValues(alpha: 0.4)
                                    : AppTheme.darkBorder,
                              ),
                            ),
                            child: Column(
                              crossAxisAlignment: CrossAxisAlignment.start,
                              children: [
                                Wrap(
                                  spacing: 8,
                                  runSpacing: 4,
                                  crossAxisAlignment: WrapCrossAlignment.center,
                                  children: [
                                    Icon(
                                      isInternal ? Icons.lock_outline : Icons.forum_outlined,
                                      size: 14,
                                      color: isInternal ? AppTheme.accentWarning : AppTheme.primaryBlue,
                                    ),
                                    Text(
                                      '${item.senderName} (${item.senderTypeLabel})',
                                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                                    ),
                                    if (isInternal)
                                      Container(
                                        padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                        decoration: BoxDecoration(
                                          color: AppTheme.accentWarning.withValues(alpha: 0.2),
                                          borderRadius: BorderRadius.circular(4),
                                        ),
                                        child: const Text(
                                          'NOTA TÉCNICA INTERNA',
                                          style: TextStyle(
                                            fontSize: 9,
                                            fontWeight: FontWeight.bold,
                                            color: AppTheme.accentWarning,
                                          ),
                                        ),
                                      ),
                                    Text(
                                      item.formattedCreatedAt,
                                      style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary),
                                    ),
                                  ],
                                ),
                                const SizedBox(height: 3),
                                Text(item.message, style: const TextStyle(fontSize: 12)),
                              ],
                            ),
                          );
                        },
                      );
                    },
                  ),
                ),
              ),
              const SizedBox(height: 8),

              // 5. Formulário de Nova Interação / Nota Interna
              if (_ticket.isActive)
                Container(
                  padding: const EdgeInsets.all(10),
                  decoration: BoxDecoration(
                    color: AppTheme.darkSurface,
                    borderRadius: BorderRadius.circular(10),
                    border: Border.all(color: AppTheme.darkBorder),
                  ),
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.stretch,
                    children: [
                      Wrap(
                        spacing: 12,
                        runSpacing: 6,
                        crossAxisAlignment: WrapCrossAlignment.center,
                        children: [
                          SizedBox(
                            width: 220,
                            child: DropdownButtonFormField<String>(
                              initialValue: _senderType,
                              isExpanded: true,
                              decoration: const InputDecoration(
                                labelText: 'Papel na Esteira',
                                isDense: true,
                              ),
                              items: const [
                                DropdownMenuItem(value: 'ATTENDANT', child: Text('Atendente N1', overflow: TextOverflow.ellipsis)),
                                DropdownMenuItem(value: 'SUPPORT_N2', child: Text('Especialista N2/NOC', overflow: TextOverflow.ellipsis)),
                                DropdownMenuItem(value: 'CUSTOMER', child: Text('Assinante', overflow: TextOverflow.ellipsis)),
                              ],
                              onChanged: (v) {
                                if (v != null) setState(() => _senderType = v);
                              },
                            ),
                          ),
                          SizedBox(
                            width: 190,
                            child: TextField(
                              controller: _senderNameCtrl,
                              decoration: const InputDecoration(
                                labelText: 'Nome do Operador',
                                isDense: true,
                              ),
                            ),
                          ),
                          Row(
                            mainAxisSize: MainAxisSize.min,
                            children: [
                              Checkbox(
                                visualDensity: VisualDensity.compact,
                                value: _isInternalNote,
                                onChanged: (v) => setState(() => _isInternalNote = v ?? false),
                              ),
                              const Text(
                                'Nota Técnica Interna (Invisível ao Assinante)',
                                style: TextStyle(fontSize: 12),
                              ),
                            ],
                          ),
                        ],
                      ),
                      const SizedBox(height: 6),
                      Row(
                        children: [
                          Expanded(
                            child: TextField(
                              controller: _messageCtrl,
                              onSubmitted: (_) => _addInteraction(),
                              decoration: InputDecoration(
                                hintText: _isInternalNote
                                    ? 'Registrar diagnóstico interno (ex: atenuação PON -26 dBm, log RADIUS)...'
                                    : 'Registrar mensagem pública para o histórico do assinante...',
                                isDense: true,
                              ),
                            ),
                          ),
                          const SizedBox(width: 10),
                          ElevatedButton.icon(
                            onPressed: _isSubmitting ? null : _addInteraction,
                            icon: const Icon(Icons.send_rounded, size: 15),
                            label: const Text('Registrar'),
                          ),
                        ],
                      ),
                    ],
                  ),
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildStatusBadge(String status, String label) {
    Color color = AppTheme.primaryBlue;
    switch (status.toUpperCase()) {
      case 'OPEN':
        color = AppTheme.accentWarning;
        break;
      case 'IN_PROGRESS':
        color = AppTheme.primaryBlue;
        break;
      case 'WAITING_FIELD_VISIT':
        color = Colors.orangeAccent;
        break;
      case 'RESOLVED':
      case 'CLOSED':
        color = AppTheme.accentGreen;
        break;
      case 'CANCELLED':
        color = AppTheme.accentError;
        break;
    }

    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.15),
        borderRadius: BorderRadius.circular(12),
      ),
      child: Text(
        label,
        style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold, color: color),
      ),
    );
  }

  Widget _buildMetaChip(IconData icon, String text) {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
      decoration: BoxDecoration(
        color: AppTheme.darkBg,
        borderRadius: BorderRadius.circular(6),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(icon, size: 13, color: AppTheme.textSecondary),
          const SizedBox(width: 5),
          Text(text, style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary)),
        ],
      ),
    );
  }
}
