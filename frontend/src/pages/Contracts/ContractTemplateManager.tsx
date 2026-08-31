import React, { useState, useEffect, useCallback, useRef } from 'react';
import {
  ContractTemplate,
  ContractTemplateRequest,
  ContractTemplateVariableInfo,
  DocumentType,
} from '../../types/contract-signature';
import { contractSignatureService } from '../../services/contractSignatureService';
import {
  FaFileContract,
  FaPlus,
  FaEdit,
  FaTrash,
  FaCopy,
  FaEye,
  FaCheckCircle,
  FaTimesCircle,
  FaTag,
  FaShieldAlt,
  FaSearch,
  FaSyncAlt,
  FaCode,
  FaFileAlt,
  FaHandshake,
} from 'react-icons/fa';

export const ContractTemplateManager: React.FC = () => {
  const [templates, setTemplates] = useState<ContractTemplate[]>([]);
  const [variables, setVariables] = useState<ContractTemplateVariableInfo[]>([]);
  const [loading, setLoading] = useState(true);
  const [searchTerm, setSearchTerm] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('ALL');
  const [activeVariableCategory, setActiveVariableCategory] = useState<string>('ALL');

  // Modal / Editor State
  const [isModalOpen, setIsModalOpen] = useState(false);
  const [editingTemplate, setEditingTemplate] = useState<ContractTemplate | null>(null);
  const [previewTab, setPreviewTab] = useState<'editor' | 'preview' | 'split'>('split');
  const [previewContent, setPreviewContent] = useState<string>('');
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  // Form State
  const [formData, setFormData] = useState<ContractTemplateRequest>({
    name: '',
    documentType: 'SERVICE_AGREEMENT',
    version: 1,
    isActive: true,
    contentMarkdown: '',
    consentClause: '',
  });

  const textareaRef = useRef<HTMLTextAreaElement>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [tplData, varData] = await Promise.all([
        contractSignatureService.listTemplates(),
        contractSignatureService.getVariables(),
      ]);
      setTemplates(tplData);
      setVariables(varData);
    } catch (err: any) {
      showNotification(err.message || 'Erro ao carregar modelos de contratos.', 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  const handleOpenCreateModal = () => {
    setEditingTemplate(null);
    setFormData({
      name: '',
      documentType: 'SERVICE_AGREEMENT',
      version: 1,
      isActive: true,
      contentMarkdown: `# CONTRATO DE PRESTAÇÃO DE SERVIÇOS SCM E SVA

**PRESTADORA:** {{company.name}}, CNPJ: {{company.cnpj}}
**ASSINANTE:** {{customer.name}}, CPF/CNPJ: {{customer.cpf_cnpj}}

### CLÁUSULA 1ª - DO PLANO E VELOCIDADES
1.1. O Assinante contrata o plano **{{plan.name}}** com velocidade nominal de **{{plan.download_speed}}** de download e **{{plan.upload_speed}}** de upload.
1.2. O endereço de instalação é: **{{contract.full_installation_address}}**.

### CLÁUSULA 2ª - DO VALOR E VENCIMENTO
2.1. O valor mensal contratado é de **{{contract.monthly_fee}}** com vencimento todo dia **{{contract.due_day}}**.

### CLÁUSULA 3ª - DA ASSINATURA ELETRÔNICA AVANÇADA
3.1. Este contrato é formalizado nos termos da MP 2.200-2/2001 e Lei 14.063/2020 mediante autenticação bancária instantânea via Pix do titular.
`,
      consentClause: 'Ao realizar o pagamento do Pix pela conta bancária do titular, declaro estar plenamente de acordo com as cláusulas deste contrato, servindo a transação como minha assinatura eletrônica definitiva.',
    });
    setPreviewTab('split');
    setIsModalOpen(true);
  };

  const handleOpenEditModal = (template: ContractTemplate) => {
    setEditingTemplate(template);
    setFormData({
      name: template.name,
      documentType: template.documentType,
      version: template.version,
      isActive: template.isActive,
      contentMarkdown: template.contentMarkdown,
      consentClause: template.consentClause,
    });
    setPreviewTab('split');
    setIsModalOpen(true);
  };

  const handleInsertVariable = (tag: string) => {
    const textarea = textareaRef.current;
    if (!textarea) {
      setFormData((prev) => ({
        ...prev,
        contentMarkdown: prev.contentMarkdown + ' ' + tag,
      }));
      return;
    }

    const start = textarea.selectionStart;
    const end = textarea.selectionEnd;
    const current = formData.contentMarkdown;
    const updated = current.substring(0, start) + tag + current.substring(end);

    setFormData((prev) => ({ ...prev, contentMarkdown: updated }));

    setTimeout(() => {
      textarea.focus();
      textarea.setSelectionRange(start + tag.length, start + tag.length);
    }, 50);
  };

  const handleUpdatePreview = async () => {
    try {
      const rendered = await contractSignatureService.previewTemplate(formData.contentMarkdown);
      setPreviewContent(rendered);
    } catch {
      setPreviewContent(formData.contentMarkdown);
    }
  };

  useEffect(() => {
    if (isModalOpen) {
      handleUpdatePreview();
    }
  }, [formData.contentMarkdown, isModalOpen]);

  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formData.name.trim() || !formData.contentMarkdown.trim() || !formData.consentClause.trim()) {
      showNotification('Preencha todos os campos obrigatórios.', 'error');
      return;
    }

    try {
      if (editingTemplate) {
        await contractSignatureService.updateTemplate(editingTemplate.id, formData);
        showNotification('Modelo de contrato atualizado com sucesso!');
      } else {
        await contractSignatureService.createTemplate(formData);
        showNotification('Modelo de contrato criado com sucesso!');
      }
      setIsModalOpen(false);
      loadData();
    } catch (err: any) {
      showNotification(err.message || 'Erro ao salvar modelo de contrato.', 'error');
    }
  };

  const handleDelete = async (id: string) => {
    if (!window.confirm('Tem certeza que deseja remover este modelo de contrato?')) return;
    try {
      await contractSignatureService.deleteTemplate(id);
      showNotification('Modelo de contrato removido.');
      loadData();
    } catch (err: any) {
      showNotification(err.message || 'Erro ao remover modelo.', 'error');
    }
  };

  const handleClone = async (id: string) => {
    try {
      await contractSignatureService.cloneTemplate(id);
      showNotification('Modelo clonado com sucesso!');
      loadData();
    } catch (err: any) {
      showNotification(err.message || 'Erro ao clonar modelo.', 'error');
    }
  };

  const filteredTemplates = templates.filter((tpl) => {
    const matchesSearch =
      tpl.name.toLowerCase().includes(searchTerm.toLowerCase()) ||
      tpl.contentMarkdown.toLowerCase().includes(searchTerm.toLowerCase());
    const matchesCategory =
      selectedCategory === 'ALL' || tpl.documentType === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  const getDocTypeBadge = (type: DocumentType) => {
    switch (type) {
      case 'SERVICE_AGREEMENT':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-blue-100 text-blue-800">SCM / SVA</span>;
      case 'LOYALTY_TERM':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-purple-100 text-purple-800">Fidelidade 12M</span>;
      case 'EQUIPMENT_COMODATO':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-emerald-100 text-emerald-800">Comodato ONT</span>;
      case 'CUSTOM_TERM':
        return <span className="px-2.5 py-0.5 rounded-full text-xs font-semibold bg-amber-100 text-amber-800">Personalizado</span>;
    }
  };

  const filteredVariables = variables.filter((v) => {
    if (activeVariableCategory === 'ALL') return true;
    return v.category === activeVariableCategory;
  });

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Toast Notification */}
      {notification && (
        <div
          className={`fixed top-5 right-5 z-50 px-4 py-3 rounded-lg shadow-lg border text-sm flex items-center gap-2 ${
            notification.type === 'success'
              ? 'bg-emerald-50 border-emerald-300 text-emerald-800'
              : 'bg-rose-50 border-rose-300 text-rose-800'
          }`}
        >
          {notification.type === 'success' ? <FaCheckCircle /> : <FaTimesCircle />}
          {notification.message}
        </div>
      )}

      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white p-6 rounded-2xl shadow-sm border border-slate-200">
        <div>
          <div className="flex items-center gap-3">
            <div className="p-3 bg-indigo-50 text-indigo-600 rounded-xl">
              <FaFileContract className="w-6 h-6" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-slate-800">Modelos de Contratos & Termos Customizáveis</h1>
              <p className="text-sm text-slate-500">
                Configure cláusulas, regras de fidelidade e termos com variáveis dinâmicas e assinatura via Pix (MP 2.200-2/01).
              </p>
            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <button
            onClick={loadData}
            className="flex items-center gap-2 px-3 py-2 text-slate-600 hover:text-slate-800 bg-slate-100 hover:bg-slate-200 rounded-lg text-sm font-medium transition"
            title="Atualizar lista"
          >
            <FaSyncAlt className={loading ? 'animate-spin' : ''} />
            Atualizar
          </button>
          <button
            onClick={handleOpenCreateModal}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-semibold shadow-sm transition"
          >
            <FaPlus />
            Novo Modelo
          </button>
        </div>
      </div>

      {/* Filters & Search */}
      <div className="flex flex-col md:flex-row items-center justify-between gap-4 bg-slate-50 p-4 rounded-xl border border-slate-200">
        <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto">
          {[
            { id: 'ALL', label: 'Todos os Modelos' },
            { id: 'SERVICE_AGREEMENT', label: 'SCM / SVA' },
            { id: 'LOYALTY_TERM', label: 'Termos de Fidelidade' },
            { id: 'EQUIPMENT_COMODATO', label: 'Comodato' },
            { id: 'CUSTOM_TERM', label: 'Personalizados' },
          ].map((cat) => (
            <button
              key={cat.id}
              onClick={() => setSelectedCategory(cat.id)}
              className={`px-3 py-1.5 rounded-lg text-xs font-semibold whitespace-nowrap transition ${
                selectedCategory === cat.id
                  ? 'bg-indigo-600 text-white shadow-sm'
                  : 'bg-white text-slate-600 hover:bg-slate-200 border border-slate-200'
              }`}
            >
              {cat.label}
            </button>
          ))}
        </div>

        <div className="relative w-full md:w-72">
          <FaSearch className="absolute left-3 top-3 text-slate-400 text-sm" />
          <input
            type="text"
            placeholder="Buscar cláusulas ou títulos..."
            value={searchTerm}
            onChange={(e) => setSearchTerm(e.target.value)}
            className="w-full pl-9 pr-4 py-1.5 text-sm bg-white border border-slate-200 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
          />
        </div>
      </div>

      {/* Templates Grid */}
      {loading ? (
        <div className="p-12 text-center text-slate-500">
          <FaSyncAlt className="animate-spin text-3xl mx-auto mb-3 text-indigo-500" />
          Carregando modelos de contrato...
        </div>
      ) : filteredTemplates.length === 0 ? (
        <div className="p-12 bg-white rounded-2xl border border-slate-200 text-center space-y-3">
          <FaFileAlt className="text-4xl text-slate-300 mx-auto" />
          <h3 className="text-lg font-semibold text-slate-700">Nenhum modelo encontrado</h3>
          <p className="text-sm text-slate-500 max-w-md mx-auto">
            Você ainda não possui modelos cadastrados para este filtro. Crie seu primeiro modelo com cláusulas personalizadas.
          </p>
          <button
            onClick={handleOpenCreateModal}
            className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-semibold shadow-sm transition"
          >
            <FaPlus />
            Criar Modelo Agora
          </button>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
          {filteredTemplates.map((template) => (
            <div
              key={template.id}
              className="bg-white rounded-2xl p-5 border border-slate-200 shadow-sm hover:shadow-md transition flex flex-col justify-between"
            >
              <div className="space-y-3">
                <div className="flex items-start justify-between gap-2">
                  <div>{getDocTypeBadge(template.documentType)}</div>
                  <span className="px-2 py-0.5 bg-slate-100 text-slate-600 rounded text-xs font-mono font-medium">
                    v{template.version}
                  </span>
                </div>

                <div>
                  <h3 className="text-base font-bold text-slate-800 line-clamp-1">{template.name}</h3>
                  <p className="text-xs text-slate-500 mt-1 line-clamp-2">
                    {template.contentMarkdown.replace(/[#*`_]/g, '').substring(0, 120)}...
                  </p>
                </div>

                <div className="p-2.5 bg-slate-50 rounded-lg border border-slate-100 text-xs text-slate-600 space-y-1">
                  <div className="flex items-center gap-1.5 font-medium text-slate-700">
                    <FaShieldAlt className="text-indigo-500" />
                    Cláusula de Consentimento Pix:
                  </div>
                  <p className="line-clamp-2 text-slate-500 italic">"{template.consentClause}"</p>
                </div>
              </div>

              <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
                <span>Atualizado em {new Date(template.updatedAt).toLocaleDateString('pt-BR')}</span>

                <div className="flex items-center gap-1">
                  <button
                    onClick={() => handleClone(template.id)}
                    className="p-1.5 hover:bg-slate-100 rounded text-slate-600 hover:text-indigo-600 transition"
                    title="Clonar modelo"
                  >
                    <FaCopy />
                  </button>
                  <button
                    onClick={() => handleOpenEditModal(template)}
                    className="p-1.5 hover:bg-slate-100 rounded text-slate-600 hover:text-blue-600 transition"
                    title="Editar modelo"
                  >
                    <FaEdit />
                  </button>
                  <button
                    onClick={() => handleDelete(template.id)}
                    className="p-1.5 hover:bg-slate-100 rounded text-slate-600 hover:text-rose-600 transition"
                    title="Excluir modelo"
                  >
                    <FaTrash />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Editor & Preview Modal */}
      {isModalOpen && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm overflow-y-auto">
          <div className="bg-white rounded-2xl shadow-2xl border border-slate-200 w-full max-w-6xl max-h-[92vh] flex flex-col my-auto">
            {/* Modal Header */}
            <div className="px-6 py-4 border-b border-slate-200 flex items-center justify-between bg-slate-50 rounded-t-2xl">
              <div>
                <h2 className="text-lg font-bold text-slate-800">
                  {editingTemplate ? `Editar Modelo: ${editingTemplate.name}` : 'Novo Modelo de Contrato / Termo'}
                </h2>
                <p className="text-xs text-slate-500">
                  Defina as cláusulas e use as variáveis dinâmicas para preenchimento automático.
                </p>
              </div>

              <div className="flex items-center gap-2">
                <div className="flex bg-slate-200 p-0.5 rounded-lg text-xs font-semibold">
                  <button
                    type="button"
                    onClick={() => setPreviewTab('editor')}
                    className={`px-3 py-1 rounded-md transition ${previewTab === 'editor' ? 'bg-white shadow text-slate-800' : 'text-slate-600'}`}
                  >
                    <FaCode className="inline mr-1" /> Editor
                  </button>
                  <button
                    type="button"
                    onClick={() => setPreviewTab('preview')}
                    className={`px-3 py-1 rounded-md transition ${previewTab === 'preview' ? 'bg-white shadow text-slate-800' : 'text-slate-600'}`}
                  >
                    <FaEye className="inline mr-1" /> Visualização
                  </button>
                  <button
                    type="button"
                    onClick={() => setPreviewTab('split')}
                    className={`hidden md:block px-3 py-1 rounded-md transition ${previewTab === 'split' ? 'bg-white shadow text-slate-800' : 'text-slate-600'}`}
                  >
                    Lado a Lado
                  </button>
                </div>

                <button
                  onClick={() => setIsModalOpen(false)}
                  className="text-slate-400 hover:text-slate-600 p-1.5 rounded-lg hover:bg-slate-200 transition"
                >
                  <FaTimesCircle className="w-5 h-5" />
                </button>
              </div>
            </div>

            {/* Modal Body */}
            <form onSubmit={handleSave} className="flex-1 overflow-y-auto p-6 space-y-4">
              <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                <div className="md:col-span-2">
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Nome do Modelo *</label>
                  <input
                    type="text"
                    required
                    value={formData.name}
                    onChange={(e) => setFormData({ ...formData, name: e.target.value })}
                    placeholder="Ex: Contrato SCM + SVA Fibra Óptica 2026"
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  />
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 mb-1">Tipo de Documento *</label>
                  <select
                    value={formData.documentType}
                    onChange={(e) => setFormData({ ...formData, documentType: e.target.value as DocumentType })}
                    className="w-full px-3 py-2 text-sm border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                  >
                    <option value="SERVICE_AGREEMENT">Contrato de Prestação de Serviços (SCM/SVA)</option>
                    <option value="LOYALTY_TERM">Termo de Permanência / Fidelidade</option>
                    <option value="EQUIPMENT_COMODATO">Termo de Comodato de Equipamentos</option>
                    <option value="CUSTOM_TERM">Termo Customizado</option>
                  </select>
                </div>
              </div>

              {/* Dynamic Variables Toolbar */}
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-xs font-bold text-slate-700 flex items-center gap-1.5">
                    <FaTag className="text-indigo-600" />
                    Inserir Variáveis Dinâmicas (Clique para adicionar ao texto):
                  </span>
                  <div className="flex gap-1">
                    {['ALL', 'CUSTOMER', 'COMPANY', 'CONTRACT', 'PLAN', 'SIGNATURE'].map((cat) => (
                      <button
                        key={cat}
                        type="button"
                        onClick={() => setActiveVariableCategory(cat)}
                        className={`px-2 py-0.5 text-[10px] font-semibold rounded ${
                          activeVariableCategory === cat
                            ? 'bg-indigo-600 text-white'
                            : 'bg-white text-slate-600 border border-slate-200 hover:bg-slate-100'
                        }`}
                      >
                        {cat === 'ALL' ? 'Todas' : cat}
                      </button>
                    ))}
                  </div>
                </div>

                <div className="flex flex-wrap gap-1.5 max-h-24 overflow-y-auto p-1">
                  {filteredVariables.map((v) => (
                    <button
                      key={v.tag}
                      type="button"
                      onClick={() => handleInsertVariable(v.tag)}
                      title={`${v.description} (Ex: ${v.example})`}
                      className="px-2 py-1 bg-white hover:bg-indigo-50 hover:border-indigo-300 border border-slate-200 rounded text-xs font-mono text-indigo-700 transition flex items-center gap-1 shadow-2xs"
                    >
                      <span className="font-semibold">{v.tag}</span>
                      <span className="text-[10px] text-slate-400 font-sans">({v.label})</span>
                    </button>
                  ))}
                </div>
              </div>

              {/* Split Editor / Preview Area */}
              <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                {(previewTab === 'editor' || previewTab === 'split') && (
                  <div className={previewTab === 'editor' ? 'md:col-span-2' : ''}>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Conteúdo do Contrato (Markdown / Cláusulas) *
                    </label>
                    <textarea
                      ref={textareaRef}
                      required
                      rows={14}
                      value={formData.contentMarkdown}
                      onChange={(e) => setFormData({ ...formData, contentMarkdown: e.target.value })}
                      placeholder="Escreva as cláusulas do contrato em formato Markdown..."
                      className="w-full p-3 font-mono text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none leading-relaxed"
                    />
                  </div>
                )}

                {(previewTab === 'preview' || previewTab === 'split') && (
                  <div className={previewTab === 'preview' ? 'md:col-span-2' : ''}>
                    <label className="block text-xs font-semibold text-slate-700 mb-1">
                      Pré-visualização Formatada (Simulação em Tempo Real)
                    </label>
                    <div className="h-[310px] p-4 bg-slate-50 border border-slate-200 rounded-lg overflow-y-auto prose prose-sm max-w-none text-slate-800">
                      <div className="whitespace-pre-wrap font-sans text-xs leading-relaxed">
                        {previewContent || 'Nenhum conteúdo para exibir no preview.'}
                      </div>
                    </div>
                  </div>
                )}
              </div>

              {/* Consent Clause Input */}
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Cláusula de Consentimento e Aceite Vinculada ao Pix (MP 2.200-2/01) *
                </label>
                <textarea
                  required
                  rows={3}
                  value={formData.consentClause}
                  onChange={(e) => setFormData({ ...formData, consentClause: e.target.value })}
                  placeholder="Texto exibido logo acima do QR Code Pix informando a concordância e assinatura legal..."
                  className="w-full p-3 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:outline-none"
                />
              </div>

              {/* Modal Footer Actions */}
              <div className="pt-4 border-t border-slate-200 flex items-center justify-end gap-3">
                <button
                  type="button"
                  onClick={() => setIsModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:text-slate-800 font-medium"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-semibold shadow-sm transition"
                >
                  Salvar Modelo
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};
