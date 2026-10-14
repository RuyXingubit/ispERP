import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import '../../../core/theme/app_theme.dart';
import '../data/electronic_signature_models.dart';
import '../data/electronic_signature_repository.dart';

/// Modal de Assinatura Eletrônica Forense (MP 2.200-2 & Lei 14.063/2020)
/// com conciliação de titularidade via PIX Simbólico (R$ 1,00 abatido na 1ª fatura) e Fallbacks.
class ContractForensicSignatureModal extends ConsumerStatefulWidget {
  final String contractId;
  final String contractNumber;
  final String customerName;

  const ContractForensicSignatureModal({
    super.key,
    required this.contractId,
    required this.contractNumber,
    required this.customerName,
  });

  static Future<void> show(
    BuildContext context, {
    required String contractId,
    required String contractNumber,
    required String customerName,
  }) {
    return showDialog(
      context: context,
      builder: (_) => ContractForensicSignatureModal(
        contractId: contractId,
        contractNumber: contractNumber,
        customerName: customerName,
      ),
    );
  }

  @override
  ConsumerState<ContractForensicSignatureModal> createState() =>
      _ContractForensicSignatureModalState();
}

class _ContractForensicSignatureModalState
    extends ConsumerState<ContractForensicSignatureModal> {
  bool _isLoading = true;
  bool _isCreatingSession = false;
  bool _isSyncingStatus = false;
  bool _isSubmittingFallback = false;

  SignatureSessionModel? _activeSession;
  FallbackMethodEnum _selectedFallback = FallbackMethodEnum.govBr;
  final _fallbackJustificationCtrl = TextEditingController();
  String? _errorMessage;

  @override
  void initState() {
    super.initState();
    _loadExistingSignatures();
  }

  @override
  void dispose() {
    _fallbackJustificationCtrl.dispose();
    super.dispose();
  }

  Future<void> _loadExistingSignatures() async {
    setState(() {
      _isLoading = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(electronicSignatureRepositoryProvider);
      final sessions = await repo.listContractSignatures(widget.contractId);
      if (!mounted) return;
      setState(() {
        _activeSession = sessions.isNotEmpty ? sessions.first : null;
        _isLoading = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isLoading = false;
        _errorMessage = 'Falha ao consultar histórico de assinaturas: $e';
      });
    }
  }

  Future<void> _generateNewSession() async {
    setState(() {
      _isCreatingSession = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(electronicSignatureRepositoryProvider);
      final created = await repo.createSignatureSession(
        contractId: widget.contractId,
        symbolicAmount: 1.00,
      );
      if (!mounted) return;
      setState(() {
        _activeSession = created;
        _isCreatingSession = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isCreatingSession = false;
        _errorMessage = 'Falha ao criar sessão de assinatura forense: $e';
      });
    }
  }

  Future<void> _syncSessionStatus() async {
    final session = _activeSession;
    if (session == null || session.token.isEmpty) return;

    setState(() {
      _isSyncingStatus = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(electronicSignatureRepositoryProvider);
      final updated = await repo.getSignatureStatus(session.token);
      if (!mounted) return;
      setState(() {
        _activeSession = updated;
        _isSyncingStatus = false;
      });
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSyncingStatus = false;
        _errorMessage = 'Erro ao sincronizar status da assinatura: $e';
      });
    }
  }

  Future<void> _applyFallback() async {
    final session = _activeSession;
    if (session == null || session.token.isEmpty) return;

    setState(() {
      _isSubmittingFallback = true;
      _errorMessage = null;
    });
    try {
      final repo = ref.read(electronicSignatureRepositoryProvider);
      final updated = await repo.selectFallbackMethod(
        token: session.token,
        fallbackMethod: _selectedFallback,
        justification: _fallbackJustificationCtrl.text,
      );
      if (!mounted) return;
      setState(() {
        _activeSession = updated;
        _isSubmittingFallback = false;
      });
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text(
            'Modalidade alternativa (${_selectedFallback.label}) registrada com sucesso!',
          ),
          backgroundColor: AppTheme.accentGreen,
        ),
      );
    } catch (e) {
      if (!mounted) return;
      setState(() {
        _isSubmittingFallback = false;
        _errorMessage = 'Erro ao registrar fallback de assinatura: $e';
      });
    }
  }

  void _copyToClipboard(String text, String label) {
    Clipboard.setData(ClipboardData(text: text));
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text('$label copiado para a área de transferência!'),
        backgroundColor: AppTheme.primaryBlue,
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final session = _activeSession;

    return Dialog(
      backgroundColor: AppTheme.darkBg,
      shape: RoundedRectangleBorder(
        borderRadius: BorderRadius.circular(14),
        side: const BorderSide(color: AppTheme.darkBorder),
      ),
      insetPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 16),
      child: ConstrainedBox(
        constraints: const BoxConstraints(maxWidth: 680, maxHeight: 620),
        child: Padding(
          padding: const EdgeInsets.all(18.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Cabeçalho
              Row(
                children: [
                  Container(
                    padding: const EdgeInsets.all(8),
                    decoration: BoxDecoration(
                      color: AppTheme.primaryBlue.withValues(alpha: 0.15),
                      borderRadius: BorderRadius.circular(8),
                    ),
                    child: const Icon(
                      Icons.verified_user_outlined,
                      color: AppTheme.primaryBlue,
                      size: 20,
                    ),
                  ),
                  const SizedBox(width: 10),
                  Expanded(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          'Assinatura Eletrônica Forense • ${widget.contractNumber}',
                          style: const TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 15,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                        Text(
                          'Titular: ${widget.customerName} • MP 2.200-2 & Conciliação PIX BACEN',
                          style: const TextStyle(
                            color: AppTheme.textSecondary,
                            fontSize: 11,
                          ),
                          overflow: TextOverflow.ellipsis,
                        ),
                      ],
                    ),
                  ),
                  IconButton(
                    onPressed: () => Navigator.of(context).pop(),
                    icon: const Icon(Icons.close, size: 18),
                  ),
                ],
              ),
              const SizedBox(height: 10),

              if (_errorMessage != null) ...[
                Container(
                  padding: const EdgeInsets.symmetric(
                    horizontal: 10,
                    vertical: 6,
                  ),
                  decoration: BoxDecoration(
                    color: AppTheme.accentError.withValues(alpha: 0.12),
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(
                      color: AppTheme.accentError.withValues(alpha: 0.4),
                    ),
                  ),
                  child: Row(
                    children: [
                      const Icon(
                        Icons.error_outline,
                        color: AppTheme.accentError,
                        size: 16,
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          _errorMessage!,
                          style: const TextStyle(
                            color: AppTheme.accentError,
                            fontSize: 11,
                          ),
                        ),
                      ),
                    ],
                  ),
                ),
                const SizedBox(height: 8),
              ],

              Expanded(
                child: _isLoading
                    ? const Center(child: CircularProgressIndicator())
                    : session == null
                        ? _buildEmptySessionView()
                        : SingleChildScrollView(
                            child: _buildSessionDetailView(session),
                          ),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildEmptySessionView() {
    return Center(
      child: Container(
        padding: const EdgeInsets.all(20),
        decoration: BoxDecoration(
          color: AppTheme.darkSurface,
          borderRadius: BorderRadius.circular(12),
          border: Border.all(color: AppTheme.darkBorder),
        ),
        child: Column(
          mainAxisSize: MainAxisSize.min,
          children: [
            const Icon(
              Icons.draw_outlined,
              color: AppTheme.primaryBlue,
              size: 42,
            ),
            const SizedBox(height: 12),
            const Text(
              'Nenhuma sessão de assinatura ativa para este contrato',
              style: TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 6),
            const Text(
              'Gere o link de Assinatura Eletrônica Forense com PIX Simbólico de R\$ 1,00 (abatido automaticamente na 1ª fatura) para validar a titularidade civil do assinante junto ao BACEN.',
              style: TextStyle(color: AppTheme.textSecondary, fontSize: 12),
              textAlign: TextAlign.center,
            ),
            const SizedBox(height: 16),
            ElevatedButton.icon(
              onPressed: _isCreatingSession ? null : _generateNewSession,
              icon: _isCreatingSession
                  ? const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(
                        strokeWidth: 2,
                        color: Colors.white,
                      ),
                    )
                  : const Icon(Icons.qr_code_2_outlined, size: 18),
              label: const Text(
                'Gerar Assinatura Forense via PIX Simbólico (R\$ 1,00)',
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildSessionDetailView(SignatureSessionModel session) {
    final statusColor = session.isSigned
        ? AppTheme.accentGreen
        : (session.isRejected ? AppTheme.accentError : AppTheme.accentWarning);

    final fallbackOptions = [
      FallbackMethodEnum.govBr,
      FallbackMethodEnum.emailOtp,
      FallbackMethodEnum.physicalNotary,
    ];

    return Column(
      crossAxisAlignment: CrossAxisAlignment.stretch,
      children: [
        // 1. Barra de Status + Sincronização
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: statusColor.withValues(alpha: 0.1),
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: statusColor.withValues(alpha: 0.4)),
          ),
          child: Wrap(
            spacing: 10,
            runSpacing: 8,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              Row(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Icon(
                    session.isSigned
                        ? Icons.verified
                        : (session.isRejected
                            ? Icons.gpp_bad_outlined
                            : Icons.pending_actions_outlined),
                    color: statusColor,
                    size: 20,
                  ),
                  const SizedBox(width: 8),
                  Flexible(
                    child: Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          session.status.label,
                          style: TextStyle(
                            fontWeight: FontWeight.bold,
                            fontSize: 13,
                            color: statusColor,
                          ),
                        ),
                        Text(
                          'Valor Simbólico: R\$ ${session.symbolicAmount.toStringAsFixed(2)} (Crédito abatido na 1ª fatura)',
                          style: const TextStyle(
                            color: AppTheme.textSecondary,
                            fontSize: 11,
                          ),
                        ),
                      ],
                    ),
                  ),
                ],
              ),
              Wrap(
                spacing: 8,
                runSpacing: 6,
                children: [
                  OutlinedButton.icon(
                    onPressed: _isSyncingStatus ? null : _syncSessionStatus,
                    icon: _isSyncingStatus
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.sync, size: 15),
                    label: const Text(
                      'Sincronizar Status',
                      style: TextStyle(fontSize: 11),
                    ),
                  ),
                  if (!session.isSigned)
                    TextButton.icon(
                      onPressed:
                          _isCreatingSession ? null : _generateNewSession,
                      icon: const Icon(Icons.refresh, size: 15),
                      label: const Text(
                        'Novo Token',
                        style: TextStyle(fontSize: 11),
                      ),
                    ),
                ],
              ),
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 2. Evidência Criptográfica & Link Público
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: AppTheme.darkSurface,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: AppTheme.darkBorder),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              const Text(
                'Evidência Forense & Link do Assinante (MP 2.200-2)',
                style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
              ),
              const SizedBox(height: 8),
              Row(
                children: [
                  Expanded(
                    child: Text(
                      'Link Público: ${session.signatureUrl}',
                      style: const TextStyle(
                        fontSize: 11,
                        color: AppTheme.primaryBlue,
                      ),
                      overflow: TextOverflow.ellipsis,
                    ),
                  ),
                  const SizedBox(width: 8),
                  OutlinedButton.icon(
                    onPressed: () => _copyToClipboard(
                      session.signatureUrl,
                      'Link de assinatura',
                    ),
                    icon: const Icon(Icons.copy, size: 14),
                    label: const Text('Copiar Link', style: TextStyle(fontSize: 11)),
                  ),
                ],
              ),
              if (session.documentSha256Hash != null &&
                  session.documentSha256Hash!.isNotEmpty) ...[
                const SizedBox(height: 6),
                Text(
                  'SHA-256 do Documento: ${session.documentSha256Hash}',
                  style: const TextStyle(
                    fontSize: 10,
                    color: AppTheme.textSecondary,
                    fontFamily: 'monospace',
                  ),
                  overflow: TextOverflow.ellipsis,
                ),
              ],
            ],
          ),
        ),
        const SizedBox(height: 12),

        // 3. PIX Simbólico (pay.xingubit.com.br) ou Dados Bancários Conciliados
        Container(
          padding: const EdgeInsets.all(12),
          decoration: BoxDecoration(
            color: AppTheme.darkSurface,
            borderRadius: BorderRadius.circular(10),
            border: Border.all(color: AppTheme.darkBorder),
          ),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              Text(
                session.isSigned
                    ? 'Certificado Forense Conciliado (SPI / BACEN)'
                    : 'PIX Simbólico para Conciliação de Titularidade (pay.xingubit.com.br)',
                style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
              ),
              const SizedBox(height: 8),
              if (session.pixCopyPaste != null &&
                  session.pixCopyPaste!.isNotEmpty &&
                  !session.isSigned) ...[
                Row(
                  children: [
                    Expanded(
                      child: Text(
                        session.pixCopyPaste!,
                        maxLines: 2,
                        overflow: TextOverflow.ellipsis,
                        style: const TextStyle(
                          fontSize: 11,
                          color: AppTheme.textSecondary,
                          fontFamily: 'monospace',
                        ),
                      ),
                    ),
                    const SizedBox(width: 8),
                    ElevatedButton.icon(
                      onPressed: () => _copyToClipboard(
                        session.pixCopyPaste!,
                        'PIX Copia e Cola',
                      ),
                      icon: const Icon(Icons.pix_outlined, size: 15),
                      label: const Text('Copiar PIX', style: TextStyle(fontSize: 11)),
                    ),
                  ],
                ),
                if (session.pixTxid != null) ...[
                  const SizedBox(height: 4),
                  Text(
                    'TxID: ${session.pixTxid}',
                    style: const TextStyle(
                      fontSize: 10,
                      color: AppTheme.textSecondary,
                    ),
                  ),
                ],
              ],
              if (session.isSigned) ...[
                Wrap(
                  spacing: 14,
                  runSpacing: 6,
                  children: [
                    Text(
                      'EndToEndId: ${session.pixEndToEndId ?? "Validado"}',
                      style: const TextStyle(fontSize: 11, fontWeight: FontWeight.w600),
                    ),
                    Text(
                      'Pagador: ${session.payerName ?? widget.customerName}',
                      style: const TextStyle(fontSize: 11),
                    ),
                    if (session.payerBankName != null)
                      Text(
                        'Banco: ${session.payerBankName}',
                        style: const TextStyle(fontSize: 11, color: AppTheme.textSecondary),
                      ),
                    if (session.forensicCertificatePdfUrl != null)
                      Text(
                        'PDF Forense: ${session.forensicCertificatePdfUrl}',
                        style: const TextStyle(
                          fontSize: 11,
                          color: AppTheme.accentGreen,
                        ),
                      ),
                  ],
                ),
              ],
              if (session.rejectionReason != null &&
                  session.rejectionReason!.isNotEmpty) ...[
                const SizedBox(height: 8),
                Container(
                  padding: const EdgeInsets.all(8),
                  decoration: BoxDecoration(
                    color: AppTheme.accentWarning.withValues(alpha: 0.1),
                    borderRadius: BorderRadius.circular(6),
                  ),
                  child: Text(
                    session.rejectionReason!,
                    style: const TextStyle(
                      fontSize: 11,
                      color: AppTheme.accentWarning,
                    ),
                  ),
                ),
              ],
            ],
          ),
        ),

        // 4. Seleção de Fallback MP 2.200-2 (se ainda não estiver assinado)
        if (!session.isSigned) ...[
          const SizedBox(height: 12),
          Container(
            padding: const EdgeInsets.all(12),
            decoration: BoxDecoration(
              color: AppTheme.darkSurface,
              borderRadius: BorderRadius.circular(10),
              border: Border.all(color: AppTheme.darkBorder),
            ),
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.stretch,
              children: [
                const Text(
                  'Contingência / Fallback Legal (MP 2.200-2)',
                  style: TextStyle(fontWeight: FontWeight.bold, fontSize: 12),
                ),
                const SizedBox(height: 4),
                const Text(
                  'Caso o titular não possua conta PIX ativa no ato, selecione uma via alternativa auditada:',
                  style: TextStyle(color: AppTheme.textSecondary, fontSize: 11),
                ),
                const SizedBox(height: 10),
                DropdownButtonFormField<FallbackMethodEnum>(
                  isExpanded: true,
                  initialValue: _selectedFallback,
                  decoration: const InputDecoration(
                    labelText: 'Modalidade Alternativa de Assinatura',
                    isDense: true,
                  ),
                  items: fallbackOptions
                      .map(
                        (m) => DropdownMenuItem<FallbackMethodEnum>(
                          value: m,
                          child: Text(
                            m.label,
                            overflow: TextOverflow.ellipsis,
                          ),
                        ),
                      )
                      .toList(),
                  onChanged: (val) {
                    if (val != null) setState(() => _selectedFallback = val);
                  },
                ),
                const SizedBox(height: 8),
                TextField(
                  controller: _fallbackJustificationCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Justificativa da Contingência (Auditável)',
                    hintText: 'Ex: Cliente optou por validação presencial/Gov.br',
                    isDense: true,
                  ),
                ),
                const SizedBox(height: 10),
                Align(
                  alignment: Alignment.centerRight,
                  child: OutlinedButton.icon(
                    onPressed: _isSubmittingFallback ? null : _applyFallback,
                    icon: _isSubmittingFallback
                        ? const SizedBox(
                            width: 14,
                            height: 14,
                            child: CircularProgressIndicator(strokeWidth: 2),
                          )
                        : const Icon(Icons.gavel_outlined, size: 15),
                    label: const Text(
                      'Registrar Fallback MP 2.200-2',
                      style: TextStyle(fontSize: 12),
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ],
    );
  }
}
