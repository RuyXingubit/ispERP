/// Modelo de dados para resultados de busca de assinantes no ispERP.
class CustomerSearchModel {
  final String id;
  final String name;
  final String cpf;
  final String? email;
  final String? phone;
  final String? address;
  final String? city;
  final String? state;
  final bool active;

  const CustomerSearchModel({
    required this.id,
    required this.name,
    required this.cpf,
    this.email,
    this.phone,
    this.address,
    this.city,
    this.state,
    this.active = true,
  });

  factory CustomerSearchModel.fromJson(Map<String, dynamic> json) {
    return CustomerSearchModel(
      id: json['id']?.toString() ?? '',
      name: json['name']?.toString() ?? '',
      cpf: json['cpf']?.toString() ?? '',
      email: json['email']?.toString(),
      phone: json['phone']?.toString(),
      address: json['address']?.toString(),
      city: json['city']?.toString(),
      state: json['state']?.toString(),
      active: json['active'] == true,
    );
  }

  /// Retorna o CPF formatado em padrão 000.000.000-00 se válido.
  String get formattedCpf {
    final digits = cpf.replaceAll(RegExp(r'\D'), '');
    if (digits.length == 11) {
      return '${digits.substring(0, 3)}.${digits.substring(3, 6)}.${digits.substring(6, 9)}-${digits.substring(9)}';
    }
    return cpf;
  }
}
