import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/theme/app_theme.dart';
import 'package:isperp_app/features/admin/data/audit_log_model.dart';
import 'package:isperp_app/features/admin/data/audit_log_notifier.dart';
import 'package:isperp_app/features/admin/data/audit_log_repository.dart';
import 'package:isperp_app/features/admin/data/user_model.dart';
import 'package:isperp_app/features/admin/data/users_notifier.dart';
import 'package:isperp_app/features/admin/data/users_repository.dart';
import 'package:isperp_app/features/admin/presentation/audit_trail_screen.dart';

class _FakeAuditRepository implements AuditLogRepository {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class _FakeUsersRepository implements UsersRepository {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

class FakeAuditNotifier extends AuditLogNotifier {
  FakeAuditNotifier(AuditLogPage initialPage)
      : super(_FakeAuditRepository()) {
    state = AuditLogState(
      pageAsync: AsyncValue.data(initialPage),
      activePeriodFilter: 'all',
    );
  }
}

class FakeUsersNotifier extends UsersNotifier {
  FakeUsersNotifier(List<UserModel> initialUsers)
      : super(_FakeUsersRepository()) {
    state = UsersState(isLoading: false, users: initialUsers);
  }
}

void main() {
  final sampleLogs = [
    AuditLogModel(
      id: '0192e123-4567-7890-abcd-ef1234567890',
      userId: '0192e123-4567-7890-abcd-ef1234567891',
      userName: 'Mariana Financeiro',
      userEmail: 'mariana@provedor.com.br',
      action: 'INVOICE_PAID',
      entityName: 'INVOICE',
      entityId: 'inv-456',
      details: '{"valor":"89.90","forma":"PIX"}',
      createdAt: DateTime(2026, 9, 5, 14, 30, 0),
    ),
    AuditLogModel(
      id: '0192e123-4567-7890-abcd-ef1234567892',
      userId: '0192e123-4567-7890-abcd-ef1234567893',
      userName: 'Carlos Admin',
      userEmail: 'carlos@provedor.com.br',
      action: 'USER_ROLE_CHANGED',
      entityName: 'USER',
      entityId: 'user-789',
      details: '{"novaRole":"FINANCIAL"}',
      createdAt: DateTime(2026, 9, 5, 15, 0, 0),
    ),
  ];

  final sampleUsers = [
    const UserModel(
      id: '0192e123-4567-7890-abcd-ef1234567891',
      name: 'Mariana Financeiro',
      email: 'mariana@provedor.com.br',
      role: 'FINANCIAL',
      active: true,
    ),
  ];

  testWidgets('AuditTrailScreen - Renderiza cabeçalho, filtros e tabela sem overflow em Desktop', (tester) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    final page = AuditLogPage(
      items: sampleLogs,
      totalElements: 2,
      totalPages: 1,
      currentPage: 0,
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          auditLogNotifierProvider.overrideWith((ref) => FakeAuditNotifier(page)),
          usersProvider.overrideWith((ref) => FakeUsersNotifier(sampleUsers)),
        ],
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const AuditTrailScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    // Cabeçalho e Título
    expect(find.text('Trilha de Auditoria'), findsOneWidget);
    expect(find.text('Trilha de Auditoria Forense'), findsNothing);
    expect(find.text('Atualizar'), findsOneWidget);

    // Filtros
    expect(find.text('Período:'), findsOneWidget);
    expect(find.text('Histórico Completo'), findsOneWidget);
    expect(find.text('Hoje'), findsOneWidget);

    // Linhas da Tabela
    expect(find.text('Mariana Financeiro'), findsOneWidget);
    expect(find.text('Contas a Receber / Faturas'), findsOneWidget);
    expect(find.text('Fatura Baixada / Paga'), findsOneWidget);

    expect(find.text('Carlos Admin'), findsOneWidget);
    expect(find.text('Gestão de Usuários / RBAC'), findsOneWidget);
    expect(find.text('Cargo/Permissão Alterado'), findsOneWidget);
  });

  testWidgets('AuditTrailScreen - Renderiza defensivamente sem overflow em Mobile compacto (400px)', (tester) async {
    tester.view.physicalSize = const Size(400, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    final page = AuditLogPage(
      items: sampleLogs,
      totalElements: 2,
      totalPages: 1,
      currentPage: 0,
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          auditLogNotifierProvider.overrideWith((ref) => FakeAuditNotifier(page)),
          usersProvider.overrideWith((ref) => FakeUsersNotifier(sampleUsers)),
        ],
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const AuditTrailScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Trilha de Auditoria'), findsOneWidget);
    expect(find.text('Trilha de Auditoria Forense'), findsNothing);
    expect(find.text('Mariana Financeiro'), findsOneWidget);
  });

  testWidgets('AuditTrailScreen - Exibe Empty State real e limpo quando não há registros', (tester) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    final emptyPage = AuditLogPage.empty();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          auditLogNotifierProvider.overrideWith((ref) => FakeAuditNotifier(emptyPage)),
          usersProvider.overrideWith((ref) => FakeUsersNotifier(sampleUsers)),
        ],
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const AuditTrailScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Nenhum registro de auditoria encontrado'), findsOneWidget);
  });

  testWidgets('AuditTrailScreen - Abre modal com título Detalhes do Evento ao clicar no botão de detalhes', (tester) async {
    tester.view.physicalSize = const Size(1920, 1080);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    final page = AuditLogPage(
      items: sampleLogs,
      totalElements: 2,
      totalPages: 1,
      currentPage: 0,
    );

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          auditLogNotifierProvider.overrideWith((ref) => FakeAuditNotifier(page)),
          usersProvider.overrideWith((ref) => FakeUsersNotifier(sampleUsers)),
        ],
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const AuditTrailScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    // Valida coluna Detalhes
    expect(find.text('Detalhes'), findsOneWidget);

    // Clica no primeiro botão de detalhes (ícone code)
    final detailButtons = find.byIcon(Icons.code);
    expect(detailButtons, findsNWidgets(2));
    await tester.ensureVisible(detailButtons.first);
    await tester.tap(detailButtons.first);
    await tester.pumpAndSettle();

    // Valida título sóbrio do modal
    expect(find.text('Detalhes do Evento'), findsOneWidget);
    expect(find.text('Dossiê de Auditoria Forense'), findsNothing);
    expect(find.text('Fechar'), findsOneWidget);

    // Fecha o modal
    await tester.tap(find.text('Fechar'));
    await tester.pumpAndSettle();
    expect(find.text('Detalhes do Evento'), findsNothing);
  });
}
