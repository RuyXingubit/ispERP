/// Modelos de domínio para a Esteira de Chamados N1 / N2 e Protocolo Anatel.
class HelpdeskTicket {
  final String id;
  final String protocol;
  final String customerId;
  final String? contractId;
  final String category;
  final String priority;
  final String status;
  final String channel;
  final String subject;
  final String description;
  final String? assignedToUserId;
  final String? workOrderId;
  final String slaDeadline;
  final String? resolutionNotes;
  final int? anatelSatisfactionRating;
  final String? resolvedAt;
  final String? closedAt;
  final String createdAt;
  final String? updatedAt;

  const HelpdeskTicket({
    required this.id,
    required this.protocol,
    required this.customerId,
    this.contractId,
    required this.category,
    required this.priority,
    required this.status,
    required this.channel,
    required this.subject,
    required this.description,
    this.assignedToUserId,
    this.workOrderId,
    required this.slaDeadline,
    this.resolutionNotes,
    this.anatelSatisfactionRating,
    this.resolvedAt,
    this.closedAt,
    required this.createdAt,
    this.updatedAt,
  });

  factory HelpdeskTicket.fromJson(Map<String, dynamic> json) {
    return HelpdeskTicket(
      id: json['id']?.toString() ?? '',
      protocol: json['protocol']?.toString() ?? '',
      customerId: json['customerId']?.toString() ?? '',
      contractId: json['contractId']?.toString(),
      category: json['category']?.toString() ?? 'OTHER',
      priority: json['priority']?.toString() ?? 'NORMAL',
      status: json['status']?.toString() ?? 'OPEN',
      channel: json['channel']?.toString() ?? 'PHONE',
      subject: json['subject']?.toString() ?? '',
      description: json['description']?.toString() ?? '',
      assignedToUserId: json['assignedToUserId']?.toString(),
      workOrderId: json['workOrderId']?.toString(),
      slaDeadline: json['slaDeadline']?.toString() ?? '',
      resolutionNotes: json['resolutionNotes']?.toString(),
      anatelSatisfactionRating: json['anatelSatisfactionRating'] is num
          ? (json['anatelSatisfactionRating'] as num).toInt()
          : int.tryParse(json['anatelSatisfactionRating']?.toString() ?? ''),
      resolvedAt: json['resolvedAt']?.toString(),
      closedAt: json['closedAt']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
      updatedAt: json['updatedAt']?.toString(),
    );
  }

  bool get isActive {
    final s = status.toUpperCase();
    return s != 'CLOSED' && s != 'CANCELLED';
  }

  bool get isN2Queue => status.toUpperCase() == 'IN_PROGRESS';

  bool get isWaitingFieldVisit =>
      status.toUpperCase() == 'WAITING_FIELD_VISIT' ||
      (workOrderId != null && workOrderId!.isNotEmpty);

  String get categoryLabel {
    switch (category.toUpperCase()) {
      case 'CONNECTION_OUTAGE':
        return 'Queda de Conexão (SLA 24h)';
      case 'SLOW_SPEED':
        return 'Lentidão / Oscilação (SLA 48h)';
      case 'ROUTER_CONFIG':
        return 'Config. Roteador / Wi-Fi (SLA 48h)';
      case 'FINANCIAL':
        return 'Financeiro / 2ª Via (SLA 24h)';
      case 'ADDRESS_CHANGE':
        return 'Mudança de Endereço (SLA 72h)';
      case 'ROOM_TRANSFER':
        return 'Mudança de Ponto/Cômodo (SLA 72h)';
      case 'CANCELLATION_REQUEST':
        return 'Solicitação de Cancelamento (SLA 24h)';
      case 'OTHER':
      default:
        return 'Outros Atendimentos (SLA 48h)';
    }
  }

  String get priorityLabel {
    switch (priority.toUpperCase()) {
      case 'URGENT':
        return 'Urgente';
      case 'HIGH':
        return 'Alta';
      case 'LOW':
        return 'Baixa';
      case 'NORMAL':
      default:
        return 'Normal';
    }
  }

  String get statusLabel {
    switch (status.toUpperCase()) {
      case 'OPEN':
        return 'Aberto (Fila N1)';
      case 'IN_PROGRESS':
        return 'Em Tratamento N2 / NOC';
      case 'WAITING_CUSTOMER':
        return 'Aguardando Assinante';
      case 'WAITING_FIELD_VISIT':
        return 'Aguardando Visita Técnica (O.S.)';
      case 'RESOLVED':
        return 'Resolvido Remotamente (N2)';
      case 'CLOSED':
        return 'Encerrado (Anatel)';
      case 'CANCELLED':
        return 'Cancelado';
      default:
        return status;
    }
  }

  String get channelLabel {
    switch (channel.toUpperCase()) {
      case 'IN_PERSON':
        return 'Presencial (Loja)';
      case 'PHONE':
        return 'Telefone (0800/Central)';
      case 'WHATSAPP_BOT':
        return 'WhatsApp';
      case 'PORTAL':
        return 'Portal do Assinante';
      case 'EMAIL':
        return 'E-mail';
      default:
        return channel;
    }
  }

  String get formattedSlaDeadline {
    final dt = DateTime.tryParse(slaDeadline);
    if (dt == null) return slaDeadline.isEmpty ? 'N/D' : slaDeadline;
    final local = dt.toLocal();
    final d = local.day.toString().padLeft(2, '0');
    final m = local.month.toString().padLeft(2, '0');
    final h = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$d/$m às $h:$min';
  }

  bool get isSlaExpired {
    if (!isActive) return false;
    final dt = DateTime.tryParse(slaDeadline);
    if (dt == null) return false;
    return DateTime.now().isAfter(dt);
  }
}

/// Interação ou Nota Técnica na Linha do Tempo de um Chamado Anatel.
class TicketInteraction {
  final String id;
  final String ticketId;
  final String? userId;
  final bool isInternalNote;
  final String senderType;
  final String senderName;
  final String message;
  final String? attachmentUrl;
  final String createdAt;

  const TicketInteraction({
    required this.id,
    required this.ticketId,
    this.userId,
    required this.isInternalNote,
    required this.senderType,
    required this.senderName,
    required this.message,
    this.attachmentUrl,
    required this.createdAt,
  });

  factory TicketInteraction.fromJson(Map<String, dynamic> json) {
    return TicketInteraction(
      id: json['id']?.toString() ?? '',
      ticketId: json['ticketId']?.toString() ?? '',
      userId: json['userId']?.toString(),
      isInternalNote: json['isInternalNote'] == true,
      senderType: json['senderType']?.toString() ?? 'ATTENDANT',
      senderName: json['senderName']?.toString() ?? 'Atendente',
      message: json['message']?.toString() ?? '',
      attachmentUrl: json['attachmentUrl']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  String get senderTypeLabel {
    switch (senderType.toUpperCase()) {
      case 'ATTENDANT':
        return 'Atendente N1';
      case 'SUPPORT_N2':
        return 'Especialista N2 / NOC';
      case 'SUPPORT_ANALYST':
        return 'Analista de Suporte';
      case 'CUSTOMER':
        return 'Assinante';
      case 'SYSTEM_BOT':
        return 'Sistema / Automação';
      default:
        return senderType;
    }
  }

  String get formattedCreatedAt {
    final dt = DateTime.tryParse(createdAt);
    if (dt == null) return createdAt;
    final local = dt.toLocal();
    final d = local.day.toString().padLeft(2, '0');
    final m = local.month.toString().padLeft(2, '0');
    final h = local.hour.toString().padLeft(2, '0');
    final min = local.minute.toString().padLeft(2, '0');
    return '$d/$m $h:$min';
  }
}
