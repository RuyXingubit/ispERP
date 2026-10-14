import 'dart:convert';
import 'dart:typed_data';
import 'package:dio/dio.dart';
import 'package:flutter/material.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:shared_preferences/shared_preferences.dart';
import 'package:isperp_app/core/providers/app_providers.dart';
import 'package:isperp_app/features/attendance/data/attendance_models.dart';
import 'package:isperp_app/features/attendance/data/attendance_notifier.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_model.dart';
import 'package:isperp_app/features/financial/data/dashboard_bi_provider.dart';
import 'package:isperp_app/features/support/data/helpdesk_models.dart';
import 'package:isperp_app/features/support/data/helpdesk_repository.dart';
import 'package:isperp_app/features/support/presentation/support_dashboard_screen.dart';
import 'package:isperp_app/features/support/presentation/ticket_detail_modal.dart';
import 'package:isperp_app/features/technician/data/technician_repository.dart';

class _MockHelpdeskAdapter implements HttpClientAdapter {
  final List<RequestOptions> requests = [];

  @override
  void close({bool force = false}) {}

  @override
  Future<ResponseBody> fetch(
    RequestOptions options,
    Stream<Uint8List>? requestStream,
    Future<void>? cancelFuture,
  ) async {
    requests.add(options);
    final path = options.path;
    final method = options.method.toUpperCase();

    if (method == 'GET' && path == '/helpdesk/tickets') {
      return _jsonResponse([
        {
          'id': '0195f000-0000-7000-8000-000000000001',
          'protocol': '2026101400001',
          'customerId': '0195f000-0000-7000-8000-000000000099',
          'category': 'CONNECTION_OUTAGE',
          'priority': 'URGENT',
          'status': 'OPEN',
          'channel': 'PHONE',
          'subject': 'Sem internet — LOS vermelho',
          'description': 'Cliente relata LED LOS piscando na ONU Huawei.',
          'slaDeadline': '2099-10-15T18:00:00Z',
          'createdAt': '2026-10-14T18:00:00Z',
        },
        {
          'id': '0195f000-0000-7000-8000-000000000002',
          'protocol': '2026101400002',
          'customerId': '0195f000-0000-7000-8000-000000000099',
          'category': 'SLOW_SPEED',
          'priority': 'HIGH',
          'status': 'IN_PROGRESS',
          'channel': 'WHATSAPP_BOT',
          'subject': 'Oscilação no período noturno',
          'description': 'Atenuação em -26.8 dBm verificada pelo N1.',
          'slaDeadline': '2099-10-16T18:00:00Z',
          'createdAt': '2026-10-14T18:10:00Z',
        },
      ]);
    }

    if (method == 'GET' && path == '/helpdesk/tickets/protocol/2026101400001') {
      return _jsonResponse({
        'id': '0195f000-0000-7000-8000-000000000001',
        'protocol': '2026101400001',
        'customerId': '0195f000-0000-7000-8000-000000000099',
        'category': 'CONNECTION_OUTAGE',
        'priority': 'URGENT',
        'status': 'OPEN',
        'channel': 'PHONE',
        'subject': 'Sem internet — LOS vermelho',
        'description': 'Cliente relata LED LOS piscando na ONU Huawei.',
        'slaDeadline': '2099-10-15T18:00:00Z',
        'createdAt': '2026-10-14T18:00:00Z',
      });
    }

    if (method == 'POST' && path == '/helpdesk/tickets') {
      return _jsonResponse(
        {
          'id': '0195f000-0000-7000-8000-000000000003',
          'protocol': '2026101400003',
          'customerId': '0195f000-0000-7000-8000-000000000099',
          'category': 'CONNECTION_OUTAGE',
          'priority': 'HIGH',
          'status': 'OPEN',
          'channel': 'IN_PERSON',
          'subject': 'Queda total',
          'description': 'Rompimento de drop na fachada.',
          'slaDeadline': '2099-10-15T19:00:00Z',
          'createdAt': '2026-10-14T19:00:00Z',
        },
        statusCode: 201,
      );
    }

    if (method == 'POST' && path.endsWith('/escalate-n2')) {
      return _jsonResponse({
        'id': '0195f000-0000-7000-8000-000000000001',
        'protocol': '2026101400001',
        'customerId': '0195f000-0000-7000-8000-000000000099',
        'category': 'CONNECTION_OUTAGE',
        'priority': 'URGENT',
        'status': 'IN_PROGRESS',
        'channel': 'PHONE',
        'subject': 'Sem internet — LOS vermelho',
        'description': 'Cliente relata LED LOS piscando na ONU Huawei.',
        'slaDeadline': '2099-10-15T18:00:00Z',
        'createdAt': '2026-10-14T18:00:00Z',
      });
    }

    if (method == 'POST' && path.endsWith('/resolve-n2')) {
      return _jsonResponse({
        'id': '0195f000-0000-7000-8000-000000000001',
        'protocol': '2026101400001',
        'customerId': '0195f000-0000-7000-8000-000000000099',
        'category': 'CONNECTION_OUTAGE',
        'priority': 'URGENT',
        'status': 'RESOLVED',
        'channel': 'PHONE',
        'subject': 'Sem internet — LOS vermelho',
        'description': 'Cliente relata LED LOS piscando na ONU Huawei.',
        'resolutionNotes': 'Reconfigurado profile VLAN na OLT.',
        'slaDeadline': '2099-10-15T18:00:00Z',
        'createdAt': '2026-10-14T18:00:00Z',
      });
    }

    if (method == 'POST' && path.endsWith('/escalate-work-order')) {
      return _jsonResponse(
        {
          'id': '0195f000-0000-7000-8000-000000000077',
          'type': 'MANUTENCAO',
          'status': 'OPEN',
        },
        statusCode: 201,
      );
    }

    if (method == 'GET' && path.contains('/interactions')) {
      return _jsonResponse([
        {
          'id': '0195f000-0000-7000-8000-000000000050',
          'ticketId': '0195f000-0000-7000-8000-000000000001',
          'isInternalNote': false,
          'senderType': 'ATTENDANT',
          'senderName': 'Maria N1',
          'message': 'Protocolo Anatel aberto e informado ao assinante.',
          'createdAt': '2026-10-14T18:01:00Z',
        },
        {
          'id': '0195f000-0000-7000-8000-000000000051',
          'ticketId': '0195f000-0000-7000-8000-000000000001',
          'isInternalNote': true,
          'senderType': 'SUPPORT_N2',
          'senderName': 'Carlos NOC',
          'message': 'Sinal RX na OLT em -29.4 dBm. Necessário verificar conector na CTO.',
          'createdAt': '2026-10-14T18:05:00Z',
        },
      ]);
    }

    if (method == 'POST' && path.endsWith('/interactions')) {
      return _jsonResponse(
        {
          'id': '0195f000-0000-7000-8000-000000000052',
          'ticketId': '0195f000-0000-7000-8000-000000000001',
          'isInternalNote': true,
          'senderType': 'SUPPORT_N2',
          'senderName': 'Carlos NOC',
          'message': 'Teste de porta realizado.',
          'createdAt': '2026-10-14T18:10:00Z',
        },
        statusCode: 201,
      );
    }

    if (method == 'POST' && path.endsWith('/close')) {
      return _jsonResponse({
        'id': '0195f000-0000-7000-8000-000000000001',
        'protocol': '2026101400001',
        'customerId': '0195f000-0000-7000-8000-000000000099',
        'category': 'CONNECTION_OUTAGE',
        'priority': 'URGENT',
        'status': 'CLOSED',
        'channel': 'PHONE',
        'subject': 'Sem internet — LOS vermelho',
        'description': 'Cliente relata LED LOS piscando na ONU Huawei.',
        'anatelSatisfactionRating': 5,
        'slaDeadline': '2099-10-15T18:00:00Z',
        'createdAt': '2026-10-14T18:00:00Z',
      });
    }

    return _jsonResponse({});
  }

  ResponseBody _jsonResponse(Object body, {int statusCode = 200}) {
    return ResponseBody.fromString(
      jsonEncode(body),
      statusCode,
      headers: {
        Headers.contentTypeHeader: [Headers.jsonContentType],
      },
    );
  }
}

void main() {
  group('Onda E — Esteira de Chamados N1 / N2 e Protocolo Anatel', () {
    test('HelpdeskTicket e TicketInteraction mapeiam labels regulatórios Anatel e SLA', () {
      final ticket = HelpdeskTicket.fromJson({
        'id': '0195f000-0000-7000-8000-000000000001',
        'protocol': '2026101400001',
        'customerId': '0195f000-0000-7000-8000-000000000099',
        'category': 'CONNECTION_OUTAGE',
        'priority': 'URGENT',
        'status': 'IN_PROGRESS',
        'channel': 'IN_PERSON',
        'subject': 'Queda de fibra',
        'description': 'LOS vermelho',
        'slaDeadline': '2099-12-31T23:59:00Z',
        'createdAt': '2026-10-14T10:00:00Z',
      });

      expect(ticket.protocol, '2026101400001');
      expect(ticket.categoryLabel, 'Queda de Conexão (SLA 24h)');
      expect(ticket.priorityLabel, 'Urgente');
      expect(ticket.statusLabel, 'Em Tratamento N2 / NOC');
      expect(ticket.channelLabel, 'Presencial (Loja)');
      expect(ticket.isActive, isTrue);
      expect(ticket.isN2Queue, isTrue);
      expect(ticket.isSlaExpired, isFalse);

      final interaction = TicketInteraction.fromJson({
        'id': '1',
        'ticketId': ticket.id,
        'isInternalNote': true,
        'senderType': 'SUPPORT_N2',
        'senderName': 'Analista NOC',
        'message': 'Atenuação -28 dBm',
        'createdAt': '2026-10-14T10:05:00Z',
      });

      expect(interaction.isInternalNote, isTrue);
      expect(interaction.senderTypeLabel, 'Especialista N2 / NOC');
    });

    test('HelpdeskRepository executa todo o ciclo N1 -> N2 -> O.S. -> Interações -> CSAT', () async {
      final dio = Dio();
      final adapter = _MockHelpdeskAdapter();
      dio.httpClientAdapter = adapter;
      final repo = HelpdeskRepository(dio);

      final all = await repo.getAllTickets();
      expect(all.length, 2);
      expect(all.first.protocol, '2026101400001');

      final byProto = await repo.getTicketByProtocol('2026101400001');
      expect(byProto, isNotNull);
      expect(byProto!.subject, 'Sem internet — LOS vermelho');

      final created = await repo.createTicket(
        customerId: '0195f000-0000-7000-8000-000000000099',
        category: 'CONNECTION_OUTAGE',
        priority: 'HIGH',
        channel: 'IN_PERSON',
        subject: 'Queda total',
        description: 'Rompimento de drop na fachada.',
        attendantName: 'Maria N1',
      );
      expect(created.protocol, '2026101400003');

      final escalatedN2 = await repo.escalateToN2(
        ticketId: all.first.id,
        attendantName: 'Maria N1',
        reason: 'Sem gerência na ONU',
      );
      expect(escalatedN2.status, 'IN_PROGRESS');

      final resolvedN2 = await repo.resolveByN2(
        ticketId: all.first.id,
        n2Name: 'Carlos NOC',
        resolutionNotes: 'Reconfigurado profile VLAN na OLT.',
      );
      expect(resolvedN2.status, 'RESOLVED');

      final woResp = await repo.escalateToWorkOrder(
        ticketId: all.first.id,
        n2Name: 'Carlos NOC',
        technicalReason: 'Troca de conector APC',
      );
      expect(woResp['type'], 'MANUTENCAO');

      final interactions = await repo.getInteractions(all.first.id);
      expect(interactions.length, 2);
      expect(interactions.last.isInternalNote, isTrue);

      final newNote = await repo.addInteraction(
        ticketId: all.first.id,
        senderType: 'SUPPORT_N2',
        senderName: 'Carlos NOC',
        message: 'Teste de porta realizado.',
        isInternalNote: true,
      );
      expect(newNote.isInternalNote, isTrue);

      final closed = await repo.closeTicket(
        ticketId: all.first.id,
        satisfactionRating: 5,
        closureNotes: 'Validado com cliente.',
      );
      expect(closed.status, 'CLOSED');
      expect(closed.anatelSatisfactionRating, 5);
    });

    test('AttendanceNotifier avança para AttendanceStep.supportTicket ao selecionar Suporte ou Dúvida', () async {
      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final container = ProviderContainer(
        overrides: [
          sharedPreferencesProvider.overrideWithValue(prefs),
        ],
      );
      addTearDown(container.dispose);

      final notifier = container.read(attendanceProvider.notifier);

      notifier.selectIntent(AttendanceIntent.support);
      expect(notifier.state.step, AttendanceStep.supportTicket);

      notifier.goBack();
      expect(notifier.state.step, AttendanceStep.overview);

      notifier.selectIntent(AttendanceIntent.general);
      expect(notifier.state.step, AttendanceStep.supportTicket);
    });

    testWidgets('SupportDashboardScreen renderiza as 3 abas operacionais e lista protocolos Anatel', (tester) async {
      await tester.binding.setSurfaceSize(const Size(1440, 900));
      addTearDown(() => tester.binding.setSurfaceSize(null));

      SharedPreferences.setMockInitialValues({});
      final prefs = await SharedPreferences.getInstance();
      final dio = Dio();
      dio.httpClientAdapter = _MockHelpdeskAdapter();

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            sharedPreferencesProvider.overrideWithValue(prefs),
            dashboardBiProvider.overrideWith((ref) async => DashboardBiModel.fromJson(const {})),
            technicianWorkOrdersProvider.overrideWith((ref) async => const []),
            helpdeskRepositoryProvider.overrideWithValue(HelpdeskRepository(dio)),
          ],
          child: const MaterialApp(home: SupportDashboardScreen()),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Central de Atendimento & Helpdesk'), findsOneWidget);
      expect(find.textContaining('1. Fila Geral & Protocolos Anatel'), findsOneWidget);
      expect(find.textContaining('2. Fila Escalada N2 / NOC'), findsOneWidget);
      expect(find.textContaining('3. Ordens de Serviço de Campo'), findsOneWidget);
      expect(find.textContaining('Protocolo 2026101400001'), findsOneWidget);
      expect(find.textContaining('Protocolo 2026101400002'), findsOneWidget);
    });

    testWidgets('TicketDetailModal exibe Protocolo Anatel, notas internas e botões de esteira', (tester) async {
      await tester.binding.setSurfaceSize(const Size(1440, 900));
      addTearDown(() => tester.binding.setSurfaceSize(null));

      final dio = Dio();
      dio.httpClientAdapter = _MockHelpdeskAdapter();
      final ticket = HelpdeskTicket.fromJson({
        'id': '0195f000-0000-7000-8000-000000000001',
        'protocol': '2026101400001',
        'customerId': '0195f000-0000-7000-8000-000000000099',
        'category': 'CONNECTION_OUTAGE',
        'priority': 'URGENT',
        'status': 'OPEN',
        'channel': 'PHONE',
        'subject': 'Sem internet — LOS vermelho',
        'description': 'Cliente relata LED LOS piscando na ONU Huawei.',
        'slaDeadline': '2099-10-15T18:00:00Z',
        'createdAt': '2026-10-14T18:00:00Z',
      });

      await tester.pumpWidget(
        ProviderScope(
          overrides: [
            helpdeskRepositoryProvider.overrideWithValue(HelpdeskRepository(dio)),
            ticketInteractionsProvider(ticket.id).overrideWith((ref) async {
              return [
                TicketInteraction.fromJson(const {
                  'id': '0195f000-0000-7000-8000-000000000050',
                  'ticketId': '0195f000-0000-7000-8000-000000000001',
                  'isInternalNote': false,
                  'senderType': 'ATTENDANT',
                  'senderName': 'Maria N1',
                  'message': 'Protocolo Anatel aberto e informado ao assinante.',
                  'createdAt': '2026-10-14T18:01:00Z',
                }),
                TicketInteraction.fromJson(const {
                  'id': '0195f000-0000-7000-8000-000000000051',
                  'ticketId': '0195f000-0000-7000-8000-000000000001',
                  'isInternalNote': true,
                  'senderType': 'SUPPORT_N2',
                  'senderName': 'Carlos NOC',
                  'message': 'Sinal RX na OLT em -29.4 dBm. Necessário verificar conector na CTO.',
                  'createdAt': '2026-10-14T18:05:00Z',
                }),
              ];
            }),
          ],
          child: MaterialApp(
            home: Scaffold(
              body: TicketDetailModal(initialTicket: ticket),
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.textContaining('Protocolo ANATEL: 2026101400001'), findsOneWidget);
      expect(find.text('Escalonar p/ N2 (NOC)'), findsOneWidget);
      expect(find.text('Resolver no N2 (Remoto)'), findsOneWidget);
      expect(find.text('Gerar O.S. de Campo'), findsOneWidget);
      expect(find.text('Encerrar Protocolo (CSAT)'), findsOneWidget);
      expect(find.text('NOTA TÉCNICA INTERNA'), findsOneWidget);
      expect(find.textContaining('Sinal RX na OLT em -29.4 dBm'), findsOneWidget);
    });
  });
}
