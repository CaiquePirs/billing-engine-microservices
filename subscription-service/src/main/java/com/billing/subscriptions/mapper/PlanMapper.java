package com.billing.subscriptions.mapper;

import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import com.billing.subscriptions.model.AuditLog;
import com.billing.subscriptions.model.Plan;
import org.springframework.stereotype.Component;

@Component
public class PlanMapper {

    public Plan toEntity(PlanRequestDTO planRequestDTO) {
        return Plan.builder()
                .name(planRequestDTO.name())
                .description(planRequestDTO.description())
                .price(planRequestDTO.price())
                .currency(planRequestDTO.currency())
                .active(true)
                .interval(planRequestDTO.interval())
                .auditLog(new AuditLog())
                .build();
    }

    public PlanResponseDTO toResponse(Plan plan) {
        return PlanResponseDTO.builder()
                .id(plan.getId())
                .name(plan.getName())
                .description(plan.getDescription())
                .interval(plan.getInterval().toString())
                .stripePriceId(plan.getStripePriceId())
                .currency(plan.getCurrency())
                .active(plan.getActive())
                .price(plan.getPrice())
                .build();
    }

}
