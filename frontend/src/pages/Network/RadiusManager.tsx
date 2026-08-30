import React, { useState, useEffect, useCallback } from 'react';
import { Nas, RadiusSession, NasVendorType } from '../../types/radius';
import {
  RadiusPolicyConfig,
  RadiusLifecycleSummary,
  RadiusLifecycleLog,
} from '../../types/radiusLifecycle';
import { radiusService } from '../../services/radiusService';
import { radiusLifecycleService } from '../../services/radiusLifecycleService';
import {
  FaServer,
  FaSignal,
  FaPlus,
  FaTrash,
  FaSyncAlt,
  FaPowerOff,
  FaCheckCircle,
  FaExclamationTriangle,
  FaSearch,
  FaKey,
  FaMicrochip,
  FaUserAlt,
  FaShieldAlt,
  FaSlidersH,
  FaHistory,
  FaBan,
  FaUnlockAlt,
  FaPlay,
  FaClock,
} from 'react-icons/fa';

export const RadiusManager: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'sessions' | 'nas' | 'lifecycle'>('sessions');
  const [sessions, setSessions] = useState<RadiusSession[]>([]);
  const [nasList, setNasList] = useState<Nas[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

  // Lifecycle & Policy State
  const [lifecycleSummary, setLifecycleSummary] = useState<RadiusLifecycleSummary | null>(null);
  const [policyConfig, setPolicyConfig] = useState<RadiusPolicyConfig | null>(null);
  const [lifecycleLogs, setLifecycleLogs] = useState<RadiusLifecycleLog[]>([]);
  const [savingPolicy, setSavingPolicy] = useState(false);
  const [runningAutoBlock, setRunningAutoBlock] = useState(false);

  // Modal Novo NAS
  const [isNasModalOpen, setIsNasModalOpen] = useState(false);
  const [nasForm, setNasForm] = useState({
    nasname: '',
    shortname: '',
    secret: '',
    vendorType: 'MIKROTIK' as NasVendorType,
    description: '',
  });

  // Modal Desconexão PoD
  const [disconnectingUser, setDisconnectingUser] = useState<string | null>(null);

  // Notificações
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [sessionsData, nasData, summaryData, policyData, logsData] = await Promise.all([
        radiusService.getActiveSessions(),
        radiusService.getAllNas(),
        radiusLifecycleService.getSummary().catch(() => null),
        radiusLifecycleService.getPolicyConfig().catch(() => null),
        radiusLifecycleService.getLogs(0, 20).catch(() => ({ content: [], totalElements: 0 })),
      ]);
      setSessions(sessionsData);
      setNasList(nasData);
      if (summaryData) setLifecycleSummary(summaryData);
      if (policyData) setPolicyConfig(policyData);
      if (logsData) setLifecycleLogs(logsData.content);
    } catch (err: any) {
      showNotification('Erro ao carregar dados do RADIUS: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Create NAS
  const handleCreateNas = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await radiusService.createNas({
        nasname: nasForm.nasname,
        shortname: nasForm.shortname || undefined,
        secret: nasForm.secret,
        vendorType: nasForm.vendorType,
        description: nasForm.description || undefined,
      });
      showNotification('NAS / BNG cadastrado com sucesso!');
      setIsNasModalOpen(false);
      setNasForm({ nasname: '', shortname: '', secret: '', vendorType: 'MIKROTIK', description: '' });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao cadastrar NAS: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Delete NAS
  const handleDeleteNas = async (id: string, name: string) => {
    if (!window.confirm(`Tem certeza que deseja excluir o NAS ${name}?`)) return;
    try {
      await radiusService.deleteNas(id);
      showNotification(`NAS ${name} excluído.`);
      loadData();
    } catch (err: any) {
      showNotification('Erro ao excluir NAS: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Disconnect PoD
  const handleDisconnect = async (session: RadiusSession) => {
    if (!window.confirm(`Deseja enviar comando PoD para derrubar a sessão de ${session.username}?`)) return;
    try {
      setDisconnectingUser(session.username);
      const res = await radiusService.disconnectUser({
        username: session.username,
        nasIpAddress: session.nasIpAddress,
        acctSessionId: session.acctSessionId,
      });
      if (res.success) {
        showNotification(res.message);
        loadData();
      } else {
        showNotification(res.message, 'error');
      }
    } catch (err: any) {
      showNotification('Erro ao desconectar: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setDisconnectingUser(null);
    }
  };

  // Salvar Políticas de Auto-Corte
  const handleSavePolicy = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!policyConfig) return;
    try {
      setSavingPolicy(true);
      const updated = await radiusLifecycleService.updatePolicyConfig({
        autoBlockEnabled: policyConfig.autoBlockEnabled,
        toleranceDays: Number(policyConfig.toleranceDays),
        blockMode: policyConfig.blockMode,
        reducedDownloadKbps: Number(policyConfig.reducedDownloadKbps),
        reducedUploadKbps: Number(policyConfig.reducedUploadKbps),
        unblockOnPayment: policyConfig.unblockOnPayment,
        sendPodOnBlock: policyConfig.sendPodOnBlock,
        sendPodOnUnblock: policyConfig.sendPodOnUnblock,
      });
      setPolicyConfig(updated);
      showNotification('Políticas de auto-corte e inadimplência salvas com sucesso!');
    } catch (err: any) {
      showNotification('Erro ao salvar políticas: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setSavingPolicy(false);
    }
  };

  // Executar Varredura Manual de Auto-Corte
  const handleRunAutoBlock = async () => {
    if (!window.confirm('Deseja iniciar a varredura e bloqueio de clientes inadimplentes agora?')) return;
    try {
      setRunningAutoBlock(true);
      await radiusLifecycleService.runAutoBlockNow();
      showNotification('Varredura de auto-corte iniciada e executada com sucesso!');
      loadData();
    } catch (err: any) {
      showNotification('Erro ao executar auto-corte: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setRunningAutoBlock(false);
    }
  };

  const filteredSessions = sessions.filter((s) => {
    const term = searchTerm.toLowerCase();
    return (
      s.username.toLowerCase().includes(term) ||
      (s.framedIpAddress && s.framedIpAddress.toLowerCase().includes(term)) ||
      (s.customerName && s.customerName.toLowerCase().includes(term)) ||
      (s.callingStationId && s.callingStationId.toLowerCase().includes(term))
    );
  });

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <FaSignal className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">FreeRADIUS - Autenticação & BNGs</h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Gestão de servidores BNG (MikroTik, Huawei, Juniper, Cisco), sessões online, ciclo de vida e auto-corte
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
            onClick={() => setIsNasModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium shadow-sm transition"
          >
            <FaPlus className="w-4 h-4" /> Novo BNG / NAS
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

      {/* Metrics Cards */}
      <div className="grid grid-cols-1 sm:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400 rounded-lg">
            <FaSignal className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Sessões Online</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{sessions.length}</h3>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-indigo-100 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400 rounded-lg">
            <FaServer className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">BNGs Conectados</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{nasList.length}</h3>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-rose-100 dark:bg-rose-900/40 text-rose-600 dark:text-rose-400 rounded-lg">
            <FaBan className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Bloqueados</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
              {lifecycleSummary ? lifecycleSummary.totalBlockedUsers : 0}
            </h3>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-lg">
            <FaUnlockAlt className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Desbloqueios Hoje</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">
              {lifecycleSummary ? lifecycleSummary.todayUnblocksCount : 0}
            </h3>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-700 gap-6">
        <button
          onClick={() => setActiveTab('sessions')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'sessions'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaSignal className="w-4 h-4" /> Sessões Online ({sessions.length})
        </button>

        <button
          onClick={() => setActiveTab('nas')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'nas'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaServer className="w-4 h-4" /> Servidores NAS / BNG ({nasList.length})
        </button>

        <button
          onClick={() => setActiveTab('lifecycle')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'lifecycle'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaShieldAlt className="w-4 h-4" /> Ciclo de Vida & Auto-Corte
        </button>
      </div>

      {/* TAB 1: Sessions */}
      {activeTab === 'sessions' && (
        <div className="space-y-4">
          <div className="bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
            <div className="relative">
              <FaSearch className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                placeholder="Buscar por usuário, IP, MAC da ONT ou nome do cliente..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>
          </div>

          <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-sm">
            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 dark:bg-slate-900/50 border-b border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 text-xs font-semibold uppercase">
                    <th className="p-4">Assinante / Login</th>
                    <th className="p-4">IP Conexão</th>
                    <th className="p-4">MAC ONT (Calling)</th>
                    <th className="p-4">BNG / Concentrador</th>
                    <th className="p-4">Tráfego (Down / Up)</th>
                    <th className="p-4">Início da Sessão</th>
                    <th className="p-4 text-center">Ações</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                  {filteredSessions.length === 0 ? (
                    <tr>
                      <td colSpan={7} className="p-8 text-center text-slate-500">
                        Nenhuma sessão ativa encontrada.
                      </td>
                    </tr>
                  ) : (
                    filteredSessions.map((s) => (
                      <tr key={s.radacctId} className="hover:bg-slate-50 dark:hover:bg-slate-700/50 transition">
                        <td className="p-4 font-medium text-slate-900 dark:text-white">
                          <div className="flex items-center gap-2">
                            <FaUserAlt className="w-3.5 h-3.5 text-indigo-500" />
                            <span>{s.username}</span>
                          </div>
                          {s.customerName && (
                            <span className="text-xs text-slate-500 block mt-0.5">{s.customerName}</span>
                          )}
                        </td>
                        <td className="p-4 font-mono text-xs text-slate-700 dark:text-slate-300">
                          {s.framedIpAddress || '-'}
                        </td>
                        <td className="p-4 font-mono text-xs text-slate-500">
                          {s.callingStationId || '-'}
                        </td>
                        <td className="p-4 text-slate-700 dark:text-slate-300">
                          {s.nasShortname || s.nasIpAddress}
                        </td>
                        <td className="p-4 text-xs">
                          <span className="text-emerald-600 dark:text-emerald-400 font-semibold">↓ {((s.acctInputOctets || 0) / 1048576).toFixed(1)} MB</span> /{' '}
                          <span className="text-blue-600 dark:text-blue-400 font-semibold">↑ {((s.acctOutputOctets || 0) / 1048576).toFixed(1)} MB</span>
                        </td>
                        <td className="p-4 text-xs text-slate-500">
                          {s.acctStartTime ? new Date(s.acctStartTime).toLocaleString('pt-BR') : '-'}
                        </td>
                        <td className="p-4 text-center">
                          <button
                            onClick={() => handleDisconnect(s)}
                            disabled={disconnectingUser === s.username}
                            className="p-1.5 bg-rose-50 hover:bg-rose-100 text-rose-600 dark:bg-rose-950/40 dark:hover:bg-rose-900/60 dark:text-rose-400 rounded transition"
                            title="Derrubar sessão (PoD Disconnect)"
                          >
                            <FaPowerOff className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* TAB 2: NAS List */}
      {activeTab === 'nas' && (
        <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 overflow-hidden shadow-sm">
          <div className="overflow-x-auto">
            <table className="w-full text-left border-collapse text-sm">
              <thead>
                <tr className="bg-slate-50 dark:bg-slate-900/50 border-b border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 text-xs font-semibold uppercase">
                  <th className="p-4">Identificador / Nome</th>
                  <th className="p-4">IP / Hostname</th>
                  <th className="p-4">Fabricante (Vendor)</th>
                  <th className="p-4">Portas Auth / Acct</th>
                  <th className="p-4">Descrição</th>
                  <th className="p-4 text-center">Ações</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                {nasList.length === 0 ? (
                  <tr>
                    <td colSpan={6} className="p-8 text-center text-slate-500">
                      Nenhum servidor NAS cadastrado. Adicione seu MikroTik ou Huawei.
                    </td>
                  </tr>
                ) : (
                  nasList.map((nas) => (
                    <tr key={nas.id} className="hover:bg-slate-50 dark:hover:bg-slate-700/50 transition">
                      <td className="p-4 font-semibold text-slate-900 dark:text-white flex items-center gap-2">
                        <FaServer className="w-4 h-4 text-indigo-600" />
                        {nas.shortname || nas.nasname}
                      </td>
                      <td className="p-4 font-mono text-xs text-slate-700 dark:text-slate-300">
                        {nas.nasname}
                      </td>
                      <td className="p-4">
                        <span className="px-2 py-0.5 text-xs font-medium rounded-full bg-slate-100 text-slate-800 dark:bg-slate-700 dark:text-slate-300">
                          {nas.vendorType}
                        </span>
                      </td>
                      <td className="p-4 text-xs font-mono text-slate-500">
                        1812 / 1813 (CoA: 3799)
                      </td>
                      <td className="p-4 text-xs text-slate-500">
                        {nas.description || '-'}
                      </td>
                      <td className="p-4 text-center">
                        <button
                          onClick={() => handleDeleteNas(nas.id, nas.shortname || nas.nasname)}
                          className="p-1.5 text-rose-600 hover:bg-rose-50 dark:hover:bg-rose-950/40 rounded transition"
                          title="Excluir NAS"
                        >
                          <FaTrash className="w-4 h-4" />
                        </button>
                      </td>
                    </tr>
                  ))
                )}
              </tbody>
            </table>
          </div>
        </div>
      )}

      {/* TAB 3: Lifecycle & Auto-Corte */}
      {activeTab === 'lifecycle' && (
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
          {/* Coluna 1: Formulário de Configuração de Políticas */}
          <div className="lg:col-span-1 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-6">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FaSlidersH className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">Políticas de Inadimplência</h2>
              </div>
            </div>

            {policyConfig && (
              <form onSubmit={handleSavePolicy} className="space-y-4">
                <div>
                  <label className="flex items-center justify-between text-sm font-medium text-slate-700 dark:text-slate-300 mb-2 cursor-pointer">
                    <span>Habilitar Auto-Corte Periódico</span>
                    <input
                      type="checkbox"
                      checked={policyConfig.autoBlockEnabled}
                      onChange={(e) => setPolicyConfig({ ...policyConfig, autoBlockEnabled: e.target.checked })}
                      className="w-4 h-4 text-indigo-600 rounded focus:ring-indigo-500"
                    />
                  </label>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    Dias de Tolerância após Vencimento
                  </label>
                  <input
                    type="number"
                    min="1"
                    max="60"
                    value={policyConfig.toleranceDays}
                    onChange={(e) => setPolicyConfig({ ...policyConfig, toleranceDays: Number(e.target.value) })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                  <span className="text-xs text-slate-400 mt-1 block">
                    Ex: 5 dias (bloqueia faturas vencidas há 6+ dias)
                  </span>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                    Modo de Bloqueio Multi-Vendor
                  </label>
                  <select
                    value={policyConfig.blockMode}
                    onChange={(e) => setPolicyConfig({ ...policyConfig, blockMode: e.target.value as any })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  >
                    <option value="CAPTIVE_PORTAL">Captive Portal (Address-List pg_bloqueados)</option>
                    <option value="BANDWIDTH_REDUCTION">Redução de Banda (ex: 256k/256k)</option>
                    <option value="COMPLETE_DISCONNECT">Desconexão Completa (Rejeição de Auth)</option>
                  </select>
                </div>

                <div className="pt-2 border-t border-slate-200 dark:border-slate-700 space-y-2">
                  <label className="flex items-center justify-between text-xs font-medium text-slate-700 dark:text-slate-300 cursor-pointer">
                    <span>Desbloqueio Imediato após PIX</span>
                    <input
                      type="checkbox"
                      checked={policyConfig.unblockOnPayment}
                      onChange={(e) => setPolicyConfig({ ...policyConfig, unblockOnPayment: e.target.checked })}
                      className="w-4 h-4 text-indigo-600 rounded"
                    />
                  </label>

                  <label className="flex items-center justify-between text-xs font-medium text-slate-700 dark:text-slate-300 cursor-pointer">
                    <span>Enviar PoD no Bloqueio</span>
                    <input
                      type="checkbox"
                      checked={policyConfig.sendPodOnBlock}
                      onChange={(e) => setPolicyConfig({ ...policyConfig, sendPodOnBlock: e.target.checked })}
                      className="w-4 h-4 text-indigo-600 rounded"
                    />
                  </label>

                  <label className="flex items-center justify-between text-xs font-medium text-slate-700 dark:text-slate-300 cursor-pointer">
                    <span>Enviar PoD no Desbloqueio</span>
                    <input
                      type="checkbox"
                      checked={policyConfig.sendPodOnUnblock}
                      onChange={(e) => setPolicyConfig({ ...policyConfig, sendPodOnUnblock: e.target.checked })}
                      className="w-4 h-4 text-indigo-600 rounded"
                    />
                  </label>
                </div>

                <div className="pt-4 flex gap-3">
                  <button
                    type="submit"
                    disabled={savingPolicy}
                    className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-semibold transition shadow-sm"
                  >
                    {savingPolicy ? 'Salvando...' : 'Salvar Políticas'}
                  </button>
                </div>
              </form>
            )}

            <div className="pt-4 border-t border-slate-200 dark:border-slate-700">
              <button
                type="button"
                onClick={handleRunAutoBlock}
                disabled={runningAutoBlock}
                className="w-full flex items-center justify-center gap-2 px-4 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-sm font-semibold transition shadow-sm"
              >
                <FaPlay className="w-3.5 h-3.5" />
                {runningAutoBlock ? 'Executando Varredura...' : 'Executar Auto-Corte Agora'}
              </button>
            </div>
          </div>

          {/* Coluna 2 e 3: Auditoria de Eventos de Ciclo de Vida */}
          <div className="lg:col-span-2 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <FaHistory className="w-5 h-5 text-indigo-600 dark:text-indigo-400" />
                <h2 className="text-lg font-bold text-slate-900 dark:text-white">Auditoria de Cortes & Desbloqueios</h2>
              </div>
              <button
                onClick={() => loadData()}
                className="text-xs text-indigo-600 hover:underline flex items-center gap-1 font-semibold"
              >
                <FaSyncAlt className="w-3 h-3" /> Atualizar
              </button>
            </div>

            <div className="overflow-x-auto">
              <table className="w-full text-left border-collapse text-sm">
                <thead>
                  <tr className="bg-slate-50 dark:bg-slate-900/50 border-b border-slate-200 dark:border-slate-700 text-slate-600 dark:text-slate-400 text-xs font-semibold uppercase">
                    <th className="p-3">Ação</th>
                    <th className="p-3">Usuário PPPoE</th>
                    <th className="p-3">Motivo</th>
                    <th className="p-3">Status PoD</th>
                    <th className="p-3">Data / Hora</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100 dark:divide-slate-700 text-xs">
                  {lifecycleLogs.length === 0 ? (
                    <tr>
                      <td colSpan={5} className="p-6 text-center text-slate-500">
                        Nenhum registro de corte ou desbloqueio recente.
                      </td>
                    </tr>
                  ) : (
                    lifecycleLogs.map((log) => (
                      <tr key={log.id} className="hover:bg-slate-50 dark:hover:bg-slate-700/50 transition">
                        <td className="p-3 font-semibold">
                          {log.actionType === 'AUTO_BLOCK' || log.actionType === 'MANUAL_BLOCK' ? (
                            <span className="inline-flex items-center gap-1 text-rose-600 bg-rose-50 dark:bg-rose-950/40 px-2 py-0.5 rounded">
                              <FaBan className="w-3 h-3" /> {log.actionType}
                            </span>
                          ) : (
                            <span className="inline-flex items-center gap-1 text-emerald-600 bg-emerald-50 dark:bg-emerald-950/40 px-2 py-0.5 rounded">
                              <FaUnlockAlt className="w-3 h-3" /> {log.actionType}
                            </span>
                          )}
                        </td>
                        <td className="p-3 font-mono font-medium text-slate-800 dark:text-slate-200">
                          {log.username}
                          {log.customerName && <span className="block text-slate-400 text-[11px]">{log.customerName}</span>}
                        </td>
                        <td className="p-3 text-slate-600 dark:text-slate-400 max-w-xs truncate">
                          {log.reason || '-'}
                        </td>
                        <td className="p-3 text-slate-500 font-mono text-[11px]">
                          {log.details || 'PoD OK'}
                        </td>
                        <td className="p-3 text-slate-400 flex items-center gap-1">
                          <FaClock className="w-3 h-3 text-slate-300" />
                          {new Date(log.createdAt).toLocaleString('pt-BR')}
                        </td>
                      </tr>
                    ))
                  )}
                </tbody>
              </table>
            </div>
          </div>
        </div>
      )}

      {/* Modal Novo NAS */}
      {isNasModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200 dark:border-slate-700">
            <div className="flex items-center justify-between border-b border-slate-200 dark:border-slate-700 pb-3">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FaServer className="w-5 h-5 text-indigo-600" /> Cadastrar Concentrador BNG / NAS
              </h3>
              <button onClick={() => setIsNasModalOpen(false)} className="text-slate-400 hover:text-slate-600">
                ✕
              </button>
            </div>

            <form onSubmit={handleCreateNas} className="space-y-3 text-sm">
              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                  IP ou Hostname do BNG *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: 10.0.0.1 ou 192.168.88.1"
                  value={nasForm.nasname}
                  onChange={(e) => setNasForm({ ...nasForm, nasname: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                  Nome Curto (Identificador)
                </label>
                <input
                  type="text"
                  placeholder="Ex: BNG-MikroTik-Centro"
                  value={nasForm.shortname}
                  onChange={(e) => setNasForm({ ...nasForm, shortname: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                  Segredo Compartilhado (RADIUS Secret) *
                </label>
                <div className="relative">
                  <FaKey className="w-3.5 h-3.5 absolute left-3 top-3 text-slate-400" />
                  <input
                    type="password"
                    required
                    placeholder="Chave secreta configurada no RouterOS/Huawei"
                    value={nasForm.secret}
                    onChange={(e) => setNasForm({ ...nasForm, secret: e.target.value })}
                    className="w-full pl-9 pr-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                  Fabricante (Vendor)
                </label>
                <select
                  value={nasForm.vendorType}
                  onChange={(e) => setNasForm({ ...nasForm, vendorType: e.target.value as NasVendorType })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                >
                  <option value="MIKROTIK">MikroTik RouterOS</option>
                  <option value="HUAWEI">Huawei VRP (NE40 / ME60)</option>
                  <option value="JUNIPER">Juniper Networks (ERX / MX)</option>
                  <option value="ACCEL_PPP">Accel-PPP Linux BNG</option>
                  <option value="CISCO">Cisco IOS-XE / ASR</option>
                  <option value="OTHER">RFC Padrão / Outro</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-600 dark:text-slate-400 mb-1">
                  Observações / Localização
                </label>
                <textarea
                  rows={2}
                  placeholder="Ex: Torre Central - POP 01"
                  value={nasForm.description}
                  onChange={(e) => setNasForm({ ...nasForm, description: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="pt-3 flex gap-3">
                <button
                  type="button"
                  onClick={() => setIsNasModalOpen(false)}
                  className="flex-1 px-4 py-2 border border-slate-200 dark:border-slate-700 hover:bg-slate-50 dark:hover:bg-slate-700 text-slate-700 dark:text-slate-300 rounded-lg font-medium transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="flex-1 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium shadow-sm transition"
                >
                  Salvar BNG
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default RadiusManager;
