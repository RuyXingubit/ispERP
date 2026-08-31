import React, { useState, useEffect } from 'react';
import { FtthCto, FtthCable, FtthClosure, FtthPop, FtthFeasibilityResponse } from '../../types/ftth';
import { ftthService } from '../../services/ftthService';
import {
  FaMapMarkedAlt,
  FaSearchLocation,
  FaBox,
  FaProjectDiagram,
  FaServer,
  FaCheckCircle,
  FaTimesCircle,
} from 'react-icons/fa';

export const FtthMap: React.FC = () => {
  const [ctos, setCtos] = useState<FtthCto[]>([]);
  const [cables, setCables] = useState<FtthCable[]>([]);
  const [closures, setClosures] = useState<FtthClosure[]>([]);
  const [pops, setPops] = useState<FtthPop[]>([]);
  const [loading, setLoading] = useState(true);

  // Consulta de Viabilidade
  const [searchLat, setSearchLat] = useState('-23.550520');
  const [searchLng, setSearchLng] = useState('-46.633308');
  const [maxDistance, setMaxDistance] = useState(200);
  const [feasibilityResult, setFeasibilityResult] = useState<FtthFeasibilityResponse | null>(null);
  const [checkingFeasibility, setCheckingFeasibility] = useState(false);

  // Elemento Selecionado
  const [selectedItem, setSelectedItem] = useState<{ type: string; item: any } | null>(null);

  const loadMapData = async () => {
    try {
      setLoading(true);
      const [ctosData, cablesData, closuresData, popsData] = await Promise.all([
        ftthService.getAllCtos(),
        ftthService.getAllCables(),
        ftthService.getAllClosures(),
        ftthService.getAllPops(),
      ]);
      setCtos(ctosData);
      setCables(cablesData);
      setClosures(closuresData);
      setPops(popsData);
    } catch (err: any) {
      console.error('Erro ao carregar dados do mapa FTTH:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadMapData();
  }, []);

  const handleCheckFeasibility = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setCheckingFeasibility(true);
      const res = await ftthService.checkFeasibility({
        latitude: Number(searchLat),
        longitude: Number(searchLng),
        maxDistanceMeters: Number(maxDistance),
      });
      setFeasibilityResult(res);
    } catch (err: any) {
      console.error('Erro ao consultar viabilidade:', err);
    } finally {
      setCheckingFeasibility(false);
    }
  };

  return (
    <div className="grid grid-cols-1 lg:grid-cols-4 gap-6">
      {/* Coluna 1: Painel de Viabilidade & Camadas */}
      <div className="lg:col-span-1 space-y-4">
        {/* Card de Consulta de Viabilidade */}
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
          <div className="flex items-center gap-2 border-b border-slate-200 dark:border-slate-700 pb-3">
            <FaSearchLocation className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
            <h3 className="font-bold text-slate-900 dark:text-white text-sm">Consulta de Viabilidade FTTH</h3>
          </div>

          <form onSubmit={handleCheckFeasibility} className="space-y-3 text-xs">
            <div>
              <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">Latitude do Cliente</label>
              <input
                type="text"
                required
                value={searchLat}
                onChange={(e) => setSearchLat(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">Longitude do Cliente</label>
              <input
                type="text"
                required
                value={searchLng}
                onChange={(e) => setSearchLng(e.target.value)}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>

            <div>
              <label className="block font-semibold text-slate-600 dark:text-slate-400 mb-1">
                Raio Máximo do Cabo Drop (Metros)
              </label>
              <input
                type="number"
                min="50"
                max="500"
                value={maxDistance}
                onChange={(e) => setMaxDistance(Number(e.target.value))}
                className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
              />
            </div>

            <button
              type="submit"
              disabled={checkingFeasibility}
              className="w-full py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition shadow-sm"
            >
              {checkingFeasibility ? 'Calculando...' : 'Verificar Viabilidade'}
            </button>
          </form>

          {/* Resultado de Viabilidade */}
          {feasibilityResult && (
            <div
              className={`p-3 rounded-lg border text-xs space-y-2 ${
                feasibilityResult.viable
                  ? 'bg-emerald-50 dark:bg-emerald-950/30 border-emerald-300 text-emerald-800 dark:text-emerald-300'
                  : 'bg-rose-50 dark:bg-rose-950/30 border-rose-300 text-rose-800 dark:text-rose-300'
              }`}
            >
              <div className="flex items-center gap-2 font-bold">
                {feasibilityResult.viable ? (
                  <FaCheckCircle className="w-4 h-4 text-emerald-600" />
                ) : (
                  <FaTimesCircle className="w-4 h-4 text-rose-600" />
                )}
                <span>
                  {feasibilityResult.viable
                    ? `VIABILIDADE APROVADA (${feasibilityResult.viableCtosCount} CTOs no Raio)`
                    : 'SEM VIABILIDADE NO RAIO INFORMADO'}
                </span>
              </div>

              {feasibilityResult.nearbyCtos.length > 0 && (
                <div className="space-y-1.5 pt-2 border-t border-slate-200 dark:border-slate-700/60 max-h-40 overflow-y-auto">
                  {feasibilityResult.nearbyCtos.map((item, idx) => (
                    <div key={idx} className="flex items-center justify-between text-[11px]">
                      <span>{item.cto.name}</span>
                      <span className="font-mono">
                        {item.distanceMeters}m • {item.freePorts} portas livres
                      </span>
                    </div>
                  ))}
                </div>
              )}
            </div>
          )}
        </div>

        {/* Resumo de Ativos */}
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-3 text-xs">
          <span className="font-bold text-slate-900 dark:text-white uppercase tracking-wider block">Ativos Cadastrados</span>
          <div className="space-y-2">
            <div className="flex items-center justify-between p-2 rounded bg-slate-50 dark:bg-slate-900/60">
              <span className="flex items-center gap-2">
                <FaBox className="text-emerald-500" /> Caixas de Atendimento (CTO)
              </span>
              <span className="font-bold text-slate-800 dark:text-slate-200">{ctos.length}</span>
            </div>
            <div className="flex items-center justify-between p-2 rounded bg-slate-50 dark:bg-slate-900/60">
              <span className="flex items-center gap-2">
                <FaProjectDiagram className="text-indigo-500" /> Caixas de Emenda (CEO)
              </span>
              <span className="font-bold text-slate-800 dark:text-slate-200">{closures.length}</span>
            </div>
            <div className="flex items-center justify-between p-2 rounded bg-slate-50 dark:bg-slate-900/60">
              <span className="flex items-center gap-2">
                <FaServer className="text-purple-500" /> POPs / Centrais
              </span>
              <span className="font-bold text-slate-800 dark:text-slate-200">{pops.length}</span>
            </div>
          </div>
        </div>
      </div>

      {/* Coluna 2, 3, 4: Mapa GIS Georreferenciado */}
      <div className="lg:col-span-3 bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <FaMapMarkedAlt className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
            <h2 className="font-bold text-slate-900 dark:text-white">Mapa de Topologia e Traçado de Fibra</h2>
          </div>
          <span className="text-xs text-slate-400">Coordenadas e Ativos em Tempo Real</span>
        </div>

        {/* Visualizador de Mapa Estilizado */}
        <div className="w-full h-[600px] bg-slate-950 rounded-xl border border-slate-800 relative overflow-hidden flex items-center justify-center p-6">
          {/* Grid de Fundo */}
          <div className="absolute inset-0 bg-[linear-gradient(to_right,#1e293b_1px,transparent_1px),linear-gradient(to_bottom,#1e293b_1px,transparent_1px)] bg-[size:4rem_4rem] [mask-image:radial-gradient(ellipse_60%_50%_at_50%_50%,#000_70%,transparent_100%)] opacity-30" />

          {/* Marcadores e Elementos no Mapa */}
          <div className="relative z-10 w-full h-full flex flex-col justify-between">
            {/* Lista dos Elementos Mapeados */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3">
              {pops.map((pop) => (
                <div
                  key={pop.id}
                  onClick={() => setSelectedItem({ type: 'POP', item: pop })}
                  className="p-3 bg-purple-950/80 border border-purple-600/50 rounded-lg text-xs cursor-pointer hover:scale-105 transition"
                >
                  <div className="flex items-center gap-2 text-purple-300 font-bold">
                    <FaServer className="w-4 h-4" /> {pop.name}
                  </div>
                  <span className="text-[10px] text-purple-400 block mt-1">
                    {pop.latitude}, {pop.longitude}
                  </span>
                </div>
              ))}

              {closures.map((closure) => (
                <div
                  key={closure.id}
                  onClick={() => setSelectedItem({ type: 'CEO', item: closure })}
                  className="p-3 bg-indigo-950/80 border border-indigo-600/50 rounded-lg text-xs cursor-pointer hover:scale-105 transition"
                >
                  <div className="flex items-center gap-2 text-indigo-300 font-bold">
                    <FaProjectDiagram className="w-4 h-4" /> {closure.name}
                  </div>
                  <span className="text-[10px] text-indigo-400 block mt-1">
                    Capacidade: {closure.capacityFusions} fusões • {closure.status}
                  </span>
                </div>
              ))}

              {ctos.map((cto) => (
                <div
                  key={cto.id}
                  onClick={() => setSelectedItem({ type: 'CTO', item: cto })}
                  className="p-3 bg-emerald-950/80 border border-emerald-600/50 rounded-lg text-xs cursor-pointer hover:scale-105 transition"
                >
                  <div className="flex items-center gap-2 text-emerald-300 font-bold">
                    <FaBox className="w-4 h-4" /> {cto.name}
                  </div>
                  <span className="text-[10px] text-emerald-400 block mt-1">
                    {cto.freePortsCount} portas livres de {cto.totalPorts} ({cto.occupancyPercentage}% ocupada)
                  </span>
                </div>
              ))}
            </div>

            {/* Modal / Detalhe Flutuante do Item Selecionado */}
            {selectedItem && (
              <div className="p-4 bg-slate-900/90 backdrop-blur-md border border-slate-700 rounded-xl text-xs space-y-2 shadow-2xl max-w-md self-center">
                <div className="flex items-center justify-between">
                  <span className="font-bold text-white uppercase">{selectedItem.type}: {selectedItem.item.name}</span>
                  <button onClick={() => setSelectedItem(null)} className="text-slate-400 hover:text-white">✕</button>
                </div>
                <div className="text-slate-300 space-y-1">
                  {selectedItem.type === 'CTO' && (
                    <>
                      <p>Splitter: {selectedItem.item.splitterType}</p>
                      <p>Portas Livres: {selectedItem.item.freePortsCount} de {selectedItem.item.totalPorts}</p>
                      <p>Coordenadas: {selectedItem.item.latitude}, {selectedItem.item.longitude}</p>
                    </>
                  )}
                  {selectedItem.type === 'CEO' && (
                    <>
                      <p>Tipo: {selectedItem.item.closureType}</p>
                      <p>Capacidade: {selectedItem.item.capacityFusions} fusões ({selectedItem.item.trayCount} bandejas)</p>
                    </>
                  )}
                  {selectedItem.type === 'POP' && (
                    <>
                      <p>Endereço: {selectedItem.item.address || 'Central Principal'}</p>
                    </>
                  )}
                </div>
              </div>
            )}
          </div>
        </div>
      </div>
    </div>
  );
};

export default FtthMap;
