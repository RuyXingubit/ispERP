package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.*;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.UserRole;
import br.dev.xb.isperp.entity.financial.*;
import br.dev.xb.isperp.event.DomainEvent;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.mapper.CustodyMapper;
import br.dev.xb.isperp.repository.InvoiceRepository;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.financial.BankDepositConfirmationRepository;
import br.dev.xb.isperp.repository.financial.CashTransferLogRepository;
import br.dev.xb.isperp.repository.financial.UserCashCustodyRepository;
import br.dev.xb.isperp.service.DomainEventPublisher;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CashCustodyService {

    private final UserCashCustodyRepository cashCustodyRepository;
    private final CashTransferLogRepository cashTransferLogRepository;
    private final BankDepositConfirmationRepository bankDepositRepository;
    private final UserRepository userRepository;
    private final InvoiceRepository invoiceRepository;
    private final DomainEventPublisher domainEventPublisher;
    private final CustodyMapper custodyMapper;

    @Transactional
    public UserCashCustody getOrCreateCustody(UUID userId) {
        return cashCustodyRepository.findByUserId(userId).orElseGet(() -> {
            User user = userRepository.findById(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Usuário não encontrado com ID: " + userId));
            UserCashCustody newCustody = UserCashCustody.builder()
                    .user(user)
                    .cpf(user.getCpf())
                    .currentBalance(BigDecimal.ZERO)
                    .build();
            return cashCustodyRepository.save(newCustody);
        });
    }

    @Transactional(readOnly = true)
    public CashCustodyDto getCustodyDtoByUserId(UUID userId) {
        UserCashCustody custody = getOrCreateCustody(userId);
        return custodyMapper.toDto(custody);
    }

    @Transactional(readOnly = true)
    public List<CashCustodyDto> getAllCustodies() {
        return custodyMapper.toCashCustodyDtoList(cashCustodyRepository.findAll());
    }

    /**
     * Recebimento de fatura em dinheiro vivo pelo técnico ou atendente.
     * Quita a fatura do cliente, gera recibo timbrado e debita o valor na custódia do CPF do colaborador.
     */
    @Transactional
    public CashCustodyDto recordCashSettlement(UUID userId, CashSettlementRequest request) {
        Invoice invoice = invoiceRepository.findById(request.getInvoiceId())
                .orElseThrow(() -> new ResourceNotFoundException("Fatura não encontrada com ID: " + request.getInvoiceId()));

        if (invoice.getStatus() == Invoice.InvoiceStatus.PAID) {
            throw new IllegalStateException("Esta fatura já se encontra quitada no sistema.");
        }

        UserCashCustody custody = getOrCreateCustody(userId);

        // 1. Quitar a fatura com identificação do recebedor
        invoice.setStatus(Invoice.InvoiceStatus.PAID);
        invoice.setPaymentMethod("DINHEIRO_ESPECIE");
        invoice.setPaidAmount(request.getAmount());
        invoice.setPaidAt(LocalDateTime.now());
        invoice.setSettledInCashByUserId(userId);
        invoice.setReceiptNumber(request.getReceiptNumber() != null ? request.getReceiptNumber() : "REC-" + System.currentTimeMillis());
        invoiceRepository.save(invoice);

        // 2. Debitar na custódia pessoal do colaborador (aumenta o saldo que ele deve à empresa)
        custody.credit(request.getAmount());
        cashCustodyRepository.save(custody);

        log.info("Recebimento em espécie registrado. Fatura: {}, Valor: R$ {}, CPF do Colaborador: {}",
                invoice.getId(), request.getAmount(), custody.getCpf());

        // 3. Disparar evento de domínio para orquestração automática (desbloqueio RADIUS, NFCom)
        Map<String, Object> payload = new HashMap<>();
        payload.put("invoiceId", invoice.getId());
        payload.put("contractId", invoice.getContractId());
        payload.put("customerId", invoice.getCustomerId());
        payload.put("amount", invoice.getPaidAmount());
        payload.put("paymentMethod", "DINHEIRO_ESPECIE");
        payload.put("settledByUserId", userId);

        domainEventPublisher.publish(GenericDomainEvent.builder()
                .eventType("INVOICE_PAID")
                .aggregateType("Invoice")
                .aggregateId(invoice.getId().toString())
                .payload(payload)
                .build());

        return custodyMapper.toDto(custody);
    }

    /**
     * Solicitação de transferência de valores entre dois colaboradores (Passagem de Gaveta/Turno).
     */
    @Transactional
    public CashTransferResponseDto requestTransfer(UUID senderUserId, CashTransferRequest request) {
        if (senderUserId.equals(request.getReceiverUserId())) {
            throw new IllegalArgumentException("Não é permitido transferir dinheiro para si mesmo.");
        }

        UserCashCustody senderCustody = getOrCreateCustody(senderUserId);
        if (senderCustody.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Saldo de custódia insuficiente para transferência. Saldo atual: R$ "
                    + senderCustody.getCurrentBalance() + ", Valor solicitado: R$ " + request.getAmount());
        }

        User sender = senderCustody.getUser();
        User receiver = userRepository.findById(request.getReceiverUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador recebedor não encontrado: " + request.getReceiverUserId()));

        CashTransferLog transferLog = CashTransferLog.builder()
                .sender(sender)
                .receiver(receiver)
                .amount(request.getAmount())
                .reason(request.getReason() != null ? request.getReason() : "Passagem de turno / caixa")
                .notes(request.getNotes())
                .status(CashTransferStatus.PENDING_ACCEPTANCE)
                .build();

        cashTransferLogRepository.save(transferLog);
        log.info("Solicitação de transferência de custódia criada. De: {} Para: {} Valor: R$ {}",
                sender.getName(), receiver.getName(), request.getAmount());

        return custodyMapper.toDto(transferLog);
    }

    /**
     * Resposta do colaborador recebedor: Aceite ou Rejeição (Duplo Aceite).
     */
    @Transactional
    public CashTransferResponseDto respondTransfer(UUID receiverUserId, UUID transferLogId, boolean accept) {
        CashTransferLog transfer = cashTransferLogRepository.findById(transferLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de transferência não encontrado: " + transferLogId));

        if (!transfer.getReceiver().getId().equals(receiverUserId)) {
            throw new IllegalStateException("Apenas o colaborador de destino pode aceitar ou rejeitar esta transferência.");
        }

        if (transfer.getStatus() != CashTransferStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException("Esta transferência já foi processada anteriormente. Status: " + transfer.getStatus());
        }

        transfer.setRespondedAt(OffsetDateTime.now());

        if (accept) {
            UserCashCustody senderCustody = getOrCreateCustody(transfer.getSender().getId());
            UserCashCustody receiverCustody = getOrCreateCustody(transfer.getReceiver().getId());

            if (senderCustody.getCurrentBalance().compareTo(transfer.getAmount()) < 0) {
                throw new IllegalStateException("O remetente não possui mais saldo suficiente para concluir a transferência.");
            }

            senderCustody.debit(transfer.getAmount());
            receiverCustody.credit(transfer.getAmount());

            cashCustodyRepository.save(senderCustody);
            cashCustodyRepository.save(receiverCustody);

            transfer.setStatus(CashTransferStatus.ACCEPTED);
            log.info("Transferência de custódia ACEITA. De: {} Para: {} Valor: R$ {}",
                    transfer.getSender().getName(), transfer.getReceiver().getName(), transfer.getAmount());
        } else {
            transfer.setStatus(CashTransferStatus.REJECTED);
            log.warn("Transferência de custódia REJEITADA pelo recebedor {}. Valor: R$ {}",
                    transfer.getReceiver().getName(), transfer.getAmount());
        }

        cashTransferLogRepository.save(transfer);
        return custodyMapper.toDto(transfer);
    }

    /**
     * Submissão de comprovante de depósito bancário pelo colaborador para prestação de contas.
     */
    @Transactional
    public BankDepositResponseDto submitBankDeposit(UUID depositorUserId, BankDepositRequest request) {
        UserCashCustody custody = getOrCreateCustody(depositorUserId);
        if (custody.getCurrentBalance().compareTo(request.getAmount()) < 0) {
            throw new IllegalStateException("Saldo de custódia insuficiente para este depósito. Saldo atual: R$ "
                    + custody.getCurrentBalance() + ", Valor do depósito: R$ " + request.getAmount());
        }

        User depositor = custody.getUser();
        BankDepositConfirmation deposit = BankDepositConfirmation.builder()
                .depositor(depositor)
                .amount(request.getAmount())
                .bankName(request.getBankName())
                .bankAgency(request.getBankAgency())
                .bankAccount(request.getBankAccount())
                .receiptFileUrl(request.getReceiptFileUrl())
                .depositDate(OffsetDateTime.now())
                .status(BankDepositStatus.PENDING_AUDIT)
                .notes(request.getNotes())
                .build();

        bankDepositRepository.save(deposit);
        log.info("Comprovante de depósito bancário submetido. Colaborador: {}, Valor: R$ {}, Banco: {}",
                depositor.getName(), request.getAmount(), request.getBankName());

        return custodyMapper.toDto(deposit);
    }

    /**
     * Auditoria e Conciliação Bancária por CFO ou Auditor Financeiro (Segregação de Funções).
     * Somente após a confirmação no extrato bancário o saldo sob o CPF do colaborador é baixado.
     */
    @Transactional
    public BankDepositResponseDto auditBankDeposit(UUID auditorUserId, UUID depositId, BankDepositAuditRequest request) {
        User auditor = userRepository.findById(auditorUserId)
                .orElseThrow(() -> new ResourceNotFoundException("Auditor não encontrado: " + auditorUserId));

        if (auditor.getRole() != UserRole.ADMIN && auditor.getRole() != UserRole.CFO &&
            auditor.getRole() != UserRole.DIRECTOR && auditor.getRole() != UserRole.FINANCIAL) {
            throw new SecurityException("Acesso negado: Apenas CFO, Administrador ou Gestor Financeiro podem conciliar depósitos bancários.");
        }

        BankDepositConfirmation deposit = bankDepositRepository.findById(depositId)
                .orElseThrow(() -> new ResourceNotFoundException("Depósito não encontrado com ID: " + depositId));

        if (deposit.getStatus() != BankDepositStatus.PENDING_AUDIT) {
            throw new IllegalStateException("Este depósito já foi auditado anteriormente com status: " + deposit.getStatus());
        }

        deposit.setAuditedBy(auditor);
        deposit.setAuditedAt(OffsetDateTime.now());

        if (Boolean.TRUE.equals(request.getApproved())) {
            UserCashCustody depositorCustody = getOrCreateCustody(deposit.getDepositor().getId());
            depositorCustody.debit(deposit.getAmount());
            cashCustodyRepository.save(depositorCustody);

            deposit.setStatus(BankDepositStatus.CONFIRMED_IN_BANK);
            deposit.setNotes(request.getNotes());
            log.info("Depósito bancário CONCILIADO e aprovado pelo auditor {}. Custódia de {} baixada em R$ {}.",
                    auditor.getName(), deposit.getDepositor().getName(), deposit.getAmount());
        } else {
            deposit.setStatus(BankDepositStatus.REJECTED);
            deposit.setRejectionReason(request.getRejectionReason() != null ? request.getRejectionReason() : "Comprovante divergente ou não localizado no extrato.");
            log.warn("Depósito bancário REJEITADO pelo auditor {}. Motivo: {}",
                    auditor.getName(), deposit.getRejectionReason());
        }

        bankDepositRepository.save(deposit);
        return custodyMapper.toDto(deposit);
    }

    @Transactional(readOnly = true)
    public List<CashTransferResponseDto> getPendingTransfersForReceiver(UUID receiverUserId) {
        return custodyMapper.toCashTransferDtoList(
                cashTransferLogRepository.findByReceiverIdAndStatus(receiverUserId, CashTransferStatus.PENDING_ACCEPTANCE));
    }

    @Transactional(readOnly = true)
    public List<BankDepositResponseDto> getPendingBankDeposits() {
        return custodyMapper.toBankDepositDtoList(
                bankDepositRepository.findByStatusOrderByDepositDateDesc(BankDepositStatus.PENDING_AUDIT));
    }
}
