import React, { useState, useEffect } from 'react';
import { 
  FiMapPin, 
  FiTrendingUp, 
  FiAlertCircle, 
  FiCheckCircle, 
  FiPlus, 
  FiRefreshCw, 
  FiActivity, 
  FiCompass, 
  FiRadio,
  FiX
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { NetworkProjectPaybackDto, NetworkProjectRequest } from '../../types/financial';

export const NetworkProjectsPayback: React.FC = () => {
  const [projects, setProjects] = useState<NetworkProjectPaybackDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [showModal, setShowModal] = useState(false);

  // Form State
  const [name, setName] = useState('');
  const [neighborhood, setNeighborhood] = useState('');
  const [city, setCity] = useState('');
  const [budgetAmount, setBudgetAmount] = useState('');
  const [targetSubscribers, setTargetSubscribers] = useState('100');
  const [notes, setNotes] = useState('');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadProjects();
  }, []);

  const loadProjects = async () => {
    try {
      setLoading(true);
      const data = await financialService.getNetworkProjectsPayback();
      setProjects(data);
    } catch (err) {
      console.error('Erro ao carregar payback dos projetos', err);
    } finally {
      setLoading(false);
    }
  };

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      const req: NetworkProjectRequest = {
        name,
        neighborhood,
        city,
        budgetAmount: parseFloat(budgetAmount),
        targetSubscribers: parseInt(targetSubscribers, 10),
        notes
      };
      await financialService.createNetworkProject(req);
      setShowModal(false);
      resetForm();
      loadProjects();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao cadastrar projeto');
    } finally {
      setSaving(false);
    }
  };

  const resetForm = () => {
    setName('');
    setNeighborhood('');
    setCity('');
    setBudgetAmount('');
    setTargetSubscribers('100');
    setNotes('');
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  const totalCapex = projects.reduce((acc, p) => acc + p.budgetAmount, 0);
  const totalPorts = projects.reduce((acc, p) => acc + p.totalPorts, 0);
  const occupiedPorts = projects.reduce((acc, p) => acc + p.occupiedPorts, 0);
  const avgOccupancy = totalPorts > 0 ? (occupiedPorts / totalPorts) * 100 : 0;

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 space-y-6">
      {/* Header Corporativo */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2.5 rounded-2xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
            <FiCompass size={24} />
          </div>
          <div>
            <div className="flex items-center gap-2">
              <h1 className="text-xl font-bold tracking-tight text-white">Mapa de Guerra Comercial & Payback de Rede</h1>
              <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-cyan-500/10 text-cyan-400 border border-cyan-500/20">
                Retorno de Expansão
              </span>
            </div>
            <p className="text-xs text-slate-400">
              Associação de CTOs a centros de custo por bairro, cálculo do tempo de retorno e direcionamento de vendas
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2.5">
          <button
            onClick={loadProjects}
            className="flex items-center gap-1.5 text-xs text-slate-400 hover:text-white bg-slate-900 border border-slate-800 px-3 py-2 rounded-xl transition-colors cursor-pointer"
          >
            <FiRefreshCw size={13} />
            Atualizar
          </button>
          <button
            onClick={() => setShowModal(true)}
            className="flex items-center gap-1.5 text-xs font-semibold bg-cyan-600 hover:bg-cyan-500 text-white px-3.5 py-2 rounded-xl shadow-lg shadow-cyan-950/40 transition-colors cursor-pointer"
          >
            <FiPlus size={15} />
            Novo Projeto de Expansão
          </button>
        </div>
      </div>

      {/* KPI Cards do Portfólio de Rede */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-5">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
            CAPEX Total Alocado em Rede
          </span>
          <span className="text-2xl font-bold font-mono text-white tracking-tight">
            {formatCurrency(totalCapex)}
          </span>
        </div>

        <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-5">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
            Capacidade Física Instalada
          </span>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono text-white tracking-tight">
              {occupiedPorts} / {totalPorts}
            </span>
            <span className="text-xs text-slate-400">portas atendendo</span>
          </div>
        </div>

        <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-5">
          <span className="text-xs font-semibold text-slate-400 uppercase tracking-wider block mb-1">
            Taxa Média de Ocupação da Fibra
          </span>
          <div className="flex items-baseline gap-2">
            <span className="text-2xl font-bold font-mono text-cyan-400 tracking-tight">
              {avgOccupancy.toFixed(1)}%
            </span>
            <span className="text-xs text-slate-500">do parque óptico</span>
          </div>
        </div>
      </div>

      {/* Grid de Projetos de Rede com Payback e Termômetro */}
      {loading ? (
        <div className="py-24 text-center text-xs text-slate-500">
          Calculando portas ocupadas, MRR por projeto e tempo de amortização...
        </div>
      ) : projects.length === 0 ? (
        <div className="py-24 text-center text-xs text-slate-500 border border-dashed border-slate-800 rounded-3xl">
          Nenhum projeto de expansão de rede cadastrado ainda. Clique em "Novo Projeto de Expansão" para iniciar.
        </div>
      ) : (
        <div className="grid grid-cols-1 lg:grid-cols-2 gap-5">
          {projects.map((proj) => {
            const isIdle = proj.priorityLevel === 'IDLE_NETWORK_FOCUS';
            const isHigh = proj.priorityLevel === 'HIGH_RETURN';

            return (
              <div 
                key={proj.projectId}
                className={`bg-slate-900/40 border rounded-3xl p-6 shadow-xl backdrop-blur-sm transition-all space-y-4 ${
                  isIdle 
                    ? 'border-amber-500/40 hover:border-amber-500/60' 
                    : isHigh 
                    ? 'border-emerald-500/40 hover:border-emerald-500/60' 
                    : 'border-slate-800/80 hover:border-slate-700'
                }`}
              >
                {/* Topo do Card */}
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <div className="flex items-center gap-2">
                      <span className="text-xs font-bold text-slate-400 flex items-center gap-1">
                        <FiMapPin size={13} className="text-cyan-400" />
                        {proj.neighborhood}, {proj.city}
                      </span>
                      <span className="text-[10px] px-2 py-0.5 rounded-full bg-slate-800 text-slate-300 font-mono">
                        {proj.ctoCount} CTOs
                      </span>
                    </div>
                    <h3 className="text-base font-bold text-white mt-1">{proj.name}</h3>
                  </div>

                  <span className="text-xs font-mono font-bold px-3 py-1 rounded-full bg-slate-900 border border-slate-800 text-slate-200">
                    CAPEX: {formatCurrency(proj.budgetAmount)}
                  </span>
                </div>

                {/* Barra de Ocupação de Portas */}
                <div className="space-y-1.5">
                  <div className="flex justify-between text-xs">
                    <span className="text-slate-400">Ocupação de Portas</span>
                    <span className="font-mono font-bold text-white">
                      {proj.occupiedPorts} / {proj.totalPorts} ({proj.occupancyRatePercentage}%)
                    </span>
                  </div>
                  <div className="w-full h-2 bg-slate-800 rounded-full overflow-hidden">
                    <div 
                      className={`h-full rounded-full transition-all duration-500 ${
                        isIdle ? 'bg-amber-400' : isHigh ? 'bg-emerald-400' : 'bg-cyan-500'
                      }`}
                      style={{ width: `${Math.min(proj.occupancyRatePercentage, 100)}%` }}
                    />
                  </div>
                </div>

                {/* Métricas de Retorno Financeiro */}
                <div className="grid grid-cols-3 gap-3 p-3 rounded-2xl bg-slate-950/60 border border-slate-800/60 font-mono text-xs">
                  <div>
                    <span className="text-[10px] font-sans text-slate-500 block">MRR no Bairro:</span>
                    <strong className="text-white">{formatCurrency(proj.generatedMrr)}</strong>
                  </div>
                  <div>
                    <span className="text-[10px] font-sans text-slate-500 block">Margem Líquida:</span>
                    <strong className="text-cyan-400">{formatCurrency(proj.monthlyNetContribution)}/m</strong>
                  </div>
                  <div>
                    <span className="text-[10px] font-sans text-slate-500 block">Payback Estimado:</span>
                    <strong className={proj.isPaybackReached ? 'text-emerald-400' : 'text-amber-400'}>
                      {proj.accumulatedPaybackMonths < 900 ? `${proj.accumulatedPaybackMonths} meses` : 'Sem vendas'}
                    </strong>
                  </div>
                </div>

                {/* O DIRECIONADOR COMERCIAL DO DONO */}
                <div className={`p-3.5 rounded-2xl border text-xs leading-relaxed flex items-start gap-2.5 ${
                  isIdle 
                    ? 'bg-amber-500/10 border-amber-500/30 text-amber-300' 
                    : isHigh 
                    ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300' 
                    : 'bg-slate-900 border-slate-800 text-slate-300'
                }`}>
                  <FiRadio size={16} className="shrink-0 mt-0.5" />
                  <div>
                    <span className="font-bold block mb-0.5">Termômetro Comercial:</span>
                    {proj.commercialDirectionAlert}
                  </div>
                </div>
              </div>
            );
          })}
        </div>
      )}

      {/* Modal de Cadastro de Projeto de Rede */}
      {showModal && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-black/80 backdrop-blur-sm animate-in fade-in">
          <div className="bg-slate-900 border border-slate-800 rounded-3xl max-w-lg w-full p-6 shadow-2xl space-y-4">
            <div className="flex items-center justify-between border-b border-slate-800 pb-3">
              <h3 className="font-bold text-white text-sm uppercase tracking-wider">Novo Projeto de Expansão de Rede</h3>
              <button onClick={() => setShowModal(false)} className="text-slate-400 hover:text-white cursor-pointer">
                <FiX size={18} />
              </button>
            </div>

            <form onSubmit={handleCreate} className="space-y-3 text-xs">
              <div>
                <label className="block text-slate-400 mb-1">Nome do Projeto</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Expansão Bairro Esperança - Fase 2"
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">Bairro</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: Esperança"
                    value={neighborhood}
                    onChange={(e) => setNeighborhood(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
                  />
                </div>
                <div>
                  <label className="block text-slate-400 mb-1">Cidade</label>
                  <input
                    type="text"
                    required
                    placeholder="Ex: Santarém"
                    value={city}
                    onChange={(e) => setCity(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
                  />
                </div>
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-slate-400 mb-1">Investimento Total (CAPEX R$)</label>
                  <input
                    type="number"
                    step="0.01"
                    required
                    placeholder="Ex: 85000.00"
                    value={budgetAmount}
                    onChange={(e) => setBudgetAmount(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white font-mono"
                  />
                </div>
                <div>
                  <label className="block text-slate-400 mb-1">Meta de Assinantes</label>
                  <input
                    type="number"
                    min="1"
                    required
                    value={targetSubscribers}
                    onChange={(e) => setTargetSubscribers(e.target.value)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white font-mono"
                  />
                </div>
              </div>

              <div>
                <label className="block text-slate-400 mb-1">Observações Técnicas / Traçado</label>
                <textarea
                  rows={2}
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  placeholder="Detalhes de postes alugados, cabos de 24FO utilizados..."
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-white"
                />
              </div>

              <div className="flex items-center justify-end gap-2 pt-2 border-t border-slate-800">
                <button
                  type="button"
                  onClick={() => setShowModal(false)}
                  className="px-4 py-2 rounded-xl text-slate-400 hover:text-white cursor-pointer"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-cyan-600 hover:bg-cyan-500 text-white font-semibold rounded-xl transition-colors cursor-pointer disabled:opacity-50"
                >
                  {saving ? 'Cadastrando...' : 'Salvar Projeto'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
