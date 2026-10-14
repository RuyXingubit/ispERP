import 'package:dio/dio.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/providers/app_providers.dart';
import 'helpdesk_models.dart';

/// Repositório API-First para a Esteira de Chamados N1 / N2 e Protocolo Anatel (`/helpdesk/tickets`).
class HelpdeskRepository {
  final Dio _dio;

  HelpdeskRepository(this._dio);

  /// Lista todos os chamados de helpdesk (`GET /helpdesk/tickets`).
  Future<List<HelpdeskTicket>> getAllTickets() async {
    final response = await _dio.get('/helpdesk/tickets');
    if (response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(HelpdeskTicket.fromJson)
          .toList();
    }
    return const [];
  }

  /// Obtém um chamado pelo ID (`GET /helpdesk/tickets/{id}`).
  Future<HelpdeskTicket?> getTicketById(String id) async {
    final response = await _dio.get('/helpdesk/tickets/$id');
    if (response.data is Map<String, dynamic>) {
      return HelpdeskTicket.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Busca um chamado pelo Protocolo Anatel (`GET /helpdesk/tickets/protocol/{protocol}`).
  Future<HelpdeskTicket?> getTicketByProtocol(String protocol) async {
    final cleanProtocol = protocol.trim();
    if (cleanProtocol.isEmpty) return null;
    final response = await _dio.get('/helpdesk/tickets/protocol/$cleanProtocol');
    if (response.data is Map<String, dynamic>) {
      return HelpdeskTicket.fromJson(response.data as Map<String, dynamic>);
    }
    return null;
  }

  /// Lista o histórico de chamados de um assinante (`GET /helpdesk/tickets/customer/{customerId}`).
  Future<List<HelpdeskTicket>> getTicketsByCustomer(String customerId) async {
    final response = await _dio.get('/helpdesk/tickets/customer/$customerId');
    if (response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(HelpdeskTicket.fromJson)
          .toList();
    }
    return const [];
  }

  /// Abre um novo chamado gerando Protocolo Anatel e SLA automático (`POST /helpdesk/tickets`).
  Future<HelpdeskTicket> createTicket({
    required String customerId,
    String? contractId,
    required String category,
    String priority = 'NORMAL',
    String channel = 'PHONE',
    required String subject,
    required String description,
    String? attendantUserId,
    String? attendantName,
  }) async {
    final response = await _dio.post(
      '/helpdesk/tickets',
      data: {
        'customerId': customerId,
        'contractId': ?contractId,
        'category': category,
        'priority': priority,
        'channel': channel,
        'subject': subject,
        'description': description,
        'attendantUserId': ?attendantUserId,
        'attendantName': ?attendantName,
      },
    );
    return HelpdeskTicket.fromJson(response.data as Map<String, dynamic>);
  }

  /// Escalona o chamado do N1 para o Suporte Avançado N2 / NOC (`POST /helpdesk/tickets/{id}/escalate-n2`).
  Future<HelpdeskTicket> escalateToN2({
    required String ticketId,
    String? attendantUserId,
    required String attendantName,
    required String reason,
  }) async {
    final response = await _dio.post(
      '/helpdesk/tickets/$ticketId/escalate-n2',
      data: {
        'attendantUserId': ?attendantUserId,
        'attendantName': attendantName,
        'reason': reason,
      },
    );
    return HelpdeskTicket.fromJson(response.data as Map<String, dynamic>);
  }

  /// Resolve o chamado remotamente pelo N2 / NOC (`POST /helpdesk/tickets/{id}/resolve-n2`).
  Future<HelpdeskTicket> resolveByN2({
    required String ticketId,
    String? n2UserId,
    required String n2Name,
    required String resolutionNotes,
  }) async {
    final response = await _dio.post(
      '/helpdesk/tickets/$ticketId/resolve-n2',
      data: {
        'n2UserId': ?n2UserId,
        'n2Name': n2Name,
        'resolutionNotes': resolutionNotes,
      },
    );
    return HelpdeskTicket.fromJson(response.data as Map<String, dynamic>);
  }

  /// Escalona o chamado do N2 para uma Ordem de Serviço de Campo (`POST /helpdesk/tickets/{id}/escalate-work-order`).
  Future<Map<String, dynamic>> escalateToWorkOrder({
    required String ticketId,
    String? n2UserId,
    required String n2Name,
    required String technicalReason,
  }) async {
    final response = await _dio.post(
      '/helpdesk/tickets/$ticketId/escalate-work-order',
      data: {
        'n2UserId': ?n2UserId,
        'n2Name': n2Name,
        'technicalReason': technicalReason,
      },
    );
    if (response.data is Map<String, dynamic>) {
      return response.data as Map<String, dynamic>;
    }
    return const {};
  }

  /// Lista a linha do tempo de interações e notas técnicas do chamado (`GET /helpdesk/tickets/{id}/interactions`).
  Future<List<TicketInteraction>> getInteractions(
    String ticketId, {
    bool includeInternal = true,
  }) async {
    final response = await _dio.get(
      '/helpdesk/tickets/$ticketId/interactions',
      queryParameters: {'includeInternal': includeInternal},
    );
    if (response.data is List) {
      return (response.data as List)
          .whereType<Map<String, dynamic>>()
          .map(TicketInteraction.fromJson)
          .toList();
    }
    return const [];
  }

  /// Adiciona uma mensagem ou nota interna na linha do tempo (`POST /helpdesk/tickets/{id}/interactions`).
  Future<TicketInteraction> addInteraction({
    required String ticketId,
    String? userId,
    required String senderType,
    required String senderName,
    required String message,
    bool isInternalNote = false,
    String? attachmentUrl,
  }) async {
    final response = await _dio.post(
      '/helpdesk/tickets/$ticketId/interactions',
      data: {
        'userId': ?userId,
        'senderType': senderType,
        'senderName': senderName,
        'message': message,
        'isInternalNote': isInternalNote,
        'attachmentUrl': ?attachmentUrl,
      },
    );
    return TicketInteraction.fromJson(response.data as Map<String, dynamic>);
  }

  /// Encerra definitivamente o chamado com nota de satisfação CSAT (`POST /helpdesk/tickets/{id}/close`).
  Future<HelpdeskTicket> closeTicket({
    required String ticketId,
    int? satisfactionRating,
    String? closureNotes,
  }) async {
    final response = await _dio.post(
      '/helpdesk/tickets/$ticketId/close',
      data: {
        'satisfactionRating': ?satisfactionRating,
        'closureNotes': ?closureNotes,
      },
    );
    return HelpdeskTicket.fromJson(response.data as Map<String, dynamic>);
  }
}

/// Provider do repositório de Helpdesk Anatel.
final helpdeskRepositoryProvider = Provider<HelpdeskRepository>((ref) {
  final dio = ref.watch(apiClientProvider).dio;
  return HelpdeskRepository(dio);
});

/// Provider que lista todos os chamados de Helpdesk (`GET /helpdesk/tickets`).
final helpdeskTicketsProvider = FutureProvider<List<HelpdeskTicket>>((ref) async {
  final repo = ref.watch(helpdeskRepositoryProvider);
  return repo.getAllTickets();
});

/// Provider que lista os chamados Anatel de um assinante específico (`GET /helpdesk/tickets/customer/{customerId}`).
final customerTicketsProvider =
    FutureProvider.family<List<HelpdeskTicket>, String>((ref, customerId) async {
  if (customerId.trim().isEmpty) return const [];
  final repo = ref.watch(helpdeskRepositoryProvider);
  return repo.getTicketsByCustomer(customerId);
});

/// Provider que carrega a linha do tempo de interações de um chamado (`GET /helpdesk/tickets/{id}/interactions`).
final ticketInteractionsProvider =
    FutureProvider.family<List<TicketInteraction>, String>((ref, ticketId) async {
  if (ticketId.trim().isEmpty) return const [];
  final repo = ref.watch(helpdeskRepositoryProvider);
  return repo.getInteractions(ticketId, includeInternal: true);
});
