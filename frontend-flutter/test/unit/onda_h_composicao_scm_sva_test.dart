import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/admin/data/catalog_repository.dart';
import 'package:isperp_app/features/financial/data/fiscal_repository.dart';

void main() {
  group('Onda H — Composição Multi-Item SCM + SVA / Locação & NFCom Mod. 62', () {
    test('AdminPlanModel parses compositionItems and computes scmAmount / svaAmount', () {
      final json = {
        'id': '0195f3a0-0000-7000-8000-000000000010',
        'name': 'Fibra 600 Mega Combo',
        'downloadSpeed': 600,
        'uploadSpeed': 300,
        'price': 99.90,
        'scmAmount': 39.90,
        'svaAmount': 60.00,
        'compositionItems': [
          {
            'id': '0195f3a0-0000-7000-8000-000000000011',
            'name': 'Acesso SCM Fibra',
            'category': 'SCM',
            'amount': 39.90,
            'fiscalCode': '5307',
          },
          {
            'id': '0195f3a0-0000-7000-8000-000000000012',
            'name': 'Locação ONT Wi-Fi 6',
            'category': 'EQUIPMENT_RENTAL',
            'amount': 30.00,
          },
          {
            'id': '0195f3a0-0000-7000-8000-000000000013',
            'name': 'Biblioteca E-Books',
            'category': 'SVA_DIGITAL_CONTENT',
            'amount': 20.00,
          },
          {
            'id': '0195f3a0-0000-7000-8000-000000000014',
            'name': 'Streaming TV',
            'category': 'SVA_STREAMING_APP',
            'amount': 10.00,
          },
        ],
      };

      final plan = AdminPlanModel.fromJson(json);
      expect(plan.price, 99.90);
      expect(plan.scmAmount, 39.90);
      expect(plan.svaAmount, 60.00);
      expect(plan.compositionItems.length, 4);
      expect(plan.compositionItems[0].isScm, isTrue);
      expect(plan.compositionItems[1].isScm, isFalse);
      expect(
        plan.compositionItems[1].categoryLabel,
        contains('Locação'),
      );
    });

    test('PlanSavePayload serializes compositionItems to JSON', () {
      const payload = PlanSavePayload(
        name: 'Fibra 600 Mega Combo',
        downloadSpeed: 600,
        uploadSpeed: 300,
        price: 99.90,
        compositionItems: [
          PlanCompositionItemModel(
            name: 'Acesso SCM',
            category: 'SCM',
            amount: 49.90,
            fiscalCode: '5307',
          ),
          PlanCompositionItemModel(
            name: 'Locação Roteador Mesh',
            category: 'EQUIPMENT_RENTAL',
            amount: 50.00,
          ),
        ],
      );

      final map = payload.toJson();
      expect(map['price'], 99.90);
      final items = map['compositionItems'] as List<dynamic>;
      expect(items.length, 2);
      expect(items[0]['category'], 'SCM');
      expect(items[0]['amount'], 49.90);
      expect(items[1]['category'], 'EQUIPMENT_RENTAL');
      expect(items[1]['amount'], 50.00);
    });

    test('NfcomRecordModel parses scmTaxableAmount and svaExemptAmount', () {
      final json = {
        'id': '0195f3a0-0000-7000-8000-000000000099',
        'companyId': '0195f3a0-0000-7000-8000-000000000001',
        'customerId': '0195f3a0-0000-7000-8000-000000000002',
        'numero': 1025,
        'serie': '1',
        'modelo': '62',
        'tipoEmissao': 'NORMAL',
        'ambiente': 'HOMOLOGACAO',
        'status': 'AUTORIZADA',
        'valorTotal': 39.90,
        'scmTaxableAmount': 39.90,
        'svaExemptAmount': 60.00,
        'valorIcms': 7.18,
        'valorFust': 0.40,
        'valorFunttel': 0.20,
      };

      final rec = NfcomRecordModel.fromJson(json);
      expect(rec.valorTotal, 39.90);
      expect(rec.scmTaxableAmount, 39.90);
      expect(rec.svaExemptAmount, 60.00);
      expect(rec.valorIcms, 7.18);
    });
  });
}
