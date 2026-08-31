import React, { useState, useEffect, useCallback } from 'react';
import { useParams } from 'react-router-dom';
import { SignaturePublicView, SignatureStatus } from '../../types/contract-signature';
import { contractSignatureService } from '../../services/contractSignatureService';
import {
  FaShieldAlt,
  FaCheckCircle,
  FaTimesCircle,
  FaQrcode,
  FaCopy,
  FaLock,
  FaFilePdf,
  FaInfoCircle,
  FaUniversity,
  FaClock,
  FaSyncAlt,
  FaExclamationTriangle,
  FaChevronDown,
  FaChevronUp,
} from 'react-icons/fa';

export const PublicSignaturePage: React.FC = () => {
  const { token } = useParams<{ token: string }>();

  const [signatureData, setSignatureData] = useState<SignaturePublicView | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);
  const [consentAccepted, setConsentAccepted] = useState(false);
  const [isSimulateOpen, setIsSimulateOpen] = useState(false);

  // Simulation form
  const [simName, setSimName] = useState('');
  const [simCpf, setSimCpf] = useState('');
  const [simulating, setSimulating] = useState(false);

  const fetchPublicView = useCallback(async () => {
    if (!token) return;
    try {
      // Tenta obter geolocalização se permitida pelo navegador
      let lat: number | undefined;
      let lon: number | undefined;
      if (navigator.geolocation) {
        navigator.geolocation.getCurrentPosition(
          (pos) => {
            lat = pos.coords.latitude;
            lon = pos.coords.longitude;
          },
          () => {}
        );
      }

      const data = await contractSignatureService.getPublicSignatureView(token, lat, lon);
      setSignatureData(data);
      if (!simName && data.customerName) setSimName(data.customerName);
      setError(null);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Link de assinatura inválido ou expirado.');
    } finally {
      setLoading(false);
    }
  }, [token, simName]);

  // Polling automático de status enquanto estiver PENDING
  useEffect(() => {
    fetchPublicView();
  }, [fetchPublicView]);

  useEffect(() => {
    if (!token || signatureData?.status === 'SIGNED' || signatureData?.status === 'EXPIRED') {
      return;
    }

    const interval = setInterval(async () => {
      try {
        const statusData = await contractSignatureService.getSignatureStatus(token);
        if (statusData.status !== signatureData?.status) {
          fetchPublicView();
        }
      } catch {
        // Ignora erros transitórios no polling
      }
    }, 2500);

    return () => clearInterval(interval);
  }, [token, signatureData?.status, fetchPublicView]);

  const handleCopyPix = () => {
    if (!signatureData?.pixCopyPaste) return;
    navigator.clipboard.writeText(signatureData.pixCopyPaste);
    setCopied(true);
    setTimeout(() => setCopied(false), 3000);
  };

  const handleSimulatePayment = async (useSameCpf: boolean) => {
    if (!token || !signatureData) return;
    try {
      setSimulating(true);
      const testCpf = useSameCpf ? simCpf || '12345678901' : '99988877766';
      await contractSignatureService.simulatePixPayment(token, {
        payerName: simName || signatureData.customerName,
        payerCpfCnpj: testCpf,
        bankName: 'Banco Simulado Sandbox (BACEN)',
      });
      await fetchPublicView();
    } catch (err: any) {
      alert('Erro na simulação: ' + (err.message || 'Falha'));
    } finally {
      setSimulating(false);
    }
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-900 flex flex-col items-center justify-center p-4 text-white">
        <FaSyncAlt className="animate-spin text-4xl text-indigo-400 mb-4" />
        <p className="text-sm text-slate-300 font-medium">Carregando contrato seguro...</p>
      </div>
    );
  }

  if (error || !signatureData) {
    return (
      <div className="min-h-screen bg-slate-900 flex items-center justify-center p-4">
        <div className="bg-white rounded-2xl p-8 max-w-md w-full text-center space-y-4 shadow-xl">
          <FaTimesCircle className="text-5xl text-rose-500 mx-auto" />
          <h2 className="text-xl font-bold text-slate-800">Assinatura Indisponível</h2>
          <p className="text-sm text-slate-600">{error || 'Não foi possível encontrar a sessão de assinatura.'}</p>
          <div className="pt-2">
            <p className="text-xs text-slate-400">
              Caso tenha dúvidas, entre em contato com a equipe de atendimento do provedor.
            </p>
          </div>
        </div>
      </div>
    );
  }

  const isSigned = signatureData.status === 'SIGNED';
  const isRejected = signatureData.status === 'REJECTED_DIVERGENT_DOCUMENT';
  const isExpired = signatureData.status === 'EXPIRED';

  return (
    <div className="min-h-screen bg-slate-950 py-8 px-4 sm:px-6 lg:px-8 font-sans text-slate-100 flex flex-col justify-between">
      <div className="max-w-3xl mx-auto w-full space-y-6">
        {/* Brand & Security Header */}
        <div className="bg-slate-900/80 backdrop-blur border border-slate-800 p-5 rounded-2xl flex flex-col sm:flex-row items-center justify-between gap-4 shadow-xl">
          <div className="flex items-center gap-3">
            <div className="p-3 bg-indigo-500/10 border border-indigo-500/30 text-indigo-400 rounded-xl">
              <FaLock className="w-5 h-5" />
            </div>
            <div>
              <span className="text-xs font-semibold tracking-wider text-indigo-400 uppercase">Portal de Assinatura Segura</span>
              <h1 className="text-lg font-bold text-white">{signatureData.companyName}</h1>
            </div>
          </div>

          <div className="flex items-center gap-2 text-xs text-emerald-400 bg-emerald-950/60 border border-emerald-800/60 px-3 py-1.5 rounded-full font-medium">
            <FaShieldAlt />
            Assinatura Eletrônica Avançada (Lei 14.063/2020)
          </div>
        </div>

        {/* Contract Info Summary */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-lg grid grid-cols-1 sm:grid-cols-3 gap-4 text-xs">
          <div>
            <span className="text-slate-400 block mb-0.5">Titular do Contrato:</span>
            <strong className="text-slate-100 text-sm">{signatureData.customerName}</strong>
          </div>
          <div>
            <span className="text-slate-400 block mb-0.5">CPF do Titular:</span>
            <strong className="text-slate-100 text-sm font-mono">{signatureData.customerDocumentMasked}</strong>
          </div>
          <div>
            <span className="text-slate-400 block mb-0.5">Documento:</span>
            <strong className="text-slate-100 text-sm">{signatureData.contractName}</strong>
          </div>
        </div>

        {/* SIGNED STATE SUCCESS CARD */}
        {isSigned ? (
          <div className="bg-gradient-to-br from-emerald-950/80 to-slate-900 border-2 border-emerald-500/60 rounded-3xl p-8 text-center space-y-6 shadow-2xl animate-fade-in">
            <div className="w-20 h-20 bg-emerald-500/20 border-2 border-emerald-400 rounded-full flex items-center justify-center mx-auto text-emerald-400 text-3xl shadow-lg shadow-emerald-500/20">
              <FaCheckCircle />
            </div>

            <div className="space-y-2">
              <h2 className="text-2xl font-black text-white">Contrato Assinado com Sucesso!</h2>
              <p className="text-sm text-slate-300 max-w-md mx-auto">
                A transação via Pix foi confirmada pelo Banco Central e autenticou sua manifestação de vontade com plena validade jurídica.
              </p>
            </div>

            <div className="bg-slate-950/80 rounded-xl p-4 border border-slate-800 max-w-md mx-auto text-left space-y-2 text-xs font-mono text-slate-300">
              <div className="flex justify-between">
                <span className="text-slate-500">Status:</span>
                <span className="text-emerald-400 font-bold">AUTENTICADO & ATIVO</span>
              </div>
              {signatureData.signedAt && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Data/Hora:</span>
                  <span>{new Date(signatureData.signedAt).toLocaleString('pt-BR')}</span>
                </div>
              )}
              {signatureData.payerBankName && (
                <div className="flex justify-between">
                  <span className="text-slate-500">Banco Autenticador:</span>
                  <span>{signatureData.payerBankName}</span>
                </div>
              )}
              {signatureData.documentSha256Hash && (
                <div className="pt-2 border-t border-slate-800">
                  <span className="text-slate-500 block mb-0.5">Hash SHA-256 de Integridade:</span>
                  <span className="text-[10px] break-all text-indigo-400">{signatureData.documentSha256Hash}</span>
                </div>
              )}
            </div>

            {signatureData.signedPdfUrl && (
              <a
                href={signatureData.signedPdfUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-2 px-6 py-3 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl font-bold shadow-lg shadow-emerald-600/30 transition transform hover:-translate-y-0.5"
              >
                <FaFilePdf />
                Baixar Contrato e Certificado em PDF
              </a>
            )}
          </div>
        ) : (
          <>
            {/* REJECTION DIVERGENT CPF WARNING */}
            {isRejected && (
              <div className="bg-rose-950/90 border-2 border-rose-500/80 rounded-2xl p-5 text-rose-200 space-y-2 shadow-xl animate-bounce-short">
                <div className="flex items-center gap-2 font-bold text-white text-base">
                  <FaExclamationTriangle className="text-rose-400" />
                  Assinatura Rejeitada por Divergência de Titularidade
                </div>
                <p className="text-xs leading-relaxed">{signatureData.rejectionReason}</p>
                <p className="text-xs text-rose-300 font-semibold pt-1">
                  💡 Por favor, efetue um novo pagamento utilizando a conta bancária do titular ({signatureData.customerName} - CPF: {signatureData.customerDocumentMasked}).
                </p>
              </div>
            )}

            {/* EXPIRED WARNING */}
            {isExpired && (
              <div className="bg-amber-950/80 border border-amber-500/60 rounded-2xl p-4 text-amber-200 text-xs flex items-center gap-3">
                <FaClock className="text-amber-400 text-xl shrink-0" />
                <div>
                  <strong>Link Expirado:</strong> O prazo limite de 72 horas para assinatura deste contrato expirou. Solicite um novo link com seu atendente.
                </div>
              </div>
            )}

            {/* Contract Reader Area */}
            <div className="bg-slate-900 border border-slate-800 rounded-2xl shadow-xl overflow-hidden">
              <div className="bg-slate-850 px-5 py-3 border-b border-slate-800 flex items-center justify-between text-xs font-semibold text-slate-300">
                <span>Termos e Cláusulas Contratuais</span>
                <span className="text-slate-500">Role para ler na íntegra</span>
              </div>

              <div className="h-72 overflow-y-auto p-5 text-slate-200 font-sans text-xs leading-relaxed space-y-3 whitespace-pre-wrap selection:bg-indigo-500 selection:text-white border-b border-slate-800">
                {signatureData.renderedContent}
              </div>

              {/* Mandatory Consent Checkbox */}
              <div className="p-5 bg-slate-950/60 space-y-3">
                <div className="p-3 bg-indigo-950/50 border border-indigo-800/60 rounded-xl text-xs text-indigo-200 leading-relaxed">
                  <strong>Cláusula de Consentimento:</strong> {signatureData.consentClause}
                </div>

                <label className="flex items-start gap-3 cursor-pointer select-none">
                  <input
                    type="checkbox"
                    checked={consentAccepted}
                    onChange={(e) => setConsentAccepted(e.target.checked)}
                    className="mt-0.5 w-4 h-4 text-indigo-600 bg-slate-800 border-slate-700 rounded focus:ring-indigo-500 cursor-pointer"
                  />
                  <span className="text-xs text-slate-300">
                    Declaro que li e concordo integralmente com todas as cláusulas e condições acima, autorizando a emissão da assinatura por meio da transação bancária via Pix.
                  </span>
                </label>
              </div>
            </div>

            {/* Pix Dynamic Payment Section */}
            <div
              className={`bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-6 transition ${
                !consentAccepted ? 'opacity-50 pointer-events-none filter grayscale' : 'opacity-100'
              }`}
            >
              <div className="text-center space-y-1">
                <span className="text-[11px] font-bold tracking-wider text-indigo-400 uppercase">
                  Etapa Final: Validação de Titularidade Bancária
                </span>
                <h2 className="text-xl font-black text-white">Efetue o Pix de R$ {signatureData.symbolicAmount.toFixed(2)}</h2>
                <p className="text-xs text-slate-400">
                  O pagamento deve ser feito <strong className="text-amber-400">obrigatoriamente pela conta bancária do titular</strong> para autenticação do CPF no Banco Central.
                </p>
              </div>

              <div className="flex flex-col md:flex-row items-center justify-center gap-8 bg-slate-950/90 p-6 rounded-2xl border border-slate-800">
                {/* QR Code */}
                <div className="bg-white p-3 rounded-2xl shadow-xl flex flex-col items-center">
                  {signatureData.pixQrCodeBase64 ? (
                    <img
                      src={signatureData.pixQrCodeBase64}
                      alt="QR Code Pix"
                      className="w-44 h-44 object-contain rounded-lg"
                    />
                  ) : (
                    <div className="w-44 h-44 bg-slate-100 flex items-center justify-center text-slate-400">
                      <FaQrcode className="text-6xl" />
                    </div>
                  )}
                  <span className="text-[10px] text-slate-500 mt-2 font-mono">Abra o app do seu banco e pague via Pix</span>
                </div>

                {/* Copy Paste Code */}
                <div className="flex-1 space-y-3 w-full">
                  <span className="text-xs font-semibold text-slate-300 block">Código Pix Copia e Cola:</span>
                  <div className="p-3 bg-slate-900 border border-slate-800 rounded-xl font-mono text-[11px] text-slate-400 break-all max-h-24 overflow-y-auto">
                    {signatureData.pixCopyPaste || 'Código Pix sendo gerado...'}
                  </div>

                  <button
                    onClick={handleCopyPix}
                    className="w-full flex items-center justify-center gap-2 py-3 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-sm font-bold shadow-lg shadow-indigo-600/30 transition active:scale-95"
                  >
                    <FaCopy />
                    {copied ? 'Código Pix Copiado com Sucesso!' : 'Copiar Código Pix'}
                  </button>

                  <div className="flex items-center justify-center gap-2 text-[11px] text-slate-500 pt-1 font-mono">
                    <FaSyncAlt className="animate-spin text-indigo-400" />
                    Aguardando confirmação bancária em tempo real...
                  </div>
                </div>
              </div>
            </div>

            {/* Sandbox Simulation Helper (For Testing & Demo) */}
            <div className="bg-slate-900/40 border border-slate-800/60 rounded-xl p-4 text-xs">
              <button
                onClick={() => setIsSimulateOpen(!isSimulateOpen)}
                className="w-full flex items-center justify-between text-slate-400 hover:text-slate-200 transition font-medium"
              >
                <span className="flex items-center gap-2">
                  <FaInfoCircle className="text-indigo-400" />
                  Homologação & Demonstração (Simular Pagamento do Banco Central)
                </span>
                {isSimulateOpen ? <FaChevronUp /> : <FaChevronDown />}
              </button>

              {isSimulateOpen && (
                <div className="mt-3 pt-3 border-t border-slate-800 space-y-3 animate-fade-in">
                  <p className="text-slate-400 text-[11px]">
                    Use os botões abaixo para simular o retorno do webhook do BACEN e validar os fluxos anti-fraude:
                  </p>

                  <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                    <button
                      disabled={simulating}
                      onClick={() => handleSimulatePayment(true)}
                      className="p-3 bg-emerald-950/70 hover:bg-emerald-900 border border-emerald-700/60 text-emerald-200 rounded-xl text-xs font-semibold text-left transition flex items-center gap-2"
                    >
                      <FaCheckCircle className="text-emerald-400 text-lg shrink-0" />
                      <div>
                        <div className="font-bold">Simular Pix do Titular (Sucesso)</div>
                        <div className="text-[10px] text-emerald-400">Mesmo CPF cadastrado ➡️ Aprova e Carimba Contrato</div>
                      </div>
                    </button>

                    <button
                      disabled={simulating}
                      onClick={() => handleSimulatePayment(false)}
                      className="p-3 bg-rose-950/70 hover:bg-rose-900 border border-rose-700/60 text-rose-200 rounded-xl text-xs font-semibold text-left transition flex items-center gap-2"
                    >
                      <FaTimesCircle className="text-rose-400 text-lg shrink-0" />
                      <div>
                        <div className="font-bold">Simular Pix de Terceiro (Anti-Fraude)</div>
                        <div className="text-[10px] text-rose-400">CPF divergente ➡️ Rejeita com alerta de segurança</div>
                      </div>
                    </button>
                  </div>
                </div>
              )}
            </div>
          </>
        )}

        {/* Footer info */}
        <div className="text-center text-xs text-slate-500 space-y-1 pt-4">
          <p>Documento assinado com certificação de autenticidade conforme MP nº 2.200-2/2001 e Lei Federal nº 14.063/2020.</p>
          <p className="text-[10px] text-slate-600">ispERP Platform - Gestão Inteligente para Provedores de Internet</p>
        </div>
      </div>
    </div>
  );
};
