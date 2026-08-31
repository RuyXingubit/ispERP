import React, { useState, useEffect } from 'react';
import {
  FtthClosureDiagramResponse,
  FtthCable,
  FtthSplitter,
  FtthFusion,
  FiberColorInfo,
  FtthSplitterType,
} from '../../types/ftth';
import { ftthService } from '../../services/ftthService';
import {
  FaProjectDiagram,
  FaPlus,
  FaTrash,
  FaMicrochip,
  FaCheckCircle,
  FaExclamationTriangle,
  FaTimes,
} from 'react-icons/fa';

interface FtthFusionDiagramProps {
  closureId: string;
  onClose?: () => void;
}

export const FtthFusionDiagram: React.FC<FtthFusionDiagramProps> = ({ closureId, onClose }) => {
  const [diagram, setDiagram] = useState<FtthClosureDiagramResponse | null>(null);
  const [loading, setLoading] = useState(true);
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  // Seleção para nova fusão
  const [selectedSourceFiber, setSelectedSourceFiber] = useState<{
    cable: FtthCable;
    fiber: FiberColorInfo;
  } | null>(null);

  // Modal Splitter
  const [isSplitterModalOpen, setIsSplitterModalOpen] = useState(false);
  const [splitterForm, setSplitterForm] = useState({
    name: 'SPL-1:8-01',
    splitterType: 'BALANCED_1_8' as FtthSplitterType,
  });

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadDiagram = async () => {
    try {
      setLoading(true);
      const data = await ftthService.getClosureDiagram(closureId);
      setDiagram(data);
    } catch (err: any) {
      showNotification('Erro ao carregar diagrama de fusão: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadDiagram();
  }, [closureId]);

  // Criação de Fusão
  const handleSelectTargetFiber = async (targetCable: FtthCable, targetFiber: FiberColorInfo) => {
    if (!selectedSourceFiber) return;
    if (selectedSourceFiber.cable.id === targetCable.id && selectedSourceFiber.fiber.fiberNumber === targetFiber.fiberNumber) {
      showNotification('Não é possível fundir uma fibra com ela mesma!', 'error');
      return;
    }

    try {
      await ftthService.createFusion(closureId, {
        closureId,
        sourceCableId: selectedSourceFiber.cable.id,
        sourceFiberNumber: selectedSourceFiber.fiber.fiberNumber,
        targetCableId: targetCable.id,
        targetFiberNumber: targetFiber.fiberNumber,
        lossDb: 0.05,
      });
      showNotification(
        `Fusão criada: ${selectedSourceFiber.cable.name} F${selectedSourceFiber.fiber.fiberNumber} [${selectedSourceFiber.fiber.fiberColorName}] ➔ ${targetCable.name} F${targetFiber.fiberNumber} [${targetFiber.fiberColorName}]`
      );
      setSelectedSourceFiber(null);
      loadDiagram();
    } catch (err: any) {
      showNotification('Erro ao criar fusão: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Fusão em Splitter
  const handleSelectTargetSplitter = async (splitter: FtthSplitter) => {
    if (!selectedSourceFiber) return;
    try {
      await ftthService.createFusion(closureId, {
        closureId,
        sourceCableId: selectedSourceFiber.cable.id,
        sourceFiberNumber: selectedSourceFiber.fiber.fiberNumber,
        targetSplitterId: splitter.id,
        lossDb: 0.05,
      });
      showNotification(`Fibra fundida na entrada do splitter ${splitter.name}!`);
      setSelectedSourceFiber(null);
      loadDiagram();
    } catch (err: any) {
      showNotification('Erro ao fundir no splitter: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Exclusão de Fusão
  const handleDeleteFusion = async (fusionId: string) => {
    if (!window.confirm('Deseja desfazer esta fusão óptica?')) return;
    try {
      await ftthService.deleteFusion(fusionId);
      showNotification('Fusão desfeita com sucesso!');
      loadDiagram();
    } catch (err: any) {
      showNotification('Erro ao desfazer fusão: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Criar Splitter
  const handleCreateSplitter = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ftthService.createSplitter(closureId, {
        name: splitterForm.name,
        splitterType: splitterForm.splitterType,
      });
      showNotification(`Splitter ${splitterForm.name} adicionado à bandeja!`);
      setIsSplitterModalOpen(false);
      loadDiagram();
    } catch (err: any) {
      showNotification('Erro ao adicionar splitter: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  if (loading) {
    return (
      <div className="p-8 text-center text-slate-500 flex flex-col items-center justify-center gap-3">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
        <span>Carregando diagrama unifilar e bandejas de emenda...</span>
      </div>
    );
  }

  if (!diagram) {
    return <div className="p-8 text-center text-slate-500">Caixa de emenda não encontrada.</div>;
  }

  // Identifica quais fibras já estão fundidas
  const fusedSourceKeys = new Set(diagram.fusions.map((f) => `${f.sourceCableId}-${f.sourceFiberNumber}`));
  const fusedTargetKeys = new Set(
    diagram.fusions
      .filter((f) => f.targetCableId && f.targetFiberNumber)
      .map((f) => `${f.targetCableId}-${f.targetFiberNumber}`)
  );

  return (
    <div className="bg-slate-900 text-slate-100 rounded-2xl border border-slate-700 shadow-2xl p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-4">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 bg-indigo-950/80 border border-indigo-500/40 text-indigo-400 rounded-xl shadow-inner">
              <FaProjectDiagram className="w-6 h-6" />
            </div>
            <div>
              <h2 className="text-xl font-bold text-white flex items-center gap-2">
                {diagram.closure.name}
                <span className="px-2.5 py-0.5 text-xs font-semibold rounded-full bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
                  {diagram.closure.closureType}
                </span>
              </h2>
              <p className="text-xs text-slate-400 mt-0.5">
                Diagrama Unifilar de Fusões • Capacidade: {diagram.closure.capacityFusions} fusões • Bandejas: {diagram.closure.trayCount}
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => setIsSplitterModalOpen(true)}
            className="flex items-center gap-2 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-md transition"
          >
            <FaPlus className="w-3.5 h-3.5" /> Adicionar Splitter
          </button>
          {onClose && (
            <button
              onClick={onClose}
              className="p-2 text-slate-400 hover:text-white hover:bg-slate-800 rounded-lg transition"
              title="Fechar"
            >
              <FaTimes className="w-5 h-5" />
            </button>
          )}
        </div>
      </div>

      {/* Notifications */}
      {notification && (
        <div
          className={`p-3 rounded-lg flex items-center justify-between text-xs font-medium shadow-md ${
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

      {/* Dica de Fusão Ativa */}
      {selectedSourceFiber && (
        <div className="p-3 bg-indigo-950/70 border border-indigo-500/60 rounded-xl flex items-center justify-between text-xs text-indigo-200 animate-pulse">
          <div className="flex items-center gap-2">
            <span className="w-3 h-3 rounded-full" style={{ backgroundColor: selectedSourceFiber.fiber.fiberColorHex }} />
            <span>
              Fibra Selecionada:{' '}
              <strong>
                {selectedSourceFiber.cable.name} - Fibra {selectedSourceFiber.fiber.fiberNumber} [{selectedSourceFiber.fiber.fiberColorName}]
              </strong>
              . Clique na fibra de destino ou em um splitter para fundir.
            </span>
          </div>
          <button
            onClick={() => setSelectedSourceFiber(null)}
            className="text-xs bg-slate-800 hover:bg-slate-700 px-2 py-1 rounded text-slate-300"
          >
            Cancelar
          </button>
        </div>
      )}

      {/* BANDEJA DE EMENDA VISUAL */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* COLUNA 1: Cabos de Entrada (Alimentadores / Troncal) */}
        <div className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">Cabos de Entrada / Origem</span>
            <span className="text-[11px] text-slate-500">{diagram.cables.length} cabo(s)</span>
          </div>

          <div className="space-y-4 max-h-[550px] overflow-y-auto pr-1">
            {diagram.cables.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-4">Nenhum cabo óptico cadastrado.</p>
            ) : (
              diagram.cables.map((cable) => (
                <div key={cable.id} className="bg-slate-900 border border-slate-800 rounded-lg p-3 space-y-2">
                  <div className="flex items-center justify-between text-xs font-semibold text-slate-200">
                    <span>{cable.name}</span>
                    <span className="text-[10px] font-mono px-2 py-0.5 rounded bg-slate-800 text-indigo-300">
                      {cable.colorStandard === 'ABNT_NBR_14106' ? 'ABNT (Nacional)' : 'TIA-598 (Internacional)'} • {cable.fiberCount}FO
                    </span>
                  </div>

                  <div className="grid grid-cols-2 gap-1.5 pt-1">
                    {cable.fibers?.map((fiber) => {
                      const isSourceFused = fusedSourceKeys.has(`${cable.id}-${fiber.fiberNumber}`);
                      const isTargetFused = fusedTargetKeys.has(`${cable.id}-${fiber.fiberNumber}`);
                      const isSelected = selectedSourceFiber?.cable.id === cable.id && selectedSourceFiber?.fiber.fiberNumber === fiber.fiberNumber;

                      return (
                        <button
                          key={fiber.fiberNumber}
                          onClick={() => {
                            if (selectedSourceFiber) {
                              handleSelectTargetFiber(cable, fiber);
                            } else {
                              setSelectedSourceFiber({ cable, fiber });
                            }
                          }}
                          className={`flex items-center gap-2 p-1.5 rounded text-xs transition border ${
                            isSelected
                              ? 'bg-indigo-600 text-white border-indigo-400 ring-2 ring-indigo-400'
                              : isSourceFused || isTargetFused
                              ? 'bg-slate-950/60 border-emerald-800/60 text-slate-300'
                              : 'bg-slate-800/80 border-slate-700 hover:border-indigo-500 text-slate-300'
                          }`}
                        >
                          <span
                            className="w-3.5 h-3.5 rounded-full shrink-0 shadow-sm border border-black/40"
                            style={{ backgroundColor: fiber.fiberColorHex }}
                            title={`Fibra ${fiber.fiberNumber} - ${fiber.fiberColorName} (Tubo ${fiber.tubeNumber})`}
                          />
                          <span className="font-mono text-[11px] truncate">
                            F{fiber.fiberNumber} {fiber.fiberColorName}
                          </span>
                          {(isSourceFused || isTargetFused) && (
                            <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 ml-auto shrink-0" title="Fundida" />
                          )}
                        </button>
                      );
                    })}
                  </div>
                </div>
              ))
            )}
          </div>
        </div>

        {/* COLUNA 2: Centro - Splitters e Fusões Ativas */}
        <div className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <span className="text-xs font-bold text-indigo-400 uppercase tracking-wider">
              Fusões Ativas ({diagram.fusions.length}) & Splitters ({diagram.splitters.length})
            </span>
          </div>

          {/* Splitters */}
          <div className="space-y-2">
            <span className="text-[11px] font-semibold text-slate-400 block">Divisores Ópticos (Splitters)</span>
            {diagram.splitters.length === 0 ? (
              <p className="text-xs text-slate-600 italic">Nenhum splitter instalado nesta caixa.</p>
            ) : (
              diagram.splitters.map((sp) => (
                <div
                  key={sp.id}
                  className="bg-indigo-950/40 border border-indigo-800/60 rounded-lg p-3 flex items-center justify-between gap-3 text-xs"
                >
                  <div className="flex items-center gap-2">
                    <FaMicrochip className="w-4 h-4 text-indigo-400" />
                    <div>
                      <span className="font-bold text-white block">{sp.name}</span>
                      <span className="text-[10px] text-indigo-300">
                        {sp.splitterType} ({sp.outputPorts} Saídas • Atenuação ~{sp.attenuationDb} dB)
                      </span>
                    </div>
                  </div>

                  {selectedSourceFiber && (
                    <button
                      onClick={() => handleSelectTargetSplitter(sp)}
                      className="px-2 py-1 bg-indigo-600 hover:bg-indigo-500 text-white rounded text-[11px] font-semibold transition"
                    >
                      Fundir na Entrada
                    </button>
                  )}
                </div>
              ))
            )}
          </div>

          {/* Lista de Fusões Ativas */}
          <div className="space-y-2 pt-2 border-t border-slate-800">
            <span className="text-[11px] font-semibold text-slate-400 block">Conexões Fibra-a-Fibra</span>
            <div className="space-y-2 max-h-[350px] overflow-y-auto pr-1">
              {diagram.fusions.length === 0 ? (
                <p className="text-xs text-slate-600 italic py-2">Nenhuma fusão realizada.</p>
              ) : (
                diagram.fusions.map((fusion) => (
                  <div
                    key={fusion.id}
                    className="bg-slate-900 border border-slate-800 rounded-lg p-2.5 flex items-center justify-between gap-2 text-xs"
                  >
                    <div className="flex items-center gap-2 flex-1 min-w-0">
                      {fusion.sourceFiberColor && (
                        <span
                          className="w-3 h-3 rounded-full shrink-0 border border-black/40"
                          style={{ backgroundColor: fusion.sourceFiberColor.fiberColorHex }}
                        />
                      )}
                      <span className="font-mono text-[11px] text-slate-300 truncate">
                        {fusion.sourceCableName} F{fusion.sourceFiberNumber}
                      </span>
                      <span className="text-indigo-400 font-bold">➔</span>
                      {fusion.targetFiberColor && (
                        <span
                          className="w-3 h-3 rounded-full shrink-0 border border-black/40"
                          style={{ backgroundColor: fusion.targetFiberColor.fiberColorHex }}
                        />
                      )}
                      <span className="font-mono text-[11px] text-slate-300 truncate">
                        {fusion.targetSplitterName
                          ? fusion.targetSplitterName
                          : `${fusion.targetCableName} F${fusion.targetFiberNumber}`}
                      </span>
                    </div>

                    <div className="flex items-center gap-2 shrink-0">
                      <span className="text-[10px] font-mono text-emerald-400 font-semibold">{fusion.lossDb} dB</span>
                      <button
                        onClick={() => handleDeleteFusion(fusion.id)}
                        className="p-1 text-rose-400 hover:text-rose-300 hover:bg-rose-950/50 rounded transition"
                        title="Desfazer fusão"
                      >
                        <FaTrash className="w-3.5 h-3.5" />
                      </button>
                    </div>
                  </div>
                ))
              )}
            </div>
          </div>
        </div>

        {/* COLUNA 3: CTOs Conectadas e Saídas */}
        <div className="bg-slate-950/80 border border-slate-800 rounded-xl p-4 space-y-4">
          <div className="flex items-center justify-between border-b border-slate-800 pb-2">
            <span className="text-xs font-bold text-slate-300 uppercase tracking-wider">Caixas de Atendimento (CTOs)</span>
            <span className="text-[11px] text-slate-500">{diagram.connectedCtos.length} CTO(s)</span>
          </div>

          <div className="space-y-3 max-h-[550px] overflow-y-auto pr-1">
            {diagram.connectedCtos.length === 0 ? (
              <p className="text-xs text-slate-500 text-center py-4">Nenhuma CTO vinculada a esta caixa de emenda.</p>
            ) : (
              diagram.connectedCtos.map((cto) => (
                <div key={cto.id} className="bg-slate-900 border border-slate-800 rounded-lg p-3 space-y-2">
                  <div className="flex items-center justify-between text-xs font-bold text-white">
                    <span>{cto.name}</span>
                    <span className="text-[10px] font-mono text-emerald-400">
                      {cto.freePortsCount} / {cto.totalPorts} Portas Livres
                    </span>
                  </div>

                  <div className="w-full bg-slate-800 h-2 rounded-full overflow-hidden">
                    <div
                      className="bg-indigo-500 h-full rounded-full transition-all duration-300"
                      style={{ width: `${cto.occupancyPercentage}%` }}
                    />
                  </div>

                  <div className="text-[11px] text-slate-400 flex items-center justify-between">
                    <span>Splitter: {cto.splitterType}</span>
                    <span>{cto.occupancyPercentage}% ocupada</span>
                  </div>
                </div>
              ))
            )}
          </div>
        </div>
      </div>

      {/* Modal Splitter */}
      {isSplitterModalOpen && (
        <div className="fixed inset-0 bg-black/60 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-slate-900 border border-slate-700 rounded-xl max-w-md w-full p-6 space-y-4 shadow-2xl">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-white flex items-center gap-2">
                <FaMicrochip className="w-4 h-4 text-indigo-400" /> Adicionar Divisor Óptico (Splitter)
              </h3>
              <button onClick={() => setIsSplitterModalOpen(false)} className="text-slate-400 hover:text-white">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateSplitter} className="space-y-4 text-sm">
              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Identificador do Splitter *</label>
                <input
                  type="text"
                  required
                  value={splitterForm.name}
                  onChange={(e) => setSplitterForm({ ...splitterForm, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white text-xs focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-300 mb-1">Tipo de Splitter *</label>
                <select
                  value={splitterForm.splitterType}
                  onChange={(e) => setSplitterForm({ ...splitterForm, splitterType: e.target.value as FtthSplitterType })}
                  className="w-full px-3 py-2 bg-slate-800 border border-slate-700 rounded-lg text-white text-xs focus:ring-2 focus:ring-indigo-500"
                >
                  <optgroup label="Balanceados PLC">
                    <option value="BALANCED_1_2">Balanceado 1:2 (~3.5 dB)</option>
                    <option value="BALANCED_1_4">Balanceado 1:4 (~7.2 dB)</option>
                    <option value="BALANCED_1_8">Balanceado 1:8 (~10.5 dB)</option>
                    <option value="BALANCED_1_16">Balanceado 1:16 (~13.8 dB)</option>
                    <option value="BALANCED_1_32">Balanceado 1:32 (~17.5 dB)</option>
                  </optgroup>
                  <optgroup label="Desbalanceados FBT (Barramento)">
                    <option value="UNBALANCED_95_05">Desbalanceado 95/05</option>
                    <option value="UNBALANCED_90_10">Desbalanceado 90/10</option>
                    <option value="UNBALANCED_85_15">Desbalanceado 85/15</option>
                    <option value="UNBALANCED_80_20">Desbalanceado 80/20</option>
                    <option value="UNBALANCED_70_30">Desbalanceado 70/30</option>
                    <option value="UNBALANCED_60_40">Desbalanceado 60/40</option>
                    <option value="UNBALANCED_50_50">Desbalanceado 50/50</option>
                  </optgroup>
                </select>
              </div>

              <div className="pt-3 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsSplitterModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-700 text-slate-300 rounded-lg text-xs font-semibold hover:bg-slate-800 transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold transition shadow-md"
                >
                  Salvar Splitter
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default FtthFusionDiagram;
