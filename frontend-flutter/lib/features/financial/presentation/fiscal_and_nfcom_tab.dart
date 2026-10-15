import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:flutter_riverpod/flutter_riverpod.dart';
import 'package:intl/intl.dart';
import '../../../core/theme/app_theme.dart';
import '../data/fiscal_repository.dart';

/// 6ª Aba do Cockpit Financeiro (`/financial`):
/// Módulo Fiscal & Conformidade Tributária — Empresa Fiscal, Certificado Digital A1,
/// NFCom (Modelo 62), Convênio ICMS 115/03 e Transição de Regime Tributário.
class FiscalAndNfcomTab extends ConsumerStatefulWidget {
  const FiscalAndNfcomTab({super.key});

  @override
  ConsumerState<FiscalAndNfcomTab> createState() => _FiscalAndNfcomTabState();
}

class _FiscalAndNfcomTabState extends ConsumerState<FiscalAndNfcomTab> {
  int _currentPage = 0;
  final _invoiceIdCtrl = TextEditingController();
  final _convYearCtrl =
      TextEditingController(text: DateTime.now().year.toString());
  final _convMonthCtrl =
      TextEditingController(text: DateTime.now().month.toString());

  bool _emittingNfcom = false;
  bool _exportingConvenio = false;
  bool _sendingAccounting = false;
  bool _processingTransitions = false;
  String? _convenioExportFeedback;

  @override
  void dispose() {
    _invoiceIdCtrl.dispose();
    _convYearCtrl.dispose();
    _convMonthCtrl.dispose();
    super.dispose();
  }

  String _formatCurrency(double val) {
    return NumberFormat.currency(locale: 'pt_BR', symbol: 'R\$').format(val);
  }

  Future<void> _handleEmitNfcom() async {
    final invoiceId = _invoiceIdCtrl.text.trim();
    if (invoiceId.isEmpty) return;

    setState(() => _emittingNfcom = true);
    try {
      final rec =
          await ref.read(fiscalRepositoryProvider).emitNfcom(invoiceId);
      ref.invalidate(nfcomRecordsPageProvider(_currentPage));
      ref.invalidate(activeFiscalCompanyProvider);
      if (mounted) {
        _invoiceIdCtrl.clear();
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'NFCom Nº ${rec.numero} (Série ${rec.serie}) — Status: ${rec.status} (${rec.chaveAcesso ?? "Sem chave"})',
            ),
            backgroundColor: rec.status == 'AUTORIZADA'
                ? AppTheme.accentGreen
                : AppTheme.accentWarning,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _emittingNfcom = false);
    }
  }

  Future<void> _handleExportConvenio115() async {
    final year = int.tryParse(_convYearCtrl.text.trim());
    final month = int.tryParse(_convMonthCtrl.text.trim());

    setState(() {
      _exportingConvenio = true;
      _convenioExportFeedback = null;
    });
    try {
      final bytes = await ref
          .read(fiscalRepositoryProvider)
          .exportConvenio115(year: year, month: month);
      if (mounted) {
        final y = year ?? DateTime.now().year;
        final m = (month ?? DateTime.now().month).toString().padLeft(2, '0');
        setState(() {
          _convenioExportFeedback =
              'Arquivo CONVENIO_115_${y}_$m.zip gerado (${bytes.length} bytes) com arquivos Mestre, Item, Destinatário e manifesto MD5.';
        });
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(
              'Lote CONVENIO_115_${y}_$m.zip gerado com sucesso (${bytes.length} bytes).',
            ),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _exportingConvenio = false);
    }
  }

  Future<void> _handleSendAccountingReport() async {
    final year = int.tryParse(_convYearCtrl.text.trim());
    final month = int.tryParse(_convMonthCtrl.text.trim());

    setState(() => _sendingAccounting = true);
    try {
      final res = await ref
          .read(fiscalRepositoryProvider)
          .sendAccountingReport(year: year, month: month);
      ref.invalidate(activeFiscalCompanyProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(res.message),
            backgroundColor:
                res.success ? AppTheme.accentGreen : AppTheme.accentWarning,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _sendingAccounting = false);
    }
  }

  Future<void> _handleProcessPendingTransitions() async {
    setState(() => _processingTransitions = true);
    try {
      final count = await ref
          .read(fiscalRepositoryProvider)
          .triggerProcessPendingTransitions();
      ref.invalidate(fiscalRegimeTransitionsProvider);
      ref.invalidate(activeFiscalCompanyProvider);
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text('$count transição(ões) pendente(s) processada(s).'),
            backgroundColor: AppTheme.accentGreen,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Text(e.toString().replaceFirst('Exception: ', '')),
            backgroundColor: AppTheme.accentError,
          ),
        );
      }
    } finally {
      if (mounted) setState(() => _processingTransitions = false);
    }
  }

  void _showFiscalCompanyDialog(FiscalCompanyModel? existing) {
    final cnpjCtrl = TextEditingController(text: existing?.cnpj ?? '');
    final razaoCtrl = TextEditingController(text: existing?.razaoSocial ?? '');
    final fantasiaCtrl =
        TextEditingController(text: existing?.nomeFantasia ?? '');
    final ieCtrl =
        TextEditingController(text: existing?.inscricaoEstadual ?? '');
    final cnaeCtrl =
        TextEditingController(text: existing?.cnaePrincipal ?? '6110-8/03');
    final logradouroCtrl =
        TextEditingController(text: existing?.logradouro ?? '');
    final numeroCtrl = TextEditingController(text: existing?.numero ?? '');
    final bairroCtrl = TextEditingController(text: existing?.bairro ?? '');
    final cidadeCtrl = TextEditingController(text: existing?.cidade ?? '');
    final ufCtrl = TextEditingController(text: existing?.uf ?? 'PA');
    final cepCtrl = TextEditingController(text: existing?.cep ?? '');
    final ibgeCtrl = TextEditingController(text: existing?.codigoIbge ?? '');
    final serieCtrl = TextEditingController(text: existing?.nfcomSerie ?? '1');
    final proxNumCtrl = TextEditingController(
        text: (existing?.nfcomProximoNumero ?? 1).toString());
    final icmsCtrl = TextEditingController(
        text: (existing?.aliquotaIcms ?? 0.0).toStringAsFixed(2));
    final pisCtrl = TextEditingController(
        text: (existing?.aliquotaPis ?? 0.65).toStringAsFixed(2));
    final cofinsCtrl = TextEditingController(
        text: (existing?.aliquotaCofins ?? 3.00).toStringAsFixed(2));
    final fustCtrl = TextEditingController(
        text: (existing?.aliquotaFust ?? 0.65).toStringAsFixed(2));
    final funttelCtrl = TextEditingController(
        text: (existing?.aliquotaFunttel ?? 0.50).toStringAsFixed(2));
    final accNameCtrl =
        TextEditingController(text: existing?.accountingName ?? '');
    final accEmailsCtrl =
        TextEditingController(text: existing?.accountingEmails ?? '');
    final accDayCtrl = TextEditingController(
        text: (existing?.accountingSendDay ?? 5).toString());

    String regime = existing?.regimeTributario ?? 'SIMPLES_NACIONAL';
    String ambiente = existing?.nfcomAmbiente ?? 'HOMOLOGACAO';
    bool autoSend = existing?.accountingAutoSend ?? true;
    bool fiscalConfirmed = existing?.fiscalConfirmed ?? true;

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: Text(existing == null
              ? 'Configurar Empresa Fiscal Emissora'
              : 'Editar Cadastro Fiscal & Parâmetros NFCom'),
          content: SizedBox(
            width: 600,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: cnpjCtrl,
                          decoration:
                              const InputDecoration(labelText: 'CNPJ *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: ieCtrl,
                          decoration: const InputDecoration(
                              labelText: 'Inscrição Estadual (IE) *'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: razaoCtrl,
                    decoration:
                        const InputDecoration(labelText: 'Razão Social *'),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: fantasiaCtrl,
                          decoration:
                              const InputDecoration(labelText: 'Nome Fantasia'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: cnaeCtrl,
                          decoration: const InputDecoration(
                              labelText: 'CNAE Principal (SCM)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          initialValue: regime,
                          decoration: const InputDecoration(
                              labelText: 'Regime Tributário'),
                          items: const [
                            DropdownMenuItem(
                              value: 'SIMPLES_NACIONAL',
                              child: Text('SIMPLES_NACIONAL'),
                            ),
                            DropdownMenuItem(
                              value: 'LUCRO_PRESUMIDO',
                              child: Text('LUCRO_PRESUMIDO'),
                            ),
                            DropdownMenuItem(
                              value: 'LUCRO_REAL',
                              child: Text('LUCRO_REAL'),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) setModalState(() => regime = v);
                          },
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: DropdownButtonFormField<String>(
                          initialValue: ambiente,
                          decoration: const InputDecoration(
                              labelText: 'Ambiente SEFAZ NFCom'),
                          items: const [
                            DropdownMenuItem(
                              value: 'HOMOLOGACAO',
                              child: Text('HOMOLOGACAO'),
                            ),
                            DropdownMenuItem(
                              value: 'PRODUCAO',
                              child: Text('PRODUCAO'),
                            ),
                          ],
                          onChanged: (v) {
                            if (v != null) setModalState(() => ambiente = v);
                          },
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: serieCtrl,
                          decoration:
                              const InputDecoration(labelText: 'Série NFCom'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: proxNumCtrl,
                          keyboardType: TextInputType.number,
                          decoration: const InputDecoration(
                              labelText: 'Próximo Número NFCom'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: icmsCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'ICMS (%)'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: pisCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'PIS (%)'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: cofinsCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'COFINS (%)'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: fustCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'FUST (%)'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: funttelCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'FUNTTEL (%)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        flex: 3,
                        child: TextField(
                          controller: logradouroCtrl,
                          decoration:
                              const InputDecoration(labelText: 'Logradouro *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: numeroCtrl,
                          decoration:
                              const InputDecoration(labelText: 'Número *'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: bairroCtrl,
                          decoration:
                              const InputDecoration(labelText: 'Bairro *'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: cidadeCtrl,
                          decoration:
                              const InputDecoration(labelText: 'Cidade *'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      SizedBox(
                        width: 70,
                        child: TextField(
                          controller: ufCtrl,
                          decoration: const InputDecoration(labelText: 'UF *'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: cepCtrl,
                          decoration: const InputDecoration(labelText: 'CEP *'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: ibgeCtrl,
                          decoration: const InputDecoration(
                              labelText: 'Código IBGE Município *'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: accNameCtrl,
                          decoration: const InputDecoration(
                              labelText: 'Escritório Contábil'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: accEmailsCtrl,
                          decoration: const InputDecoration(
                              labelText: 'E-mail(s) da Contabilidade'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      SizedBox(
                        width: 90,
                        child: TextField(
                          controller: accDayCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'Dia Envio'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 8),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text(
                        'Enviar fechamento fiscal mensal automaticamente para a contabilidade'),
                    value: autoSend,
                    onChanged: (v) =>
                        setModalState(() => autoSend = v ?? true),
                  ),
                  CheckboxListTile(
                    contentPadding: EdgeInsets.zero,
                    title: const Text(
                        'Parâmetros fiscais validados pelo contador responsável'),
                    value: fiscalConfirmed,
                    onChanged: (v) =>
                        setModalState(() => fiscalConfirmed = v ?? true),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                try {
                  await ref.read(fiscalRepositoryProvider).saveFiscalCompany(
                        id: existing?.id,
                        cnpj: cnpjCtrl.text.trim(),
                        razaoSocial: razaoCtrl.text.trim(),
                        nomeFantasia: fantasiaCtrl.text.trim().isEmpty
                            ? null
                            : fantasiaCtrl.text.trim(),
                        inscricaoEstadual: ieCtrl.text.trim(),
                        cnaePrincipal: cnaeCtrl.text.trim(),
                        regimeTributario: regime,
                        aliquotaIcms: double.tryParse(icmsCtrl.text.trim()),
                        aliquotaPis: double.tryParse(pisCtrl.text.trim()),
                        aliquotaCofins: double.tryParse(cofinsCtrl.text.trim()),
                        aliquotaFust: double.tryParse(fustCtrl.text.trim()),
                        aliquotaFunttel:
                            double.tryParse(funttelCtrl.text.trim()),
                        logradouro: logradouroCtrl.text.trim(),
                        numero: numeroCtrl.text.trim(),
                        bairro: bairroCtrl.text.trim(),
                        cidade: cidadeCtrl.text.trim(),
                        uf: ufCtrl.text.trim(),
                        cep: cepCtrl.text.trim(),
                        codigoIbge: ibgeCtrl.text.trim(),
                        nfcomAmbiente: ambiente,
                        nfcomSerie: serieCtrl.text.trim(),
                        nfcomProximoNumero:
                            int.tryParse(proxNumCtrl.text.trim()),
                        accountingName: accNameCtrl.text.trim().isEmpty
                            ? null
                            : accNameCtrl.text.trim(),
                        accountingEmails: accEmailsCtrl.text.trim().isEmpty
                            ? null
                            : accEmailsCtrl.text.trim(),
                        accountingSendDay: int.tryParse(accDayCtrl.text.trim()),
                        accountingAutoSend: autoSend,
                        fiscalConfirmed: fiscalConfirmed,
                      );
                  ref.invalidate(activeFiscalCompanyProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content:
                            Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Salvar Cadastro Fiscal'),
            ),
          ],
        ),
      ),
    );
  }

  void _showUploadCertificateDialog(FiscalCompanyModel company) {
    final filenameCtrl = TextEditingController(text: 'certificado_a1.pfx');
    final base64Ctrl = TextEditingController();
    final passwordCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: const Text('Upload do Certificado Digital A1 (.pfx / .p12)'),
        content: SizedBox(
          width: 480,
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Empresa: ${company.razaoSocial} (${company.cnpj})',
                  style: const TextStyle(
                      fontSize: 12, color: AppTheme.textSecondary),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: filenameCtrl,
                  decoration: const InputDecoration(
                    labelText: 'Nome do Arquivo (.pfx / .p12)',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: base64Ctrl,
                  maxLines: 3,
                  decoration: const InputDecoration(
                    labelText: 'Conteúdo do Certificado A1 (Base64 PKCS#12) *',
                    hintText: 'Cole aqui o conteúdo Base64 do arquivo .pfx/.p12',
                  ),
                ),
                const SizedBox(height: 12),
                TextField(
                  controller: passwordCtrl,
                  obscureText: true,
                  decoration: const InputDecoration(
                    labelText: 'Senha do Certificado A1 *',
                  ),
                ),
              ],
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Cancelar'),
          ),
          ElevatedButton.icon(
            onPressed: () async {
              final rawB64 = base64Ctrl.text.trim();
              final pwd = passwordCtrl.text.trim();
              if (rawB64.isEmpty || pwd.isEmpty) return;
              try {
                List<int> bytes;
                try {
                  bytes = base64Decode(rawB64);
                } catch (_) {
                  bytes = utf8.encode(rawB64);
                }
                final res = await ref
                    .read(fiscalRepositoryProvider)
                    .uploadFiscalCertificate(
                      companyId: company.id,
                      fileBytes: bytes,
                      filename: filenameCtrl.text.trim().isEmpty
                          ? 'certificado_a1.pfx'
                          : filenameCtrl.text.trim(),
                      password: pwd,
                    );
                ref.invalidate(activeFiscalCompanyProvider);
                if (ctx.mounted) {
                  Navigator.of(ctx).pop();
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(
                        res.success
                            ? (res.message ??
                                'Certificado A1 validado (${res.subjectName ?? ""})')
                            : (res.errorMessage ??
                                'Falha na validação do certificado'),
                      ),
                      backgroundColor: res.success
                          ? AppTheme.accentGreen
                          : AppTheme.accentError,
                    ),
                  );
                }
              } catch (e) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content:
                          Text(e.toString().replaceFirst('Exception: ', '')),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            icon: const Icon(Icons.verified_user_outlined, size: 18),
            label: const Text('Validar & Instalar A1'),
          ),
        ],
      ),
    );
  }

  void _showInspectNfcomModal(NfcomRecordModel rec) {
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: Text(
            'NFCom Modelo ${rec.modelo} — Nº ${rec.numero} / Série ${rec.serie}'),
        content: SizedBox(
          width: 620,
          child: SingleChildScrollView(
            child: Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              mainAxisSize: MainAxisSize.min,
              children: [
                Wrap(
                  spacing: 12,
                  runSpacing: 8,
                  children: [
                    Chip(label: Text('Status: ${rec.status}')),
                    Chip(label: Text('Ambiente: ${rec.ambiente}')),
                    Chip(
                      label: Text(
                        'Base SCM (NFCom): ${_formatCurrency(rec.scmTaxableAmount > 0 ? rec.scmTaxableAmount : rec.valorTotal)}',
                      ),
                    ),
                    if (rec.svaExemptAmount > 0)
                      Chip(
                        label: Text(
                          'Isento SVA/Locação: ${_formatCurrency(rec.svaExemptAmount)}',
                        ),
                      ),
                    Chip(
                        label: Text(
                            'ICMS: ${_formatCurrency(rec.valorIcms)} | FUST: ${_formatCurrency(rec.valorFust)} | FUNTTEL: ${_formatCurrency(rec.valorFunttel)}')),
                  ],
                ),
                const SizedBox(height: 12),
                SelectableText(
                  'Chave de Acesso (44 dígitos): ${rec.chaveAcesso ?? "N/A"}',
                  style: const TextStyle(
                      fontFamily: 'monospace',
                      fontSize: 12,
                      fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 4),
                Text(
                  'Protocolo SEFAZ: ${rec.protocoloAutorizacao ?? "N/A"} • Autorização: ${rec.dataAutorizacao ?? "N/A"}',
                  style: const TextStyle(
                      fontSize: 12, color: AppTheme.textSecondary),
                ),
                if (rec.danfePdfUrl != null) ...[
                  const SizedBox(height: 4),
                  SelectableText(
                    'DANFE-COM URL: ${rec.danfePdfUrl}',
                    style: const TextStyle(
                        fontSize: 12, color: AppTheme.primaryBlue),
                  ),
                ],
                if (rec.motivoCancelamento != null) ...[
                  const SizedBox(height: 8),
                  Text(
                    'Cancelamento (${rec.dataCancelamento ?? ""}): ${rec.motivoCancelamento}',
                    style: const TextStyle(
                        fontSize: 12, color: AppTheme.accentError),
                  ),
                ],
                const Divider(height: 24),
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'XML Autorizado NFCom (Mod. 62):',
                      style:
                          TextStyle(fontSize: 13, fontWeight: FontWeight.bold),
                    ),
                    if (rec.xmlAutorizado != null &&
                        rec.xmlAutorizado!.isNotEmpty)
                      TextButton.icon(
                        onPressed: () {
                          Clipboard.setData(
                              ClipboardData(text: rec.xmlAutorizado!));
                          ScaffoldMessenger.of(ctx).showSnackBar(
                            const SnackBar(
                              content:
                                  Text('XML copiado para a área de transferência.'),
                              backgroundColor: AppTheme.accentGreen,
                            ),
                          );
                        },
                        icon: const Icon(Icons.copy, size: 16),
                        label: const Text('Copiar XML'),
                      ),
                  ],
                ),
                const SizedBox(height: 6),
                Container(
                  width: double.infinity,
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: AppTheme.darkBg,
                    borderRadius: BorderRadius.circular(8),
                    border: Border.all(color: AppTheme.darkCard),
                  ),
                  child: SelectableText(
                    rec.xmlAutorizado ??
                        'Nenhum XML armazenado para este registro.',
                    style: const TextStyle(
                        fontFamily: 'monospace',
                        fontSize: 11,
                        color: AppTheme.textSecondary),
                  ),
                ),
              ],
            ),
          ),
        ),
        actions: [
          ElevatedButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Fechar'),
          ),
        ],
      ),
    );
  }

  void _showCancelNfcomDialog(NfcomRecordModel rec) {
    final reasonCtrl = TextEditingController(
      text: 'Cancelamento solicitado pelo provedor dentro do prazo legal SEFAZ',
    );
    showDialog(
      context: context,
      builder: (ctx) => AlertDialog(
        backgroundColor: AppTheme.darkSurface,
        title: Text('Cancelar NFCom Nº ${rec.numero} (Série ${rec.serie})'),
        content: SizedBox(
          width: 440,
          child: TextField(
            controller: reasonCtrl,
            maxLines: 2,
            decoration: const InputDecoration(
              labelText: 'Justificativa de Cancelamento SEFAZ (mín. 15 caracteres) *',
            ),
          ),
        ),
        actions: [
          TextButton(
            onPressed: () => Navigator.of(ctx).pop(),
            child: const Text('Voltar'),
          ),
          ElevatedButton(
            style: ElevatedButton.styleFrom(
              backgroundColor: AppTheme.accentError,
              foregroundColor: Colors.white,
            ),
            onPressed: () async {
              try {
                final res =
                    await ref.read(fiscalRepositoryProvider).cancelNfcom(
                          recordId: rec.id,
                          reason: reasonCtrl.text.trim(),
                        );
                ref.invalidate(nfcomRecordsPageProvider(_currentPage));
                if (ctx.mounted) {
                  Navigator.of(ctx).pop();
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content: Text(
                        res.success
                            ? 'NFCom cancelada (Protocolo: ${res.protocoloCancelamento ?? "OK"})'
                            : (res.errorMessage ?? 'Erro ao cancelar NFCom'),
                      ),
                      backgroundColor: res.success
                          ? AppTheme.accentGreen
                          : AppTheme.accentError,
                    ),
                  );
                }
              } catch (e) {
                if (ctx.mounted) {
                  ScaffoldMessenger.of(ctx).showSnackBar(
                    SnackBar(
                      content:
                          Text(e.toString().replaceFirst('Exception: ', '')),
                      backgroundColor: AppTheme.accentError,
                    ),
                  );
                }
              }
            },
            child: const Text('Confirmar Cancelamento SEFAZ'),
          ),
        ],
      ),
    );
  }

  void _showScheduleRegimeTransitionDialog(FiscalCompanyModel? company) {
    String newRegime = 'LUCRO_PRESUMIDO';
    final dateCtrl = TextEditingController(
      text: '${DateTime.now().year + 1}-01-01',
    );
    final icmsCtrl = TextEditingController(text: '18.00');
    final pisCtrl = TextEditingController(text: '0.65');
    final cofinsCtrl = TextEditingController(text: '3.00');
    final fustCtrl = TextEditingController(text: '0.65');
    final funttelCtrl = TextEditingController(text: '0.50');
    final notesCtrl = TextEditingController();

    showDialog(
      context: context,
      builder: (ctx) => StatefulBuilder(
        builder: (context, setModalState) => AlertDialog(
          backgroundColor: AppTheme.darkSurface,
          title: const Text('Agendar / Aplicar Transição de Regime Tributário'),
          content: SizedBox(
            width: 480,
            child: SingleChildScrollView(
              child: Column(
                mainAxisSize: MainAxisSize.min,
                children: [
                  DropdownButtonFormField<String>(
                    initialValue: newRegime,
                    decoration: const InputDecoration(
                        labelText: 'Novo Regime Tributário *'),
                    items: const [
                      DropdownMenuItem(
                        value: 'SIMPLES_NACIONAL',
                        child: Text('SIMPLES_NACIONAL'),
                      ),
                      DropdownMenuItem(
                        value: 'LUCRO_PRESUMIDO',
                        child: Text('LUCRO_PRESUMIDO'),
                      ),
                      DropdownMenuItem(
                        value: 'LUCRO_REAL',
                        child: Text('LUCRO_REAL'),
                      ),
                    ],
                    onChanged: (v) {
                      if (v != null) setModalState(() => newRegime = v);
                    },
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: dateCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Data Efetiva da Virada (YYYY-MM-DD) *',
                    ),
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: icmsCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'ICMS (%)'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: pisCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'PIS (%)'),
                        ),
                      ),
                      const SizedBox(width: 8),
                      Expanded(
                        child: TextField(
                          controller: cofinsCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'COFINS (%)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  Row(
                    children: [
                      Expanded(
                        child: TextField(
                          controller: fustCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'FUST (%)'),
                        ),
                      ),
                      const SizedBox(width: 12),
                      Expanded(
                        child: TextField(
                          controller: funttelCtrl,
                          keyboardType: TextInputType.number,
                          decoration:
                              const InputDecoration(labelText: 'FUNTTEL (%)'),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 12),
                  TextField(
                    controller: notesCtrl,
                    decoration: const InputDecoration(
                      labelText: 'Motivo / Observação Contábil',
                    ),
                  ),
                ],
              ),
            ),
          ),
          actions: [
            TextButton(
              onPressed: () => Navigator.of(ctx).pop(),
              child: const Text('Cancelar'),
            ),
            ElevatedButton(
              onPressed: () async {
                try {
                  await ref
                      .read(fiscalRepositoryProvider)
                      .scheduleOrApplyTransition(
                        companyId: company?.id,
                        newRegime: newRegime,
                        effectiveDate: dateCtrl.text.trim(),
                        aliquotaIcms: double.tryParse(icmsCtrl.text.trim()),
                        aliquotaPis: double.tryParse(pisCtrl.text.trim()),
                        aliquotaCofins: double.tryParse(cofinsCtrl.text.trim()),
                        aliquotaFust: double.tryParse(fustCtrl.text.trim()),
                        aliquotaFunttel:
                            double.tryParse(funttelCtrl.text.trim()),
                        notes: notesCtrl.text.trim().isEmpty
                            ? null
                            : notesCtrl.text.trim(),
                      );
                  ref.invalidate(fiscalRegimeTransitionsProvider);
                  ref.invalidate(activeFiscalCompanyProvider);
                  if (ctx.mounted) Navigator.of(ctx).pop();
                } catch (e) {
                  if (ctx.mounted) {
                    ScaffoldMessenger.of(ctx).showSnackBar(
                      SnackBar(
                        content:
                            Text(e.toString().replaceFirst('Exception: ', '')),
                        backgroundColor: AppTheme.accentError,
                      ),
                    );
                  }
                }
              },
              child: const Text('Salvar Transição'),
            ),
          ],
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    final companyAsync = ref.watch(activeFiscalCompanyProvider);
    final gatewayAsync = ref.watch(activeFiscalGatewayConfigProvider);
    final recordsPageAsync = ref.watch(nfcomRecordsPageProvider(_currentPage));
    final transitionsAsync = ref.watch(fiscalRegimeTransitionsProvider);

    return SingleChildScrollView(
      padding: const EdgeInsets.all(20),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // 1. Cabeçalho & Empresa Fiscal Ativa
          companyAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Card(
              child: Padding(
                padding: const EdgeInsets.all(16),
                child: Text(
                  err.toString().replaceFirst('Exception: ', ''),
                  style: const TextStyle(color: AppTheme.accentError),
                ),
              ),
            ),
            data: (company) => _buildFiscalCompanyBanner(
              company,
              gatewayAsync.valueOrNull,
            ),
          ),
          const SizedBox(height: 20),

          // 2. Linha de Operação: Emissão NFCom Mod. 62 + Convênio ICMS 115/03
          LayoutBuilder(
            builder: (context, constraints) {
              final isWide = constraints.maxWidth > 860;
              if (isWide) {
                return Row(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Expanded(child: _buildEmitNfcomCard()),
                    const SizedBox(width: 16),
                    Expanded(child: _buildConvenio115Card()),
                  ],
                );
              }
              return Column(
                children: [
                  _buildEmitNfcomCard(),
                  const SizedBox(height: 16),
                  _buildConvenio115Card(),
                ],
              );
            },
          ),
          const SizedBox(height: 24),

          // 3. Painel Paginado de Notas Fiscais NFCom Modelo 62
          const Text(
            'Notas Fiscais Emitidas — NFCom (Modelo 62 SEFAZ)',
            style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
          ),
          const SizedBox(height: 10),
          recordsPageAsync.when(
            loading: () => const LinearProgressIndicator(),
            error: (err, _) => Text(
              err.toString().replaceFirst('Exception: ', ''),
              style: const TextStyle(color: AppTheme.accentError),
            ),
            data: (pageData) => _buildNfcomRecordsSection(pageData),
          ),
          const SizedBox(height: 24),

          // 4. Transições de Regime Tributário
          _buildRegimeTransitionsSection(
            companyAsync.valueOrNull,
            transitionsAsync,
          ),
        ],
      ),
    );
  }

  Widget _buildFiscalCompanyBanner(
    FiscalCompanyModel? company,
    FiscalGatewayConfigModel? gateway,
  ) {
    if (company == null) {
      return Card(
        child: Padding(
          padding: const EdgeInsets.all(20),
          child: Wrap(
            spacing: 16,
            runSpacing: 12,
            alignment: WrapAlignment.spaceBetween,
            crossAxisAlignment: WrapCrossAlignment.center,
            children: [
              const Column(
                crossAxisAlignment: CrossAxisAlignment.start,
                children: [
                  Text(
                    'Nenhuma Empresa Fiscal Ativa Configurada',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                  ),
                  SizedBox(height: 4),
                  Text(
                    'Configure o CNPJ, Inscrição Estadual, Regime Tributário e Certificado Digital A1 para habilitar a emissão de NFCom (Mod. 62).',
                    style:
                        TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                  ),
                ],
              ),
              ElevatedButton.icon(
                onPressed: () => _showFiscalCompanyDialog(null),
                icon: const Icon(Icons.domain_add, size: 18),
                label: const Text('Configurar Empresa Fiscal'),
              ),
            ],
          ),
        ),
      );
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Wrap(
              spacing: 12,
              runSpacing: 12,
              alignment: WrapAlignment.spaceBetween,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Text(
                      '${company.razaoSocial} (${company.cnpj})',
                      style: const TextStyle(
                          fontSize: 17, fontWeight: FontWeight.bold),
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'IE: ${company.inscricaoEstadual} • CNAE: ${company.cnaePrincipal} • Município: ${company.cidade}/${company.uf} (IBGE ${company.codigoIbge})',
                      style: const TextStyle(
                          fontSize: 12, color: AppTheme.textSecondary),
                    ),
                  ],
                ),
                Wrap(
                  spacing: 8,
                  runSpacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () => _showUploadCertificateDialog(company),
                      icon: Icon(
                        Icons.verified_user_outlined,
                        size: 16,
                        color: company.hasCertificate
                            ? AppTheme.accentGreen
                            : AppTheme.accentWarning,
                      ),
                      label: Text(company.hasCertificate
                          ? 'Renovar Certificado A1'
                          : 'Instalar Certificado A1'),
                    ),
                    ElevatedButton.icon(
                      onPressed: () => _showFiscalCompanyDialog(company),
                      icon: const Icon(Icons.edit_outlined, size: 16),
                      label: const Text('Editar Parâmetros Fiscais'),
                    ),
                  ],
                ),
              ],
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 10,
              runSpacing: 8,
              children: [
                Chip(
                  avatar: const Icon(Icons.account_balance,
                      size: 16, color: AppTheme.primaryBlue),
                  label: Text('Regime: ${company.regimeTributario}'),
                ),
                Chip(
                  avatar: Icon(
                    Icons.cloud_done_outlined,
                    size: 16,
                    color: company.nfcomAmbiente == 'PRODUCAO'
                        ? AppTheme.accentGreen
                        : AppTheme.accentWarning,
                  ),
                  label: Text(
                      'NFCom Mod. 62: ${company.nfcomAmbiente} (Série ${company.nfcomSerie} • Próx. Nº ${company.nfcomProximoNumero})'),
                ),
                Chip(
                  avatar: Icon(
                    company.hasCertificate
                        ? Icons.lock_outline
                        : Icons.lock_open,
                    size: 16,
                    color: company.hasCertificate
                        ? AppTheme.accentGreen
                        : AppTheme.accentError,
                  ),
                  label: Text(company.hasCertificate
                      ? 'Certificado A1 Instalado (Val: ${company.certificateExpiresAt ?? "Ativo"})'
                      : 'Certificado A1 Pendente'),
                ),
                Chip(
                  label: Text(
                      'Alíquotas: ICMS ${company.aliquotaIcms.toStringAsFixed(2)}% | PIS ${company.aliquotaPis.toStringAsFixed(2)}% | COFINS ${company.aliquotaCofins.toStringAsFixed(2)}% | FUST ${company.aliquotaFust.toStringAsFixed(2)}% | FUNTTEL ${company.aliquotaFunttel.toStringAsFixed(2)}%'),
                ),
                if (gateway != null)
                  Chip(
                    label: Text(
                        'Emissor: ${gateway.gatewayType} (${gateway.environment})'),
                  ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildEmitNfcomCard() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Emissão Direta de NFCom (Modelo 62)',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              'Assina o XML com o Certificado Digital A1 e transmite para autorização na SEFAZ',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 12,
              runSpacing: 10,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                SizedBox(
                  width: 280,
                  child: TextField(
                    controller: _invoiceIdCtrl,
                    decoration: const InputDecoration(
                      labelText: 'ID da Fatura (UUID)',
                      hintText: 'Ex: 01912e73-b789-7000-8000-...',
                      isDense: true,
                    ),
                  ),
                ),
                ElevatedButton.icon(
                  onPressed: _emittingNfcom ? null : _handleEmitNfcom,
                  icon: const Icon(Icons.receipt_long_outlined, size: 18),
                  label: Text(_emittingNfcom
                      ? 'Transmitindo SEFAZ...'
                      : 'Transmitir NFCom Mod. 62'),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildConvenio115Card() {
    return Card(
      child: Padding(
        padding: const EdgeInsets.all(18),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            const Text(
              'Convênio ICMS 115/03 & Fechamento Contábil',
              style: TextStyle(fontSize: 15, fontWeight: FontWeight.bold),
            ),
            const SizedBox(height: 4),
            const Text(
              'Exportação dos arquivos Mestre, Item e Destinatário (MD5) e envio à contabilidade',
              style: TextStyle(fontSize: 12, color: AppTheme.textSecondary),
            ),
            const SizedBox(height: 12),
            Wrap(
              spacing: 10,
              runSpacing: 10,
              crossAxisAlignment: WrapCrossAlignment.center,
              children: [
                SizedBox(
                  width: 90,
                  child: TextField(
                    controller: _convYearCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Ano',
                      isDense: true,
                    ),
                  ),
                ),
                SizedBox(
                  width: 80,
                  child: TextField(
                    controller: _convMonthCtrl,
                    keyboardType: TextInputType.number,
                    decoration: const InputDecoration(
                      labelText: 'Mês',
                      isDense: true,
                    ),
                  ),
                ),
                OutlinedButton.icon(
                  onPressed:
                      _exportingConvenio ? null : _handleExportConvenio115,
                  icon: const Icon(Icons.folder_zip_outlined, size: 18),
                  label: Text(_exportingConvenio
                      ? 'Gerando ZIP...'
                      : 'Exportar ZIP Convênio 115'),
                ),
                ElevatedButton.icon(
                  onPressed:
                      _sendingAccounting ? null : _handleSendAccountingReport,
                  icon: const Icon(Icons.send_outlined, size: 18),
                  label: Text(_sendingAccounting
                      ? 'Enviando...'
                      : 'Enviar Fechamento Contábil'),
                ),
              ],
            ),
            if (_convenioExportFeedback != null) ...[
              const SizedBox(height: 10),
              Text(
                _convenioExportFeedback!,
                style: const TextStyle(
                    fontSize: 12,
                    color: AppTheme.accentGreen,
                    fontWeight: FontWeight.w600),
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildNfcomRecordsSection(NfcomRecordsPageModel pageData) {
    if (pageData.content.isEmpty) {
      return const Card(
        child: Padding(
          padding: EdgeInsets.all(18),
          child: Text(
            'Nenhuma NFCom Modelo 62 emitida até o momento.',
            style: TextStyle(color: AppTheme.textSecondary),
          ),
        ),
      );
    }

    return Card(
      child: Padding(
        padding: const EdgeInsets.all(12),
        child: Column(
          children: [
            ...pageData.content.map((rec) {
              final isAuthorized = rec.status == 'AUTORIZADA';
              final isCancelled = rec.status == 'CANCELADA';
              final badgeColor = isAuthorized
                  ? AppTheme.accentGreen
                  : (isCancelled
                      ? AppTheme.accentError
                      : AppTheme.accentWarning);

              return ListTile(
                leading: Icon(
                  isAuthorized
                      ? Icons.verified_outlined
                      : (isCancelled
                          ? Icons.cancel_outlined
                          : Icons.pending_outlined),
                  color: badgeColor,
                ),
                title: Text(
                  'NFCom Nº ${rec.numero} (Série ${rec.serie} • Mod. ${rec.modelo}) — Base SCM: ${_formatCurrency(rec.scmTaxableAmount > 0 ? rec.scmTaxableAmount : rec.valorTotal)}${rec.svaExemptAmount > 0 ? " (+ SVA/Locação Isento: ${_formatCurrency(rec.svaExemptAmount)})" : ""}',
                  style: const TextStyle(
                      fontSize: 13, fontWeight: FontWeight.bold),
                ),
                subtitle: Text(
                  'Status: ${rec.status} (${rec.ambiente}) • Chave: ${rec.chaveAcesso ?? "-"} • Prot.: ${rec.protocoloAutorizacao ?? "-"}',
                  style: const TextStyle(
                      fontSize: 11, color: AppTheme.textSecondary),
                ),
                trailing: Wrap(
                  spacing: 8,
                  children: [
                    OutlinedButton.icon(
                      onPressed: () => _showInspectNfcomModal(rec),
                      icon: const Icon(Icons.code, size: 16),
                      label: const Text('XML / DANFE'),
                    ),
                    if (isAuthorized)
                      TextButton(
                        onPressed: () => _showCancelNfcomDialog(rec),
                        child: const Text(
                          'Cancelar SEFAZ',
                          style: TextStyle(color: AppTheme.accentError),
                        ),
                      ),
                  ],
                ),
              );
            }),
            if (pageData.totalPages > 1) ...[
              const Divider(),
              Wrap(
                spacing: 12,
                crossAxisAlignment: WrapCrossAlignment.center,
                alignment: WrapAlignment.end,
                children: [
                  Text(
                    'Página ${pageData.number + 1} de ${pageData.totalPages} (${pageData.totalElements} notas)',
                    style: const TextStyle(
                        fontSize: 12, color: AppTheme.textSecondary),
                  ),
                  TextButton(
                    onPressed: _currentPage > 0
                        ? () => setState(() => _currentPage--)
                        : null,
                    child: const Text('Anterior'),
                  ),
                  TextButton(
                    onPressed: (_currentPage + 1) < pageData.totalPages
                        ? () => setState(() => _currentPage++)
                        : null,
                    child: const Text('Próxima'),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }

  Widget _buildRegimeTransitionsSection(
    FiscalCompanyModel? company,
    AsyncValue<List<FiscalRegimeTransitionModel>> transitionsAsync,
  ) {
    return Column(
      crossAxisAlignment: CrossAxisAlignment.start,
      children: [
        Wrap(
          spacing: 12,
          runSpacing: 10,
          alignment: WrapAlignment.spaceBetween,
          crossAxisAlignment: WrapCrossAlignment.center,
          children: [
            const Column(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text(
                  'Transições de Regime Tributário (Simples Nacional ↔ Lucro Presumido / Real)',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                SizedBox(height: 2),
                Text(
                  'Agendamento de virada de exercício fiscal e atualização automática de alíquotas',
                  style:
                      TextStyle(fontSize: 12, color: AppTheme.textSecondary),
                ),
              ],
            ),
            Wrap(
              spacing: 8,
              runSpacing: 8,
              children: [
                OutlinedButton.icon(
                  onPressed: _processingTransitions
                      ? null
                      : _handleProcessPendingTransitions,
                  icon: const Icon(Icons.sync, size: 16),
                  label: Text(_processingTransitions
                      ? 'Processando...'
                      : 'Processar Vencidas'),
                ),
                ElevatedButton.icon(
                  onPressed: () =>
                      _showScheduleRegimeTransitionDialog(company),
                  icon: const Icon(Icons.event_repeat_outlined, size: 18),
                  label: const Text('Agendar Transição de Regime'),
                ),
              ],
            ),
          ],
        ),
        const SizedBox(height: 10),
        transitionsAsync.when(
          loading: () => const LinearProgressIndicator(),
          error: (err, _) => Text(
            err.toString().replaceFirst('Exception: ', ''),
            style: const TextStyle(color: AppTheme.accentError),
          ),
          data: (transitions) {
            if (transitions.isEmpty) {
              return const Card(
                child: Padding(
                  padding: EdgeInsets.all(16),
                  child: Text(
                    'Nenhuma transição de regime tributário agendada ou registrada.',
                    style: TextStyle(color: AppTheme.textSecondary),
                  ),
                ),
              );
            }
            return Card(
              child: Column(
                children: transitions.map((tr) {
                  final isScheduled = tr.status == 'SCHEDULED';
                  return ListTile(
                    leading: Icon(
                      isScheduled
                          ? Icons.schedule_outlined
                          : (tr.status == 'APPLIED'
                              ? Icons.check_circle_outline
                              : Icons.cancel_outlined),
                      color: isScheduled
                          ? AppTheme.accentWarning
                          : (tr.status == 'APPLIED'
                              ? AppTheme.accentGreen
                              : AppTheme.textMuted),
                    ),
                    title: Text(
                      '${tr.previousRegime}  ➔  ${tr.newRegime} (Vigência: ${tr.effectiveDate})',
                      style: const TextStyle(
                          fontSize: 13, fontWeight: FontWeight.bold),
                    ),
                    subtitle: Text(
                      'Status: ${tr.status} • ICMS ${tr.aliquotaIcms ?? 0}% | PIS ${tr.aliquotaPis ?? 0}% | COFINS ${tr.aliquotaCofins ?? 0}% • ${tr.notes ?? ""}',
                      style: const TextStyle(
                          fontSize: 11, color: AppTheme.textSecondary),
                    ),
                    trailing: isScheduled && tr.id != null
                        ? TextButton(
                            onPressed: () async {
                              await ref
                                  .read(fiscalRepositoryProvider)
                                  .cancelTransition(tr.id!);
                              ref.invalidate(fiscalRegimeTransitionsProvider);
                            },
                            child: const Text(
                              'Cancelar Agendamento',
                              style: TextStyle(color: AppTheme.accentError),
                            ),
                          )
                        : null,
                  );
                }).toList(),
              ),
            );
          },
        ),
      ],
    );
  }
}
