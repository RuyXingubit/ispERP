import React, { useState, useEffect, useCallback } from 'react';
import { CgnatMapping, CgnatScriptImportResponse } from '../../types/cgnat';
import { Nas, NasVendorType } from '../../types/radius';
import { cgnatService } from '../../services/cgnatService';
import { radiusService } from '../../services/radiusService';
import {
  FaLayerGroup,
  FaFileImport,
  FaPlus,
  FaTrash,
  FaSyncAlt,
  FaSearch,
  FaCheckCircle,
  FaExclamationTriangle,
  FaCode,
  FaServer,
} from 'react-icons/fa';

export const CgnatManager: React.FC = () => {
  const [mappings, setMappings] = useState<CgnatMapping[]>([]);
  const [nasList, setNasList] = useState<Nas[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Modais
  const [isImportModalOpen, setIsImportModalOpen] = useState(false);
  const [isManualModalOpen, setIsManualModalOpen] = useState(false);

  // Script Import Form
  const [importForm, setImportForm] = useState({
    nasId: '',
    vendorType: 'MIKROTIK' as NasVendorType,
    scriptContent: '',
    replaceExisting: false,
  });
  const [importLoading, setImportLoading] = useState(false);
  const [importResult, setImportResult] = useState<CgnatScriptImportResponse | null>(null);

  // Manual Form
  const [manualForm, setManualForm] = useState({
    nasId: '',
    vendorType: 'MIKROTIK' as NasVendorType,
    publicIp: '',
    portStart: 1024,
    portEnd: 2047,
    privateIpStart: '100.64.1.2',
    privateIpEnd: '100.64.1.2',
    protocol: 'BOTH' as 'TCP' | 'UDP' | 'BOTH',
    notes: '',
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
      const [mappingsData, nasData] = await Promise.all([
        cgnatService.getAllMappings(),
        radiusService.getAllNas(),
      ]);
      setMappings(mappingsData);
      setNasList(nasData);
    } catch (err: any) {
      showNotification('Erro ao carregar mapeamentos CGNAT: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Import Script
  const handleImportScript = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setImportLoading(true);
      const res = await cgnatService.importScript({
        nasId: importForm.nasId || undefined,
        vendorType: importForm.vendorType,
        scriptContent: importForm.scriptContent,
        replaceExisting: importForm.replaceExisting,
      });
      setImportResult(res);
      showNotification(`Importação concluída! ${res.totalSaved} regras cadastradas.`);
      loadData();
    } catch (err: any) {
      showNotification('Erro ao importar script: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setImportLoading(false);
    }
  };

  // Create Manual
  const handleCreateManual = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await cgnatService.createMapping({
        nasId: manualForm.nasId || undefined,
        vendorType: manualForm.vendorType,
        publicIp: manualForm.publicIp,
        portStart: Number(manualForm.portStart),
        portEnd: Number(manualForm.portEnd),
        privateIpStart: manualForm.privateIpStart,
        privateIpEnd: manualForm.privateIpEnd,
        protocol: manualForm.protocol,
        notes: manualForm.notes || undefined,
      });
      showNotification('Mapeamento CGNAT criado com sucesso!');
      setIsManualModalOpen(false);
      loadData();
    } catch (err: any) {
      showNotification('Erro ao cadastrar CGNAT: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Delete Mapping
  const handleDelete = async (id: string) => {
    if (!window.confirm('Excluir este bloco de mapeamento CGNAT?')) return;
    try {
      await cgnatService.deleteMapping(id);
      showNotification('Mapeamento excluído.');
      loadData();
    } catch (err: any) {
      showNotification('Erro ao excluir: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  const filteredMappings = mappings.filter((m) => {
    const term = searchTerm.toLowerCase();
    return (
      m.publicIp.toLowerCase().includes(term) ||
      m.privateIpStart.toLowerCase().includes(term) ||
      (m.nasName && m.nasName.toLowerCase().includes(term)) ||
      (m.notes && m.notes.toLowerCase().includes(term))
    );
  });

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <FaLayerGroup className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">CGNAT Forense Multi-Vendor</h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Mapeamento de portas e IPs públicos/privados para conformidade com o Marco Civil da Internet (Lei 12.965/2014)
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
            onClick={() => {
              setImportResult(null);
              setIsImportModalOpen(true);
            }}
            className="flex items-center gap-2 px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white rounded-lg font-medium shadow-sm transition"
          >
            <FaFileImport className="w-4 h-4" /> Importar Script / CSV
          </button>
          <button
            onClick={() => setIsManualModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium shadow-sm transition"
          >
            <FaPlus className="w-4 h-4" /> Nova Regra
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

      {/* Search Bar */}
      <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
        <div className="relative">
          <FaSearch className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
          <input
            type="text"
            placeholder="Buscar por IP público, IP privado (100.64...), BNG ou anotações..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* Mappings Table */}
      <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm overflow-hidden">
        {loading ? (
          <div className="p-8 text-center text-slate-500">Carregando regras CGNAT...</div>
        ) : filteredMappings.length === 0 ? (
          <div className="p-8 text-center text-slate-500">
            Nenhuma regra de CGNAT cadastrada. Clique em "Importar Script / CSV" para carregar as regras do seu BNG.
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
              <thead className="bg-slate-50 dark:bg-slate-900/50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                <tr>
                  <th className="px-6 py-4">IP Público (WAN)</th>
                  <th className="px-6 py-4">Faixa de Portas</th>
                  <th className="px-6 py-4">IP Privado (CGNAT)</th>
                  <th className="px-6 py-4">BNG / Fabricante</th>
                  <th className="px-6 py-4">Protocolo</th>
                  <th className="px-6 py-4 text-right">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {filteredMappings.map((m) => (
                  <tr key={m.id} className="hover:bg-slate-50 dark:hover:bg-slate-750 transition">
                    <td className="px-6 py-4">
                      <span className="font-mono font-bold text-slate-900 dark:text-white">
                        {m.publicIp}
                      </span>
                      {m.notes && <p className="text-xs text-slate-400 mt-0.5">{m.notes}</p>}
                    </td>

                    <td className="px-6 py-4">
                      <span className="px-2.5 py-1 text-xs font-mono font-bold bg-indigo-50 dark:bg-indigo-900/40 text-indigo-700 dark:text-indigo-300 rounded">
                        {m.portStart} - {m.portEnd}
                      </span>
                      <span className="text-xs text-slate-400 ml-2">({m.portEnd - m.portStart + 1} portas)</span>
                    </td>

                    <td className="px-6 py-4 font-mono font-semibold text-emerald-600 dark:text-emerald-400">
                      {m.privateIpStart}
                      {m.privateIpEnd !== m.privateIpStart && ` a ${m.privateIpEnd}`}
                    </td>

                    <td className="px-6 py-4">
                      <span className="text-xs font-bold px-2 py-0.5 rounded bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300">
                        {m.vendorType}
                      </span>
                      {m.nasName && <p className="text-xs text-indigo-500 mt-0.5">{m.nasName}</p>}
                    </td>

                    <td className="px-6 py-4 text-xs font-medium">
                      {m.protocol}
                    </td>

                    <td className="px-6 py-4 text-right">
                      <button
                        onClick={() => handleDelete(m.id)}
                        className="p-1.5 text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-900/30 rounded transition"
                        title="Excluir"
                      >
                        <FaTrash className="w-4 h-4" />
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* MODAL: Importar Script / CSV */}
      {isImportModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-2xl w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <FaCode className="w-5 h-5 text-indigo-600" /> Importador de Scripts CGNAT & Planilhas
            </h3>
            <p className="text-xs text-slate-500">
              Cole o script do firewall (MikroTik RouterOS, Huawei VRP, A10 Networks, Cisco) ou linhas em formato CSV/Planilha
            </p>

            <form onSubmit={handleImportScript} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Fabricante / Formato *
                  </label>
                  <select
                    value={importForm.vendorType}
                    onChange={(e: any) => setImportForm({ ...importForm, vendorType: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="MIKROTIK">MikroTik RouterOS (/ip firewall nat)</option>
                    <option value="HUAWEI">Huawei VRP (address-group / nat-policy)</option>
                    <option value="A10">A10 Networks (cgnv6 nat pool)</option>
                    <option value="CISCO">Cisco IOS-XE / ASR (port-block)</option>
                    <option value="GENERIC">CSV / Planilha (public,port_start,port_end,priv)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Vincular ao BNG (Opcional)
                  </label>
                  <select
                    value={importForm.nasId}
                    onChange={(e) => setImportForm({ ...importForm, nasId: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="">Nenhum / Global</option>
                    {nasList.map((n) => (
                      <option key={n.id} value={n.id}>
                        {n.shortname || n.nasname} ({n.vendorType})
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Conteúdo do Script ou CSV *
                </label>
                <textarea
                  rows={8}
                  required
                  placeholder={`Exemplo MikroTik:
/ip firewall nat add chain=srcnat action=src-nat src-address=100.64.1.2 to-addresses=200.150.10.2 to-ports=1024-2047 protocol=tcp
/ip firewall nat add chain=srcnat action=src-nat src-address=100.64.1.3 to-addresses=200.150.10.2 to-ports=2048-3071 protocol=tcp`}
                  value={importForm.scriptContent}
                  onChange={(e) => setImportForm({ ...importForm, scriptContent: e.target.value })}
                  className="w-full px-3 py-2 text-xs bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="flex items-center gap-2">
                <input
                  type="checkbox"
                  id="replaceExisting"
                  checked={importForm.replaceExisting}
                  onChange={(e) => setImportForm({ ...importForm, replaceExisting: e.target.checked })}
                  className="rounded text-indigo-600 focus:ring-indigo-500"
                />
                <label htmlFor="replaceExisting" className="text-xs text-slate-600 dark:text-slate-300">
                  Substituir regras anteriores associadas a este BNG
                </label>
              </div>

              {importResult && (
                <div className="p-3 bg-emerald-50 dark:bg-emerald-950/40 border border-emerald-200 dark:border-emerald-800 rounded-lg text-xs space-y-1">
                  <p className="font-bold text-emerald-800 dark:text-emerald-300">
                    Importação realizada: {importResult.totalSaved} regras adicionadas com sucesso.
                  </p>
                  {importResult.warnings.length > 0 && (
                    <div className="text-amber-700 dark:text-amber-400">
                      {importResult.warnings.map((w, i) => (
                        <div key={i}>{w}</div>
                      ))}
                    </div>
                  )}
                </div>
              )}

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
                <button
                  type="button"
                  onClick={() => setIsImportModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Fechar
                </button>
                <button
                  type="submit"
                  disabled={importLoading}
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
                >
                  {importLoading ? 'Processando...' : 'Processar & Importar'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Nova Regra Manual */}
      {isManualModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">Cadastrar Bloco CGNAT</h3>
            <form onSubmit={handleCreateManual} className="space-y-4">
              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    IP Público (WAN) *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: 200.150.10.2"
                    value={manualForm.publicIp}
                    onChange={(e) => setManualForm({ ...manualForm, publicIp: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Protocolo
                  </label>
                  <select
                    value={manualForm.protocol}
                    onChange={(e: any) => setManualForm({ ...manualForm, protocol: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="BOTH">TCP e UDP (BOTH)</option>
                    <option value="TCP">Apenas TCP</option>
                    <option value="UDP">Apenas UDP</option>
                  </select>
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Porta Inicial *
                  </label>
                  <input
                    type="number"
                    required
                    value={manualForm.portStart}
                    onChange={(e) => setManualForm({ ...manualForm, portStart: Number(e.target.value) })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono font-bold"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Porta Final *
                  </label>
                  <input
                    type="number"
                    required
                    value={manualForm.portEnd}
                    onChange={(e) => setManualForm({ ...manualForm, portEnd: Number(e.target.value) })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono font-bold"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    IP Privado Inicial *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="100.64.1.2"
                    value={manualForm.privateIpStart}
                    onChange={(e) => setManualForm({ ...manualForm, privateIpStart: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    IP Privado Final *
                  </label>
                  <input
                    type="text"
                    required
                    placeholder="100.64.1.2"
                    value={manualForm.privateIpEnd}
                    onChange={(e) => setManualForm({ ...manualForm, privateIpEnd: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono"
                  />
                </div>
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
                <button
                  type="button"
                  onClick={() => setIsManualModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
                >
                  Salvar Regra
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default CgnatManager;
