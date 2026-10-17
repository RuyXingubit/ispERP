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
  final String? planId;
  final double monthlyFee;
  final double pendingProRataAdjustment;
  final int dueDay;
  final String installationAddress;
  final String city;
  final String state;
  final String zipCode;
  final String? ctoId;
  final int? ctoPortNumber;

  const ContractSummary({
    required this.id,
    required this.contractNumber,
    required this.status,
    this.planId,
    required this.monthlyFee,
    this.pendingProRataAdjustment = 0.0,
    required this.dueDay,
    required this.installationAddress,
    this.city = '',
    this.state = '',
    this.zipCode = '',
    this.ctoId,
    this.ctoPortNumber,
  });

  factory ContractSummary.fromJson(Map<String, dynamic> json) {
    return ContractSummary(
      id: json['id']?.toString() ?? '',
      contractNumber: json['contractNumber']?.toString() ?? '',
      status: json['status']?.toString() ?? 'ACTIVE',
      planId: json['planId']?.toString(),
      monthlyFee: (json['monthlyFee'] as num?)?.toDouble() ?? 0.0,
      pendingProRataAdjustment: (json['pendingProRataAdjustment'] as num?)?.toDouble() ?? 0.0,
      dueDay: (json['dueDay'] as num?)?.toInt() ?? 10,
      installationAddress: json['installationAddress']?.toString() ?? '',
      city: json['city']?.toString() ?? '',
      state: json['state']?.toString() ?? '',
      zipCode: json['zipCode']?.toString() ?? '',
      ctoId: json['ctoId']?.toString(),
      ctoPortNumber: (json['ctoPortNumber'] as num?)?.toInt(),
    );
  }

  /// Calcula a prévia Pro-Rata Die (base comercial 30 dias) entre a mensalidade atual e a nova.
  static ({int remainingDays, double proRataDelta}) calculateProRataPreview({
    required double currentMonthlyFee,
    required double newMonthlyFee,
    required int dueDay,
    DateTime? referenceDate,
  }) {
    final today = referenceDate ?? DateTime.now();
    final safeDueDay = dueDay.clamp(1, 28);
    final baseToday = DateTime(today.year, today.month, today.day);
    var nextDue = DateTime(today.year, today.month, safeDueDay);
    if (!nextDue.isAfter(baseToday)) {
      nextDue = DateTime(today.year, today.month + 1, safeDueDay);
    }
    final remainingDays = nextDue.difference(baseToday).inDays.clamp(1, 30);
    final rawDelta = ((newMonthlyFee - currentMonthlyFee) / 30.0) * remainingDays;
    final roundedDelta = (rawDelta * 100).roundToDouble() / 100.0;
    return (remainingDays: remainingDays, proRataDelta: roundedDelta);
  }
}

/// Evento auditado do ciclo de vida pós-venda de um contrato (Onda I).
class ContractLifecycleEventModel {
  final String id;
  final String contractId;
  final String eventType;
  final String? previousPlanId;
  final String? newPlanId;
  final double? previousMonthlyFee;
  final double? newMonthlyFee;
  final int? proRataDays;
  final double? proRataAdjustmentAmount;
  final String? adjustedInvoiceId;
  final String? previousAddress;
  final String? newAddress;
  final String? generatedWorkOrderId;
  final String? previousCustomerId;
  final String? newCustomerId;
  final int? migratedInvoicesCount;
  final String? signatureSessionId;
  final bool radiusCoaSynced;
  final String? notes;
  final String createdAt;

  const ContractLifecycleEventModel({
    required this.id,
    required this.contractId,
    required this.eventType,
    this.previousPlanId,
    this.newPlanId,
    this.previousMonthlyFee,
    this.newMonthlyFee,
    this.proRataDays,
    this.proRataAdjustmentAmount,
    this.adjustedInvoiceId,
    this.previousAddress,
    this.newAddress,
    this.generatedWorkOrderId,
    this.previousCustomerId,
    this.newCustomerId,
    this.migratedInvoicesCount,
    this.signatureSessionId,
    this.radiusCoaSynced = false,
    this.notes,
    required this.createdAt,
  });

  factory ContractLifecycleEventModel.fromJson(Map<String, dynamic> json) {
    return ContractLifecycleEventModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      eventType: json['eventType']?.toString() ?? 'PLAN_CHANGE',
      previousPlanId: json['previousPlanId']?.toString(),
      newPlanId: json['newPlanId']?.toString(),
      previousMonthlyFee: (json['previousMonthlyFee'] as num?)?.toDouble(),
      newMonthlyFee: (json['newMonthlyFee'] as num?)?.toDouble(),
      proRataDays: (json['proRataDays'] as num?)?.toInt(),
      proRataAdjustmentAmount: (json['proRataAdjustmentAmount'] as num?)?.toDouble(),
      adjustedInvoiceId: json['adjustedInvoiceId']?.toString(),
      previousAddress: json['previousAddress']?.toString(),
      newAddress: json['newAddress']?.toString(),
      generatedWorkOrderId: json['generatedWorkOrderId']?.toString(),
      previousCustomerId: json['previousCustomerId']?.toString(),
      newCustomerId: json['newCustomerId']?.toString(),
      migratedInvoicesCount: (json['migratedInvoicesCount'] as num?)?.toInt(),
      signatureSessionId: json['signatureSessionId']?.toString(),
      radiusCoaSynced: json['radiusCoaSynced'] == true,
      notes: json['notes']?.toString(),
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }

  String get eventTypeLabel {
    switch (eventType.toUpperCase()) {
      case 'PLAN_CHANGE':
        return 'Upgrade / Downgrade de Plano (Pro-Rata + CoA)';
      case 'ADDRESS_CHANGE':
        return 'Mudança de Endereço + O.S. Instalação';
      case 'OWNERSHIP_TRANSFER':
        return 'Troca de Titularidade + Migração de Faturas';
      default:
        return eventType;
    }
  }
}

/// Opção de plano comercial ativa para Upgrade / Downgrade (Onda I).
class PlanOptionModel {
  final String id;
  final String name;
  final int downloadSpeedMbps;
  final int uploadSpeedMbps;
  final double monthlyPrice;

  const PlanOptionModel({
    required this.id,
    required this.name,
    required this.downloadSpeedMbps,
    required this.uploadSpeedMbps,
    required this.monthlyPrice,
  });

  factory PlanOptionModel.fromJson(Map<String, dynamic> json) {
    return PlanOptionModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      downloadSpeedMbps: (json['downloadSpeedMbps'] as num?)?.toInt() ?? 0,
      uploadSpeedMbps: (json['uploadSpeedMbps'] as num?)?.toInt() ?? 0,
      monthlyPrice: (json['monthlyPrice'] as num?)?.toDouble() ?? 0.0,
    );
  }
}

/// Resposta consolidada de operação de ciclo de vida pós-venda (Onda I).
class ContractLifecycleActionResultModel {
  final ContractSummary contract;
  final ContractLifecycleEventModel lifecycleEvent;
  final String message;

  const ContractLifecycleActionResultModel({
    required this.contract,
    required this.lifecycleEvent,
    required this.message,
  });

  factory ContractLifecycleActionResultModel.fromJson(Map<String, dynamic> json) {
    return ContractLifecycleActionResultModel(
      contract: ContractSummary.fromJson(
        (json['contract'] as Map<String, dynamic>?) ?? const <String, dynamic>{},
      ),
      lifecycleEvent: ContractLifecycleEventModel.fromJson(
        (json['lifecycleEvent'] as Map<String, dynamic>?) ?? const <String, dynamic>{},
      ),
      message: json['message']?.toString() ?? 'Operação concluída com sucesso.',
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

/// Incidente massivo de rede óptica FTTH ativo correlacionado à CTO/PON do contrato (Onda J).
class ActiveNetworkIncidentModel {
  final String id;
  final String oltId;
  final String ponPort;
  final String? ctoId;
  final String incidentType;
  final String status;
  final int affectedCount;
  final String description;
  final String? detectedAt;

  const ActiveNetworkIncidentModel({
    required this.id,
    required this.oltId,
    required this.ponPort,
    this.ctoId,
    required this.incidentType,
    required this.status,
    required this.affectedCount,
    required this.description,
    this.detectedAt,
  });

  factory ActiveNetworkIncidentModel.fromJson(Map<String, dynamic> json) {
    return ActiveNetworkIncidentModel(
      id: json['id']?.toString() ?? '',
      oltId: json['oltId']?.toString() ?? '',
      ponPort: json['ponPort']?.toString() ?? '',
      ctoId: json['ctoId']?.toString(),
      incidentType: json['incidentType']?.toString() ?? 'MASS_OUTAGE',
      status: json['status']?.toString() ?? 'OPEN',
      affectedCount: (json['affectedCount'] as num?)?.toInt() ?? 0,
      description: json['description']?.toString() ??
          'Manutenção emergencial / rompimento óptico em andamento na sua região.',
      detectedAt: json['detectedAt']?.toString(),
    );
  }
}

/// Dispositivo conectado na LAN/WLAN da ONU/CPE reportado via TR-069 (Onda M).
class Tr069ConnectedHostModel {
  final String hostName;
  final String ipAddress;
  final String macAddress;
  final String band;
  final int? rssiDbm;

  const Tr069ConnectedHostModel({
    required this.hostName,
    required this.ipAddress,
    required this.macAddress,
    required this.band,
    this.rssiDbm,
  });

  factory Tr069ConnectedHostModel.fromJson(Map<String, dynamic> json) {
    return Tr069ConnectedHostModel(
      hostName: json['hostName']?.toString() ?? 'Host LAN/Wi-Fi',
      ipAddress: json['ipAddress']?.toString() ?? '-',
      macAddress: json['macAddress']?.toString() ?? '-',
      band: json['band']?.toString() ?? '2.4GHz',
      rssiDbm: (json['rssiDbm'] as num?)?.toInt(),
    );
  }
}

/// Registro de auditoria de comando TR-069 / CWMP despachado para a CPE (Onda M).
class Tr069CommandLogModel {
  final String id;
  final String contractId;
  final String? deviceId;
  final String commandType;
  final String triggeredByRole;
  final String? triggeredByName;
  final String status;
  final String summary;
  final String createdAt;

  const Tr069CommandLogModel({
    required this.id,
    required this.contractId,
    this.deviceId,
    required this.commandType,
    required this.triggeredByRole,
    this.triggeredByName,
    required this.status,
    required this.summary,
    required this.createdAt,
  });

  factory Tr069CommandLogModel.fromJson(Map<String, dynamic> json) {
    return Tr069CommandLogModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      deviceId: json['deviceId']?.toString(),
      commandType: json['commandType']?.toString() ?? 'SET_WIFI_CONFIG',
      triggeredByRole: json['triggeredByRole']?.toString() ?? 'SUPPORT_AGENT',
      triggeredByName: json['triggeredByName']?.toString(),
      status: json['status']?.toString() ?? 'APPLIED',
      summary: json['summary']?.toString() ?? '',
      createdAt: json['createdAt']?.toString() ?? '',
    );
  }
}

/// Estado completo Dual-Band Wi-Fi, telemetria e logs de auditoria TR-069 / CWMP da CPE (Onda M).
class Tr069CpeStatusModel {
  final String id;
  final String contractId;
  final String deviceId;
  final String serialNumber;
  final String modelName;
  final String firmwareVersion;
  final String ssid24ghz;
  final String channel24ghz;
  final String ssid5ghz;
  final String channel5ghz;
  final bool bandSteeringEnabled;
  final String securityMode;
  final int uptimeSeconds;
  final String acsSyncStatus;
  final String? lastInformAt;
  final String? lastRebootAt;
  final List<Tr069ConnectedHostModel> connectedHosts;
  final List<Tr069CommandLogModel> recentCommands;

  const Tr069CpeStatusModel({
    required this.id,
    required this.contractId,
    required this.deviceId,
    required this.serialNumber,
    required this.modelName,
    required this.firmwareVersion,
    required this.ssid24ghz,
    required this.channel24ghz,
    required this.ssid5ghz,
    required this.channel5ghz,
    required this.bandSteeringEnabled,
    required this.securityMode,
    required this.uptimeSeconds,
    required this.acsSyncStatus,
    this.lastInformAt,
    this.lastRebootAt,
    this.connectedHosts = const [],
    this.recentCommands = const [],
  });

  factory Tr069CpeStatusModel.fromJson(Map<String, dynamic> json) {
    final rawHosts = json['connectedHosts'];
    final rawCommands = json['recentCommands'];
    return Tr069CpeStatusModel(
      id: json['id']?.toString() ?? '',
      contractId: json['contractId']?.toString() ?? '',
      deviceId: json['deviceId']?.toString() ?? '',
      serialNumber: json['serialNumber']?.toString() ?? '',
      modelName: json['modelName']?.toString() ?? 'ONU Wi-Fi Dual-Band',
      firmwareVersion: json['firmwareVersion']?.toString() ?? '-',
      ssid24ghz: json['ssid24ghz']?.toString() ?? '',
      channel24ghz: json['channel24ghz']?.toString() ?? 'AUTO',
      ssid5ghz: json['ssid5ghz']?.toString() ?? '',
      channel5ghz: json['channel5ghz']?.toString() ?? 'AUTO',
      bandSteeringEnabled: json['bandSteeringEnabled'] == true,
      securityMode: json['securityMode']?.toString() ?? 'WPA2_WPA3_PSK',
      uptimeSeconds: (json['uptimeSeconds'] as num?)?.toInt() ?? 0,
      acsSyncStatus: json['acsSyncStatus']?.toString() ?? 'SYNCED',
      lastInformAt: json['lastInformAt']?.toString(),
      lastRebootAt: json['lastRebootAt']?.toString(),
      connectedHosts: rawHosts is List
          ? rawHosts
              .whereType<Map<String, dynamic>>()
              .map(Tr069ConnectedHostModel.fromJson)
              .toList()
          : const [],
      recentCommands: rawCommands is List
          ? rawCommands
              .whereType<Map<String, dynamic>>()
              .map(Tr069CommandLogModel.fromJson)
              .toList()
          : const [],
    );
  }

  String get formattedUptime {
    final hours = uptimeSeconds ~/ 3600;
    final mins = (uptimeSeconds % 3600) ~/ 60;
    if (hours >= 24) {
      final days = hours ~/ 24;
      final remHours = hours % 24;
      return '${days}d ${remHours}h';
    }
    return '${hours}h ${mins}m';
  }
}


