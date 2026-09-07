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

