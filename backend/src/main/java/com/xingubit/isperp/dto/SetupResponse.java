package com.xingubit.isperp.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SetupResponse {
    
    private Boolean isSetupCompleted;
    private Integer setupStep;
    private String message;
    
    public static SetupResponse success(String message) {
        return SetupResponse.builder()
                .isSetupCompleted(true)
                .setupStep(3)
                .message(message)
                .build();
    }
    
    public static SetupResponse inProgress(Integer step, String message) {
        return SetupResponse.builder()
                .isSetupCompleted(false)
                .setupStep(step)
                .message(message)
                .build();
    }
}