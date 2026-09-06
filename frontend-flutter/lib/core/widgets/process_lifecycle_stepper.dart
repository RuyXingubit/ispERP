import 'package:flutter/material.dart';
import '../theme/app_theme.dart';

/// Modelo de uma etapa dentro do ciclo de vida de um processo no ispERP.
@immutable
class ProcessLifecycleStep {
  final String id;
  final String title;
  final String subtitle;
  final String responsibleRoleName;
  final List<String> allowedRoles;
  final bool isCompleted;
  final bool isActive;
  final String? assignedPersonName;
  final String? popGuideTitle;
  final String? popGuideContent;
  final IconData icon;

  const ProcessLifecycleStep({
    required this.id,
    required this.title,
    required this.subtitle,
    required this.responsibleRoleName,
    required this.allowedRoles,
    this.isCompleted = false,
    this.isActive = false,
    this.assignedPersonName,
    this.popGuideTitle,
    this.popGuideContent,
    required this.icon,
  });
}

/// Componente visual reutilizável de Linha do Tempo e Governança de Processos.
/// Exibe a esteira completa (visibilidade para todos) e destaca a atribuição do operador atual.
class ProcessLifecycleStepper extends StatelessWidget {
  final String processTitle;
  final List<ProcessLifecycleStep> steps;
  final String? currentUserRole;

  const ProcessLifecycleStepper({
    super.key,
    required this.processTitle,
    required this.steps,
    this.currentUserRole,
  });

  @override
  Widget build(BuildContext context) {
    if (steps.isEmpty) return const SizedBox.shrink();

    final completedCount = steps.where((s) => s.isCompleted).length;
    final totalCount = steps.length;
    final progressPercent = (completedCount / totalCount).clamp(0.0, 1.0);

    // Identifica se a etapa ativa pertence ao perfil logado
    final activeStep = steps.where((s) => s.isActive).toList();
    final currentActive = activeStep.isNotEmpty ? activeStep.first : null;
    final isUserResponsibleForActive = currentActive != null &&
        (currentUserRole == 'ADMIN' ||
            currentActive.allowedRoles.contains(currentUserRole?.toUpperCase()));

    return Container(
      padding: const EdgeInsets.all(16),
      decoration: BoxDecoration(
        color: AppTheme.darkSurface,
        borderRadius: BorderRadius.circular(12),
        border: Border.all(color: AppTheme.darkBorder),
      ),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.stretch,
        children: [
          // Cabeçalho da Esteira e Barra de Progresso
          Row(
            mainAxisAlignment: MainAxisAlignment.spaceBetween,
            children: [
              Expanded(
                child: Row(
                  children: [
                    const Icon(Icons.alt_route_rounded, color: AppTheme.primaryBlue, size: 20),
                    const SizedBox(width: 8),
                    Expanded(
                      child: Text(
                        processTitle,
                        style: const TextStyle(
                          fontSize: 13,
                          fontWeight: FontWeight.bold,
                          color: AppTheme.textPrimary,
                        ),
                        overflow: TextOverflow.ellipsis,
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(width: 12),
              Container(
                padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 4),
                decoration: BoxDecoration(
                  color: AppTheme.primaryBlue.withValues(alpha: 0.12),
                  borderRadius: BorderRadius.circular(6),
                  border: Border.all(color: AppTheme.primaryBlue.withValues(alpha: 0.3)),
                ),
                child: Text(
                  'Etapa ${completedCount + (currentActive != null ? 1 : 0)} de $totalCount (${(progressPercent * 100).toInt()}%)',
                  style: const TextStyle(
                    fontSize: 11,
                    fontWeight: FontWeight.bold,
                    color: AppTheme.primaryBlue,
                  ),
                ),
              ),
            ],
          ),
          const SizedBox(height: 10),

          // Barra linear de progresso
          ClipRRect(
            borderRadius: BorderRadius.circular(4),
            child: LinearProgressIndicator(
              value: progressPercent,
              backgroundColor: AppTheme.darkCard,
              valueColor: const AlwaysStoppedAnimation<Color>(AppTheme.accentGreen),
              minHeight: 4,
            ),
          ),
          const SizedBox(height: 14),

          // Alerta contextual de atribuição do usuário logado na etapa ativa
          if (currentActive != null) ...[
            Container(
              padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 8),
              decoration: BoxDecoration(
                color: isUserResponsibleForActive
                    ? AppTheme.accentGreen.withValues(alpha: 0.1)
                    : AppTheme.primaryBlue.withValues(alpha: 0.08),
                borderRadius: BorderRadius.circular(8),
                border: Border.all(
                  color: isUserResponsibleForActive
                      ? AppTheme.accentGreen.withValues(alpha: 0.35)
                      : AppTheme.primaryBlue.withValues(alpha: 0.25),
                ),
              ),
              child: Row(
                children: [
                  Icon(
                    isUserResponsibleForActive ? Icons.assignment_ind : Icons.info_outline_rounded,
                    size: 16,
                    color: isUserResponsibleForActive ? AppTheme.accentGreen : AppTheme.primaryBlue,
                  ),
                  const SizedBox(width: 8),
                  Expanded(
                    child: Text(
                      isUserResponsibleForActive
                          ? 'Etapa ativa sob sua responsabilidade (${currentActive.responsibleRoleName}). Ações liberadas para execução.'
                          : 'Etapa ativa sob responsabilidade do setor: ${currentActive.responsibleRoleName}${currentActive.assignedPersonName != null ? ' (${currentActive.assignedPersonName})' : ''}. Modo somente leitura.',
                      style: TextStyle(
                        fontSize: 11,
                        fontWeight: FontWeight.w600,
                        color: isUserResponsibleForActive ? AppTheme.accentGreen : AppTheme.textPrimary,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  if (currentActive.popGuideContent != null) ...[
                    const SizedBox(width: 8),
                    InkWell(
                      onTap: () => _showPopGuideModal(context, currentActive),
                      borderRadius: BorderRadius.circular(4),
                      child: Container(
                        padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 3),
                        decoration: BoxDecoration(
                          color: AppTheme.darkCard,
                          borderRadius: BorderRadius.circular(4),
                          border: Border.all(color: AppTheme.darkBorder),
                        ),
                        child: Row(
                          mainAxisSize: MainAxisSize.min,
                          children: const [
                            Icon(Icons.menu_book_outlined, size: 12, color: AppTheme.textSecondary),
                            SizedBox(width: 4),
                            Text(
                              'Ver POP',
                              style: TextStyle(fontSize: 10, fontWeight: FontWeight.bold, color: AppTheme.textSecondary),
                            ),
                          ],
                        ),
                      ),
                    ),
                  ],
                ],
              ),
            ),
            const SizedBox(height: 14),
          ],

          // Stepper horizontal com scroll defensivo anti-overflow
          SingleChildScrollView(
            scrollDirection: Axis.horizontal,
            child: Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                for (int i = 0; i < steps.length; i++) ...[
                  _buildStepNode(context, steps[i], i + 1),
                  if (i < steps.length - 1) _buildConnector(steps[i].isCompleted),
                ],
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildStepNode(BuildContext context, ProcessLifecycleStep step, int index) {
    Color nodeColor;
    IconData nodeIcon;

    if (step.isCompleted) {
      nodeColor = AppTheme.accentGreen;
      nodeIcon = Icons.check_circle_rounded;
    } else if (step.isActive) {
      nodeColor = AppTheme.primaryBlue;
      nodeIcon = step.icon;
    } else {
      nodeColor = AppTheme.textMuted;
      nodeIcon = step.icon;
    }

    return Container(
      width: 170,
      padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 4),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.center,
        children: [
          // Ícone e Círculo do Nó
          Container(
            width: 36,
            height: 36,
            decoration: BoxDecoration(
              shape: BoxShape.circle,
              color: step.isActive
                  ? nodeColor.withValues(alpha: 0.18)
                  : AppTheme.darkCard,
              border: Border.all(
                color: nodeColor,
                width: step.isActive ? 2 : 1,
              ),
            ),
            child: Icon(nodeIcon, size: 18, color: nodeColor),
          ),
          const SizedBox(height: 8),

          // Título da Etapa
          Text(
            step.title,
            style: TextStyle(
              fontSize: 12,
              fontWeight: step.isActive ? FontWeight.bold : FontWeight.w600,
              color: step.isActive ? AppTheme.textPrimary : (step.isCompleted ? AppTheme.textPrimary : AppTheme.textMuted),
            ),
            textAlign: TextAlign.center,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),
          const SizedBox(height: 4),

          // Badge de Setor Responsável
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
            decoration: BoxDecoration(
              color: nodeColor.withValues(alpha: 0.1),
              borderRadius: BorderRadius.circular(4),
            ),
            child: Text(
              step.responsibleRoleName,
              style: TextStyle(
                fontSize: 9,
                fontWeight: FontWeight.bold,
                color: nodeColor,
              ),
              maxLines: 1,
              overflow: TextOverflow.ellipsis,
            ),
          ),
          const SizedBox(height: 4),

          // Subtítulo descritivo ou Pessoa Atribuída
          Text(
            step.assignedPersonName != null ? 'Resp: ${step.assignedPersonName}' : step.subtitle,
            style: const TextStyle(fontSize: 10, color: AppTheme.textSecondary),
            textAlign: TextAlign.center,
            maxLines: 2,
            overflow: TextOverflow.ellipsis,
          ),

          // Link para abrir POP individual da etapa
          if (step.popGuideContent != null) ...[
            const SizedBox(height: 4),
            InkWell(
              onTap: () => _showPopGuideModal(context, step),
              child: const Text(
                'Consultar POP',
                style: TextStyle(fontSize: 9, color: AppTheme.primaryBlue, decoration: TextDecoration.underline),
              ),
            ),
          ],
        ],
      ),
    );
  }

  Widget _buildConnector(bool isCompleted) {
    return Container(
      width: 24,
      height: 2,
      margin: const EdgeInsets.only(top: 18),
      color: isCompleted ? AppTheme.accentGreen : AppTheme.darkBorder,
    );
  }

  void _showPopGuideModal(BuildContext context, ProcessLifecycleStep step) {
    showDialog(
      context: context,
      builder: (ctx) {
        return AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
          title: Row(
            children: [
              const Icon(Icons.menu_book_rounded, color: AppTheme.primaryBlue, size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  step.popGuideTitle ?? 'Procedimento Operacional Padrão (POP)',
                  style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                  overflow: TextOverflow.ellipsis,
                ),
              ),
            ],
          ),
          content: ConstrainedBox(
            constraints: const BoxConstraints(maxWidth: 500, maxHeight: 400),
            child: SingleChildScrollView(
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Container(
                    padding: const EdgeInsets.all(10),
                    decoration: BoxDecoration(
                      color: AppTheme.darkCard,
                      borderRadius: BorderRadius.circular(8),
                      border: Border.all(color: AppTheme.darkBorder),
                    ),
                    child: Row(
                      children: [
                        const Icon(Icons.badge_outlined, size: 16, color: AppTheme.accentGreen),
                        const SizedBox(width: 6),
                        Text(
                          'Perfil Executor: ${step.responsibleRoleName}',
                          style: const TextStyle(fontSize: 12, fontWeight: FontWeight.bold, color: AppTheme.accentGreen),
                        ),
                      ],
                    ),
                  ),
                  const SizedBox(height: 12),
                  Text(
                    step.popGuideContent ?? 'Nenhuma instrução de trabalho adicional configurada para esta etapa.',
                    style: const TextStyle(fontSize: 12, height: 1.5, color: AppTheme.textPrimary),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.pop(ctx),
              child: const Text('Entendido / Fechar'),
            ),
          ],
        );
      },
    );
  }
}
