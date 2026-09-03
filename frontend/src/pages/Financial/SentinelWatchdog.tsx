import React, { useState, useEffect } from 'react';
import { 
  FiShield, 
  FiAlertTriangle, 
  FiCheckCircle, 
  FiRefreshCw, 
  FiCpu, 
  FiZap, 
  FiUserX, 
  FiDollarSign,
  FiFileText
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { SentinelAuditLogDto, SentinelSeverity } from '../../types/financial';

export const SentinelWatchdog: React.FC = () => {
  const [alerts, setAlerts] = useState<SentinelAuditLogDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [sweeping, setSweeping] = useState(false);

  useEffect(() => {
    loadAlerts();
  }, []);

  const loadAlerts = async () => {
    try {
      setLoading(true);
      const data = await financialService.getSentinelAlerts();
      setAlerts(data);
    } catch (err) {
      console.error('Erro ao carregar alertas do Sentinela IA', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSweep = async () => {
    try {
      setSweeping(true);
      const updatedAlerts = await financialService.triggerSentinelSweep();
      setAlerts(updatedAlerts);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao executar varredura');
    } finally {
      setSweeping(false);
    }
  };

  const handleResolve = async (id: string) => {
    try {
      await financialService.resolveSentinelAlert(id);
      loadAlerts();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao resolver alerta');
    }
  };

  const getSeverityBadge = (sev: SentinelSeverity) => {
    switch (sev) {
      case 'CRITICAL':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-500/20 text-rose-300 border border-rose-500/30">CRÍTICO</span>;
      case 'HIGH':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-amber-500/20 text-amber-300 border border-amber-500/30">ALTA GRAVIDADE</span>;
      case 'MEDIUM':
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-yellow-500/20 text-yellow-300 border border-yellow-500/30">MÉDIA</span>;
      default:
        return <span className="px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-slate-800 text-slate-400">INFO</span>;
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 space-y-6">
      {/* Header Corporativo */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-rose-500/10 border border-rose-500/20 text-rose-400 shadow-inner">
            <FiShield size={24} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-white">Sentinela Anti-Fraude & Auditoria Forense</h1>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-rose-500/10 text-rose-400 border border-rose-500/20">
                IA Watchdog
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Varredura de anomalias patrimoniais, retenção indevida de dinheiro vivo por CPF e auditoria comportamental
            </p>
          </div>
        </div>

        <button
          onClick={handleSweep}
          disabled={sweeping}
          className="flex items-center gap-2 text-xs font-semibold bg-gradient-to-r from-rose-600 to-purple-600 hover:from-rose-500 hover:to-purple-500 text-white px-4 py-2.5 rounded-xl shadow-lg shadow-rose-950/40 transition-all cursor-pointer disabled:opacity-50"
        >
          <FiZap className={sweeping ? 'animate-spin' : ''} size={15} />
          {sweeping ? 'Executando Perícia Forense...' : 'Executar Varredura Forense com IA'}
        </button>
      </div>

      {/* Feed de Alertas e Dossiês Periciais */}
      {loading ? (
        <div className="py-24 text-center text-xs text-slate-500 flex items-center justify-center gap-3">
          <FiRefreshCw className="animate-spin text-rose-400" size={18} />
          Carregando dossiês periciais do Sentinela...
        </div>
      ) : alerts.length === 0 ? (
        <div className="py-24 text-center space-y-3 border border-dashed border-slate-800 rounded-3xl p-8 bg-slate-900/20">
          <div className="w-12 h-12 rounded-full bg-emerald-500/10 border border-emerald-500/20 text-emerald-400 mx-auto flex items-center justify-center">
            <FiCheckCircle size={24} />
          </div>
          <h3 className="text-base font-bold text-white">Operação 100% em Conformidade</h3>
          <p className="text-xs text-slate-400 max-w-md mx-auto">
            Nenhum desvio financeiro, retenção anômala de dinheiro ou suspeita de cobrança por fora foi identificada. O Sentinela continua monitorando a esteira 24/7.
          </p>
        </div>
      ) : (
        <div className="space-y-4">
          <div className="flex items-center justify-between text-xs text-slate-400 px-1">
            <span>Dossiês Periciais Pendentes ({alerts.length})</span>
            <span>Clique em "Auditar & Resolver" após tomar a ação administrativa cabível</span>
          </div>

          <div className="grid grid-cols-1 gap-4">
            {alerts.map((alert) => (
              <div
                key={alert.id}
                className="bg-slate-900/40 border border-slate-800/80 rounded-3xl p-6 shadow-xl backdrop-blur-sm space-y-4 hover:border-slate-700 transition-colors"
              >
                {/* Topo do Dossiê */}
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2 border-b border-slate-800/60 pb-3">
                  <div className="flex items-center gap-2.5">
                    {getSeverityBadge(alert.severity)}
                    <h3 className="text-sm font-bold text-white">{alert.title}</h3>
                  </div>
                  <span className="text-[11px] text-slate-500 font-mono">
                    Detectado em: {new Date(alert.createdAt).toLocaleString('pt-BR')}
                  </span>
                </div>

                {/* Descrição do Fato */}
                <p className="text-xs text-slate-300 leading-relaxed">
                  {alert.description}
                </p>

                {/* Box de Análise Forense da IA */}
                {alert.geminiAnalysis && (
                  <div className="p-4 rounded-2xl bg-purple-950/20 border border-purple-500/30 space-y-2">
                    <div className="flex items-center gap-2 text-xs font-bold text-purple-300">
                      <FiCpu size={15} />
                      <span>Diagnóstico Pericial da Inteligência Artificial:</span>
                    </div>
                    <p className="text-xs text-slate-300 leading-relaxed font-sans">
                      {alert.geminiAnalysis}
                    </p>
                  </div>
                )}

                {/* Box de Recomendação de Ação */}
                {alert.recommendedAction && (
                  <div className="p-3.5 rounded-2xl bg-slate-950 border border-slate-800 space-y-1">
                    <span className="text-[11px] font-bold text-emerald-400 block uppercase tracking-wider">
                      Recomendação Preventiva para a Diretoria:
                    </span>
                    <p className="text-xs text-slate-400">
                      {alert.recommendedAction}
                    </p>
                  </div>
                )}

                {/* Ações */}
                <div className="flex items-center justify-end pt-2">
                  <button
                    onClick={() => handleResolve(alert.id)}
                    className="flex items-center gap-1.5 px-4 py-2 bg-slate-800 hover:bg-slate-700 text-slate-200 hover:text-white rounded-xl text-xs font-semibold transition-colors cursor-pointer"
                  >
                    <FiCheckCircle size={14} className="text-emerald-400" />
                    Auditar & Resolver Alerta
                  </button>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
