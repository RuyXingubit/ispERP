import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/main.dart';
import 'package:shared_preferences/shared_preferences.dart';

void main() {
  testWidgets('App smoke test - Deve inicializar na tela de conexão com servidor', (WidgetTester tester) async {
    SharedPreferences.setMockInitialValues({});
    final prefs = await SharedPreferences.getInstance();

    await tester.pumpWidget(
      ProviderScope(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
        child: const IspErpApp(),
      ),
    );

    await tester.pump();
    await tester.pump(const Duration(milliseconds: 200));

    // Deve exibir o título do app e a tela inicial de setup do servidor
    expect(find.text('ispERP'), findsWidgets);
    expect(find.text('Conexão com Servidor do Provedor'), findsOneWidget);
    expect(find.text('Testar Conexão'), findsOneWidget);
    expect(find.text('Conectar'), findsOneWidget);
  });
}
