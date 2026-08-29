<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Código de Segurança para Alteração de Plano</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 24px; margin: 0;">
  <div style="max-width: 540px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
    
    <div style="text-align: center; margin-bottom: 24px;">
      <h2 style="color: #0f172a; margin: 0 0 8px 0;">Confirmação de Upgrade de Plano</h2>
      <p style="color: #64748b; font-size: 14px; margin: 0;">Olá, <strong>${customerName}</strong>! Use o código abaixo para confirmar a troca do seu plano:</p>
    </div>

    <div style="background: #f1f5f9; border-radius: 12px; padding: 24px; text-align: center; margin: 24px 0;">
      <span style="font-size: 12px; font-weight: bold; color: #64748b; text-transform: uppercase; letter-spacing: 1px;">Código de Verificação (2FA)</span>
      <div style="font-size: 36px; font-weight: 800; letter-spacing: 8px; color: #0284c7; margin: 12px 0; font-family: monospace;">
        ${verificationCode}
      </div>
      <span style="font-size: 12px; color: #dc2626;">⏱️ Este código expira em 10 minutos.</span>
    </div>

    <div style="background: #f8fafc; border-radius: 8px; padding: 14px; font-size: 13px; color: #475569; margin-bottom: 24px;">
      <strong>Plano Solicitado:</strong> ${targetPlanName!"Super Fibra"}<br>
      <strong>Novo Valor Mensal:</strong> R$ ${targetPlanPrice!"99,90"}<br>
      <strong>Data da Solicitação:</strong> ${requestDateTime}
    </div>

    <p style="font-size: 12px; color: #94a3b8; text-align: center; margin: 0;">
      Se você não solicitou esta alteração, por favor ignore este e-mail e entre em contato imediatamente com o suporte.
    </p>
  </div>
</body>
</html>
