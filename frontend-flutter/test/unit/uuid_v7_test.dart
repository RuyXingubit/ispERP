import 'package:flutter_test/flutter_test.dart';
import 'package:uuid/uuid.dart';

void main() {
  group('UUID v7 Validation Tests (Mandatory Rule)', () {
    const uuid = Uuid();

    test('Deve gerar UUID na versão 7 com estrutura de timestamp monotônica', () {
      final id1 = uuid.v7();
      final id2 = uuid.v7();

      expect(id1, isNotEmpty);
      expect(id2, isNotEmpty);
      expect(id1, isNot(equals(id2)));

      // Regex padrão para UUID v7 (posição da versão deve conter o caractere '7')
      // Formato: xxxxxxxx-xxxx-7xxx-axxx-xxxxxxxxxxxx
      final uuidV7Regex = RegExp(
        r'^[0-9a-f]{8}-[0-9a-f]{4}-7[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$',
        caseSensitive: false,
      );

      expect(uuidV7Regex.hasMatch(id1), isTrue, reason: 'ID gerado deve ser UUIDv7: $id1');
      expect(uuidV7Regex.hasMatch(id2), isTrue, reason: 'ID gerado deve ser UUIDv7: $id2');
    });

    test('UUID v7 consecutivos devem ter ordenação cronológica/lexicográfica', () async {
      final idA = uuid.v7();
      await Future.delayed(const Duration(milliseconds: 5));
      final idB = uuid.v7();

      expect(idA.compareTo(idB), lessThan(0));
    });
  });
}
