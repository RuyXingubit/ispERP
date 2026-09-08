enum NasVendorType {
  mikrotik('MIKROTIK', 'MikroTik (RouterOS / CHR)'),
  huawei('HUAWEI', 'Huawei (BRAS / BNG / NE)'),
  cisco('CISCO', 'Cisco IOS-XE / XR'),
  juniper('JUNIPER', 'Juniper JunOS'),
  other('OTHER', 'Outro / Padrão RFC 2865');

  final String value;
  final String label;
  const NasVendorType(this.value, this.label);

  static NasVendorType fromString(String? val) {
    if (val == null) return NasVendorType.mikrotik;
    return NasVendorType.values.firstWhere(
      (e) => e.value.toUpperCase() == val.toUpperCase() || e.name.toUpperCase() == val.toUpperCase(),
      orElse: () => NasVendorType.mikrotik,
    );
  }
}

class NasModel {
  final String? id;
  final String? companyId;
  final String nasname;
  final String shortname;
  final String type;
  final int ports;
  final String secret;
  final String? server;
  final String? community;
  final String? description;
  final NasVendorType vendorType;
  final DateTime? createdAt;
  final DateTime? updatedAt;

  NasModel({
    this.id,
    this.companyId,
    required this.nasname,
    required this.shortname,
    this.type = 'other',
    this.ports = 1812,
    required this.secret,
    this.server,
    this.community,
    this.description,
    this.vendorType = NasVendorType.mikrotik,
    this.createdAt,
    this.updatedAt,
  });

  factory NasModel.fromJson(Map<String, dynamic> json) {
    return NasModel(
      id: json['id']?.toString(),
      companyId: json['companyId']?.toString(),
      nasname: json['nasname']?.toString() ?? '',
      shortname: json['shortname']?.toString() ?? '',
      type: json['type']?.toString() ?? 'other',
      ports: json['ports'] is int ? json['ports'] as int : int.tryParse(json['ports']?.toString() ?? '1812') ?? 1812,
      secret: json['secret']?.toString() ?? '',
      server: json['server']?.toString(),
      community: json['community']?.toString(),
      description: json['description']?.toString(),
      vendorType: NasVendorType.fromString(json['vendorType']?.toString()),
      createdAt: json['createdAt'] != null ? DateTime.tryParse(json['createdAt'].toString()) : null,
      updatedAt: json['updatedAt'] != null ? DateTime.tryParse(json['updatedAt'].toString()) : null,
    );
  }

  Map<String, dynamic> toJson() {
    return {
      if (id != null) 'id': id,
      if (companyId != null) 'companyId': companyId,
      'nasname': nasname,
      'shortname': shortname,
      'type': type,
      'ports': ports,
      'secret': secret,
      if (server != null && server!.isNotEmpty) 'server': server,
      if (community != null && community!.isNotEmpty) 'community': community,
      if (description != null && description!.isNotEmpty) 'description': description,
      'vendorType': vendorType.value,
    };
  }

  NasModel copyWith({
    String? id,
    String? companyId,
    String? nasname,
    String? shortname,
    String? type,
    int? ports,
    String? secret,
    String? server,
    String? community,
    String? description,
    NasVendorType? vendorType,
  }) {
    return NasModel(
      id: id ?? this.id,
      companyId: companyId ?? this.companyId,
      nasname: nasname ?? this.nasname,
      shortname: shortname ?? this.shortname,
      type: type ?? this.type,
      ports: ports ?? this.ports,
      secret: secret ?? this.secret,
      server: server ?? this.server,
      community: community ?? this.community,
      description: description ?? this.description,
      vendorType: vendorType ?? this.vendorType,
      createdAt: createdAt,
      updatedAt: updatedAt,
    );
  }
}
