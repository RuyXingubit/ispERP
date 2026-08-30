import React, { useState, useEffect, useCallback } from 'react';
import { Nas, RadiusSession, NasVendorType } from '../../types/radius';
import { radiusService } from '../../services/radiusService';
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
} from 'react-icons/fa';

export const RadiusManager: React.FC = () => {
  const [activeTab, setActiveTab] = useState<'sessions' | 'nas'>('sessions');
  const [sessions, setSessions] = useState<RadiusSession[]>([]);
  const [nasList, setNasList] = useState<Nas[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');

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
      const [sessionsData, nasData] = await Promise.all([
        radiusService.getActiveSessions(),
        radiusService.getAllNas(),
      ]);
      setSessions(sessionsData);
      setNasList(nasData);
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
            Gestão de servidores BNG (MikroTik, Huawei, Juniper, Cisco), sessões online e desconexão PoD
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
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
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
          <div className="p-3 bg-purple-100 dark:bg-purple-900/40 text-purple-600 dark:text-purple-400 rounded-lg">
            <FaMicrochip className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Protocolos Ativos</p>
            <h3 className="text-xl font-bold text-slate-900 dark:text-white mt-1">PPPoE & IPoE</h3>
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

          <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm overflow-hidden">
            {loading ? (
              <div className="p-8 text-center text-slate-500">Carregando sessões...</div>
            ) : filteredSessions.length === 0 ? (
              <div className="p-8 text-center text-slate-500">Nenhuma sessão online no momento.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                  <thead className="bg-slate-50 dark:bg-slate-900/50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                    <tr>
                      <th className="px-6 py-4">Usuário PPPoE</th>
                      <th className="px-6 py-4">Cliente / Assinante</th>
                      <th className="px-6 py-4">Endereço IP</th>
                      <th className="px-6 py-4">MAC ONT / BNG</th>
                      <th className="px-6 py-4">Tráfego (Down / Up)</th>
                      <th className="px-6 py-4 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                    {filteredSessions.map((s) => (
                      <tr key={s.radacctId} className="hover:bg-slate-50 dark:hover:bg-slate-750 transition">
                        <td className="px-6 py-4">
                          <div className="flex items-center gap-2">
                            <span className="w-2.5 h-2.5 rounded-full bg-emerald-500 animate-pulse" />
                            <span className="font-mono font-bold text-slate-900 dark:text-white">
                              {s.username}
                            </span>
                          </div>
                          {s.acctStartTime && (
                            <span className="text-xs text-slate-400">
                              Online desde: {new Date(s.acctStartTime).toLocaleTimeString()}
                            </span>
                          )}
                        </td>

                        <td className="px-6 py-4">
                          {s.customerName ? (
                            <div>
                              <p className="font-medium text-slate-900 dark:text-white">{s.customerName}</p>
                              <p className="text-xs text-slate-400">{s.customerCpfCnpj}</p>
                            </div>
                          ) : (
                            <span className="text-xs text-slate-400">Não vinculado</span>
                          )}
                        </td>

                        <td className="px-6 py-4 font-mono text-xs">
                          <span className="font-bold text-indigo-600 dark:text-indigo-400">
                            {s.framedIpAddress || 'N/A'}
                          </span>
                          {s.delegatedIpv6Prefix && (
                            <p className="text-purple-600 text-xs mt-0.5">{s.delegatedIpv6Prefix}</p>
                          )}
                        </td>

                        <td className="px-6 py-4 text-xs font-mono">
                          <div>{s.callingStationId || 'N/A'}</div>
                          <div className="text-slate-400 font-sans mt-0.5">
                            BNG: {s.nasShortname || s.nasIpAddress}
                          </div>
                        </td>

                        <td className="px-6 py-4 text-xs">
                          <div className="text-emerald-600 font-medium">
                            ↓ {(s.acctInputOctets / (1024 * 1024)).toFixed(1)} MB
                          </div>
                          <div className="text-blue-600 font-medium mt-0.5">
                            ↑ {(s.acctOutputOctets / (1024 * 1024)).toFixed(1)} MB
                          </div>
                        </td>

                        <td className="px-6 py-4 text-right">
                          <button
                            onClick={() => handleDisconnect(s)}
                            disabled={disconnectingUser === s.username}
                            className="p-2 text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-900/30 rounded-lg transition"
                            title="Derrubar Conexão (PoD)"
                          >
                            <FaPowerOff className="w-4 h-4" />
                          </button>
                        </td>
                      </tr>
                    ))}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: NAS / BNGs */}
      {activeTab === 'nas' && (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {nasList.map((nas) => (
            <div
              key={nas.id}
              className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4 flex flex-col justify-between"
            >
              <div>
                <div className="flex justify-between items-start">
                  <div>
                    <span className="px-2 py-0.5 text-xs font-bold rounded bg-indigo-100 text-indigo-800 dark:bg-indigo-900/50 dark:text-indigo-300">
                      {nas.vendorType}
                    </span>
                    <h3 className="font-bold text-lg text-slate-900 dark:text-white mt-2">
                      {nas.shortname || nas.nasname}
                    </h3>
                  </div>
                  <button
                    onClick={() => handleDeleteNas(nas.id, nas.shortname || nas.nasname)}
                    className="text-rose-500 hover:text-rose-700 p-1"
                  >
                    <FaTrash className="w-4 h-4" />
                  </button>
                </div>

                <div className="mt-4 space-y-2 text-xs">
                  <div className="flex justify-between">
                    <span className="text-slate-500">IP / Host:</span>
                    <span className="font-mono font-bold">{nas.nasname}</span>
                  </div>
                  <div className="flex justify-between">
                    <span className="text-slate-500">Secret:</span>
                    <span className="font-mono text-slate-400">••••••••</span>
                  </div>
                  {nas.description && (
                    <p className="text-slate-400 pt-2 border-t border-slate-100 dark:border-slate-700">
                      {nas.description}
                    </p>
                  )}
                </div>
              </div>

              <div className="pt-4 border-t border-slate-100 dark:border-slate-700 flex justify-between items-center text-xs text-slate-500">
                <span>Portas CoA: 3799</span>
                <span className="text-emerald-500 font-semibold flex items-center gap-1">
                  <FaCheckCircle className="w-3 h-3" /> Ativo
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* MODAL: Novo NAS */}
      {isNasModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">Cadastrar BNG / NAS</h3>
            <form onSubmit={handleCreateNas} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Endereço IP ou Hostname do BNG *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: 10.0.0.1 ou bng01.provedor.net"
                  value={nasForm.nasname}
                  onChange={(e) => setNasForm({ ...nasForm, nasname: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Nome Amigável / Identificador
                </label>
                <input
                  type="text"
                  placeholder="Ex: BNG-Centro-Huawei-NE40"
                  value={nasForm.shortname}
                  onChange={(e) => setNasForm({ ...nasForm, shortname: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Fabricante (Vendor) *
                  </label>
                  <select
                    value={nasForm.vendorType}
                    onChange={(e: any) => setNasForm({ ...nasForm, vendorType: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="MIKROTIK">MikroTik RouterOS</option>
                    <option value="HUAWEI">Huawei (NE40/ME60)</option>
                    <option value="JUNIPER">Juniper (MX/ERX)</option>
                    <option value="ACCEL_PPP">Accel-PPP</option>
                    <option value="CISCO">Cisco ASR/IOS</option>
                    <option value="GENERIC">Genérico / RFC</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Secret RADIUS *
                  </label>
                  <input
                    type="password"
                    required
                    placeholder="Chave secreta"
                    value={nasForm.secret}
                    onChange={(e) => setNasForm({ ...nasForm, secret: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  />
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Descrição / Localização
                </label>
                <input
                  type="text"
                  placeholder="Ex: Datacenter Pop 1"
                  value={nasForm.description}
                  onChange={(e) => setNasForm({ ...nasForm, description: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
                <button
                  type="button"
                  onClick={() => setIsNasModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
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
