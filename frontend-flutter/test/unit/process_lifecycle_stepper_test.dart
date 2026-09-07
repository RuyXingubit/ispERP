import 'package:flutter/material.dart';
import 'package:flutter_test/flutter_test.dart';
import 'package:isperp_app/core/widgets/process_lifecycle_stepper.dart';

void main() {
  final testSteps = [
    const ProcessLifecycleStep(
      id: 'step_1',
      title: '1. Venda & Contrato',
      subtitle: 'Contrato Assinado',
      responsibleRoleName: 'Vendas',
      allowedRoles: ['SALES', 'ADMIN'],
      isCompleted: true,
      isActive: false,
      icon: Icons.assignment_turned_in_rounded,
      popGuideTitle: 'POP-01: Formalização',
      popGuideContent: 'Conteúdo do POP 01: Assinatura e checagem de viabilidade.',
    ),
    const ProcessLifecycleStep(
      id: 'step_2',
      title: '2. Triagem Técnica',
      subtitle: 'CTO 04 - 120m Drop',
      responsibleRoleName: 'Torre de Controle',
      allowedRoles: ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
      isCompleted: true,
      isActive: false,
      icon: Icons.cable_rounded,
      popGuideTitle: 'POP-02: Triagem',
      popGuideContent: 'Conteúdo do POP 02: Dimensionamento de insumos e porta.',
    ),
    const ProcessLifecycleStep(
      id: 'step_3',
      title: '3. Despacho Veicular',
      subtitle: 'Aguardando Alocação',
      responsibleRoleName: 'Torre de Controle',
      allowedRoles: ['SUPPORT_ANALYST', 'SUPPORT_N2', 'ADMIN'],
      isCompleted: false,
      isActive: true,
      icon: Icons.local_shipping_rounded,
      popGuideTitle: 'POP-03: Despacho',
      popGuideContent: 'Conteúdo do POP 03: Alocação do técnico de campo mais próximo.',
    ),
    const ProcessLifecycleStep(
      id: 'step_4',
      title: '4. Instalação em Campo',
      subtitle: 'Aguardando Despacho',
      responsibleRoleName: 'Técnico de Campo',
      allowedRoles: ['TECHNICIAN', 'ADMIN'],
      isCompleted: false,
      isActive: false,
      icon: Icons.engineering_rounded,
      popGuideTitle: 'POP-04: Instalação',
      popGuideContent: 'Conteúdo do POP 04: Lançamento do drop e medição óptica.',
    ),
  ];

  Widget buildTestWidget({
    required List<ProcessLifecycleStep> steps,
    String? currentUserRole,
  }) {
    return MaterialApp(
      home: Scaffold(
        body: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.all(16.0),
            child: ProcessLifecycleStepper(
              processTitle: 'Esteira Operacional FTTH',
              steps: steps,
              currentUserRole: currentUserRole,
            ),
          ),
        ),
      ),
    );
  }

  group('ProcessLifecycleStepper Tests', () {
    testWidgets('renders all steps and displays correct progress percentage', (tester) async {
      await tester.pumpWidget(
        buildTestWidget(steps: testSteps, currentUserRole: 'SUPPORT_ANALYST'),
      );
      await tester.pumpAndSettle();

      // Verifica título da esteira
      expect(find.text('Esteira Operacional FTTH'), findsOneWidget);

      // 2 de 4 passos completados + 1 ativo -> Etapa 3 de 4 (50%)
      expect(find.text('Etapa 3 de 4 (50%)'), findsOneWidget);

      // Verifica renderização dos títulos dos passos
      expect(find.text('1. Venda & Contrato'), findsOneWidget);
      expect(find.text('2. Triagem Técnica'), findsOneWidget);
      expect(find.text('3. Despacho Veicular'), findsOneWidget);
      expect(find.text('4. Instalação em Campo'), findsOneWidget);
    });

    testWidgets('shows direct attribution banner when user role matches active step', (tester) async {
      await tester.pumpWidget(
        buildTestWidget(steps: testSteps, currentUserRole: 'SUPPORT_ANALYST'),
      );
      await tester.pumpAndSettle();

      // Etapa ativa é Despacho (allowedRoles: SUPPORT_ANALYST, ADMIN)
      expect(
        find.textContaining('Etapa ativa sob sua responsabilidade (Torre de Controle). Ações liberadas para execução.'),
        findsOneWidget,
      );
    });

    testWidgets('shows observer / handoff banner when user role does not match active step', (tester) async {
      await tester.pumpWidget(
        buildTestWidget(steps: testSteps, currentUserRole: 'SALES'),
      );
      await tester.pumpAndSettle();

      // Para perfil SALES, a etapa ativa (Torre de Controle) não é atribuição direta
      expect(
        find.textContaining('Etapa ativa sob responsabilidade do setor: Torre de Controle'),
        findsOneWidget,
      );
      expect(
        find.textContaining('Modo somente leitura.'),
        findsOneWidget,
      );
    });

    testWidgets('admin role always receives direct execution authority', (tester) async {
      await tester.pumpWidget(
        buildTestWidget(steps: testSteps, currentUserRole: 'ADMIN'),
      );
      await tester.pumpAndSettle();

      expect(
        find.textContaining('Etapa ativa sob sua responsabilidade (Torre de Controle). Ações liberadas para execução.'),
        findsOneWidget,
      );
    });

    testWidgets('tapping POP guide opens modal dialog with standard operating procedure', (tester) async {
      await tester.pumpWidget(
        buildTestWidget(steps: testSteps, currentUserRole: 'SUPPORT_ANALYST'),
      );
      await tester.pumpAndSettle();

      // Localiza o botão 'Ver POP' no banner da etapa ativa
      final popButton = find.text('Ver POP');
      expect(popButton, findsOneWidget);

      // Toca no botão de POP
      await tester.tap(popButton);
      await tester.pumpAndSettle();

      // Verifica se o modal abriu com o título e conteúdo do POP
      expect(find.text('POP-03: Despacho'), findsOneWidget);
      expect(
        find.text('Perfil Executor: Torre de Controle'),
        findsOneWidget,
      );
      expect(
        find.text('Conteúdo do POP 03: Alocação do técnico de campo mais próximo.'),
        findsOneWidget,
      );

      // Fecha o modal
      final closeButton = find.text('Entendido / Fechar');
      expect(closeButton, findsOneWidget);
      await tester.tap(closeButton);
      await tester.pumpAndSettle();

      // Modal fechado
      expect(find.text('POP-03: Despacho'), findsNothing);
    });

    testWidgets('renders empty widget if steps list is empty', (tester) async {
      await tester.pumpWidget(
        buildTestWidget(steps: const [], currentUserRole: 'ADMIN'),
      );
      await tester.pumpAndSettle();

      expect(find.byType(ProcessLifecycleStepper), findsOneWidget);
      expect(find.text('Esteira Operacional FTTH'), findsNothing);
    });

    testWidgets('renders maintenance process stepper with optical diagnosis POP guide', (tester) async {
      final maintenanceSteps = [
        const ProcessLifecycleStep(
          id: 'step_man_1',
          title: '1. Abertura & Triagem N1',
          subtitle: 'LOS Vermelho / Sem Sinal',
          responsibleRoleName: 'Suporte N1',
          allowedRoles: ['SUPPORT', 'SUPPORT_ANALYST', 'ADMIN'],
          isCompleted: true,
          isActive: false,
          icon: Icons.headset_mic_rounded,
        ),
        const ProcessLifecycleStep(
          id: 'step_man_2',
          title: '2. Diagnóstico Remoto N2',
          subtitle: 'Porta PON Flapando',
          responsibleRoleName: 'NOC / Suporte N2',
          allowedRoles: ['SUPPORT_N2', 'ADMIN'],
          isCompleted: false,
          isActive: true,
          icon: Icons.router_rounded,
          popGuideTitle: 'POP-NOC-02: Diagnóstico Avançado de Atenuação Óptica',
          popGuideContent: '1. Medição de potência óptica na OLT.\n2. Se sinal < -27 dBm, emitir reparo físico.',
        ),
      ];

      await tester.pumpWidget(
        MaterialApp(
          home: Scaffold(
            body: ProcessLifecycleStepper(
              processTitle: 'Esteira de Resolução de Incidente & Reparo FTTH',
              steps: maintenanceSteps,
              currentUserRole: 'SUPPORT_N2',
            ),
          ),
        ),
      );
      await tester.pumpAndSettle();

      expect(find.text('Esteira de Resolução de Incidente & Reparo FTTH'), findsOneWidget);
      expect(find.text('1. Abertura & Triagem N1'), findsOneWidget);
      expect(find.text('2. Diagnóstico Remoto N2'), findsOneWidget);
      expect(find.textContaining('Etapa ativa sob sua responsabilidade (NOC / Suporte N2)'), findsOneWidget);

      // Toca em Ver POP
      await tester.tap(find.text('Ver POP'));
      await tester.pumpAndSettle();

      expect(find.text('POP-NOC-02: Diagnóstico Avançado de Atenuação Óptica'), findsOneWidget);
      expect(find.textContaining('Medição de potência óptica na OLT'), findsOneWidget);
    });
  });
}
