package br.dev.xb.isperp.service.consumer;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.event.GenericDomainEvent;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.util.UuidCreatorUtils;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@SuppressWarnings({"null", "java:S2583"})
public final class ConsumerPayloadUtils {

    private ConsumerPayloadUtils() {
    }

    public record CustomerAndContract(Customer customer, Contract contract) {
    }

    @SuppressWarnings("unchecked")
    public static Map<String, Object> extractPayload(ObjectMapper objectMapper, Object payload) {
        if (payload instanceof Map) {
            return (Map<String, Object>) payload;
        }
        try {
            return objectMapper.readValue(payload.toString(), new TypeReference<Map<String, Object>>() {});
        } catch (Exception e) {
            throw new IllegalArgumentException("Erro ao converter payload para Map", e);
        }
    }

    public static CustomerAndContract upsertCustomerAndCreateContract(
            CustomerRepository customerRepository,
            ContractRepository contractRepository,
            Map<String, Object> data,
            Contract.ContractStatus initialStatus
    ) {
        String customerCpf = (String) data.get("customerCpf");
        String customerName = (String) data.get("customerName");
        String customerEmail = (String) data.get("customerEmail");
        String customerPhone = (String) data.get("customerPhone");
        String installationAddress = (String) data.get("installationAddress");
        String city = (String) data.get("city");
        String state = (String) data.get("state");
        String zipCode = (String) data.get("zipCode");
        UUID planId = UUID.fromString(data.get("planId").toString());
        UUID saleId = UUID.fromString(data.get("saleId").toString());
        BigDecimal monthlyFee = new BigDecimal(data.get("monthlyFee").toString());
        Integer preferredDueDate = data.get("preferredDueDate") != null
                ? Integer.parseInt(data.get("preferredDueDate").toString()) : 10;

        Customer customer = customerRepository.findByCpf(customerCpf).orElseGet(() -> Customer.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .cpf(customerCpf)
                .active(true)
                .build());

        customer.setName(customerName);
        customer.setEmail(customerEmail);
        customer.setPhone(customerPhone);
        customer.setAddress(installationAddress);
        customer.setCity(city);
        customer.setState(state);
        customer.setZipCode(zipCode);
        if (data.get("latitude") != null) {
            customer.setLatitude(new BigDecimal(data.get("latitude").toString()));
        }
        if (data.get("longitude") != null) {
            customer.setLongitude(new BigDecimal(data.get("longitude").toString()));
        }
        customer.setActive(true);

        Customer savedCustomer = customerRepository.save(customer);

        UUID ctoId = data.get("ctoId") != null ? UUID.fromString(data.get("ctoId").toString()) : null;
        Integer ctoPortNumber = data.get("ctoPortNumber") != null ? Integer.parseInt(data.get("ctoPortNumber").toString()) : null;

        String contractNumber = "CTR-" + System.currentTimeMillis();
        Contract contract = Contract.builder()
                .id(UuidCreatorUtils.generateUuidV7())
                .customerId(savedCustomer.getId())
                .planId(planId)
                .saleId(saleId)
                .contractNumber(contractNumber)
                .status(initialStatus)
                .monthlyFee(monthlyFee)
                .dueDay(preferredDueDate)
                .installationAddress(installationAddress)
                .city(city)
                .state(state)
                .zipCode(zipCode)
                .ctoId(ctoId)
                .ctoPortNumber(ctoPortNumber)
                .build();

        Contract savedContract = contractRepository.save(contract);
        return new CustomerAndContract(savedCustomer, savedContract);
    }

    public static GenericDomainEvent buildEvent(String eventType, String aggregateType, String aggregateId, Map<String, Object> payload) {
        return GenericDomainEvent.builder()
                .eventId(UuidCreatorUtils.generateUuidV7())
                .eventType(eventType)
                .aggregateType(aggregateType)
                .aggregateId(aggregateId)
                .payload(payload)
                .build();
    }
}
