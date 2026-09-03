import React, { useState, useEffect } from 'react';
import { 
  FiTrendingUp, 
  FiAlertTriangle, 
  FiCheckCircle, 
  FiCalendar, 
  FiDollarSign, 
  FiActivity, 
  FiZap, 
  FiArrowRight, 
  FiRefreshCw, 
  FiHelpCircle,
  FiSliders
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { 
  DeleveragingProjectionDto, 
  SimulationRequest, 
  SimulationResponse 
} from '../../types/financial';

export const DeleveragingDashboard: React.FC = () => {
  const [projection, setProjection] = useState<DeleveragingProjectionDto | null>(null);
  const [loading, setLoading] = useState(true);

  // Estados do Simulador E Se...?
  const [simDesc, setSimDesc] = useState('Compra de Máquina OTDR + Fusão');
  const [simTotal, setSimTotal] = useState('36000');
  const [simInstallments, setSimInstallments] = useState('12');
  const [simFirstDue, setSimFirstDue] = useState(() => {
    const d = new Date();
    d.setMonth(d.getMonth() + 1);
    return d.toISOString().split('T')[0];
  });
  const [simulating, setSimulating] = useState(false);
  const [simResult, setSimResult] = useState<SimulationResponse | null>(null);

  useEffect(() => {
    loadProjection();
  }, []);

  const loadProjection = async () => {
    try {
      setLoading(true);
      const data = await financialService.getDeleveragingProjection();
      setProjection(data);
    } catch (err) {
      console.error('Erro ao carregar projeção de desalavancagem', err);
    } finally {
      setLoading(false);
    }
  };

  const handleSimulate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSimulating(true);
      const req: SimulationRequest = {
        description: simDesc,
        totalAmount: parseFloat(simTotal),
        installmentsCount: parseInt(simInstallments, 10),
        firstDueDate: simFirstDue
      };
      const res = await financialService.simulateInvestment(req);
      setSimResult(res);
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro na simulação');
    } finally {
      setSimulating(false);
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  if (loading) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8 flex items-center justify-center">
        <div className="flex items-center gap-3 text-slate-400 text-sm">
          <FiRefreshCw className="animate-spin text-purple-400" size={20} />
          Calculando os 3 Números Sagrados e a Curva de Desalavancagem de 36 Meses...
        </div>
      </div>
    );
  }

  if (!projection) {
    return (
      <div className="min-h-screen bg-slate-950 text-slate-100 p-8 text-center text-slate-500">
        Não foi possível carregar a projeção de desalavancagem.
      </div>
    );
  }

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 space-y-6">
      {/* Header Executivo Privativo */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800/80 pb-5">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2.5 rounded-2xl bg-purple-500/10 border border-purple-500/20 text-purple-400 shadow-inner">
              <FiTrendingUp size={24} />
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-xl font-bold tracking-tight text-white">Cockpit de Desalavancagem & EBITDA</h1>
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-400 border border-purple-500/20">
                  Privativo do Dono / CFO
                </span>
              </div>
              <p className="text-xs text-slate-400">
                Projeção contínua de caixa para 36 meses com identificação do Fundo do Poço e Data da Alforria Financeira
              </p>
            </div>
          </div>
        </div>

        <button
          onClick={loadProjection}
          className="flex items-center gap-2 text-xs text-slate-400 hover:text-white bg-slate-900 border border-slate-800 px-3.5 py-2 rounded-xl transition-colors cursor-pointer"
        >
          <FiRefreshCw size={14} />
          Recalcular Curva
        </button>
      </div>

      {/* OS 3 NÚMEROS SAGRADOS DO EMPRESÁRIO */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5">
        {/* NÚMERO 1: EBITDA SAGRADO */}
        <div className="relative overflow-hidden bg-gradient-to-br from-slate-900/90 to-slate-900/40 border border-emerald-500/30 rounded-3xl p-6 shadow-2xl backdrop-blur-md">
          <div className="absolute top-0 right-0 p-4 opacity-10 text-emerald-400 pointer-events-none">
            <FiActivity size={100} />
          </div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider text-emerald-400">
              1. EBITDA Sagrado de Telecom
            </span>
            <span className="text-[11px] px-2 py-0.5 rounded-full bg-emerald-500/10 text-emerald-400 border border-emerald-500/20 font-mono">
              Operacional Real
            </span>
          </div>
          <div className="text-3xl font-extrabold text-white tracking-tight">
            {formatCurrency(projection.currentEbitda)}
          </div>
          <div className="flex items-center gap-2 text-xs text-slate-400 mt-2">
            <span>MRR Ativo: <strong className="text-slate-200">{formatCurrency(projection.currentMrr)}</strong></span>
            <span>•</span>
            <span>Churn: <strong className="text-slate-200">{projection.monthlyChurnRatePercentage}%/mês</strong></span>
          </div>
        </div>

        {/* NÚMERO 2: O FUNDO DO POÇO (MAXIMUM DRAWDOWN) */}
        <div className="relative overflow-hidden bg-gradient-to-br from-slate-900/90 to-slate-900/40 border border-amber-500/30 rounded-3xl p-6 shadow-2xl backdrop-blur-md">
          <div className="absolute top-0 right-0 p-4 opacity-10 text-amber-400 pointer-events-none">
            <FiAlertTriangle size={100} />
          </div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider text-amber-400">
              2. Ponto do Fundo do Poço
            </span>
            <span className="text-[11px] px-2 py-0.5 rounded-full bg-amber-500/10 text-amber-400 border border-amber-500/20 font-mono">
              {projection.worstMonthYearMonth}
            </span>
          </div>
          <div className="text-3xl font-extrabold text-white tracking-tight">
            {formatCurrency(projection.worstMonthProjectedBalance)}
          </div>
          <div className="text-xs text-slate-400 mt-2">
            Mês de maior pressão de caixa devido ao acúmulo de parcelas de expansão.
          </div>
        </div>

        {/* NÚMERO 3: DATA DA ALFORRIA / VIRADA DE CAIXA */}
        <div className="relative overflow-hidden bg-gradient-to-br from-slate-900/90 to-slate-900/40 border border-purple-500/30 rounded-3xl p-6 shadow-2xl backdrop-blur-md">
          <div className="absolute top-0 right-0 p-4 opacity-10 text-purple-400 pointer-events-none">
            <FiZap size={100} />
          </div>
          <div className="flex items-center justify-between mb-3">
            <span className="text-xs font-semibold uppercase tracking-wider text-purple-400">
              3. Data da Alforria Financeira
            </span>
            <span className="text-[11px] px-2 py-0.5 rounded-full bg-purple-500/10 text-purple-400 border border-purple-500/20 font-mono">
              Em {projection.monthsUntilFreedom} meses
            </span>
          </div>
          <div className="text-3xl font-extrabold text-white tracking-tight">
            {projection.breakEvenYearMonth}
          </div>
          <div className="text-xs text-slate-400 mt-2">
            Momento em que as dívidas de máquinas/CAPEX são quitadas e o caixa decola.
          </div>
        </div>
      </div>

      {/* TIMELINE / CURVA CONTÍNUA DE 36 MESES */}
      <div className="bg-slate-900/40 border border-slate-800/80 rounded-3xl p-6 shadow-xl backdrop-blur-sm">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-3 mb-6 pb-4 border-b border-slate-800/60">
          <div>
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">
              Curva de Caixa Projetada (Próximos 36 Meses)
            </h2>
            <p className="text-xs text-slate-400">
              Saldo acumulado mês a mês considerando MRR com decaimento, OPEX operacional e quitação de parcelas
            </p>
          </div>
          <div className="flex items-center gap-4 text-xs">
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-amber-400 inline-block" />
              <span className="text-slate-400">Fundo do Poço</span>
            </div>
            <div className="flex items-center gap-1.5">
              <span className="w-2.5 h-2.5 rounded-full bg-emerald-400 inline-block" />
              <span className="text-slate-400">Alforria Financeira</span>
            </div>
          </div>
        </div>

        {/* Visualizador de Alta Densidade da Curva */}
        <div className="overflow-x-auto pb-2">
          <div className="min-w-[900px]">
            <table className="w-full text-left text-xs">
              <thead>
                <tr className="border-b border-slate-800 text-slate-400 uppercase tracking-wider text-[11px]">
                  <th className="py-2.5 px-3">Mês</th>
                  <th className="py-2.5 px-3 text-right">MRR Projetado</th>
                  <th className="py-2.5 px-3 text-right">OPEX Fixo</th>
                  <th className="py-2.5 px-3 text-right">Parcelas CAPEX</th>
                  <th className="py-2.5 px-3 text-right">Fluxo Líquido</th>
                  <th className="py-2.5 px-3 text-right">Saldo Acumulado</th>
                  <th className="py-2.5 px-3 text-center">Status</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-800/40 font-mono">
                {projection.timeline.slice(0, 24).map((pt) => {
                  const isPositive = pt.accumulatedCashBalance >= 0;
                  return (
                    <tr 
                      key={pt.yearMonth}
                      className={`hover:bg-slate-800/40 transition-colors ${
                        pt.isWorstMonth ? 'bg-amber-500/10' : pt.isBreakEvenMonth ? 'bg-emerald-500/10' : ''
                      }`}
                    >
                      <td className="py-2 px-3 font-semibold text-slate-200">
                        {pt.yearMonth} <span className="text-[10px] text-slate-500 font-sans">(Mês {pt.monthIndex})</span>
                      </td>
                      <td className="py-2 px-3 text-right text-slate-300">{formatCurrency(pt.projectedMrr)}</td>
                      <td className="py-2 px-3 text-right text-slate-400">-{formatCurrency(pt.projectedOpex)}</td>
                      <td className="py-2 px-3 text-right text-rose-400">
                        {pt.activeCapexInstallments > 0 ? `-${formatCurrency(pt.activeCapexInstallments)}` : 'R$ 0,00'}
                      </td>
                      <td className="py-2 px-3 text-right text-slate-300">
                        {formatCurrency(pt.netMonthlyCashFlow)}
                      </td>
                      <td className={`py-2 px-3 text-right font-bold ${isPositive ? 'text-emerald-400' : 'text-rose-400'}`}>
                        {formatCurrency(pt.accumulatedCashBalance)}
                      </td>
                      <td className="py-2 px-3 text-center">
                        {pt.isWorstMonth && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] bg-amber-500/20 text-amber-300 border border-amber-500/30 font-sans font-bold">
                            ⚠️ Fundo do Poço
                          </span>
                        )}
                        {pt.isBreakEvenMonth && (
                          <span className="px-2 py-0.5 rounded-full text-[10px] bg-emerald-500/20 text-emerald-300 border border-emerald-500/30 font-sans font-bold">
                            🚀 Alforria
                          </span>
                        )}
                        {!pt.isWorstMonth && !pt.isBreakEvenMonth && (
                          <span className="text-[10px] text-slate-600 font-sans">Normal</span>
                        )}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* SIMULADOR INTERATIVO "E SE...?" */}
      <div className="bg-gradient-to-br from-slate-900/60 to-slate-900/30 border border-slate-800 rounded-3xl p-6 shadow-2xl backdrop-blur-md">
        <div className="flex items-center gap-3 mb-4">
          <div className="p-2 rounded-xl bg-cyan-500/10 border border-cyan-500/20 text-cyan-400">
            <FiSliders size={20} />
          </div>
          <div>
            <h2 className="text-sm font-bold text-white uppercase tracking-wider">
              Simulador "E Se...?" (Avaliação Prévia de Novos Investimentos)
            </h2>
            <p className="text-xs text-slate-400">
              Teste o impacto de novas compras de máquinas, veículos ou cabos antes de assinar o contrato.
            </p>
          </div>
        </div>

        <form onSubmit={handleSimulate} className="grid grid-cols-1 md:grid-cols-4 gap-4 mb-6">
          <div>
            <label className="block text-xs text-slate-400 mb-1">Descrição do Investimento</label>
            <input
              type="text"
              required
              value={simDesc}
              onChange={(e) => setSimDesc(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-cyan-500"
            />
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Valor Total (R$)</label>
            <input
              type="number"
              step="0.01"
              required
              value={simTotal}
              onChange={(e) => setSimTotal(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-cyan-500 font-mono"
            />
          </div>

          <div>
            <label className="block text-xs text-slate-400 mb-1">Nº Parcelas</label>
            <input
              type="number"
              min="1"
              max="36"
              required
              value={simInstallments}
              onChange={(e) => setSimInstallments(e.target.value)}
              className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-cyan-500 font-mono"
            />
          </div>

          <div className="flex items-end">
            <button
              type="submit"
              disabled={simulating}
              className="w-full h-[38px] bg-cyan-600 hover:bg-cyan-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-cyan-950/40 transition-colors cursor-pointer flex items-center justify-center gap-2 disabled:opacity-50"
            >
              <FiZap size={16} />
              {simulating ? 'Simulando...' : 'Simular Impacto no Caixa'}
            </button>
          </div>
        </form>

        {/* Parecer do Simulador */}
        {simResult && (
          <div className={`p-5 rounded-2xl border transition-all animate-in fade-in zoom-in-95 ${
            simResult.feasible 
              ? 'bg-emerald-500/10 border-emerald-500/30 text-emerald-300' 
              : 'bg-rose-500/10 border-rose-500/30 text-rose-300'
          }`}>
            <div className="flex items-start gap-3">
              {simResult.feasible ? (
                <FiCheckCircle size={24} className="text-emerald-400 shrink-0 mt-0.5" />
              ) : (
                <FiAlertTriangle size={24} className="text-rose-400 shrink-0 mt-0.5" />
              )}
              <div className="space-y-2 flex-1">
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-2">
                  <h4 className="font-bold text-sm text-white">
                    {simResult.feasible ? 'INVESTIMENTO VIÁVEL' : 'ALERTA: RISCO DE LIQUIDEZ'}
                  </h4>
                  <span className="font-mono text-xs px-2.5 py-1 rounded-full bg-slate-900 border border-slate-800 text-slate-300">
                    Parcela Mensal: {formatCurrency(simResult.monthlyInstallmentAmount)}
                  </span>
                </div>

                <p className="text-xs leading-relaxed">{simResult.riskAnalysisSummary}</p>

                <div className="grid grid-cols-1 sm:grid-cols-3 gap-3 pt-3 border-t border-slate-800/40 text-xs">
                  <div>
                    <span className="text-slate-400 block text-[11px]">Novo Saldo Mínimo:</span>
                    <strong className="text-white font-mono">{formatCurrency(simResult.simulatedWorstBalance)}</strong>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Impacto no Fundo do Poço:</span>
                    <strong className="text-rose-400 font-mono">-{formatCurrency(simResult.balanceImpactAtWorstMonth)}</strong>
                  </div>
                  <div>
                    <span className="text-slate-400 block text-[11px]">Atraso na Alforria:</span>
                    <strong className="text-amber-400 font-mono">
                      {simResult.delayInMonthsForFreedom > 0 
                        ? `+${simResult.delayInMonthsForFreedom} meses` 
                        : 'Nenhum impacto temporal'}
                    </strong>
                  </div>
                </div>
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
