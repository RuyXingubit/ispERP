import React, { useState, useEffect, useCallback } from 'react';
import {
  FtthCto,
  FtthClosure,
  FtthCable,
  FtthPop,
  FiberColorStandard,
  FtthCableType,
  FtthClosureType,
} from '../../types/ftth';
import { ftthService } from '../../services/ftthService';
import { FtthFusionDiagram } from './FtthFusionDiagram';
import { FtthCtoDetail } from './FtthCtoDetail';
import { FtthMap } from './FtthMap';
import {
  FaNetworkWired,
  FaBox,
  FaProjectDiagram,
  FaMapMarkedAlt,
  FaPlus,
  FaSyncAlt,
  FaCheckCircle,
  FaExclamationTriangle,
  FaSearch,
  FaPalette,
} from 'react-icons/fa';

export const FtthManager: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'ctos' | 'fusions' | 'map' | 'cables'>('ctos');
  const [ctos, setCtos] = useState<FtthCto[]>([]);
  const [closures, setClosures] = useState<FtthClosure[]>([]);
  const [cables, setCables] = useState<FtthCable[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Seleções para detalhe/modal
  const [selectedClosureId, setSelectedClosureId] = useState<string | null>(null);
  const [selectedCtoId, setSelectedCtoId] = useState<string | null>(null);

  // Modais de Criação
  const [isCtoModalOpen, setIsCtoModalOpen] = useState(false);
  const [ctoForm, setCtoForm] = useState({
    name: '',
    latitude: '-23.550520',
    longitude: '-46.633308',
    totalPorts: 16,
    splitterType: 'BALANCED_1_16',
    description: '',
  });

  const [isClosureModalOpen, setIsClosureModalOpen] = useState(false);
  const [closureForm, setClosureForm] = useState({
    name: '',
    closureType: 'DOMO' as FtthClosureType,
    capacityFusions: 48,
    trayCount: 4,
  });

  const [isCableModalOpen, setIsCableModalOpen] = useState(false);
  const [cableForm, setCableForm] = useState({
    name: '',
    cableType: 'DISTRIBUICAO' as FtthCableType,
    fiberCount: 12,
    tubeCount: 1,
    colorStandard: 'ABNT_NBR_14106' as FiberColorStandard,
    lengthMeters: 1000,
  });

  // Notificações
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [ctosData, closuresData, cablesData] = await Promise.all([
        ftthService.getAllCtos(),
        ftthService.getAllClosures(),
        ftthService.getAllCables(),
      ]);
      setCtos(ctosData);
      setClosures(closuresData);
      setCables(cablesData);
      if (closuresData.length > 0 && !selectedClosureId) {
        setSelectedClosureId(closuresData[0].id);
      }
    } catch (err: any) {
      showNotification('Erro ao carregar dados FTTH: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  }, [selectedClosureId]);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Criar CTO
  const handleCreateCto = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ftthService.createCto({
        name: ctoForm.name,
        latitude: Number(ctoForm.latitude),
        longitude: Number(ctoForm.longitude),
        totalPorts: Number(ctoForm.totalPorts),
        splitterType: ctoForm.splitterType,
        description: ctoForm.description || undefined,
      });
      showNotification('Caixa de atendimento (CTO) criada com sucesso!');
      setIsCtoModalOpen(false);
      setCtoForm({ name: '', latitude: '-23.550520', longitude: '-46.633308', totalPorts: 16, splitterType: 'BALANCED_1_16', description: '' });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao criar CTO: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Criar CEO
  const handleCreateClosure = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ftthService.createClosure({
        name: closureForm.name,
        closureType: closureForm.closureType,
        capacityFusions: Number(closureForm.capacityFusions),
        trayCount: Number(closureForm.trayCount),
      });
      showNotification('Caixa de emenda (CEO) criada com sucesso!');
      setIsClosureModalOpen(false);
      setClosureForm({ name: '', closureType: 'DOMO', capacityFusions: 48, trayCount: 4 });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao criar CEO: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Criar Cabo
  const handleCreateCable = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ftthService.createCable({
        name: cableForm.name,
        cableType: cableForm.cableType,
        fiberCount: Number(cableForm.fiberCount),
        tubeCount: Number(cableForm.tubeCount),
        colorStandard: cableForm.colorStandard,
        lengthMeters: Number(cableForm.lengthMeters),
      });
      showNotification('Cabo óptico cadastrado com sucesso!');
      setIsCableModalOpen(false);
      setCableForm({ name: '', cableType: 'DISTRIBUICAO', fiberCount: 12, tubeCount: 1, colorStandard: 'ABNT_NBR_14106', lengthMeters: 1000 });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao cadastrar cabo: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  const filteredCtos = ctos.filter((c) =>
    c.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
    (c.description && c.description.toLowerCase().includes(searchTerm.toLowerCase()))
  );

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <FaNetworkWired className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">Documentação de Rede FTTH & Fibras</h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Gestão de CTOs, diagramas unifilares de fusões (ABNT & TIA-598), mapa GIS e viabilidade de atendimento
          </p>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => loadData()}
            className="p-2 text-slate-600 dark:text-slate-300 hover:bg-slate-100 dark:hover:bg-slate-700 rounded-lg transition"
            title="Atualizar"
          >
            <FaSyncAlt className="w-5 h-5" />
          </button>
          <button
            onClick={() => setIsCtoModalOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-medium shadow-sm transition text-xs"
          >
            <FaPlus className="w-3.5 h-3.5" /> Nova CTO
          </button>
          <button
            onClick={() => setIsClosureModalOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium shadow-sm transition text-xs"
          >
            <FaPlus className="w-3.5 h-3.5" /> Nova CEO
          </button>
          <button
            onClick={() => setIsCableModalOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg font-medium shadow-sm transition text-xs"
          >
            <FaPlus className="w-3.5 h-3.5" /> Novo Cabo
          </button>
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
            {notification.type === 'success' ? <FaCheckCircle className="w-5 h-5" /> : <FaExclamationTriangle className="w-5 h-5" />}
            <span className="font-medium text-sm">{notification.message}</span>
          </div>
        </div>
      )}

      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-700 gap-6">
        <button
          onClick={() => {
            setActiveTab('ctos');
            setSelectedCtoId(null);
          }}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'ctos'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaBox className="w-4 h-4" /> Caixas de Atendimento (CTOs) ({ctos.length})
        </button>

        <button
          onClick={() => setActiveTab('fusions')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'fusions'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaProjectDiagram className="w-4 h-4" /> Diagrama de Fusões Unifilar ({closures.length} CEOs)
        </button>

        <button
          onClick={() => setActiveTab('map')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'map'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaMapMarkedAlt className="w-4 h-4" /> Mapa GIS & Viabilidade
        </button>

        <button
          onClick={() => setActiveTab('cables')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'cables'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaPalette className="w-4 h-4" /> Cabos & Padrões de Cores ({cables.length})
        </button>
      </div>

      {/* TAB 1: CTOs & Portas */}
      {activeTab === 'ctos' && (
        <div className="space-y-6">
          {selectedCtoId ? (
            <FtthCtoDetail ctoId={selectedCtoId} onClose={() => setSelectedCtoId(null)} />
          ) : (
            <>
              <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
                <div className="relative">
                  <FaSearch className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="text"
                    placeholder="Buscar por nome da CTO, localização ou descrição..."
                    value={searchTerm}
                    onChange={(e) => setSearchTerm(e.target.value)}
                    className="w-full pl-9 pr-4 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
                {filteredCtos.length === 0 ? (
                  <div className="col-span-full p-8 text-center text-slate-500 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
                    Nenhuma CTO encontrada. Cadastre uma nova caixa de atendimento.
                  </div>
                ) : (
                  filteredCtos.map((cto) => (
                    <div
                      key={cto.id}
                      onClick={() => setSelectedCtoId(cto.id)}
                      className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm hover:shadow-md hover:border-indigo-500/50 transition cursor-pointer space-y-3"
                    >
                      <div className="flex items-center justify-between">
                        <div className="flex items-center gap-2">
                          <FaBox className="w-4 h-4 text-emerald-600" />
                          <h3 className="font-bold text-slate-900 dark:text-white text-sm">{cto.name}</h3>
                        </div>
                        <span className="px-2 py-0.5 text-[10px] font-semibold rounded-full bg-emerald-100 text-emerald-800 dark:bg-emerald-950/50 dark:text-emerald-300">
                          {cto.status}
                        </span>
                      </div>

                      <div className="space-y-1 text-xs text-slate-500 dark:text-slate-400">
                        <div className="flex justify-between">
                          <span>Splitter: {cto.splitterType}</span>
                          <span className="font-bold text-emerald-600 dark:text-emerald-400">
                            {cto.freePortsCount} portas livres
                          </span>
                        </div>
                        <div className="w-full bg-slate-100 dark:bg-slate-700 h-2 rounded-full overflow-hidden">
                          <div
                            className="bg-indigo-600 h-full rounded-full transition-all duration-300"
                            style={{ width: `${cto.occupancyPercentage}%` }}
                          />
                        </div>
                        <div className="flex justify-between text-[11px] pt-1">
                          <span>Ocupação: {cto.occupancyPercentage}%</span>
                          <span>Total: {cto.totalPorts} portas</span>
                        </div>
                      </div>
                    </div>
                  ))
                )}
              </div>
            </>
          )}
        </div>
      )}

      {/* TAB 2: Diagrama de Fusões Unifilar */}
      {activeTab === 'fusions' && (
        <div className="space-y-4">
          <div className="flex items-center gap-3 bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
            <span className="text-xs font-semibold text-slate-700 dark:text-slate-300">Selecione a Caixa de Emenda (CEO):</span>
            <select
              value={selectedClosureId || ''}
              onChange={(e) => setSelectedClosureId(e.target.value)}
              className="px-3 py-1.5 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
            >
              {closures.map((c) => (
                <option key={c.id} value={c.id}>
                  {c.name} ({c.closureType} • {c.capacityFusions} Fusões)
                </option>
              ))}
            </select>
          </div>

          {selectedClosureId ? (
            <FtthFusionDiagram closureId={selectedClosureId} />
          ) : (
            <div className="p-8 text-center text-slate-500 bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700">
              Nenhuma caixa de emenda cadastrada para visualização do diagrama unifilar.
            </div>
          )}
        </div>
      )}

      {/* TAB 3: Mapa GIS & Viabilidade */}
      {activeTab === 'map' && <FtthMap />}

      {/* TAB 4: Cabos & Padrões de Cores */}
      {activeTab === 'cables' && (
        <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 dark:bg-slate-900/50 border-b border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 text-xs font-semibold uppercase">
                  <th className="p-4">Identificador do Cabo</th>
                  <th className="p-4">Tipo</th>
                  <th className="p-4">Capacidade (FO)</th>
                  <th className="p-4">Padrão de Cores</th>
                  <th className="p-4">Comprimento</th>
                  <th className="p-4">Atenuação / km</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {cables.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-slate-500">
                      Nenhum cabo óptico cadastrado.
                    </td>
                  </tr>
                ) : (
                  cables.map((cable) => (
                    <tr key={cable.id} className="hover:bg-slate-50 dark:hover:bg-slate-700/50 transition">
                      <td className="p-4 font-semibold text-slate-900 dark:text-white">
                        {cable.name}
                      </td>
                      <td className="p-4 text-xs font-medium">
                        <span className="px-2 py-0.5 rounded-full bg-slate-100 text-slate-800 dark:bg-slate-700 dark:text-slate-300">
                          {cable.cableType}
                        </span>
                      </td>
                      <td className="p-4 text-xs font-mono text-slate-700 dark:text-slate-300">
                        {cable.fiberCount} FO ({cable.tubeCount} Tubos)
                      </td>
                      <td className="p-4 text-xs font-semibold">
                        {cable.colorStandard === 'ABNT_NBR_14106' ? (
                          <span className="text-emerald-600 dark:text-emerald-400">ABNT NBR 14106 (Nacional)</span>
                        ) : (
                          <span className="text-blue-600 dark:text-blue-400">TIA/EIA-598 (Internacional)</span>
                        )}
                      </td>
                      <td className="p-4 text-xs text-slate-500 font-mono">
                        {cable.lengthMeters} metros
                      </td>
                      <td className="p-4 text-xs text-slate-500 font-mono">
                        {cable.attenuationDbPerKm} dB/km
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* Modal Nova CTO */}
      {isCtoModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-700 pb-3">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FaBox className="w-5 h-5 text-emerald-600" /> Cadastrar Caixa de Atendimento (CTO)
              </h3>
              <button onClick={() => setIsCtoModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateCto} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Nome da CTO *</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: CTO-CENTRO-08"
                  value={ctoForm.name}
                  onChange={(e) => setCtoForm({ ...ctoForm, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-emerald-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Latitude *</label>
                  <input
                    type="text"
                    required
                    value={ctoForm.latitude}
                    onChange={(e) => setCtoForm({ ...ctoForm, latitude: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Longitude *</label>
                  <input
                    type="text"
                    required
                    value={ctoForm.longitude}
                    onChange={(e) => setCtoForm({ ...ctoForm, longitude: e.target.value })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Total de Portas *</label>
                <select
                  value={ctoForm.totalPorts}
                  onChange={(e) => setCtoForm({ ...ctoForm, totalPorts: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                >
                  <option value={8}>8 Portas (Splitter 1:8)</option>
                  <option value={16}>16 Portas (Splitter 1:16)</option>
                </select>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Descrição / Local</label>
                <textarea
                  rows={2}
                  placeholder="Ex: Poste P-102 em frente à padaria"
                  value={ctoForm.description}
                  onChange={(e) => setCtoForm({ ...ctoForm, description: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                />
              </div>

              <div className="pt-3 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsCtoModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold hover:bg-slate-50 dark:hover:bg-slate-700 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-lg font-semibold transition shadow-sm"
                >
                  Salvar CTO
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Nova CEO */}
      {isClosureModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-700 pb-3">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FaProjectDiagram className="w-5 h-5 text-indigo-600" /> Cadastrar Caixa de Emenda (CEO)
              </h3>
              <button onClick={() => setIsClosureModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateClosure} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Nome da CEO *</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: CEO-AV-BRASIL-01"
                  value={closureForm.name}
                  onChange={(e) => setClosureForm({ ...closureForm, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Tipo de Caixa *</label>
                <select
                  value={closureForm.closureType}
                  onChange={(e) => setClosureForm({ ...closureForm, closureType: e.target.value as FtthClosureType })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                >
                  <option value="DOMO">Tipo Domo / Cilíndrica (Poste)</option>
                  <option value="RETANGULAR">Tipo Retangular / Horizontal</option>
                  <option value="SUBTERRANEA">Subterrânea / Galeria</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Capacidade (Fusões)</label>
                  <input
                    type="number"
                    min="12"
                    max="288"
                    value={closureForm.capacityFusions}
                    onChange={(e) => setClosureForm({ ...closureForm, capacityFusions: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                  />
                </div>
                <div>
                  <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Qtd. Bandejas</label>
                  <input
                    type="number"
                    min="1"
                    max="12"
                    value={closureForm.trayCount}
                    onChange={(e) => setClosureForm({ ...closureForm, trayCount: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                  />
                </div>
              </div>

              <div className="pt-3 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsClosureModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold hover:bg-slate-50 dark:hover:bg-slate-700 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-semibold transition shadow-sm"
                >
                  Salvar CEO
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* Modal Novo Cabo */}
      {isCableModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-700 pb-3">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FaPalette className="w-5 h-5 text-purple-600" /> Cadastrar Cabo Óptico
              </h3>
              <button onClick={() => setIsCableModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateCable} className="space-y-3 text-xs">
              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Nome do Cabo *</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: CAB-TRONCAL-01"
                  value={cableForm.name}
                  onChange={(e) => setCableForm({ ...cableForm, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white focus:ring-2 focus:ring-purple-500"
                />
              </div>

              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Padrão de Cores de Fibra *</label>
                <select
                  value={cableForm.colorStandard}
                  onChange={(e) => setCableForm({ ...cableForm, colorStandard: e.target.value as FiberColorStandard })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white font-semibold"
                >
                  <option value="ABNT_NBR_14106">ABNT NBR 14106 / Telebrás (Padrão Nacional)</option>
                  <option value="TIA_EIA_598">TIA/EIA-598 (Padrão Internacional / Importado)</option>
                </select>
              </div>

              <div className="grid grid-cols-2 gap-2">
                <div>
                  <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Tipo de Cabo</label>
                  <select
                    value={cableForm.cableType}
                    onChange={(e) => setCableForm({ ...cableForm, cableType: e.target.value as FtthCableType })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                  >
                    <option value="ALIMENTADOR">Alimentador / Troncal</option>
                    <option value="DISTRIBUICAO">Distribuição</option>
                    <option value="DROP">Drop de Atendimento</option>
                  </select>
                </div>

                <div>
                  <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Qtd. Fibras (FO)</label>
                  <select
                    value={cableForm.fiberCount}
                    onChange={(e) => setCableForm({ ...cableForm, fiberCount: Number(e.target.value) })}
                    className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                  >
                    <option value={6}>6 FO</option>
                    <option value={12}>12 FO</option>
                    <option value={24}>24 FO</option>
                    <option value={36}>36 FO</option>
                    <option value={72}>72 FO</option>
                    <option value={144}>144 FO</option>
                  </select>
                </div>
              </div>

              <div>
                <label className="block font-semibold text-slate-700 dark:text-slate-300 mb-1">Metragem (Metros)</label>
                <input
                  type="number"
                  min="1"
                  value={cableForm.lengthMeters}
                  onChange={(e) => setCableForm({ ...cableForm, lengthMeters: Number(e.target.value) })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg text-slate-900 dark:text-white"
                />
              </div>

              <div className="pt-3 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsCableModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-200 dark:border-slate-700 text-slate-700 dark:text-slate-300 rounded-lg font-semibold hover:bg-slate-50 dark:hover:bg-slate-700 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg font-semibold transition shadow-sm"
                >
                  Salvar Cabo
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default FtthManager;
