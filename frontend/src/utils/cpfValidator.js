/**
 * Valida se um CPF é válido
 * @param {string} cpf - CPF a ser validado (pode conter pontos e traços)
 * @returns {boolean} - true se o CPF for válido, false caso contrário
 */
export const validateCPF = (cpf) => {
  // Remove caracteres não numéricos
  const cleanCPF = cpf.replace(/[^\d]/g, '');
  
  // Verifica se tem 11 dígitos
  if (cleanCPF.length !== 11) {
    return false;
  }
  
  // Verifica se todos os dígitos são iguais (CPF inválido)
  if (/^(\d)\1{10}$/.test(cleanCPF)) {
    return false;
  }
  
  // Calcula o primeiro dígito verificador
  let sum = 0;
  for (let i = 0; i < 9; i++) {
    sum += parseInt(cleanCPF.charAt(i)) * (10 - i);
  }
  let remainder = sum % 11;
  let firstDigit = remainder < 2 ? 0 : 11 - remainder;
  
  // Verifica o primeiro dígito
  if (parseInt(cleanCPF.charAt(9)) !== firstDigit) {
    return false;
  }
  
  // Calcula o segundo dígito verificador
  sum = 0;
  for (let i = 0; i < 10; i++) {
    sum += parseInt(cleanCPF.charAt(i)) * (11 - i);
  }
  remainder = sum % 11;
  let secondDigit = remainder < 2 ? 0 : 11 - remainder;
  
  // Verifica o segundo dígito
  return parseInt(cleanCPF.charAt(10)) === secondDigit;
};

/**
 * Formata um CPF adicionando pontos e traço
 * @param {string} cpf - CPF a ser formatado
 * @returns {string} - CPF formatado (xxx.xxx.xxx-xx)
 */
export const formatCPF = (cpf) => {
  // Remove caracteres não numéricos
  const cleanCPF = cpf.replace(/[^\d]/g, '');
  
  // Aplica a máscara
  return cleanCPF
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d)/, '$1.$2')
    .replace(/(\d{3})(\d{1,2})$/, '$1-$2');
};

/**
 * Remove a formatação do CPF, deixando apenas números
 * @param {string} cpf - CPF formatado
 * @returns {string} - CPF apenas com números
 */
export const cleanCPF = (cpf) => {
  return cpf.replace(/[^\d]/g, '');
};