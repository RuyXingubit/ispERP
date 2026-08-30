import React, { useState } from 'react';
import { QRCodeSVG } from 'qrcode.react';
import {
  MarcoCivilSearchResult,
  MarcoCivilReportResponse,
} from '../../types/marcoCivil';
import { marcoCivilService } from '../../services/marcoCivilService';
import {
  FaSearch,
  FaBalanceScale,
  FaFilePdf,
  FaQrcode,
  FaCheckCircle,
  FaTimesCircle,
  FaUserShield,
  FaClock,
  FaFingerprint,
  FaPrint,
  FaCopy,
} from 'react-icons/fa';

export const MarcoCivilSearch: React.FC = () => {
  // Busca Form
  const [ip, setIp] = useState('200.150.10.2');
  const [port, setPort] = useState<number | ''>(1024);
  const [dateTime, setDateTime] = useState(
    new Date(Date.now() - 3600000).toISOString().slice(0, 16)
  );

  const [loading, setLoading] = useState(false);
  const [result, setResult] = useState<MarcoCivilSearchResult | null>(null);

  // Laudo Modal / Emissão
  const [courtOrderNumber, setCourtOrderNumber] = useState('');
  const [requesterAuthority, setRequesterAuthority] = useState('1ª Delegacia de Crimes Cibernéticos');
  const [reportNotes, setReportNotes] = useState('');
  const [generatingReport, setGeneratingReport] = useState(false);
  const [issuedReport, setIssuedReport] = useState<MarcoCivilReportResponse | null>(null);

  // Notificações
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const handleSearch = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setLoading(true);
      setIssuedReport(null);
      const res = await marcoCivilService.searchSubscriber({
        ip,
        port: port ? Number(port) : undefined,
        timestamp: new Date(dateTime).toISOString(),
      });
      setResult(res);
    } catch (err: any) {
      showNotification('Erro na busca forense: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  };

  const handleGenerateReport = async () => {
    if (!result) return;
    try {
      setGeneratingReport(true);
      const report = await marcoCivilService.generateOfficialReport({
        courtOrderNumber: courtOrderNumber || undefined,
        requesterAuthority: requesterAuthority || undefined,
        queriedIp: result.queriedIp,
        queriedPort: result.queriedPort,
        queriedTimestamp: result.queriedTimestamp,
        matchedContractId: result.contractId,
        notes: reportNotes || undefined,
      });
      setIssuedReport(report);
      showNotification('Laudo Pericial Oficial emitido com Hash SHA-256 e QR Code!');
    } catch (err: any) {
      showNotification('Erro ao emitir laudo: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setGeneratingReport(false);
    }
  };

  const copyToClipboard = (text: string, label: string) => {
    navigator.clipboard.writeText(text);
    showNotification(`${label} copiado para a área de transferência!`);
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <FaBalanceScale className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">
              Central de Investigação - Marco Civil da Internet
            </h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Atendimento a Ofícios Judiciais e Requisições Policiais com Cruzamento Forense CGNAT/RADIUS e Laudo Anti-Fraude
          </p>
        </div>
      </div>

      {/* Notifications */}
      {notification && (
        <div
          className={`p-4 rounded-lg flex items-center justify-between shadow-md ${
            notification.type === 'success'
              ? 'bg-emerald-50 border border-emerald-200 text-emerald-800 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-300'
              : 'bg-rose-50 border border-rose-200 text-rose-800 dark:bg-rose-950/40 dark:border-rose-800 dark:text-rose-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === 'success' ? <FaCheckCircle className="w-5 h-5" /> : <FaTimesCircle className="w-5 h-5" />}
            <span className="font-medium text-sm">{notification.message}</span>
          </div>
        </div>
      )}

      {/* Search Form */}
      <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
        <h2 className="text-base font-bold text-slate-900 dark:text-white flex items-center gap-2">
          <FaSearch className="w-4 h-4 text-indigo-600" /> Parâmetros do Fato Investigado (Art. 10 e 13 - Lei 12.965/2014)
        </h2>

        <form onSubmit={handleSearch} className="grid grid-cols-1 md:grid-cols-4 gap-4">
          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Endereço IP Pesquisado *
            </label>
            <input
              type="text"
              required
              placeholder="Ex: 200.150.10.2 ou 2804:192c::1"
              value={ip}
              onChange={(e) => setIp(e.target.value)}
              className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Porta de Origem (CGNAT)
            </label>
            <input
              type="number"
              placeholder="Ex: 1024"
              value={port}
              onChange={(e) => setPort(e.target.value === '' ? '' : Number(e.target.value))}
              className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono"
            />
          </div>

          <div>
            <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
              Data e Hora do Evento *
            </label>
            <input
              type="datetime-local"
              required
              value={dateTime}
              onChange={(e) => setDateTime(e.target.value)}
              className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
            />
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={loading}
              className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold text-sm shadow-sm transition flex items-center justify-center gap-2"
            >
              <FaSearch className="w-4 h-4" /> {loading ? 'Pesquisando...' : 'Identificar Assinante'}
            </button>
          </div>
        </form>
      </div>

      {/* Search Result */}
      {result && (
        <div className="space-y-6">
          {result.matched ? (
            <div className="bg-emerald-50/50 dark:bg-emerald-950/20 border border-emerald-200 dark:border-emerald-800/60 p-6 rounded-2xl shadow-sm space-y-6">
              <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-emerald-200 dark:border-emerald-800/40 pb-4">
                <div className="flex items-center gap-3">
                  <div className="p-3 bg-emerald-500 text-white rounded-xl">
                    <FaCheckCircle className="w-6 h-6" />
                  </div>
                  <div>
                    <span className="text-xs font-bold uppercase tracking-wider text-emerald-700 dark:text-emerald-300">
                      Assinante Identificado com Sucesso
                    </span>
                    <h3 className="text-xl font-bold text-slate-900 dark:text-white">
                      {result.customerName || result.username}
                    </h3>
                  </div>
                </div>

                <button
                  onClick={handleGenerateReport}
                  disabled={generatingReport}
                  className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl font-bold text-sm shadow-md transition"
                >
                  <FaFilePdf className="w-4 h-4" />
                  {generatingReport ? 'Gerando Laudo...' : 'Emitir Laudo Oficial Anti-Fraude'}
                </button>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-3 gap-6 text-sm">
                {/* Coluna 1: Dados do Cliente */}
                <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2">
                  <h4 className="font-bold text-slate-900 dark:text-white text-xs uppercase flex items-center gap-1">
                    <FaUserShield className="w-3.5 h-3.5 text-indigo-500" /> Dados Cadastrais
                  </h4>
                  <div>
                    <span className="text-xs text-slate-400">CPF / CNPJ:</span>
                    <p className="font-mono font-bold text-slate-900 dark:text-white">{result.customerCpfCnpj || 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">Telefone:</span>
                    <p className="text-slate-700 dark:text-slate-300">{result.customerPhone || 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">Email:</span>
                    <p className="text-slate-700 dark:text-slate-300">{result.customerEmail || 'N/A'}</p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">Endereço de Instalação:</span>
                    <p className="text-xs text-slate-700 dark:text-slate-300">{result.installationAddress || 'N/A'}</p>
                  </div>
                </div>

                {/* Coluna 2: Dados da Conexão & CGNAT */}
                <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2">
                  <h4 className="font-bold text-slate-900 dark:text-white text-xs uppercase flex items-center gap-1">
                    <FaFingerprint className="w-3.5 h-3.5 text-purple-500" /> Roteamento & CGNAT
                  </h4>
                  <div>
                    <span className="text-xs text-slate-400">IP Público / Porta Consultada:</span>
                    <p className="font-mono font-bold text-indigo-600">
                      {result.queriedIp} : {result.queriedPort || 'N/A'}
                    </p>
                  </div>
                  {result.usedCgnat && (
                    <div>
                      <span className="text-xs text-slate-400">IP Privado Decodificado (CGNAT):</span>
                      <p className="font-mono font-bold text-emerald-600">{result.resolvedPrivateIp}</p>
                    </div>
                  )}
                  <div>
                    <span className="text-xs text-slate-400">Usuário PPPoE:</span>
                    <p className="font-mono font-semibold text-slate-900 dark:text-white">{result.username}</p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">MAC da ONT / CPE:</span>
                    <p className="font-mono text-slate-700 dark:text-slate-300">{result.callingStationId || 'N/A'}</p>
                  </div>
                </div>

                {/* Coluna 3: Auditoria Temporal */}
                <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700 space-y-2">
                  <h4 className="font-bold text-slate-900 dark:text-white text-xs uppercase flex items-center gap-1">
                    <FaClock className="w-3.5 h-3.5 text-amber-500" /> Auditoria Temporal
                  </h4>
                  <div>
                    <span className="text-xs text-slate-400">Instante do Fato:</span>
                    <p className="font-mono font-bold text-slate-900 dark:text-white">
                      {new Date(result.queriedTimestamp).toLocaleString()}
                    </p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">Início da Sessão RADIUS:</span>
                    <p className="font-mono text-xs text-slate-700 dark:text-slate-300">
                      {result.sessionStartTime ? new Date(result.sessionStartTime).toLocaleString() : 'N/A'}
                    </p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">Término da Sessão:</span>
                    <p className="font-mono text-xs text-slate-700 dark:text-slate-300">
                      {result.sessionStopTime ? new Date(result.sessionStopTime).toLocaleString() : 'Em andamento (Online)'}
                    </p>
                  </div>
                  <div>
                    <span className="text-xs text-slate-400">BNG Responsável:</span>
                    <p className="font-mono text-xs text-slate-700 dark:text-slate-300">{result.nasIpAddress}</p>
                  </div>
                </div>
              </div>
            </div>
          ) : (
            <div className="bg-rose-50 dark:bg-rose-950/20 border border-rose-200 dark:border-rose-800 p-6 rounded-2xl text-center space-y-2">
              <FaTimesCircle className="w-8 h-8 text-rose-500 mx-auto" />
              <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                Nenhum registro localizado para o IP e data/hora informados
              </h3>
              <p className="text-sm text-slate-500 max-w-lg mx-auto">
                Verifique se o IP público possui regras de CGNAT cadastradas no ispERP para a porta consultada ou se o timestamp está no fuso horário correto (UTC/BRT).
              </p>
            </div>
          )}
        </div>
      )}

      {/* LAUDO PERICIAL OFICIAL EMITIDO (ANTI-FRAUDE) */}
      {issuedReport && (
        <div className="bg-white dark:bg-slate-800 p-8 rounded-2xl border-2 border-indigo-600 shadow-xl space-y-6">
          <div className="flex flex-col md:flex-row justify-between items-start gap-4 border-b border-slate-200 dark:border-slate-700 pb-6">
            <div>
              <span className="px-3 py-1 bg-indigo-100 dark:bg-indigo-900/50 text-indigo-800 dark:text-indigo-300 text-xs font-bold rounded-full">
                DOCUMENTO OFICIAL AUDITÁVEL
              </span>
              <h2 className="text-2xl font-black text-slate-900 dark:text-white mt-2">
                Laudo Pericial de Identificação de Conexão à Internet
              </h2>
              <p className="text-xs text-slate-500 mt-1">
                Emitido em conformidade com o Marco Civil da Internet (Lei nº 12.965/2014) e Decreto nº 8.771/2016
              </p>
            </div>

            <div className="flex gap-2">
              <button
                onClick={() => window.print()}
                className="flex items-center gap-2 px-4 py-2 bg-slate-100 dark:bg-slate-700 hover:bg-slate-200 text-slate-700 dark:text-slate-200 rounded-lg text-sm font-semibold transition"
              >
                <FaPrint className="w-4 h-4" /> Imprimir
              </button>
            </div>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 items-center">
            <div className="md:col-span-2 space-y-4">
              <div className="p-4 bg-slate-50 dark:bg-slate-900 rounded-xl space-y-2 text-xs">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Hash Criptográfico SHA-256:</span>
                  <button
                    onClick={() => copyToClipboard(issuedReport.sha256Hash, 'Hash SHA-256')}
                    className="text-indigo-600 hover:text-indigo-800 flex items-center gap-1 font-semibold"
                  >
                    <FaCopy className="w-3 h-3" /> Copiar
                  </button>
                </div>
                <p className="font-mono font-bold text-slate-900 dark:text-white break-all text-xs">
                  {issuedReport.sha256Hash}
                </p>
              </div>

              <div className="p-4 bg-slate-50 dark:bg-slate-900 rounded-xl space-y-2 text-xs">
                <div className="flex justify-between items-center">
                  <span className="text-slate-500">Token de Autenticidade Pública:</span>
                  <button
                    onClick={() => copyToClipboard(issuedReport.validationToken, 'Token')}
                    className="text-indigo-600 hover:text-indigo-800 flex items-center gap-1 font-semibold"
                  >
                    <FaCopy className="w-3 h-3" /> Copiar
                  </button>
                </div>
                <p className="font-mono font-bold text-indigo-600 text-sm">
                  {issuedReport.validationToken}
                </p>
              </div>

              <div className="text-xs text-slate-600 dark:text-slate-400 space-y-1">
                <p><strong>Autoridade Solicitante:</strong> {issuedReport.requesterAuthority || 'Autoridade Policial / Judicial'}</p>
                <p><strong>Número do Procedimento / Ofício:</strong> {issuedReport.courtOrderNumber || 'N/A'}</p>
                <p><strong>IP & Porta Investigados:</strong> {issuedReport.queriedIp} : {issuedReport.queriedPort || 'N/A'}</p>
                <p><strong>Data/Hora do Fato:</strong> {new Date(issuedReport.queriedTimestamp).toLocaleString()}</p>
                <p><strong>Titular Identificado:</strong> {issuedReport.matchedCustomerName} ({issuedReport.matchedCpfCnpj})</p>
              </div>
            </div>

            {/* QR CODE ANTI-FRAUDE */}
            <div className="flex flex-col items-center justify-center p-6 bg-slate-50 dark:bg-slate-900 rounded-2xl border border-slate-200 dark:border-slate-700 text-center space-y-3">
              <div className="p-3 bg-white rounded-xl shadow-md">
                <QRCodeSVG value={issuedReport.publicValidationUrl} size={150} />
              </div>
              <div className="space-y-1">
                <span className="text-xs font-bold text-indigo-600 dark:text-indigo-400 flex items-center justify-center gap-1">
                  <FaQrcode className="w-3.5 h-3.5" /> Validação Anti-Fraude
                </span>
                <p className="text-xs text-slate-500 max-w-[200px]">
                  Aponte a câmera para conferir a autenticidade deste laudo nos servidores do provedor
                </p>
                <a
                  href={issuedReport.publicValidationUrl}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="text-xs font-semibold text-indigo-600 underline block mt-2"
                >
                  Abrir link de validação
                </a>
              </div>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default MarcoCivilSearch;
