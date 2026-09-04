import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/models/user_role.dart';

void main() {
  group('UserRole Enum & RBAC Tests', () {
    test('Deve converter strings em maiúsculas e com prefixo ROLE_ corretamente', () {
      expect(UserRole.fromString('ROLE_ADMIN'), UserRole.admin);
      expect(UserRole.fromString('admin'), UserRole.admin);
      expect(UserRole.fromString('ADMINISTRATOR'), UserRole.admin);

      expect(UserRole.fromString('ROLE_FINANCIAL'), UserRole.financial);
      expect(UserRole.fromString('FINANCE'), UserRole.financial);
      expect(UserRole.fromString('ADMINISTRATIVO'), UserRole.financial);

      expect(UserRole.fromString('ROLE_SUPPORT'), UserRole.support);
      expect(UserRole.fromString('ATENDIMENTO'), UserRole.support);
      expect(UserRole.fromString('HELPDESK'), UserRole.support);

      expect(UserRole.fromString('ROLE_SALES'), UserRole.sales);
      expect(UserRole.fromString('COMERCIAL'), UserRole.sales);
      expect(UserRole.fromString('VENDAS'), UserRole.sales);

      expect(UserRole.fromString('ROLE_TECHNICIAN'), UserRole.technician);
      expect(UserRole.fromString('TECNICO'), UserRole.technician);
      expect(UserRole.fromString('FIELD_TECH'), UserRole.technician);
    });

    test('Deve retornar UserRole.support como fallback para valores nulos ou desconhecidos', () {
      expect(UserRole.fromString(null), UserRole.support);
      expect(UserRole.fromString(''), UserRole.support);
      expect(UserRole.fromString('UNKNOWN_ROLE_123'), UserRole.support);
    });

    test('Deve retornar a rota inicial correta por perfil', () {
      expect(UserRole.admin.initialRoute, '/admin');
      expect(UserRole.financial.initialRoute, '/financial');
      expect(UserRole.support.initialRoute, '/support');
      expect(UserRole.sales.initialRoute, '/sales');
      expect(UserRole.technician.initialRoute, '/technician');
    });

    test('Deve fornecer nomes amigáveis para todos os perfis', () {
      for (final role in UserRole.values) {
        expect(role.displayName, isNotEmpty);
      }
    });
  });
}
