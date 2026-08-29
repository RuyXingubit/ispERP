<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Abertura de Chamado - Protocolo ANATEL</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 24px; margin: 0;">
  <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
    
    <h2 style="color: #0284c7; margin-top: 0;">Seu chamado foi registrado com sucesso!</h2>
    <p style="font-size: 14px; color: #475569;">
      Olá, <strong>${customerName}</strong>! Registramos sua solicitação de suporte técnico em nossa central.
    </p>

    <div style="background: #f1f5f9; border-radius: 8px; padding: 18px; margin: 20px 0;">
      <table style="width: 100%; border-collapse: collapse; font-size: 13px;">
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Protocolo Regulatório ANATEL:</td>
          <td style="padding: 6px 0; text-align: right; font-weight: bold; font-family: monospace; font-size: 14px; color: #0f172a;">${protocol}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Motivo / Categoria:</td>
          <td style="padding: 6px 0; text-align: right; font-weight: 500;">${category}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Assunto:</td>
          <td style="padding: 6px 0; text-align: right;">${subject}</td>
        </tr>
        <tr>
          <td style="padding: 6px 0; color: #64748b;">Prazo Máximo de Solução (SLA):</td>
          <td style="padding: 6px 0; text-align: right; font-weight: bold; color: #0284c7;">${slaDeadline}</td>
        </tr>
      </table>
    </div>

    <p style="font-size: 13px; color: #334155; line-height: 1.5;">
      Nossa equipe técnica já está analisando o seu caso. Você pode acompanhar o andamento ou interagir diretamente pelo portal do assinante.
    </p>

    <div style="text-align: center; margin-top: 24px;">
      <a href="${portalUrl!"https://isperp.local/portal"}" style="background: #0284c7; color: #ffffff; text-decoration: none; padding: 10px 20px; border-radius: 6px; font-weight: 500; font-size: 13px; display: inline-block;">
        Acompanhar Chamado Online
      </a>
    </div>
  </div>
</body>
</html>
