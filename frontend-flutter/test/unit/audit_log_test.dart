import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/admin/data/audit_log_model.dart';

void main() {
  group('AuditLogModel Tests', () {
    test('Deve desserializar JSON com sucesso e aplicar fallbacks padrão', () {
      final json = {
        'id': '0192e123-4567-7890-abcd-ef1234567890',
        'userId': '0192e123-4567-7890-abcd-ef1234567891',
        'userName': 'Mariana Financeiro',
        'userEmail': 'mariana@provedor.com.br',
        'action': 'INVOICE_PAID',
        'entityName': 'INVOICE',
        'entityId': 'inv-456',
        'details': '{"valor":"89.90","forma":"PIX"}',
        'createdAt': '2026-09-05T14:30:00.000Z',
      };

      final log = AuditLogModel.fromJson(json);

      expect(log.id, '0192e123-4567-7890-abcd-ef1234567890');
      expect(log.userId, '0192e123-4567-7890-abcd-ef1234567891');
      expect(log.userName, 'Mariana Financeiro');
      expect(log.userEmail, 'mariana@provedor.com.br');
      expect(log.action, 'INVOICE_PAID');
      expect(log.entityName, 'INVOICE');
      expect(log.entityId, 'inv-456');
      expect(log.moduleDisplay, 'Contas a Receber / Faturas');
      expect(log.actionDisplay, 'Fatura Baixada / Paga');
      expect(log.formattedDate, isNotEmpty);
    });

    test('Deve mapear módulos e ações desconhecidas de forma defensiva', () {
      final json = {
        'id': '0192e123-4567-7890-abcd-ef1234567892',
        'action': 'CUSTOM_AUDIT_EVENT',
        'entityName': 'SYSTEM_MODULE',
        'createdAt': '2026-09-05T10:00:00.000Z',
      };

      final log = AuditLogModel.fromJson(json);

      expect(log.userName, 'Sistema');
      expect(log.userEmail, 'sistema@isperp.local');
      expect(log.moduleDisplay, 'SYSTEM_MODULE');
      expect(log.actionDisplay, 'CUSTOM AUDIT EVENT');
    });

    test('AuditLogPage deve desserializar lista de itens e metadados de paginação', () {
      final pageJson = {
        'content': [
          {
            'id': '0192e123-4567-7890-abcd-ef1234567890',
            'action': 'FINANCIAL_DEBIT',
            'entityName': 'FINANCIAL',
            'createdAt': '2026-09-05T10:00:00.000Z',
          }
        ],
        'totalElements': 1,
        'totalPages': 1,
        'number': 0,
      };

      final page = AuditLogPage.fromJson(pageJson);

      expect(page.items.length, 1);
      expect(page.totalElements, 1);
      expect(page.totalPages, 1);
      expect(page.currentPage, 0);
      expect(page.items.first.action, 'FINANCIAL_DEBIT');
      expect(page.items.first.moduleDisplay, 'Financeiro / Lançamentos');
    });
  });
}
