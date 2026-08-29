package br.dev.xb.isperp.service;

import br.dev.xb.isperp.entity.Contract;
import br.dev.xb.isperp.entity.Customer;
import br.dev.xb.isperp.entity.Invoice;
import br.dev.xb.isperp.entity.Plan;
import br.dev.xb.isperp.repository.ContractRepository;
import br.dev.xb.isperp.repository.CustomerRepository;
import br.dev.xb.isperp.repository.PlanRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class NfcomDecisionService {

    private final CustomerRepository customerRepository;
    private final ContractRepository contractRepository;
    private final PlanRepository planRepository;

    public boolean shouldIssueNfcom(Invoice invoice) {
        if (invoice == null) {
            return false;
        }

        // 1. Prioridade Máxima: Configuração no Nível do Cliente
        Optional<Customer> customerOpt = customerRepository.findById(invoice.getCustomerId());
        if (customerOpt.isPresent()) {
            Customer customer = customerOpt.get();
            if (customer.getAlwaysIssueNfcom() != null) {
                return customer.getAlwaysIssueNfcom();
            }
        }

        // 2. Segunda Prioridade: Configuração no Nível do Plano Contratado
        if (invoice.getContractId() != null) {
            Optional<Contract> contractOpt = contractRepository.findById(invoice.getContractId());
            if (contractOpt.isPresent()) {
                Contract contract = contractOpt.get();
                Optional<Plan> planOpt = planRepository.findById(contract.getPlanId());
                if (planOpt.isPresent()) {
                    Plan plan = planOpt.get();
                    if (plan.getAlwaysIssueNfcom() != null) {
                        return plan.getAlwaysIssueNfcom();
                    }
                }
            }
        }

        // 3. Padrão Geral do Sistema
        return false;
    }
}
