package com.cmriddles.thenewereconomy.api.workflow;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Durable progress. DELIVERY_PENDING means delivery may have happened; never automatically replay
 * it.
 */
public record Workflow(
        UUID id,
        String key,
        UUID reservationId,
        List<String> steps,
        int currentStep,
        WorkflowStatus status,
        Optional<UUID> deliveryToken,
        String note) {
    public Workflow {
        steps = List.copyOf(steps);
    }
}
