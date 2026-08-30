import React, { useState, useEffect, useMemo, useCallback } from 'react';
import {
  IpamSubnet,
  IpamAsn,
  IpamVrf,
  IpamIpAddress,
  SubnetCalculationResult,
  IpamSplitResponse,
  IpamSubnetCategory,
} from '../../types/ipam';
import { ipamService } from '../../services/ipamService';
import {
  FaNetworkWired,
  FaCalculator,
  FaPlus,
  FaTrash,
  FaSearch,
  FaCheckCircle,
  FaExclamationTriangle,
  FaServer,
  FaLayerGroup,
  FaGlobe,
  FaSyncAlt,
  FaEye,
  FaProjectDiagram,
} from 'react-icons/fa';

export const IpamManager: React.FC = () => {
  const [subnets, setSubnets] = useState<IpamSubnet[]>([]);
  const [asns, setAsns] = useState<IpamAsn[]>([]);
  const [vrfs, setVrfs] = useState<IpamVrf[]>([]);
  const [loading, setLoading] = useState(true);
  const [activeTab, setActiveTab] = useState<'subnets' | 'calculator' | 'resources'>('subnets');

  // Search & Filters
  const [searchTerm, setSearchTerm] = useState('');
  const [versionFilter, setVersionFilter] = useState<'ALL' | 'IPV4' | 'IPV6'>('ALL');
  const [categoryFilter, setCategoryFilter] = useState<string>('ALL');

  // Modals
  const [isSubnetModalOpen, setIsSubnetModalOpen] = useState(false);
  const [isSplitModalOpen, setIsSplitModalOpen] = useState(false);
  const [isIpModalOpen, setIsIpModalOpen] = useState(false);
  const [isAsnModalOpen, setIsAsnModalOpen] = useState(false);
  const [isVrfModalOpen, setIsVrfModalOpen] = useState(false);

  // Selected for Actions
  const [selectedSubnet, setSelectedSubnet] = useState<IpamSubnet | null>(null);
  const [subnetIps, setSubnetIps] = useState<IpamIpAddress[]>([]);
  const [loadingIps, setLoadingIps] = useState(false);
  const [nextAvailableIp, setNextAvailableIp] = useState<string>('');

  // Subnet Form
  const [subnetForm, setSubnetForm] = useState({
    cidr: '',
    vrfId: '',
    asnId: '',
    category: 'CUSTOMER_ACCESS' as IpamSubnetCategory,
    isPool: false,
    poolName: '',
    description: '',
  });

  // Split Form
  const [targetPrefix, setTargetPrefix] = useState<number>(28);
  const [splitPreview, setSplitPreview] = useState<IpamSplitResponse | null>(null);
  const [splitSubmitting, setSplitSubmitting] = useState(false);

  // Calculator Form
  const [calcInput, setCalcInput] = useState('200.150.10.0/24');
  const [calcResult, setCalcResult] = useState<SubnetCalculationResult | null>(null);
  const [calcLoading, setCalcLoading] = useState(false);

  // ASN / VRF Forms
  const [asnForm, setAsnForm] = useState({ asn: 265000, name: '', description: '' });
  const [vrfForm, setVrfForm] = useState({ name: '', rd: '', description: '', isDefault: false });

  // Notifications / Feedback
  const [notification, setNotification] = useState<{ message: string; type: 'success' | 'error' } | null>(null);

  const showNotification = (message: string, type: 'success' | 'error' = 'success') => {
    setNotification({ message, type });
    setTimeout(() => setNotification(null), 4000);
  };

  const loadData = useCallback(async () => {
    try {
      setLoading(true);
      const [subnetsData, asnsData, vrfsData] = await Promise.all([
        ipamService.getSubnets(),
        ipamService.getAsns(),
        ipamService.getVrfs(),
      ]);
      setSubnets(subnetsData);
      setAsns(asnsData);
      setVrfs(vrfsData);
    } catch (err: any) {
      showNotification('Erro ao carregar dados do IPAM: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoading(false);
    }
  }, []);

  useEffect(() => {
    loadData();
  }, [loadData]);

  // Calculator Runner
  const handleCalculate = async (inputVal: string) => {
    if (!inputVal) return;
    try {
      setCalcLoading(true);
      const result = await ipamService.calculateCidr(inputVal);
      setCalcResult(result);
    } catch (err: any) {
      showNotification('CIDR inválido: ' + (err.response?.data?.detail || err.message), 'error');
      setCalcResult(null);
    } finally {
      setCalcLoading(false);
    }
  };

  useEffect(() => {
    if (activeTab === 'calculator') {
      handleCalculate(calcInput);
    }
  }, [activeTab]);

  // Filtered Subnets
  const filteredSubnets = useMemo(() => {
    return subnets.filter((s) => {
      const matchSearch =
        s.cidr.toLowerCase().includes(searchTerm.toLowerCase()) ||
        s.category.toLowerCase().includes(searchTerm.toLowerCase()) ||
        (s.description && s.description.toLowerCase().includes(searchTerm.toLowerCase())) ||
        (s.vrfName && s.vrfName.toLowerCase().includes(searchTerm.toLowerCase()));

      const matchVersion = versionFilter === 'ALL' || s.ipVersion === versionFilter;
      const matchCategory = categoryFilter === 'ALL' || s.category === categoryFilter;

      return matchSearch && matchVersion && matchCategory;
    });
  }, [subnets, searchTerm, versionFilter, categoryFilter]);

  // Summary Metrics
  const metrics = useMemo(() => {
    const totalV4 = subnets.filter((s) => s.ipVersion === 'IPV4').length;
    const totalV6 = subnets.filter((s) => s.ipVersion === 'IPV6').length;
    const totalAllocatedIps = subnets.reduce((acc, s) => acc + (s.allocatedHosts || 0), 0);
    const avgUtil =
      subnets.length > 0
        ? Math.round(subnets.reduce((acc, s) => acc + (s.utilizationPercentage || 0), 0) / subnets.length)
        : 0;

    return { totalV4, totalV6, totalAllocatedIps, avgUtil };
  }, [subnets]);

  // Create Subnet
  const handleCreateSubnet = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ipamService.createSubnet({
        cidr: subnetForm.cidr,
        vrfId: subnetForm.vrfId || undefined,
        asnId: subnetForm.asnId || undefined,
        category: subnetForm.category,
        isPool: subnetForm.isPool,
        poolName: subnetForm.isPool ? subnetForm.poolName : undefined,
        description: subnetForm.description || undefined,
      });
      showNotification('Sub-rede criada com sucesso!');
      setIsSubnetModalOpen(false);
      setSubnetForm({
        cidr: '',
        vrfId: '',
        asnId: '',
        category: 'CUSTOMER_ACCESS',
        isPool: false,
        poolName: '',
        description: '',
      });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao criar sub-rede: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Delete Subnet
  const handleDeleteSubnet = async (id: string, cidr: string) => {
    if (!window.confirm(`Tem certeza que deseja excluir a sub-rede ${cidr}?`)) return;
    try {
      await ipamService.deleteSubnet(id);
      showNotification(`Sub-rede ${cidr} excluída com sucesso.`);
      loadData();
    } catch (err: any) {
      showNotification('Erro ao excluir sub-rede: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Open Split Modal
  const openSplitModal = (subnet: IpamSubnet) => {
    setSelectedSubnet(subnet);
    const initialTarget = subnet.prefixLength + (subnet.ipVersion === 'IPV4' ? 4 : 8);
    setTargetPrefix(initialTarget <= (subnet.ipVersion === 'IPV4' ? 32 : 128) ? initialTarget : subnet.prefixLength + 1);
    setSplitPreview(null);
    setIsSplitModalOpen(true);
  };

  // Execute Split Preview / Confirmation
  const handlePreviewSplit = async (create: boolean = false) => {
    if (!selectedSubnet) return;
    try {
      setSplitSubmitting(true);
      const res = await ipamService.splitSubnet({
        subnetId: selectedSubnet.id,
        targetPrefixLength: targetPrefix,
        createSubnets: create,
      });
      setSplitPreview(res);
      if (create) {
        showNotification(`Split executado! ${res.totalSubnetsGenerated} sub-redes criadas.`);
        setIsSplitModalOpen(false);
        loadData();
      }
    } catch (err: any) {
      showNotification('Erro ao executar split: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setSplitSubmitting(false);
    }
  };

  // Open IPs / Matrix Modal
  const openIpDetails = async (subnet: IpamSubnet) => {
    setSelectedSubnet(subnet);
    setIsIpModalOpen(true);
    try {
      setLoadingIps(true);
      const [ips, nextIp] = await Promise.all([
        ipamService.getIpsBySubnet(subnet.id),
        ipamService.getNextAvailableIp(subnet.id),
      ]);
      setSubnetIps(ips);
      setNextAvailableIp(nextIp);
    } catch (err: any) {
      showNotification('Erro ao carregar IPs da sub-rede: ' + (err.response?.data?.detail || err.message), 'error');
    } finally {
      setLoadingIps(false);
    }
  };

  // Create ASN
  const handleCreateAsn = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ipamService.createAsn({
        asn: Number(asnForm.asn),
        name: asnForm.name,
        rir: 'REGISTRO_BR',
        description: asnForm.description,
      });
      showNotification('ASN cadastrado com sucesso!');
      setIsAsnModalOpen(false);
      setAsnForm({ asn: 265000, name: '', description: '' });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao cadastrar ASN: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  // Create VRF
  const handleCreateVrf = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await ipamService.createVrf({
        name: vrfForm.name,
        rd: vrfForm.rd || undefined,
        description: vrfForm.description || undefined,
        isDefault: vrfForm.isDefault,
      });
      showNotification('VRF criada com sucesso!');
      setIsVrfModalOpen(false);
      setVrfForm({ name: '', rd: '', description: '', isDefault: false });
      loadData();
    } catch (err: any) {
      showNotification('Erro ao criar VRF: ' + (err.response?.data?.detail || err.message), 'error');
    }
  };

  return (
    <div className="p-6 max-w-7xl mx-auto space-y-6">
      {/* Header */}
      <div className="flex flex-col md:flex-row md:items-center justify-between gap-4 bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm">
        <div>
          <div className="flex items-center gap-2">
            <FaNetworkWired className="w-8 h-8 text-indigo-600 dark:text-indigo-400" />
            <h1 className="text-2xl font-bold text-slate-900 dark:text-white">IPAM - Gestão de Endereçamento IP</h1>
          </div>
          <p className="text-sm text-slate-500 dark:text-slate-400 mt-1">
            Inventário corporativo de recursos de numeração, ASN, VRF, subnets IPv4/IPv6 e cálculo VLSM
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
            onClick={() => setIsSubnetModalOpen(true)}
            className="flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg font-medium shadow-sm transition"
          >
            <FaPlus className="w-4 h-4" /> Nova Sub-rede
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
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-blue-100 dark:bg-blue-900/40 text-blue-600 dark:text-blue-400 rounded-lg">
            <FaNetworkWired className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Subnets IPv4</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{metrics.totalV4}</h3>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-purple-100 dark:bg-purple-900/40 text-purple-600 dark:text-purple-400 rounded-lg">
            <FaGlobe className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Prefixos IPv6</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{metrics.totalV6}</h3>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-emerald-100 dark:bg-emerald-900/40 text-emerald-600 dark:text-emerald-400 rounded-lg">
            <FaGlobe className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">IPs Alocados</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{metrics.totalAllocatedIps}</h3>
          </div>
        </div>

        <div className="bg-white dark:bg-slate-800 p-5 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm flex items-center gap-4">
          <div className="p-3 bg-amber-100 dark:bg-amber-900/40 text-amber-600 dark:text-amber-400 rounded-lg">
            <FaLayerGroup className="w-6 h-6" />
          </div>
          <div>
            <p className="text-xs font-semibold text-slate-500 uppercase tracking-wider">Ocupação Média</p>
            <h3 className="text-2xl font-bold text-slate-900 dark:text-white mt-1">{metrics.avgUtil}%</h3>
          </div>
        </div>
      </div>

      {/* Tabs */}
      <div className="flex border-b border-slate-200 dark:border-slate-700 gap-6">
        <button
          onClick={() => setActiveTab('subnets')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'subnets'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaNetworkWired className="w-4 h-4" /> Subnets & Prefixos
        </button>

        <button
          onClick={() => setActiveTab('calculator')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'calculator'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaCalculator className="w-4 h-4" /> Calculadora & Simulador VLSM
        </button>

        <button
          onClick={() => setActiveTab('resources')}
          className={`pb-3 text-sm font-semibold flex items-center gap-2 border-b-2 transition ${
            activeTab === 'resources'
              ? 'border-indigo-600 text-indigo-600 dark:text-indigo-400 dark:border-indigo-400'
              : 'border-transparent text-slate-500 hover:text-slate-700 dark:text-slate-400'
          }`}
        >
          <FaServer className="w-4 h-4" /> ASNs & VRFs ({asns.length}/{vrfs.length})
        </button>
      </div>

      {/* TAB 1: Subnets */}
      {activeTab === 'subnets' && (
        <div className="space-y-4">
          {/* Filter Bar */}
          <div className="flex flex-col md:flex-row gap-3 justify-between bg-white dark:bg-slate-800 p-4 rounded-xl border border-slate-200 dark:border-slate-700">
            <div className="relative flex-1">
              <FaSearch className="w-4 h-4 absolute left-3 top-3 text-slate-400" />
              <input
                type="text"
                placeholder="Buscar por CIDR, descrição, VRF..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="w-full pl-9 pr-4 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg focus:outline-none focus:ring-2 focus:ring-indigo-500"
              />
            </div>

            <div className="flex items-center gap-3">
              <select
                value={versionFilter}
                onChange={(e: any) => setVersionFilter(e.target.value)}
                className="text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg px-3 py-2"
              >
                <option value="ALL">Todas as Versões</option>
                <option value="IPV4">Apenas IPv4</option>
                <option value="IPV6">Apenas IPv6</option>
              </select>

              <select
                value={categoryFilter}
                onChange={(e) => setCategoryFilter(e.target.value)}
                className="text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg px-3 py-2"
              >
                <option value="ALL">Todas as Categorias</option>
                <option value="CUSTOMER_ACCESS">Acesso Assinantes</option>
                <option value="CGNAT">CGNAT</option>
                <option value="MANAGEMENT">Gerência / OLTs</option>
                <option value="INFRASTRUCTURE">Infraestrutura</option>
                <option value="PTP">Ponto-a-Ponto (PTP)</option>
                <option value="LOOPBACK">Loopback</option>
              </select>
            </div>
          </div>

          {/* Subnets Table */}
          <div className="bg-white dark:bg-slate-800 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm overflow-hidden">
            {loading ? (
              <div className="p-8 text-center text-slate-500">Carregando sub-redes...</div>
            ) : filteredSubnets.length === 0 ? (
              <div className="p-8 text-center text-slate-500">Nenhuma sub-rede encontrada.</div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-left text-sm text-slate-600 dark:text-slate-300">
                  <thead className="bg-slate-50 dark:bg-slate-900/50 text-xs uppercase font-semibold text-slate-500 border-b border-slate-200 dark:border-slate-700">
                    <tr>
                      <th className="px-6 py-4">CIDR / Prefixo</th>
                      <th className="px-6 py-4">Categoria / VRF</th>
                      <th className="px-6 py-4">Faixa de Hosts</th>
                      <th className="px-6 py-4">Ocupação</th>
                      <th className="px-6 py-4 text-right">Ações</th>
                    </tr>
                  </thead>
                  <tbody className="divide-y divide-slate-100 dark:divide-slate-700">
                    {filteredSubnets.map((sub) => {
                      const util = sub.utilizationPercentage || 0;
                      const utilColor =
                        util > 90
                          ? 'bg-rose-500'
                          : util > 70
                          ? 'bg-amber-500'
                          : 'bg-emerald-500';

                      return (
                        <tr key={sub.id} className="hover:bg-slate-50 dark:hover:bg-slate-750 transition">
                          <td className="px-6 py-4">
                            <div className="flex items-center gap-2">
                              <span
                                className={`px-2 py-0.5 text-xs font-bold rounded ${
                                  sub.ipVersion === 'IPV4'
                                    ? 'bg-blue-100 text-blue-800 dark:bg-blue-900/50 dark:text-blue-300'
                                    : 'bg-purple-100 text-purple-800 dark:bg-purple-900/50 dark:text-purple-300'
                                }`}
                              >
                                {sub.ipVersion}
                              </span>
                              <span className="font-mono font-bold text-slate-900 dark:text-white text-base">
                                {sub.cidr}
                              </span>
                            </div>
                            {sub.description && (
                              <p className="text-xs text-slate-400 mt-1">{sub.description}</p>
                            )}
                          </td>

                          <td className="px-6 py-4">
                            <span className="inline-flex items-center px-2.5 py-0.5 rounded-full text-xs font-medium bg-slate-100 dark:bg-slate-700 text-slate-800 dark:text-slate-200">
                              {sub.category}
                            </span>
                            {sub.vrfName && (
                              <p className="text-xs text-indigo-500 font-medium mt-1">VRF: {sub.vrfName}</p>
                            )}
                          </td>

                          <td className="px-6 py-4 font-mono text-xs">
                            <div>Rede: {sub.networkAddress}</div>
                            {sub.broadcastAddress && <div>BCast: {sub.broadcastAddress}</div>}
                            <div className="text-slate-400 font-sans mt-0.5">
                              Total: {sub.totalHosts.toLocaleString()} hosts
                            </div>
                          </td>

                          <td className="px-6 py-4">
                            <div className="w-36">
                              <div className="flex justify-between text-xs mb-1 font-medium">
                                <span>{sub.allocatedHosts || 0} alocados</span>
                                <span>{util}%</span>
                              </div>
                              <div className="w-full bg-slate-200 dark:bg-slate-700 h-2 rounded-full overflow-hidden">
                                <div
                                  className={`h-full ${utilColor} transition-all duration-300`}
                                  style={{ width: `${Math.max(4, util)}%` }}
                                />
                              </div>
                            </div>
                          </td>

                          <td className="px-6 py-4 text-right space-x-2">
                            <button
                              onClick={() => openIpDetails(sub)}
                              className="px-2.5 py-1.5 bg-slate-100 hover:bg-slate-200 dark:bg-slate-700 dark:hover:bg-slate-600 rounded text-xs font-medium text-slate-700 dark:text-slate-200 inline-flex items-center gap-1 transition"
                              title="Visualizar IPs"
                            >
                              <FaEye className="w-3.5 h-3.5" /> IPs
                            </button>

                            <button
                              onClick={() => openSplitModal(sub)}
                              className="px-2.5 py-1.5 bg-indigo-50 hover:bg-indigo-100 dark:bg-indigo-900/40 dark:hover:bg-indigo-900/60 rounded text-xs font-medium text-indigo-700 dark:text-indigo-300 inline-flex items-center gap-1 transition"
                              title="Dividir Sub-rede (Split)"
                            >
                              <FaProjectDiagram className="w-3.5 h-3.5" /> Split
                            </button>

                            <button
                              onClick={() => handleDeleteSubnet(sub.id, sub.cidr)}
                              className="p-1.5 text-rose-500 hover:bg-rose-50 dark:hover:bg-rose-900/30 rounded transition"
                              title="Excluir"
                            >
                              <FaTrash className="w-4 h-4" />
                            </button>
                          </td>
                        </tr>
                      );
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>
        </div>
      )}

      {/* TAB 2: Calculator */}
      {activeTab === 'calculator' && (
        <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-6">
          <div>
            <h2 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <FaCalculator className="w-5 h-5 text-indigo-600" /> Calculadora Matemática de Sub-redes (IPv4 / IPv6)
            </h2>
            <p className="text-sm text-slate-500">
              Digite qualquer CIDR para visualizar gateway, broadcast, máscaras e faixas utilizáveis
            </p>
          </div>

          <div className="flex gap-3 max-w-xl">
            <input
              type="text"
              value={calcInput}
              onChange={(e) => setCalcInput(e.target.value)}
              placeholder="Ex: 200.150.10.0/24 ou 2804:192c:100::/40"
              className="flex-1 px-4 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
            />
            <button
              onClick={() => handleCalculate(calcInput)}
              disabled={calcLoading}
              className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
            >
              {calcLoading ? 'Calculando...' : 'Calcular'}
            </button>
          </div>

          {calcResult && (
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4 pt-4 border-t border-slate-200 dark:border-slate-700">
              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="text-xs font-semibold text-slate-500 uppercase">Endereço de Rede</span>
                <p className="font-mono font-bold text-slate-900 dark:text-white text-lg mt-1">
                  {calcResult.networkAddress}
                </p>
              </div>

              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="text-xs font-semibold text-slate-500 uppercase">Broadcast</span>
                <p className="font-mono font-bold text-slate-900 dark:text-white text-lg mt-1">
                  {calcResult.broadcastAddress || 'N/A (IPv6)'}
                </p>
              </div>

              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="text-xs font-semibold text-slate-500 uppercase">Máscara de Rede</span>
                <p className="font-mono font-bold text-slate-900 dark:text-white text-lg mt-1">
                  {calcResult.netmask}
                </p>
              </div>

              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="text-xs font-semibold text-slate-500 uppercase">Primeiro IP Útil</span>
                <p className="font-mono font-bold text-emerald-600 dark:text-emerald-400 text-lg mt-1">
                  {calcResult.firstUsableIp}
                </p>
              </div>

              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="text-xs font-semibold text-slate-500 uppercase">Último IP Útil</span>
                <p className="font-mono font-bold text-emerald-600 dark:text-emerald-400 text-lg mt-1">
                  {calcResult.lastUsableIp}
                </p>
              </div>

              <div className="p-4 bg-slate-50 dark:bg-slate-900/50 rounded-lg border border-slate-200 dark:border-slate-700">
                <span className="text-xs font-semibold text-slate-500 uppercase">Total de Hosts Úteis</span>
                <p className="font-mono font-bold text-indigo-600 dark:text-indigo-400 text-lg mt-1">
                  {calcResult.usableHosts.toLocaleString()}
                </p>
              </div>
            </div>
          )}
        </div>
      )}

      {/* TAB 3: Resources (ASNs & VRFs) */}
      {activeTab === 'resources' && (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* ASNs */}
          <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FaGlobe className="w-5 h-5 text-indigo-500" /> ASNs do Provedor
              </h3>
              <button
                onClick={() => setIsAsnModalOpen(true)}
                className="px-3 py-1.5 bg-indigo-50 dark:bg-indigo-900/40 text-indigo-600 dark:text-indigo-400 rounded-lg text-xs font-medium hover:bg-indigo-100 transition"
              >
                + Novo ASN
              </button>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {asns.map((a) => (
                <div key={a.id} className="py-3 flex justify-between items-center">
                  <div>
                    <span className="font-mono font-bold text-slate-900 dark:text-white">AS{a.asn}</span>
                    <span className="ml-2 text-sm text-slate-600 dark:text-slate-300">{a.name}</span>
                    {a.description && <p className="text-xs text-slate-400">{a.description}</p>}
                  </div>
                  <span className="px-2 py-0.5 text-xs bg-slate-100 dark:bg-slate-700 text-slate-700 dark:text-slate-300 rounded">
                    {a.rir}
                  </span>
                </div>
              ))}
            </div>
          </div>

          {/* VRFs */}
          <div className="bg-white dark:bg-slate-800 p-6 rounded-xl border border-slate-200 dark:border-slate-700 shadow-sm space-y-4">
            <div className="flex justify-between items-center">
              <h3 className="font-bold text-slate-900 dark:text-white flex items-center gap-2">
                <FaLayerGroup className="w-5 h-5 text-purple-500" /> VRFs / Tabelas de Roteamento
              </h3>
              <button
                onClick={() => setIsVrfModalOpen(true)}
                className="px-3 py-1.5 bg-purple-50 dark:bg-purple-900/40 text-purple-600 dark:text-purple-400 rounded-lg text-xs font-medium hover:bg-purple-100 transition"
              >
                + Nova VRF
              </button>
            </div>

            <div className="divide-y divide-slate-100 dark:divide-slate-700">
              {vrfs.map((v) => (
                <div key={v.id} className="py-3 flex justify-between items-center">
                  <div>
                    <span className="font-bold text-slate-900 dark:text-white">{v.name}</span>
                    {v.rd && <span className="ml-2 font-mono text-xs text-slate-400">(RD: {v.rd})</span>}
                    {v.description && <p className="text-xs text-slate-400">{v.description}</p>}
                  </div>
                  {v.isDefault && (
                    <span className="px-2 py-0.5 text-xs bg-emerald-100 text-emerald-800 dark:bg-emerald-900/40 dark:text-emerald-300 rounded font-medium">
                      Padrão
                    </span>
                  )}
                </div>
              ))}
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Nova Sub-rede */}
      {isSubnetModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">Cadastrar Nova Sub-rede</h3>
            <form onSubmit={handleCreateSubnet} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  CIDR (IPv4 ou IPv6) *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: 200.150.10.0/24 ou 100.64.0.0/16 ou 2804:192c::/32"
                  value={subnetForm.cidr}
                  onChange={(e) => setSubnetForm({ ...subnetForm, cidr: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono focus:ring-2 focus:ring-indigo-500"
                />
              </div>

              <div className="grid grid-cols-2 gap-4">
                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                    Categoria
                  </label>
                  <select
                    value={subnetForm.category}
                    onChange={(e: any) => setSubnetForm({ ...subnetForm, category: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="CUSTOMER_ACCESS">Acesso Assinantes</option>
                    <option value="CGNAT">CGNAT</option>
                    <option value="MANAGEMENT">Gerência / OLTs</option>
                    <option value="INFRASTRUCTURE">Infraestrutura</option>
                    <option value="PTP">Ponto-a-Ponto</option>
                    <option value="LOOPBACK">Loopback</option>
                  </select>
                </div>

                <div>
                  <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">VRF</label>
                  <select
                    value={subnetForm.vrfId}
                    onChange={(e) => setSubnetForm({ ...subnetForm, vrfId: e.target.value })}
                    className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                  >
                    <option value="">Nenhuma (Global)</option>
                    {vrfs.map((v) => (
                      <option key={v.id} value={v.id}>
                        {v.name}
                      </option>
                    ))}
                  </select>
                </div>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">Descrição</label>
                <input
                  type="text"
                  placeholder="Ex: Bloco público BNG Centro"
                  value={subnetForm.description}
                  onChange={(e) => setSubnetForm({ ...subnetForm, description: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
                <button
                  type="button"
                  onClick={() => setIsSubnetModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded-lg transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
                >
                  Salvar Sub-rede
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Split / Divisão de Sub-rede */}
      {isSplitModalOpen && selectedSubnet && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-2xl w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white flex items-center gap-2">
              <FaProjectDiagram className="w-5 h-5 text-indigo-600" /> Dividir Sub-rede (Split VLSM)
            </h3>
            <p className="text-xs text-slate-500">
              Bloco Pai: <span className="font-mono font-bold text-indigo-600">{selectedSubnet.cidr}</span> ({selectedSubnet.totalHosts} hosts)
            </p>

            <div className="flex items-center gap-4 bg-slate-50 dark:bg-slate-900 p-4 rounded-xl">
              <label className="text-sm font-semibold text-slate-700 dark:text-slate-300">
                Prefixo Alvo (/X):
              </label>
              <input
                type="number"
                min={selectedSubnet.prefixLength + 1}
                max={selectedSubnet.ipVersion === 'IPV4' ? 32 : 128}
                value={targetPrefix}
                onChange={(e) => setTargetPrefix(Number(e.target.value))}
                className="w-24 px-3 py-1.5 text-sm bg-white dark:bg-slate-800 border border-slate-200 dark:border-slate-700 rounded-lg font-bold"
              />
              <button
                type="button"
                onClick={() => handlePreviewSplit(false)}
                disabled={splitSubmitting}
                className="px-4 py-1.5 bg-slate-200 dark:bg-slate-700 hover:bg-slate-300 text-xs font-semibold rounded-lg transition"
              >
                Pré-visualizar Divisão
              </button>
            </div>

            {splitPreview && (
              <div className="space-y-2">
                <div className="flex justify-between items-center text-xs font-semibold text-slate-600 dark:text-slate-300">
                  <span>Total de Sub-redes geradas: {splitPreview.totalSubnetsGenerated}</span>
                </div>
                <div className="max-h-60 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-700 bg-slate-50 dark:bg-slate-900/50 rounded-lg p-2 font-mono text-xs">
                  {splitPreview.generatedSubnets.slice(0, 100).map((sub, idx) => (
                    <div key={idx} className="py-1.5 flex justify-between items-center px-2">
                      <span className="font-bold text-indigo-600 dark:text-indigo-400">{sub.cidr}</span>
                      <span className="text-slate-500">Úteis: {sub.firstUsableIp} - {sub.lastUsableIp} ({sub.usableHosts} hosts)</span>
                    </div>
                  ))}
                  {splitPreview.totalSubnetsGenerated > 100 && (
                    <div className="py-2 text-center text-slate-400 text-xs font-sans">
                      ... e mais {splitPreview.totalSubnetsGenerated - 100} sub-redes
                    </div>
                  )}
                </div>
              </div>
            )}

            <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
              <button
                type="button"
                onClick={() => setIsSplitModalOpen(false)}
                className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 dark:text-slate-300 dark:hover:bg-slate-700 rounded-lg transition"
              >
                Cancelar
              </button>
              <button
                type="button"
                onClick={() => handlePreviewSplit(true)}
                disabled={splitSubmitting}
                className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
              >
                {splitSubmitting ? 'Salvando...' : 'Salvar Sub-redes no Banco'}
              </button>
            </div>
          </div>
        </div>
      )}

      {/* MODAL: Detalhes de IPs / Matrix */}
      {isIpModalOpen && selectedSubnet && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-3xl w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <div className="flex justify-between items-center">
              <div>
                <h3 className="text-lg font-bold text-slate-900 dark:text-white">
                  Inventário de IPs da Sub-rede {selectedSubnet.cidr}
                </h3>
                <p className="text-xs text-slate-500">
                  Próximo IP Livre: <span className="font-mono font-bold text-emerald-600">{nextAvailableIp || 'Nenhum'}</span>
                </p>
              </div>
              <button
                onClick={() => setIsIpModalOpen(false)}
                className="p-1 text-slate-400 hover:text-slate-600"
              >
                ✕
              </button>
            </div>

            {loadingIps ? (
              <div className="p-8 text-center text-slate-500">Carregando IPs...</div>
            ) : subnetIps.length === 0 ? (
              <div className="p-8 text-center text-slate-500 bg-slate-50 dark:bg-slate-900/50 rounded-xl">
                Nenhum IP alocado individualmente ainda nesta sub-rede. Todos os hosts estão disponíveis.
              </div>
            ) : (
              <div className="max-h-80 overflow-y-auto divide-y divide-slate-100 dark:divide-slate-700">
                {subnetIps.map((ip) => (
                  <div key={ip.id} className="py-2 flex justify-between items-center text-sm">
                    <span className="font-mono font-bold text-slate-900 dark:text-white">{ip.ipAddress}</span>
                    <span className="px-2 py-0.5 text-xs bg-blue-100 text-blue-800 rounded">
                      {ip.assignedToLabel || ip.status}
                    </span>
                  </div>
                ))}
              </div>
            )}
          </div>
        </div>
      )}

      {/* MODAL: Novo ASN */}
      {isAsnModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">Cadastrar ASN</h3>
            <form onSubmit={handleCreateAsn} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Número do ASN *
                </label>
                <input
                  type="number"
                  required
                  value={asnForm.asn}
                  onChange={(e) => setAsnForm({ ...asnForm, asn: Number(e.target.value) })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono font-bold"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Nome do Titular / Razão Social *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: ISP Telecom Brasil Ltda"
                  value={asnForm.name}
                  onChange={(e) => setAsnForm({ ...asnForm, name: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
                <button
                  type="button"
                  onClick={() => setIsAsnModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-sm font-medium transition"
                >
                  Salvar ASN
                </button>
              </div>
            </form>
          </div>
        </div>
      )}

      {/* MODAL: Nova VRF */}
      {isVrfModalOpen && (
        <div className="fixed inset-0 bg-black/50 backdrop-blur-sm z-50 flex items-center justify-center p-4">
          <div className="bg-white dark:bg-slate-800 rounded-2xl max-w-md w-full p-6 shadow-xl border border-slate-200 dark:border-slate-700 space-y-4">
            <h3 className="text-lg font-bold text-slate-900 dark:text-white">Criar Nova VRF</h3>
            <form onSubmit={handleCreateVrf} className="space-y-4">
              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Nome da VRF *
                </label>
                <input
                  type="text"
                  required
                  placeholder="Ex: CGNAT-01 ou GERENCIA-OLT"
                  value={vrfForm.name}
                  onChange={(e) => setVrfForm({ ...vrfForm, name: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-bold"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 dark:text-slate-300 mb-1">
                  Route Distinguisher (RD)
                </label>
                <input
                  type="text"
                  placeholder="Ex: 65000:1"
                  value={vrfForm.rd}
                  onChange={(e) => setVrfForm({ ...vrfForm, rd: e.target.value })}
                  className="w-full px-3 py-2 text-sm bg-slate-50 dark:bg-slate-900 border border-slate-200 dark:border-slate-700 rounded-lg font-mono"
                />
              </div>

              <div className="flex justify-end gap-3 pt-4 border-t border-slate-100 dark:border-slate-700">
                <button
                  type="button"
                  onClick={() => setIsVrfModalOpen(false)}
                  className="px-4 py-2 text-sm text-slate-600 hover:bg-slate-100 rounded-lg transition"
                >
                  Cancelar
                </button>
                <button
                  type="submit"
                  className="px-5 py-2 bg-purple-600 hover:bg-purple-700 text-white rounded-lg text-sm font-medium transition"
                >
                  Salvar VRF
                </button>
              </div>
            </form>
          </div>
        </div>
      )}
    </div>
  );
};

export default IpamManager;
