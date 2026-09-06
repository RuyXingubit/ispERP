import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/admin/data/user_model.dart';
import 'package:isperp_app/features/admin/data/users_notifier.dart';

void main() {
  group('UserModel Unit Tests', () {
    test('Deve desserializar JSON com dados completos de colaborador', () {
      final json = {
        'id': '0192e123-4567-7890-abcd-ef1234567890',
        'name': 'Carlos Alberto Silva',
        'email': 'carlos.silva@nexusfibra.com.br',
        'role': 'SUPPORT_N2',
        'cpf': '123.456.789-00',
        'active': true,
        'createdAt': '2026-09-01T10:00:00Z',
        'updatedAt': '2026-09-05T12:00:00Z',
      };

      final user = UserModel.fromJson(json);

      expect(user.id, equals('0192e123-4567-7890-abcd-ef1234567890'));
      expect(user.name, equals('Carlos Alberto Silva'));
      expect(user.email, equals('carlos.silva@nexusfibra.com.br'));
      expect(user.role, equals('SUPPORT_N2'));
      expect(user.roleDisplay, equals('Suporte N2 (NOC)'));
      expect(user.cpf, equals('123.456.789-00'));
      expect(user.active, isTrue);
      expect(user.initials, equals('CS'));
    });

    test('Deve gerar iniciais corretas para nomes simples e múltiplos', () {
      const user1 = UserModel(id: '1', name: 'Admin', email: 'a@a.com', role: 'ADMIN');
      expect(user1.initials, equals('A'));

      const user2 = UserModel(id: '2', name: 'Roberto Silveira Junior', email: 'r@a.com', role: 'FINANCIAL');
      expect(user2.initials, equals('RJ'));

      const user3 = UserModel(id: '3', name: '', email: 'x@a.com', role: 'USER');
      expect(user3.initials, equals('U'));
    });

    test('Deve mapear roleDisplay para todos os perfis canônicos', () {
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'ADMIN').roleDisplay, equals('Administrador'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'DIRECTOR').roleDisplay, equals('Diretoria'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'CFO').roleDisplay, equals('CFO / Diretor Financeiro'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'FINANCIAL').roleDisplay, equals('Financeiro & Contas'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'ATTENDANT').roleDisplay, equals('Atendimento / SAC'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'ADMINISTRATIVE_ASSISTANT').roleDisplay, equals('Assistente Administrativo'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'SUPPORT_N2').roleDisplay, equals('Suporte N2 (NOC)'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'SUPPORT_ANALYST').roleDisplay, equals('Analista de Suporte'));
      expect(const UserModel(id: '1', name: 'N', email: 'e', role: 'TECHNICIAN').roleDisplay, equals('Técnico de Campo'));
    });

    test('Deve serializar para JSON de forma compatível', () {
      final user = UserModel(
        id: '123',
        name: 'Teste',
        email: 't@t.com',
        role: 'ADMIN',
        cpf: '11122233344',
        active: true,
        createdAt: DateTime.parse('2026-09-01T10:00:00.000Z'),
      );

      final json = user.toJson();
      expect(json['id'], equals('123'));
      expect(json['name'], equals('Teste'));
      expect(json['email'], equals('t@t.com'));
      expect(json['role'], equals('ADMIN'));
      expect(json['active'], isTrue);
    });
  });

  group('UsersState Filtering & Metrics Unit Tests', () {
    final sampleUsers = [
      const UserModel(id: '1', name: 'Alice Admin', email: 'alice@nexus.com', role: 'ADMIN', active: true),
      const UserModel(id: '2', name: 'Bruno Financeiro', email: 'bruno@nexus.com', role: 'FINANCIAL', active: true),
      const UserModel(id: '3', name: 'Carlos Suporte', email: 'carlos@nexus.com', role: 'SUPPORT_N2', active: false),
      const UserModel(id: '4', name: 'Daniel Tecnico', email: 'daniel@nexus.com', role: 'TECHNICIAN', active: true),
    ];

    test('Deve calcular métricas de ativos e suspensos corretamente', () {
      final state = UsersState(users: sampleUsers);

      expect(state.totalActive, equals(3));
      expect(state.totalSuspended, equals(1));
      expect(state.filteredUsers.length, equals(4));
    });

    test('Deve filtrar colaboradores por status ACTIVE', () {
      final state = UsersState(users: sampleUsers, statusFilter: 'ACTIVE');

      final filtered = state.filteredUsers;
      expect(filtered.length, equals(3));
      expect(filtered.every((u) => u.active), isTrue);
    });

    test('Deve filtrar colaboradores por status SUSPENDED', () {
      final state = UsersState(users: sampleUsers, statusFilter: 'SUSPENDED');

      final filtered = state.filteredUsers;
      expect(filtered.length, equals(1));
      expect(filtered.first.name, equals('Carlos Suporte'));
    });

    test('Deve filtrar colaboradores por busca textual de nome ou email', () {
      final state = UsersState(users: sampleUsers, searchQuery: 'bruno');

      final filtered = state.filteredUsers;
      expect(filtered.length, equals(1));
      expect(filtered.first.email, equals('bruno@nexus.com'));
    });

    test('Deve filtrar colaboradores por cargo/role', () {
      final state = UsersState(users: sampleUsers, roleFilter: 'TECHNICIAN');

      final filtered = state.filteredUsers;
      expect(filtered.length, equals(1));
      expect(filtered.first.name, equals('Daniel Tecnico'));
    });
  });
}
