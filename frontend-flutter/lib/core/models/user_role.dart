/// Perfis de acesso (RBAC) do ecossistema ispERP.
enum UserRole {
  admin,
  financial,
  support,
  sales,
  technician;

  /// Converte uma string (ex: 'ROLE_ADMIN', 'admin', 'ADMIN') para o enum [UserRole].
  static UserRole fromString(String? role) {
    if (role == null) return UserRole.support;
    final normalized = role.toUpperCase().replaceAll('ROLE_', '').trim();
    switch (normalized) {
      case 'ADMIN':
      case 'ADMINISTRATOR':
      case 'DIRECTOR':
        return UserRole.admin;
      case 'FINANCIAL':
      case 'FINANCE':
      case 'ADMINISTRATIVO':
        return UserRole.financial;
      case 'SUPPORT':
      case 'ATENDIMENTO':
      case 'HELPDESK':
        return UserRole.support;
      case 'SALES':
      case 'COMERCIAL':
      case 'VENDAS':
        return UserRole.sales;
      case 'TECHNICIAN':
      case 'TECNICO':
      case 'FIELD_TECH':
        return UserRole.technician;
      default:
        return UserRole.support;
    }
  }

  /// Nome amigável de exibição para a interface.
  String get displayName {
    switch (this) {
      case UserRole.admin:
        return 'Diretoria & Administração Geral';
      case UserRole.financial:
        return 'Administrativo & Financeiro';
      case UserRole.support:
        return 'Atendimento & Suporte Técnico';
      case UserRole.sales:
        return 'Comercial & Vendas';
      case UserRole.technician:
        return 'Técnico de Campo';
    }
  }

  /// Rota padrão inicial atribuída ao perfil após o login.
  String get initialRoute {
    switch (this) {
      case UserRole.admin:
        return '/admin';
      case UserRole.financial:
        return '/financial';
      case UserRole.support:
        return '/support';
      case UserRole.sales:
        return '/sales';
      case UserRole.technician:
        return '/technician';
    }
  }
}
