/// Canais de entrada do atendimento omnichannel.
enum AttendanceChannel {
  presential('Presencial (Balcão/Loja)'),
  phone('Telefone (Chamada/Voz)'),
  whatsapp('WhatsApp / Mensagem'),
  email('E-mail / Portal');

  final String label;
  const AttendanceChannel(this.label);
}

/// Intenções de atendimento selecionáveis no fluxo guiado.
enum AttendanceIntent {
  payment('Pagar Fatura / Financeiro'),
  support('Suporte Técnico / Conexão'),
  sales('Venda / Upgrade de Plano'),
  general('Dúvida / 2ª Via / Informação');

  final String label;
  const AttendanceIntent(this.label);
}

/// Resumo de contrato do assinante.
class ContractSummary {
  final String id;
  final String contractNumber;
  final String status;
  final double monthlyFee;
  final int dueDay;
  final String installationAddress;

  const ContractSummary({
    required this.id,
    required this.contractNumber,
    required this.status,
    required this.monthlyFee,
    required this.dueDay,
    required this.installationAddress,
  });

  factory ContractSummary.fromJson(Map<String, dynamic> json) {
    return ContractSummary(
      id: json['id']?.toString() ?? '',
      contractNumber: json['contractNumber']?.toString() ?? '',
      status: json['status']?.toString() ?? 'ACTIVE',
      monthlyFee: (json['monthlyFee'] as num?)?.toDouble() ?? 0.0,
      dueDay: (json['dueDay'] as num?)?.toInt() ?? 10,
      installationAddress: json['installationAddress']?.toString() ?? '',
    );
  }
}

/// Fatura pendente de pagamento do cliente.
class PendingInvoice {
  final String id;
  final String contractId;
  final double amount;
  final String dueDate;
  final String status;
  final String? pixCopiaECola;
  final String? barcode;

  const PendingInvoice({
    required this.id,
    required this.contractId,
    required this.amount,
    required this.dueDate,
    required this.status,
    this.pixCopiaECola,
    this.barcode,
  });

  factory PendingInvoice.fromJson(Map<String, dynamic> json) {
    return PendingInvoice(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      amount: (json['amount'] as num?)?.toDouble() ?? 0.0,
      dueDate: json['dueDate']?.toString() ?? '',
      status: json['status']?.toString() ?? 'PENDING',
      pixCopiaECola: json['pixCopiaECola']?.toString(),
      barcode: json['barcode']?.toString(),
    );
  }

  bool get isOverdue {
    try {
      final due = DateTime.parse(dueDate);
      final today = DateTime.now();
      return due.isBefore(DateTime(today.year, today.month, today.day));
    } catch (_) {
      return false;
    }
  }
}

/// Ordem de serviço ativa para a linha do tempo viva.
class LiveWorkOrder {
  final String id;
  final String type;
  final String status;
  final String? technicianName;
  final String? scheduledDate;
  final String? scheduledPeriod;
  final double? fiberSignalDbm;
  final String? onuSerial;

  const LiveWorkOrder({
    required this.id,
    required this.type,
    required this.status,
    this.technicianName,
    this.scheduledDate,
    this.scheduledPeriod,
    this.fiberSignalDbm,
    this.onuSerial,
  });

  factory LiveWorkOrder.fromJson(Map<String, dynamic> json) {
    return LiveWorkOrder(
      id: json['id']?.toString() ?? '',
      type: json['type']?.toString() ?? 'REPAIR',
      status: json['status']?.toString() ?? 'PENDING',
      technicianName: json['technicianName']?.toString(),
      scheduledDate: json['scheduledDate']?.toString(),
      scheduledPeriod: json['scheduledPeriod']?.toString(),
      fiberSignalDbm: (json['fiberSignalDbm'] as num?)?.toDouble(),
      onuSerial: json['onuSerial']?.toString(),
    );
  }

  String get friendlyStatus {
    switch (status.toUpperCase()) {
      case 'SCHEDULED':
        return 'Agendada na rota';
      case 'IN_PROGRESS':
        return 'Técnico em deslocamento / atendimento';
      case 'COMPLETED':
        return 'Concluída';
      case 'CANCELED':
        return 'Cancelada';
      default:
        return 'Pendente na fila';
    }
  }
}
