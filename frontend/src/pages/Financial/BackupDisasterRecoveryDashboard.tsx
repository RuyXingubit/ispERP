import React, { useState, useEffect } from 'react';
import {
  backupService,
  BackupOverview,
  BackupDestinationResponse,
  BackupExecutionLog,
  StorageTestResult,
  BackupDestinationRequest,
} from '../../services/backupService';
import {
  FiShield,
  FiCloud,
  FiHardDrive,
  FiRefreshCw,
  FiDownload,
  FiCheckCircle,
  FiAlertTriangle,
  FiTrash2,
  FiPlay,
  FiLock,
  FiKey,
  FiActivity,
  FiServer,
  FiPlus,
} from 'react-icons/fi';

export const BackupDisasterRecoveryDashboard: React.FC = () => {
  const [overview, setOverview] = useState<BackupOverview | null>(null);
  const [destinations, setDestinations] = useState<BackupDestinationResponse[]>([]);
  const [history, setHistory] = useState<BackupExecutionLog[]>([]);
  const [loading, setLoading] = useState<boolean>(true);
  const [executingBackup, setExecutingBackup] = useState<boolean>(false);
  const [testingDestId, setTestingDestId] = useState<string | null>(null);
  const [testResult, setTestResult] = useState<{ id: string; res: StorageTestResult } | null>(null);
  const [isAddDestOpen, setIsAddDestOpen] = useState<boolean>(false);

  // Form para novo destino
  const [newDest, setNewDest] = useState<BackupDestinationRequest>({
    name: '',
    storageType: 'S3_COMPATIBLE',
    endpointUrl: '',
    bucketName: '',
    region: 'auto',
    accessKey: '',
    secretKey: '',
    pathPrefix: 'backups/isperp',
    isPrimary: false,
  });

  const fetchData = async () => {
    try {
      setLoading(true);
      const [ovData, destData, histData] = await Promise.all([
        backupService.getOverview(),
        backupService.listDestinations(),
        backupService.listHistory(),
      ]);
      setOverview(ovData);
      setDestinations(destData);
      setHistory(histData);
    } catch (err) {
      console.error('Erro ao carregar dados de backup:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchData();
  }, []);

  const handleManualBackup = async () => {
    try {
      setExecutingBackup(true);
      await backupService.executeManualBackup();
      await fetchData();
    } catch (err: any) {
      alert('Erro ao disparar backup: ' + (err.response?.data?.message || err.message));
    } finally {
      setExecutingBackup(false);
    }
  };

  const handleTestDestination = async (id: string) => {
    try {
      setTestingDestId(id);
      setTestResult(null);
      const res = await backupService.testDestination(id);
      setTestResult({ id, res });
      await fetchData();
    } catch (err: any) {
      alert('Falha ao testar destino: ' + (err.response?.data?.message || err.message));
    } finally {
      setTestingDestId(null);
    }
  };

  const handleDeleteDestination = async (id: string) => {
    if (!window.confirm('Deseja realmente remover este destino de backup?')) return;
    try {
      await backupService.deleteDestination(id);
      await fetchData();
    } catch (err: any) {
      alert('Erro ao remover destino: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleCreateDestination = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await backupService.createDestination(newDest);
      setIsAddDestOpen(false);
      setNewDest({
        name: '',
        storageType: 'S3_COMPATIBLE',
        endpointUrl: '',
        bucketName: '',
        region: 'auto',
        accessKey: '',
        secretKey: '',
        pathPrefix: 'backups/isperp',
        isPrimary: false,
      });
      await fetchData();
    } catch (err: any) {
      alert('Erro ao cadastrar destino: ' + (err.response?.data?.message || err.message));
    }
  };

  const handleDownloadEmergencyKit = async () => {
    try {
      const blob = await backupService.downloadEmergencyKit();
      const url = window.URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = 'KIT_RESGATE_EMERGENCIA_ISPERP.md';
      document.body.appendChild(a);
      a.click();
      a.remove();
      window.URL.revokeObjectURL(url);
      await fetchData();
    } catch (err: any) {
      alert('Erro ao baixar Kit de Resgate: ' + (err.response?.data?.message || err.message));
    }
  };

  const formatBytes = (bytes?: number) => {
    if (!bytes || bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB', 'TB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return parseFloat((bytes / Math.pow(k, i)).toFixed(2)) + ' ' + sizes[i];
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-8 animate-fade-in font-sans text-slate-100">
      {/* HEADER EXECUTIVO */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-6">
        <div>
          <div className="flex items-center gap-2.5 text-xs font-bold uppercase tracking-wider text-emerald-400">
            <FiShield className="text-base" />
            <span>Blindagem Patrimonial & Disaster Recovery</span>
          </div>
          <h1 className="text-3xl font-black text-white tracking-tight mt-1">
            Backup Multi-Destino & Nuvem de Contingência
          </h1>
          <p className="text-sm text-slate-400 mt-1">
            Pipeline de streaming em memória sem estouro de disco, compressão ZStandard e criptografia militar AES-256.
          </p>
        </div>

        <div className="flex flex-wrap items-center gap-3">
          <button
            onClick={handleDownloadEmergencyKit}
            className="flex items-center gap-2 px-4 py-2.5 bg-slate-800 hover:bg-slate-700 border border-slate-700 rounded-xl text-xs font-semibold text-slate-200 transition shadow cursor-pointer"
          >
            <FiDownload />
            Baixar Kit de Resgate (PDF)
          </button>
          <button
            onClick={handleManualBackup}
            disabled={executingBackup}
            className="flex items-center gap-2 px-5 py-2.5 bg-emerald-600 hover:bg-emerald-500 disabled:opacity-50 text-white rounded-xl text-xs font-bold shadow-lg shadow-emerald-600/30 transition transform hover:-translate-y-0.5 cursor-pointer"
          >
            <FiPlay className={executingBackup ? 'animate-spin' : ''} />
            {executingBackup ? 'Executando Streaming...' : 'Fazer Backup Agora'}
          </button>
        </div>
      </div>

      {/* OS 3 INDICADORES SAGRADOS */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Card 1: Último Backup */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl relative overflow-hidden">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Último Backup</span>
            <div className="w-8 h-8 rounded-lg bg-emerald-500/10 border border-emerald-500/30 flex items-center justify-center text-emerald-400">
              <FiCheckCircle />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-white">
              {overview?.lastBackupSizeBytes ? formatBytes(overview.lastBackupSizeBytes) : 'Nenhum'}
            </span>
            {overview?.lastBackupCompressionRatio && (
              <span className="text-xs font-semibold text-emerald-400 ml-2">
                (-{overview.lastBackupCompressionRatio}%)
              </span>
            )}
          </div>
          <div className="mt-2 text-xs text-slate-400 flex items-center gap-1.5">
            <FiActivity className="text-slate-500 shrink-0" />
            <span>
              {overview?.lastBackupAt
                ? new Date(overview.lastBackupAt).toLocaleString('pt-BR')
                : 'Aguardando primeiro disparo'}
            </span>
          </div>
          {overview?.lastBackupSha256 && (
            <div className="mt-3 pt-2 border-t border-slate-800/80 text-[10px] font-mono text-indigo-400 truncate">
              SHA: {overview.lastBackupSha256}
            </div>
          )}
        </div>

        {/* Card 2: Agendamento & Retenção */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Rotina Automática</span>
            <div className="w-8 h-8 rounded-lg bg-indigo-500/10 border border-indigo-500/30 flex items-center justify-center text-indigo-400">
              <FiRefreshCw />
            </div>
          </div>
          <div className="mt-3">
            <span className="text-2xl font-black text-white">
              Diário às {overview?.cronExpression?.split(' ')[2] || '03'}:00 AM
            </span>
          </div>
          <div className="mt-2 text-xs text-slate-400">
            Retenção contínua dos últimos <strong className="text-slate-200">{overview?.retentionDays || 30} dias</strong> de histórico.
          </div>
          <div className="mt-3 pt-2 border-t border-slate-800/80 text-xs text-slate-400 flex items-center gap-1.5">
            <FiLock className="text-indigo-400" />
            <span>Modo: <strong>{overview?.securityMode || 'MANAGED_RESCUE'}</strong></span>
          </div>
        </div>

        {/* Card 3: Integridade Pericial (Dry-Run) */}
        <div className="bg-slate-900 border border-slate-800 rounded-2xl p-5 shadow-xl">
          <div className="flex items-center justify-between">
            <span className="text-xs font-bold uppercase tracking-wider text-slate-400">Teste de Restauração</span>
            <div className="w-8 h-8 rounded-lg bg-blue-500/10 border border-blue-500/30 flex items-center justify-center text-blue-400">
              <FiShield />
            </div>
          </div>
          <div className="mt-3 flex items-center gap-2">
            <span className="text-2xl font-black text-white">
              {overview?.isDryRunVerified ? '100% Íntegro' : 'Pendente'}
            </span>
          </div>
          <div className="mt-2 text-xs text-slate-400">
            Simulação de descriptografia e validação estrutural DDL realizada periodicamente.
          </div>
          <div className="mt-3 pt-2 border-t border-slate-800/80 text-xs text-slate-400 flex items-center gap-1.5">
            <FiKey className="text-amber-400" />
            <span>Kit de Resgate: <strong>{overview?.rescueKitDownloaded ? 'Baixado & Seguro' : 'Não baixado ainda'}</strong></span>
          </div>
        </div>
      </div>

      {/* SEÇÃO DE DESTINOS DE ARMAZENAMENTO */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-5">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-black text-white flex items-center gap-2">
              <FiCloud className="text-indigo-400" />
              Destinos de Armazenamento Remoto
            </h2>
            <p className="text-xs text-slate-400 mt-0.5">
              Envio simultâneo para S3 (AWS, Cloudflare R2, MinIO, Wasabi), SFTP ou NAS local da sede.
            </p>
          </div>
          <button
            onClick={() => setIsAddDestOpen(!isAddDestOpen)}
            className="flex items-center gap-1.5 px-3 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white rounded-xl text-xs font-bold transition cursor-pointer"
          >
            <FiPlus />
            Adicionar Destino
          </button>
        </div>

        {/* Modal/Form inline para novo destino */}
        {isAddDestOpen && (
          <form onSubmit={handleCreateDestination} className="p-4 rounded-xl bg-slate-950 border border-indigo-500/40 space-y-4">
            <span className="text-xs font-bold text-white uppercase tracking-wider block">Novo Destino de Storage</span>
            <div className="grid grid-cols-1 md:grid-cols-3 gap-3 text-xs">
              <div>
                <label className="text-slate-400 block mb-1">Nome de Identificação</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Cloudflare R2 Bucket"
                  value={newDest.name}
                  onChange={(e) => setNewDest({ ...newDest, name: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>
              <div>
                <label className="text-slate-400 block mb-1">Tipo de Armazenamento</label>
                <select
                  value={newDest.storageType}
                  onChange={(e) => setNewDest({ ...newDest, storageType: e.target.value as any })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                >
                  <option value="S3_COMPATIBLE">S3 Compatível (R2 / AWS / MinIO)</option>
                  <option value="LOCAL_VOLUME">Volume / NAS Local</option>
                  <option value="SFTP">SFTP Remoto</option>
                  <option value="ISPERP_CLOUD">ispERP Safe Cloud</option>
                </select>
              </div>
              <div>
                <label className="text-slate-400 block mb-1">Bucket (se S3)</label>
                <input
                  type="text"
                  placeholder="meu-bucket-backups"
                  value={newDest.bucketName}
                  onChange={(e) => setNewDest({ ...newDest, bucketName: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>
              <div className="md:col-span-2">
                <label className="text-slate-400 block mb-1">Endpoint URL (Custom S3 / R2 / MinIO)</label>
                <input
                  type="text"
                  placeholder="https://<account_id>.r2.cloudflarestorage.com"
                  value={newDest.endpointUrl}
                  onChange={(e) => setNewDest({ ...newDest, endpointUrl: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>
              <div>
                <label className="text-slate-400 block mb-1">Access Key ID</label>
                <input
                  type="text"
                  placeholder="AKIA..."
                  value={newDest.accessKey}
                  onChange={(e) => setNewDest({ ...newDest, accessKey: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>
              <div>
                <label className="text-slate-400 block mb-1">Secret Access Key</label>
                <input
                  type="password"
                  placeholder="••••••••••••••••"
                  value={newDest.secretKey}
                  onChange={(e) => setNewDest({ ...newDest, secretKey: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>
              <div>
                <label className="text-slate-400 block mb-1">Caminho / Prefixo</label>
                <input
                  type="text"
                  value={newDest.pathPrefix}
                  onChange={(e) => setNewDest({ ...newDest, pathPrefix: e.target.value })}
                  className="w-full px-3 py-2 bg-slate-900 border border-slate-700 rounded-lg text-white"
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setIsAddDestOpen(false)}
                className="px-3 py-1.5 bg-slate-800 text-slate-300 rounded-lg text-xs"
              >
                Cancelar
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 bg-indigo-600 hover:bg-indigo-500 text-white font-bold rounded-lg text-xs"
              >
                Salvar Destino
              </button>
            </div>
          </form>
        )}

        {/* Lista de destinos cadastrados */}
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {destinations.length === 0 ? (
            <div className="p-8 text-center text-slate-500 text-xs border border-dashed border-slate-800 rounded-xl md:col-span-2">
              Nenhum destino remoto cadastrado. O backup será salvo apenas no volume local padrão.
            </div>
          ) : (
            destinations.map((d) => (
              <div key={d.id} className="p-4 bg-slate-950/80 border border-slate-800 rounded-xl flex items-center justify-between gap-4">
                <div className="space-y-1">
                  <div className="flex items-center gap-2">
                    <FiServer className="text-indigo-400" />
                    <span className="font-bold text-white text-xs">{d.name}</span>
                    {d.isPrimary && (
                      <span className="px-1.5 py-0.5 bg-emerald-500/20 text-emerald-400 text-[10px] font-bold rounded">
                        Principal
                      </span>
                    )}
                  </div>
                  <div className="text-[11px] text-slate-400">
                    Tipo: <strong className="text-slate-300">{d.storageType}</strong> | Prefixo: <strong className="text-slate-300">{d.pathPrefix}</strong>
                  </div>
                  {d.lastTestStatus && (
                    <div className="text-[10px]">
                      Status Teste:{' '}
                      <span className={d.lastTestStatus === 'SUCCESS' ? 'text-emerald-400 font-bold' : 'text-rose-400 font-bold'}>
                        {d.lastTestStatus === 'SUCCESS' ? '✓ Operacional' : '✗ Erro de Conexão'}
                      </span>
                    </div>
                  )}
                </div>

                <div className="flex items-center gap-2 shrink-0">
                  <button
                    onClick={() => handleTestDestination(d.id)}
                    disabled={testingDestId === d.id}
                    className="p-2 bg-slate-800 hover:bg-slate-700 text-indigo-400 rounded-lg text-xs transition cursor-pointer"
                    title="Testar Conectividade em Tempo Real"
                  >
                    <FiRefreshCw className={testingDestId === d.id ? 'animate-spin' : ''} />
                  </button>
                  <button
                    onClick={() => handleDeleteDestination(d.id)}
                    className="p-2 bg-slate-800 hover:bg-rose-950/80 text-rose-400 rounded-lg text-xs transition cursor-pointer"
                    title="Remover Destino"
                  >
                    <FiTrash2 />
                  </button>
                </div>
              </div>
            ))
          )}
        </div>

        {/* Feedback do Teste de Conexão */}
        {testResult && (
          <div
            className={`p-3 rounded-xl border text-xs flex items-center justify-between ${
              testResult.res.success
                ? 'bg-emerald-950/60 border-emerald-500/40 text-emerald-300'
                : 'bg-rose-950/60 border-rose-500/40 text-rose-300'
            }`}
          >
            <div className="flex items-center gap-2">
              {testResult.res.success ? <FiCheckCircle /> : <FiAlertTriangle />}
              <span>{testResult.res.message}</span>
            </div>
            <span className="text-[10px] font-mono">Latência: {testResult.res.latencyMs}ms</span>
          </div>
        )}
      </div>

      {/* TABELA DE HISTÓRICO E AUDITORIA */}
      <div className="bg-slate-900 border border-slate-800 rounded-2xl p-6 shadow-xl space-y-4">
        <h2 className="text-lg font-black text-white flex items-center gap-2">
          <FiHardDrive className="text-emerald-400" />
          Histórico e Dossiês de Backup
        </h2>

        <div className="overflow-x-auto">
          <table className="w-full text-left text-xs">
            <thead className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[10px]">
              <tr>
                <th className="py-3 px-4">Arquivo / Cifra</th>
                <th className="py-3 px-4">Data/Hora</th>
                <th className="py-3 px-4">Tamanho Original</th>
                <th className="py-3 px-4">Tamanho Criptografado</th>
                <th className="py-3 px-4">Redução</th>
                <th className="py-3 px-4">Status</th>
                <th className="py-3 px-4">Dry-Run</th>
              </tr>
            </thead>
            <tbody className="divide-y divide-slate-800/60">
              {history.length === 0 ? (
                <tr>
                  <td colSpan={7} className="py-8 text-center text-slate-500">
                    Nenhum backup registrado no histórico.
                  </td>
                </tr>
              ) : (
                history.map((log) => (
                  <tr key={log.id} className="hover:bg-slate-850/50 transition">
                    <td className="py-3 px-4 font-mono text-indigo-300 font-semibold">
                      {log.fileName}
                      {log.sha256Hash && (
                        <span className="block text-[9px] text-slate-500 truncate max-w-xs font-mono">
                          SHA: {log.sha256Hash}
                        </span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-300">
                      {new Date(log.startedAt).toLocaleString('pt-BR')}
                    </td>
                    <td className="py-3 px-4 text-slate-400">
                      {formatBytes(log.originalSizeBytes)}
                    </td>
                    <td className="py-3 px-4 font-bold text-white">
                      {formatBytes(log.compressedSizeBytes)}
                    </td>
                    <td className="py-3 px-4 text-emerald-400 font-bold">
                      {log.compressionRatio ? `-${log.compressionRatio}%` : 'N/A'}
                    </td>
                    <td className="py-3 px-4">
                      <span
                        className={`px-2 py-0.5 rounded text-[10px] font-bold ${
                          log.status === 'SUCCESS'
                            ? 'bg-emerald-500/20 text-emerald-400 border border-emerald-500/30'
                            : 'bg-rose-500/20 text-rose-400 border border-rose-500/30'
                        }`}
                      >
                        {log.status}
                      </span>
                    </td>
                    <td className="py-3 px-4">
                      {log.isDryRunVerified ? (
                        <span className="text-emerald-400 font-semibold flex items-center gap-1 text-[11px]">
                          <FiCheckCircle /> Verificado
                        </span>
                      ) : (
                        <span className="text-slate-500 text-[11px]">-</span>
                      )}
                    </td>
                  </tr>
                ))
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
