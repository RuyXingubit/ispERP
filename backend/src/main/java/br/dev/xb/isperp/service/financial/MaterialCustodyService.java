package br.dev.xb.isperp.service.financial;

import br.dev.xb.isperp.dto.financial.MaterialCustodyDto;
import br.dev.xb.isperp.dto.financial.MaterialTransferRequest;
import br.dev.xb.isperp.dto.financial.MaterialTransferResponseDto;
import br.dev.xb.isperp.entity.User;
import br.dev.xb.isperp.entity.financial.CashTransferStatus;
import br.dev.xb.isperp.entity.financial.MaterialTransferLog;
import br.dev.xb.isperp.entity.financial.UserMaterialCustody;
import br.dev.xb.isperp.exception.ResourceNotFoundException;
import br.dev.xb.isperp.mapper.CustodyMapper;
import br.dev.xb.isperp.repository.UserRepository;
import br.dev.xb.isperp.repository.financial.MaterialTransferLogRepository;
import br.dev.xb.isperp.repository.financial.UserMaterialCustodyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MaterialCustodyService {

    private final UserMaterialCustodyRepository materialCustodyRepository;
    private final MaterialTransferLogRepository materialTransferRepository;
    private final UserRepository userRepository;
    private final CustodyMapper custodyMapper;

    @Transactional(readOnly = true)
    public List<MaterialCustodyDto> getMaterialsByUserId(UUID userId) {
        return custodyMapper.toMaterialCustodyDtoList(materialCustodyRepository.findByUserId(userId));
    }

    /**
     * Carga patrimonial inicial realizada pelo Almoxarifado para o CPF do colaborador.
     * Veículo não tem CPF: o material fica sob a responsabilidade civil do técnico.
     */
    @Transactional
    public MaterialCustodyDto allocateMaterialToUser(UUID userId, MaterialCustodyDto dto) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("Colaborador não encontrado com ID: " + userId));

        UserMaterialCustody custody = UserMaterialCustody.builder()
                .user(user)
                .itemName(dto.getItemName())
                .itemType(dto.getItemType())
                .serialNumber(dto.getSerialNumber())
                .macAddress(dto.getMacAddress())
                .quantity(dto.getQuantity() != null ? dto.getQuantity() : BigDecimal.ONE)
                .unit(dto.getUnit() != null ? dto.getUnit() : "UN")
                .allocatedAt(OffsetDateTime.now())
                .notes(dto.getNotes())
                .build();

        UserMaterialCustody saved = materialCustodyRepository.save(custody);
        log.info("Carga patrimonial alocada ao CPF do colaborador {}. Item: {}, Serial: {}, Qtd: {}",
                user.getName(), saved.getItemName(), saved.getSerialNumber(), saved.getQuantity());

        return custodyMapper.toDto(saved);
    }

    /**
     * Solicitação de transferência de materiais/ferramentas entre técnicos na rua (Duplo Aceite).
     */
    @Transactional
    public MaterialTransferResponseDto requestMaterialTransfer(UUID senderUserId, MaterialTransferRequest request) {
        if (senderUserId.equals(request.getReceiverUserId())) {
            throw new IllegalArgumentException("Não é permitido transferir materiais para si mesmo.");
        }

        UserMaterialCustody custody = materialCustodyRepository.findById(request.getMaterialCustodyId())
                .orElseThrow(() -> new ResourceNotFoundException("Custódia de material não encontrada: " + request.getMaterialCustodyId()));

        if (!custody.getUser().getId().equals(senderUserId)) {
            throw new IllegalStateException("Você não é o detentor da carga deste material.");
        }

        if (custody.getQuantity().compareTo(request.getQuantity()) < 0) {
            throw new IllegalStateException("Quantidade solicitada (" + request.getQuantity() + ") excede a quantidade disponível (" + custody.getQuantity() + ").");
        }

        User sender = custody.getUser();
        User receiver = userRepository.findById(request.getReceiverUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Técnico recebedor não encontrado: " + request.getReceiverUserId()));

        MaterialTransferLog transferLog = MaterialTransferLog.builder()
                .sender(sender)
                .receiver(receiver)
                .materialCustody(custody)
                .quantity(request.getQuantity())
                .status(CashTransferStatus.PENDING_ACCEPTANCE)
                .notes(request.getNotes())
                .build();

        materialTransferRepository.save(transferLog);
        log.info("Solicitação de transferência de material criada. De: {} Para: {} Item: {} Qtd: {}",
                sender.getName(), receiver.getName(), custody.getItemName(), request.getQuantity());

        return custodyMapper.toDto(transferLog);
    }

    /**
     * Resposta do técnico recebedor: Aceite ou Rejeição da carga física com conferência de seriais.
     */
    @Transactional
    public MaterialTransferResponseDto respondMaterialTransfer(UUID receiverUserId, UUID transferLogId, boolean accept) {
        MaterialTransferLog transfer = materialTransferRepository.findById(transferLogId)
                .orElseThrow(() -> new ResourceNotFoundException("Registro de transferência não encontrado: " + transferLogId));

        if (!transfer.getReceiver().getId().equals(receiverUserId)) {
            throw new IllegalStateException("Apenas o técnico de destino pode aceitar esta carga de material.");
        }

        if (transfer.getStatus() != CashTransferStatus.PENDING_ACCEPTANCE) {
            throw new IllegalStateException("Esta transferência já foi processada anteriormente. Status: " + transfer.getStatus());
        }

        transfer.setRespondedAt(OffsetDateTime.now());

        if (accept) {
            UserMaterialCustody originalCustody = transfer.getMaterialCustody();

            if (originalCustody.getQuantity().compareTo(transfer.getQuantity()) < 0) {
                throw new IllegalStateException("A quantidade disponível na carga do remetente foi alterada e é insuficiente.");
            }

            // Se for transferência total do item serializado (ex: 1 ONT ou 1 Máquina de Fusão)
            if (originalCustody.getQuantity().compareTo(transfer.getQuantity()) == 0) {
                originalCustody.setUser(transfer.getReceiver());
                originalCustody.setAllocatedAt(OffsetDateTime.now());
                materialCustodyRepository.save(originalCustody);
            } else {
                // Transferência parcial de insumo fracionado (ex: metros de cabo drop ou conectores)
                originalCustody.setQuantity(originalCustody.getQuantity().subtract(transfer.getQuantity()));
                materialCustodyRepository.save(originalCustody);

                UserMaterialCustody newCustody = UserMaterialCustody.builder()
                        .user(transfer.getReceiver())
                        .itemName(originalCustody.getItemName())
                        .itemType(originalCustody.getItemType())
                        .serialNumber(originalCustody.getSerialNumber())
                        .macAddress(originalCustody.getMacAddress())
                        .quantity(transfer.getQuantity())
                        .unit(originalCustody.getUnit())
                        .allocatedAt(OffsetDateTime.now())
                        .notes("Transferido de " + transfer.getSender().getName())
                        .build();
                materialCustodyRepository.save(newCustody);
            }

            transfer.setStatus(CashTransferStatus.ACCEPTED);
            log.info("Transferência de material ACEITA por {}. Responsabilidade transferida.", transfer.getReceiver().getName());
        } else {
            transfer.setStatus(CashTransferStatus.REJECTED);
            log.warn("Transferência de material REJEITADA por {}.", transfer.getReceiver().getName());
        }

        materialTransferRepository.save(transfer);
        return custodyMapper.toDto(transfer);
    }

    /**
     * Baixa de material na conclusão de Ordem de Serviço (ONU instalada no cliente ou drop consumido).
     */
    @Transactional
    public void consumeMaterialOnWorkOrder(UUID technicianUserId, String serialNumber, BigDecimal quantity) {
        Optional<UserMaterialCustody> custodyOpt = materialCustodyRepository.findBySerialNumber(serialNumber);

        if (custodyOpt.isPresent()) {
            UserMaterialCustody custody = custodyOpt.get();
            if (custody.getUser().getId().equals(technicianUserId)) {
                if (custody.getQuantity().compareTo(quantity) <= 0) {
                    materialCustodyRepository.delete(custody);
                } else {
                    custody.setQuantity(custody.getQuantity().subtract(quantity));
                    materialCustodyRepository.save(custody);
                }
                log.info("Material serial {} consumido com sucesso da carga do técnico.", serialNumber);
            }
        }
    }

    @Transactional(readOnly = true)
    public List<MaterialTransferResponseDto> getPendingTransfersForReceiver(UUID receiverUserId) {
        return custodyMapper.toMaterialTransferDtoList(
                materialTransferRepository.findByReceiverIdAndStatus(receiverUserId, CashTransferStatus.PENDING_ACCEPTANCE));
    }
}
