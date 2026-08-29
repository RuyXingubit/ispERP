<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Fechamento Fiscal - Convênio ICMS 115/03</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 24px; margin: 0;">
  <div style="max-width: 620px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.05);">
    
    <div style="border-bottom: 2px solid #0284c7; padding-bottom: 16px; margin-bottom: 24px;">
      <h2 style="color: #0369a1; margin: 0; font-size: 20px;">ispERP • Fechamento Fiscal Mensal</h2>
      <p style="color: #64748b; font-size: 13px; margin: 4px 0 0 0;">Arquivos Magnéticos do Convênio ICMS 115/03 & Demonstrativo NFCom</p>
    </div>

    <p style="font-size: 15px; line-height: 1.5;">Olá, <strong>${accountingName!"Equipe de Assessoria Contábil"}</strong>,</p>
    
    <p style="font-size: 14px; line-height: 1.6; color: #334155;">
      Disponibilizamos em anexo os arquivos fiscais magnéticos referentes ao período de apuração de <strong>${mesReferencia}</strong> da empresa <strong>${empresa.razaoSocial}</strong> (CNPJ: ${empresa.cnpj} • I.E.: ${empresa.inscricaoEstadual}).
    </p>

    <!-- Resumo dos Valores -->
    <div style="background: #f1f5f9; border-radius: 8px; padding: 18px; margin: 24px 0;">
      <h3 style="margin: 0 0 12px 0; font-size: 14px; text-transform: uppercase; letter-spacing: 0.5px; color: #475569;">Resumo da Apuração Mensal</h3>
      <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Volume de Faturas / NFCom:</td>
          <td style="padding: 6px 0; text-align: right; font-weight: bold;">${totalFaturas}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Faturamento Bruto (SCM):</td>
          <td style="padding: 6px 0; text-align: right; font-weight: bold; color: #059669; font-size: 15px;">R$ ${totalFaturado}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">ICMS Apurado:</td>
          <td style="padding: 6px 0; text-align: right; font-weight: bold;">R$ ${totalIcms}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Alíquota FUST (0,65%):</td>
          <td style="padding: 6px 0; text-align: right;">R$ ${totalFust}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Alíquota FUNTTEL (0,50%):</td>
          <td style="padding: 6px 0; text-align: right;">R$ ${totalFunttel}</td>
        </tr>
      </table>
    </div>

    <!-- Hashes de Integridade -->
    <div style="border-left: 4px solid #0284c7; background: #f0f9ff; padding: 12px 16px; margin-bottom: 24px; font-size: 12px;">
      <p style="margin: 0 0 4px 0; font-weight: bold; color: #0369a1;">Autenticação Digital (Hashes MD5 SEFAZ):</p>
      <div style="font-family: monospace; color: #0c4a6e;">• Mestre (M): ${md5Mestre}</div>
      <div style="font-family: monospace; color: #0c4a6e;">• Itens (I): ${md5Item}</div>
      <div style="font-family: monospace; color: #0c4a6e;">• Destinatários (D): ${md5Destinatario}</div>
    </div>

    <div style="background: #ecfdf5; border-radius: 8px; padding: 12px 16px; margin-bottom: 24px; font-size: 13px; color: #065f46; display: flex; align-items: center;">
      <span>📦 <strong>Arquivo Anexado:</strong> <code>${nomeArquivoZip}</code> (contendo os arquivos .M, .I, .D e .C).</span>
    </div>

    <p style="font-size: 12px; color: #94a3b8; border-top: 1px solid #e2e8f0; padding-top: 16px; margin-top: 24px;">
      Este é um e-mail automatizado gerado pelo <strong>ispERP</strong>. Dúvidas fiscais podem ser tratadas respondendo a este e-mail.
    </p>
  </div>
</body>
</html>
