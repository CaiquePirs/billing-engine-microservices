package com.billing.subscriptions.controller;

import com.billing.subscriptions.controller.dto.PlanRequestDTO;
import com.billing.subscriptions.controller.dto.PlanResponseDTO;
import com.billing.subscriptions.mapper.PlanMapper;
import com.billing.subscriptions.model.Plan;
import com.billing.subscriptions.service.PlanService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/plans")
@RequiredArgsConstructor
public class PlanController {

    private final PlanService planService;
    private final PlanMapper planMapper;

    @PostMapping
    @PreAuthorize("hasAuthority('ROLE_TENANT')")
    public ResponseEntity<PlanResponseDTO> createPlan(@Valid @RequestBody PlanRequestDTO planRequestDTO) {
        Plan plan = planService.createPlan(planRequestDTO);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(planMapper.toResponse(plan));
    }

}
