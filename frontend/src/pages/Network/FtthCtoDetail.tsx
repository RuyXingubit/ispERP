import React, { useState, useEffect } from 'react';
import { FtthCto, FtthCtoPort, LightPathTraceResult } from '../../types/ftth';
import { ftthService } from '../../services/ftthService';
import {
  FaBox,
  FaSignal,
  FaUserAlt,
  FaMicrochip,
  FaCheckCircle,
  FaExclamationTriangle,
  FaTimes,
  FaRoute,
} from 'react-icons/fa';

interface FtthCtoDetailProps {
  ctoId: string;
  onClose?: () => void;
}

export const FtthCtoDetail: React.FC<FtthCtoDetailProps> = ({ ctoId, onClose }) => {
  const [cto, setCto] = useState<FtthCto | null>(null);
  const [loading, setLoading] = useState(true);
  const [selectedPort, setSelectedPort] = useState<FtthCtoPort | null>(null);
  const [lightPathResult, setLightPathResult] = useState<LightPathTraceResult | null>(null);
  const [tracingLight, setTracingLight] = useState(false);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadCto = async () => {
    try {
      setLoading(true);
      const data = await ftthService.getCtoById(ctoId);
      setCto(data);
    } catch (err: any) {
      showNotification('Erro ao carregar dados da CTO: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadCto();
  }, [ctoId]);

  const handleTraceLight = async (port: FtthCtoPort) => {
    try {
      setTracingLight(true);
      setSelectedPort(port);
      const result = await ftthService.traceLightPath(port.id);
      setLightPathResult(result);
    } catch (err: any) {
      showNotification('Erro ao rastrear rota óptica: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setTracingLight(false);
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-slate-500 flex flex-col items-center justify-center gap-3">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
        <span>Carregando painel frontal da CTO...</span>
      </div>
    );
  }

  if (!cto) {
    return <div className="p-8 text-center text-slate-500">Caixa de atendimento não encontrada.</div>;
  }

  return (
    <div className="bg-white dark:bg-slate-800 rounded-2xl border border-slate-200 dark:border-slate-700 shadow-xl p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-200 dark:border-slate-700 pb-4">
        <div className="flex items-center gap-3">
          <div className="p-3 bg-emerald-100 dark:bg-emerald-950/60 text-emerald-600 dark:text-emerald-400 rounded-xl">
            <FaBox className="w-6 h-6" />
          </div>
          <div>
            <h2 className="text-xl font-bold text-slate-900 dark:text-white flex items-center gap-2">
              {cto.name}
              <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300">
                {cto.status}
              </span>
            </h2>
            <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
              Splitter {cto.splitterType} • {cto.freePortsCount} portas livres de {cto.totalPorts} ({cto.occupancyPercentage}% ocupada)
            </p>
          </div>
        </div>

        {onClose && (
          <button
            onClick={onClose}
            className="p-2 text-slate-400 hover:text-slate-600 dark:hover:text-white rounded-lg transition"
          >
            <FaTimes className="w-5 h-5" />
          </button>
        )}
      </div>

      {/* Notifications */}
      {notification && (
        <div
          className={`p-3 rounded-lg flex items-center justify-between text-xs font-medium ${
            notification.type === 'success'
              ? 'bg-emerald-50 border border-emerald-200 text-emerald-800 dark:bg-emerald-950/40 dark:border-emerald-800 dark:text-emerald-300'
              : 'bg-rose-50 border border-rose-200 text-rose-800 dark:bg-rose-950/40 dark:border-rose-800 dark:text-rose-300'
          }`}
        >
          <div className="flex items-center gap-2">
            {notification.type === 'success' ? <FaCheckCircle className="w-4 h-4" /> : <FaExclamationTriangle className="w-4 h-4" />}
            <span>{notification.message}</span>
          </div>
        </div>
      )}

      {/* PAINEL FRONTAL DE PORTAS SC-APC */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <span className="text-xs font-bold uppercase tracking-wider text-slate-600 dark:text-slate-400">
            Painel Frontal de Adaptadores SC-APC (Portas 1 a {cto.totalPorts})
          </span>
          <div className="flex items-center gap-3 text-[11px]">
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-500" /> Livre
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-600" /> Ocupada
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-500" /> Reservada
            </span>
            <span className="flex items-center gap-1">
              <span className="w-2.5 h-2.5 rounded-full bg-rose-500" /> Defeito
            </span>
          </div>
        </div>

        <div className="grid grid-cols-2 sm:grid-cols-4 md:grid-cols-8 gap-3">
          {cto.ports?.map((port) => {
            const isOccupied = port.status === 'OCUPADA';
            const isFree = port.status === 'LIVRE';
            const isReserved = port.status === 'RESERVADA';

            return (
              <div
                key={port.id}
                onClick={() => setSelectedPort(port)}
                className={`p-3 rounded-xl border transition cursor-pointer flex flex-col items-center justify-between text-center relative ${
                  selectedPort?.id === port.id
                    ? 'ring-2 ring-indigo-500 shadow-md'
                    : 'hover:shadow-sm'
                } ${
                  isFree
                    ? 'bg-emerald-50/60 dark:bg-emerald-950/20 border-emerald-200 dark:border-emerald-800/60'
                    : isOccupied
                    ? 'bg-blue-50/60 dark:bg-blue-950/20 border-blue-200 dark:border-blue-800/60'
                    : isReserved
                    ? 'bg-amber-50/60 dark:bg-amber-950/20 border-amber-200 dark:border-amber-800/60'
                    : 'bg-rose-50/60 dark:bg-rose-950/20 border-rose-200 dark:border-rose-800/60'
                }`}
              >
                {/* Conector SC-APC Visual */}
                <div
                  className={`w-8 h-8 rounded-lg flex items-center justify-center text-xs font-bold text-white shadow-inner mb-2 ${
                    isFree
                      ? 'bg-emerald-600'
                      : isOccupied
                      ? 'bg-blue-600'
                      : isReserved
                      ? 'bg-amber-500'
                      : 'bg-rose-600'
                  }`}
                >
                  {port.portNumber}
                </div>

                <span className="text-[11px] font-bold text-slate-800 dark:text-slate-200 truncate w-full">
                  {port.customerName || (isFree ? 'Disponível' : port.status)}
                </span>

                {port.pppoeUser && (
                  <span className="text-[10px] font-mono text-slate-500 dark:text-slate-400 truncate w-full mt-0.5">
                    {port.pppoeUser}
                  </span>
                )}

                {isOccupied && (
                  <button
                    onClick={(e) => {
                      e.stopPropagation();
                      handleTraceLight(port);
                    }}
                    className="mt-2 text-[10px] text-indigo-600 dark:text-indigo-400 hover:underline flex items-center gap-1 font-semibold"
                    title="Rastrear rota óptica da porta"
                  >
                    <FaRoute className="w-3 h-3" /> Rota Óptica
                  </button>
                )}
              </div>
            );
          })}
        </div>
      </div>

      {/* Modal / Detalhe da Rota Óptica (Light Path Trace) */}
      {lightPathResult && selectedPort && (
        <div className="bg-slate-50 dark:bg-slate-900/60 border border-slate-200 dark:border-slate-700 rounded-xl p-5 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-700 pb-3">
            <div className="flex items-center gap-2">
              <FaRoute className="w-5 h-5 text-indigo-600" />
              <div>
                <h3 className="text-sm font-bold text-slate-900 dark:text-white">
                  Rastreamento Óptico (Power Budget) • Porta {selectedPort.portNumber}
                </h3>
                <span className="text-xs text-slate-500">
                  Origem: {lightPathResult.sourcePopName} ➔ Destino: {cto.name}
                </span>
              </div>
            </div>
            <button onClick={() => setLightPathResult(null)} className="text-xs text-slate-400 hover:text-slate-600">
              ✕ Fechar Rota
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
            <div className="p-3 bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700">
              <span className="text-xs text-slate-500 block">Atenuação Teórica Calculada</span>
              <span className="text-xl font-bold text-slate-900 dark:text-white">
                {lightPathResult.totalAttenuationDb} dB
              </span>
            </div>
            <div className="p-3 bg-white dark:bg-slate-800 rounded-lg border border-slate-200 dark:border-slate-700">
              <span className="text-xs text-slate-500 block">Potência Estimada na ONU (Rx Power)</span>
              <span className="text-xl font-bold text-emerald-600 dark:text-emerald-400 font-mono">
                {lightPathResult.estimatedRxPowerDbm} dBm
              </span>
            </div>
          </div>

          <div className="space-y-2">
            <span className="text-xs font-bold text-slate-700 dark:text-slate-300">Nós do Caminho Óptico:</span>
            <div className="space-y-1.5 max-h-48 overflow-y-auto">
              {lightPathResult.nodes.map((node, idx) => (
                <div
                  key={idx}
                  className="flex items-center justify-between p-2 rounded bg-white dark:bg-slate-800 text-xs border border-slate-100 dark:border-slate-700"
                >
                  <div className="flex items-center gap-2">
                    <span className="font-mono text-indigo-500 font-bold">#{idx + 1}</span>
                    <span className="font-semibold text-slate-800 dark:text-slate-200">{node.name}</span>
                    <span className="text-slate-400 text-[11px]">({node.details})</span>
                  </div>
                  <div className="text-right font-mono text-[11px]">
                    <span className="text-slate-400">+{node.addedAttenuationDb} dB</span> •{' '}
                    <span className="font-bold text-slate-700 dark:text-slate-300">
                      Total: {node.cumulativeAttenuationDb} dB
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        </div>
      )}
    </div>
  );
};

export default FtthCtoDetail;
