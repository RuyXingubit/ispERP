import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/server_setup/data/initial_setup_repository.dart';
import 'package:isperp_app/features/admin/data/payment_gateway_model.dart';
import 'package:isperp_app/features/admin/data/nas_model.dart';

void main() {
  group('InitialSetupData Unit Tests', () {
    test('Serialização para JSON gera payload correto para POST /initial-setup', () {
      final setupData = InitialSetupData(
        companyName: 'Nexus Provedor Telecom LTDA',
        companyCnpj: '12.345.678/0001-90',
        companyPhone: '11999999999',
        companyEmail: 'contato@nexusprovedor.com.br',
        companyAddress: 'Av. Paulista, 1000',
        companyWebsite: 'https://nexusprovedor.com.br',
        adminName: 'Admin Mestre',
        adminEmail: 'admin@nexusprovedor.com.br',
        adminPassword: 'SuperSecretPassword123!',
      );

      final json = setupData.toJson();

      expect(json['companyName'], 'Nexus Provedor Telecom LTDA');
      expect(json['companyCnpj'], '12.345.678/0001-90');
      expect(json['companyPhone'], '11999999999');
      expect(json['companyEmail'], 'contato@nexusprovedor.com.br');
      expect(json['adminName'], 'Admin Mestre');
      expect(json['adminEmail'], 'admin@nexusprovedor.com.br');
      expect(json['adminPassword'], 'SuperSecretPassword123!');
    });
  });

  group('PaymentGatewayModel Unit Tests', () {
    test('PaymentGatewayType converte strings legadas e enum com fallback seguro', () {
      expect(PaymentGatewayType.fromString('XINGUBIT_PAY'), PaymentGatewayType.xingubitPay);
      expect(PaymentGatewayType.fromString('xingubitPay'), PaymentGatewayType.xingubitPay);
      expect(PaymentGatewayType.fromString('MERCADOPAGO'), PaymentGatewayType.mercadopago);
      expect(PaymentGatewayType.fromString('INVALIDO'), PaymentGatewayType.xingubitPay);
    });

    test('Serialização e deserialização do Xingubit Pay mantém integridade das credenciais', () {
      final model = PaymentGatewayModel(
        id: 'cfg-001',
        companyId: 'comp-001',
        gatewayType: PaymentGatewayType.xingubitPay,
        name: 'Xingubit Pay Principal',
        apiKey: 'xb_live_pk_test_123',
        secretKey: 'xb_live_sk_test_456',
        webhookSecret: 'whsec_789',
        pixKey: '12.345.678/0001-90',
        sandbox: false,
        active: true,
      );

      final json = model.toJson();
      expect(json['gatewayType'], 'XINGUBIT_PAY');
      expect(json['apiKey'], 'xb_live_pk_test_123');
      expect(json['secretKey'], 'xb_live_sk_test_456');
      expect(json['pixKey'], '12.345.678/0001-90');
      expect(json['sandbox'], false);
      expect(json['active'], true);

      final deserialized = PaymentGatewayModel.fromJson(json);
      expect(deserialized.gatewayType, PaymentGatewayType.xingubitPay);
      expect(deserialized.apiKey, 'xb_live_pk_test_123');
      expect(deserialized.pixKey, '12.345.678/0001-90');
      expect(deserialized.sandbox, false);
      expect(deserialized.active, true);
    });
  });

  group('NasModel Unit Tests (MikroTik & Huawei)', () {
    test('NasVendorType identifica MikroTik e Huawei corretamente', () {
      expect(NasVendorType.fromString('MIKROTIK'), NasVendorType.mikrotik);
      expect(NasVendorType.fromString('HUAWEI'), NasVendorType.huawei);
      expect(NasVendorType.fromString('CISCO'), NasVendorType.cisco);
      expect(NasVendorType.fromString('JUNIPER'), NasVendorType.juniper);
      expect(NasVendorType.fromString('DESCONHECIDO'), NasVendorType.mikrotik);
    });

    test('Serialização do concentrador MikroTik para FreeRADIUS', () {
      final mikrotikNas = NasModel(
        id: 'nas-mkt-01',
        nasname: '192.168.88.1',
        shortname: 'bng-mikrotik-core',
        secret: 'radiussecret123',
        ports: 1812,
        vendorType: NasVendorType.mikrotik,
        description: 'POP Centro - MikroTik CHR 10G',
      );

      final json = mikrotikNas.toJson();
      expect(json['nasname'], '192.168.88.1');
      expect(json['shortname'], 'bng-mikrotik-core');
      expect(json['secret'], 'radiussecret123');
      expect(json['ports'], 1812);
      expect(json['vendorType'], 'MIKROTIK');

      final fromJson = NasModel.fromJson(json);
      expect(fromJson.vendorType, NasVendorType.mikrotik);
      expect(fromJson.nasname, '192.168.88.1');
      expect(fromJson.shortname, 'bng-mikrotik-core');
    });

    test('Serialização do concentrador Huawei BRAS para FreeRADIUS', () {
      final huaweiNas = NasModel(
        id: 'nas-hw-01',
        nasname: '10.0.0.1',
        shortname: 'huawei-bras-pop01',
        secret: 'huaweisecret999',
        ports: 1812,
        vendorType: NasVendorType.huawei,
        description: 'POP Norte - Huawei NE40E',
      );

      final json = huaweiNas.toJson();
      expect(json['vendorType'], 'HUAWEI');
      expect(json['nasname'], '10.0.0.1');

      final fromJson = NasModel.fromJson(json);
      expect(fromJson.vendorType, NasVendorType.huawei);
      expect(fromJson.nasname, '10.0.0.1');
    });
  });
}
