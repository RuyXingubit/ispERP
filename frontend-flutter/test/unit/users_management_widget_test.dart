import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/theme/app_theme.dart';
import 'package:isperp_app/features/admin/data/user_model.dart';
import 'package:isperp_app/features/admin/data/users_notifier.dart';
import 'package:isperp_app/features/admin/data/users_repository.dart';
import 'package:isperp_app/features/admin/presentation/users_management_screen.dart';

// Fake notifier que não dispara chamadas HTTP
class FakeUsersNotifier extends UsersNotifier {
  FakeUsersNotifier(List<UserModel> initialUsers)
      : super(_DummyUsersRepository()) {
    state = UsersState(isLoading: false, users: initialUsers);
  }
}

class _DummyUsersRepository implements UsersRepository {
  @override
  noSuchMethod(Invocation invocation) => super.noSuchMethod(invocation);
}

void main() {
  final sampleUsers = [
    const UserModel(
      id: '0192e123-4567-7890-abcd-ef1234567890',
      name: 'Roberto Silveira (CFO)',
      email: 'cfo@nexusfibra.com.br',
      role: 'CFO',
      cpf: '123.456.789-01',
      active: true,
    ),
    const UserModel(
      id: '0192e123-4567-7890-abcd-ef1234567891',
      name: 'Carlos Técnico',
      email: 'carlos@nexusfibra.com.br',
      role: 'TECHNICIAN',
      cpf: '987.654.321-00',
      active: false,
    ),
  ];

  testWidgets('UsersManagementScreen - Renderiza métricas, filtros e tabela sem overflow em Desktop', (tester) async {
    tester.view.physicalSize = const Size(1280, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          usersProvider.overrideWith((ref) => FakeUsersNotifier(sampleUsers)),
        ],
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const UsersManagementScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    // Cabeçalho e Título
    expect(find.text('Gestão de Colaboradores & RBAC'), findsOneWidget);
    expect(find.text('Novo Colaborador'), findsOneWidget);

    // Métricas
    expect(find.text('Total de Colaboradores'), findsOneWidget);
    expect(find.text('2'), findsOneWidget); // total
    expect(find.text('Acessos Ativos'), findsOneWidget);
    expect(find.text('1'), findsNWidgets(2)); // 1 ativo e 1 suspenso
    expect(find.text('Acessos Suspensos'), findsOneWidget);

    // Chips de Filtro
    expect(find.text('Todos'), findsOneWidget);
    expect(find.text('Ativos'), findsOneWidget);
    expect(find.text('Suspensos'), findsOneWidget);

    // Linhas da Tabela
    expect(find.text('Roberto Silveira (CFO)'), findsOneWidget);
    expect(find.text('cfo@nexusfibra.com.br'), findsOneWidget);
    expect(find.text('Carlos Técnico'), findsOneWidget);
    expect(find.text('carlos@nexusfibra.com.br'), findsOneWidget);
    expect(find.text('Ativo'), findsOneWidget);
    expect(find.text('Suspenso'), findsOneWidget);
  });

  testWidgets('UsersManagementScreen - Renderiza defensivamente sem overflow em Mobile compacto', (tester) async {
    tester.view.physicalSize = const Size(400, 800);
    tester.view.devicePixelRatio = 1.0;
    addTearDown(tester.view.resetPhysicalSize);

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          usersProvider.overrideWith((ref) => FakeUsersNotifier(sampleUsers)),
        ],
        child: MaterialApp(
          theme: AppTheme.darkTheme,
          home: const UsersManagementScreen(),
        ),
      ),
    );

    await tester.pumpAndSettle();

    expect(find.text('Gestão de Colaboradores & RBAC'), findsOneWidget);
    expect(find.text('Roberto Silveira (CFO)'), findsOneWidget);
    expect(find.text('Carlos Técnico'), findsOneWidget);
  });
}
