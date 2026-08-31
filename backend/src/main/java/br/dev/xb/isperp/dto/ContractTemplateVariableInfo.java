package br.dev.xb.isperp.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContractTemplateVariableInfo {

    private String tag;
    private String label;
    private String category; // CUSTOMER, COMPANY, CONTRACT, PLAN, SIGNATURE
    private String example;
    private String description;
}
