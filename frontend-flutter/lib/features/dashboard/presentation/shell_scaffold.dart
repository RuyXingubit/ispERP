import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../../core/models/user_role.dart';
import '../../../core/providers/app_providers.dart';
import '../../../core/theme/app_theme.dart';
import '../../attendance/presentation/attendance_hub_modal.dart';

/// Shell de navegação adaptativo (Sidebar em Desktop/Web; BottomBar em Mobile).
class ShellScaffold extends ConsumerWidget {
  final Widget child;

  const ShellScaffold({super.key, required this.child});

  @override
  Widget build(BuildContext context, WidgetRef ref) {
    final authState = ref.watch(authProvider);
    final userRole = authState.role ?? UserRole.support;
    final currentPath = GoRouterState.of(context).uri.path;

    return LayoutBuilder(
      builder: (context, constraints) {
        final isDesktop = constraints.maxWidth > 800;

        if (isDesktop) {
          return Scaffold(
            body: Row(
              children: [
                // Barra Lateral de Navegação (Desktop / Web)
                _buildSidebar(context, ref, authState, userRole, currentPath),
                const VerticalDivider(width: 1),
                // Área de Conteúdo da Tela Selecionada
                Expanded(child: child),
              ],
            ),
          );
        } else {
          // Layout Mobile com Bottom Bar
          return Scaffold(
            appBar: AppBar(
              title: Row(
                children: [
                  const Icon(Icons.hub_outlined, color: AppTheme.primaryBlue, size: 20),
                  const SizedBox(width: 8),
                  const Text('ispERP', style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18)),
                  const SizedBox(width: 10),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 2),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(12),
                    ),
                    child: Text(
                      userRole.name.toUpperCase(),
                      style: const TextStyle(color: AppTheme.primaryBlue, fontSize: 10, fontWeight: FontWeight.bold),
                    ),
                  ),
                ],
              ),
              actions: [
                IconButton(
                  icon: const Icon(Icons.logout, size: 20),
                  tooltip: 'Desconectar',
                  onPressed: () => ref.read(authProvider.notifier).logout(),
                ),
              ],
            ),
            body: child,
            bottomNavigationBar: _buildMobileBottomBar(context, userRole, currentPath),
            drawer: _buildMobileDrawer(context, ref, authState, userRole),
            floatingActionButton: FloatingActionButton.extended(
              backgroundColor: AppTheme.primaryBlue,
              foregroundColor: Colors.white,
              icon: const Icon(Icons.support_agent_rounded),
              label: const Text('Atendimento', style: TextStyle(fontWeight: FontWeight.bold)),
              onPressed: () => AttendanceHubModal.show(context),
            ),
          );
        }
      },
    );
  }

  Widget _buildSidebar(
    BuildContext context,
    WidgetRef ref,
    AuthState auth,
    UserRole role,
    String currentPath,
  ) {
    return Container(
      width: 250,
      color: AppTheme.darkSurface,
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Topo: Logo & Servidor
          Padding(
            padding: const EdgeInsets.all(20.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    Container(
                      padding: const EdgeInsets.all(8),
                      decoration: BoxDecoration(
                        color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                        borderRadius: BorderRadius.circular(8),
                      ),
                      child: const Icon(Icons.hub_outlined, color: AppTheme.primaryBlue, size: 22),
                    ),
                    const SizedBox(width: 10),
                    const Text(
                      'ispERP',
                      style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, letterSpacing: -0.5),
                    ),
                  ],
                ),
                const SizedBox(height: 12),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: AppTheme.darkBg,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.darkCard),
                  ),
                  child: Row(
                    children: [
                      const Icon(Icons.circle, color: AppTheme.accentGreen, size: 8),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          auth.serverUrl ?? 'Local',
                          style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
          ),
          // Botão Central de Destaque: [ Iniciar Atendimento ]
          Padding(
            padding: const EdgeInsets.symmetric(horizontal: 14.0, vertical: 6.0),
            child: ElevatedButton.icon(
              style: ElevatedButton.styleFrom(
                backgroundColor: AppTheme.primaryBlue,
                foregroundColor: Colors.white,
                padding: const EdgeInsets.symmetric(vertical: 14),
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(10)),
                elevation: 2,
              ),
              icon: const Icon(Icons.support_agent_rounded, size: 20),
              label: const Text(
                'Iniciar Atendimento',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
              ),
              onPressed: () => AttendanceHubModal.show(context),
            ),
          ),
          const Divider(height: 1),

          // Menu de Navegação por Perfil
          Expanded(
            child: ListView(
              padding: const EdgeInsets.symmetric(vertical: 12, horizontal: 8),
              children: _buildNavMenuItems(context, role, currentPath),
            ),
          ),

          const Divider(height: 1),
          // Rodapé: Usuário Conectado & Logout
          Padding(
            padding: const EdgeInsets.all(16.0),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  children: [
                    CircleAvatar(
                      radius: 16,
                      backgroundColor: AppTheme.primaryBlue.withValues(alpha: 0.2),
                      child: Text(
                        (auth.name?.isNotEmpty == true ? auth.name![0] : 'U').toUpperCase(),
                        style: const TextStyle(color: AppTheme.primaryBlue, fontWeight: FontWeight.bold, fontSize: 13),
                      ),
                    ),
                    const SizedBox(width: 10),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            auth.name ?? 'Colaborador',
                            style: const TextStyle(fontSize: 12, fontWeight: FontWeight.w600),
                            overflow: TextOverflow.ellipsis,
                          ),
                          Text(
                            role.name.toUpperCase(),
                            style: const TextStyle(fontSize: 10, color: AppTheme.primaryBlue, fontWeight: FontWeight.bold),
                          ),
                        ],
                      ),
                    ),
                    IconButton(
                      icon: const Icon(Icons.logout, size: 18, color: AppTheme.textMuted),
                      tooltip: 'Sair da Conta',
                      onPressed: () => ref.read(authProvider.notifier).logout(),
                    ),
                  ],
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  List<Widget> _buildNavMenuItems(BuildContext context, UserRole role, String currentPath) {
    final List<Widget> items = [];

    // Se o usuário for ADMIN, ele tem acesso a todos os módulos
    if (role == UserRole.admin) {
      items.add(_buildNavItem(context, '/admin', 'Diretoria & OLTs', Icons.shield_outlined, currentPath == '/admin'));
      items.add(_buildNavItem(context, '/inventory', 'Estoque & Almoxarifado', Icons.inventory_2_outlined, currentPath == '/inventory'));
      items.add(_buildNavItem(context, '/dispatch', 'Torre de Despacho', Icons.local_shipping_outlined, currentPath == '/dispatch'));
      items.add(_buildNavItem(context, '/financial', 'Financeiro & DRE', Icons.account_balance_outlined, currentPath == '/financial'));
      items.add(_buildNavItem(context, '/support', 'Atendimento & SAC', Icons.headset_mic_outlined, currentPath == '/support'));
      items.add(_buildNavItem(context, '/sales', 'Vendas & Planos', Icons.trending_up_outlined, currentPath == '/sales'));
      items.add(_buildNavItem(context, '/technician', 'Técnico de Campo', Icons.build_circle_outlined, currentPath == '/technician'));
    } else {
      // Usuário com perfil específico tem a tela dedicada ao seu cargo
      switch (role) {
        case UserRole.financial:
          items.add(_buildNavItem(context, '/financial', 'Tesouraria & DRE', Icons.account_balance_outlined, true));
          break;
        case UserRole.support:
          items.add(_buildNavItem(context, '/support', 'Central de Suporte', Icons.headset_mic_outlined, currentPath == '/support'));
          items.add(_buildNavItem(context, '/inventory', 'Estoque & Almoxarifado', Icons.inventory_2_outlined, currentPath == '/inventory'));
          items.add(_buildNavItem(context, '/dispatch', 'Torre de Despacho', Icons.local_shipping_outlined, currentPath == '/dispatch'));
          break;
        case UserRole.sales:
          items.add(_buildNavItem(context, '/sales', 'Comercial & Vendas', Icons.trending_up_outlined, true));
          break;
        case UserRole.technician:
          items.add(_buildNavItem(context, '/technician', 'Minhas O.S.', Icons.build_circle_outlined, true));
          break;
        case UserRole.admin:
          break;
      }
    }

    items.add(const Padding(padding: EdgeInsets.symmetric(vertical: 4), child: Divider(height: 1)));
    items.add(_buildNavItem(context, '/my-cash-custody', 'Meu Caixa & Custódia', Icons.account_balance_wallet_outlined, currentPath == '/my-cash-custody'));

    return items;
  }

  Widget _buildNavItem(BuildContext context, String path, String title, IconData icon, bool isSelected) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 4.0),
      child: ListTile(
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
        tileColor: isSelected ? AppTheme.primaryBlue.withValues(alpha: 0.15) : Colors.transparent,
        leading: Icon(icon, size: 20, color: isSelected ? AppTheme.primaryBlue : AppTheme.textSecondary),
        title: Text(
          title,
          style: TextStyle(
            fontSize: 13,
            fontWeight: isSelected ? FontWeight.bold : FontWeight.w500,
            color: isSelected ? AppTheme.primaryBlue : AppTheme.textPrimary,
          ),
        ),
        onTap: () => context.go(path),
      ),
    );
  }

  Widget? _buildMobileBottomBar(BuildContext context, UserRole role, String currentPath) {
    if (role != UserRole.admin) return null; // Usuário específico já tem tela única

    int currentIndex = 0;
    if (currentPath == '/admin') currentIndex = 0;
    if (currentPath == '/financial') currentIndex = 1;
    if (currentPath == '/support') currentIndex = 2;
    if (currentPath == '/sales') currentIndex = 3;
    if (currentPath == '/technician') currentIndex = 4;

    return NavigationBar(
      selectedIndex: currentIndex,
      onDestinationSelected: (idx) {
        switch (idx) {
          case 0:
            context.go('/admin');
            break;
          case 1:
            context.go('/financial');
            break;
          case 2:
            context.go('/support');
            break;
          case 3:
            context.go('/sales');
            break;
          case 4:
            context.go('/technician');
            break;
        }
      },
      destinations: const [
        NavigationDestination(icon: Icon(Icons.shield_outlined), label: 'Admin'),
        NavigationDestination(icon: Icon(Icons.account_balance_outlined), label: 'Finanças'),
        NavigationDestination(icon: Icon(Icons.headset_mic_outlined), label: 'Suporte'),
        NavigationDestination(icon: Icon(Icons.trending_up_outlined), label: 'Vendas'),
        NavigationDestination(icon: Icon(Icons.build_circle_outlined), label: 'Técnico'),
      ],
    );
  }

  Widget _buildMobileDrawer(BuildContext context, WidgetRef ref, AuthState auth, UserRole role) {
    return Drawer(
      backgroundColor: AppTheme.darkSurface,
      child: SafeArea(
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.stretch,
          children: [
            Padding(
              padding: const EdgeInsets.all(20.0),
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  const Text('ispERP Mobile', style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold)),
                  const SizedBox(height: 4),
                  Text('Servidor: ${auth.serverUrl ?? "Local"}', style: const TextStyle(fontSize: 12, color: AppTheme.textSecondary)),
                ],
              ),
            ),
            const Divider(),
            ListTile(
              leading: const Icon(Icons.support_agent_rounded, color: AppTheme.primaryBlue),
              title: const Text('Iniciar Atendimento', style: TextStyle(fontWeight: FontWeight.bold, color: AppTheme.primaryBlue)),
              onTap: () {
                Navigator.pop(context);
                AttendanceHubModal.show(context);
              },
            ),
            ListTile(
              leading: const Icon(Icons.account_balance_wallet_outlined),
              title: const Text('Meu Caixa & Custódia'),
              onTap: () {
                Navigator.pop(context);
                context.go('/my-cash-custody');
              },
            ),
            ListTile(
              leading: const Icon(Icons.dns_outlined),
              title: const Text('Trocar de Servidor'),
              onTap: () {
                Navigator.pop(context);
                context.go('/server-setup');
              },
            ),
            ListTile(
              leading: const Icon(Icons.logout, color: AppTheme.accentError),
              title: const Text('Sair', style: TextStyle(color: AppTheme.accentError)),
              onTap: () {
                Navigator.pop(context);
                ref.read(authProvider.notifier).logout();
              },
            ),
          ],
        ),
      ),
    );
  }
}
