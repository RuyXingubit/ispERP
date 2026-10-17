import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/features/dispatch/data/dispatch_models.dart';
import 'package:isperp_app/features/sales/data/sales_models.dart';
import 'package:isperp_app/features/sales/data/sales_onboarding_notifier.dart';

void main() {
  group('Onda L - Viabilidade Técnica por CTO e Agenda Técnica por Turno', () {
    test('ScheduleAvailabilityModel parseia vagas por turno (MANHA e TARDE) corretamente', () {
      final json = {
        'date': '2026-04-20',
        'maxCapacityPerTechnicianPerShift': 4,
        'shifts': [
          {
            'period': 'MANHA',
            'totalCapacity': 8,
            'occupiedSlots': 3,
            'availableSlots': 5,
            'available': true,
          },
          {
            'period': 'TARDE',
            'totalCapacity': 8,
            'occupiedSlots': 8,
            'availableSlots': 0,
            'available': false,
          },
        ],
      };

      final model = ScheduleAvailabilityModel.fromJson(json);

      expect(model.date, '2026-04-20');
      expect(model.maxCapacityPerTechnicianPerShift, 4);
      final morning = model.getShift('MANHA');
      final afternoon = model.getShift('TARDE');
      expect(morning, isNotNull);
      expect(morning!.availableSlots, 5);
      expect(morning.available, isTrue);
      expect(afternoon, isNotNull);
      expect(afternoon!.availableSlots, 0);
      expect(afternoon.available, isFalse);
    });

    test('TechnicianCandidateModel identifica lotação máxima de 4 O.S. por turno (Overbooking Guard)', () {
      final candidate = TechnicianCandidateModel.fromJson({
        'technicianId': 'tech-1',
        'technicianName': 'Roberto instalador',
        'hasCompleteKit': true,
        'hasOnu': true,
        'hasDropCable': true,
        'hasConnectors': true,
        'dropCableBalanceMeters': 350,
        'recommendedScore': 95.0,
        'morningOccupiedSlots': 4,
        'afternoonOccupiedSlots': 2,
        'maxSlotsPerShift': 4,
      });

      expect(candidate.occupiedForPeriod('MANHA'), 4);
      expect(candidate.isShiftFull('MANHA'), isTrue);
      expect(candidate.occupiedForPeriod('TARDE'), 2);
      expect(candidate.isShiftFull('TARDE'), isFalse);
    });

    test('InstallationDemandModel parseia CTO reservada e turno agendado da venda', () {
      final demand = InstallationDemandModel.fromJson({
        'id': 'dem-1',
        'workOrderId': 'wo-1',
        'contractId': 'cnt-1',
        'customerName': 'Maria Oliveira',
        'customerAddress': 'Av. Brasil, 500',
        'ctoId': 'cto-centro-01',
        'ctoName': 'CTO-CENTRO-01',
        'ctoPortNumber': 3,
        'estimatedDropMeters': 65,
        'onuModelRequired': 'ONT Wi-Fi 6 AX3000',
        'fastConnectorsCount': 2,
        'ptoRosetteCount': 1,
        'status': 'PENDING_ALLOCATION',
        'scheduledDate': '2026-04-21',
        'scheduledPeriod': 'TARDE',
      });

      expect(demand.ctoId, 'cto-centro-01');
      expect(demand.ctoName, 'CTO-CENTRO-01');
      expect(demand.ctoPortNumber, 3);
      expect(demand.scheduledDate, '2026-04-21');
      expect(demand.scheduledPeriod, 'TARDE');
    });

    test('SalesOnboardingState preserva seleção de CTO viável e turno preferencial de instalação', () {
      const initial = SalesOnboardingState();
      const cto = FeasibleCtoItem(
        ctoId: 'cto-uuid-01',
        ctoName: 'CTO-01-CENTRO',
        distanceMeters: 42.0,
        freePorts: 6,
        totalPorts: 16,
        hasCapacity: true,
      );
      final updated = initial.copyWith(
        selectedCto: cto,
        preferredScheduleDate: '2026-04-22',
        preferredSchedulePeriod: 'TARDE',
      );

      expect(updated.selectedCto?.ctoId, 'cto-uuid-01');
      expect(updated.selectedCto?.ctoName, 'CTO-01-CENTRO');
      expect(updated.selectedCto?.freePorts, 6);
      expect(updated.preferredScheduleDate, '2026-04-22');
      expect(updated.preferredSchedulePeriod, 'TARDE');
    });
  });
}
