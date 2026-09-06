import 'package:flutter/material.dart';

class AuditLogModel {
  final String id;
  final String? userId;
  final String userName;
  final String userEmail;
  final String action;
  final String entityName;
  final String? entityId;
  final String? details;
  final DateTime createdAt;

  AuditLogModel({
    required this.id,
    this.userId,
    required this.userName,
    required this.userEmail,
    required this.action,
    required this.entityName,
    this.entityId,
    this.details,
    required this.createdAt,
  });

  factory AuditLogModel.fromJson(Map<String, dynamic> json) {
    return AuditLogModel(
      id: json['id'] as String? ?? '',
      userId: json['userId'] as String?,
      userName: json['userName'] as String? ?? 'Sistema',
      userEmail: json['userEmail'] as String? ?? 'sistema@isperp.local',
      action: json['action'] as String? ?? '',
      entityName: json['entityName'] as String? ?? '',
      entityId: json['entityId'] as String?,
      details: json['details'] as String?,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'] as String) ?? DateTime.now()
          : DateTime.now(),
    );
  }

  String get formattedDate {
    final day = createdAt.day.toString().padLeft(2, '0');
    final month = createdAt.month.toString().padLeft(2, '0');
    final year = createdAt.year.toString();
    final hour = createdAt.hour.toString().padLeft(2, '0');
    final minute = createdAt.minute.toString().padLeft(2, '0');
    final second = createdAt.second.toString().padLeft(2, '0');
    return '$day/$month/$year $hour:$minute:$second';
  }

  String get moduleDisplay {
    switch (entityName.toUpperCase()) {
      case 'FINANCIAL':
        return 'Financeiro / Lançamentos';
      case 'INVOICE':
        return 'Contas a Receber / Faturas';
      case 'USER':
        return 'Gestão de Usuários / RBAC';
      case 'CONTRACT':
        return 'Contratos de Clientes';
      case 'CUSTOMER':
        return 'Cadastro de Clientes';
      case 'WORK_ORDER':
        return 'Ordens de Serviço';
      default:
        return entityName.isEmpty ? 'Geral' : entityName;
    }
  }

  String get actionDisplay {
    switch (action.toUpperCase()) {
      case 'USER_CREATED':
        return 'Colaborador Criado';
      case 'USER_UPDATED':
        return 'Dados de Usuário Atualizados';
      case 'USER_STATUS_CHANGED':
        return 'Status Alterado (Ativação/Suspensão)';
      case 'USER_ROLE_CHANGED':
        return 'Cargo/Permissão Alterado';
      case 'USER_PASSWORD_RESET':
        return 'Redefinição de Senha';
      case 'USER_DELETED':
        return 'Colaborador Removido';
      case 'INVOICE_CREATED':
        return 'Fatura Emitida';
      case 'INVOICE_PAID':
        return 'Fatura Baixada / Paga';
      case 'INVOICE_CANCELLED':
        return 'Fatura Cancelada';
      case 'FINANCIAL_ENTRY_CREATED':
        return 'Lançamento Criado';
      case 'FINANCIAL_DEBIT':
        return 'Lançamento de Débito';
      case 'FINANCIAL_CREDIT':
        return 'Lançamento de Crédito';
      default:
        return action.replaceAll('_', ' ');
    }
  }

  Color get tagColor {
    final act = action.toUpperCase();
    if (act.contains('PAID') || act.contains('ACTIVE') || act.contains('CREATED')) {
      return const Color(0xFF10B981); // Emerald
    }
    if (act.contains('CANCEL') || act.contains('DELETE') || act.contains('SUSPEND')) {
      return const Color(0xFFEF4444); // Red
    }
    if (act.contains('RESET') || act.contains('STATUS')) {
      return const Color(0xFFF59E0B); // Amber
    }
    if (act.contains('ROLE') || act.contains('FINANCIAL')) {
      return const Color(0xFF6366F1); // Indigo
    }
    return const Color(0xFF3B82F6); // Blue
  }
}

class AuditLogPage {
  final List<AuditLogModel> items;
  final int totalElements;
  final int totalPages;
  final int currentPage;

  AuditLogPage({
    required this.items,
    required this.totalElements,
    required this.totalPages,
    required this.currentPage,
  });

  factory AuditLogPage.empty() {
    return AuditLogPage(
      items: [],
      totalElements: 0,
      totalPages: 0,
      currentPage: 0,
    );
  }

  factory AuditLogPage.fromJson(Map<String, dynamic> json) {
    final content = json['content'] as List? ?? [];
    final items = content
        .map((e) => AuditLogModel.fromJson(e as Map<String, dynamic>))
        .toList();
    return AuditLogPage(
      items: items,
      totalElements: json['totalElements'] as int? ?? items.length,
      totalPages: json['totalPages'] as int? ?? 1,
      currentPage: json['number'] as int? ?? 0,
    );
  }
}
