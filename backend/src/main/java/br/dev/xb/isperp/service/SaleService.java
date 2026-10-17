package br.dev.xb.isperp.service;

import br.dev.xb.isperp.dto.CreateSaleRequest;
import br.dev.xb.isperp.entity.FtthCto;
import br.dev.xb.isperp.entity.FtthCtoPort;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.entity.Sale;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.ftth.FtthPortStatus;
import br.dev.xb.isperp.repository.FtthCtoPortRepository;
import br.dev.xb.isperp.repository.FtthCtoRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.repository.SaleRepository;
import br.dev.xb.isperp.util.CpfValidator;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.Nullable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Comparator;
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

    @Autowired(required = false)
    private @Nullable FtthCtoRepository ctoRepository;

    @Autowired(required = false)
    private @Nullable FtthCtoPortRepository ctoPortRepository;

    public List<Sale> getAllSales() {
        return saleRepository.findAll();
    }

    public Optional<Sale> getSaleById(UUID id) {
        return saleRepository.findById(id);
    }

    @Transactional
    public Sale submitSale(CreateSaleRequest request) {
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

        // 3. Viabilidade Técnica Comercial por CTO e Pré-Reserva de Porta (Onda L)
        UUID resolvedCtoId = request.getCtoId();
        Integer resolvedPortNumber = request.getCtoPortNumber();

        if (ctoRepository != null && ctoPortRepository != null) {
            if (resolvedCtoId == null && request.getLatitude() != null && request.getLongitude() != null) {
                resolvedCtoId = findNearestViableCtoId(request.getLatitude(), request.getLongitude());
            }
            if (resolvedCtoId != null) {
                resolvedPortNumber = reserveCtoPortForSale(
                        resolvedCtoId,
                        resolvedPortNumber,
                        request.getCustomerName().trim()
                );
            }
        }

        // 4. Criar e persistir Venda
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
                .ctoId(resolvedCtoId)
                .ctoPortNumber(resolvedPortNumber)
                .latitude(request.getLatitude() != null ? BigDecimal.valueOf(request.getLatitude()) : null)
                .longitude(request.getLongitude() != null ? BigDecimal.valueOf(request.getLongitude()) : null)
                .preferredScheduleDate(request.getPreferredScheduleDate())
                .preferredSchedulePeriod(request.getPreferredSchedulePeriod() != null ? request.getPreferredSchedulePeriod().trim().toUpperCase() : null)
                .status(Sale.SaleStatus.SUBMITTED)
                .build();

        Sale savedSale = saleRepository.save(sale);

        // 5. Emitir evento de domínio atômico
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
        if (savedSale.getCtoId() != null) {
            eventData.put("ctoId", savedSale.getCtoId().toString());
        }
        if (savedSale.getCtoPortNumber() != null) {
            eventData.put("ctoPortNumber", savedSale.getCtoPortNumber());
        }
        if (savedSale.getLatitude() != null) {
            eventData.put("latitude", savedSale.getLatitude().doubleValue());
        }
        if (savedSale.getLongitude() != null) {
            eventData.put("longitude", savedSale.getLongitude().doubleValue());
        }
        if (savedSale.getPreferredScheduleDate() != null) {
            eventData.put("preferredScheduleDate", savedSale.getPreferredScheduleDate().toString());
        }
        if (savedSale.getPreferredSchedulePeriod() != null) {
            eventData.put("preferredSchedulePeriod", savedSale.getPreferredSchedulePeriod());
        }

        GenericDomainEvent domainEvent = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("SALE_SUBMITTED")
                .aggregateType("Sale")
                .aggregateId(savedSale.getId().toString())
                .payload(eventData)
                .build();

        domainEventPublisher.publish(domainEvent);

        log.info("Venda {} submetida com sucesso (CTO={}, Porta={}). Evento SALE_SUBMITTED gravado na Outbox",
                savedSale.getId(), savedSale.getCtoId(), savedSale.getCtoPortNumber());
        return savedSale;
    }

    private @Nullable UUID findNearestViableCtoId(double lat, double lon) {
        if (ctoRepository == null || ctoPortRepository == null) return null;
        List<FtthCto> allCtos = ctoRepository.findAll();
        FtthCto nearest = null;
        double minDistance = 300.0; // Raio comercial padrão de 300m

        for (FtthCto cto : allCtos) {
            if (cto.getLatitude() != null && cto.getLongitude() != null) {
                long freePorts = ctoPortRepository.countByCtoIdAndStatus(cto.getId(), FtthPortStatus.LIVRE);
                if (freePorts > 0) {
                    double dist = calculateHaversineMeters(lat, lon, cto.getLatitude().doubleValue(), cto.getLongitude().doubleValue());
                    if (dist <= minDistance) {
                        minDistance = dist;
                        nearest = cto;
                    }
                }
            }
        }
        return nearest != null ? nearest.getId() : null;
    }

    private @Nullable Integer reserveCtoPortForSale(UUID ctoId, @Nullable Integer requestedPort, String customerName) {
        if (ctoPortRepository == null) return requestedPort;
        List<FtthCtoPort> ports = ctoPortRepository.findByCtoIdOrderByPortNumberAsc(ctoId);
        if (ports.isEmpty()) {
            return requestedPort != null ? requestedPort : 1;
        }

        Optional<FtthCtoPort> targetPortOpt = Optional.empty();
        if (requestedPort != null) {
            targetPortOpt = ports.stream()
                    .filter(p -> p.getPortNumber() == requestedPort && p.getStatus() == FtthPortStatus.LIVRE)
                    .findFirst();
        }
        if (targetPortOpt.isEmpty()) {
            targetPortOpt = ports.stream()
                    .filter(p -> p.getStatus() == FtthPortStatus.LIVRE)
                    .min(Comparator.comparingInt(FtthCtoPort::getPortNumber));
        }

        if (targetPortOpt.isPresent()) {
            FtthCtoPort port = targetPortOpt.get();
            port.setStatus(FtthPortStatus.RESERVADA);
            port.setNotes("Reservada no Onboarding Comercial para " + customerName);
            ctoPortRepository.save(port);
            log.info("Porta {} da CTO {} reservada automaticamente para {}", port.getPortNumber(), ctoId, customerName);
            return port.getPortNumber();
        }

        return requestedPort;
    }

    private double calculateHaversineMeters(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

    public void setCtoRepositories(@Nullable FtthCtoRepository ctoRepository, @Nullable FtthCtoPortRepository ctoPortRepository) {
        this.ctoRepository = ctoRepository;
        this.ctoPortRepository = ctoPortRepository;
    }
}
