import React, { useState, useEffect } from 'react';
import { useParams, Link } from 'react-router-dom';
import { PublicValidationResponse } from '../../types/marcoCivil';
import { marcoCivilService } from '../../services/marcoCivilService';
import {
  FaShieldAlt,
  FaCheckCircle,
  FaTimesCircle,
  FaLock,
  FaBalanceScale,
  FaArrowLeft,
} from 'react-icons/fa';

export const ReportValidation: React.FC = () => {
  const { token } = useParams<{ token: string }>();
  const [loading, setLoading] = useState(true);
  const [data, setData] = useState<PublicValidationResponse | null>(null);

  useEffect(() => {
    if (token) {
      marcoCivilService
        .validatePublicToken(token)
        .then((res) => setData(res))
        .catch(() =>
          setData({
            valid: false,
            validationToken: token,
            sha256Hash: 'N/A',
            queriedIp: 'N/A',
            queriedTimestamp: new Date().toISOString(),
            reportIssuedAt: new Date().toISOString(),
            statusMessage: 'Erro de comunicação ao validar documento.',
          })
        )
        .finally(() => setLoading(false));
    }
  }, [token]);

  return (
    <div className="min-h-screen bg-slate-900 text-slate-100 flex flex-col items-center justify-center p-4">
      <div className="max-w-2xl w-full bg-slate-800 rounded-3xl p-8 border border-slate-700 shadow-2xl space-y-6">
        {/* Header */}
        <div className="text-center space-y-2">
          <div className="inline-flex p-3 bg-indigo-600/20 text-indigo-400 rounded-2xl mb-2">
            <FaShieldAlt className="w-10 h-10" />
          </div>
          <h1 className="text-2xl font-black text-white">Portal de Verificação de Autenticidade</h1>
          <p className="text-xs text-slate-400">
            Validador Criptográfico de Laudos Periciais - Marco Civil da Internet (Lei 12.965/2014)
          </p>
        </div>

        {loading ? (
          <div className="py-12 text-center text-slate-400">
            Consultando assinatura digital e registros do ISP...
          </div>
        ) : data?.valid ? (
          <div className="space-y-6">
            {/* Valid Badge */}
            <div className="p-4 bg-emerald-500/10 border border-emerald-500/30 rounded-2xl flex items-center gap-3">
              <FaCheckCircle className="w-8 h-8 text-emerald-400 shrink-0" />
              <div>
                <h3 className="font-bold text-emerald-300 text-sm">DOCUMENTO OFICIAL AUTÊNTICO</h3>
                <p className="text-xs text-emerald-400/80">
                  Os dados do laudo pericial conferem com o registro imutável emitido pelo sistema ispERP.
                </p>
              </div>
            </div>

            {/* Details */}
            <div className="bg-slate-900/60 p-6 rounded-2xl border border-slate-700/50 space-y-3 text-xs">
              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">Hash SHA-256 do Laudo:</span>
                <span className="font-mono font-bold text-indigo-400 break-all text-right max-w-xs">
                  {data.sha256Hash}
                </span>
              </div>

              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">Número do Procedimento / Ofício:</span>
                <span className="font-semibold text-white">{data.courtOrderNumber || 'N/A'}</span>
              </div>

              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">Autoridade Requisitante:</span>
                <span className="font-semibold text-white">{data.requesterAuthority || 'Autoridade Policial'}</span>
              </div>

              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">IP & Porta Investigados:</span>
                <span className="font-mono font-bold text-emerald-400">
                  {data.queriedIp} : {data.queriedPort || 'N/A'}
                </span>
              </div>

              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">Data/Hora do Evento (UTC/BRT):</span>
                <span className="font-mono text-white">
                  {new Date(data.queriedTimestamp).toLocaleString()}
                </span>
              </div>

              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">Assinante Vinculado (LGPD):</span>
                <span className="font-bold text-white">{data.customerNameMasked}</span>
              </div>

              <div className="flex justify-between border-b border-slate-700/40 pb-2">
                <span className="text-slate-400">CPF / CNPJ Mascarado:</span>
                <span className="font-mono text-slate-300">{data.customerCpfCnpjMasked}</span>
              </div>

              <div className="flex justify-between pt-1">
                <span className="text-slate-400">Data de Emissão do Laudo:</span>
                <span className="font-mono text-slate-400">
                  {new Date(data.reportIssuedAt).toLocaleString()}
                </span>
              </div>
            </div>

            <div className="text-center text-xs text-slate-500 flex items-center justify-center gap-1">
              <FaLock className="w-3 h-3 text-indigo-400" />
              Verificação protegida por criptografia assimétrica de ponta a ponta.
            </div>
          </div>
        ) : (
          <div className="space-y-4">
            <div className="p-6 bg-rose-500/10 border border-rose-500/30 rounded-2xl flex items-center gap-3">
              <FaTimesCircle className="w-10 h-10 text-rose-400 shrink-0" />
              <div>
                <h3 className="font-bold text-rose-300">DOCUMENTO NÃO ENCONTRADO OU ADULTERADO</h3>
                <p className="text-xs text-rose-400/80 mt-1">
                  O token informado não corresponde a nenhum laudo pericial oficial registrado nos servidores deste provedor. Alerta de possível falsificação ou token expirado.
                </p>
              </div>
            </div>
          </div>
        )}

        {/* Back Link */}
        <div className="pt-4 border-t border-slate-700 flex justify-center">
          <Link
            to="/network/marco-civil"
            className="text-xs text-indigo-400 hover:text-indigo-300 font-semibold flex items-center gap-2 transition"
          >
            <FaArrowLeft className="w-3 h-3" /> Voltar ao Painel Administrativo ispERP
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ReportValidation;
