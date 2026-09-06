import 'package:flutter/foundation.dart';

/// Modelo de dados de Colaborador / Usuário para governança administrativa RBAC.
@immutable
class UserModel {
  final String id;
  final String name;
  final String email;
  final String role;
  final String? cpf;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  const UserModel({
    required this.id,
    required this.name,
    required this.email,
    required this.role,
    this.cpf,
    this.active = true,
    this.createdAt,
    this.updatedAt,
  });

  factory UserModel.fromJson(Map<String, dynamic> json) {
    return UserModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      email: json['email']?.toString() ?? '',
      role: json['role']?.toString().toUpperCase() ?? 'USER',
      cpf: json['cpf']?.toString(),
      active: json['active'] as bool? ?? true,
      createdAt: json['createdAt'] != null
          ? DateTime.tryParse(json['createdAt'].toString())
          : null,
      updatedAt: json['updatedAt'] != null
          ? DateTime.tryParse(json['updatedAt'].toString())
          : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      'id': id,
      'name': name,
      'email': email,
      'role': role,
      'cpf': cpf,
      'active': active,
      'createdAt': createdAt?.toIso8601String(),
      'updatedAt': updatedAt?.toIso8601String(),
    };
  }

  UserModel copyWith({
    String? id,
    String? name,
    String? email,
    String? role,
    String? cpf,
    bool? active,
    DateTime? createdAt,
    DateTime? updatedAt,
  }) {
    return UserModel(
      id: id ?? this.id,
      name: name ?? this.name,
      email: email ?? this.email,
      role: role ?? this.role,
      cpf: cpf ?? this.cpf,
      active: active ?? this.active,
      createdAt: createdAt ?? this.createdAt,
      updatedAt: updatedAt ?? this.updatedAt,
    );
  }

  /// Retorna as iniciais do colaborador (ex: "Carlos Silva" -> "CS")
  String get initials {
    if (name.trim().isEmpty) return 'U';
    final parts = name.trim().split(RegExp(r'\s+'));
    if (parts.length == 1) {
      return parts[0][0].toUpperCase();
    }
    return '${parts[0][0]}${parts.last[0]}'.toUpperCase();
  }

  /// Nome formatado para exibição do perfil RBAC
  String get roleDisplay {
    switch (role.toUpperCase()) {
      case 'ADMIN':
        return 'Administrador';
      case 'DIRECTOR':
        return 'Diretoria';
      case 'CFO':
        return 'CFO / Diretor Financeiro';
      case 'FINANCIAL':
        return 'Financeiro & Contas';
      case 'ATTENDANT':
        return 'Atendimento / SAC';
      case 'ADMINISTRATIVE_ASSISTANT':
        return 'Assistente Administrativo';
      case 'SUPPORT_N2':
        return 'Suporte N2 (NOC)';
      case 'SUPPORT_ANALYST':
        return 'Analista de Suporte';
      case 'TECHNICIAN':
        return 'Técnico de Campo';
      case 'CLIENT':
        return 'Cliente';
      default:
        return role;
    }
  }
}
