import React, { useState, useEffect, useCallback } from 'react';
import {
  NocMonitoringSummary,
  OltPonPort,
  FtthIncident,
  IncidentSeverity,
} from '../../types/monitoring';
import { monitoringService } from '../../services/monitoringService';
import {
  FaHeartbeat,
  FaServer,
  FaNetworkWired,
  FaBroadcastTower,
  FaExclamationTriangle,
  FaCheckCircle,
  FaSyncAlt,
  FaBolt,
  FaMapMarkerAlt,
  FaTruck,
} from 'react-icons/fa';

export const NocDashboard: React.FC = () => {
  const [summary, setSummary] = useState<NocMonitoringSummary | null>(null);
  const [ponPorts, setPonPorts] = useState<OltPonPort[]>([]);
  const [loading, setLoading] = useState(true);
  const [polling, setPolling] = useState(false);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  // Modais de Ação
  const [selectedIncident, setSelectedIncident] = useState<FtthIncident | null>(null);
  const [isDispatchModalOpen, setIsDispatchModalOpen] = useState(false);
  const [dispatchNotes, setDispatchNotes] = useState('');

  const [isResolveModalOpen, setIsResolveModalOpen] = useState(false);
  const [resolveNotes, setResolveNotes] = useState('');

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [sumData, ponsData] = await Promise.all([
        monitoringService.getSummary(),
        monitoringService.getAllPonPorts(),
      ]);
      setSummary(sumData);
      setPonPorts(ponsData);
    } catch (err: any) {
      showNotification('Erro ao carregar dados do NOC: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
    // Auto-refresh a cada 30 segundos
    const interval = setInterval(loadData, 30000);
    return () => clearInterval(interval);
  }, [loadData]);

  const handleForcePoll = async () => {
    try {
      setPolling(true);
      const data = await monitoringService.forcePollCycle();
      setSummary(data);
      const pons = await monitoringService.getAllPonPorts();
      setPonPorts(pons);
      showNotification('Ciclo de telemetria e correlação executado com sucesso!');
    } catch (err: any) {
      showNotification('Erro ao executar polling forçado: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setPolling(false);
    }
  };

  const handleDispatch = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIncident) return;

    try {
      await monitoringService.dispatchIncident(selectedIncident.id, {
        notes: dispatchNotes || undefined,
      });
      showNotification(`Equipe de campo despachada para o incidente: ${selectedIncident.title}`);
      setIsDispatchModalOpen(false);
      setDispatchNotes('');
      loadData();
    } catch (err: any) {
      showNotification('Erro ao despachar equipe: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  const handleResolve = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedIncident) return;

    try {
      await monitoringService.resolveIncident(selectedIncident.id, {
        rootCauseNotes: resolveNotes,
      });
      showNotification(`Incidente ${selectedIncident.title} finalizado com sucesso!`);
      setIsResolveModalOpen(false);
      setResolveNotes('');
      loadData();
    } catch (err: any) {
      showNotification('Erro ao resolver incidente: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  const getSeverityBadge = (severity: IncidentSeverity) => {
    switch (severity) {
      case 'CRITICAL':
        return 'bg-rose-500/20 text-rose-300 border-rose-500/40 animate-pulse';
      case 'MAJOR':
        return 'bg-amber-500/20 text-amber-300 border-amber-500/40';
      case 'WARNING':
        return 'bg-yellow-500/20 text-yellow-300 border-yellow-500/40';
      default:
        return 'bg-blue-500/20 text-blue-300 border-blue-500/40';
    }
  };

  if (loading && !summary) {
    return (
      <div className="p-8 text-center text-slate-500 flex flex-col items-center justify-center gap-3">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
        <span>Carregando telemetria NOC e alarmes ópticos...</span>
      </div>
    );
  }

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-slate-900 text-white p-6 rounded-2xl border border-slate-800 shadow-2xl">
        <div className="flex items-center gap-4">
          <div className="p-3.5 bg-indigo-950/80 border border-indigo-500/40 text-indigo-400 rounded-2xl shadow-inner">
            <FaHeartbeat className="w-8 h-8 animate-pulse text-indigo-400" />
          </div>
          <div>
            <div className="flex items-center gap-3">
              <h1 className="text-2xl font-bold tracking-tight">NOC • Monitoramento & Telemetria Óptica</h1>
              <span className="px-2.5 py-0.5 rounded-full text-xs font-bold uppercase tracking-wider bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                Tempo Real
              </span>
            </div>
            <p className="text-xs text-slate-400 mt-1">
              Detecção inteligente de rompimentos de fibra (LOS), falhas de energia (Dying Gasp) e saúde das PONs
            </p>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={handleForcePoll}
            disabled={polling}
            className="flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-lg shadow-indigo-600/30 transition"
          >
            <FaSyncAlt className={`w-3.5 h-3.5 ${polling ? 'animate-spin' : ''}`} />
            {polling ? 'Consultando OLTs...' : 'Forçar Varredura (Polling)'}
          </button>
        </div>
      </div>

      {/* Notifications */}
      {notification && (
        <div
          className={`p-4 rounded-xl flex items-center justify-between shadow-md text-xs font-semibold ${
            notification.type === 'success'
              ? 'bg-emerald-950/60 border border-emerald-700 text-emerald-300'
              : 'bg-rose-950/60 border border-rose-700 text-rose-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === 'success' ? <FaCheckCircle className="w-4 h-4" /> : <FaExclamationTriangle className="w-4 h-4" />}
            <span>{notification.message}</span>
          </div>
        </div>
      )}

      {/* KPI METRIC CARDS */}
      {summary && (
        <div className="grid grid-cols-2 sm:grid-cols-3 lg:grid-cols-6 gap-4">
          {/* OLTs */}
          <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl space-y-1 shadow-md">
            <div className="flex items-center justify-between text-slate-400 text-xs font-semibold">
              <span>OLTs Ativas</span>
              <FaServer className="text-indigo-400" />
            </div>
            <span className="text-2xl font-bold text-white block">{summary.totalOlts}</span>
            <span className="text-[10px] text-slate-500">{summary.activePonPorts} PONs operacionais</span>
          </div>

          {/* ONUs Online */}
          <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl space-y-1 shadow-md">
            <div className="flex items-center justify-between text-emerald-400 text-xs font-semibold">
              <span>ONUs Online</span>
              <FaCheckCircle className="text-emerald-400" />
            </div>
            <span className="text-2xl font-bold text-white block">{summary.onlineOnus}</span>
            <span className="text-[10px] text-emerald-400 font-semibold">{summary.globalHealthPercentage}% sinal saudável</span>
          </div>

          {/* ONUs em LOS (Loss of Signal) */}
          <div
            className={`p-4 rounded-xl space-y-1 shadow-md border ${
              summary.losOnus > 0
                ? 'bg-rose-950/40 border-rose-700/80 text-rose-300'
                : 'bg-slate-900 border-slate-800 text-slate-400'
            }`}
          >
            <div className="flex items-center justify-between text-xs font-semibold">
              <span>ONUs em LOS (Sem Luz)</span>
              <FaExclamationTriangle className={summary.losOnus > 0 ? 'text-rose-400 animate-bounce' : 'text-slate-500'} />
            </div>
            <span className="text-2xl font-bold text-white block">{summary.losOnus}</span>
            <span className="text-[10px]">{summary.losOnus > 0 ? 'Possível rompimento ativo' : 'Nenhuma perda de luz'}</span>
          </div>

          {/* Dying Gasp (Falta de Energia) */}
          <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl space-y-1 shadow-md">
            <div className="flex items-center justify-between text-amber-400 text-xs font-semibold">
              <span>Dying Gasp</span>
              <FaBolt className="text-amber-400" />
            </div>
            <span className="text-2xl font-bold text-white block">{summary.dyingGaspOnus}</span>
            <span className="text-[10px] text-slate-500">Queda de energia no cliente</span>
          </div>

          {/* Total ONUs */}
          <div className="bg-slate-900 border border-slate-800 p-4 rounded-xl space-y-1 shadow-md">
            <div className="flex items-center justify-between text-slate-400 text-xs font-semibold">
              <span>Total Provisionadas</span>
              <FaNetworkWired className="text-purple-400" />
            </div>
            <span className="text-2xl font-bold text-white block">{summary.totalOnus}</span>
            <span className="text-[10px] text-slate-500">{summary.offlineOnus} offline/desligadas</span>
          </div>

          {/* Incidentes Ativos */}
          <div
            className={`p-4 rounded-xl space-y-1 shadow-md border ${
              summary.criticalIncidentsCount > 0
                ? 'bg-rose-950/60 border-rose-600 text-rose-300 ring-2 ring-rose-500'
                : 'bg-slate-900 border-slate-800 text-slate-400'
            }`}
          >
            <div className="flex items-center justify-between text-xs font-semibold">
              <span>Incidentes Ativos</span>
              <FaBroadcastTower className={summary.criticalIncidentsCount > 0 ? 'text-rose-400' : 'text-slate-500'} />
            </div>
            <span className="text-2xl font-bold text-white block">{summary.activeIncidentsCount}</span>
            <span className="text-[10px] font-bold text-rose-400">
              {summary.criticalIncidentsCount} Crítico(s)
            </span>
          </div>
        </div>
      )}

      {/* SEÇÃO 1: INCIDENTES ATIVOS E ALARMES DE ROMPIMENTO */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <FaBroadcastTower className="w-5 h-5 text-rose-400" />
            <h2 className="text-base font-bold text-white">Incidentes Ativos & Alarmes de Rompimento de Fibra</h2>
          </div>
          <span className="text-xs font-mono text-slate-400">
            {summary?.activeIncidents.length || 0} incidente(s) em aberto
          </span>
        </div>

        {summary?.activeIncidents.length === 0 ? (
          <div className="p-8 text-center bg-slate-950/60 rounded-xl border border-slate-800/80 text-slate-400 text-xs flex flex-col items-center justify-center gap-2">
            <FaCheckCircle className="w-8 h-8 text-emerald-500" />
            <span className="font-bold text-slate-200">Rede FTTH Operando em 100% de Normalidade</span>
            <span>Nenhum rompimento ou alarme em massa detectado no momento.</span>
          </div>
        ) : (
          <div className="space-y-3">
            {summary?.activeIncidents.map((incident) => (
              <div
                key={incident.id}
                className="bg-slate-950/90 border border-slate-800 hover:border-slate-700 rounded-xl p-4 transition space-y-3 shadow-lg"
              >
                <div className="flex flex-col md:flex-row md:items-center justify-between gap-2">
                  <div className="flex items-center gap-3">
                    <span className={`px-2.5 py-1 text-[10px] font-bold rounded-lg border ${getSeverityBadge(incident.severity)}`}>
                      {incident.severity}
                    </span>
                    <span className="text-xs font-semibold px-2 py-0.5 rounded bg-indigo-950 border border-indigo-800 text-indigo-300">
                      {incident.incidentTypeDescription}
                    </span>
                    <h3 className="font-bold text-white text-sm">{incident.title}</h3>
                  </div>

                  <div className="flex items-center gap-2">
                    <span className="px-2 py-1 text-[11px] font-mono rounded bg-slate-800 text-slate-300">
                      Status: <strong>{incident.status}</strong>
                    </span>
                    {incident.status === 'ACTIVE' && (
                      <button
                        onClick={() => {
                          setSelectedIncident(incident);
                          setIsDispatchModalOpen(true);
                        }}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-bold transition shadow-md"
                      >
                        <FaTruck className="w-3 h-3" /> Despachar Equipe
                      </button>
                    )}
                    {incident.status !== 'RESOLVED' && (
                      <button
                        onClick={() => {
                          setSelectedIncident(incident);
                          setIsResolveModalOpen(true);
                        }}
                        className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg text-xs font-bold transition shadow-md"
                      >
                        <FaCheckCircle className="w-3 h-3" /> Resolver
                      </button>
                    )}
                  </div>
                </div>

                <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs text-slate-400 bg-slate-900/60 p-3 rounded-lg border border-slate-800/60">
                  <div>
                    <span className="text-slate-500 block">Clientes Impactados:</span>
                    <span className="font-bold text-rose-400 text-sm font-mono">{incident.affectedCustomersCount} assinantes</span>
                  </div>

                  <div>
                    <span className="text-slate-500 block">CTOs Inoperantes:</span>
                    <span className="font-semibold text-slate-200">
                      {incident.affectedCtoNames?.join(', ') || 'Avaliando...'}
                    </span>
                  </div>

                  <div>
                    <span className="text-slate-500 block">Ponto Estimado do Rompimento:</span>
                    <span className="font-mono text-indigo-400 flex items-center gap-1">
                      <FaMapMarkerAlt /> {incident.estimatedCutLatitude}, {incident.estimatedCutLongitude}
                    </span>
                  </div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>

      {/* SEÇÃO 2: GRID DE SAÚDE DAS PORTAS PON */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 space-y-4 shadow-xl">
        <div className="flex items-center justify-between border-b border-slate-800 pb-3">
          <div className="flex items-center gap-2">
            <FaNetworkWired className="w-5 h-5 text-indigo-400" />
            <h2 className="text-base font-bold text-white">Status das Portas PON (GPON / EPON)</h2>
          </div>
          <span className="text-xs text-slate-400">{ponPorts.length} portas monitoradas</span>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-4">
          {ponPorts.map((pon) => {
            const isFault = pon.operStatus === 'FAULT';
            const isDegraded = pon.operStatus === 'DEGRADED';

            return (
              <div
                key={pon.id}
                className={`p-4 rounded-xl border transition space-y-2.5 ${
                  isFault
                    ? 'bg-rose-950/40 border-rose-800 text-rose-300'
                    : isDegraded
                    ? 'bg-amber-950/40 border-amber-800 text-amber-300'
                    : 'bg-slate-950/80 border-slate-800 text-slate-300'
                }`}
              >
                <div className="flex items-center justify-between">
                  <span className="font-bold text-white text-xs">{pon.ponName}</span>
                  <span
                    className={`px-2 py-0.5 text-[10px] font-bold rounded-full ${
                      isFault
                        ? 'bg-rose-500 text-white'
                        : isDegraded
                        ? 'bg-amber-500 text-black'
                        : 'bg-emerald-500/20 text-emerald-300'
                    }`}
                  >
                    {pon.operStatus}
                  </span>
                </div>

                <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                  <div
                    className={`h-full rounded-full transition-all duration-300 ${
                      isFault ? 'bg-rose-500' : isDegraded ? 'bg-amber-500' : 'bg-emerald-500'
                    }`}
                    style={{ width: `${pon.healthPercentage}%` }}
                  />
                </div>

                <div className="flex items-center justify-between text-[11px] text-slate-400">
                  <span>
                    ONUs: <strong>{pon.onlineOnus}</strong> / {pon.totalOnus}
                  </span>
                  <span className="font-mono">{pon.healthPercentage}% Saúde</span>
                </div>

                {pon.losOnus > 0 && (
                  <div className="text-[10px] font-bold text-rose-400 flex items-center gap-1">
                    <FaExclamationTriangle className="w-3 h-3" /> {pon.losOnus} ONU(s) em LOS
                  </div>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Modal Despacho */}
      {isDispatchModalOpen && selectedIncident && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-white flex items-center gap-2">
                <FaTruck className="w-4 h-4 text-rose-400" /> Despachar Equipe de Campo
              </h3>
              <button onClick={() => setIsDispatchModalOpen(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <form onSubmit={handleDispatch} className="space-y-4 text-xs">
              <p className="text-slate-300">
                Você está gerando uma Ordem de Serviço de emergência para reparo do incidente{' '}
                <strong>{selectedIncident.title}</strong>.
              </p>

              <div>
                <label className="block font-semibold text-slate-300 mb-1">Notas / Instruções para o Técnico</label>
                <textarea
                  rows={3}
                  placeholder="Ex: Levar máquina de fusão e bobina de 12FO. Rompimento provável no Poste P-102."
                  value={dispatchNotes}
                  onChange={(e) => setDispatchNotes(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white text-xs focus:ring-2 focus:ring-rose-500"
                />
              </div>

              <div className="pt-2 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsDispatchModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-700 text-slate-300 rounded-lg font-semibold hover:bg-slate-800"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg font-semibold shadow-md"
                >
                  Confirmar Despacho
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Resolução */}
      {isResolveModalOpen && selectedIncident && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-white flex items-center gap-2">
                <FaCheckCircle className="w-4 h-4 text-emerald-400" /> Finalizar / Resolver Incidente
              </h3>
              <button onClick={() => setIsResolveModalOpen(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <form onSubmit={handleResolve} className="space-y-4 text-xs">
              <div>
                <label className="block font-semibold text-slate-300 mb-1">Causa Raiz e Relatório do Reparo *</label>
                <textarea
                  required
                  rows={3}
                  placeholder="Ex: Cabo rompido por caminhão alto. Realizada sangria e fusão de 4 fibras na CEO-02. Sinal normalizado."
                  value={resolveNotes}
                  onChange={(e) => setResolveNotes(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white text-xs focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="pt-2 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsResolveModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-700 text-slate-300 rounded-lg font-semibold hover:bg-slate-800"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold shadow-md"
                >
                  Salvar Resolução
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default NocDashboard;
