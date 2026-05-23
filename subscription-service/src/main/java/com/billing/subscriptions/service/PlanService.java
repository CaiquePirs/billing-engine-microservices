package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.mapper.PlanMapper;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.repository.PlanRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@AllArgsConstructor
public class PlanService {

    private final PlanRepository planRepository;
    private final PlanMapper planMapper;
    private final SecurityService securityService;
    private final StripePlanService stripePlanService;

    public Plan createPlan(PlanRequestDTO planRequestDTO) {
        Plan plan = planMapper.toEntity(planRequestDTO);

        UUID currentLoggedAdmin = securityService.getLoggedInAdmin();
        String stripePriceId = stripePlanService.createStripePrice(planRequestDTO);

        plan.setCreatedBy(currentLoggedAdmin);
        plan.setStripePriceId(stripePriceId);

        return planRepository.save(plan);
    }

    public Plan findPlanById(UUID planId) {
        return planRepository.findById(planId)
                .filter(plan -> plan.getActive().equals(true))
                .orElseThrow(() -> new NotFoundException("Plan ID not found"));
    }
}
