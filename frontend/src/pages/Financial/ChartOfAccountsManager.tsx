import React, { useState, useEffect } from 'react';
import { 
  FiFolder as FolderTree, 
  FiPlus as Plus, 
  FiChevronRight as ChevronRight, 
  FiChevronDown as ChevronDown, 
  FiSearch as Search, 
  FiX as X
} from 'react-icons/fi';
import { financialService } from '../../services/financialService';
import { ChartOfAccountDto, AccountType, DreCategory } from '../../types/financial';

export const ChartOfAccountsManager: React.FC = () => {
  const [tree, setTree] = useState<ChartOfAccountDto[]>([]);
  const [loading, setLoading] = useState(true);
  const [search, setSearch] = useState('');
  const [selectedGroup, setSelectedGroup] = useState<string>('ALL');
  const [expandedNodes, setExpandedNodes] = useState<Record<string, boolean>>({
    '01': true,
    '02': true,
    '03': true,
    '04': true,
    '05': true,
  });

  // Modal de criação
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [parentAccount, setParentAccount] = useState<ChartOfAccountDto | null>(null);
  const [newCode, setNewCode] = useState('');
  const [newName, setNewName] = useState('');
  const [newType, setNewType] = useState<AccountType>('OPEX');
  const [newDreCategory, setNewDreCategory] = useState<DreCategory>('OPEX_ADMIN');
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    loadTree();
  }, []);

  const loadTree = async () => {
    try {
      setLoading(true);
      const data = await financialService.getChartTree();
      setTree(data);
    } catch (err) {
      console.error('Erro ao carregar plano de contas', err);
    } finally {
      setLoading(false);
    }
  };

  const toggleNode = (code: string) => {
    setExpandedNodes(prev => ({ ...prev, [code]: !prev[code] }));
  };

  const openCreateModal = (parent?: ChartOfAccountDto) => {
    if (parent) {
      setParentAccount(parent);
      setNewCode(`${parent.code}.`);
      setNewType(parent.accountType);
      setNewDreCategory(parent.dreCategory);
    } else {
      setParentAccount(null);
      setNewCode('');
      setNewType('OPEX');
      setNewDreCategory('OPEX_ADMIN');
    }
    setNewName('');
    setIsModalOpen(true);
  };

  const handleSaveAccount = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      setSaving(true);
      await financialService.createAccount({
        parentId: parentAccount ? parentAccount.id : null,
        code: newCode.trim(),
        name: newName.trim(),
        accountType: newType,
        dreCategory: newDreCategory,
        isSynthetic: false,
        isAnalytical: true,
        active: true
      });
      setIsModalOpen(false);
      await loadTree();
    } catch (err: any) {
      alert(err.response?.data?.message || 'Erro ao criar conta contábil');
    } finally {
      setSaving(false);
    }
  };

  const getTypeBadgeColor = (type: AccountType) => {
    switch (type) {
      case 'REVENUE':
        return 'bg-emerald-500/10 text-emerald-400 border-emerald-500/20';
      case 'TAX':
        return 'bg-amber-500/10 text-amber-400 border-amber-500/20';
      case 'DIRECT_COST':
        return 'bg-cyan-500/10 text-cyan-400 border-cyan-500/20';
      case 'OPEX':
        return 'bg-rose-500/10 text-rose-400 border-rose-500/20';
      case 'CAPEX':
        return 'bg-purple-500/10 text-purple-400 border-purple-500/20';
      default:
        return 'bg-slate-500/10 text-slate-400 border-slate-500/20';
    }
  };

  const renderNode = (node: ChartOfAccountDto, depth: number = 0) => {
    const hasChildren = node.children && node.children.length > 0;
    const isExpanded = expandedNodes[node.code] ?? (depth < 2);

    // Filtro simples de pesquisa
    const matchesSearch = search === '' || 
      node.name.toLowerCase().includes(search.toLowerCase()) || 
      node.code.includes(search);

    if (!matchesSearch && !hasChildren) return null;

    return (
      <div key={node.id} className="flex flex-col">
        <div 
          className={`flex items-center justify-between py-2.5 px-3 rounded-lg hover:bg-slate-800/40 transition-colors group ${
            depth === 0 ? 'bg-slate-800/20 my-1 font-semibold text-white' : 'text-slate-300'
          }`}
          style={{ paddingLeft: `${depth * 24 + 12}px` }}
        >
          <div className="flex items-center gap-2.5 flex-1 min-w-0">
            {hasChildren ? (
              <button 
                onClick={() => toggleNode(node.code)}
                className="p-1 hover:bg-slate-700/50 rounded text-slate-400 hover:text-white transition-colors"
              >
                {isExpanded ? <ChevronDown size={16} /> : <ChevronRight size={16} />}
              </button>
            ) : (
              <span className="w-6" />
            )}

            <span className="font-mono text-xs text-slate-400 px-1.5 py-0.5 rounded bg-slate-900/60 border border-slate-700/50">
              {node.code}
            </span>

            <span className={`truncate text-sm ${node.isSynthetic ? 'font-medium text-slate-100' : 'text-slate-300'}`}>
              {node.name}
            </span>

            {node.isSynthetic && (
              <span className="text-[10px] uppercase font-bold tracking-wider px-1.5 py-0.5 rounded bg-slate-800 text-slate-400 border border-slate-700/60">
                Sintética
              </span>
            )}
          </div>

          <div className="flex items-center gap-3">
            <span className={`text-xs px-2 py-0.5 rounded-full border ${getTypeBadgeColor(node.accountType)}`}>
              {node.accountType}
            </span>

            <button
              onClick={() => openCreateModal(node)}
              title="Adicionar subconta analítica"
              className="opacity-0 group-hover:opacity-100 p-1 hover:bg-slate-700/60 rounded text-slate-400 hover:text-emerald-400 transition-all"
            >
              <Plus size={15} />
            </button>
          </div>
        </div>

        {hasChildren && isExpanded && (
          <div className="flex flex-col border-l border-slate-800 ml-5 my-0.5">
            {node.children!.map(child => renderNode(child, depth + 1))}
          </div>
        )}
      </div>
    );
  };

  const filteredTree = tree.filter(node => {
    if (selectedGroup === 'ALL') return true;
    return node.code.startsWith(selectedGroup);
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 p-6">
      {/* Header Executivo */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 mb-6 border-b border-slate-800 pb-5">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-2 rounded-xl bg-purple-500/10 border border-purple-500/20 text-purple-400">
              <FolderTree size={24} />
            </div>
            <div>
              <h1 className="text-xl font-bold tracking-tight text-white">Plano de Contas Dinâmico</h1>
              <p className="text-xs text-slate-400">Estrutura contábil canônica de 5 níveis para ISPs brasileiros</p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={() => openCreateModal()}
            className="flex items-center gap-2 bg-emerald-600 hover:bg-emerald-500 text-white text-xs font-semibold px-4 py-2.5 rounded-xl shadow-lg shadow-emerald-950/30 transition-all cursor-pointer"
          >
            <Plus size={16} />
            Nova Conta Raiz
          </button>
        </div>
      </div>

      {/* Barra de Filtros Rápidos */}
      <div className="flex flex-col sm:flex-row items-center justify-between gap-3 mb-6">
        <div className="flex items-center gap-2 w-full sm:w-80 relative">
          <Search size={16} className="absolute left-3 text-slate-500" />
          <input
            type="text"
            placeholder="Filtrar por código ou descrição..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="w-full pl-9 pr-4 py-2 bg-slate-900/60 border border-slate-800 rounded-xl text-xs text-slate-200 focus:outline-none focus:border-purple-500/50 transition-colors"
          />
        </div>

        <div className="flex items-center gap-1.5 overflow-x-auto w-full sm:w-auto pb-1">
          {[
            { id: 'ALL', label: 'Todos os Grupos' },
            { id: '01', label: '01. Receitas', color: 'text-emerald-400' },
            { id: '02', label: '02. Impostos', color: 'text-amber-400' },
            { id: '03', label: '03. Interconexão', color: 'text-cyan-400' },
            { id: '04', label: '04. OPEX', color: 'text-rose-400' },
            { id: '05', label: '05. CAPEX', color: 'text-purple-400' }
          ].map(grp => (
            <button
              key={grp.id}
              onClick={() => setSelectedGroup(grp.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-medium whitespace-nowrap transition-colors cursor-pointer ${
                selectedGroup === grp.id 
                  ? 'bg-slate-800 text-white border border-slate-700 shadow-sm' 
                  : 'text-slate-400 hover:text-slate-200 hover:bg-slate-900/50'
              }`}
            >
              <span className={grp.color}>{grp.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Árvore Contábil */}
      <div className="bg-slate-900/40 border border-slate-800/80 rounded-2xl p-4 shadow-xl backdrop-blur-sm">
        {loading ? (
          <div className="py-20 text-center text-xs text-slate-500">
            Carregando árvore do plano de contas...
          </div>
        ) : filteredTree.length === 0 ? (
          <div className="py-20 text-center text-xs text-slate-500">
            Nenhuma conta encontrada para o filtro selecionado.
          </div>
        ) : (
          <div className="divide-y divide-slate-800/30">
            {filteredTree.map(rootNode => renderNode(rootNode))}
          </div>
        )}
      </div>

      {/* Modal de Criação de Conta */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/70 backdrop-blur-xs p-4">
          <div className="bg-slate-900 border border-slate-800 rounded-2xl w-full max-w-md p-6 shadow-2xl animate-in fade-in zoom-in-95 duration-150">
            <div className="flex items-center justify-between pb-4 border-b border-slate-800 mb-5">
              <div className="flex items-center gap-2">
                <FolderTree size={18} className="text-purple-400" />
                <h3 className="font-semibold text-sm text-white">
                  {parentAccount ? `Adicionar Subconta em ${parentAccount.code}` : 'Nova Conta Raiz'}
                </h3>
              </div>
              <button 
                onClick={() => setIsModalOpen(false)}
                className="text-slate-400 hover:text-white transition-colors"
              >
                <X size={18} />
              </button>
            </div>

            <form onSubmit={handleSaveAccount} className="space-y-4">
              <div>
                <label className="block text-xs text-slate-400 mb-1">Código Estrutural</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: 04.01.05"
                  value={newCode}
                  onChange={(e) => setNewCode(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs font-mono text-white focus:outline-none focus:border-purple-500"
                />
              </div>

              <div>
                <label className="block text-xs text-slate-400 mb-1">Descrição da Conta</label>
                <input
                  type="text"
                  required
                  placeholder="Ex: Aluguel de Fibra Apagada (Dark Fiber)"
                  value={newName}
                  onChange={(e) => setNewName(e.target.value)}
                  className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-purple-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-3">
                <div>
                  <label className="block text-xs text-slate-400 mb-1">Grupo Contábil</label>
                  <select
                    value={newType}
                    onChange={(e) => setNewType(e.target.value as AccountType)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-purple-500"
                  >
                    <option value="REVENUE">Receita (01)</option>
                    <option value="TAX">Imposto (02)</option>
                    <option value="DIRECT_COST">Interconexão (03)</option>
                    <option value="OPEX">OPEX (04)</option>
                    <option value="CAPEX">CAPEX (05)</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs text-slate-400 mb-1">Categoria DRE</label>
                  <select
                    value={newDreCategory}
                    onChange={(e) => setNewDreCategory(e.target.value as DreCategory)}
                    className="w-full px-3 py-2 bg-slate-950 border border-slate-800 rounded-xl text-xs text-white focus:outline-none focus:border-purple-500"
                  >
                    <option value="GROSS_REVENUE">Receita Bruta</option>
                    <option value="TAX_DEDUCTION">Dedução de Imposto</option>
                    <option value="DIRECT_COST_INTERCONNECTION">Interconexão IP</option>
                    <option value="OPEX_HR">RH e Folha</option>
                    <option value="OPEX_POLES">Postes e Concessionárias</option>
                    <option value="OPEX_FLEET">Frota Operacional</option>
                    <option value="OPEX_MARKETING">Marketing e Vendas</option>
                    <option value="OPEX_ADMIN">Administrativo</option>
                    <option value="CAPEX_NETWORK">CAPEX Rede / Fibras</option>
                    <option value="CAPEX_EQUIPMENT">CAPEX Aparelhos / ONTs</option>
                    <option value="CAPEX_FLEET">CAPEX Veículos</option>
                  </select>
                </div>
              </div>

              <div className="flex items-center justify-end gap-2 pt-4 border-t border-slate-800 mt-6">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 rounded-xl text-xs font-medium text-slate-400 hover:text-white hover:bg-slate-800 transition-colors"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  disabled={saving}
                  className="px-4 py-2 bg-purple-600 hover:bg-purple-500 text-white rounded-xl text-xs font-semibold shadow-lg shadow-purple-950/40 transition-colors disabled:opacity-50"
                >
                  {saving ? 'Salvando...' : 'Criar Conta'}
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
