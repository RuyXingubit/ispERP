import React, { useState, useEffect } from 'react';
import { 
  FiShield as ShieldCheck, 
  FiCheckCircle as CheckCircle2, 
  FiXCircle as XCircle, 
  FiAlertTriangle as AlertTriangle, 
  FiMessageSquare as MessageSquare, 
  FiSend as Send, 
  FiUser as User, 
  FiFileText as FileText,
  FiClock as Clock
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { WorkOrderFeeDto } from '../../types/financial';

export const WorkOrderFeeWaivers: React.FC = () => {
  const [pendingWaivers, setPendingWaivers] = useState<WorkOrderFeeDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [actingWorkOrderId, setActingWorkOrderId] = useState<string | null>(null);
  const [successBanner, setSuccessBanner] = useState<string | null>(null);

  // Mock do ID do gestor logado (em prod vem do authContext)
  const currentManagerId = '01a00000-0000-7000-0000-000000000099';

  useEffect(() => {
    loadPending();
  }, []);

  const loadPending = async () => {
    try {
      setLoading(true);
      const data = await financialService.getPendingWaivers();
      setPendingWaivers(data);
    } catch (err) {
      console.error('Erro ao carregar isenções pendentes', err);
    } finally {
      setLoading(false);
    }
  };

  const handleAudit = async (workOrder: WorkOrderFeeDto, approved: boolean) => {
    try {
      setActingWorkOrderId(workOrder.workOrderId);
      await financialService.auditWaiver(
        workOrder.workOrderId, 
        currentManagerId, 
        { approved, notes: approved ? 'Isenção aprovada por relacionamento' : 'Taxa mantida na fatura' }
      );

      if (approved) {
        setSuccessBanner(
          `Isenção da O.S. ${workOrder.protocol} aprovada! Notificação oficial anti-fraude disparada para o WhatsApp do cliente confirmando serviço 100% gratuito.`
        );
      } else {
        setSuccessBanner(`Isenção da O.S. ${workOrder.protocol} recusada. O valor será cobrado normalmente na fatura.`);
      }

      await loadPending();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao auditar isenção');
    } finally {
      setActingWorkOrderId(null);
    }
  };

  const formatCurrency = (val: number) => {
    return new Intl.NumberFormat('pt-BR', { style: 'currency', currency: 'BRL' }).format(val);
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6 border-b border-slate-800 pb-5">
        <div className="flex items-center gap-3">
          <div className="p-2 rounded-xl bg-amber-500/10 border border-amber-500/20 text-amber-400">
            <ShieldCheck size={24} />
          </div>
          <div>
            <h1 className="text-xl font-bold tracking-tight text-white">Esteira de Isenção de Taxas de O.S.</h1>
            <p className="text-xs text-slate-400">
              Alçada gerencial e disparo de notificação anti-fraude ao cliente (Prevenção de Caixa 2 em Campo)
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2 text-xs text-slate-400 bg-slate-900 border border-slate-800 px-3 py-1.5 rounded-xl">
          <Clock size={14} className="text-amber-400" />
          <span>Pendentes de Auditoria: <strong className="text-white">{pendingWaivers.length}</strong></span>
        </div>
      </div>

      {/* Banner de Sucesso Anti-Fraude */}
      {successBanner && (
        <div className="mb-6 p-4 rounded-2xl bg-emerald-500/10 border border-emerald-500/30 text-emerald-300 text-xs flex items-center justify-between gap-3 animate-in fade-in slide-in-from-top-2">
          <div className="flex items-center gap-2.5">
            <Send size={16} className="text-emerald-400 shrink-0" />
            <span>{successBanner}</span>
          </div>
          <button 
            onClick={() => setSuccessBanner(null)}
            className="text-emerald-400 hover:text-emerald-200 font-bold px-2 py-1"
          >
            ✕
          </button>
        </div>
      )}

      {/* Informativo de Segurança */}
      <div className="mb-6 p-4 rounded-2xl bg-slate-900/40 border border-slate-800/80 text-xs text-slate-400 flex items-start gap-3">
        <AlertTriangle size={18} className="text-amber-400 shrink-0 mt-0.5" />
        <div>
          <strong className="text-white block mb-1">Como esta esteira blinda o provedor contra serviços "por fora"?</strong>
          Ao aprovar a isenção, o sistema envia imediatamente um WhatsApp ao cliente com a mensagem da diretoria: 
          <em> "Seu serviço foi 100% isentado. Não pague nenhum valor ao técnico em campo."</em> Isso desarma cobranças clandestinas no ato da visita.
        </div>
      </div>

      {/* Lista de Solicitações Pendentes */}
      <div className="space-y-4">
        {loading ? (
          <div className="py-20 text-center text-xs text-slate-500">
            Carregando solicitações de isenção...
          </div>
        ) : pendingWaivers.length === 0 ? (
          <div className="py-20 text-center text-xs text-slate-500 bg-slate-900/20 border border-slate-800/40 rounded-2xl">
            <CheckCircle2 size={32} className="mx-auto text-slate-600 mb-2" />
            Nenhuma taxa aguardando aprovação gerencial no momento. Fila limpa!
          </div>
        ) : (
          pendingWaivers.map((wo) => {
            const isActing = actingWorkOrderId === wo.workOrderId;
            return (
              <div 
                key={wo.workOrderId}
                className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-5 shadow-xl backdrop-blur-sm"
              >
                <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 pb-4 border-b border-slate-800/60">
                  <div className="flex items-center gap-3">
                    <span className="font-mono text-xs font-bold text-amber-400 px-2.5 py-1 bg-amber-500/10 border border-amber-500/20 rounded-lg">
                      {wo.protocol}
                    </span>
                    <div>
                      <span className="font-semibold text-sm text-white block">{wo.customerName || 'Cliente ISP'}</span>
                      <span className="text-xs text-slate-400">Serviço: {wo.serviceType || 'Mudança de Endereço / Ponto'}</span>
                    </div>
                  </div>

                  <div className="text-right">
                    <span className="text-xs text-slate-400 block">Valor da Taxa Tabelada</span>
                    <span className="text-lg font-bold text-rose-400">{formatCurrency(wo.standardFeeAmount)}</span>
                  </div>
                </div>

                <div className="py-4">
                  <span className="text-xs text-slate-400 block mb-1.5 flex items-center gap-1.5">
                    <MessageSquare size={14} className="text-slate-500" />
                    Justificativa Comercial do Atendente:
                  </span>
                  <div className="p-3 rounded-xl bg-slate-950/70 border border-slate-800 text-xs text-slate-200 italic">
                    "{wo.waiverReason || 'Sem justificativa preenchida.'}"
                  </div>
                </div>

                <div className="flex items-center justify-end gap-3 pt-3 border-t border-slate-800/60">
                  <button
                    disabled={isActing}
                    onClick={() => handleAudit(wo, false)}
                    className="flex items-center gap-1.5 px-4 py-2 rounded-xl text-xs font-medium bg-slate-800 hover:bg-rose-950/40 text-slate-300 hover:text-rose-400 border border-slate-700/60 hover:border-rose-500/30 transition-all cursor-pointer disabled:opacity-50"
                  >
                    <XCircle size={15} />
                    Recusar Isenção (Cobrar)
                  </button>

                  <button
                    disabled={isActing}
                    onClick={() => handleAudit(wo, true)}
                    className="flex items-center gap-1.5 px-4 py-2 bg-emerald-600 hover:bg-emerald-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-emerald-950/40 transition-all cursor-pointer disabled:opacity-50"
                  >
                    <CheckCircle2 size={15} />
                    {isActing ? 'Processando...' : 'Aprovar Isenção & Notificar Cliente'}
                  </button>
                </div>
              </div>
            );
          })
        )}
      </div>
    </div>
  );
};
