package com.xingchen.backend.agent.human;

import lombok.Data;
import lombok.Builder;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ApprovalRequest {

    private String requestId;
    private Long userId;
    private String agentType;
    private String actionType;
    private String riskLevel;
    private String title;
    private String description;
    private Map<String, Object> payload;
    private ApprovalStatus status;
    private String reviewerId;
    private String reviewComment;
    private Instant createdAt;
    private Instant reviewedAt;
    private Instant expiresAt;

    public enum ApprovalStatus {
        PENDING,
        APPROVED,
        REJECTED,
        EXPIRED,
        CANCELLED
    }

    public enum RiskLevel {
        LOW,
        MEDIUM,
        HIGH,
        CRITICAL
    }

    public boolean isHighRisk() {
        return "HIGH".equals(riskLevel) || "CRITICAL".equals(riskLevel);
    }

    public boolean isPending() {
        return status == ApprovalStatus.PENDING;
    }

    public boolean isExpired() {
        return expiresAt != null && Instant.now().isAfter(expiresAt);
    }

    public static ApprovalRequest create(
            Long userId,
            String agentType,
            String actionType,
            String riskLevel,
            String title,
            String description,
            Map<String, Object> payload,
            long ttlHours) {

        Instant now = Instant.now();
        return ApprovalRequest.builder()
                .requestId(java.util.UUID.randomUUID().toString())
                .userId(userId)
                .agentType(agentType)
                .actionType(actionType)
                .riskLevel(riskLevel)
                .title(title)
                .description(description)
                .payload(payload)
                .status(ApprovalStatus.PENDING)
                .createdAt(now)
                .expiresAt(now.plusSeconds(ttlHours * 3600))
                .build();
    }
}