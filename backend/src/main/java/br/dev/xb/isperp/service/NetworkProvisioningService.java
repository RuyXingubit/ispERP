package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.OnuProvisioning;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.network.NetworkDriverResolver;
import br.dev.xb.isperp.network.NetworkDriverType;
import br.dev.xb.isperp.network.NetworkProvisioner;
import br.dev.xb.isperp.network.dto.OnuProvisionRequest;
import br.dev.xb.isperp.network.dto.OnuStatusResponse;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.OnuProvisioningRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class NetworkProvisioningService {

    private final OnuProvisioningRepository onuRepository;
    private final CustomerRepository customerRepository;
    private final PlanRepository planRepository;
    private final NetworkDriverResolver driverResolver;
    private final DomainEventPublisher domainEventPublisher;

    public List<OnuProvisioning> getAllProvisionings() {
        return onuRepository.findAll();
    }

    public Optional<OnuProvisioning> getProvisioningById(@NonNull UUID id) {
        return onuRepository.findById(id);
    }

    public Optional<OnuProvisioning> getProvisioningByContractId(@NonNull UUID contractId) {
        return onuRepository.findByContractId(contractId);
    }

    @Transactional
    public OnuProvisioning provisionOnuForContract(@NonNull Contract contract, @NonNull String onuMac, @NonNull String onuSerial, BigDecimal rxPowerDbm) {
        log.info("Iniciando provisionamento de rede para contrato {}: MAC={}, SN={}, dBm={}",
                contract.getContractNumber(), onuMac, onuSerial, rxPowerDbm);

        Customer customer = customerRepository.findById(contract.getCustomerId())
                .orElseThrow(() -> new RuntimeException("Cliente não encontrado para provisionamento"));

        Plan plan = planRepository.findById(contract.getPlanId())
                .orElseThrow(() -> new RuntimeException("Plano não encontrado para provisionamento"));

        NetworkDriverResolver.ResolvedNetworkDriver resolved = driverResolver.resolve(NetworkDriverType.SMARTOLT);
        NetworkProvisioner driver = resolved.provisioner();

        OnuProvisionRequest req = OnuProvisionRequest.builder()
                .contractId(contract.getId())
                .customerId(customer.getId())
                .customerName(customer.getName())
                .onuMac(onuMac)
                .onuSerial(onuSerial)
                .vlanId(100)
                .pppoeUser(customer.getCpf())
                .pppoePassword("xb123456")
                .downloadSpeed(plan.getDownloadSpeed())
                .uploadSpeed(plan.getUploadSpeed())
                .rxPowerDbm(rxPowerDbm)
                .build();

        driver.provisionOnu(req, resolved.device());

        OnuProvisioning onu = onuRepository.findByContractId(contract.getId())
                .orElse(OnuProvisioning.builder()
                        .id(UuidCreatorUtils.generateUuidV7())
                        .contractId(contract.getId())
                        .customerId(customer.getId())
                        .build());

        onu.setNetworkDeviceId(resolved.device().getId());
        onu.setOnuMac(onuMac);
        onu.setOnuSerial(onuSerial);
        onu.setVlanId(100);
        onu.setPppoeUser(customer.getCpf());
        onu.setPppoePassword("xb123456");
        onu.setDownloadSpeed(plan.getDownloadSpeed());
        onu.setUploadSpeed(plan.getUploadSpeed());
        onu.setStatus(OnuProvisioning.OnuStatus.PROVISIONED);
        onu.setRxPowerDbm(rxPowerDbm);
        onu.setLastSyncAt(LocalDateTime.now());

        OnuProvisioning saved = onuRepository.save(onu);

        // Emite evento ONU_PROVISIONED
        Map<String, Object> payload = new HashMap<>();
        payload.put("onuId", saved.getId().toString());
        payload.put("contractId", contract.getId().toString());
        payload.put("customerId", customer.getId().toString());
        payload.put("onuMac", saved.getOnuMac());
        payload.put("onuSerial", saved.getOnuSerial());
        payload.put("downloadSpeed", saved.getDownloadSpeed());
        payload.put("uploadSpeed", saved.getUploadSpeed());
        payload.put("status", saved.getStatus().name());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("ONU_PROVISIONED")
                .aggregateType("OnuProvisioning")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);

        log.info("ONU {} provisionada com sucesso na OLT {}.", saved.getOnuMac(), resolved.device().getName());
        return saved;
    }

    @Transactional
    public OnuProvisioning blockInternetAccess(@NonNull UUID contractId, String reason) {
        log.info("Bloqueando acesso de internet para contrato {}. Motivo: {}", contractId, reason);

        OnuProvisioning onu = onuRepository.findByContractId(contractId)
                .orElseThrow(() -> new RuntimeException("Provisionamento de ONU não encontrado para o contrato"));

        NetworkDriverResolver.ResolvedNetworkDriver resolved = driverResolver.resolve(NetworkDriverType.SMARTOLT);
        resolved.provisioner().blockInternetAccess(onu.getOnuMac(), reason, resolved.device());

        onu.setStatus(OnuProvisioning.OnuStatus.BLOCKED);
        OnuProvisioning saved = onuRepository.save(onu);

        Map<String, Object> payload = new HashMap<>();
        payload.put("onuId", saved.getId().toString());
        payload.put("contractId", contractId.toString());
        payload.put("reason", reason);

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("INTERNET_ACCESS_BLOCKED")
                .aggregateType("OnuProvisioning")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);
        return saved;
    }

    @Transactional
    public OnuProvisioning unblockInternetAccess(@NonNull UUID contractId) {
        log.info("Desbloqueando acesso de internet para contrato {}", contractId);

        OnuProvisioning onu = onuRepository.findByContractId(contractId)
                .orElseThrow(() -> new RuntimeException("Provisionamento de ONU não encontrado para o contrato"));

        NetworkDriverResolver.ResolvedNetworkDriver resolved = driverResolver.resolve(NetworkDriverType.SMARTOLT);
        resolved.provisioner().unblockInternetAccess(onu.getOnuMac(), resolved.device());

        onu.setStatus(OnuProvisioning.OnuStatus.PROVISIONED);
        OnuProvisioning saved = onuRepository.save(onu);

        Map<String, Object> payload = new HashMap<>();
        payload.put("onuId", saved.getId().toString());
        payload.put("contractId", contractId.toString());

        GenericDomainEvent event = GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType("INTERNET_ACCESS_UNBLOCKED")
                .aggregateType("OnuProvisioning")
                .aggregateId(saved.getId().toString())
                .payload(payload)
                .build();

        domainEventPublisher.publish(event);
        return saved;
    }

    public OnuStatusResponse diagnoseOnuSignal(@NonNull UUID onuId) {
        OnuProvisioning onu = onuRepository.findById(onuId)
                .orElseThrow(() -> new RuntimeException("ONU não encontrada"));

        NetworkDriverResolver.ResolvedNetworkDriver resolved = driverResolver.resolve(NetworkDriverType.SMARTOLT);
        OnuStatusResponse status = resolved.provisioner().checkOnuStatus(onu.getOnuMac(), resolved.device());

        if (status.getRxPowerDbm() != null) {
            onu.setRxPowerDbm(status.getRxPowerDbm());
            onu.setLastSyncAt(LocalDateTime.now());
            onuRepository.save(onu);
        }

        return status;
    }
}
