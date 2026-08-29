<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Desbloqueio em Confiança Ativado</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 24px; margin: 0;">
  <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
    
    <div style="text-align: center; margin-bottom: 24px;">
      <div style="font-size: 32px; margin-bottom: 8px;">🎉</div>
      <h2 style="color: #16a34a; margin: 0 0 8px 0;">Seu sinal foi reativado com sucesso!</h2>
      <p style="color: #64748b; font-size: 14px; margin: 0;">Olá, <strong>${customerName}</strong>! Seu pedido de Desbloqueio em Confiança foi aprovado.</p>
    </div>

    <div style="background: #f0fdf4; border-radius: 8px; border: 1px solid #bbf7d0; padding: 18px; margin: 24px 0; text-align: center;">
      <span style="font-size: 12px; color: #15803d; text-transform: uppercase; font-weight: bold;">Período Provisório de Conexão</span>
      <div style="font-size: 24px; font-weight: bold; color: #14532d; margin: 6px 0;">48 Horas</div>
      <span style="font-size: 13px; color: #166534;">Válido até: <strong>${expirationDateTime}</strong></span>
    </div>

    <p style="font-size: 13px; color: #334155; line-height: 1.5;">
      Aproveite esse período para efetuar a quitação da sua fatura via Pix instantâneo para evitar nova suspensão automática do sinal.
    </p>

    <div style="text-align: center; margin-top: 24px;">
      <a href="${portalUrl!"https://isperp.local/portal"}" style="background: #16a34a; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: bold; font-size: 14px; display: inline-block;">
        Pagar Fatura com Pix 2ª Via
      </a>
    </div>
  </div>
</body>
</html>
