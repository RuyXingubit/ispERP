import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:go_router/go_router.dart';
import '../../features/admin/presentation/admin_dashboard_screen.dart';
import '../../features/admin/presentation/audit_trail_screen.dart';
import '../../features/admin/presentation/nas_management_screen.dart';
import '../../features/admin/presentation/payment_gateway_config_screen.dart';
import '../../features/admin/presentation/users_management_screen.dart';
import '../../features/auth/presentation/login_screen.dart';
import '../../features/dashboard/presentation/shell_scaffold.dart';
import '../../features/dispatch/presentation/dispatch_control_tower_screen.dart';
import '../../features/financial/presentation/financial_dashboard_screen.dart';
import '../../features/financial/presentation/my_cash_custody_screen.dart';
import '../../features/inventory/presentation/inventory_screen.dart';
import '../../features/sales/presentation/sales_dashboard_screen.dart';
import '../../features/server_setup/presentation/initial_system_setup_screen.dart';
import '../../features/server_setup/presentation/server_setup_screen.dart';
import '../../features/support/presentation/support_dashboard_screen.dart';
import '../../features/technician/presentation/technician_dashboard_screen.dart';
import '../providers/app_providers.dart';

class RouterNotifier extends ChangeNotifier {
  final Ref _ref;

  RouterNotifier(this._ref) {
    _ref.listen<AuthState>(authProvider, (previous, next) => notifyListeners());
  }
}

final routerNotifierProvider = Provider<RouterNotifier>((ref) {
  return RouterNotifier(ref);
});

final routerProvider = Provider<GoRouter>((ref) {
  final notifier = ref.watch(routerNotifierProvider);

  return GoRouter(
    initialLocation: '/login',
    refreshListenable: notifier,
    redirect: (context, state) {
      final currentAuth = ref.read(authProvider);
      if (currentAuth.isLoading) return null;

      final isServerSetup = state.matchedLocation == '/server-setup';
      final isInitialSetup = state.matchedLocation == '/initial-setup';
      final isLogin = state.matchedLocation == '/login';

      // 1. Se o servidor ainda não foi configurado, força a tela de Setup de conexão
      if (!currentAuth.hasServerConfigured && !isServerSetup) {
        return '/server-setup';
      }

      // 2. Se o servidor está configurado mas o usuário não está autenticado
      // Permite /login, /server-setup e /initial-setup livremente
      if (currentAuth.hasServerConfigured && !currentAuth.isAuthenticated && !isLogin && !isServerSetup && !isInitialSetup) {
        return '/login';
      }

      // 3. Se o usuário já está autenticado e tenta acessar Login, ServerSetup ou InitialSetup, vai para o seu dashboard
      if (currentAuth.isAuthenticated && (isLogin || isServerSetup || isInitialSetup)) {
        return currentAuth.role?.initialRoute ?? '/admin';
      }

      return null;
    },
    routes: [
      GoRoute(
        path: '/server-setup',
        builder: (context, state) => const ServerSetupScreen(),
      ),
      GoRoute(
        path: '/initial-setup',
        builder: (context, state) => const InitialSystemSetupScreen(),
      ),
      GoRoute(
        path: '/login',
        builder: (context, state) => const LoginScreen(),
      ),
      ShellRoute(
        builder: (context, state, child) => ShellScaffold(child: child),
        routes: [
          GoRoute(
            path: '/admin',
            builder: (context, state) => const AdminDashboardScreen(),
          ),
          GoRoute(
            path: '/admin/users',
            builder: (context, state) => const UsersManagementScreen(),
          ),
          GoRoute(
            path: '/admin/audit-logs',
            builder: (context, state) => const AuditTrailScreen(),
          ),
          GoRoute(
            path: '/admin/nas',
            builder: (context, state) => const NasManagementScreen(),
          ),
          GoRoute(
            path: '/admin/gateways',
            builder: (context, state) => const PaymentGatewayConfigScreen(),
          ),
          GoRoute(
            path: '/financial',
            builder: (context, state) => const FinancialDashboardScreen(),
          ),
          GoRoute(
            path: '/support',
            builder: (context, state) => const SupportDashboardScreen(),
          ),
          GoRoute(
            path: '/sales',
            builder: (context, state) => const SalesDashboardScreen(),
          ),
          GoRoute(
            path: '/technician',
            builder: (context, state) => const TechnicianDashboardScreen(),
          ),
          GoRoute(
            path: '/my-cash-custody',
            builder: (context, state) => const MyCashCustodyScreen(),
          ),
          GoRoute(
            path: '/dispatch',
            builder: (context, state) => const DispatchControlTowerScreen(),
          ),
          GoRoute(
            path: '/inventory',
            builder: (context, state) => const InventoryScreen(),
          ),
        ],
      ),
    ],
  );
});
