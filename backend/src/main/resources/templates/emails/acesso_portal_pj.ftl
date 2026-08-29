<!DOCTYPE html>
<html>
<head>
  <meta charset="UTF-8">
  <title>Acesso Corporativo - Central do Assinante</title>
</head>
<body style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Helvetica, Arial, sans-serif; color: #1e293b; background-color: #f8fafc; padding: 24px; margin: 0;">
  <div style="max-width: 580px; margin: 0 auto; background: #ffffff; border-radius: 12px; padding: 32px; border: 1px solid #e2e8f0;">
    
    <h2 style="color: #0f172a; margin-top: 0;">Acesso Corporativo • Central do Assinante</h2>
    <p style="font-size: 14px; color: #475569;">
      Olá, <strong>${companyName}</strong> (CNPJ: ${companyCnpj})!
    </p>

    <p style="font-size: 14px; line-height: 1.6; color: #334155;">
      Recebemos uma solicitação de acesso seguro ao painel corporativo do seu contrato de telecomunicações. Clique no botão abaixo para autenticar sua sessão sem senha:
    </p>

    <div style="text-align: center; margin: 28px 0;">
      <a href="${magicLinkUrl}" style="background: #0284c7; color: #ffffff; text-decoration: none; padding: 14px 28px; border-radius: 8px; font-weight: bold; font-size: 15px; display: inline-block; box-shadow: 0 2px 4px rgba(2, 132, 199, 0.2);">
        🔐 Acessar Painel Corporativo
      </a>
    </div>

    <div style="background: #fffbeb; border: 1px solid #fef3c7; border-radius: 8px; padding: 12px 16px; font-size: 12px; color: #92400e; margin-bottom: 24px;">
      ⚠️ <strong>Atenção:</strong> Este link é de uso exclusivo e expira em 30 minutos. Não o compartilhe com terceiros.
    </div>

    <p style="font-size: 12px; color: #94a3b8; margin: 0;">
      Caso não consiga clicar no botão, copie e cole o link a seguir no seu navegador:<br>
      <span style="font-family: monospace; word-break: break-all; color: #0284c7;">${magicLinkUrl}</span>
    </p>
  </div>
</body>
</html>
