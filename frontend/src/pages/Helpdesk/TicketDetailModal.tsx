import React, { useState, useEffect } from 'react';
import {
  Dialog,
  DialogTitle,
  DialogContent,
  DialogActions,
  Box,
  Typography,
  Chip,
  Button,
  TextField,
  FormControlLabel,
  Switch,
  Divider,
  Paper,
  Rating,
  Alert,
  CircularProgress
} from '@mui/material';
import {
  Send as SendIcon,
  Build as BuildIcon,
  CheckCircle as CheckCircleIcon,
  ArrowUpward as EscalateIcon,
  Lock as LockIcon
} from '@mui/icons-material';
import { helpdeskService } from '../../services/helpdeskService';
import { toast } from 'react-toastify';

const TicketDetailModal = ({ open, onClose, ticket, onUpdated, currentUserRole = 'ATTENDANT', currentUserId = null, currentUserName = 'Atendente' }) => {
  const [interactions, setInteractions] = useState([]);
  const [loading, setLoading] = useState(false);
  const [newMessage, setNewMessage] = useState('');
  const [isInternal, setIsInternal] = useState(false);
  const [submitting, setSubmitting] = useState(false);

  // Modals de Ação
  const [escalateModalOpen, setEscalateModalOpen] = useState(false);
  const [escalateReason, setEscalateReason] = useState('');
  const [resolveModalOpen, setResolveModalOpen] = useState(false);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [woModalOpen, setWoModalOpen] = useState(false);
  const [woReason, setWoReason] = useState('');
  const [closeModalOpen, setCloseModalOpen] = useState(false);
  const [rating, setRating] = useState(5);
  const [closeNotes, setCloseNotes] = useState('');

  const loadInteractions = async () => {
    if (!ticket) return;
    try {
      setLoading(true);
      const res: any = await helpdeskService.getInteractions(ticket.id);
      setInteractions(Array.isArray(res) ? res : (res?.data || []));
    } catch (err) {
      console.error('Erro ao carregar interações:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    if (open && ticket) {
      loadInteractions();
    }
  }, [open, ticket]);

  const handleSendMessage = async () => {
    if (!newMessage.trim()) return;
    try {
      setSubmitting(true);
      await helpdeskService.addInteraction(ticket.id, {
        userId: currentUserId,
        senderType: currentUserRole === 'SUPPORT_N2' ? 'SUPPORT_N2' : 'ATTENDANT',
        senderName: currentUserName,
        message: newMessage,
        isInternalNote: isInternal,
      } as any);
      setNewMessage('');
      loadInteractions();
      toast.success(isInternal ? 'Nota interna salva.' : 'Mensagem enviada.');
    } catch (err) {
      toast.error('Erro ao enviar mensagem.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEscalateToN2 = async () => {
    if (!escalateReason.trim()) {
      toast.warning('Informe o motivo do escalonamento.');
      return;
    }
    try {
      setSubmitting(true);
      await helpdeskService.escalateToN2(ticket.id, escalateReason);
      toast.success('Chamado escalonado para o Suporte Nível 2 em tempo real!');
      setEscalateModalOpen(false);
      setEscalateReason('');
      onUpdated();
      onClose();
    } catch (err) {
      toast.error('Erro ao escalonar chamado.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleResolveByN2 = async () => {
    if (!resolutionNotes.trim()) {
      toast.warning('Informe as notas de resolução.');
      return;
    }
    try {
      setSubmitting(true);
      await helpdeskService.resolveByN2(ticket.id, resolutionNotes);
      toast.success('Chamado marcado como RESOLVIDO pelo Suporte N2!');
      setResolveModalOpen(false);
      setResolutionNotes('');
      onUpdated();
      onClose();
    } catch (err) {
      toast.error('Erro ao resolver chamado.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleEscalateToWorkOrder = async () => {
    if (!woReason.trim()) {
      toast.warning('Informe a justificativa técnica para abertura da O.S.');
      return;
    }
    try {
      setSubmitting(true);
      const res: any = await helpdeskService.escalateToWorkOrder(ticket.id, {
        n2UserId: currentUserId,
        n2Name: currentUserName,
        technicalReason: woReason,
      });
      const woResult = res?.data || res;
      toast.success(`Ordem de Serviço #${woResult?.id?.substring(0, 8)} gerada para a equipe de campo!`);
      setWoModalOpen(false);
      setWoReason('');
      onUpdated();
      onClose();
    } catch (err) {
      toast.error('Erro ao gerar Ordem de Serviço.');
    } finally {
      setSubmitting(false);
    }
  };

  const handleCloseTicket = async () => {
    try {
      setSubmitting(true);
      await helpdeskService.closeTicket(ticket.id);
      toast.success('Chamado encerrado com registro regulatório ANATEL!');
      setCloseModalOpen(false);
      onUpdated();
      onClose();
    } catch (err) {
      toast.error('Erro ao encerrar chamado.');
    } finally {
      setSubmitting(false);
    }
  };

  if (!ticket) return null;

  const isClosed = ticket.status === 'CLOSED' || ticket.status === 'CANCELLED';

  return (
    <>
      <Dialog open={open} onClose={onClose} maxWidth="md" fullWidth>
        <DialogTitle sx={{ bgcolor: '#1976d2', color: '#fff', display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
          <Box>
            <Typography variant="h6" fontWeight="bold">
              Protocolo ANATEL: {ticket.protocol}
            </Typography>
            <Typography variant="caption" sx={{ opacity: 0.9 }}>
              Categoria: {ticket.category} | Canal: {ticket.channel}
            </Typography>
          </Box>
          <Chip
            label={ticket.status}
            color={ticket.status === 'RESOLVED' || ticket.status === 'CLOSED' ? 'success' : 'warning'}
            sx={{ fontWeight: 'bold' }}
          />
        </DialogTitle>

        <DialogContent sx={{ p: 3 }}>
          {/* Card Resumo */}
          <Paper variant="outlined" sx={{ p: 2, mb: 2, borderRadius: 2, bgcolor: '#fbfbfb' }}>
            <Typography variant="subtitle1" fontWeight="bold" gutterBottom>
              {ticket.subject}
            </Typography>
            <Typography variant="body2" color="text.secondary" paragraph>
              {ticket.description}
            </Typography>
            <Box sx={{ display: 'flex', gap: 2, flexWrap: 'wrap', fontSize: '0.85rem', color: '#666' }}>
              <span>⏱️ SLA Limite: <strong>{new Date(ticket.slaDeadline).toLocaleString('pt-BR')}</strong></span>
              {ticket.workOrderId && <span>🛠️ O.S. Vinculada: <strong>#{ticket.workOrderId.substring(0, 8)}</strong></span>}
              {ticket.anatelSatisfactionRating && <span>⭐ Avaliação ANATEL: <strong>{ticket.anatelSatisfactionRating} / 5</strong></span>}
            </Box>
          </Paper>

          {/* Barra de Ações Rápidas de Nível */}
          {!isClosed && (
            <Box sx={{ display: 'flex', gap: 1, mb: 2, flexWrap: 'wrap' }}>
              <Button
                variant="outlined"
                color="warning"
                size="small"
                startIcon={<EscalateIcon />}
                onClick={() => setEscalateModalOpen(true)}
              >
                Escalonar para N2
              </Button>
              <Button
                variant="outlined"
                color="success"
                size="small"
                startIcon={<CheckCircleIcon />}
                onClick={() => setResolveModalOpen(true)}
              >
                Resolver (N2)
              </Button>
              <Button
                variant="outlined"
                color="error"
                size="small"
                startIcon={<BuildIcon />}
                onClick={() => setWoModalOpen(true)}
              >
                Gerar O.S. de Campo (N2)
              </Button>
              <Button
                variant="contained"
                color="primary"
                size="small"
                onClick={() => setCloseModalOpen(true)}
              >
                Encerrar com Nota ANATEL
              </Button>
            </Box>
          )}

          <Divider sx={{ my: 2 }} />

          {/* Timeline de Mensagens e Notas Internas */}
          <Typography variant="subtitle2" fontWeight="bold" gutterBottom>
            Histórico de Atendimento & Tramitação
          </Typography>

          {loading ? (
            <Box sx={{ display: 'flex', justifyContent: 'center', py: 4 }}>
              <CircularProgress size={30} />
            </Box>
          ) : (
            <Box sx={{ maxHeight: 300, overflowY: 'auto', mb: 2, display: 'flex', flexDirection: 'column', gap: 1.5 }}>
              {interactions.map((it) => (
                <Paper
                  key={it.id}
                  sx={{
                    p: 1.5,
                    borderRadius: 2,
                    bgcolor: it.isInternalNote ? '#fffde7' : '#f5f5f5',
                    borderLeft: it.isInternalNote ? '4px solid #fbc02d' : '4px solid #1976d2',
                  }}
                >
                  <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center', mb: 0.5 }}>
                    <Box sx={{ display: 'flex', alignItems: 'center', gap: 1 }}>
                      {it.isInternalNote && <LockIcon sx={{ fontSize: 16, color: '#fbc02d' }} />}
                      <Typography variant="subtitle2" fontWeight="bold">
                        {it.senderName} ({it.senderType})
                      </Typography>
                      {it.isInternalNote && (
                        <Chip label="Nota Interna" size="small" color="warning" variant="outlined" sx={{ height: 18, fontSize: '0.65rem' }} />
                      )}
                    </Box>
                    <Typography variant="caption" color="text.secondary">
                      {new Date(it.createdAt).toLocaleString('pt-BR')}
                    </Typography>
                  </Box>
                  <Typography variant="body2">{it.message}</Typography>
                </Paper>
              ))}
            </Box>
          )}

          {/* Input de Nova Interação */}
          {!isClosed && (
            <Box sx={{ display: 'flex', flexDirection: 'column', gap: 1 }}>
              <TextField
                fullWidth
                multiline
                rows={2}
                placeholder="Escreva uma mensagem para o cliente ou adicione uma nota interna..."
                value={newMessage}
                onChange={(e) => setNewMessage(e.target.value)}
                size="small"
              />
              <Box sx={{ display: 'flex', justifyContent: 'space-between', alignItems: 'center' }}>
                <FormControlLabel
                  control={<Switch checked={isInternal} onChange={(e) => setIsInternal(e.target.checked)} color="warning" size="small" />}
                  label={<Typography variant="caption">Nota interna (não visível ao cliente)</Typography>}
                />
                <Button
                  variant="contained"
                  color={isInternal ? 'warning' : 'primary'}
                  endIcon={<SendIcon />}
                  disabled={submitting || !newMessage.trim()}
                  onClick={handleSendMessage}
                  size="small"
                >
                  Enviar
                </Button>
              </Box>
            </Box>
          )}
        </DialogContent>

        <DialogActions sx={{ p: 2 }}>
          <Button onClick={onClose} color="inherit">
            Fechar
          </Button>
        </DialogActions>
      </Dialog>

      {/* Sub-modal: Escalonar para N2 */}
      <Dialog open={escalateModalOpen} onClose={() => setEscalateModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Escalonar para Suporte Nível 2</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            O Suporte N2 receberá um evento em tempo real no painel técnico.
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Motivo do Escalonamento"
            value={escalateReason}
            onChange={(e) => setEscalateReason(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setEscalateModalOpen(false)}>Cancelar</Button>
          <Button variant="contained" color="warning" onClick={handleEscalateToN2} disabled={submitting}>
            Confirmar Escalonamento
          </Button>
        </DialogActions>
      </Dialog>

      {/* Sub-modal: Resolver N2 */}
      <Dialog open={resolveModalOpen} onClose={() => setResolveModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Resolver Chamado (N2)</DialogTitle>
        <DialogContent>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Notas Técnicas de Resolução"
            placeholder="Ex: Reset de porta lógica executado na OLT, autenticação PPPoE restabelecida."
            value={resolutionNotes}
            onChange={(e) => setResolutionNotes(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setResolveModalOpen(false)}>Cancelar</Button>
          <Button variant="contained" color="success" onClick={handleResolveByN2} disabled={submitting}>
            Concluir Resolução
          </Button>
        </DialogActions>
      </Dialog>

      {/* Sub-modal: Gerar O.S. */}
      <Dialog open={woModalOpen} onClose={() => setWoModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Gerar Ordem de Serviço de Campo</DialogTitle>
        <DialogContent>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            O chamado será enviado para o Analista de Suporte agendar e incluir no roteirizador GeoCEP.
          </Typography>
          <TextField
            fullWidth
            multiline
            rows={3}
            label="Justificativa Técnica de Campo"
            placeholder="Ex: Sinal óptico atenuado em -32dBm na CTO. Necessário fusão e conectorização."
            value={woReason}
            onChange={(e) => setWoReason(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setWoModalOpen(false)}>Cancelar</Button>
          <Button variant="contained" color="error" onClick={handleEscalateToWorkOrder} disabled={submitting}>
            Gerar O.S.
          </Button>
        </DialogActions>
      </Dialog>

      {/* Sub-modal: Fechar Chamado ANATEL */}
      <Dialog open={closeModalOpen} onClose={() => setCloseModalOpen(false)} maxWidth="xs" fullWidth>
        <DialogTitle>Encerrar Chamado Regulatório</DialogTitle>
        <DialogContent sx={{ textAlign: 'center' }}>
          <Typography variant="body2" color="text.secondary" gutterBottom>
            Avaliação de Satisfação ANATEL (1 a 5 estrelas)
          </Typography>
          <Rating value={rating} onChange={(e, val) => setRating(val)} size="large" sx={{ my: 1 }} />
          <TextField
            fullWidth
            multiline
            rows={2}
            label="Observações Finais de Encerramento"
            value={closeNotes}
            onChange={(e) => setCloseNotes(e.target.value)}
            sx={{ mt: 1 }}
          />
        </DialogContent>
        <DialogActions>
          <Button onClick={() => setCloseModalOpen(false)}>Cancelar</Button>
          <Button variant="contained" color="primary" onClick={handleCloseTicket} disabled={submitting}>
            Encerrar
          </Button>
        </DialogActions>
      </Dialog>
    </>
  );
};

export default TicketDetailModal;
