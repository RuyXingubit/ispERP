import test from 'node:test';
import assert from 'node:assert/strict';

// Funções de formatação puras
function formatDocument(value) {
  if (!value) return '';
  const clean = value.replace(/\D/g, '');
  if (clean.length <= 11) {
    return clean
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d)/, '$1.$2')
      .replace(/(\d{3})(\d{1,2})$/, '$1-$2')
      .slice(0, 14);
  }
  return clean
    .replace(/^(\d{2})(\d)/, '$1.$2')
    .replace(/^(\d{2})\.(\d{3})(\d)/, '$1.$2.$3')
    .replace(/\.(\d{3})(\d)/, '.$1/$2')
    .replace(/(\d{4})(\d{1,2})$/, '$1-$2')
    .slice(0, 18);
}

function formatCurrency(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function formatDate(isoDate) {
  if (!isoDate) return '';
  const parts = isoDate.split('-');
  if (parts.length === 3) {
    return `${parts[2]}/${parts[1]}/${parts[0]}`;
  }
  return isoDate;
}

function getConnectionStatusInfo(isBlocked, isTrustUnblocked) {
  if (isBlocked) {
    return {
      statusClass: 'status-blocked',
      title: 'Conexão Suspensa',
      description: 'Identificamos faturas em aberto. Efetue o pagamento via Pix para liberação imediata ou solicite o Desbloqueio em Confiança (48h).',
      badgeText: 'Bloqueado',
    };
  }
  if (isTrustUnblocked) {
    return {
      statusClass: 'status-trust',
      title: 'Liberado em Confiança',
      description: 'Sua conexão está liberada temporariamente (48h). Quite suas pendências para evitar novo bloqueio automático.',
      badgeText: 'Liberado em Confiança',
    };
  }
  return {
    statusClass: 'status-online',
    title: 'Conexão Online e Ativa',
    description: 'Sua internet de fibra óptica de alta velocidade está funcionando normalmente.',
    badgeText: 'Online',
  };
}

test('formatDocument - formata CPF de 11 dígitos com pontuação padrão', () => {
  assert.equal(formatDocument('20037041088'), '200.370.410-88');
  assert.equal(formatDocument('200.370.410-88'), '200.370.410-88');
  assert.equal(formatDocument(''), '');
});

test('formatDocument - formata CNPJ de 14 dígitos com pontuação padrão', () => {
  assert.equal(formatDocument('12345678000195'), '12.345.678/0001-95');
  assert.equal(formatDocument('12.345.678/0001-95'), '12.345.678/0001-95');
});

test('formatCurrency - formata valores monetários em BRL', () => {
  assert.equal(formatCurrency(79.9), '79,90');
  assert.equal(formatCurrency(0), '0,00');
  assert.equal(formatCurrency(1250.5), '1.250,50');
});

test('formatDate - converte ISO YYYY-MM-DD para DD/MM/YYYY', () => {
  assert.equal(formatDate('2026-09-10'), '10/09/2026');
  assert.equal(formatDate('2026-01-01'), '01/01/2026');
  assert.equal(formatDate(''), '');
});

test('getConnectionStatusInfo - avalia estados de bloqueio e confiança', () => {
  const blocked = getConnectionStatusInfo(true, false);
  assert.equal(blocked.badgeText, 'Bloqueado');
  assert.equal(blocked.statusClass, 'status-blocked');

  const trust = getConnectionStatusInfo(false, true);
  assert.equal(trust.badgeText, 'Liberado em Confiança');
  assert.equal(trust.statusClass, 'status-trust');

  const online = getConnectionStatusInfo(false, false);
  assert.equal(online.badgeText, 'Online');
  assert.equal(online.statusClass, 'status-online');
});

function validatePin(pin) {
  if (!pin) return false;
  return /^\d{4}$/.test(pin.trim());
}

test('validatePin - valida formato exato de 4 dígitos numéricos', () => {
  assert.equal(validatePin('1234'), true);
  assert.equal(validatePin('0000'), true);
  assert.equal(validatePin('9876'), true);

  // Inválidos
  assert.equal(validatePin('123'), false); // 3 dígitos
  assert.equal(validatePin('12345'), false); // 5 dígitos
  assert.equal(validatePin('abcd'), false); // letras
  assert.equal(validatePin('12a4'), false); // misto
  assert.equal(validatePin(''), false); // vazio
  assert.equal(validatePin(null), false); // nulo
});

function extractSignatureToken(pathname, search) {
  if (pathname) {
    const cleanPath = pathname.replace(/\/+$/, '');
    const match = cleanPath.match(/(?:^|\/)sign\/([A-Za-z0-9_-]{8,})$/);
    if (match && match[1]) {
      return match[1];
    }
  }
  if (search) {
    const params = new URLSearchParams(search.startsWith('?') ? search : `?${search}`);
    const tokenParam = params.get('token') || params.get('sign');
    if (tokenParam && /^[A-Za-z0-9_-]{8,}$/.test(tokenParam.trim())) {
      return tokenParam.trim();
    }
  }
  return null;
}

function formatContractStatus(status) {
  switch ((status || '').toUpperCase()) {
    case 'ACTIVE':
      return 'Ativo';
    case 'BLOCKED':
    case 'SUSPENDED':
      return 'Bloqueado / Suspenso';
    case 'PENDING':
    case 'DRAFT':
      return 'Aguardando Ativação';
    case 'CANCELED':
    case 'CANCELLED':
      return 'Cancelado';
    default:
      return status || 'Ativo';
  }
}

function formatFallbackMethodLabel(method) {
  switch ((method || '').toUpperCase()) {
    case 'GOV_BR':
      return 'Assinatura Digital Gov.br (ICP-Brasil / Prata ou Ouro)';
    case 'EMAIL_OTP':
      return 'Token OTP Auditado (E-mail / WhatsApp Oficial + Evidência de IP)';
    case 'PHYSICAL_NOTARY':
      return 'Assinatura Física / Reconhecimento em Cartório';
    default:
      return method || 'Contingência MP 2.200-2';
  }
}

function getSignedContractPdfUrl(token, signedPdfUrl, apiBaseUrl = '/api') {
  if (signedPdfUrl && signedPdfUrl.startsWith('http')) {
    return signedPdfUrl;
  }
  return `${apiBaseUrl}/public/signatures/${encodeURIComponent(token)}/pdf`;
}

test('extractSignatureToken - extrai token de rota limpa /sign/{token} ou query ?token= / ?sign=', () => {
  assert.equal(
    extractSignatureToken('/sign/019633f8a4b07d8e9f1234567890abcd', ''),
    '019633f8a4b07d8e9f1234567890abcd'
  );
  assert.equal(
    extractSignatureToken('/sign/token_forense_12345/', ''),
    'token_forense_12345'
  );
  assert.equal(
    extractSignatureToken('/', '?token=019633f8a4b07d8e9f1234567890abcd'),
    '019633f8a4b07d8e9f1234567890abcd'
  );
  assert.equal(
    extractSignatureToken('/index.html', '?sign=abc123456789'),
    'abc123456789'
  );
  assert.equal(extractSignatureToken('/', ''), null);
  assert.equal(extractSignatureToken('/dashboard', '?token=curto'), null);
});

test('formatContractStatus e formatFallbackMethodLabel - traduzem estados de contrato e contingência MP 2.200-2', () => {
  assert.equal(formatContractStatus('ACTIVE'), 'Ativo');
  assert.equal(formatContractStatus('SUSPENDED'), 'Bloqueado / Suspenso');
  assert.equal(formatContractStatus('PENDING'), 'Aguardando Ativação');

  assert.equal(
    formatFallbackMethodLabel('GOV_BR'),
    'Assinatura Digital Gov.br (ICP-Brasil / Prata ou Ouro)'
  );
  assert.equal(
    formatFallbackMethodLabel('EMAIL_OTP'),
    'Token OTP Auditado (E-mail / WhatsApp Oficial + Evidência de IP)'
  );
  assert.equal(
    formatFallbackMethodLabel('PHYSICAL_NOTARY'),
    'Assinatura Física / Reconhecimento em Cartório'
  );
});

test('getSignedContractPdfUrl - resolve URL de PDF assinado com certificado forense', () => {
  assert.equal(
    getSignedContractPdfUrl('tok_12345678', null),
    '/api/public/signatures/tok_12345678/pdf'
  );
  assert.equal(
    getSignedContractPdfUrl('tok_12345678', 'https://cdn.provedor.com.br/contratos/tok_12345678.pdf'),
    'https://cdn.provedor.com.br/contratos/tok_12345678.pdf'
  );
});

function summarizeInvoiceComposition(items = []) {
  let scmAmount = 0;
  let svaAmount = 0;
  for (const item of items) {
    if (item.category === 'SCM') {
      scmAmount += item.amount;
    } else {
      svaAmount += item.amount;
    }
  }
  return {
    scmAmount: Number(scmAmount.toFixed(2)),
    svaAmount: Number(svaAmount.toFixed(2)),
    totalAmount: Number((scmAmount + svaAmount).toFixed(2)),
  };
}

test('summarizeInvoiceComposition - separa base tributável SCM (NFCom) de itens SVA/Locação', () => {
  const summary = summarizeInvoiceComposition([
    { name: 'Acesso SCM Fibra', category: 'SCM', amount: 39.9 },
    { name: 'Locação ONT Wi-Fi 6', category: 'EQUIPMENT_RENTAL', amount: 30.0 },
    { name: 'E-Books', category: 'SVA_DIGITAL_CONTENT', amount: 20.0 },
    { name: 'Streaming TV', category: 'SVA_STREAMING_APP', amount: 10.0 },
  ]);

  assert.equal(summary.scmAmount, 39.9);
  assert.equal(summary.svaAmount, 60.0);
  assert.equal(summary.totalAmount, 99.9);
});



