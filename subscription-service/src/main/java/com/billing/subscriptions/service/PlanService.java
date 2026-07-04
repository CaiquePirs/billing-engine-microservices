package com.billing.subscriptions.service;

import com.billing.subscriptions.controller.advice.exception.NotFoundException;
import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.mapper.PlanMapper;
import com.billing.subscriptions.metrics.BillingSubscriptionMetrics;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.repository.PlanRepository;
import io.micrometer.core.instrument.distribution.Histogram;
import io.micrometer.core.instrument.distribution.HistogramGauges;
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
    private final BillingSubscriptionMetrics billingSubscriptionMetrics;

    public Plan createPlan(PlanRequestDTO planRequestDTO) {
        Plan plan = planMapper.toEntity(planRequestDTO);

        UUID currentLoggedAdmin = securityService.getLoggedInAdmin();
        String stripePriceId = stripePlanService.createStripePrice(planRequestDTO);

        plan.setCreatedBy(currentLoggedAdmin);
        plan.setStripePriceId(stripePriceId);

        Plan planCreated = planRepository.save(plan);
        billingSubscriptionMetrics.recordPlanCreatedTotal(planCreated.getName());

        return planCreated;
    }

    public Plan findPlanById(UUID planId) {
        return planRepository.findById(planId)
                .filter(plan -> plan.getActive().equals(true))
                .orElseThrow(() -> new NotFoundException("No active plan found with ID: " + planId));
    }
}
