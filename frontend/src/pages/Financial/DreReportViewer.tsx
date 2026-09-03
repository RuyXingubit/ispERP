import React, { useState, useEffect } from 'react';
import { 
  FiFileText, 
  FiCalendar, 
  FiFilter, 
  FiRefreshCw, 
  FiPieChart, 
  FiTrendingUp, 
  FiLayers,
  FiDownload
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { DreReportDto, AccountingMethod } from '../../types/financial';

export const DreReportViewer: React.FC = () => {
  const [method, setMethod] = useState<AccountingMethod>('ACCRUAL');
  const [startDate, setStartDate] = useState(() => {
    const d = new Date();
    d.setDate(1);
    return d.toISOString().split('T')[0];
  });
  const [endDate, setEndDate] = useState(() => {
    const d = new Date();
    d.setMonth(d.getMonth() + 1, 0);
    return d.toISOString().split('T')[0];
  });
  const [dre, setDre] = useState<DreReportDto | null>(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    loadDre();
  }, [method]);

  const loadDre = async () => {
    try {
      setLoading(true);
      const data = await financialService.getDre(startDate, endDate, method);
      setDre(data);
    } catch (err) {
      console.error('Erro ao gerar DRE', err);
    } finally {
      setLoading(false);
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6 space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-emerald-500/10 border border-emerald-500/20 text-emerald-400">
            <FiFileText size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">DRE Telecom em Tempo Real</h1>
            <p className="text-xs text-slate-400">
              Demonstração do Resultado do Exercício por Competência e Caixa com EBITDA Real de Provedor
            </p>
          </div>
        </div>

        {/* Chaveador de Regime de Contabilidade */}
        <div className="flex items-center gap-1.5 p-1 bg-slate-900 border border-slate-800 rounded-xl">
          <button
            onClick={() => setMethod('ACCRUAL')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
              method === 'ACCRUAL' 
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Regime de Competência
          </button>
          <button
            onClick={() => setMethod('CASH')}
            className={`px-3 py-1.5 rounded-lg text-xs font-medium transition-colors cursor-pointer ${
              method === 'CASH' 
                ? 'bg-slate-800 text-white shadow-sm border border-slate-700' 
                : 'text-slate-400 hover:text-white'
            }`}
          >
            Regime de Caixa (Real)
          </button>
        </div>
      </div>

      {/* Barra de Filtro de Período */}
      <div className="flex flex-col sm:flex-row items-center gap-3 bg-slate-900/40 border border-slate-800/80 rounded-2xl p-4">
        <div className="flex items-center gap-2 text-xs text-slate-400">
          <FiCalendar size={15} />
          <span>Período:</span>
        </div>
        <input
          type="date"
          value={startDate}
          onChange={(e) => setStartDate(e.target.value)}
          className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white"
        />
        <span className="text-xs text-slate-500">até</span>
        <input
          type="date"
          value={endDate}
          onChange={(e) => setEndDate(e.target.value)}
          className="px-3 py-1.5 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white"
        />
        <button
          onClick={loadDre}
          className="flex items-center gap-1.5 px-3 py-1.5 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold shadow transition-colors cursor-pointer"
        >
          <FiRefreshCw size={13} />
          Atualizar Relatório
        </button>
      </div>

      {/* Relatório DRE em Formato Cascata */}
      {loading ? (
        <div className="py-24 text-center text-xs text-slate-500">
          Consolidando receitas, impostos e custos operacionais...
        </div>
      ) : !dre ? (
        <div className="py-24 text-center text-xs text-slate-500">
          Não foi possível gerar a DRE para o período selecionado.
        </div>
      ) : (
        <div className="bg-slate-900/40 border border-slate-800/80 rounded-3xl p-6 shadow-2xl backdrop-blur-md max-w-4xl mx-auto">
          <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-4">
            <h3 className="font-bold text-sm text-white uppercase tracking-wider">
              Demonstração do Resultado ({method === 'ACCRUAL' ? 'Competência' : 'Caixa'})
            </h3>
            <span className="text-xs text-slate-400 font-mono">
              {new Date(dre.periodStart).toLocaleDateString('pt-BR')} a {new Date(dre.periodEnd).toLocaleDateString('pt-BR')}
            </span>
          </div>

          <div className="space-y-1 text-xs font-mono">
            {/* 1. Receita Bruta */}
            <div className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-slate-900/80 text-white font-bold">
              <span className="font-sans">(+) 01. RECEITA OPERACIONAL BRUTA</span>
              <span className="text-emerald-400">{formatCurrency(dre.grossRevenue)}</span>
            </div>

            {/* 2. Deduções */}
            <div className="flex items-center justify-between py-2 px-3 text-rose-400 pl-6">
              <span className="font-sans">(-) 02. Impostos e Deduções (DAS, ICMS, PIS/COFINS, FUST)</span>
              <span>-{formatCurrency(dre.taxDeductions)}</span>
            </div>

            {/* 3. Receita Líquida */}
            <div className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-slate-900/40 text-slate-200 font-semibold border-t border-slate-800">
              <span className="font-sans">(=) RECEITA OPERACIONAL LÍQUIDA</span>
              <span>{formatCurrency(dre.netRevenue)}</span>
            </div>

            {/* 4. Custos Diretos Interconexão */}
            <div className="flex items-center justify-between py-2 px-3 text-rose-400 pl-6">
              <span className="font-sans">(-) 03. Custos de Interconexão (Trânsito IP Primário/Secundário & PTT)</span>
              <span>-{formatCurrency(dre.directCostsInterconnection)}</span>
            </div>

            {/* 5. Margem de Contribuição */}
            <div className="flex items-center justify-between py-2.5 px-3 rounded-xl bg-slate-900/40 text-cyan-300 font-semibold border-t border-slate-800">
              <span className="font-sans">(=) MARGEM DE CONTRIBUIÇÃO DE TELECOM</span>
              <span>{formatCurrency(dre.contributionMargin)}</span>
            </div>

            {/* 6. OPEX */}
            <div className="pt-2 pb-1 px-3 text-slate-400 font-sans font-bold text-[11px] uppercase tracking-wider">
              (-) 04. Despesas Operacionais (OPEX):
            </div>
            <div className="flex items-center justify-between py-1.5 px-3 text-slate-400 pl-8">
              <span className="font-sans">Pessoal e Recursos Humanos</span>
              <span className="text-rose-400">-{formatCurrency(dre.opexHr)}</span>
            </div>
            <div className="flex items-center justify-between py-1.5 px-3 text-slate-400 pl-8">
              <span className="font-sans">Compartilhamento de Postes (Concessionárias)</span>
              <span className="text-rose-400">-{formatCurrency(dre.opexPoles)}</span>
            </div>
            <div className="flex items-center justify-between py-1.5 px-3 text-slate-400 pl-8">
              <span className="font-sans">Frota e Combustível</span>
              <span className="text-rose-400">-{formatCurrency(dre.opexFleet)}</span>
            </div>
            <div className="flex items-center justify-between py-1.5 px-3 text-slate-400 pl-8">
              <span className="font-sans">Marketing e Vendas</span>
              <span className="text-rose-400">-{formatCurrency(dre.opexMarketing)}</span>
            </div>
            <div className="flex items-center justify-between py-1.5 px-3 text-slate-400 pl-8">
              <span className="font-sans">Administrativo e Outros</span>
              <span className="text-rose-400">-{formatCurrency(dre.opexAdmin)}</span>
            </div>

            {/* 7. EBITDA SAGRADO */}
            <div className="flex items-center justify-between py-3 px-4 rounded-2xl bg-gradient-to-r from-purple-950/40 to-slate-900 border border-purple-500/40 text-white font-extrabold text-sm my-3 shadow-lg">
              <div className="flex items-center gap-2 font-sans">
                <FiTrendingUp className="text-purple-400" />
                <span>(=) EBITDA OPERACIONAL DE TELECOM</span>
                <span className="text-xs px-2 py-0.5 rounded-full bg-purple-500/20 text-purple-300 font-mono">
                  Margem: {dre.ebitdaMarginPercentage}%
                </span>
              </div>
              <span className="text-purple-300 text-base">{formatCurrency(dre.ebitda)}</span>
            </div>

            {/* 8. CAPEX */}
            <div className="flex items-center justify-between py-2 px-3 text-rose-400 pl-6">
              <span className="font-sans">(-) 05. Amortização de CAPEX / Financiamentos de Expansão</span>
              <span>-{formatCurrency(dre.capexAmortization)}</span>
            </div>

            {/* 9. Fluxo de Caixa Livre */}
            <div className="flex items-center justify-between py-3 px-4 rounded-2xl bg-slate-950 border border-slate-800 text-white font-bold my-2">
              <span className="font-sans">(=) FLUXO DE CAIXA LIVRE DO PROVEDOR</span>
              <span className={dre.freeCashFlow >= 0 ? 'text-emerald-400' : 'text-rose-400'}>
                {formatCurrency(dre.freeCashFlow)}
              </span>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
