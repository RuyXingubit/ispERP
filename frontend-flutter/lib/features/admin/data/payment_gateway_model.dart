enum PaymentGatewayType {
  xingubitPay('XINGUBIT_PAY', 'Xingubit Pay'),
  mercadopago('MERCADOPAGO', 'Mercado Pago'),
  gerencianet('GERENCIANET', 'Efí (Gerencianet)'),
  asaas('ASAAS', 'Asaas');

  final String value;
  final String label;
  const PaymentGatewayType(this.value, this.label);

  static PaymentGatewayType fromString(String? val) {
    if (val == null) return PaymentGatewayType.xingubitPay;
    return PaymentGatewayType.values.firstWhere(
      (e) => e.value.toUpperCase() == val.toUpperCase() || e.name.toUpperCase() == val.toUpperCase(),
      orElse: () => PaymentGatewayType.xingubitPay,
    );
  }
}

class PaymentGatewayModel {
  final String? id;
  final String? companyId;
  final PaymentGatewayType gatewayType;
  final String name;
  final String? apiKey;
  final String? secretKey;
  final String? webhookSecret;
  final String? pixKey;
  final bool sandbox;
  final bool active;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  PaymentGatewayModel({
    this.id,
    this.companyId,
    required this.gatewayType,
    required this.name,
    this.apiKey,
    this.secretKey,
    this.webhookSecret,
    this.pixKey,
    this.sandbox = false,
    this.active = true,
    this.createdAt,
    this.updatedAt,
  });

  factory PaymentGatewayModel.fromJson(Map<String, dynamic> json) {
    return PaymentGatewayModel(
      id: json['id']?.toString(),
      companyId: json['companyId']?.toString(),
      gatewayType: PaymentGatewayType.fromString(json['gatewayType']?.toString()),
      name: json['name']?.toString() ?? 'Xingubit Pay',
      apiKey: json['apiKey']?.toString(),
      secretKey: json['secretKey']?.toString(),
      webhookSecret: json['webhookSecret']?.toString(),
      pixKey: json['pixKey']?.toString(),
      sandbox: json['sandbox'] == true,
      active: json['active'] != false,
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'].toString()) : null,
      updatedAt: json['updatedAt'] != null ? DateTime.tryParse(json['updatedAt'].toString()) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      if (companyId != null) 'companyId': companyId,
      'gatewayType': gatewayType.value,
      'name': name,
      if (apiKey != null) 'apiKey': apiKey,
      if (secretKey != null) 'secretKey': secretKey,
      if (webhookSecret != null) 'webhookSecret': webhookSecret,
      if (pixKey != null) 'pixKey': pixKey,
      'sandbox': sandbox,
      'active': active,
    };
  }

  PaymentGatewayModel copyWith({
    String? id,
    String? companyId,
    PaymentGatewayType? gatewayType,
    String? name,
    String? apiKey,
    String? secretKey,
    String? webhookSecret,
    String? pixKey,
    bool? sandbox,
    bool? active,
  }) {
    return PaymentGatewayModel(
      id: id ?? this.id,
      companyId: companyId ?? this.companyId,
      gatewayType: gatewayType ?? this.gatewayType,
      name: name ?? this.name,
      apiKey: apiKey ?? this.apiKey,
      secretKey: secretKey ?? this.secretKey,
      webhookSecret: webhookSecret ?? this.webhookSecret,
      pixKey: pixKey ?? this.pixKey,
      sandbox: sandbox ?? this.sandbox,
      active: active ?? this.active,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }
}
