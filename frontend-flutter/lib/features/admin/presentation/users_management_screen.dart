import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/user_model.dart';
import '../data/users_notifier.dart';

/// Tela de Gestão de Colaboradores e Matriz de Acesso RBAC.
class UsersManagementScreen extends ConsumerStatefulWidget {
  const UsersManagementScreen({super.key});

  @override
  ConsumerState<UsersManagementScreen> createState() => _UsersManagementScreenState();
}

class _UsersManagementScreenState extends ConsumerState<UsersManagementScreen> {
  final TextEditingController _searchController = TextEditingController();

  @override
  void dispose() {
    _searchController.dispose();
    super.dispose();
  }

  @override
  Widget build(BuildContext context) {
    final state = ref.watch(usersProvider);
    final notifier = ref.read(usersProvider.notifier);

    // Feedback SnackBar para mensagens de sucesso ou erro
    ref.listen<UsersState>(usersProvider, (prev, next) {
      if (next.errorMessage != null && next.errorMessage != prev?.errorMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.errorMessage!),
            backgroundColor: AppTheme.accentError,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
      if (next.successMessage != null && next.successMessage != prev?.successMessage) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(next.successMessage!),
            backgroundColor: AppTheme.accentGreen,
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    });

    return Scaffold(
      body: SingleChildScrollView(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            // Cabeçalho defensivo com título e botão de ação
            _buildHeader(context, state, notifier),
            const SizedBox(height: 20),

            // Métricas Rápidas (Cards de Indicadores Reais)
            _buildStatCards(state),
            const SizedBox(height: 20),

            // Barra de Busca e Filtros
            _buildFilterBar(state, notifier),
            const SizedBox(height: 20),

            // Tabela / Lista de Colaboradores
            if (state.isLoading)
              const Center(
                child: Padding(
                  padding: EdgeInsets.all(40.0),
                  child: CircularProgressIndicator(),
                ),
              )
            else if (state.filteredUsers.isEmpty)
              _buildEmptyState()
            else
              _buildUsersTable(context, state.filteredUsers, notifier, state.isSubmitting),
          ],
        ),
      ),
    );
  }

  Widget _buildHeader(BuildContext context, UsersState state, UsersNotifier notifier) {
    return LayoutBuilder(
      builder: (context, constraints) {
        final isCompact = constraints.maxWidth < 650;
        return Flex(
          direction: isCompact ? Axis.vertical : Axis.horizontal,
          crossAxisAlignment: isCompact ? CrossAxisAlignment.start : CrossAxisAlignment.center,
          mainAxisAlignment: MainAxisAlignment.spaceBetween,
          children: [
            if (!isCompact)
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: const [
                    Text(
                      'Gestão de Colaboradores & RBAC',
                      style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                      overflow: TextOverflow.ellipsis,
                    ),
                    SizedBox(height: 4),
                    Text(
                      'Controle de credenciais, suspensão de acesso e matriz de cargos do provedor',
                      style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              )
            else
              Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: const [
                  Text(
                    'Gestão de Colaboradores & RBAC',
                    style: TextStyle(fontSize: 22, fontWeight: FontWeight.bold),
                    overflow: TextOverflow.ellipsis,
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Controle de credenciais, suspensão de acesso e matriz de cargos do provedor',
                    style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
                    overflow: TextOverflow.ellipsis,
                  ),
                ],
              ),
            if (isCompact) const SizedBox(height: 12),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                IconButton(
                  tooltip: 'Atualizar Lista',
                  icon: const Icon(Icons.refresh),
                  onPressed: state.isLoading ? null : () => notifier.loadUsers(),
                ),
                ElevatedButton.icon(
                  style: ElevatedButton.styleFrom(
                    backgroundColor: AppTheme.primaryBlue,
                    foregroundColor: Colors.white,
                    padding: const EdgeInsets.symmetric(horizontal: 14, vertical: 10),
                  ),
                  icon: const Icon(Icons.person_add_alt_1_outlined, size: 18),
                  label: const Text('Novo Colaborador', style: TextStyle(fontWeight: FontWeight.bold)),
                  onPressed: () => _showCreateUserModal(context, notifier),
                ),
              ],
            ),
          ],
        );
      },
    );
  }

  Widget _buildStatCards(UsersState state) {
    return Wrap(
      spacing: 16,
      runSpacing: 16,
      children: [
        _buildSmallMetricCard(
          title: 'Total de Colaboradores',
          value: '${state.users.length}',
          icon: Icons.people_outline,
          color: AppTheme.primaryBlue,
        ),
        _buildSmallMetricCard(
          title: 'Acessos Ativos',
          value: '${state.totalActive}',
          icon: Icons.check_circle_outline,
          color: AppTheme.accentGreen,
        ),
        _buildSmallMetricCard(
          title: 'Acessos Suspensos',
          value: '${state.totalSuspended}',
          icon: Icons.block_outlined,
          color: AppTheme.accentError,
        ),
      ],
    );
  }

  Widget _buildSmallMetricCard({
    required String title,
    required String value,
    required IconData icon,
    required Color color,
  }) {
    return Container(
      width: 220,
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Row(
        children: [
          Container(
            padding: const EdgeInsets.all(10),
            decoration: BoxDecoration(
              color: color.withValues(alpha: 0.12),
              borderRadius: BorderRadius.circular(10),
            ),
            child: Icon(icon, color: color, size: 22),
          ),
          const SizedBox(width: 14),
          Expanded(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  title,
                  style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
                const SizedBox(height: 2),
                Text(
                  value,
                  style: const TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildFilterBar(UsersState state, UsersNotifier notifier) {
    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isCompact = constraints.maxWidth < 700;
          return Flex(
            direction: isCompact ? Axis.vertical : Axis.horizontal,
            crossAxisAlignment: isCompact ? CrossAxisAlignment.stretch : CrossAxisAlignment.center,
            children: [
              // Campo de Busca
              Expanded(
                flex: isCompact ? 0 : 4,
                child: TextField(
                  controller: _searchController,
                  decoration: InputDecoration(
                    hintText: 'Pesquisar por nome, e-mail, CPF ou cargo...',
                    prefixIcon: const Icon(Icons.search, size: 20),
                    suffixIcon: _searchController.text.isNotEmpty
                        ? IconButton(
                            icon: const Icon(Icons.clear, size: 18),
                            onPressed: () {
                              _searchController.clear();
                              notifier.setSearchQuery('');
                            },
                          )
                        : null,
                    isDense: true,
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(8)),
                  ),
                  onChanged: (val) => notifier.setSearchQuery(val),
                ),
              ),
              if (isCompact) const SizedBox(height: 12) else const SizedBox(width: 16),

              // Filtros de Status (Chips)
              Wrap(
                spacing: 8,
                children: [
                  _buildStatusFilterChip(
                    label: 'Todos',
                    selected: state.statusFilter == 'ALL',
                    onSelected: () => notifier.setStatusFilter('ALL'),
                  ),
                  _buildStatusFilterChip(
                    label: 'Ativos',
                    selected: state.statusFilter == 'ACTIVE',
                    onSelected: () => notifier.setStatusFilter('ACTIVE'),
                    activeColor: AppTheme.accentGreen,
                  ),
                  _buildStatusFilterChip(
                    label: 'Suspensos',
                    selected: state.statusFilter == 'SUSPENDED',
                    onSelected: () => notifier.setStatusFilter('SUSPENDED'),
                    activeColor: AppTheme.accentError,
                  ),
                ],
              ),
            ],
          );
        },
      ),
    );
  }

  Widget _buildStatusFilterChip({
    required String label,
    required bool selected,
    required VoidCallback onSelected,
    Color? activeColor,
  }) {
    final color = activeColor ?? AppTheme.primaryBlue;
    return ChoiceChip(
      label: Text(label, style: TextStyle(fontSize: 12, color: selected ? Colors.white : AppTheme.textSecondary)),
      selected: selected,
      selectedColor: color,
      backgroundColor: AppTheme.darkBg,
      onSelected: (_) => onSelected(),
    );
  }

  Widget _buildEmptyState() {
    return Container(
      width: double.infinity,
      padding: const EdgeInsets.all(40),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      child: Column(
        children: const [
          Icon(Icons.person_search_outlined, size: 48, color: AppTheme.textMuted),
          SizedBox(height: 12),
          Text(
            'Nenhum colaborador encontrado',
            style: TextStyle(fontWeight: FontWeight.bold, fontSize: 16),
          ),
          SizedBox(height: 4),
          Text(
            'Tente ajustar os termos de pesquisa ou o filtro de status.',
            style: TextStyle(color: AppTheme.textSecondary, fontSize: 13),
          ),
        ],
      ),
    );
  }

  Widget _buildUsersTable(
    BuildContext context,
    List<UserModel> users,
    UsersNotifier notifier,
    bool isSubmitting,
  ) {
    return Container(
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkCard),
      ),
      clipBehavior: Clip.antiAlias,
      child: ListView.separated(
        shrinkWrap: true,
        physics: const NeverScrollableScrollPhysics(),
        itemCount: users.length,
        separatorBuilder: (_, _) => const Divider(height: 1),
        itemBuilder: (context, index) {
          final user = users[index];
          return _buildUserRow(context, user, notifier, isSubmitting);
        },
      ),
    );
  }

  Widget _buildUserRow(
    BuildContext context,
    UserModel user,
    UsersNotifier notifier,
    bool isSubmitting,
  ) {
    return Padding(
      padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
      child: LayoutBuilder(
        builder: (context, constraints) {
          final isCompact = constraints.maxWidth < 800;

          if (isCompact) {
            // Layout Compacto para telas estreitas
            return Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    _buildAvatar(user),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(user.name, style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14), overflow: TextOverflow.ellipsis),
                          Text(user.email, style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary), overflow: TextOverflow.ellipsis),
                        ],
                      ),
                    ),
                    _buildStatusBadge(user.active),
                  ],
                ),
                const SizedBox(height: 8),
                Row(
                  children: [
                    Expanded(
                      child: Align(
                        alignment: Alignment.centerLeft,
                        child: _buildRoleBadge(user.role, user.roleDisplay),
                      ),
                    ),
                    const SizedBox(width: 8),
                    _buildActionButtons(context, user, notifier, isSubmitting),
                  ],
                ),
              ],
            );
          }

          // Layout Tabela Desktop/Web
          return Row(
            children: [
              _buildAvatar(user),
              const SizedBox(width: 14),
              // Nome & Email
              Expanded(
                flex: 4,
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      user.name,
                      style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 14),
                      overflow: TextOverflow.ellipsis,
                    ),
                    const SizedBox(height: 2),
                    Text(
                      user.email,
                      style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ],
                ),
              ),
              // CPF
              Expanded(
                flex: 2,
                child: Text(
                  user.cpf ?? 'Não informado',
                  style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
              // Cargo / Role Badge
              Expanded(
                flex: 3,
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: _buildRoleBadge(user.role, user.roleDisplay),
                ),
              ),
              // Status Badge
              Expanded(
                flex: 2,
                child: Align(
                  alignment: Alignment.centerLeft,
                  child: _buildStatusBadge(user.active),
                ),
              ),
              // Ações de Governança
              _buildActionButtons(context, user, notifier, isSubmitting),
            ],
          );
        },
      ),
    );
  }

  Widget _buildAvatar(UserModel user) {
    final color = _getRoleColor(user.role);
    return CircleAvatar(
      radius: 18,
      backgroundColor: color.withValues(alpha: 0.15),
      child: Text(
        user.initials,
        style: TextStyle(color: color, fontWeight: FontWeight.bold, fontSize: 12),
      ),
    );
  }

  Widget _buildStatusBadge(bool active) {
    final color = active ? AppTheme.accentGreen : AppTheme.accentError;
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(20),
        border: Border.all(color: color.withValues(alpha: 0.4)),
      ),
      child: Row(
        mainAxisSize: MainAxisSize.min,
        children: [
          Icon(Icons.circle, color: color, size: 6),
          const SizedBox(width: 6),
          Text(
            active ? 'Ativo' : 'Suspenso',
            style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.bold),
          ),
        ],
      ),
    );
  }

  Widget _buildRoleBadge(String role, String displayName) {
    final color = _getRoleColor(role);
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
      decoration: BoxDecoration(
        color: color.withValues(alpha: 0.12),
        borderRadius: BorderRadius.circular(8),
      ),
      child: Text(
        displayName,
        style: TextStyle(color: color, fontSize: 11, fontWeight: FontWeight.w600),
        overflow: TextOverflow.ellipsis,
      ),
    );
  }

  Color _getRoleColor(String role) {
    switch (role.toUpperCase()) {
      case 'ADMIN':
      case 'DIRECTOR':
        return Colors.purpleAccent;
      case 'CFO':
      case 'FINANCIAL':
        return AppTheme.accentGreen;
      case 'SUPPORT_N2':
      case 'SUPPORT_ANALYST':
      case 'ATTENDANT':
        return AppTheme.primaryBlue;
      case 'TECHNICIAN':
        return AppTheme.accentWarning;
      default:
        return AppTheme.textSecondary;
    }
  }

  Widget _buildActionButtons(
    BuildContext context,
    UserModel user,
    UsersNotifier notifier,
    bool isSubmitting,
  ) {
    return PopupMenuButton<String>(
      icon: const Icon(Icons.more_vert, size: 20),
      tooltip: 'Ações de Governança',
      enabled: !isSubmitting,
      onSelected: (action) {
        switch (action) {
          case 'toggle_status':
            _confirmToggleStatus(context, user, notifier);
            break;
          case 'change_role':
            _showChangeRoleModal(context, user, notifier);
            break;
          case 'reset_password':
            _showResetPasswordModal(context, user, notifier);
            break;
          case 'delete':
            _confirmDeleteUser(context, user, notifier);
            break;
        }
      },
      itemBuilder: (context) => [
        PopupMenuItem(
          value: 'toggle_status',
          child: Row(
            children: [
              Icon(
                user.active ? Icons.block_outlined : Icons.check_circle_outline,
                size: 18,
                color: user.active ? AppTheme.accentError : AppTheme.accentGreen,
              ),
              const SizedBox(width: 10),
              Text(user.active ? 'Suspender Acesso' : 'Reativar Acesso'),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'change_role',
          child: Row(
            children: const [
              Icon(Icons.badge_outlined, size: 18, color: AppTheme.primaryBlue),
              SizedBox(width: 10),
              Text('Alterar Cargo / Perfil'),
            ],
          ),
        ),
        PopupMenuItem(
          value: 'reset_password',
          child: Row(
            children: const [
              Icon(Icons.lock_reset_outlined, size: 18, color: AppTheme.accentWarning),
              SizedBox(width: 10),
              Text('Redefinir Senha'),
            ],
          ),
        ),
        const PopupMenuDivider(),
        PopupMenuItem(
          value: 'delete',
          child: Row(
            children: const [
              Icon(Icons.delete_outline, size: 18, color: AppTheme.accentError),
              SizedBox(width: 10),
              Text('Remover Colaborador', style: TextStyle(color: AppTheme.accentError)),
            ],
          ),
        ),
      ],
    );
  }

  void _confirmToggleStatus(BuildContext context, UserModel user, UsersNotifier notifier) {
    final willSuspend = user.active;
    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        title: Text(willSuspend ? 'Suspender Acesso?' : 'Reativar Acesso?'),
        content: Text(
          willSuspend
              ? 'Ao suspender, o colaborador "${user.name}" não conseguirá mais efetuar login no sistema nem acessar a API.'
              : 'O acesso do colaborador "${user.name}" será restaurado imediatamente.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: willSuspend ? AppTheme.accentError : AppTheme.accentGreen,
              foregroundColor: Colors.white,
            ),
            onPressed: () {
              Navigator.pop(dialogCtx);
              notifier.toggleUserStatus(user.id, !willSuspend);
            },
            child: Text(willSuspend ? 'Suspender' : 'Reativar'),
          ),
        ],
      ),
    );
  }

  void _confirmDeleteUser(BuildContext context, UserModel user, UsersNotifier notifier) {
    showDialog(
      context: context,
      builder: (dialogCtx) => AlertDialog(
        title: const Text('Remover Colaborador?'),
        content: Text(
          'Deseja remover permanentemente "${user.name}"? Esta ação só terá efeito se o usuário não possuir lançamentos históricos obrigatórios.',
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(dialogCtx),
            child: const Text('Cancelar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentError,
              foregroundColor: Colors.white,
            ),
            onPressed: () {
              Navigator.pop(dialogCtx);
              notifier.deleteUser(user.id);
            },
            child: const Text('Remover'),
          ),
        ],
      ),
    );
  }

  void _showCreateUserModal(BuildContext context, UsersNotifier notifier) {
    final formKey = GlobalKey<FormState>();
    final nameCtrl = TextEditingController();
    final emailCtrl = TextEditingController();
    final cpfCtrl = TextEditingController();
    final passCtrl = TextEditingController();
    String selectedRole = 'ATTENDANT';
    bool obscure = true;

    showDialog(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          title: const Text('Cadastrar Novo Colaborador'),
          content: SizedBox(
            width: 460,
            child: Form(
              key: formKey,
              child: SingleChildScrollView(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    TextFormField(
                      controller: nameCtrl,
                      decoration: const InputDecoration(labelText: 'Nome Completo *', isDense: true),
                      validator: (v) => (v == null || v.trim().isEmpty) ? 'Informe o nome' : null,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: emailCtrl,
                      decoration: const InputDecoration(labelText: 'E-mail Corporativo *', isDense: true),
                      keyboardType: TextInputType.emailAddress,
                      validator: (v) => (v == null || !v.contains('@')) ? 'Informe um e-mail válido' : null,
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: cpfCtrl,
                      decoration: const InputDecoration(labelText: 'CPF (opcional)', isDense: true),
                    ),
                    const SizedBox(height: 12),
                    DropdownButtonFormField<String>(
                      initialValue: selectedRole,
                      decoration: const InputDecoration(labelText: 'Cargo / Perfil RBAC *', isDense: true),
                      items: const [
                        DropdownMenuItem(value: 'ADMIN', child: Text('Administrador Geral (Acesso Total)')),
                        DropdownMenuItem(value: 'DIRECTOR', child: Text('Diretoria (Visão Executiva & BI)')),
                        DropdownMenuItem(value: 'CFO', child: Text('CFO / Diretor Financeiro')),
                        DropdownMenuItem(value: 'FINANCIAL', child: Text('Financeiro & Contas a Receber')),
                        DropdownMenuItem(value: 'ATTENDANT', child: Text('Atendente / SAC')),
                        DropdownMenuItem(value: 'ADMINISTRATIVE_ASSISTANT', child: Text('Assistente Administrativo')),
                        DropdownMenuItem(value: 'SUPPORT_N2', child: Text('Suporte N2 (NOC & Telecom)')),
                        DropdownMenuItem(value: 'SUPPORT_ANALYST', child: Text('Analista de Suporte Técnico')),
                        DropdownMenuItem(value: 'TECHNICIAN', child: Text('Técnico de Campo')),
                      ],
                      onChanged: (val) {
                        if (val != null) setModalState(() => selectedRole = val);
                      },
                    ),
                    const SizedBox(height: 12),
                    TextFormField(
                      controller: passCtrl,
                      obscureText: obscure,
                      decoration: InputDecoration(
                        labelText: 'Senha Inicial *',
                        isDense: true,
                        suffixIcon: IconButton(
                          icon: Icon(obscure ? Icons.visibility_off : Icons.visibility, size: 18),
                          onPressed: () => setModalState(() => obscure = !obscure),
                        ),
                      ),
                      validator: (v) => (v == null || v.length < 6) ? 'Mínimo de 6 caracteres' : null,
                    ),
                  ],
                ),
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogCtx),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              onPressed: () async {
                if (formKey.currentState?.validate() == true) {
                  final ok = await notifier.createUser(
                    name: nameCtrl.text.trim(),
                    email: emailCtrl.text.trim(),
                    password: passCtrl.text,
                    role: selectedRole,
                    cpf: cpfCtrl.text.trim().isEmpty ? null : cpfCtrl.text.trim(),
                  );
                  if (ok && dialogCtx.mounted) {
                    Navigator.pop(dialogCtx);
                  }
                }
              },
              child: const Text('Cadastrar'),
            ),
          ],
        ),
      ),
    );
  }

  void _showChangeRoleModal(BuildContext context, UserModel user, UsersNotifier notifier) {
    String selected = user.role.toUpperCase();

    showDialog(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          title: Text('Alterar Perfil: ${user.name}'),
          content: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Selecione o novo papel de acesso atribuído ao colaborador:',
                style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
              ),
              const SizedBox(height: 16),
              DropdownButtonFormField<String>(
                initialValue: selected,
                decoration: const InputDecoration(labelText: 'Novo Cargo', isDense: true),
                items: const [
                  DropdownMenuItem(value: 'ADMIN', child: Text('Administrador Geral')),
                  DropdownMenuItem(value: 'DIRECTOR', child: Text('Diretoria')),
                  DropdownMenuItem(value: 'CFO', child: Text('CFO')),
                  DropdownMenuItem(value: 'FINANCIAL', child: Text('Financeiro')),
                  DropdownMenuItem(value: 'ATTENDANT', child: Text('Atendente / SAC')),
                  DropdownMenuItem(value: 'ADMINISTRATIVE_ASSISTANT', child: Text('Assistente Administrativo')),
                  DropdownMenuItem(value: 'SUPPORT_N2', child: Text('Suporte N2 (NOC)')),
                  DropdownMenuItem(value: 'SUPPORT_ANALYST', child: Text('Analista de Suporte')),
                  DropdownMenuItem(value: 'TECHNICIAN', child: Text('Técnico de Campo')),
                ],
                onChanged: (v) {
                  if (v != null) setModalState(() => selected = v);
                },
              ),
            ],
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogCtx),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
              ),
              onPressed: () async {
                Navigator.pop(dialogCtx);
                await notifier.updateUserRole(user.id, selected);
              },
              child: const Text('Salvar Perfil'),
            ),
          ],
        ),
      ),
    );
  }

  void _showResetPasswordModal(BuildContext context, UserModel user, UsersNotifier notifier) {
    final formKey = GlobalKey<FormState>();
    final passCtrl = TextEditingController();
    bool obscure = true;

    showDialog(
      context: context,
      builder: (dialogCtx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          title: Text('Redefinir Senha: ${user.name}'),
          content: Form(
            key: formKey,
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Text(
                  'Digite a nova senha segura que será atribuída ao colaborador:',
                  style: TextStyle(fontSize: 13, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 16),
                TextFormField(
                  controller: passCtrl,
                  obscureText: obscure,
                  decoration: InputDecoration(
                    labelText: 'Nova Senha *',
                    isDense: true,
                    suffixIcon: IconButton(
                      icon: Icon(obscure ? Icons.visibility_off : Icons.visibility, size: 18),
                      onPressed: () => setModalState(() => obscure = !obscure),
                    ),
                  ),
                  validator: (v) => (v == null || v.length < 6) ? 'Mínimo de 6 caracteres' : null,
                ),
              ],
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(dialogCtx),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.accentWarning,
                foregroundColor: Colors.black,
              ),
              onPressed: () async {
                if (formKey.currentState?.validate() == true) {
                  Navigator.pop(dialogCtx);
                  await notifier.resetPassword(user.id, passCtrl.text);
                }
              },
              child: const Text('Redefinir', style: TextStyle(fontWeight: FontWeight.bold)),
            ),
          ],
        ),
      ),
    );
  }
}
