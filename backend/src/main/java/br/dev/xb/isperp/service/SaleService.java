package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CreateSaleRequest;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.Sale;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.repository.SaleRepository;
import br.dev.xb.isperp.util.CpfValidator;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
@SuppressWarnings("null")
public class SaleService {

    private final SaleRepository saleRepository;
    private final PlanRepository planRepository;
    private final DomainEventPublisher domainEventPublisher;

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public Optional<Sale> getSaleById(@NonNull UUID id) {
        return saleRepository.findById(id);
    }

    @Transactional
    public Sale submitSale(@NonNull CreateSaleRequest request) {
        log.info("Submetendo nova venda para cliente: {} (CPF: {})", request.getCustomerName(), request.getCustomerCpf());

        // 1. Validar e limpar CPF
        String cleanCpf = CpfValidator.clean(request.getCustomerCpf());
        if (!CpfValidator.isValid(cleanCpf)) {
            throw new RuntimeException("CPF inválido para cadastro da venda");
        }

        // 2. Validar existência do plano
        Plan plan = planRepository.findById(request.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plano contratado não encontrado"));

        if (!Boolean.TRUE.equals(plan.getActive())) {
            throw new RuntimeException("Plano selecionado está inativo no catálogo");
        }

        // 3. Criar e persistir Venda
        Sale sale = Sale.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .planId(plan.getId())
                .customerName(request.getCustomerName().trim())
                .customerCpf(cleanCpf)
                .customerEmail(request.getCustomerEmail() != null ? request.getCustomerEmail().trim() : null)
                .customerPhone(request.getCustomerPhone().replaceAll("\\D", ""))
                .installationAddress(request.getInstallationAddress().trim())
                .city(request.getCity().trim())
                .state(request.getState().trim().toUpperCase())
                .zipCode(request.getZipCode().replaceAll("\\D", ""))
                .preferredDueDate(request.getPreferredDueDate() != null ? request.getPreferredDueDate() : 10)
                .notificationChannel(request.getNotificationChannel() != null ? request.getNotificationChannel() : "WHATSAPP")
                .sellerName(request.getSellerName())
                .status(Sale.SaleStatus.SUBMITTED)
                .build();

        Sale savedSale = saleRepository.save(sale);

        // 4. Emitir evento de domínio atômico
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("saleId", savedSale.getId().toString());
        eventData.put("planId", plan.getId().toString());
        eventData.put("planName", plan.getName());
        eventData.put("monthlyFee", plan.getPrice());
        eventData.put("customerName", savedSale.getCustomerName());
        eventData.put("customerCpf", savedSale.getCustomerCpf());
        eventData.put("customerEmail", savedSale.getCustomerEmail());
        eventData.put("customerPhone", savedSale.getCustomerPhone());
        eventData.put("installationAddress", savedSale.getInstallationAddress());
        eventData.put("city", savedSale.getCity());
        eventData.put("state", savedSale.getState());
        eventData.put("zipCode", savedSale.getZipCode());
        eventData.put("preferredDueDate", savedSale.getPreferredDueDate());
        eventData.put("notificationChannel", savedSale.getNotificationChannel());

        GenericDomainEvent domainEvent = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("SALE_SUBMITTED")
                .aggregateType("Sale")
                .aggregateId(savedSale.getId().toString())
                .payload(eventData)
                .build();

        domainEventPublisher.publish(domainEvent);

        log.info("Venda {} submetida com sucesso. Evento SALE_SUBMITTED gravado na Outbox", savedSale.getId());
        return savedSale;
    }
}
