/// Modelo de resposta da consulta de CEP via GeoCep API.
class CepLookupModel {
  final String cep;
  final String street;
  final String neighborhood;
  final String city;
  final String state;
  final double? latitude;
  final double? longitude;

  const CepLookupModel({
    required this.cep,
    required this.street,
    required this.neighborhood,
    required this.city,
    required this.state,
    this.latitude,
    this.longitude,
  });

  factory CepLookupModel.fromJson(Map<String, dynamic> json) {
    double? parseCoord(dynamic val) {
      if (val == null) return null;
      if (val is num) return val.toDouble();
      return double.tryParse(val.toString());
    }

    double? lat = parseCoord(json['latitude']);
    double? lon = parseCoord(json['longitude']);
    if ((lat == null || lon == null) && json['coordenadas'] is Map) {
      final coords = json['coordenadas'] as Map;
      lat ??= parseCoord(coords['latitude']);
      lon ??= parseCoord(coords['longitude']);
    }

    final street = json['logradouro']?.toString() ??
        json['street']?.toString() ??
        json['endereco']?.toString() ??
        '';

    final neighborhood = json['bairro']?.toString() ??
        json['neighborhood']?.toString() ??
        '';

    final city = json['cidade']?.toString() ??
        json['localidade']?.toString() ??
        json['city']?.toString() ??
        '';

    final state = json['uf']?.toString() ??
        json['state']?.toString() ??
        '';

    return CepLookupModel(
      cep: json['cep']?.toString() ?? '',
      street: street,
      neighborhood: neighborhood,
      city: city,
      state: state,
      latitude: lat,
      longitude: lon,
    );
  }
}

/// Item de CTO viável próxima ao endereço.
class FeasibleCtoItem {
  final String ctoName;
  final double distanceMeters;
  final int freePorts;
  final bool hasCapacity;

  const FeasibleCtoItem({
    required this.ctoName,
    required this.distanceMeters,
    required this.freePorts,
    required this.hasCapacity,
  });

  factory FeasibleCtoItem.fromJson(Map<String, dynamic> json) {
    final ctoObj = json['cto'];
    String name = 'CTO Desconhecida';
    if (ctoObj is Map<String, dynamic>) {
      name = ctoObj['name']?.toString() ?? ctoObj['code']?.toString() ?? name;
    }

    return FeasibleCtoItem(
      ctoName: name,
      distanceMeters: (json['distanceMeters'] as num?)?.toDouble() ?? 0.0,
      freePorts: (json['freePorts'] as num?)?.toInt() ?? 0,
      hasCapacity: json['hasCapacity'] == true,
    );
  }
}

/// Resposta do cálculo de viabilidade óptica FTTH.
class FtthFeasibilityModel {
  final bool viable;
  final int viableCtosCount;
  final List<FeasibleCtoItem> nearbyCtos;

  const FtthFeasibilityModel({
    required this.viable,
    required this.viableCtosCount,
    this.nearbyCtos = const [],
  });

  factory FtthFeasibilityModel.fromJson(Map<String, dynamic> json) {
    final rawList = json['nearbyCtos'];
    final items = <FeasibleCtoItem>[];
    if (rawList is List) {
      for (final item in rawList) {
        if (item is Map<String, dynamic>) {
          items.add(FeasibleCtoItem.fromJson(item));
        }
      }
    }

    return FtthFeasibilityModel(
      viable: json['viable'] == true,
      viableCtosCount: (json['viableCtosCount'] as num?)?.toInt() ?? 0,
      nearbyCtos: items,
    );
  }
}

/// Plano comercial ativo para contratação.
class CommercialPlan {
  final String id;
  final String name;
  final int downloadSpeed;
  final int uploadSpeed;
  final double price;
  final String? description;
  final String? svaIncluded;

  const CommercialPlan({
    required this.id,
    required this.name,
    required this.downloadSpeed,
    required this.uploadSpeed,
    required this.price,
    this.description,
    this.svaIncluded,
  });

  factory CommercialPlan.fromJson(Map<String, dynamic> json) {
    return CommercialPlan(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? 'Plano Sem Nome',
      downloadSpeed: (json['downloadSpeed'] as num?)?.toInt() ?? 0,
      uploadSpeed: (json['uploadSpeed'] as num?)?.toInt() ?? 0,
      price: (json['price'] as num?)?.toDouble() ?? 0.0,
      description: json['description']?.toString(),
      svaIncluded: json['svaIncluded']?.toString(),
    );
  }
}

/// Payload para submissão de nova venda expressa (POST /sales).
class CreateSalePayload {
  final String planId;
  final String customerName;
  final String customerCpf;
  final String? customerEmail;
  final String customerPhone;
  final String installationAddress;
  final String city;
  final String state;
  final String zipCode;
  final int preferredDueDate;
  final String notificationChannel;
  final String? sellerName;

  const CreateSalePayload({
    required this.planId,
    required this.customerName,
    required this.customerCpf,
    this.customerEmail,
    required this.customerPhone,
    required this.installationAddress,
    required this.city,
    required this.state,
    required this.zipCode,
    this.preferredDueDate = 10,
    this.notificationChannel = 'WHATSAPP',
    this.sellerName,
  });

  Map<String, dynamic> toJson() {
    return {
      'planId': planId,
      'customerName': customerName,
      'customerCpf': customerCpf,
      if (customerEmail != null && customerEmail!.isNotEmpty) 'customerEmail': customerEmail,
      'customerPhone': customerPhone,
      'installationAddress': installationAddress,
      'city': city,
      'state': state,
      'zipCode': zipCode,
      'preferredDueDate': preferredDueDate,
      'notificationChannel': notificationChannel,
      if (sellerName != null && sellerName!.isNotEmpty) 'sellerName': sellerName,
    };
  }
}

/// Resultado retornado pela API após a criação da venda.
class SaleResult {
  final String id;
  final String status;
  final String customerName;
  final String customerCpf;

  const SaleResult({
    required this.id,
    required this.status,
    required this.customerName,
    required this.customerCpf,
  });

  factory SaleResult.fromJson(Map<String, dynamic> json) {
    return SaleResult(
      id: json['id']?.toString() ?? '',
      status: json['status']?.toString() ?? 'SUBMITTED',
      customerName: json['customerName']?.toString() ?? '',
      customerCpf: json['customerCpf']?.toString() ?? '',
    );
  }
}

/// Validador e formatador matemático de CPF.
class CpfUtils {
  static String clean(String? cpf) {
    if (cpf == null) return '';
    return cpf.replaceAll(RegExp(r'\D'), '');
  }

  static String format(String? cpf) {
    final digits = clean(cpf);
    if (digits.length != 11) return digits;
    return '${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9, 11)}';
  }

  static String formatCep(String? cep) {
    final digits = clean(cep);
    if (digits.length != 8) return digits;
    return '${digits.substring(0, 5)}-${digits.substring(5, 8)}';
  }

  static bool isValid(String? cpf) {
    final cleanCpf = clean(cpf);
    if (cleanCpf.length != 11) return false;

    // Bloqueia CPFs com todos dígitos iguais (ex: 111.111.111-11)
    if (RegExp(r'^(\d)\1{10}$').hasMatch(cleanCpf)) return false;

    int calcDigit(int length) {
      var sum = 0;
      var weight = length + 1;
      for (var i = 0; i < length; i++) {
        sum += int.parse(cleanCpf[i]) * weight--;
      }
      final remainder = sum % 11;
      return remainder < 2 ? 0 : 11 - remainder;
    }

    final d1 = calcDigit(9);
    final d2 = calcDigit(10);

    return int.parse(cleanCpf[9]) == d1 && int.parse(cleanCpf[10]) == d2;
  }
}
