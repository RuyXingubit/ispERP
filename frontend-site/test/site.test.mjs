import test from 'node:test';
import assert from 'node:assert/strict';

function formatCurrency(value) {
  return new Intl.NumberFormat('pt-BR', {
    style: 'decimal',
    minimumFractionDigits: 2,
    maximumFractionDigits: 2,
  }).format(value);
}

function resolveCustomerAreaUrl(envUrl, fallback) {
  return envUrl || fallback || 'http://localhost:3001';
}

function buildWhatsAppLink(phone, companyName, plan) {
  const cleanPhone = (phone || '').replace(/\D/g, '');
  if (!cleanPhone) return '#contato';
  const priceFormatted = formatCurrency(plan.price);
  const msg = `Olá! Tenho interesse no plano ${plan.name} (${plan.downloadSpeed} Mega por R$ ${priceFormatted}/mês) da ${companyName}. Poderiam verificar a disponibilidade para o meu endereço?`;
  return `https://wa.me/55${cleanPhone}?text=${encodeURIComponent(msg)}`;
}

test('formatCurrency - formata valores monetários no padrão brasileiro', () => {
  assert.equal(formatCurrency(99.9), '99,90');
  assert.equal(formatCurrency(149.99), '149,99');
  assert.equal(formatCurrency(100), '100,00');
});

test('resolveCustomerAreaUrl - resolve URL com fallback configurável', () => {
  assert.equal(resolveCustomerAreaUrl('https://cliente.provedor.com.br', 'http://localhost:3001'), 'https://cliente.provedor.com.br');
  assert.equal(resolveCustomerAreaUrl(undefined, 'http://localhost:3001'), 'http://localhost:3001');
});

test('buildWhatsAppLink - gera URL de WhatsApp com mensagem pré-preenchida do plano', () => {
  const plan = { name: 'Fibra Gamer', downloadSpeed: 600, price: 119.90 };
  const link = buildWhatsAppLink('(99) 98765-4321', 'Nexus Fibra', plan);
  
  assert.match(link, /^https:\/\/wa\.me\/5599987654321\?text=/);
  assert.ok(link.includes('Fibra%20Gamer'));
  assert.ok(link.includes('600%20Mega'));
});
