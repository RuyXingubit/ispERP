<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Sua Fatura de Internet está Disponível</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 24px; margin: 0;">
  <div style="max-width: 600px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
    
    <h2 style="color: #0284c7; margin-top: 0;">Olá, ${customerName}!</h2>
    <p style="font-size: 14px; color: #475569;">
      A sua fatura do plano de internet <strong>${planName!"Fibra Óptica"}</strong> referente ao vencimento <strong>${dueDate}</strong> já está disponível.
    </p>

    <div style="background: #f8fafc; border: 1px dashed #cbd5e1; border-radius: 8px; padding: 20px; text-align: center; margin: 24px 0;">
      <span style="font-size: 13px; color: #64748b; text-transform: uppercase;">Valor da Fatura</span>
      <div style="font-size: 28px; font-weight: bold; color: #0f172a; margin: 6px 0;">R$ ${amount}</div>
      <span style="font-size: 13px; color: #0284c7; font-weight: 500;">Vencimento: ${dueDate}</span>
    </div>

    <#if pixCopiaECola??>
    <div style="background: #f0fdf4; border-radius: 8px; padding: 16px; margin-bottom: 20px;">
      <h4 style="margin: 0 0 8px 0; color: #166534; font-size: 14px;">⚡ Pague instantaneamente via Pix:</h4>
      <p style="font-size: 12px; color: #15803d; margin: 0 0 8px 0;">Copie o código abaixo e cole no aplicativo do seu banco:</p>
      <div style="background: #ffffff; border: 1px solid #bbf7d0; padding: 10px; font-family: monospace; font-size: 11px; word-break: break-all; color: #14532d; border-radius: 6px;">
        ${pixCopiaECola}
      </div>
    </div>
    </#if>

    <div style="text-align: center; margin-top: 24px;">
      <a href="${portalUrl!"https://isperp.local/portal"}" style="background: #0284c7; color: #ffffff; text-decoration: none; padding: 12px 24px; border-radius: 8px; font-weight: bold; font-size: 14px; display: inline-block;">
        Acessar Central do Assinante
      </a>
    </div>

    <p style="font-size: 12px; color: #94a3b8; margin-top: 32px; text-align: center;">
      ${companyName!"ispERP Provedor de Internet"} • Conectando você ao que importa.
    </p>
  </div>
</body>
</html>
