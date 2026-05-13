package com.xingchen.backend.agent.human;

import com.xingchen.backend.agent.content.CriticAgent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class HumanReviewService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final CriticAgent criticAgent;

    private static final String APPROVAL_PREFIX = "human:approval:";
    private static final String USER_APPROVALS_PREFIX = "human:user:approvals:";
    private static final long DEFAULT_TTL_HOURS = 24;

    private final Map<String, ApprovalRequest> pendingApprovals = new ConcurrentHashMap<>();

    public ApprovalRequest requestApproval(
            Long userId,
            String agentType,
            String actionType,
            String riskLevel,
            String title,
            String description,
            Map<String, Object> payload) {

        long ttlHours = "CRITICAL".equals(riskLevel) ? 1 : ("HIGH".equals(riskLevel) ? 4 : DEFAULT_TTL_HOURS);

        ApprovalRequest request = ApprovalRequest.create(
                userId, agentType, actionType, riskLevel, title, description, payload, ttlHours);

        pendingApprovals.put(request.getRequestId(), request);

        String approvalKey = APPROVAL_PREFIX + request.getRequestId();
        String userApprovalsKey = USER_APPROVALS_PREFIX + userId;

        try {
            redisTemplate.opsForValue().set(approvalKey, request, Duration.ofHours(ttlHours));
            redisTemplate.opsForList().rightPush(userApprovalsKey, request.getRequestId());
            redisTemplate.expire(userApprovalsKey, Duration.ofHours(ttlHours * 2));
        } catch (Exception e) {
            log.warn("Redis存储审批请求失败，使用本地存储: requestId={}", request.getRequestId(), e);
        }

        log.info("人工审批请求已创建: requestId={}, userId={}, riskLevel={}, title={}",
                request.getRequestId(), userId, riskLevel, title);

        return request;
    }

    public ApprovalRequest getApprovalRequest(String requestId) {
        ApprovalRequest request = pendingApprovals.get(requestId);
        if (request != null) {
            return request;
        }

        try {
            String approvalKey = APPROVAL_PREFIX + requestId;
            Object cached = redisTemplate.opsForValue().get(approvalKey);
            if (cached instanceof ApprovalRequest) {
                return (ApprovalRequest) cached;
            }
        } catch (Exception e) {
            log.warn("从Redis获取审批请求失败: requestId={}", requestId, e);
        }

        return null;
    }

    public List<ApprovalRequest> getPendingApprovals(Long userId) {
        List<ApprovalRequest> localPending = pendingApprovals.values().stream()
                .filter(r -> r.getUserId().equals(userId) && r.isPending() && !r.isExpired())
                .collect(Collectors.toList());

        try {
            String userApprovalsKey = USER_APPROVALS_PREFIX + userId;
            List<Object> requestIds = redisTemplate.opsForList().range(userApprovalsKey, 0, -1);

            if (requestIds != null) {
                for (Object idObj : requestIds) {
                    String requestId = idObj.toString();
                    if (localPending.stream().noneMatch(r -> r.getRequestId().equals(requestId))) {
                        ApprovalRequest request = getApprovalRequest(requestId);
                        if (request != null && request.isPending() && !request.isExpired()) {
                            localPending.add(request);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("从Redis获取用户审批列表失败: userId={}", userId, e);
        }

        return localPending;
    }

    public ApprovalRequest approve(String requestId, Long reviewerId, String comment) {
        return processApproval(requestId, reviewerId, ApprovalRequest.ApprovalStatus.APPROVED, comment);
    }

    public ApprovalRequest reject(String requestId, Long reviewerId, String comment) {
        return processApproval(requestId, reviewerId, ApprovalRequest.ApprovalStatus.REJECTED, comment);
    }

    private ApprovalRequest processApproval(String requestId, Long reviewerId,
                                           ApprovalRequest.ApprovalStatus newStatus, String comment) {
        ApprovalRequest request = getApprovalRequest(requestId);
        if (request == null) {
            log.warn("审批请求不存在: requestId={}", requestId);
            return null;
        }

        if (!request.isPending()) {
            log.warn("审批请求状态不是待审批: requestId={}, status={}", requestId, request.getStatus());
            return request;
        }

        if (request.isExpired()) {
            request.setStatus(ApprovalRequest.ApprovalStatus.EXPIRED);
            log.warn("审批请求已过期: requestId={}", requestId);
            return request;
        }

        request.setStatus(newStatus);
        request.setReviewerId(reviewerId.toString());
        request.setReviewComment(comment);
        request.setReviewedAt(Instant.now());

        pendingApprovals.remove(requestId);

        try {
            String approvalKey = APPROVAL_PREFIX + requestId;
            redisTemplate.delete(approvalKey);
        } catch (Exception e) {
            log.warn("删除Redis中的审批请求失败: requestId={}", requestId, e);
        }

        log.info("审批请求已{}: requestId={}, reviewerId={}, comment={}",
                newStatus == ApprovalRequest.ApprovalStatus.APPROVED ? "批准" : "拒绝",
                requestId, reviewerId, comment);

        return request;
    }

    public boolean needsHumanApproval(Long userId, String agentType, String actionType,
                                     Map<String, Object> context, double riskScore) {
        if (riskScore >= 0.8) {
            return true;
        }

        if ("CRITICAL".equals(getRiskLevel(agentType, actionType, context))) {
            return true;
        }

        return false;
    }

    public String getRiskLevel(String agentType, String actionType, Map<String, Object> context) {
        if ("ContentGenerationAgent".equals(agentType) && "generate".equals(actionType)) {
            Object tags = context.get("tags");
            if (tags != null && tags.toString().toLowerCase().contains("敏感")) {
                return "CRITICAL";
            }
        }

        if ("ContentGenerationAgent".equals(agentType) && "publish".equals(actionType)) {
            return "HIGH";
        }

        if ("SEOOptimizationAgent".equals(agentType) && "update".equals(actionType)) {
            return "MEDIUM";
        }

        Object riskOverride = context.get("riskLevel");
        if (riskOverride != null) {
            return riskOverride.toString().toUpperCase();
        }

        return "LOW";
    }

    public ApprovalRequest requestApprovalIfNeeded(
            Long userId,
            String agentType,
            String actionType,
            Map<String, Object> context,
            double riskScore,
            String title,
            String description,
            Map<String, Object> payload) {

        if (!needsHumanApproval(userId, agentType, actionType, context, riskScore)) {
            return null;
        }

        String riskLevel = getRiskLevel(agentType, actionType, context);

        return requestApproval(
                userId,
                agentType,
                actionType,
                riskLevel,
                title,
                description,
                payload
        );
    }

    public void cancelExpiredApprovals() {
        List<String> expiredIds = pendingApprovals.values().stream()
                .filter(ApprovalRequest::isExpired)
                .map(ApprovalRequest::getRequestId)
                .collect(Collectors.toList());

        for (String requestId : expiredIds) {
            ApprovalRequest request = pendingApprovals.get(requestId);
            if (request != null) {
                request.setStatus(ApprovalRequest.ApprovalStatus.EXPIRED);
                pendingApprovals.remove(requestId);
                log.info("过期审批请求已标记: requestId={}", requestId);
            }
        }
    }

    public Map<String, Object> buildApprovalContext(ApprovalRequest request) {
        return Map.of(
                "requestId", request.getRequestId(),
                "agentType", request.getAgentType(),
                "actionType", request.getActionType(),
                "riskLevel", request.getRiskLevel(),
                "title", request.getTitle(),
                "description", request.getDescription(),
                "payload", request.getPayload() != null ? request.getPayload() : Map.of(),
                "createdAt", request.getCreatedAt().toString(),
                "expiresAt", request.getExpiresAt().toString(),
                "status", request.getStatus().name()
        );
    }
}